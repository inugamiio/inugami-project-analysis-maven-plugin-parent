package io.inugami.maven.plugin.analysis.plugin.services.info.release.note;

import io.inugami.api.processors.ConfigHandler;
import io.inugami.commons.files.FilesUtils;
import io.inugami.configuration.services.ConfigHandlerHashMap;
import io.inugami.maven.plugin.analysis.api.models.InfoContext;
import io.inugami.maven.plugin.analysis.plugin.services.info.release.note.writers.asciidoc.ReleaseNoteAsciidocWriter;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcher;

import java.io.File;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReleaseNoteIT {


    public static void main(final String... args) {

        final String[]     sysReplacements = getReplaceValues().split(";");
        final MavenProject project         = mock(MavenProject.class);
        when(project.getGroupId()).thenReturn("io.inugami.demo");
        when(project.getArtifactId()).thenReturn("spring-boot-training-rest");
        when(project.getVersion()).thenReturn("0.0.2-SNAPSHOT");
        when(project.getPackaging()).thenReturn("jar");
        when(project.getDescription()).thenReturn("Basic springboot training application");
        when(project.getUrl()).thenReturn("https://github.com/inugamiio/inugami-plugin-dashboard-demo");
        when(project.getBasedir()).thenReturn(new File(".").getAbsoluteFile().getParentFile());

        final ConfigHandler<String, String> config = new ConfigHandlerHashMap();
        config.put("inugami.maven.plugin.analysis.writer.neo4j.url", "bolt://localhost:7687");
        config.put("inugami.maven.plugin.analysis.writer.neo4j.password", "password");
        config.put("inugami.maven.plugin.analysis.writer.neo4j.user", "neo4j");
        config.put(ReleaseNoteAsciidocWriter.FEATURE_NAME, "true");
        //config.put(PREVIOUS_VERSION, "0.0.1-SNAPSHOT");
        //config.put("io.inugami.maven.plugin.analysis.asciidoc.authors.enabled", "false");
        //config.put("io.inugami.maven.plugin.analysis.asciidoc.errorCode.enabled", "false");

        if (sysReplacements.length >= 5) {
            config.put(ReleaseNote.REPLACEMENTS, "[\n" +
                    "    {\"from\":\"(.*)(@gmail.com)\",\"to\":\"$1@inugami.io\"},\n" +
                    "    {\"from\":\"(.*)(" + sysReplacements[0] + ")(.*)\",\"to\":\"$1InugamiCi$3\"},\n" +
                    "    {\"from\":\"(.*)(" + sysReplacements[1] + ")(.*)\",\"to\":\"$1inugami.io$3\"},\n" +
                    "    {\"from\":\"(.*)(" + sysReplacements[2] + ")(.*)\",\"to\":\"$1joefoobar$3\"},\n" +
                    "    {\"from\":\"(.*)(" + sysReplacements[3] + ")(.*)\",\"to\":\"$1Joe Foobar$3\"},\n" +
                    "    {\"from\":\"(.*)(" + sysReplacements[4] + ")(.*)\",\"to\":\"$1John Smith$3\"},\n" +
                    "    {\"from\":\"(.*)(" + sysReplacements[1] + ")(.*)\",\"to\":\"$1inugami.io$3\"}\n" +
                    "]");
        }


        final ReleaseNote display = new ReleaseNote();

        final InfoContext context = InfoContext.builder()
                                               .basedir(project.getBasedir())
                                               .buildDir(FilesUtils.buildFile(project.getBasedir(), "target"))
                                               .project(project)
                                               .configuration(config)
                                               .secDispatcher(mock(SecDispatcher.class))
                                               .artifactHandler(mock(ArtifactHandler.class))
                                               .settings(mock(Settings.class))
                                               .build();
        display.process(context);
    }

    private static String getReplaceValues() {
        final String value = System.getProperty("replaceValues");
        return value == null ? "" : value;
    }


}