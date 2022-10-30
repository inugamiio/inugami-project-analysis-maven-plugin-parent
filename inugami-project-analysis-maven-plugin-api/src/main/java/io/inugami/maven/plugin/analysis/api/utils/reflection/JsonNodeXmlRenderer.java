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
package io.inugami.maven.plugin.analysis.api.utils.reflection;

import io.inugami.api.models.JsonBuilder;
import io.inugami.maven.plugin.analysis.functional.CheckUtils;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static io.inugami.maven.plugin.analysis.api.utils.reflection.JsonNodeRendererUtils.buildIndentation;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class JsonNodeXmlRenderer {


    // =========================================================================
    // ATTRIBUTES
    // =========================================================================
    public static final String OPEN_TAG      = "<";
    public static final String TAG_CLOSE     = "/";
    public static final String CLOSE_TAG     = ">";
    public static final String OPEN_COMMENT  = "<!--";
    public static final String CLOSE_COMMENT = "-->";
    public static final String EMPTY         = "";

    // =========================================================================
    // API
    // =========================================================================
    public static String toXml(final JsonNode node, final int nbTab, final boolean strict) {
        final JsonBuilder xml = new JsonBuilder();

        if (node == null) {
            return xml.toString();
        }
        boolean shouldAddLine = false;


        if (node.getDescription() != null) {
            xml.write(renderDescription(node.getDescription(), nbTab, strict));
        }

        if (node.isList()) {
            xml.write(buildIndentation(nbTab));
            xml.write(OPEN_COMMENT);
            xml.write("multiple occurrence possible");
            xml.write(CLOSE_COMMENT);
            xml.line();
        }

        xml.write(buildIndentation(nbTab));
        xml.write(writeOpenTag(node.getFieldName()));
        if (node.isList() || node.isMap() || node.isStructure()) {
            shouldAddLine = true;
            xml.write(renderComplexType(node, nbTab, strict));
        }
        else {
            xml.write(renderSimpleType(node));
        }

        if (shouldAddLine) {
            xml.line();
            xml.write(buildIndentation(nbTab));
        }
        xml.write(writeCloseTag(node.getFieldName()));
        return xml.toString();
    }


    // =========================================================================
    // RENDERING
    // =========================================================================
    public static String renderRootDescription(final DescriptionDTO description,
                                               final boolean strict,
                                               final List<JsonNode> nodes,
                                               final boolean deprecated) {
        if (!strict || description == null) {
            return EMPTY;
        }

        Set<PotentialErrorDTO> potentialErrors = new LinkedHashSet<>();
        if (description.getPotentialErrors() != null) {
            potentialErrors.addAll(description.getPotentialErrors());
        }
        if (nodes != null) {
            potentialErrors.addAll(searchAllPotentialErrors(nodes));
        }

        final DescriptionDTO globalDescription = description.toBuilder()
                                                            .potentialErrors(new ArrayList<>(potentialErrors))
                                                            .build();

        return renderDescription(globalDescription, 0, strict, deprecated);
    }


    public static String renderDescription(final DescriptionDTO description, final int nbTab, final boolean strict) {
        return renderDescription(description, nbTab, strict, false);
    }

    public static String renderDescription(final DescriptionDTO description,
                                           final int nbTab,
                                           final boolean strict,
                                           final boolean deprecated) {
        final JsonBuilder xml = new JsonBuilder();

        if (!strict || description == null) {
            return xml.toString();
        }

        boolean shoudAddLine = false;

        xml.write(buildIndentation(nbTab));
        xml.write(OPEN_COMMENT);

        if (deprecated) {
            xml.write("[/!\\ DEPRECATED ]*********************************");
            xml.line();
        }
        if (description.getContent().contains("\n")) {
            xml.write(renderIndentedText(description.getContent(), nbTab));
            shoudAddLine = true;
        }
        else {
            xml.write(description.getContent());
        }


        if (CheckUtils.notEmpty(description.getExample())) {
            xml.line();
            xml.write(buildIndentation(nbTab)).write("Example:").line();
            xml.write(renderIndentedText(xmlEscape(description.getExample()), nbTab));
            shoudAddLine = true;
        }
        if (CheckUtils.notEmpty(description.getPotentialErrors())) {
            xml.line();
            xml.write(buildIndentation(nbTab + 1)).write("Potential errors :");
            for (PotentialErrorDTO potentialError : description.getPotentialErrors()) {
                xml.line();
                xml.write(buildIndentation(nbTab + 2));
                xml.write("* ")
                   .write("[").write(potentialError.getErrorCode()).write("] ");

                xml.write("[HTTP-");
                xml.write(potentialError.getHttpStatus());
                if (CheckUtils.notEmpty(potentialError.getType())) {
                    xml.write("_");
                    xml.write(potentialError.getType());
                }
                xml.write("] ");


                if (CheckUtils.notEmpty(potentialError.getErrorMessage())) {
                    xml.writeSpace();
                    xml.write(potentialError.getErrorMessage());
                }
                if (CheckUtils.notEmpty(potentialError.getDescription())) {
                    xml.write(": ");
                    xml.write(potentialError.getDescription());
                }
                if (CheckUtils.notEmpty(potentialError.getUrl())) {
                    xml.write(" ( see ");
                    xml.write(potentialError.getUrl());
                    xml.write(")");
                }
            }
            shoudAddLine = true;
        }

        if (shoudAddLine) {
            xml.line();
            xml.write(buildIndentation(nbTab));
        }
        xml.write(CLOSE_COMMENT);
        xml.line();
        return xml.toString();
    }


    private static String renderSimpleType(final JsonNode node) {
        final JsonBuilder xml = new JsonBuilder();
        xml.write(node.getType());
        return xml.toString();
    }

    private static String renderComplexType(final JsonNode node, final int nbTab, final boolean strict) {
        final JsonBuilder xml = new JsonBuilder();

        if (node.isList()) {
            xml.write(renderList(node, nbTab, strict));
        }
        else if (node.isStructure()) {
            xml.write(renderStructure(node, nbTab, strict));
        }

        return xml.toString();
    }


    private static String renderList(final JsonNode node, final int nbTab, final boolean strict) {
        final JsonBuilder xml = new JsonBuilder();

        if (node.getChildren().isEmpty()) {
            return xml.toString();
        }
        for (JsonNode child : node.getChildren().get(0).getChildren()) {
            xml.line();
            xml.write(toXml(child, nbTab + 1, strict));
        }

        return xml.toString();
    }

    private static String renderStructure(final JsonNode node, final int nbTab, final boolean strict) {
        final JsonBuilder xml = new JsonBuilder();

        for (JsonNode child : node.getChildren()) {
            xml.line();
            xml.write(toXml(child, nbTab + 1, strict));
        }

        return xml.toString();
    }


    // =========================================================================
    // TOOLS
    // =========================================================================
    private static String writeOpenTag(final String fieldName) {
        return new JsonBuilder()
                .write(OPEN_TAG)
                .write(fieldName)
                .write(CLOSE_TAG)
                .toString();
    }

    private static String writeCloseTag(final String fieldName) {
        return new JsonBuilder()
                .write(OPEN_TAG)
                .write(TAG_CLOSE)
                .write(fieldName)
                .write(CLOSE_TAG)
                .toString();
    }

    private static String xmlEscape(final String value) {
        return value == null ? null : value.replaceAll("<", "&lt;").replaceAll(">", "&gt;");
    }

    private static String renderIndentedText(final String text, final int nbTab) {
        final JsonBuilder xml   = new JsonBuilder();
        final String[]    lines = text.split("\n");

        for (String line : lines) {
            xml.line();
            xml.write(buildIndentation(nbTab));
            xml.write(line);
        }
        return xml.toString();
    }


    private static List<PotentialErrorDTO> searchAllPotentialErrors(final List<JsonNode> nodes) {
        List<PotentialErrorDTO> result = new ArrayList<>();
        if (nodes == null) {
            return result;
        }

        for (JsonNode node : nodes) {

            if (node.getChildren() != null) {
                result.addAll(searchAllPotentialErrors(node.getChildren()));
            }
            if (node.getDescription() != null && node.getDescription().getPotentialErrors() != null) {
                result.addAll(node.getDescription().getPotentialErrors());
            }
        }

        return result;
    }
}
