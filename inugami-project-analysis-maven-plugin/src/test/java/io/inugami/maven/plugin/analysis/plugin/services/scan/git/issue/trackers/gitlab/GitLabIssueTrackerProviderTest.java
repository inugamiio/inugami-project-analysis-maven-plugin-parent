package io.inugami.maven.plugin.analysis.plugin.services.scan.git.issue.trackers.gitlab;

import edu.emory.mathcs.backport.java.util.Arrays;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class GitLabIssueTrackerProviderTest {


    @Test
    public void extractTicketNumber_withTicketsInCommit_shouldExtractThese() {
        final GitLabIssueTrackerProvider service = new GitLabIssueTrackerProvider();

        assertThat(service.extractTicketNumber("MergeCommand.FastForwardMode.Merge branch '#21 my_feature' into 'dev' fix front See merge request project/internal/spring-boot-training!14 and !15"))
                .isEqualTo(buildSet("#21", "!14", "!15"));
        assertThat(service.extractTicketNumber("Dev/104 create kafka provider"))
                .isEqualTo(buildSet("#104"));
        assertThat(service.extractTicketNumber(null)).isNull();
        assertThat(service.extractTicketNumber("simple commit message")).isNull();

    }

    private Set<String> buildSet(final String... values) {
        return new LinkedHashSet<>(Arrays.asList(values));
    }
}