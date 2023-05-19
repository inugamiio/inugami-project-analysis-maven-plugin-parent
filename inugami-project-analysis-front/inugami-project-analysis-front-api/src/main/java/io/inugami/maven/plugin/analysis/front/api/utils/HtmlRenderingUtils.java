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
package io.inugami.maven.plugin.analysis.front.api.utils;

import io.inugami.api.models.JsonBuilder;
import io.inugami.maven.plugin.analysis.front.api.models.HtmlAttribute;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.inugami.maven.plugin.analysis.front.api.RenderingConstants.*;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class HtmlRenderingUtils {
    public static Pattern XML_ATTRIBUTE = Pattern.compile(
            "(\\s*((?:(?:[a-zA-Z0-9-]+:){0,1}[a-zA-Z0-9-]+)\\s*=\\s*\\\"([^\\\"]+)\\\"))");

    // =========================================================================
    // API
    // =========================================================================
    public static String tag(final String tagName,
                             final Supplier<String> appender,
                             final HtmlAttribute... attributes) {
        final JsonBuilder result = new JsonBuilder();
        if (result == null || tagName == null) {
            return result.toString();
        }
        result.write(TAG_OPEN).append(tagName);

        if (attributes.length > 0) {
            for (final HtmlAttribute attribute : attributes) {
                result.write(SPACE);
                result.write(attribute.getName());
                result.write(EQUALS);
                result.valueQuot(attribute.getValue());
            }
        }
        result.write(TAG_CLOSE);
        if (appender != null) {
            result.write(appender.get());
        }

        return result.write(TAG_OPEN_CLOSABLE)
                     .write(tagName)
                     .write(TAG_CLOSE)
                     .line()
                     .toString();
    }

    public static String autoClosableTag(final String tagName,
                                         final HtmlAttribute... attributes) {
        final JsonBuilder result = new JsonBuilder();
        if (tagName == null) {
            return result.toString();
        }
        result.write(TAG_OPEN).append(tagName);

        if (attributes.length > 0) {
            for (final HtmlAttribute attribute : attributes) {
                result.write(SPACE);
                result.write(attribute.getName());
                result.write(EQUALS);
                result.valueQuot(attribute.getValue());
            }
        }
        result.write(TAG_AUTO_CLOSABLE);
        result.line();
        return result.toString();
    }

    public static String openTag(final String tagName,
                                 final HtmlAttribute... attributes) {

        final JsonBuilder result = new JsonBuilder();
        if (tagName == null) {
            return result.toString();
        }
        result.write(TAG_OPEN).append(tagName);

        if (attributes.length > 0) {
            for (final HtmlAttribute attribute : attributes) {
                result.write(SPACE);
                result.write(attribute.getName());
                result.write(EQUALS);
                result.valueQuot(attribute.getValue());
            }
        }
        result.write(TAG_CLOSE);
        result.line();
        return result.toString();
    }

    public static String closeTag(final String tagName) {
        return new JsonBuilder()
                .write(TAG_OPEN_CLOSABLE)
                .write(tagName)
                .write(TAG_CLOSE)
                .line()
                .toString();
    }

    public static String buildPath(final String contextPath, final String resources) {
        return contextPath + (resources.startsWith(PATH_SEP, 0) ? resources : PATH_SEP + resources);
    }

    public static String loadResource(final String resourcePath) {
        String result = null;
        final InputStream inputStream = HtmlRenderingUtils.class.getClassLoader()
                                                                .getResourceAsStream(resourcePath);
        if (inputStream != null) {
            final StringBuilder resultStringBuilder = new StringBuilder();
            try (final BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
                String line;
                while ((line = br.readLine()) != null) {
                    resultStringBuilder.append(line).append("\n");
                }
            } catch (final IOException e) {
                log.error(e.getMessage(), e);
            }

            result = resultStringBuilder.toString();
        }

        return result;
    }

    public static String loadSvg(final String resourcePath, final int width, final int height) {
        final String   content = loadResource(resourcePath);
        final String[] lines   = content == null ? new String[]{} : content.split(">");

        final List<String> buffer = new ArrayList<>();
        for (int i = 0; i < lines.length; i++) {
            final String line       = lines[i];
            String       newContent = replaceSize(line, width, height);
            if (line.length() < 120) {
                newContent = line;
            } else {
                final Matcher matcher = XML_ATTRIBUTE.matcher(newContent);
                if (matcher.find()) {
                    newContent = splitXmlLine(newContent, matcher);
                } else {
                    newContent = String.join("\n", newContent.split(" "));
                }
            }

            if (!isBanXmlContent(newContent)) {
                if (i == lines.length) {
                    log.info("last");
                }
                buffer.add(newContent);
            }
        }


        return (String.join(">", buffer) + ">").replace("\n\n", "\n");
    }

    private static String replaceSize(final String line, final int width, final int height) {
        String content = line;
        if (line.contains("<svg") || line.contains("<SVG")) {
            final Matcher matcher = XML_ATTRIBUTE.matcher(line);
            if (matcher.find()) {
                content = splitXmlLine(line, matcher,
                                       ReplacementStrategy.builder()
                                                          .filter(l -> l.contains("width"))
                                                          .replacement(s -> "width=\"" + width + "\"")
                                                          .build(),
                                       ReplacementStrategy.builder()
                                                          .filter(l -> l.contains("height"))
                                                          .replacement(s -> "height=\"" + height + "\"")
                                                          .build(),
                                       ReplacementStrategy.builder()
                                                          .filter(l -> l.contains("viewBox"))
                                                          .replacement(s -> "")
                                                          .build(),
                                       ReplacementStrategy.builder()
                                                          .filter(l -> l.contains("inkscape:"))
                                                          .replacement(s -> "")
                                                          .build()
                )
                        .replace("\n", " ");
            }
        }
        return content;
    }


    private static boolean isBanXmlContent(final String content) {
        return content == null || content.trim().isEmpty() || content.contains("<?xml") || content.contains("<?XML");
    }

    private static String splitXmlLine(final String line, final Matcher matcher) {
        return splitXmlLine(line, matcher, null, null);
    }

    private static String splitXmlLine(final String line, final Matcher matcher,
                                       final ReplacementStrategy... replacementStrategies) {
        final StringBuilder result     = new StringBuilder();
        int                 cursor     = 0;
        boolean             beginAdded = false;

        while (matcher.find(cursor)) {
            final int start = matcher.start();
            final int end   = matcher.end();

            if (!beginAdded) {
                result.append(line.substring(cursor, start)).append("\n");
                beginAdded = true;
            }

            String                    content     = line.substring(start, end);
            final ReplacementStrategy replacement = resolveReplacement(content, replacementStrategies);
            if (replacement != null) {
                content = replacement.getReplacement().apply(content);
            }

            if (!content.trim().isEmpty()) {
                result.append(content).append("\n");
            }
            cursor = end;
        }

        if (cursor < line.length()) {
            result.append(line.substring(cursor));
        }
        return result.toString();
    }

    private static ReplacementStrategy resolveReplacement(final String content,
                                                          final ReplacementStrategy[] replacementStrategies) {
        ReplacementStrategy result = null;
        if (content != null && replacementStrategies != null && replacementStrategies.length > 0) {
            for (final ReplacementStrategy strategy : replacementStrategies) {
                if (strategy != null && strategy.getFilter() != null && strategy.getFilter()
                                                                                .test(content) && strategy.getReplacement() != null) {
                    result = strategy;
                    break;
                }
            }
        }
        return result;
    }


    @Builder
    @Getter
    @RequiredArgsConstructor
    private static class ReplacementStrategy {
        private final Function<String, String> replacement;
        private final Predicate<String>        filter;
    }
}
