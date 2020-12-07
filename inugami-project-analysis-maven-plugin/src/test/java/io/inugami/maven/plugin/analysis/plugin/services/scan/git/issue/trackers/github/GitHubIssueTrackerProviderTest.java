package io.inugami.maven.plugin.analysis.plugin.services.scan.git.issue.trackers.github;

import edu.emory.mathcs.backport.java.util.Arrays;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class GitHubIssueTrackerProviderTest {
    @Test
    public void extractTicketNumber_withTicketsInCommit_shouldExtractThese(){
        final GitHubIssueTrackerProvider service = new GitHubIssueTrackerProvider();

        assertThat(service.extractTicketNumber("Dev/104 create kafka provider"))
                .isEqualTo(buildSet("#104"));

        assertThat(service.extractTicketNumber("#10 update documentation on Git scm analyzer and merge #11"))
                .isEqualTo(buildSet("#10","#11"));

        assertThat(service.extractTicketNumber("Merge pull request #7 from inugamiio/spelling_suggestions "))
                .isEqualTo(buildSet("!7"));
        assertThat(service.extractTicketNumber(null)).isNull();
        assertThat(service.extractTicketNumber("simple commit message")).isNull();

    }


    private Set<String> buildSet(final String... values) {
        return new LinkedHashSet<>(Arrays.asList(values));
    }
}