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
package io.inugami.maven.plugin.analysis.plugin.services.info.release.note.extractors;

import io.inugami.api.models.data.basic.JsonObject;
import io.inugami.maven.plugin.analysis.api.models.Gav;
import io.inugami.maven.plugin.analysis.api.models.InfoContext;
import io.inugami.maven.plugin.analysis.api.services.info.release.note.ReleaseNoteExtractor;
import io.inugami.maven.plugin.analysis.api.services.info.release.note.models.Differential;
import io.inugami.maven.plugin.analysis.api.services.info.release.note.models.ReleaseNoteResult;
import io.inugami.maven.plugin.analysis.api.services.info.release.note.models.Replacement;
import io.inugami.maven.plugin.analysis.api.services.neo4j.Neo4jDao;
import org.neo4j.driver.Record;
import org.neo4j.driver.internal.value.NodeValue;
import org.neo4j.driver.types.Node;

import java.util.ArrayList;
import java.util.List;

import static io.inugami.maven.plugin.analysis.api.constant.Constants.*;
import static io.inugami.maven.plugin.analysis.api.tools.Neo4jUtils.*;
import static io.inugami.maven.plugin.analysis.plugin.services.MainQueryProducer.QUERIES_SEARCH_DEPENDENCIES_CQL;
import static io.inugami.maven.plugin.analysis.plugin.services.MainQueryProducer.QUERIES_SEARCH_PROJECT_DEPENDENCIES_CQL;

public class DependenciesExtractor implements ReleaseNoteExtractor {

    // =========================================================================
    // ATTRIBUTES
    // =========================================================================
    public static final String TYPE         = "dependencies";
    public static final String TYPE_PROJECT = "dependencies_project";
    public static final String PACKAGING    = "packaging";

    // =========================================================================
    // API
    // =========================================================================
    @Override
    public void extractInformation(final ReleaseNoteResult releaseNoteResult, final Gav currentVersion,
                                   final Gav previousVersion, final Neo4jDao dao, final List<Replacement> replacements,
                                   final InfoContext context) {

        extractDependencies(releaseNoteResult, currentVersion, previousVersion, dao, context);
        extractProjectDependencies(releaseNoteResult, currentVersion, previousVersion, dao, context);
    }

    private void extractProjectDependencies(final ReleaseNoteResult releaseNoteResult, final Gav currentVersion,
                                            final Gav previousVersion, final Neo4jDao dao, final InfoContext context) {
        final List<JsonObject> currentValues = search(QUERIES_SEARCH_PROJECT_DEPENDENCIES_CQL, currentVersion,
                                                      context.getConfiguration(), dao, this::convert);

        final List<JsonObject> previousValues = previousVersion == null ? null : search(
                QUERIES_SEARCH_PROJECT_DEPENDENCIES_CQL,
                previousVersion,
                context.getConfiguration(),
                dao,
                this::convert);


        releaseNoteResult.addDifferential(TYPE_PROJECT, Differential
                .buildDifferential(newSet(currentValues), newSet(previousValues)));
    }

    private void extractDependencies(final ReleaseNoteResult releaseNoteResult, final Gav currentVersion,
                                     final Gav previousVersion, final Neo4jDao dao, final InfoContext context) {
        final List<JsonObject> currentValues = search(QUERIES_SEARCH_DEPENDENCIES_CQL, currentVersion,
                                                      context.getConfiguration(), dao, this::convert);

        final List<JsonObject> previousValues = previousVersion == null ? null : search(
                QUERIES_SEARCH_DEPENDENCIES_CQL,
                previousVersion,
                context.getConfiguration(),
                dao,
                this::convert);


        releaseNoteResult.addDifferential(TYPE, Differential
                .buildDifferential(newSet(currentValues), newSet(previousValues)));
    }


    // =========================================================================
    // OVERRIDES
    // =========================================================================
    private List<JsonObject> convert(final List<Record> resultSet) {
        final List<JsonObject> result = new ArrayList<>();

        for (final Record record : resultSet) {
            final NodeValue property = extractNode("dependency", record);
            if (isNotNull(property)) {
                final Node propertyNode = property.asNode();
                result.add(Gav.builder()
                              .groupId(retrieve(GROUP_ID, propertyNode))
                              .artifactId(retrieve(ARTIFACT_ID, propertyNode))
                              .version(retrieve(VERSION, propertyNode))
                              .type(retrieve(PACKAGING, propertyNode))
                              .build()
                );
            }
        }
        return result;
    }

}
