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
import io.inugami.api.spi.SpiLoader;
import io.inugami.commons.threads.RunAndCloseService;
import io.inugami.maven.plugin.analysis.api.actions.PropertiesInitialization;
import io.inugami.maven.plugin.analysis.api.connectors.HttpConnectorBuilder;
import io.inugami.maven.plugin.analysis.api.models.ScanNeo4jResult;
import io.inugami.maven.plugin.analysis.api.scan.issue.tracker.IssueTrackerProvider;
import io.inugami.maven.plugin.analysis.api.scan.issue.tracker.JiraCustomFieldsAppender;
import org.apache.maven.settings.Server;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcher;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.inugami.maven.plugin.analysis.api.utils.NodeUtils.processIfNotNull;

@SuppressWarnings({"java:S1845"})
public class JiraIssueTrackerProvider implements IssueTrackerProvider, PropertiesInitialization {


    // =========================================================================
    // ATTRIBUTES
    // =========================================================================
    public static final String FEATURE_NAME    = "inugami.maven.plugin.analysis.issue.tracker.jira";
    public static final String SERVER_USER     = FEATURE_NAME + ".user";
    public static final String SERVER_PASSWORD = FEATURE_NAME + ".password";
    public static final String TIMEOUT         = FEATURE_NAME + ".timeout";
    public static final String NB_THREADS      = FEATURE_NAME + ".threads";
    public static final String JIRA            = "jira";

    private static final String  GRP_ISSUE = "issue";
    private static final Pattern REGEX     = Pattern.compile("(?<issue>[a-zA-Z]+[-][0-9]+)");

    private String url;
    private long   timeout;
    private int    nbThreads;

    private String username;
    private String password;

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

    @Override
    public void postConstruct(final ConfigHandler<String, String> configuration) {
        url = configuration.grab(IssueTrackerProvider.URL);
        username = configuration.grab(SERVER_USER);
        password = configuration.grab(SERVER_PASSWORD);
        timeout = configuration.grabLong(TIMEOUT, 30000L);
        nbThreads = configuration.grabInt(NB_THREADS, 10);
    }

    // =========================================================================
    // API
    // =========================================================================
    @Override
    public Set<String> extractTicketNumber(final String commitMessage) {
        final Set<String> result = new LinkedHashSet<>();
        if (commitMessage != null && commitMessage.contains("-")) {
            final Matcher matcher = REGEX.matcher(commitMessage);
            while (matcher.find()) {
                final String issue = matcher.group(GRP_ISSUE);
                processIfNotNull(issue, value -> result.add(value));
            }
        }

        return result;
    }

    @Override
    public ScanNeo4jResult buildNodes(final Set<String> tickets, final String versionUid) {
        final ScanNeo4jResult      result      = ScanNeo4jResult.builder().build();
        final HttpConnectorBuilder httpBuilder = new HttpConnectorBuilder();
        final List<JiraCustomFieldsAppender> customFieldsAppenders = SpiLoader.getInstance()
                                                                              .loadSpiServicesByPriority(JiraCustomFieldsAppender.class);

        final List<Callable<ScanNeo4jResult>> tasks = new ArrayList<>();
        if (tickets != null && !tickets.isEmpty()) {
            for (final String ticket : tickets) {
                tasks.add(new JiraTask(ticket,
                                       username,
                                       password,
                                       url,
                                       httpBuilder,
                                       versionUid,
                                       Optional.ofNullable(customFieldsAppenders).orElse(new ArrayList<>())));
            }
        }

        final List<ScanNeo4jResult> resultSet = new RunAndCloseService(JIRA, timeout, nbThreads, tasks).run();
        for (final ScanNeo4jResult itemResult : Optional.ofNullable(resultSet).orElse(new ArrayList<>())) {
            if (itemResult != null) {
                ScanNeo4jResult.merge(itemResult, result);
            }
        }

        return result;
    }
}
