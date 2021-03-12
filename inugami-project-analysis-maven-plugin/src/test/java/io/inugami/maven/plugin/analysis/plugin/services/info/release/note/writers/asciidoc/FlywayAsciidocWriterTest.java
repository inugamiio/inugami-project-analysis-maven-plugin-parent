package io.inugami.maven.plugin.analysis.plugin.services.info.release.note.writers.asciidoc;

import io.inugami.maven.plugin.analysis.api.models.InfoContext;
import io.inugami.maven.plugin.analysis.api.services.info.release.note.models.Differential;
import io.inugami.maven.plugin.analysis.api.services.info.release.note.models.ReleaseNoteResult;
import io.inugami.maven.plugin.analysis.plugin.services.info.release.note.extractors.FlywayExtractor;
import io.inugami.maven.plugin.analysis.plugin.services.info.release.note.models.FlywayDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import static io.inugami.commons.test.UnitTestHelper.assertText;
import static io.inugami.commons.test.UnitTestHelper.loadJsonReference;


@ExtendWith(MockitoExtension.class)
public class FlywayAsciidocWriterTest {

    @Mock
    private InfoContext context;

    @Test
    public void rendering_withFlywayScripts_shouldrenderingParagraph() {
        FlywayAsciidocWriter writer = new FlywayAsciidocWriter();

        ReleaseNoteResult data = new ReleaseNoteResult();
        data.addDifferential(FlywayExtractor.FLYWAY,
                             Differential.buildDifferential(List.of(
                                     FlywayDTO.builder()
                                              .id("bed7140535657b70072cc2f2ba72344a69810495f8b93b8c58ecbfff47646ae1d7d8658d2d06bc1c05d6cf1f722d155ce41a5aecbf7eec30d67af71e68d15698")
                                              .type("mysql")
                                              .name("v1_0_0_init_issue_table.sql")
                                              .content("create table issue{\n" +
                                                               "    `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,\n" +
                                                               "    `title` varchar(255) NOT NULL,\n" +
                                                               "    PRIMARY KEY(`id`)\n" +
                                                               "}")
                                              .projectsUsing(Set.of("io.inugami:example:1.0.0:jar","io.inugami:consumer:1.5.0:jar"))
                                              .build(),
                                     FlywayDTO.builder()
                                              .id("06a40c4bcf198ed11529d45ec240d2e991962dd4952c2827f2eb8bd4e7e8965212d4f97406bf1928f6ff151ba429cbf8c8ade5c6ae343b57b5cf459c783aa347")
                                              .type("postgresql")
                                              .name("v1_0_0_init_issue_table.sql")
                                              .content("create table issue{\n" +
                                                               "    id integer unsigned NOT NULL AUTO_INCREMENT,\n" +
                                                               "    title text NOT NULL,\n" +
                                                               "    PRIMARY KEY(`id`)\n" +
                                                               "}")
                                              .projectsUsing(Set.of("io.inugami:example:1.0.0:jar"))
                                              .build(),
                                     FlywayDTO.builder()
                                              .id("a085184be08ff92f51362ad622cd506d7e9cf1b8fd611ab4a6c7d860d21674b013c4025833d8833d4efb5b270a9a34076c80cecb6ddf55058913f41be14cbcc2")
                                              .type("mysql")
                                              .name("v1_0_1_add_description_column.sql")
                                              .content("alter table issue\n" +
                                                               "    add column description varchar(255) null;")
                                              .projectsUsing(Set.of("io.inugami:example:1.0.0:jar"))
                                              .build()
                                                                   ), null)
                            );


        LinkedHashMap<String, String> result = writer.rendering(data, false, context);
        assertText(result, loadJsonReference("info/release/note/writers/asciidoc/flyway-result.json"));
    }
}