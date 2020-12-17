package io.inugami.maven.plugin.analysis.plugin.services.info.properties;

import io.inugami.api.models.JsonBuilder;
import io.inugami.api.processors.ConfigHandler;
import io.inugami.api.tools.ConsoleColors;
import io.inugami.configuration.services.ConfigHandlerHashMap;
import io.inugami.maven.plugin.analysis.api.actions.ProjectInformation;
import io.inugami.maven.plugin.analysis.api.actions.QueryConfigurator;
import io.inugami.maven.plugin.analysis.api.models.Gav;
import io.inugami.maven.plugin.analysis.api.models.InfoContext;
import io.inugami.maven.plugin.analysis.api.tools.QueriesLoader;
import io.inugami.maven.plugin.analysis.api.tools.TemplateRendering;
import io.inugami.maven.plugin.analysis.plugin.services.neo4j.Neo4jDao;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.Record;
import org.neo4j.driver.Value;
import org.neo4j.driver.types.Node;

import java.util.*;
import java.util.function.Function;

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
    public void process(final InfoContext context) {
        final Neo4jDao dao       = new Neo4jDao(context.getConfiguration());
        final Gav      gav       = convertMavenProjectToGav(context.getProject());
        final String   queryPath = QUERIES.get(0);
        final String query = TemplateRendering.render(QueriesLoader.getQuery(queryPath),
                                                      configure(queryPath,
                                                                gav,
                                                                context.getConfiguration()));
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

        final Map<String, List<PropertyDto>> properties = new LinkedHashMap<>();

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

                final int propertyColSize = searchMaxSize(propertyData, PropertyDto::getName);
                final int typeColSize     = searchMaxSize(propertyData, PropertyDto::getType);
                for (final PropertyDto property : propertyData) {
                    renderProperty(property, writer, propertyColSize, typeColSize);
                }
            }
        }

        log.info(writer.toString());
    }

    private int searchMaxSize(final List<PropertyDto> propertyData, final Function<PropertyDto, String> extractor) {
        int result = 0;
        for (final PropertyDto dto : propertyData) {
            final String value = extractor.apply(dto);
            if (value != null && value.length() > result) {
                result = value.length();
            }
        }
        return result;
    }

    private void mapResultSet(final Map<String, List<PropertyDto>> properties, final Node artifact,
                              final Node property) {
        if(artifact!=null){
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
                                              .mandatory(notNull(property.get("mandatory")) ?property.get("mandatory").asBoolean() : false)
                                              .constraintType(notNull(property.get("constraintType")) ? property.get("constraintType").asString() : null)
                                              .constraintDetail(notNull(property.get("constraintDetail")) ? property.get("constraintDetail").asString() : null)

                                              .useForConditionalBean(notNull(property.get("useForConditionalBean")) ?property.get("useForConditionalBean").asBoolean() : false)
                                              .matchIfMissing(notNull(property.get("matchIfMissing")) ?property.get("matchIfMissing").asBoolean() : false)
                                              .build());
            //@formatter:on
        }
    }

    private void renderProperty(final PropertyDto property, final JsonBuilder writer, final int propertyColSize,
                                final int typeColSize) {
        writer.line();

        if (property.isMandatory()) {
            writer.write(ConsoleColors.RED);
            writer.write("* ");
        }
        else if (property.useForConditionalBean) {
            writer.write(ConsoleColors.YELLOW);
            writer.write("! ");
        }
        else {
            writer.write("  ");
        }

        writer.write(property.getName());
        writer.write(ConsoleColors.createLine(" ", propertyColSize - property.getName().length()));

        writer.write(" | ");
        writer.write(property.getType());
        writer.write(ConsoleColors.createLine(" ", typeColSize - property.getType().length()));

        if (property.getDefaultValue() != null) {
            writer.write(" | default : ");
            writer.write(property.getDefaultValue());
        }else{
            writer.write(" | default : null");
        }

        final int detailTab = propertyColSize + typeColSize + 6;
        if (property.isUseForConditionalBean()) {
            writer.line()
                  .write(ConsoleColors.createLine(" ", detailTab))
                  .write("| use for conditional bean :").write("true");
        }
        if (property.isMatchIfMissing()) {
            writer.line()
                  .write(ConsoleColors.createLine(" ", detailTab))
                  .write("| match if missing : ").write(property.isMatchIfMissing());
        }
        if (property.getConstraintType() != null) {
            writer.line()
                  .write(ConsoleColors.createLine(" ", detailTab))
                  .write("| constraint :").write(property.getConstraintType());
        }
        if (property.getConstraintDetail() != null) {
            writer.line()
                  .write(ConsoleColors.createLine(" ", detailTab))
                  .write("| constraint detail :").write(property.getConstraintDetail());
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
        return value != null && !"null".equals(String.valueOf(value.asObject()));
    }

    @EqualsAndHashCode(onlyExplicitlyIncluded = true)
    @Getter
    @Builder
    private static class PropertyDto {
        private final boolean mandatory;
        private final boolean useForConditionalBean;
        private final boolean matchIfMissing;

        @EqualsAndHashCode.Include
        private final String name;
        private final String type;
        private final String defaultValue;
        private final String constraintType;
        private final String constraintDetail;
    }
}
