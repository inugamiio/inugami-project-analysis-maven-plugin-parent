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
package io.inugami.maven.plugin.analysis.plugin.services.scan.git;

import io.inugami.api.models.JsonBuilder;
import io.inugami.api.models.data.basic.JsonObject;
import io.inugami.api.spi.SpiLoader;
import io.inugami.api.tools.ConsoleColors;
import io.inugami.maven.plugin.analysis.api.actions.ProjectScanner;
import io.inugami.maven.plugin.analysis.api.models.Node;
import io.inugami.maven.plugin.analysis.api.models.Relationship;
import io.inugami.maven.plugin.analysis.api.models.ScanConext;
import io.inugami.maven.plugin.analysis.api.models.ScanNeo4jResult;
import io.inugami.maven.plugin.analysis.api.scan.issue.tracker.IssueTrackerProvider;
import io.inugami.maven.plugin.analysis.api.utils.NodeUtils;
import io.inugami.maven.plugin.analysis.plugin.services.scan.git.issue.trackers.IssueTrackerCommons;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

import static io.inugami.maven.plugin.analysis.api.tools.BuilderTools.*;
import static io.inugami.maven.plugin.analysis.api.utils.NodeUtils.processIfNotNull;

@SuppressWarnings({"java:S5361", "java:S3824"})
@Slf4j
public class GitLogScan implements ProjectScanner {


    // =========================================================================
    // ATTRIBUTES
    // =========================================================================
    public static final String FEATURE_NAME                   = "inugami.maven.plugin.analysis.git";
    public static final String REFS_TAGS                      = "refs/tags/";
    public static final String RELATIONSHIP_HAVE_AUTHOR       = "HAVE_AUTHOR";
    public static final String RELATIONSHIP_HAVE_SCM_INFO     = "HAVE_SCM_INFO";
    public static final String RELATIONSHIP_WORKED_ON_VERSION = "WORKED_ON_VERSION";
    public static final String SCM                            = "scm";
    public static final String AUTHOR                         = "Author";
    public static final String SCM_TYPE                       = "Scm";
    public static final String OPEN                           = "[";
    public static final String CLOSE                          = "]";
    public static final String CLOSE_SPACE                    = "] ";
    public static final String SPACE                          = " ";
    public static final String HAS_ISSUE                      = "HAS_ISSUE";
    public static final String HAS_SCM                        = "HAS_SCM";
    public static final String COMMIT_UID                     = "commitUid";


    // =========================================================================
    // FEATURE
    // =========================================================================
    @Override
    public boolean enable(final ScanConext context) {
        return Boolean.parseBoolean(context.getConfiguration().grabOrDefault(FEATURE_NAME, "false"));
    }

    // =========================================================================
    // API
    // =========================================================================
    @Override
    public List<JsonObject> scan(final ScanConext context) {
        List<JsonObject> result = null;
        try {
            result = process(context);
        } catch (final Exception e) {
            log.error(e.getMessage(), e);
        }
        return result == null ? List.of() : result;
    }

    protected List<JsonObject> process(final ScanConext context) throws Exception {
        final ScanNeo4jResult result  = ScanNeo4jResult.builder().build();
        final List<GitLog>    gitLogs = extractGitLogs(context.getBasedir(), context.getProject().getVersion());

        if (gitLogs != null && !gitLogs.isEmpty()) {

            final Node versionNode = buildNodeVersion(context.getProject());
            result.addNode(versionNode);

            final IssueResult issues = buildIssues(gitLogs, versionNode, context);
            ScanNeo4jResult.merge(issues.getScanNeo4jResult(), result);

            final List<Node> scm = buildScmNode(gitLogs);
            result.addNodeToDelete(scm.stream().map(Node::getUid).collect(Collectors.toList()));
            result.addNode(scm);


            for (final Node scmNode : scm) {
                result.addRelationship(Relationship
                                               .builder()
                                               .from(versionNode.getUid())
                                               .to(scmNode.getUid())
                                               .type(RELATIONSHIP_HAVE_SCM_INFO)
                                               .build());
            }


            final List<Node> authors = buildAuthors(gitLogs);
            if (!authors.isEmpty()) {
                result.addNode(authors);
                final List<Relationship> authorRelationships = new ArrayList<>();
                authors.stream()
                       .map(author -> buildAuthorRelationships(versionNode, author, scm))
                       .forEach(authorRelationships::addAll);
                result.addRelationship(authorRelationships);
            }
            attachIssueToGit(result, issues.getGitIssues());
        }

        return List.of(result);
    }


    protected List<GitLog> extractGitLogs(final File baseDir,
                                          final String projectVersion) throws IOException, GitAPIException {
        final List<GitLog>          result      = new ArrayList<>();
        final FileRepositoryBuilder builder     = new FileRepositoryBuilder();
        final File                  gitRepoFile = resolveGitRepo(baseDir.getAbsolutePath());
        final Repository            repo        = builder.setGitDir(gitRepoFile).setMustExist(true).build();
        final Git                   git         = new Git(repo);


        final AnyObjectId headCommitId = git.getRepository().resolve("HEAD");
        final AnyObjectId tagFound     = resolveLastTag(projectVersion, git);
        ObjectId          tagCommit    = null;
        if (tagFound != null) {
            try (final RevWalk walk = new RevWalk(repo)) {
                final RevCommit commit = walk.parseCommit(tagFound);
                tagCommit = commit.getId();
            }
        }
        log.debug(" tag {}", tagCommit);

        final Iterable<RevCommit> logs = tagFound == null ? git.log().call()
                : git.log().addRange(tagCommit, headCommitId).call();

        for (final Iterator<RevCommit> iterator = logs.iterator(); iterator.hasNext(); ) {
            final RevCommit rev = iterator.next();

            final GitLog gitLog = GitLog.builder()
                                        .id(String.valueOf(rev.getId()))
                                        .type(rev.getType())
                                        .name(rev.getName())
                                        .message(rev.getFullMessage().replaceAll("\n", " "))
                                        .author(rev.getAuthorIdent().getName())
                                        .authorEmail(rev.getAuthorIdent().getEmailAddress())
                                        .date(LocalDateTime.ofEpochSecond(rev.getCommitTime(), 0, ZoneOffset.UTC))
                                        .build();
            result.add(gitLog);
            log.debug("{}", gitLog);
        }

        git.close();
        return result;
    }

    private File resolveGitRepo(final String absolutePath) {
        File result = null;
        if (absolutePath != null) {
            File file = new File(absolutePath + File.separator + ".git");
            if (file.exists()) {
                result = file;
            } else {
                file = resolveGitRepo(new File(absolutePath).getParent());
                if (file.exists()) {
                    result = file;
                }
            }
        }

        return result;
    }

    // =========================================================================
    // RESOLVE LAST TAG
    // =========================================================================
    private AnyObjectId resolveLastTag(final String projectVersion, final Git git) {
        try {
            final Integer   majorVersion = extractMajorVersion(projectVersion);
            final Integer   minorVersion = extractMinorVersion(projectVersion);
            final Integer   pathVersion  = extractPatchVersion(projectVersion);
            final List<Ref> tagsRef      = git.getRepository().getRefDatabase().getRefsByPrefix(REFS_TAGS);

            final Map<String, AnyObjectId> tags = new LinkedHashMap<>();
            for (final Ref tag : tagsRef) {
                final String tagName = tag.getName().replace(REFS_TAGS, "");
                tags.put(tagName, tag.getObjectId());
            }

            return resolveLastTag(majorVersion, minorVersion, pathVersion, tags);
        } catch (final IOException error) {
            return null;
        }
    }

    protected AnyObjectId resolveLastTag(final Integer majorVersion, final Integer minorVersion,
                                         final Integer patchVersion,
                                         final Map<String, AnyObjectId> tags) {

        AnyObjectId result = null;
        if (tags == null) {
            return result;
        }

        final List<TagRefName> tagNames = tags.keySet()
                                              .stream()
                                              .map(TagRefName::new)
                                              .collect(Collectors.toList());
        Collections.sort(tagNames);

        log.debug("{}.{}.{}", majorVersion, minorVersion, patchVersion);
        for (final TagRefName tagName : tagNames) {
            log.debug("tag version : {}", tagName.getTagName());

            if (tagName.isPreviousOrSame(majorVersion, minorVersion, patchVersion)) {
                result = tags.get(tagName.getTagName());
            }
        }


        return result;
    }

    // =========================================================================
    //  NODES BBUILDERS
    // =========================================================================
    private List<Node> buildAuthors(final List<GitLog> gitLogs) {

        final Set<CommitAuthor> authors = new LinkedHashSet<>();

        for (final GitLog gitLog : gitLogs) {
            if (gitLog.getAuthor() != null) {
                authors.add(new CommitAuthor(gitLog.getAuthorEmail(), gitLog.getAuthor()));
            }
        }

        final List<Node> result = new ArrayList<>(authors.size());

        for (final CommitAuthor author : authors) {
            final LinkedHashMap<String, Serializable> properties = new LinkedHashMap<>();
            processIfNotNull(author.getEmail(), value -> properties.put("email", value));
            result.add(Node.builder()
                           .type(AUTHOR)
                           .uid(String.join("_", author.getName(), author.getEmail()))
                           .name(author.getName())
                           .properties(properties)
                           .build());
        }

        return result;
    }

    private List<Node> buildScmNode(final List<GitLog> gitLogs) {
        final List<Node> result = new ArrayList<>();

        if (gitLogs != null) {
            for (final GitLog log : gitLogs) {
                result.add(buildScmNode(log));
            }
        }
        return result;
    }

    private Node buildScmNode(final GitLog gitLog) {
        final String                              name       = String.join("_", gitLog.getName());
        final LinkedHashMap<String, Serializable> properties = new LinkedHashMap<>();
        final JsonBuilder                         commit     = new JsonBuilder();

        int dateSize   = 0;
        int nameSize   = 0;
        int authorSize = 0;

        final String commitUid = String.valueOf(gitLog.getName());
        final String author    = String.valueOf(gitLog.getAuthor());
        final String date      = String.valueOf(gitLog.getDate());
        if (date.length() > dateSize) {
            dateSize = date.length();
        }
        if (commitUid.length() > nameSize) {
            nameSize = commitUid.length();
        }
        if (author.length() > authorSize) {
            authorSize = author.length();
        }


        commit.write(OPEN);
        commit.write(gitLog.getDate());
        commit.write(ConsoleColors.createLine(SPACE, dateSize - String.valueOf(gitLog.getDate()).length()));
        commit.write(CLOSE);

        commit.write(OPEN);
        commit.write(gitLog.getName());
        commit.write(ConsoleColors.createLine(SPACE, nameSize - gitLog.getName().length()));
        commit.write(CLOSE);

        commit.write(OPEN);
        commit.write(gitLog.getAuthor());
        commit.write(ConsoleColors.createLine(SPACE, authorSize - gitLog.getAuthor().length()));
        commit.write(CLOSE_SPACE);
        commit.write(gitLog.getMessage() == null ? "" : gitLog.getMessage().replace("\n", SPACE));


        properties.put("commit", commit.toString());
        properties.put("message", gitLog.getMessage().replace("\n", SPACE));
        properties.put(COMMIT_UID, commitUid);
        properties.put("author", author);
        properties.put("date", date);

        return Node.builder()
                   .type(SCM_TYPE)
                   .uid(name)
                   .name(name)
                   .properties(properties)
                   .build();
    }

    private List<Relationship> buildAuthorRelationships(final Node versionNode, final Node author, final List<Node> scmNodes) {
        final List<Relationship> result = new ArrayList<>();

        for (final Node scm : scmNodes) {
            result.addAll(List.of(Relationship
                                          .builder()
                                          .from(versionNode.getUid())
                                          .to(author.getUid())
                                          .type(RELATIONSHIP_HAVE_AUTHOR)
                                          .build(),
                                  Relationship
                                          .builder()
                                          .from(scm.getUid())
                                          .to(author.getUid())
                                          .type(RELATIONSHIP_HAVE_AUTHOR)
                                          .build(),
                                  Relationship
                                          .builder()
                                          .from(author.getUid())
                                          .to(versionNode.getUid())
                                          .type(RELATIONSHIP_WORKED_ON_VERSION)
                                          .build(),
                                  Relationship
                                          .builder()
                                          .from(author.getUid())
                                          .to(scm.getUid())
                                          .type(RELATIONSHIP_WORKED_ON_VERSION)
                                          .build()
            ));
        }
        return result;
    }


    private IssueResult buildIssues(final List<GitLog> gitLogs, final Node versionNode, final ScanConext context) {
        final List<IssueTrackerProvider> providers = SpiLoader.getInstance()
                                                              .loadSpiServicesByPriority(IssueTrackerProvider.class);

        final List<IssueTrackerProvider> providersEnabled = providers.stream()
                                                                     .filter(provider -> provider.enable(context))
                                                                     .collect(Collectors.toList());

        final Map<IssueTrackerProvider, Set<String>> issues    = new LinkedHashMap<>();
        final Map<GitLog, Set<String>>               gitIssues = new LinkedHashMap<>();

        for (final GitLog gitLog : gitLogs) {
            for (final IssueTrackerProvider provider : providersEnabled) {
                buildIssuesOnIssueTracker(context, issues, gitIssues, gitLog, provider);
            }
        }

        return IssueResult.builder()
                          .gitIssues(gitIssues)
                          .scanNeo4jResult(getNeo4jResult(versionNode, context, issues))
                          .build();
    }

    private static void buildIssuesOnIssueTracker(final ScanConext context, final Map<IssueTrackerProvider, Set<String>> issues, final Map<GitLog, Set<String>> gitIssues, final GitLog gitLog, final IssueTrackerProvider provider) {
        if (provider.enable(context)) {
            final Set<String> extractedTicketsNumber = provider.extractTicketNumber(gitLog.getMessage());
            if (extractedTicketsNumber != null && !extractedTicketsNumber.isEmpty()) {
                Set<String> providerIssues = issues.get(provider);
                if (providerIssues == null) {
                    providerIssues = new LinkedHashSet<>();
                    issues.put(provider, providerIssues);
                }
                providerIssues.addAll(extractedTicketsNumber);
                gitIssues.put(gitLog, extractedTicketsNumber);
            }
        }
    }


    private synchronized ScanNeo4jResult getNeo4jResult(final Node versionNode, final ScanConext context,
                                                        final Map<IssueTrackerProvider, Set<String>> issues) {
        final ScanNeo4jResult result = ScanNeo4jResult.builder().build();
        if (!issues.isEmpty()) {
            for (final Map.Entry<IssueTrackerProvider, Set<String>> entry : issues.entrySet()) {
                final IssueTrackerProvider provider = entry.getKey();
                provider.postConstruct(context.getConfiguration());
                try {
                    final ScanNeo4jResult providerResult = provider.buildNodes(entry.getValue(), versionNode.getUid());
                    ScanNeo4jResult.merge(providerResult, result);
                } catch (final Exception error) {
                    log.error(error.getMessage(), error);
                }
            }
            for (final IssueTrackerProvider provider : issues.keySet()) {
                try {
                    provider.shutdown();
                } catch (final Exception error) {
                    log.error(error.getMessage(), error);
                }
            }
        }

        return result;
    }

    private void attachIssueToGit(final ScanNeo4jResult providerResult, final Map<GitLog, Set<String>> gitIssues) {
        if (gitIssues == null) {
            return;
        }
        final List<Node> nodeIssues = extractNodeType(providerResult.getNodes(), IssueTrackerCommons.TicketType.ISSUE.getNodeType());
        final List<Node> nodeGit    = extractNodeType(providerResult.getNodes(), SCM_TYPE);


        for (final Map.Entry<GitLog, Set<String>> entry : gitIssues.entrySet()) {
            final Node       gitNode     = searchGitNode(entry.getKey(), nodeGit);
            final List<Node> isssueNodes = searchIssueNodes(entry.getValue(), nodeIssues);

            if (gitNode != null) {
                for (final Node isssueNode : isssueNodes) {
                    providerResult.addRelationship(Relationship
                                                           .builder()
                                                           .from(gitNode.getUid())
                                                           .to(isssueNode.getUid())
                                                           .type(HAS_ISSUE)
                                                           .build());

                    providerResult.addRelationship(Relationship
                                                           .builder()
                                                           .from(isssueNode.getUid())
                                                           .to(gitNode.getUid())
                                                           .type(HAS_SCM)
                                                           .build());
                }
            }
        }
    }


    private Node searchGitNode(final GitLog gitlog, final List<Node> nodes) {
        for (final Node node : nodes) {
            if (gitlog.getName().equals(NodeUtils.getStringValue(COMMIT_UID, node))) {
                return node;
            }
        }
        return null;
    }

    private List<Node> searchIssueNodes(final Collection<String> issues, final List<Node> nodeIssues) {
        if (issues == null || nodeIssues == null) {
            return new ArrayList<>();
        }
        final Set<Node> result = new LinkedHashSet<>();

        for (final String issue : issues) {
            final String issueNumber = cleanIssueNumber(issue);
            for (final Node node : nodeIssues) {
                if (node.getName().equalsIgnoreCase("issue_" + issueNumber)) {
                    result.add(node);
                }
            }
        }
        return new ArrayList<>(result);
    }

    private String cleanIssueNumber(final String issue) {
        return issue.replace("#", "");
    }

    private List<Node> extractNodeType(final List<Node> nodes, final String nodeType) {
        return nodes.stream()
                    .filter(node -> node.getType().equals(nodeType))
                    .collect(Collectors.toList());
    }

    @Builder
    @AllArgsConstructor
    @Getter
    private static class IssueResult {
        private final ScanNeo4jResult          scanNeo4jResult;
        private final Map<GitLog, Set<String>> gitIssues;
    }
}
