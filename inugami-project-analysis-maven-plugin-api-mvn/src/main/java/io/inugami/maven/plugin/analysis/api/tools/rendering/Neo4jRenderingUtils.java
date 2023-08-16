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
package io.inugami.maven.plugin.analysis.api.tools.rendering;

import io.inugami.api.loggers.Loggers;
import io.inugami.api.models.JsonBuilder;
import io.inugami.api.processors.ConfigHandler;
import io.inugami.api.tools.ConsoleColors;
import io.inugami.commons.files.FilesUtils;
import io.inugami.maven.plugin.analysis.api.tools.Neo4jUtils;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.neo4j.driver.types.Node;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.function.Consumer;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Neo4jRenderingUtils {

    // =========================================================================
    // ATTRIBUTES
    // =========================================================================
    public static final String COLUMN_SEP = " | ";
    public static final String CSV_SEP    = ";";

    // =========================================================================
    // API
    // =========================================================================
    public static String rendering(final Map<String, Collection<DataRow>> data,
                                   final ConfigHandler<String, String> configuration,
                                   final String context) {
        String result = "no result";
        if (data != null && !data.isEmpty()) {
            result = processRendering(data);
            try {
                processExport(data, configuration, context);
            } catch (final Exception error) {
                Loggers.DEBUG.error(error.getMessage(), error);
            }


        }
        return result;
    }

    private static String processRendering(final Map<String, Collection<DataRow>> data) {
        final JsonBuilder writer = new JsonBuilder();

        final List<String> keys = orderKeys(data.keySet());

        for (final String key : keys) {
            writer.line();
            writer.write(ConsoleColors.CYAN);
            writer.write(ConsoleColors.createLine("-", 80)).line();
            writer.write(key).line();
            writer.write(ConsoleColors.createLine("-", 80)).line();
            writer.write(ConsoleColors.RESET);
            final Collection<DataRow> groupData = data.get(key);
            writer.write(renderTable(groupData));
        }

        return writer.toString();
    }

    private static String renderTable(final Collection<DataRow> inputData) {
        final JsonBuilder writer = new JsonBuilder();

        if (inputData == null || inputData.isEmpty()) {
            writer.write("no result").line();
        } else {
            final List<DataRow> data = new ArrayList<>(inputData);
            data.sort((ref, value) -> {
                final String refUid   = ref == null || ref.getUid() == null ? "null" : ref.getUid();
                final String valueUid = value == null || value.getUid() == null ? "null" : value.getUid();
                return refUid.compareTo(valueUid);
            });
            final Map<String, Integer> columnsSize = computeColumnSize(data);
            int displaySize = columnsSize.entrySet().stream().mapToInt(Map.Entry::getValue)
                                         .sum();
            displaySize = displaySize + (columnsSize.keySet().size() * 3) - 1;


            for (final Map.Entry<String, Integer> header : columnsSize.entrySet()) {
                writer.write(header.getKey());
                writer.write(ConsoleColors.createLine(" ", header.getValue() - header.getKey().length()));
                writer.write(COLUMN_SEP);
            }
            writer.line();
            writer.write(ConsoleColors.createLine("-", displaySize));
            writer.line();

            renderRows(writer, data, columnsSize);
        }
        return writer.toString();
    }

    private static void renderRows(final JsonBuilder writer, final List<DataRow> data, final Map<String, Integer> columnsSize) {
        for (final DataRow row : data) {
            if (row.getRowColor() != null) {
                writer.write(row.getRowColor());
            }
            int cursor = 0;
            for (final Map.Entry<String, Integer> header : columnsSize.entrySet()) {
                cursor = renderRow(writer, columnsSize, row, cursor, header);
            }
            writer.write(ConsoleColors.RESET);
            writer.line();
        }
    }

    private static int renderRow(final JsonBuilder writer, final Map<String, Integer> columnsSize, final DataRow row, int cursor, final Map.Entry<String, Integer> header) {
        final Serializable value = row.getProperties().get(header.getKey());
        if (value == null) {
            writer.write(ConsoleColors.createLine(" ", header.getValue()));
            writer.write(COLUMN_SEP);
        } else {
            final String renderedValue = processRenderingValue(value, cursor,
                                                               columnsSize.get(header.getKey()));
            writer.write(renderedValue);
            writer.write(ConsoleColors.createLine(" ", columnsSize.get(header.getKey()) - renderedValue
                    .length()));
            writer.write(COLUMN_SEP);
        }
        cursor += columnsSize.get(header.getKey()) + COLUMN_SEP.length();
        return cursor;
    }

    private static String processRenderingValue(final Serializable value, final int tab, final int columnSize) {
        String result = String.valueOf(value);
        if (result.contains("\n")) {
            result = String.join("\n" + ConsoleColors.createLine(" ", tab), result.split("\n"))
                    + "\n"
                    + ConsoleColors.createLine(" ", tab + columnSize);
        }
        return result;
    }

    private static Map<String, Integer> computeColumnSize(final Collection<DataRow> data) {
        final Map<String, Integer> result = new LinkedHashMap<>();

        if (data != null) {
            for (final DataRow item : data) {
                if (item != null) {
                    processComputeColumnSize(result, item);
                }
            }
        }

        return result;
    }

    private static void processComputeColumnSize(final Map<String, Integer> result, final DataRow item) {
        for (final Map.Entry<String, Serializable> entry : item.getProperties().entrySet()) {
            final String  value        = String.valueOf(entry.getValue());
            final int     maxValueSize = computeMaxValueSize(value);
            final Integer resultItem   = result.get(entry.getKey());
            final int     valueLength  = value == null ? 0 : value.length();
            if (resultItem == null || resultItem < maxValueSize) {
                result.put(entry.getKey(),
                           valueLength < entry.getKey().length() ? entry.getKey()
                                                                        .length() : maxValueSize);
            }
        }
    }

    private static int computeMaxValueSize(final String value) {
        int result = value == null ? 0 : value.length();
        if (value != null && value.contains("\n")) {
            result = 0;
            for (final String item : value.split("\n")) {
                if (item.length() > result) {
                    result = item.length();
                }
            }
        }
        return result;
    }


    // =========================================================================
    // EXPORT
    // =========================================================================
    private static void processExport(final Map<String, Collection<DataRow>> data,
                                      final ConfigHandler<String, String> configuration,
                                      final String context) {

        if (data == null || data.isEmpty() || !Boolean.parseBoolean(configuration.grabOrDefault("export", "false"))) {
            return;
        }

        final String buildDir  = configuration.get("project.build.directory");
        File         targetDir = null;
        if (buildDir != null) {
            targetDir = FilesUtils.buildFile(new File(buildDir), "inugami");
            targetDir.mkdirs();
        }

        for (final Map.Entry<String, Collection<DataRow>> entry : data.entrySet()) {
            writeCsvFile(entry.getKey(), entry.getValue(), context, targetDir);
        }
    }

    private static void writeCsvFile(final String key, final Collection<DataRow> values, final String context,
                                     final File targetDir) {
        if (values == null || values.isEmpty()) {
            return;
        }
        final List<DataRow> data = new ArrayList<>(values);
        final String filePath = new StringBuilder().append(targetDir.getAbsolutePath())
                                                   .append(File.separator)
                                                   .append(context)
                                                   .append("_")
                                                   .append(cleanFileName(key))
                                                   .append(".csv")
                                                   .toString();
        final File file = new File(filePath);

        final JsonBuilder csv     = new JsonBuilder();
        final Set<String> headers = writeHeaders(data, csv);

        final Iterator<DataRow> iterator = data.iterator();
        while (iterator.hasNext()) {
            final DataRow row = iterator.next();
            writeRow(row, headers, csv);
            if (iterator.hasNext()) {
                csv.line();
            }
        }
        file.getParentFile().mkdirs();
        try (final FileWriter writer = new FileWriter(file)) {
            writer.write(csv.toString());
            writer.flush();
            Loggers.IO.info("write file : {}", file.getAbsolutePath());
        } catch (final IOException e) {
            Loggers.DEBUG.error(e.getMessage(), e);
        }
    }

    private static String cleanFileName(final String value) {
        return value.replace(" ", "").replace(":", "_");
    }


    private static Set<String> writeHeaders(final Collection<DataRow> data, final JsonBuilder csv) {
        final Map<String, Integer> columnsSize = computeColumnSize(data);

        final Iterator<String> iterator = columnsSize.keySet().iterator();
        while (iterator.hasNext()) {
            final String header = iterator.next();
            csv.valueQuot(header);
            if (iterator.hasNext()) {
                csv.write(CSV_SEP);
            }
        }
        csv.line();
        return columnsSize.keySet();
    }

    private static void writeRow(final DataRow row, final Set<String> headers,
                                 final JsonBuilder csv) {
        if (row.getProperties() != null) {
            final Iterator<String> iterator = headers.iterator();
            while (iterator.hasNext()) {
                final String header = iterator.next();
                csv.valueQuot(cleanCsvValue(row.getProperties().get(header)));
                if (iterator.hasNext()) {
                    csv.write(CSV_SEP);
                }
            }
        }
    }

    private static String cleanCsvValue(final Serializable serializable) {
        if (serializable == null) {
            return "";
        }

        final String value = String.valueOf(serializable);
        return value.replace("\n", "")
                    .replace("\t", " ")
                    .replace("\"", "\\\"");
    }

    // =========================================================================
    // TOOLS
    // =========================================================================
    public static List<String> orderKeys(final Collection<String> values) {
        final List<String> result = new ArrayList<>();
        if (values != null) {
            result.addAll(values);
            result.sort(Comparable::compareTo);
        }
        return result;
    }

    // =========================================================================
    // TOOLS
    // =========================================================================
    public static String getNodeName(final Object node) {
        return Neo4jUtils.getNodeName(node);
    }

    public static Node getNode(final Object node) {
        return Neo4jUtils.getNode(node);
    }

    public static String retrieve(final String key, final Node node) {
        return Neo4jUtils.retrieve(key, node);
    }

    public static void ifPropertyNotNull(final String key, final Node node, final Consumer<Object> consumer) {
        Neo4jUtils.ifPropertyNotNull(key, node, consumer);
    }
}
