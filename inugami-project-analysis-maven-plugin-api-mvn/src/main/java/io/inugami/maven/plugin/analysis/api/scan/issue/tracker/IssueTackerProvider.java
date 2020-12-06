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

import io.inugami.maven.plugin.analysis.api.models.Node;
import io.inugami.maven.plugin.analysis.api.models.ScanConext;

import java.util.List;
import java.util.Set;

public interface IssueTackerProvider {

    default boolean enable(final ScanConext context) {
        final String toggle = context.getConfiguration().grabOrDefault(getFeatureName(), "false");
        return Boolean.parseBoolean(toggle);
    }

    String getFeatureName();

    Set<String> extractTicketNumber(final String commitMessage);

    List<Node> buildNodes(Set<String> tickets);
}
