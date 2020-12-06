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
package io.inugami.maven.plugin.analysis.plugin.services.scan.git.issue.trackers.jira;

import io.inugami.api.processors.ConfigHandler;
import io.inugami.maven.plugin.analysis.api.actions.PropertiesInitialization;
import io.inugami.maven.plugin.analysis.api.models.ScanNeo4jResult;
import io.inugami.maven.plugin.analysis.api.scan.issue.tracker.IssueTackerProvider;
import org.apache.maven.settings.Server;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcher;

import java.util.Set;

import static io.inugami.maven.plugin.analysis.api.utils.NodeUtils.processIfNotNull;

public class JiraIssueTrackerProvider implements IssueTackerProvider, PropertiesInitialization {


    // =========================================================================
    // ATTRIBUTES
    // =========================================================================
    public static final String FEATURE_NAME    = "inugami.maven.plugin.analysis.issue.tracker.jira";
    public static final String SERVER_URL      = FEATURE_NAME + ".url";
    public static final String SERVER_USER     = FEATURE_NAME + ".user";
    public static final String SERVER_PASSWORD = FEATURE_NAME + ".password";
    public static final String JIRA            = "jira";

    // =========================================================================
    // ACTIVATION
    // =========================================================================
    @Override
    public String getServerName() {
        return JIRA;
    }

    @Override
    public String getSystemName() {
        return JIRA;
    }

    @Override
    public void processInitialization(final Server server, final ConfigHandler<String, String> configuration,
                                      final SecDispatcher secDispatcher, final Xpp3Dom serverConfig) throws Exception {

        processIfNotNull(getDecryptedValue(server.getUsername(), secDispatcher),
                         value -> configuration.put(SERVER_USER, value));

        processIfNotNull(getDecryptedValue(server.getPassword(), secDispatcher),
                         value -> configuration.put(SERVER_PASSWORD, value));
    }

    // =========================================================================
    // API
    // =========================================================================
    @Override
    public Set<String> extractTicketNumber(final String commitMessage) {
        return null;
    }

    @Override
    public ScanNeo4jResult buildNodes(final Set<String> tickets, final String versionUid) {
        return null;
    }
}
