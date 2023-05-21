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
package io.inugami.maven.plugin.analysis.plugin.services.info.release.note.models;

import io.inugami.api.models.data.basic.JsonObject;
import lombok.*;

import java.util.LinkedHashSet;
import java.util.Set;

@Builder(toBuilder = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString
@Setter
@Getter
public class ServiceDto implements JsonObject, Comparable<ServiceDto> {
    private static final long        serialVersionUID = -6151404690187054191L;
    @EqualsAndHashCode.Include
    private              String      name;
    private              String      shortName;
    private              String      type;
    private              Set<String> consumers        = new LinkedHashSet<>();
    private              Set<String> producers        = new LinkedHashSet<>();
    private              Set<String> methods          = new LinkedHashSet<>();

    private String uri;
    private String contentType;
    private String consumeContentType;
    private String payload;
    private String responsePayload;
    private String verb;
    private String headers;
    private String additionalInfo;


    public ServiceDto addConsumer(final String consumer) {
        if (consumers == null) {
            consumers = new LinkedHashSet<>();
        }
        if (consumer != null) {
            consumers.add(consumer);
        }
        return this;
    }

    public ServiceDto addProducer(final String producer) {
        if (producers == null) {
            producers = new LinkedHashSet<>();
        }
        if (producer != null) {
            producers.add(producer);
        }
        return this;
    }

    public ServiceDto addMethod(final String value) {
        if (methods == null) {
            methods = new LinkedHashSet<>();
        }
        if (value != null) {
            methods.add(value);
        }
        return this;
    }

    public ServiceDto mergeConsumers(final Set<String> values) {
        if (this.consumers == null) {
            this.consumers = new LinkedHashSet<>();
        }
        if (values != null) {
            consumers.addAll(values);
        }
        return this;
    }

    public ServiceDto mergeProducers(final Set<String> values) {
        if (this.producers == null) {
            this.producers = new LinkedHashSet<>();
        }
        if (values != null) {
            producers.addAll(values);
        }
        return this;
    }

    public ServiceDto mergeMethods(final Set<String> values) {
        if (this.methods == null) {
            this.methods = new LinkedHashSet<>();
        }
        methods.addAll(values);
        return this;
    }

    @Override
    public int compareTo(final ServiceDto other) {
        return String.valueOf(name).compareTo(String.valueOf(other == null ? null : other.getName()));
    }
}
