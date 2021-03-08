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
package io.inugami.maven.plugin.analysis.api.exceptions;

import io.inugami.api.exceptions.ErrorCode;
import io.inugami.api.exceptions.UncheckedException;

public class ConfigurationException extends UncheckedException {


    private static final long serialVersionUID = 1727600084790540016L;

    public ConfigurationException() {
    }

    public ConfigurationException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public ConfigurationException(final String message) {
        super(message);
    }

    public ConfigurationException(final Throwable cause) {
        super(cause);
    }

    public ConfigurationException(final String message, final Object... values) {
        super(message, values);
    }

    public ConfigurationException(final Throwable cause, final String message, final Object... values) {
        super(cause, message, values);
    }

    public ConfigurationException(final ErrorCode errorCode, final String message) {
        super(errorCode, message);
    }

    public ConfigurationException(final ErrorCode errorCode, final Throwable cause, final String message,
                                  final Object... values) {
        super(errorCode, cause, message, values);
    }

}
