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
package io.inugami.maven.plugin.analysis.plugin.services.scan.git.issue.trackers;

import io.inugami.maven.plugin.analysis.api.models.Node;
import io.inugami.maven.plugin.analysis.api.scan.issue.tracker.IssueTackerProvider;

import java.util.List;
import java.util.Set;

public class GitLabIssueTrackerProvider implements IssueTackerProvider {


    // =========================================================================
    // ATTRIBUTES
    // =========================================================================
    public static final String FEATURE_NAME                   = "inugami.maven.plugin.analysis.issue.tracker.gitlab";

    // =========================================================================
    // ACTIVATION
    // =========================================================================
    @Override
    public String getFeatureName() {
        return FEATURE_NAME;
    }


    // =========================================================================
    // API
    // =========================================================================
    @Override
    public Set<String> extractTicketNumber(final String commitMessage) {
        return null;
    }

    @Override
    public List<Node> buildNodes(final Set<String> tickets) {
        return null;
    }
}
