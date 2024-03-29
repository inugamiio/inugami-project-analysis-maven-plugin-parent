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
package io.inugami.maven.plugin.analysis.plugin.services.info.imports;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.inugami.api.exceptions.UncheckedException;
import io.inugami.api.processors.ConfigHandler;
import io.inugami.commons.files.FilesUtils;
import io.inugami.maven.plugin.analysis.api.actions.ProjectInformation;
import io.inugami.maven.plugin.analysis.api.models.Gav;
import io.inugami.maven.plugin.analysis.api.models.InfoContext;
import io.inugami.maven.plugin.analysis.api.models.ScanNeo4jResult;
import io.inugami.maven.plugin.analysis.api.tools.ConsoleTools;
import io.inugami.maven.plugin.analysis.api.tools.TemplateRendering;
import io.inugami.maven.plugin.analysis.plugin.services.writer.neo4j.Neo4jWriter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

@SuppressWarnings({"java:S2259"})
@Slf4j
public class ImportData implements ProjectInformation {

    // =========================================================================
    // ATTRIBUTES
    // =========================================================================
    public static final String QUERY_PATH = "inugami.query.path";

    // =========================================================================
    // PROCESS
    // =========================================================================
    @Override
    public void process(final InfoContext context) {


        final boolean interactive  = context.getConfiguration().grabBoolean("interactive");
        final Gav     gav          = convertMavenProjectToGav(context.getProject());
        final File    templatePath = loadQuery(context.getConfiguration(), interactive);
        FilesUtils.assertCanRead(templatePath);
        final boolean cypherScript = templatePath.getName().endsWith(".cql");

        final Map<String, String> properties = new LinkedHashMap<>(context.getConfiguration());
        properties.put("artifactId", gav.getArtifactId());
        properties.put("groupId", gav.getGroupId());
        properties.put("version", gav.getVersion());

        final String importScript = TemplateRendering.render(templatePath, properties);

        ScanNeo4jResult data = null;

        if (cypherScript) {
            data = ScanNeo4jResult.builder().build();
            data.addCreateScript(importScript);
        } else {
            data = readJson(importScript);
        }

        if (data == null) {
            log.warn("no data to import");
        } else {
            final Neo4jWriter neo4jWriter = new Neo4jWriter().init(context.getConfiguration());
            neo4jWriter.appendData(data);
            neo4jWriter.write();
            neo4jWriter.shutdown(null);
        }

    }


    // =========================================================================
    // API
    // =========================================================================
    private File loadQuery(final ConfigHandler<String, String> configuration, final boolean interactive) {
        File   result = null;
        String path   = configuration.get(QUERY_PATH);
        if (interactive || path == null) {
            path = ConsoleTools.askQuestion("Where is the import file?");
        }

        if (path != null) {
            result = new File(path);
        }
        return result;
    }

    // =========================================================================
    // JSON
    // =========================================================================
    private ScanNeo4jResult readJson(final String importScript) {
        final ObjectMapper objectMapper = buildObjectMapper();

        final ScanNeo4jResult result;
        try {
            result = objectMapper.readValue(importScript, new TypeReference<ScanNeo4jResult>() {
            });
        } catch (final JsonProcessingException e) {
            throw new UncheckedException(e.getMessage(), e);
        }

        return result;
    }

    private ObjectMapper buildObjectMapper() {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    // =========================================================================
    // GETTERS & SETTERS
    // =========================================================================
}
