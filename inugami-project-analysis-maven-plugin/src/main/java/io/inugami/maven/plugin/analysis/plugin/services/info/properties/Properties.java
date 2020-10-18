package io.inugami.maven.plugin.analysis.plugin.services.info.properties;

import io.inugami.api.models.JsonBuilder;
import io.inugami.api.processors.ConfigHandler;
import io.inugami.api.tools.ConsoleColors;
import io.inugami.configuration.services.ConfigHandlerHashMap;
import io.inugami.maven.plugin.analysis.api.actions.ProjectInformation;
import io.inugami.maven.plugin.analysis.api.actions.QueryConfigurator;
import io.inugami.maven.plugin.analysis.api.models.Gav;
import io.inugami.maven.plugin.analysis.api.tools.QueriesLoader;
import io.inugami.maven.plugin.analysis.api.tools.TemplateRendering;
import io.inugami.maven.plugin.analysis.plugin.services.neo4j.Neo4jDao;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.project.MavenProject;
import org.neo4j.driver.Record;
import org.neo4j.driver.Value;
import org.neo4j.driver.types.Node;

import java.util.*;

@Slf4j
public class Properties implements ProjectInformation, QueryConfigurator {

    // =========================================================================
    // ATTRIBUTES
    // =========================================================================
    private static final List<String> QUERIES = List.of(
            "META-INF/queries/search_properties.cql"
                                                       );

    // =========================================================================
    // QUERIES
    // =========================================================================
    @Override
    public boolean accept(final String queryPath) {
        return QUERIES.contains(queryPath);
    }

    @Override
    public ConfigHandler<String, String> configure(final String queryPath, final Gav gav,
                                                   final ConfigHandler<String, String> configuration) {
        final ConfigHandler<String, String> config = new ConfigHandlerHashMap(configuration);
        config.putAll(Map.ofEntries(
                Map.entry("groupId", gav.getGroupId()),
                Map.entry("artifactId", gav.getArtifactId()),
                Map.entry("version", gav.getVersion())
                                   ));
        return config;
    }

    // =========================================================================
    // API
    // =========================================================================
    @Override
    public void process(final MavenProject project, final ConfigHandler<String, String> configuration) {
        final Neo4jDao dao       = new Neo4jDao(configuration);
        final Gav      gav       = convertMavenProjectToGav(project);
        final String   queryPath = QUERIES.get(0);
        final String query = TemplateRendering.render(QueriesLoader.getQuery(queryPath),
                                                      configure(queryPath,
                                                                gav,
                                                                configuration));
        log.info("query:\n{}", query);
        final List<Record> resultSet = dao.search(query);

        if (resultSet == null || resultSet.isEmpty()) {
            renderingNotResult();
        }
        else {
            rendering(resultSet);
        }


        dao.shutdown();
    }


    // =========================================================================
    // RENDERING
    // =========================================================================
    private void renderingNotResult() {
        log.info("no result");
    }

    private void rendering(final List<Record> resultSet) {
        final JsonBuilder writer = new JsonBuilder();

        final Map<String, List<PropertyDto>> properties = new HashMap();

        for (final Record record : resultSet) {
            final Map<String, Object> data     = record.asMap();
            final Node                artifact = (Node) data.get("artifact");
            final Node                property = (Node) data.get("property");

            mapResultSet(properties, artifact, property);
        }


        if (!properties.isEmpty()) {

            final List<String> artifactKeys = new ArrayList<>(properties.keySet());
            Collections.sort(artifactKeys);

            for (final String artifactName : artifactKeys) {
                writer.line();
                writer.write(ConsoleColors.CYAN);
                writer.write(ConsoleColors.createLine("-", 80)).line();
                writer.write(artifactName).line();
                writer.write(ConsoleColors.createLine("-", 80)).line();
                writer.write(ConsoleColors.RESET);

                final List<PropertyDto> propertyData = properties.get(artifactName);
                Collections.sort(propertyData, (value, ref) -> value.getName().compareTo(ref.getName()));

                for (final PropertyDto property : propertyData) {
                    renderProperty(property, writer);
                }
            }
        }

        log.info(writer.toString());
    }

    private void mapResultSet(final Map<String, List<PropertyDto>> properties, final Node artifact,
                              final Node property) {
        final String artifactName = artifact.get("name").asString();

        List<PropertyDto> artifactProperties = properties.get(artifactName);
        if (artifactProperties == null) {
            artifactProperties = new ArrayList<>();
            properties.put(artifactName, artifactProperties);

        }

        //@formatter:off
        artifactProperties.add(PropertyDto.builder()
                                          .name(property.get("name").asString())
                                          .type(property.get("propertyType").asString())
                                          .defaultValue(notNull(property.get("defaultValue")) ? property.get("defaultValue").asString() : null)
                                          .constraintType(notNull(property.get("constraintType")) ? property.get("constraintType").asString() : null)
                                          .constraintDetail(notNull(property.get("constraintDetail")) ? property.get("constraintDetail").asString() : null)
                                          .build());
        //@formatter:on
    }

    private void renderProperty(final PropertyDto property, final JsonBuilder writer) {

        if (property.isMandatory()) {
            writer.write(ConsoleColors.RED);
            writer.write("* ");
        }
        else {
            writer.write("  ");
        }
        writer.line();
        writer.write(property.getName());
        writer.write(" : ");
        writer.write(property.getType());

        if(property.getDefaultValue() != null){
            writer.write(" | default : ");
            writer.write(property.getDefaultValue());
        }



        if (property.getConstraintType() != null) {
            writer.line().write("\t\t|").write(property.getConstraintType());
        }
        if (property.getConstraintDetail() != null) {
            writer.line().write("\t\t|").write(property.getConstraintDetail());
        }
        writer.write(ConsoleColors.RESET);
    }

    private boolean isMandatory(final Node property) {
        boolean     result = false;
        final Value value  = property.get("mandatory");
        if (notNull(value)) {
            result = value.asBoolean();
        }
        return result;
    }

    private boolean notNull(final Value value) {
        return value != null && !"null".equals(value.asString());
    }

    @EqualsAndHashCode(onlyExplicitlyIncluded = true)
    @Getter
    @Builder
    private static class PropertyDto {
        private final boolean mandatory;
        @EqualsAndHashCode.Include
        private final String  name;
        private final String  type;
        private final String  defaultValue;
        private final String  constraintType;
        private final String  constraintDetail;
    }
}
