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
package io.inugami.maven.plugin.analysis.plugin.services.scan.flyway;

import io.inugami.api.models.data.basic.JsonObject;
import io.inugami.api.processors.ConfigHandler;
import io.inugami.commons.files.FilesUtils;
import io.inugami.commons.security.EncryptionUtils;
import io.inugami.maven.plugin.analysis.api.actions.ProjectScanner;
import io.inugami.maven.plugin.analysis.api.models.Node;
import io.inugami.maven.plugin.analysis.api.models.Relationship;
import io.inugami.maven.plugin.analysis.api.models.ScanConext;
import io.inugami.maven.plugin.analysis.api.models.ScanNeo4jResult;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

import static io.inugami.maven.plugin.analysis.api.tools.BuilderTools.buildNodeVersion;

@Slf4j
public class FlywayScan implements ProjectScanner {


    // =========================================================================
    // ATTRIBUTES
    // =========================================================================
    public static final String FEATURE_NAME        = "inugami.maven.plugin.analysis.analyzer.flyway";
    public static final String FEATURE_ENABLED     = FEATURE_NAME + ".enabled";
    public static final String SCRIPTS_PATHS       = FEATURE_NAME + ".paths";
    public static final String DEFAULT_DB          = FEATURE_NAME + ".defaultDb";
    public static final String SCRIPT_TYPES        = FEATURE_NAME + ".scriptTypes";
    public static final String SEPARATOR           = ";";
    public static final String DB_TYPE             = "dbType";
    public static final String NODE_FLYWAY         = "Flyway";
    public static final String NODE_FLYWAY_CONTENT = "FlywayContent";
    public static final String CONTENT             = "content";
    public static final String HAS_FLYWAY          = "HAS_FLYWAY";


    // =========================================================================
    // FEATURE TOGGLE
    // =========================================================================
    @Override
    public boolean enable(final ScanConext context) {
        return Boolean.parseBoolean(context.getConfiguration().grabOrDefault(FEATURE_ENABLED, "false"))
                && context.getConfiguration().containsKey(SCRIPTS_PATHS);
    }


    // =========================================================================
    // API
    // =========================================================================
    @Override
    public List<JsonObject> scan(final ScanConext context) {
        final ScanNeo4jResult result = ScanNeo4jResult.builder().build();
        final List<File>      paths  = loadPaths(context.getConfiguration());


        if (!paths.isEmpty()) {
            final String defaultDb = context.getConfiguration().grabOrDefault(DEFAULT_DB, null);
            final List<String> fileTypes = Arrays
                    .asList(context.getConfiguration().grabOrDefault(SCRIPT_TYPES, "sql").split(SEPARATOR))
                    .stream()
                    .map(String::trim)
                    .collect(Collectors.toList());

            final Node projectNode = buildNodeVersion(context.getProject());
            for (final File file : paths) {
                if (file.isDirectory()) {
                    final List<File> files = Arrays.asList(file.list())
                                                   .stream()
                                                   .map(fileName -> new File(
                                                     file.getAbsolutePath() + File.separator + fileName))
                                                   .collect(Collectors.toList());
                    scanFolder(defaultDb, file.getName(), files, fileTypes, result, projectNode);
                }
            }

        }


        return List.of(result);
    }

    private void scanFolder(final String defaultDb,
                            final String dbName,
                            final List<File> scripts,
                            final List<String> fileTypes,
                            final ScanNeo4jResult result,
                            final Node projectNode) {

        for (final File script : scripts) {
            final String extension = resolveExtension(script.getName());
            if (fileTypes.contains(extension)) {
                buildFlywayNode(script, dbName, defaultDb, result, projectNode);
            }
        }
    }


    private void buildFlywayNode(final File script,
                                 final String dbName,
                                 final String defaultDb,
                                 final ScanNeo4jResult result,
                                 final Node projectNode) {
        final String content     = loadScriptContent(script);
        final String contentSha1 = encodeSha1(content == null ? "" : content);
        final String uid         = encodeSha1(String.join(SEPARATOR, script.getName(), contentSha1));

        final LinkedHashMap<String, Serializable> flywayNodeProperties = new LinkedHashMap<>();
        flywayNodeProperties.put(DB_TYPE, defaultDb == null ? dbName : defaultDb);

        final Node flywayNode = Node.builder()
                                    .type(NODE_FLYWAY)
                                    .uid(uid)
                                    .name(script.getName())
                                    .properties(flywayNodeProperties)
                                    .build();
        result.addNode(flywayNode);
        result.addRelationship(Relationship.builder()
                                           .from(projectNode.getUid())
                                           .to(flywayNode.getUid())
                                           .type(HAS_FLYWAY)
                                           .build());
        if (content != null) {
            flywayNodeProperties.put(CONTENT, content);
            final Node flywayNodeContent = Node.builder()
                                               .type(NODE_FLYWAY_CONTENT)
                                               .uid(contentSha1)
                                               .name(contentSha1)
                                               .properties(flywayNodeProperties)
                                               .build();

            result.addNode(flywayNodeContent);
            result.addRelationship(Relationship.builder()
                                               .from(flywayNode.getUid())
                                               .to(flywayNodeContent.getUid())
                                               .type("HAS_FLYWAY_CONTENT")
                                               .build());
        }


    }


    private String loadScriptContent(final File script) {
        String result = null;
        try {
            result = FilesUtils.readContent(script);
        } catch (final IOException e) {
            log.error(e.getMessage(), e);
        }
        return result;
    }

    // =========================================================================
    // TOOLS
    // =========================================================================
    private List<File> loadPaths(final ConfigHandler<String, String> configuration) {
        return Arrays.asList(configuration.grab(SCRIPTS_PATHS).split(SEPARATOR))
                     .stream()
                     .map(String::trim)
                     .map(File::new)
                     .filter(File::exists)
                     .filter(File::canRead)
                     .collect(Collectors.toList());
    }

    protected String resolveExtension(final String name) {
        String result = null;
        if (name.contains(".")) {
            final int index = name.lastIndexOf(".");
            result = index < (name.length() - 1) ? name.substring(index + 1) : null;
        }
        return result;
    }

    private String encodeSha1(final String value) {
        return new EncryptionUtils().encodeSha1(value);
    }
}
