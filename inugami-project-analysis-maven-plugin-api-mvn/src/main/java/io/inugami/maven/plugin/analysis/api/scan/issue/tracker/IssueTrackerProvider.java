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
package io.inugami.maven.plugin.analysis.api.scan.issue.tracker;

import io.inugami.api.processors.ConfigHandler;
import io.inugami.maven.plugin.analysis.api.models.ScanConext;
import io.inugami.maven.plugin.analysis.api.models.ScanNeo4jResult;

import java.util.Set;

@SuppressWarnings({"java:S1845"})
public interface IssueTrackerProvider {

    String SYSTEM = "project.issue.management.system";
    String URL    = "project.issue.management.url";

    default boolean enable(final ScanConext context) {
        return enable(context.getConfiguration());
    }

    default boolean enable(final ConfigHandler<String, String> configuration) {
        final String system = configuration.grabOrDefault(SYSTEM, null);
        return system == null ? false : system.equals(getSystemName());
    }

    default void postConstruct(final ConfigHandler<String, String> configuration) {

    }

    default void shutdown() {

    }

    String getSystemName();

    Set<String> extractTicketNumber(final String commitMessage);

    ScanNeo4jResult buildNodes(Set<String> tickets, String versionUid);
}
