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

import io.inugami.api.processors.ConfigHandler;
import io.inugami.commons.security.EncryptionUtils;
import io.inugami.commons.threads.RunAndCloseService;
import io.inugami.maven.plugin.analysis.api.actions.PropertiesInitialization;
import io.inugami.maven.plugin.analysis.api.models.ScanNeo4jResult;
import io.inugami.maven.plugin.analysis.api.scan.issue.tracker.IssueTrackerProvider;
import io.inugami.maven.plugin.analysis.plugin.services.scan.git.issue.trackers.IssueTrackerCommons;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.settings.Server;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcher;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.inugami.maven.plugin.analysis.api.utils.NodeUtils.processIfNotNull;
import static io.inugami.maven.plugin.analysis.plugin.services.scan.git.issue.trackers.IssueTrackerCommons.PR_URL;

@SuppressWarnings({"java:S1845", "java:S6397", "java:S6395","java:S6353"})
@Slf4j
public class GitLabIssueTrackerProvider implements IssueTrackerProvider, PropertiesInitialization {
    // =========================================================================
    // ATTRIBUTES
    // =========================================================================
    public static final String FEATURE_NAME = "inugami.maven.plugin.analysis.issue.tracker.gitlab";
    public static final String SERVER_TOKEN = FEATURE_NAME + ".token";
    public static final String NB_THREADS   = FEATURE_NAME + ".threads";
    public static final String TIMEOUT      = FEATURE_NAME + ".timeout";
    public static final String GITLAB       = "gitlab";

    private static final String GRP_FEATURE     = "feature";
    private static final String GRP_REF_FEATURE = "refFeature";
    private static final String GRP_PR          = "pr";

    private static final Pattern REGEX = Pattern
            .compile("(?<feature>[#][0-9]+)|(?<pr>[!][0-9]+)|(?:[\\/](?<refFeature>[0-9]+)[\\s-_])");

    private String url;
    private String urlPr;
    private long   timeout;
    private int    nbThreads;
    private String token;
    private String projectSha;

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
        url = configuration.grab(IssueTrackerProvider.URL);
        urlPr = configuration.grabOrDefault(PR_URL, url);
        token = configuration.grab(SERVER_TOKEN);
        timeout = configuration.grabLong(TIMEOUT, 30000L);
        nbThreads = configuration.grabInt(NB_THREADS, 10);
        projectSha = new EncryptionUtils().encodeSha1(url);
    }


    // =========================================================================
    // API
    // =========================================================================
    @Override
    public Set<String> extractTicketNumber(final String commitMessage) {
        final Set<String> result = new LinkedHashSet<>();
        if (commitMessage != null && (commitMessage.contains("#") ||
                commitMessage.contains("!") ||
                commitMessage.contains("/"))) {

            final Matcher matcher = REGEX.matcher(commitMessage);
            while (matcher.find()) {
                final String feature    = matcher.group(GRP_FEATURE);
                final String refFeature = matcher.group(GRP_REF_FEATURE);
                final String pr         = matcher.group(GRP_PR);

                processIfNotNull(feature, result::add);
                processIfNotNull(refFeature, value -> result.add("#" + value));
                processIfNotNull(pr, result::add);
            }

        }
        return result.isEmpty() ? null : result;
    }

    @Override
    public ScanNeo4jResult buildNodes(final Set<String> tickets, final String versionUid) {
        final ScanNeo4jResult result = ScanNeo4jResult.builder().build();

        final List<Callable<ScanNeo4jResult>> tasks = new ArrayList<>();
        if (tickets != null && !tickets.isEmpty()) {
            for (final String ticket : tickets) {
                final String ticketId = ticket.trim();
                if (ticketId.startsWith("!", 0)) {
                    tasks.add(new GitlabTask(ticketId.substring(1),
                                             IssueTrackerCommons.TicketType.MERGE_REQUEST,
                                             token,
                                             url,
                                             urlPr,
                                             versionUid,
                                             projectSha));
                } else {
                    tasks.add(new GitlabTask(ticketId.substring(1),
                                             IssueTrackerCommons.TicketType.ISSUE,
                                             token,
                                             url,
                                             urlPr,
                                             versionUid,
                                             projectSha));
                }
            }
        }

        return retrieveNodeInformation(result, tasks);
    }

    private ScanNeo4jResult retrieveNodeInformation(final ScanNeo4jResult result,
                                                    final List<Callable<ScanNeo4jResult>> tasks) {
        final List<ScanNeo4jResult> resultSet = new RunAndCloseService<>(GITLAB, timeout, nbThreads, tasks).run();

        for (final ScanNeo4jResult itemResult : Optional.ofNullable(resultSet).orElse(new ArrayList<>())) {
            if (itemResult != null) {
                ScanNeo4jResult.merge(itemResult, result);
            }
        }

        return result;
    }
}
