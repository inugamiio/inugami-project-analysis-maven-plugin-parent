package io.inugami.maven.plugin.analysis.plugin.services.info.release.note.writers.asciidoc;

import io.inugami.configuration.services.ConfigHandlerHashMap;
import io.inugami.maven.plugin.analysis.api.models.InfoContext;
import io.inugami.maven.plugin.analysis.api.services.info.release.note.models.Author;
import io.inugami.maven.plugin.analysis.api.services.info.release.note.models.Issue;
import io.inugami.maven.plugin.analysis.api.services.info.release.note.models.MergeRequests;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static io.inugami.commons.test.UnitTestHelper.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
public class ReleaseNoteAsciidocWriterTest {
    @Mock
    private InfoContext context;

    @BeforeEach
    public void setup() {
        lenient().when(context.getConfiguration()).thenReturn(new ConfigHandlerHashMap());
    }


    @Test
    public void renderAuthors_withValues_shouldRenderParagraph() {
        final ReleaseNoteAsciidocWriter writer = new ReleaseNoteAsciidocWriter();
        final Set<Author> authors = new LinkedHashSet(List.of(Author.builder()
                                                                    .name("Joe Foobar")
                                                                    .email("joe.foobar@inugami.io")
                                                                    .build(),
                                                              Author.builder()
                                                                    .name("John Smith")
                                                                    .email("john.smith@inugami.io")
                                                                    .build()));


        assertTextRelative(writer.renderAuthors(authors, false, context.getConfiguration()),
                           "info/release/note/writers/asciidoc-authors-split.adoc");
        assertTextRelative(writer.renderAuthors(authors, true, context.getConfiguration()),
                           "info/release/note/writers/asciidoc-authors.adoc");
    }

    @Test
    public void renderCommit_withValues_shouldRenderParagraph() {
        final ReleaseNoteAsciidocWriter writer = new ReleaseNoteAsciidocWriter();
        final Set<String> commit = new LinkedHashSet(List.of(
                "[2019-07-23T19:16:11][24133ed1b100e0a61bbda19a90653ec415a87fe1][Patrick Guillerm] Merge branch 'master' into development",
                "[2019-07-23T19:16:20][fae62f793c1b71597a18c95fa83e4cee10c17f20][Patrick Guillerm] Merge pull request #11 from inugamiio/development  Development",
                "[2019-07-23T19:19:21][16f846aab972e5b7188cf4232cda4255caf92843][InugamiCi       ] [maven-release-plugin] prepare release v1.2.0",
                "[2019-07-23T19:19:28][5d77140f171efd2704e868f515ded7cdc8e82188][InugamiCi       ] [maven-release-plugin] prepare for next development iteration",
                "[2020-06-30T09:29:55][3b5cccdb69c651bfcb42c8ed27ba4101ea464380][Patrick Guillerm] add mutation tests",
                "[2020-06-30T09:32:14][e1c3f0edae4db1514b059f763710d92c70aaf7c1][Patrick Guillerm] Merge pull request #12 from inugamiio/development  add mutation tests",
                "[2020-09-17T21:13:03][215d03a5d9586e038aa0f4d14b6e416be2d258c0][Patrick Guillerm] change groupId for io.inugami",
                "[2020-09-17T21:13:52][21192a2e8c596794bcc996aa8c1173df7af23a0d][Patrick Guillerm] Merge branch 'master' into development",
                "[2020-10-31T13:58:12][0150b50af2444810487dbbae906d2248aab7e372][Patrick Guillerm] prepare to deploy on sonatype repository",
                "[2020-10-31T14:03:43][e636be118db974543bfc1630772ebc47d0cc0e0b][Patrick Guillerm] prepare release"
        ));


        assertText(readFileRelative("info/release/note/writers/asciidoc-commit-split.adoc"),
                   writer.renderCommit(commit, false, context.getConfiguration()));
        assertText(readFileRelative("info/release/note/writers/asciidoc-commit.adoc"),
                   writer.renderCommit(commit, true, context.getConfiguration()));
    }

    @Test
    public void renderMergeRequest_withValues_shouldRenderParagraph() {
        final ReleaseNoteAsciidocWriter writer = new ReleaseNoteAsciidocWriter();
        final List<MergeRequests> mergeRequests = List.of(
                MergeRequests.builder()
                             .date("2019-04-17T18:26:18Z")
                             .title("implement svg generic map component example")
                             .uid("pr_4")
                             .url("https://github.com/inugamiio/inugami-plugin-dashboard-demo/pull/4")
                             .build(),

                MergeRequests.builder()
                             .date("2019-04-12T15:39:58Z")
                             .title("define basic dashboard layout")
                             .uid("pr_1")
                             .url("https://github.com/inugamiio/inugami-plugin-dashboard-demo/pull/1")
                             .build(),

                MergeRequests.builder()
                             .date("2019-04-12T15:46:07Z")
                             .title("Development")
                             .uid("pr_3")
                             .url("https://github.com/inugamiio/inugami-plugin-dashboard-demo/pull/3")
                             .build()
        );

        assertText(readFileRelative("info/release/note/writers/asciidoc-merge-split.adoc"),
                   writer.renderMergeRequest(mergeRequests, false, context.getConfiguration()));
        assertText(readFileRelative("info/release/note/writers/asciidoc-merge.adoc"),
                   writer.renderMergeRequest(mergeRequests, true, context.getConfiguration()));
    }

    @Test
    public void renderIssues_withValues_shouldRenderParagraph() {
        final ReleaseNoteAsciidocWriter writer = new ReleaseNoteAsciidocWriter();
        final List<Issue> issues = List.of(
                Issue.builder()
                     .date("2019-03-27T09:42:13Z")
                     .labels(Set.of("waitting_for_release", "technical_issue", "Front"))
                     .name("issue_8")
                     .title("upgrade to Angular 8")
                     .url("https://github.com/inugamiio/inugami/issues/8")
                     .build(),

                Issue.builder()
                     .date("2019-07-05T08:07:49Z")
                     .labels(Set.of("technical_issue", "Feature", "Back"))
                     .name("issue_104")
                     .title("Monitoring - create Kafka sender")
                     .url("https://github.com/inugamiio/inugami/issues/104")
                     .build()
        );

        assertText(readFileRelative("info/release/note/writers/asciidoc-issues-split.adoc"),
                   writer.renderIssues(issues, false, context.getConfiguration()));
        assertText(readFileRelative("info/release/note/writers/asciidoc-issues.adoc"),
                   writer.renderIssues(issues, true, context.getConfiguration()));
    }
}