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

import io.inugami.maven.plugin.analysis.functional.ActionWithThrowable;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ReflexionServiceUtils {


    // =========================================================================
    // API
    // =========================================================================
    public static void setAccessible(final Method method) {
        if (method != null) {
            try {
                method.setAccessible(true);
            }
            catch (Throwable e) {
                traceError(e, log);
            }
        }
    }

    public static void setAccessible(Field field) {
        if (field != null) {
            try {
                field.setAccessible(true);
            }
            catch (Throwable e) {
                traceError(e, log);
            }
        }
    }

    // =========================================================================
    // API CONVERTORS
    // =========================================================================
    public static int parseInt(final Object value) {
        int result = 500;
        try {
            result = (int) value;
        }
        catch (Throwable e) {
            traceError(e, log);
        }
        return result;
    }

    // =========================================================================
    // ERRORS
    // =========================================================================
    public static <T> T runSafe(final ActionWithThrowable action) {
        if (action == null) {
            return null;
        }

        try {
            return (T) action.process();
        }
        catch (Throwable e) {
            traceError(e, log);
            return null;
        }
    }

    public static void traceError(final Throwable e, Logger logger) {
        if (logger.isDebugEnabled()) {
            if (e instanceof ClassNotFoundException) {
                logger.error(e.getMessage());
            }
            else {
                logger.error(e.getMessage(), e);
            }

        }
    }
}
