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
package io.inugami.maven.plugin.analysis.plugin.services.scan.git.issue.trackers.gitlab;

import io.inugami.api.exceptions.TechnicalException;
import io.inugami.api.processors.ConfigHandler;
import io.inugami.commons.threads.ThreadsExecutorService;
import io.inugami.maven.plugin.analysis.api.actions.PropertiesInitialization;
import io.inugami.maven.plugin.analysis.api.models.ScanNeo4jResult;
import io.inugami.maven.plugin.analysis.api.scan.issue.tracker.IssueTackerProvider;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.settings.Server;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcher;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.inugami.maven.plugin.analysis.api.utils.NodeUtils.processIfNotNull;

@Slf4j
public class GitLabIssueTrackerProvider implements IssueTackerProvider, PropertiesInitialization {
    // =========================================================================
    // ATTRIBUTES
    // =========================================================================
    public static final String FEATURE_NAME = "inugami.maven.plugin.analysis.issue.tracker.gitlab";
    public static final String SERVER_TOKEN = FEATURE_NAME + ".token";
    public static final String NB_THREADS   = FEATURE_NAME + ".threads";
    public static final String TIMEOUT      = FEATURE_NAME + ".timeout";
    public static final String GITLAB       = "gitlab";

    private static final String  GRP_FEATURE = "feature";
    private static final String  GRP_PR      = "pr";
    private static final Pattern REGEX       = Pattern.compile("(?<feature>[#][0-9]+)|(?<pr>[!][0-9]+)");

    private String                 url;
    private ThreadsExecutorService threadsExecutorService;
    private long                   timeout;
    private String                 token;

    // =========================================================================
    // ACTIVATION
    // =========================================================================
    @Override
    public String getServerName() {
        return GITLAB;
    }

    @Override
    public String getSystemName() {
        return GITLAB;
    }


    @Override
    public void processInitialization(final Server server, final ConfigHandler<String, String> configuration,
                                      final SecDispatcher secDispatcher, final Xpp3Dom serverConfig) throws Exception {
        processIfNotNull(getDecryptedValue(server.getPrivateKey(), secDispatcher),
                         value -> configuration.put(SERVER_TOKEN, value));
    }

    @Override
    public void postConstruct(final ConfigHandler<String, String> configuration) {
        url                    = configuration.grab(IssueTackerProvider.URL);
        token                  = configuration.grab(SERVER_TOKEN);
        timeout                = configuration.grabLong(TIMEOUT, 30000L);
        threadsExecutorService = new ThreadsExecutorService(GITLAB,
                                                            configuration.grabInt(NB_THREADS, 10),
                                                            false,
                                                            configuration.grabLong(TIMEOUT, 30000L));
    }

    @Override
    public void shutdown() {
        threadsExecutorService.shutdown();
    }

    // =========================================================================
    // API
    // =========================================================================
    @Override
    public Set<String> extractTicketNumber(final String commitMessage) {
        final Set<String> result = new LinkedHashSet<>();
        if (commitMessage != null && (commitMessage.contains("#") || commitMessage.contains("!"))) {
            final Matcher matcher = REGEX.matcher(commitMessage);
            while (matcher.find()) {
                final String feature = matcher.group(GRP_FEATURE);
                final String pr      = matcher.group(GRP_PR);

                processIfNotNull(feature, value -> result.add(value));
                processIfNotNull(pr, value -> result.add(value));
            }

        }
        return result.isEmpty() ? null : result;
    }

    @Override
    public ScanNeo4jResult buildNodes(final Set<String> tickets, final String versionUid) {
        final ScanNeo4jResult result = ScanNeo4jResult.builder().build();

        final List<ScanNeo4jResult>           extraInfo = new ArrayList<>();
        final List<Callable<ScanNeo4jResult>> tasks     = new ArrayList<>();
        if (tickets != null && !tickets.isEmpty()) {
            for (final String ticket : tickets) {
                final String ticketId = ticket.trim();
                if (ticketId.startsWith("!")) {
                    tasks.add(new GitlabTask(ticketId.substring(1),
                                             GitlabTask.TicketType.MERGE_REQUEST,
                                             token,
                                             url,
                                             versionUid));
                }
                else {
                    tasks.add(new GitlabTask(ticketId.substring(1),
                                             GitlabTask.TicketType.ISSUE,
                                             token,
                                             url,
                                             versionUid));
                }
            }
        }

        List<ScanNeo4jResult> resultSet = null;
        if (!tasks.isEmpty()) {
            try {
                resultSet = threadsExecutorService.runAndGrab(tasks, timeout);
            }
            catch (final TechnicalException error) {
                log.error(error.getMessage(), error);
            }

        }

        if (resultSet != null) {
            for (final ScanNeo4jResult itemResult : resultSet) {
                ScanNeo4jResult.merge(itemResult, result);
            }
        }

        return result;
    }
}
