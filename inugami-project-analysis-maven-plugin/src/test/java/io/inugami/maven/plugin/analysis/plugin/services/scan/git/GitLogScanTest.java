package io.inugami.maven.plugin.analysis.plugin.services.scan.git;

import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.ObjectId;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GitLogScanTest {

    @Test
    void resolveLastTag_basicUseCase() {
        final GitLogScan gitLogScan = new GitLogScan();

        //@formatter:off
        final Map<String, AnyObjectId> tags = new LinkedHashMap<>();
        tags.put("app-test-0.0.1", ObjectId.fromString("19f3a2f7fa929340cf3056369492914613a8ccc5"));
        tags.put("app-test-0.1", ObjectId.fromString("29f3a2f7fa929340cf3056369492914613a8ccc5"));
        tags.put("app-test-0.0.1-RC2", ObjectId.fromString("3b75232a6a5ce4b6d32c229db38281979dfef6b8"));
        tags.put("app-test-0.0.1-RC3", ObjectId.fromString("4759132c66fa16cfb26d492dc46c027104ad2cc6"));
        tags.put("app-test-1.0.0-RC1", ObjectId.fromString("5de72a940930035fed1604af67f731f7c2ccdaec"));
        tags.put("app-test-1.0.0-RC2", ObjectId.fromString("6980a7b7b8e9c8b432f19afcf984fefe64aebb64"));
        //@formatter:off

        assertThat(gitLogScan.resolveLastTag(1, 0, 0, tags)).isEqualTo(tags.get("app-test-1.0.0-RC2"));
        assertThat(gitLogScan.resolveLastTag(0, 0, 1, tags)).isEqualTo(tags.get("app-test-0.0.1-RC3"));
        assertThat(gitLogScan.resolveLastTag(0, 0, 1, tags)).isEqualTo(tags.get("app-test-0.0.1-RC3"));
    }

    @Test
    void resolveLastTag_withFixOnPreviousVersion() {
        final GitLogScan gitLogScan = new GitLogScan();

        //@formatter:off
        final Map<String, AnyObjectId> tags = new LinkedHashMap<>();
        tags.put("app-test-0.0.1", ObjectId.fromString("19f3a2f7fa929340cf3056369492914613a8ccc5"));
        tags.put("app-test-0.1", ObjectId.fromString("29f3a2f7fa929340cf3056369492914613a8ccc5"));
        tags.put("app-test-0.0.1-RC2", ObjectId.fromString("3b75232a6a5ce4b6d32c229db38281979dfef6b8"));
        tags.put("app-test-0.0.1-RC3", ObjectId.fromString("4759132c66fa16cfb26d492dc46c027104ad2cc6"));
        tags.put("app-test-1.0.0-RC1", ObjectId.fromString("5de72a940930035fed1604af67f731f7c2ccdaec"));
        tags.put("app-test-1.0.0-RC2", ObjectId.fromString("6980a7b7b8e9c8b432f19afcf984fefe64aebb64"));
        tags.put("app-test-0.0.1-RC4", ObjectId.fromString("7759132c66fa16cfb26d492dc46c027104ad2cc6"));
        //@formatter:off

        assertThat(gitLogScan.resolveLastTag(0, 0, 1, tags)).isEqualTo(tags.get("app-test-0.0.1-RC4"));
        assertThat(gitLogScan.resolveLastTag(1, 0, 0, tags)).isEqualTo(tags.get("app-test-1.0.0-RC2"));
    }

    @Test
    void resolveLastTag_withExoticVersion() {
        final GitLogScan gitLogScan = new GitLogScan();

        //@formatter:off
        final Map<String, AnyObjectId> tags = new LinkedHashMap<>();
        tags.put("RELEASE_1_0_0", ObjectId.fromString("19f3a2f7fa929340cf3056369492914613a8ccc5"));
        tags.put("RELEASE_1.0", ObjectId.fromString("29f3a2f7fa929340cf3056369492914613a8ccc5"));
        tags.put("RELEASE_1.0_1-RC2", ObjectId.fromString("3b75232a6a5ce4b6d32c229db38281979dfef6b8"));
        tags.put("RELEASE_1.0_1-RC2-alpha2", ObjectId.fromString("4759132c66fa16cfb26d492dc46c027104ad2cc6"));
        tags.put("RELEASE_1.0_1-RC2-alpha3", ObjectId.fromString("5759132c66fa16cfb26d492dc46c027104ad2cc6"));
        tags.put("v2.0.10.RELEASE", ObjectId.fromString("6de72a940930035fed1604af67f731f7c2ccdaec"));
        tags.put("app-test-3.3.0.RELEASE", ObjectId.fromString("7980a7b7b8e9c8b432f19afcf984fefe64aebb64"));
        //@formatter:off

        assertThat(gitLogScan.resolveLastTag(1, 0, 1, tags)).isEqualTo(tags.get("RELEASE_1.0_1-RC2-alpha3"));
        assertThat(gitLogScan.resolveLastTag(3, 3, 0, tags)).isEqualTo(tags.get("app-test-3.3.0.RELEASE"));
    }
}


