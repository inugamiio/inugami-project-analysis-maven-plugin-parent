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
import io.inugami.api.processors.ConfigHandler;
import io.inugami.configuration.services.ConfigHandlerHashMap;
import io.inugami.maven.plugin.analysis.api.models.Gav;
import io.inugami.maven.plugin.analysis.api.models.InfoContext;
import io.inugami.maven.plugin.analysis.api.services.info.release.note.ReleaseNoteExtractor;
import io.inugami.maven.plugin.analysis.api.services.info.release.note.models.Differential;
import io.inugami.maven.plugin.analysis.api.services.info.release.note.models.ReleaseNoteResult;
import io.inugami.maven.plugin.analysis.api.services.info.release.note.models.Replacement;
import io.inugami.maven.plugin.analysis.api.services.neo4j.Neo4jDao;
import io.inugami.maven.plugin.analysis.api.tools.QueriesLoader;
import io.inugami.maven.plugin.analysis.api.tools.TemplateRendering;
import io.inugami.maven.plugin.analysis.plugin.services.info.release.note.models.ErrorCodeDTO;
import org.neo4j.driver.Record;
import org.neo4j.driver.internal.value.NodeValue;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.inugami.maven.plugin.analysis.api.constant.Constants.*;
import static io.inugami.maven.plugin.analysis.api.tools.Neo4jUtils.extractNode;
import static io.inugami.maven.plugin.analysis.plugin.services.MainQueryProducer.QUERIES_SEARCH_ERRORS_CQL;

@SuppressWarnings({"java:S6213"})
public class ErrorCodeExtractor implements ReleaseNoteExtractor {
    // =========================================================================
    // ATTRIBUTES
    // =========================================================================
    public static final String ERROR_CODES = "errorCodes";

    // =========================================================================
    // API
    // =========================================================================

    @Override
    public void extractInformation(final ReleaseNoteResult releaseNoteResult, final Gav currentVersion,
                                   final Gav previousVersion, final Neo4jDao dao, final List<Replacement> replacements,
                                   final InfoContext context) {

        final Set<JsonObject> currentErrorCodes = searchErrorCode(currentVersion, context.getConfiguration(), dao);
        final Set<JsonObject> previousErrorCodes = previousVersion == null ? null : searchErrorCode(previousVersion,
                                                                                                    context.getConfiguration(),
                                                                                                    dao);


        releaseNoteResult.addDifferential(ERROR_CODES, Differential.buildDifferential(currentErrorCodes, previousErrorCodes));
    }


    private Set<JsonObject> searchErrorCode(final Gav gav, final ConfigHandler<String, String> configuration,
                                            final Neo4jDao dao) {
        Set<JsonObject>                     result = null;
        final ConfigHandler<String, String> config = new ConfigHandlerHashMap(configuration);
        config.putAll(Map.ofEntries(
                Map.entry(GROUP_ID, gav.getGroupId()),
                Map.entry(ARTIFACT_ID, gav.getArtifactId()),
                Map.entry(VERSION, gav.getVersion())
        ));
        final String query = TemplateRendering.render(QueriesLoader.getQuery(QUERIES_SEARCH_ERRORS_CQL),
                                                      config);
        final List<Record> resultSet = dao.search(query);
        if (resultSet != null) {
            result = convertToModel(resultSet);
        }
        return result;
    }

    private Set<JsonObject> convertToModel(final List<Record> resultSet) {
        final Set<JsonObject> result = new LinkedHashSet<>();
        for (final Record record : resultSet) {
            final NodeValue error    = extractNode("error", record);
            final NodeValue artifact = extractNode("dependency", record);

            if (error != null) {
                final Map<String, Object> errorData  = error.asMap();
                final int                 statusCode = extractStatusCode(errorData);
                result.add(ErrorCodeDTO.builder()
                                       .errorCode(retrieveString("shortName", errorData))
                                       .message(retrieveString("message", errorData))
                                       .type(retrieveString("errorType", errorData))
                                       .statusCode(statusCode)

                                       .messageDetail(retrieveString("messageDetail", errorData))
                                       .payload(retrieveString("payload", errorData))
                                       .exploitationError(retrieveBoolean("exploitationError", errorData))
                                       .rollback(retrieveBoolean("rollback", errorData))
                                       .retryable(retrieveBoolean("retryable", errorData))
                                       .field(retrieveString("field", errorData))
                                       .url(retrieveString("url", errorData))
                                       .errorDomain(retrieveString("errorDomain", errorData))
                                       .errorSubDomain(retrieveString("errorSubDomain", errorData))

                                       .artifact(artifact == null ? null : retrieveString("name", artifact.asMap()))
                                       .build());
            }

        }
        return result;
    }


    private int extractStatusCode(final Map<String, Object> errorData) {
        Object status = errorData.get("errorStatus");
        if (status == null) {
            status = errorData.get("statusCode");
        }

        if (status == null) {
            return 500;
        } else if (status instanceof Integer) {
            return (Integer) status;
        } else if (status instanceof Long) {
            return ((Long) status).intValue();
        } else {
            return 500;
        }
    }


}
