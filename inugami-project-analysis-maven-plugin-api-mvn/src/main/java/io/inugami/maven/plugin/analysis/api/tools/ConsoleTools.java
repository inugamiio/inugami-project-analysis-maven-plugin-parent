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
package io.inugami.maven.plugin.analysis.api.tools;

import io.inugami.api.models.JsonBuilder;
import io.inugami.api.tools.ConsoleColors;
import jline.console.ConsoleReader;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ConsoleTools {


    // =========================================================================
    // API
    // =========================================================================
    public static String askQuestion(final String question) {
        return askQuestion(question, null);
    }

    public static String askQuestion(final String question, final String defaultValue) {
        String result = null;
        try (final ConsoleReader consoleReader = new ConsoleReader()) {
            result = consoleReader.readLine(renderQuestion(question, defaultValue));
            if (result != null && result.trim().isEmpty()) {
                result = null;
            }
        } catch (final IOException error) {
            log.error(error.getMessage());
        }

        return result == null ? defaultValue : result.trim();
    }

    public static String askPassword(final String question, final String defaultValue) {
        String result = null;
        try (final ConsoleReader consoleReader = new ConsoleReader()) {
            result = consoleReader.readLine(renderQuestion(question, defaultValue), '*');
            
            if (result != null && result.trim().isEmpty()) {
                result = null;
            }
        } catch (final IOException error) {
            log.error(error.getMessage());
        }

        return result == null ? defaultValue : result.trim();
    }

    private static String renderQuestion(final String question, final String defaultValue) {
        final JsonBuilder result = new JsonBuilder().write(question);
        if (defaultValue != null) {
            result.write(ConsoleColors.CYAN)
                  .write("(").write(defaultValue).write(")").line()
                  .write(ConsoleColors.RESET);
        }
        return result.toString();
    }
}
