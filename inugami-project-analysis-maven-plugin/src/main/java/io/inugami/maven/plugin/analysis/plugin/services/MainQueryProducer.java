/* --------------------------------------------------------------------
 *  Inugami
 * --------------------------------------------------------------------
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package io.inugami.maven.plugin.analysis.plugin.services;

import io.inugami.maven.plugin.analysis.api.actions.QueryProducer;
import io.inugami.maven.plugin.analysis.api.models.QueryDefinition;

import java.util.List;

public class MainQueryProducer implements QueryProducer {

    public static final String QUERIES_SEARCH_SERVICES_REST_CQL = "META-INF/queries/search_services_rest.cql";
    public static final String QUERIES_SEARCH_CONSUMERS_CQL = "META-INF/queries/search_consumers.cql";
    public static final String QUERIES_SEARCH_PRODUCE_CQL = "META-INF/queries/search_produce.cql";
    public static final String QUERIES_SEARCH_PROPERTIES_CQL = "META-INF/queries/search_properties.cql";
    public static final String QUERIES_SEARCH_SERVICES_QUEUE_EXPOSE_CQL = "META-INF/queries/search_services_queue_expose.cql";
    public static final String QUERIES_SEARCH_SERVICES_QUEUE_CONSUME_CQL = "META-INF/queries/search_services_queue_consume.cql";
    public static final String QUERIES_SEARCH_ERRORS_CQL = "META-INF/queries/search_errors.cql";
    public static final String QUERIES_SEARCH_DEPLOY_ARTIFACT_CQL = "META-INF/queries/search_deploy_artifact.cql";
    public static final String QUERIES_SEARCH_MISSING_SERVICE_CQL = "META-INF/queries/search_missing_service.cql";
    public static final String QUERIES_SEARCH_PUBLISH_ARTIFACT_INFO_CQL = "META-INF/queries/search_publish_artifact_info.cql";
    public static final String QUERIES_SEARCH_ENV_INFO_CQL = "META-INF/queries/search_env_info.cql";
    public static final String QUERIES_SEARCH_RELEASE_NOTE_SIMPLE_CQL = "META-INF/queries/search_release_note_simple.cql";
    public static final String QUERIES_SEARCH_RELEASE_NOTE_FULL_CQL = "META-INF/queries/search_release_note_full.cql";
    public static final String QUERIES_SEARCH_DEPENDENCIES_CQL = "META-INF/queries/search_dependencies.cql";
    public static final String QUERIES_SEARCH_PROJECT_DEPENDENCIES_CQL = "META-INF/queries/search_dependencies_project.cql";

    public static final String QUERIES_SEARCH_ALL_EXPOSED_SERVICES = "META-INF/queries/search_all_exposed_services.cql";
    public static final String QUERIES_SEARCH_ALL_CONSUMED_SERVICES = "META-INF/queries/search_all_consumed_services.cql";
    public static final String QUERIES_SEARCH_ENTITIES = "META-INF/queries/search_entities.cql";
    public static final String QUERIES_SEARCH_FLYWAY = "META-INF/queries/search_flyway.cql";
    // =========================================================================
    // API
    // =========================================================================
    @Override
    public List<QueryDefinition> extractQueries() {
        return List.of(
                QueryDefinition.builder()
                               .path(QUERIES_SEARCH_SERVICES_REST_CQL)
                               .name("search_services_rest")
                               .type("cql")
                               .description(
                                       "Allow to search all rest services consume or expose by current project and these dependencies who expose/consume them")
                               .build(),

                QueryDefinition.builder()
                               .path(QUERIES_SEARCH_CONSUMERS_CQL)
                               .name("search_consumers")
                               .type("cql")
                               .description("Allow to search all consumed rest services")
                               .build(),

                QueryDefinition.builder()
                               .path(QUERIES_SEARCH_PRODUCE_CQL)
                               .name("search_produce")
                               .type("cql")
                               .description("Allow to search all exposed rest services")
                               .build(),

                QueryDefinition.builder()
                               .path(QUERIES_SEARCH_PROPERTIES_CQL)
                               .name("search_properties")
                               .type("cql")
                               .description("Allow to search all consumed properties")
                               .build(),

                QueryDefinition.builder()
                               .path(QUERIES_SEARCH_SERVICES_QUEUE_EXPOSE_CQL)
                               .name("search_queue")
                               .type("cql")
                               .description("Allow to search all produced queue")
                               .build(),

                QueryDefinition.builder()
                               .path(QUERIES_SEARCH_SERVICES_QUEUE_CONSUME_CQL)
                               .name("search_queue")
                               .type("cql")
                               .description("Allow to search all consumes queue")
                               .build(),

                QueryDefinition.builder()
                               .path(QUERIES_SEARCH_ERRORS_CQL)
                               .name("search_error_codes")
                               .type("cql")
                               .description("Allow to search all error codes")
                               .build(),

                QueryDefinition.builder()
                               .path(QUERIES_SEARCH_DEPLOY_ARTIFACT_CQL)
                               .name("search_version_deployments")
                               .type("cql")
                               .description("Allow to search environments where version have been deployed")
                               .build(),

                QueryDefinition.builder()
                               .path(QUERIES_SEARCH_MISSING_SERVICE_CQL)
                               .name("search_missing_service")
                               .type("cql")
                               .description("Allow to search missing services producers")
                               .build(),

                QueryDefinition.builder()
                               .path(QUERIES_SEARCH_PUBLISH_ARTIFACT_INFO_CQL)
                               .name("search_publish_artifact_info")
                               .type("cql")
                               .description("Allow to retrieve artifact publishing information")
                               .build(),

                QueryDefinition.builder()
                               .path(QUERIES_SEARCH_ENV_INFO_CQL)
                               .name("search_env_info")
                               .type("cql")
                               .description("Allow to retrieve deployed artifacts on environments")
                               .build(),

                QueryDefinition.builder()
                               .path(QUERIES_SEARCH_RELEASE_NOTE_SIMPLE_CQL)
                               .name("search_release_note_simple")
                               .type("cql")
                               .description("Allow to retrieve release note information")
                               .build(),

                QueryDefinition.builder()
                               .path(QUERIES_SEARCH_RELEASE_NOTE_FULL_CQL)
                               .name("search_release_note_simple")
                               .type("cql")
                               .description("Allow to retrieve full release note information")
                               .build(),


                QueryDefinition.builder()
                               .path(QUERIES_SEARCH_DEPENDENCIES_CQL)
                               .name("search_dependencies")
                               .type("cql")
                               .description("Allow to retrieve dependencies")
                               .build(),

                QueryDefinition.builder()
                               .path(QUERIES_SEARCH_PROJECT_DEPENDENCIES_CQL)
                               .name("search_project_dependencies")
                               .type("cql")
                               .description("Allow to retrieve project dependencies")
                               .build(),

                QueryDefinition.builder()
                               .path(QUERIES_SEARCH_ALL_EXPOSED_SERVICES)
                               .name("search_all_exposed_services")
                               .type("cql")
                               .description("Allow to retrieve all project exposed services")
                               .build(),

                QueryDefinition.builder()
                               .path(QUERIES_SEARCH_ALL_CONSUMED_SERVICES)
                               .name("search_all_consumed_services")
                               .type("cql")
                               .description("Allow to retrieve all project consumed services")
                               .build(),

                QueryDefinition.builder()
                               .path(QUERIES_SEARCH_ENTITIES)
                               .name("search_entities")
                               .type("cql")
                               .description("Allow to retrieve all project entities")
                               .build(),

                QueryDefinition.builder()
                              .path(QUERIES_SEARCH_FLYWAY)
                              .name("search_flyway")
                              .type("cql")
                              .description("Allow to retrieve all flyway scripts")
                              .build()
                      );
    }

}
