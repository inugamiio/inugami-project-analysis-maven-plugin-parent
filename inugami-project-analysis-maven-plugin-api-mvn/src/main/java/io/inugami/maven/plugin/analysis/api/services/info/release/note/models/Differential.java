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
package io.inugami.maven.plugin.analysis.api.services.info.release.note.models;

import io.inugami.api.models.data.basic.JsonObject;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class Differential {

    final List<JsonObject> newValues     = new ArrayList<>();
    final List<JsonObject> deletedValues = new ArrayList<>();
    final List<JsonObject> sameValues    = new ArrayList<>();

    public static Differential buildDifferential(final Collection<JsonObject> inputCurrents, final Collection<JsonObject> inputPrevious) {
        final Differential           result   = new Differential();
        final Collection<JsonObject> currents = inputCurrents == null ? List.of() : inputCurrents;
        final Collection<JsonObject> previous = inputPrevious == null ? List.of() : inputPrevious;

        if (empty(currents) && !empty(previous)) {
            result.addDeletedValues(previous);
        } else if (!empty(currents) && empty(previous)) {
            result.addNewValues(currents);
        } else if (empty(currents) && empty(previous)) {
            //nothing to do
        } else {
            result.addNewValues(resolveNewValues(currents, previous));
            result.addDeletedValues(resolveDeleteValues(currents, previous));
            result.addSameValues(resolveSameValues(currents, result.getNewValues(), result.getDeletedValues()));
        }
        return result;
    }


    private static List<JsonObject> resolveNewValues(final Collection<JsonObject> currents, final Collection<JsonObject> previous) {
        final List<JsonObject> result = new ArrayList<>();
        for (final JsonObject value : currents) {
            if (!previous.contains(value)) {
                result.add(value);
            }
        }
        return result;
    }

    private static List<JsonObject> resolveDeleteValues(final Collection<JsonObject> currents,
                                                        final Collection<JsonObject> previous) {
        final List<JsonObject> result = new ArrayList<>();
        for (final JsonObject value : previous) {
            if (!currents.contains(value)) {
                result.add(value);
            }
        }
        return result;
    }

    private static List<JsonObject> resolveSameValues(final Collection<JsonObject> currents, final List<JsonObject> newValues,
                                                      final List<JsonObject> deletedValues) {
        final List<JsonObject> result = new ArrayList<>();
        for (final JsonObject value : currents) {
            if (!newValues.contains(value) && !deletedValues.contains(value)) {
                result.add(value);
            }
        }
        return result;
    }


    public Differential addNewValues(final Collection<JsonObject> values) {
        if (values != null && !values.isEmpty()) {
            newValues.addAll(values);
        }
        return this;
    }

    public Differential addNewValues(final JsonObject... values) {
        if (values.length > 0) {
            for (final JsonObject value : values) {
                if (value != null) {
                    newValues.add(value);
                }
            }
        }
        return this;
    }


    public Differential addDeletedValues(final Collection<JsonObject> values) {
        if (values != null && !values.isEmpty()) {
            deletedValues.addAll(values);
        }
        return this;
    }

    public Differential addDeletedValues(final JsonObject... values) {
        if (values.length > 0) {
            for (final JsonObject value : values) {
                if (value != null) {
                    deletedValues.add(value);
                }
            }
        }
        return this;
    }

    public Differential addSameValues(final Collection<JsonObject> values) {
        if (values != null && !values.isEmpty()) {
            sameValues.addAll(values);
        }
        return this;
    }

    public Differential addSameValues(final JsonObject... values) {
        if (values.length > 0) {
            for (final JsonObject value : values) {
                if (value != null) {
                    sameValues.add(value);
                }
            }
        }
        return this;
    }


    private static boolean empty(final Collection<JsonObject> values) {
        return values.isEmpty();
    }
}
