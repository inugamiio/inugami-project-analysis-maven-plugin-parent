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

import io.inugami.api.processors.ConfigHandler;
import io.inugami.configuration.services.ConfigHandlerHashMap;
import io.inugami.maven.plugin.analysis.api.models.Gav;
import io.inugami.maven.plugin.analysis.api.models.InfoContext;
import io.inugami.maven.plugin.analysis.api.services.info.release.note.ReleaseNoteExtractor;
import io.inugami.maven.plugin.analysis.api.services.info.release.note.models.ErrorCodeDTO;
import io.inugami.maven.plugin.analysis.api.services.info.release.note.models.ReleaseNoteResult;
import io.inugami.maven.plugin.analysis.api.services.info.release.note.models.Replacement;
import io.inugami.maven.plugin.analysis.api.services.neo4j.Neo4jDao;
import io.inugami.maven.plugin.analysis.api.tools.QueriesLoader;
import io.inugami.maven.plugin.analysis.api.tools.TemplateRendering;
import org.neo4j.driver.Record;
import org.neo4j.driver.internal.value.NodeValue;

import java.util.*;

import static io.inugami.maven.plugin.analysis.api.tools.Neo4jUtils.extractNode;
import static io.inugami.maven.plugin.analysis.api.utils.Constants.*;
import static io.inugami.maven.plugin.analysis.plugin.services.MainQueryProducer.QUERIES_SEARCH_ERRORS_CQL;

public class ErrorCodeExtractor implements ReleaseNoteExtractor {

    // =========================================================================
    // API
    // =========================================================================

    @Override
    public void extractInformation(final ReleaseNoteResult releaseNoteResult, final Gav currentVersion,
                                   final Gav previousVersion, final Neo4jDao dao, final List<Replacement> replacements,
                                   final InfoContext context) {

        final Set<ErrorCodeDTO> currentErrorCodes = searchErrorCode(currentVersion, context.getConfiguration(), dao);
        final Set<ErrorCodeDTO> previousErrorCodes = previousVersion == null ? null : searchErrorCode(previousVersion,
                                                                                                      context.getConfiguration(),
                                                                                                      dao);

        List<ErrorCodeDTO> newErrorCodes     = null;
        List<ErrorCodeDTO> deletedErrorCodes = null;
        List<ErrorCodeDTO> sameErrorCodes    = null;

        if (currentErrorCodes != null) {
            if (previousErrorCodes == null) {
                sameErrorCodes    = new ArrayList<>(currentErrorCodes);
                newErrorCodes     = new ArrayList<>();
                deletedErrorCodes = new ArrayList<>();
            }
            else {
                newErrorCodes     = resolveNewErrorCodes(currentErrorCodes, previousErrorCodes);
                deletedErrorCodes = resolveDeletedErrorCodes(currentErrorCodes, previousErrorCodes);
                sameErrorCodes    = resolveSameErrorCodes(currentErrorCodes, newErrorCodes);
            }
        }
        releaseNoteResult.getErrorCodes().addNewErrorCodes(newErrorCodes);
        releaseNoteResult.getErrorCodes().addDeletedErrorCodes(deletedErrorCodes);
        releaseNoteResult.getErrorCodes().addErrorCodes(sameErrorCodes);
    }


    private List<ErrorCodeDTO> resolveNewErrorCodes(final Set<ErrorCodeDTO> currentErrorCodes,
                                                    final Set<ErrorCodeDTO> previousErrorCodes) {
        final List<ErrorCodeDTO> result = new ArrayList<>();

        for (final ErrorCodeDTO errorCode : currentErrorCodes) {
            if (!previousErrorCodes.contains(errorCode)) {
                result.add(errorCode);
            }
        }

        return result;
    }

    private List<ErrorCodeDTO> resolveDeletedErrorCodes(final Set<ErrorCodeDTO> currentErrorCodes,
                                                        final Set<ErrorCodeDTO> previousErrorCodes) {
        final List<ErrorCodeDTO> result = new ArrayList<>();
        for (final ErrorCodeDTO errorCode : previousErrorCodes) {
            if (!currentErrorCodes.contains(errorCode)) {
                result.add(errorCode);
            }
        }
        return result;
    }

    private List<ErrorCodeDTO> resolveSameErrorCodes(final Set<ErrorCodeDTO> currentErrorCodes,
                                                     final List<ErrorCodeDTO> newErrorCodes) {
        final List<ErrorCodeDTO> result = new ArrayList<>();
        for (final ErrorCodeDTO errorCode : currentErrorCodes) {
            if (!newErrorCodes.contains(errorCode)) {
                result.add(errorCode);
            }
        }
        return result;
    }

    private Set<ErrorCodeDTO> searchErrorCode(final Gav gav, final ConfigHandler<String, String> configuration,
                                              final Neo4jDao dao) {
        Set<ErrorCodeDTO>                   result = null;
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

    private Set<ErrorCodeDTO> convertToModel(final List<Record> resultSet) {
        final Set<ErrorCodeDTO> result = new LinkedHashSet<>();
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
                                       .artifact(artifact == null ? null : retrieveString("name", artifact.asMap()))
                                       .build());
            }

        }
        return result;
    }

    private int extractStatusCode(final Map<String, Object> errorData) {
        final Object status = errorData.get("statusCode");
        return status instanceof Integer ? (Integer) status : 500;
    }


}
