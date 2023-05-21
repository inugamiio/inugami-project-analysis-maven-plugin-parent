package io.inugami.maven.plugin.analysis.plugin.services.info.release.note.writers;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReleaseNoteJsonWriterTest {

    @Test
    void sortReleasesNotes_nominal_shouldSortDesc() {
        final ReleaseNoteJsonWriter writer = new ReleaseNoteJsonWriter();

        final String first  = "spring-boot-training.0.0.1-SNAPSHOT.json";
        final String second = "spring-boot-training.0.0.2-SNAPSHOT.json";
        final String third  = "spring-boot-training.0.0.3-SNAPSHOT.json";
        final List<String> result = writer.sortReleasesNotes(List.of(first,
                                                                     third,
                                                                     second));

        assertThat(result.get(0)).isEqualTo(third);
        assertThat(result.get(1)).isEqualTo(second);
        assertThat(result.get(2)).isEqualTo(first);
    }
}