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
package io.inugami.maven.plugin.analysis.plugin.services.info.rest;

import lombok.Getter;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
public class DependencyRest {

    private final Set<String> producers;
    private final Set<String> consumers;

    public DependencyRest(final String producer, final String consumer) {
        this.producers = producer == null ? new HashSet<>() : new HashSet<>(List.of(producer));
        this.consumers = consumer == null ? new HashSet<>() : new HashSet<>(List.of(consumer));
    }

    public DependencyRest(final Set<String> producers, final Set<String> consumers) {
        this.producers = producers == null ? new HashSet<>() : producers;
        this.consumers = consumers == null ? new HashSet<>() : consumers;
    }

    public DependencyRest() {
        this.producers = new HashSet<>();
        this.consumers = new HashSet<>();
    }

    public void addProducer(final String producer) {
        if (producer != null) {
            producers.add(producer);
        }
    }

    public void addProducers(final Collection<String> producers) {
        if (producers != null) {
            this.producers.addAll(producers);
        }
    }

    public void addConsumer(final String consumer) {
        if (consumer != null) {
            consumers.add(consumer);
        }
    }

    public void addConsumers(final Collection<String> consumers) {
        if (consumers != null) {
            this.consumers.addAll(consumers);
        }
    }
}
