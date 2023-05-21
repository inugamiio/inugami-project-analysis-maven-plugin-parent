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
package io.inugami.maven.plugin.analysis.plugin.services.scan.git.issue.trackers.github;

import io.inugami.api.processors.ConfigHandler;
import io.inugami.configuration.services.ConfigHandlerHashMap;
import io.inugami.maven.plugin.analysis.api.scan.issue.tracker.IssueTrackerProvider;

import java.util.LinkedHashSet;
import java.util.List;

public class GitHubIssueTrackerProviderIT {


    // =========================================================================
    // API
    // =========================================================================
    public static void main(final String... args) {

        final GitHubIssueTrackerProvider    provider = new GitHubIssueTrackerProvider();
        final ConfigHandler<String, String> config   = new ConfigHandlerHashMap();
        config.put(GitHubIssueTrackerProvider.SERVER_TOKEN, System.getProperty("project.token"));
        config.put(IssueTrackerProvider.SYSTEM, GitHubIssueTrackerProvider.GITHUB);
        config.put(IssueTrackerProvider.URL, System.getProperty("project.issue.url"));
        provider.postConstruct(config);

        provider.buildNodes(new LinkedHashSet<>(List.of("!7")), "project:app:0.0.1:jar");

        provider.buildNodes(new LinkedHashSet<>(List.of("#10")), "project:app:0.0.1:jar");


        provider.shutdown();
    }
}
