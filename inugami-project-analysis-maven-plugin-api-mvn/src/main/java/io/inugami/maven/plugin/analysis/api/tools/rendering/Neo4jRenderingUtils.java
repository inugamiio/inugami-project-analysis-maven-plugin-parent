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

import io.inugami.api.models.JsonBuilder;
import io.inugami.api.tools.ConsoleColors;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.neo4j.driver.Value;
import org.neo4j.driver.types.Node;

import java.io.Serializable;
import java.util.*;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Neo4jRenderingUtils {


    public static final String COLUMN_SEP = " | ";

    // =========================================================================
    // API
    // =========================================================================
    public static String rendering(final Map<String, Collection<DataRow>> data) {
        String result = "no result";
        if (data != null && !data.isEmpty()) {
            result = processRendering(data);
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

    private static String renderTable(final Collection<DataRow> data) {
        final JsonBuilder writer = new JsonBuilder();

        if (data == null || data.isEmpty()) {
            writer.write("no result").line();
        }
        else {
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

            for (final DataRow row : data) {
                if (row.getRowColor() != null) {
                    writer.write(row.getRowColor());
                }
                for (final Map.Entry<String, Serializable> rowData : row.getProperties().entrySet()) {
                    final String renderedValue = String.valueOf(rowData.getValue());
                    writer.write(renderedValue);
                    writer.write(
                            ConsoleColors.createLine(" ", columnsSize.get(rowData.getKey()) - renderedValue.length()));
                    writer.write(COLUMN_SEP);
                }
                writer.write(ConsoleColors.RESET);
                writer.line();
            }
        }
        return writer.toString();
    }

    private static Map<String, Integer> computeColumnSize(final Collection<DataRow> data) {
        final Map<String, Integer> result = new LinkedHashMap<>();

        if (data != null) {
            for (final DataRow item : data) {
                if (item != null) {
                    for (final Map.Entry<String, Serializable> entry : item.getProperties().entrySet()) {
                        final String  value      = String.valueOf(entry.getValue());
                        final Integer resultItem = result.get(entry.getKey());
                        if (resultItem == null || resultItem < value.length()) {
                            result.put(entry.getKey(),
                                       value.length() < entry.getKey().length() ? entry.getKey().length() : value
                                               .length());
                        }
                    }
                }
            }
        }

        return result;
    }


    // =========================================================================
    // TOOLS
    // =========================================================================
    public static List<String> orderKeys(final Collection<String> values) {
        final List<String> result = new ArrayList<>();
        if (values != null) {
            result.addAll(values);
            result.sort((ref, value) -> ref.compareTo(value));
        }
        return result;
    }

    // =========================================================================
    // TOOLS
    // =========================================================================
    public static String getNodeName(final Node node) {
        return retrieve("name", node);
    }

    public static String retrieve(final String key, final Node node) {
        String result = null;
        if (node != null) {
            final Value value = node.get(key);
            if (value != null && !value.isNull()) {
                result = value.asString();
            }
        }
        return result;
    }
}
