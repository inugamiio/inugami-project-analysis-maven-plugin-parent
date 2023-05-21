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
import io.inugami.maven.plugin.analysis.plugin.services.info.release.note.models.PropertyDTO;
import org.neo4j.driver.Record;
import org.neo4j.driver.Value;
import org.neo4j.driver.internal.value.NodeValue;
import org.neo4j.driver.types.Node;

import java.util.ArrayList;
import java.util.List;

import static io.inugami.maven.plugin.analysis.api.constant.Constants.*;
import static io.inugami.maven.plugin.analysis.api.tools.Neo4jUtils.*;
import static io.inugami.maven.plugin.analysis.plugin.services.MainQueryProducer.QUERIES_SEARCH_PROPERTIES_CQL;

@SuppressWarnings({"java:S6213"})
public class PropertiesExtractor implements ReleaseNoteExtractor {

    // =========================================================================
    // ATTRIBUTES
    // =========================================================================
    public static final String TYPE                     = "properties";
    public static final String ARTIFACT                 = "artifact";
    public static final String PROPERTY                 = "property";
    public static final String USE_FOR_CONDITIONAL_BEAN = "useForConditionalBean";
    public static final String MANDATORY                = "mandatory";
    public static final String CONSTRAINT_DETAIL        = "constraintDetail";
    public static final String CONSTRAINT_TYPE          = "constraintType";
    public static final String PROPERTY_TYPE            = "propertyType";
    public static final String DEFAULT_VALUE            = "defaultValue";


    // =========================================================================
    // API
    // =========================================================================
    @Override
    public void extractInformation(final ReleaseNoteResult releaseNoteResult, final Gav currentVersion,
                                   final Gav previousVersion, final Neo4jDao dao, final List<Replacement> replacements,
                                   final InfoContext context) {

        final List<JsonObject> currentErrorCodes = search(QUERIES_SEARCH_PROPERTIES_CQL, currentVersion,
                                                          context.getConfiguration(), dao, this::convert);

        final List<JsonObject> previousErrorCodes = previousVersion == null ? null : search(
                QUERIES_SEARCH_PROPERTIES_CQL,
                previousVersion,
                context.getConfiguration(),
                dao,
                this::convert);

        releaseNoteResult.addDifferential(TYPE, Differential
                .buildDifferential(newSet(currentErrorCodes), newSet(previousErrorCodes)));
    }


    // =========================================================================
    // OVERRIDES
    // =========================================================================
    private List<JsonObject> convert(final List<Record> resultSet) {
        final List<JsonObject> result = new ArrayList<>();

        for (final Record record : resultSet) {
            final NodeValue property = extractNode(PROPERTY, record);
            if (isNotNull(property)) {
                final Node propertyNode = property.asNode();
                result.add(PropertyDTO.builder()
                                      .artifact(buildArtifact(extractNode(ARTIFACT, record)))
                                      .name(retrieve(NAME, propertyNode))
                                      .defaultValue(retrieve(DEFAULT_VALUE, propertyNode))
                                      .propertyType(retrieve(PROPERTY_TYPE, propertyNode))
                                      .constraintType(retrieve(CONSTRAINT_TYPE, propertyNode))
                                      .constraintDetail(retrieve(CONSTRAINT_DETAIL, propertyNode))
                                      .mandatory(retrieveBoolean(property.get(MANDATORY)))
                                      .useForConditionalBean(retrieveBoolean(property.get(USE_FOR_CONDITIONAL_BEAN)))
                                      .build());
            }


        }
        return result;
    }

    private boolean retrieveBoolean(final Value value) {
        boolean result = false;
        if (value != null && !value.isNull()) {
            try {
                result = value.asBoolean(false);
            } catch (final Exception e) {
                //nothing
            }

        }
        return result;
    }

    private String buildArtifact(final NodeValue artifact) {
        String result = null;
        if (artifact != null && !artifact.isNull()) {
            final Node node = artifact.asNode();
            result = String.join(SPACE,
                                 retrieve(GROUP_ID, node),
                                 retrieve(ARTIFACT_ID, node));
        }

        return result;
    }


}
