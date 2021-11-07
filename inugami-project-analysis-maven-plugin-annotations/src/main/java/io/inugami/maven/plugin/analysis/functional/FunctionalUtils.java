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
package io.inugami.maven.plugin.analysis.functional;

import java.util.Collection;
import java.util.Map;
import java.util.function.Consumer;

public final class FunctionalUtils {

    private FunctionalUtils(){
    }


    public static <T> void applyIfNotNull(T value, Consumer<T> consumer){
        if(value != null  && consumer!=null){
            consumer.accept(value);
        }
    }

    public static  void applyIfNotEmpty(String value, Consumer<String> consumer){
        if(value != null && !value.isEmpty() && consumer!=null){
            consumer.accept(value);
        }
    }

    public static <T>  void applyIfNotEmpty(Collection<T> values, Consumer<Collection<T>> consumer){
        if(values != null && !values.isEmpty() && consumer!=null){
            consumer.accept(values);
        }
    }

    public static <T,V>  void applyIfNotEmpty(Map<T,V> values, Consumer<Map<T,V>> consumer){
        if(values != null && !values.isEmpty() && consumer!=null){
            consumer.accept(values);
        }
    }
}
