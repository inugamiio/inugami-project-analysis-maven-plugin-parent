package io.inugami.maven.plugin.analysis.plugin.services.preAnalyzers;

import io.inugami.api.processors.ConfigHandler;
import io.inugami.configuration.services.ConfigHandlerHashMap;
import io.inugami.maven.plugin.analysis.api.actions.ProjectPreAnalyzer;
import io.inugami.maven.plugin.analysis.api.actions.QueryConfigurator;
import io.inugami.maven.plugin.analysis.api.models.Gav;
import io.inugami.maven.plugin.analysis.api.models.Relationship;
import io.inugami.maven.plugin.analysis.api.models.ScanConext;
import io.inugami.maven.plugin.analysis.api.models.ScanNeo4jResult;
import io.inugami.maven.plugin.analysis.api.tools.Neo4jUtils;
import io.inugami.maven.plugin.analysis.api.tools.QueriesLoader;
import io.inugami.maven.plugin.analysis.api.tools.TemplateRendering;
import io.inugami.maven.plugin.analysis.plugin.services.info.publish.Publish;
import io.inugami.maven.plugin.analysis.plugin.services.neo4j.DefaultNeo4jDao;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.Record;
import org.neo4j.driver.internal.InternalRelationship;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.inugami.maven.plugin.analysis.api.tools.Neo4jUtils.getRelationship;

@Slf4j
public class PublishInfoKeeper implements ProjectPreAnalyzer, QueryConfigurator {


    // =========================================================================
    // QUERIES
    // =========================================================================
    private static final List<String> QUERIES = List.of("META-INF/queries/search_publish_artifact_info.cql");

    @Override
    public boolean accept(final String queryPath) {
        return QUERIES.contains(queryPath);
    }

    @Override
    public ConfigHandler<String, String> configure(final String queryPath, final Gav gav,
                                                   final ConfigHandler<String, String> configuration) {
        final ConfigHandler<String, String> config = new ConfigHandlerHashMap(configuration);
        config.putAll(gavToMap(gav));
        return config;
    }

    // =========================================================================
    // API
    // =========================================================================
    @Override
    public void preAnalyze(final ScanConext context) {
        final DefaultNeo4jDao dao = new DefaultNeo4jDao(context.getConfiguration());
        context.getConfiguration().put("useMavenProject", "true");
        final Gav gav = Neo4jUtils.buildGav(context.getProject(), context.getConfiguration());
        final String query = TemplateRendering.render(QueriesLoader.getQuery(QUERIES.get(0)),
                                                      configure(QUERIES.get(0),
                                                                gav,
                                                                context.getConfiguration()));

        log.debug(query);
        final List<Record> recordResult = dao.search(query);
        if (recordResult != null) {
            for (final Record record : recordResult) {
                savePublishInfo(record, context);
            }

        }
        dao.shutdown();
    }

    private void savePublishInfo(final Record record, final ScanConext context) {
        final ScanNeo4jResult backup = context.getPostNeo4jResult();

        final Map<String, Object>  data                = record.asMap();
        final String               version             = Neo4jUtils.getNodeName(data.get("version"));
        final String               env                 = Neo4jUtils.getNodeName(data.get("env"));
        final InternalRelationship deploy              = getRelationship(data.get("deploy"));
        final InternalRelationship haveArtifactVersion = getRelationship(data.get("haveArtifactVersion"));

        if (version != null && env != null) {
            backup.addRelationship(Relationship.builder()
                                               .from(version)
                                               .to(env)
                                               .type(Publish.DEPLOY)
                                               .properties(extractProperties(deploy))
                                               .build());

            backup.addRelationship(Relationship.builder()
                                               .from(env)
                                               .to(version)
                                               .type(Publish.HAVE_ARTIFACT_VERSION)
                                               .properties(extractProperties(haveArtifactVersion))
                                               .build());
        }
    }

    private LinkedHashMap<String, Serializable> extractProperties(final InternalRelationship relationship) {
        final LinkedHashMap<String, Serializable> result = new LinkedHashMap<>();
        if (relationship != null) {
            final Map<String, Object> properties = relationship.asMap();
            for (final Map.Entry<String, Object> entry : properties.entrySet()) {
                if (entry.getValue() == null) {
                    continue;
                }
                else if (entry.getValue() instanceof Serializable) {
                    result.put(entry.getKey(), (Serializable) entry.getValue());
                }
                else {
                    result.put(entry.getKey(), String.valueOf(entry.getValue()));
                }
            }
        }

        return result;
    }
    // =========================================================================
    // OVERRIDES
    // =========================================================================

    // =========================================================================
    // GETTERS & SETTERS
    // =========================================================================
}
