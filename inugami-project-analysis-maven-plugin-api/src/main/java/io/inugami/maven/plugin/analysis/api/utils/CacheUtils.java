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
package io.inugami.maven.plugin.analysis.api.utils;

import io.inugami.api.loggers.Loggers;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.ehcache.impl.internal.concurrent.ConcurrentHashMap;
import org.ehcache.spi.loaderwriter.CacheWritingException;

import java.util.Map;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CacheUtils {

    // =========================================================================
    // ATTRIBUTES
    // =========================================================================
    private static final Map<String, Object> CACHES = new ConcurrentHashMap<>();

    // =========================================================================
    // API
    // =========================================================================
    public static synchronized void put(final String key,
                                        final Object value) throws CacheWritingException {
        Loggers.CACHE.debug("push to cache : {}", key);
        CACHES.put(key, value);
    }

    public static <T> T get(final String key) {
        return (T) CACHES.get(key);
    }

    public static synchronized void clear() {
        Loggers.CACHE.debug("clear all");
        CACHES.clear();
    }


}
