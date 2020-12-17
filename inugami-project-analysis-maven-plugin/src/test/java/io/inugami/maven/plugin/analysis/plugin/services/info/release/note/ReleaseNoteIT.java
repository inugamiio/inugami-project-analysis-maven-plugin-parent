package io.inugami.maven.plugin.analysis.plugin.services.info.release.note;

import io.inugami.api.processors.ConfigHandler;
import io.inugami.commons.files.FilesUtils;
import io.inugami.configuration.services.ConfigHandlerHashMap;
import io.inugami.maven.plugin.analysis.api.models.InfoContext;
import io.inugami.maven.plugin.analysis.plugin.services.info.release.note.writers.ReleaseNoteAsciidocWriter;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcher;

import java.io.File;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReleaseNoteIT {


    public static void main(final String... args) {
        final MavenProject project = mock(MavenProject.class);
        when(project.getGroupId()).thenReturn("io.inugami.plugins");
        when(project.getArtifactId()).thenReturn("inugami-plugin-dashboard-demo");
        when(project.getVersion()).thenReturn("2.0.0-SNAPSHOT");
        when(project.getPackaging()).thenReturn("jar");
        when(project.getDescription()).thenReturn("Simple Inugami plugin");
        when(project.getUrl()).thenReturn("https://github.com/inugamiio/inugami-plugin-dashboard-demo");
        when(project.getBasedir()).thenReturn(new File(".").getAbsoluteFile().getParentFile());

        final ConfigHandler<String, String> config = new ConfigHandlerHashMap();
        config.put("inugami.maven.plugin.analysis.writer.neo4j.url", "bolt://localhost:7687");
        config.put("inugami.maven.plugin.analysis.writer.neo4j.password", "password");
        config.put("inugami.maven.plugin.analysis.writer.neo4j.user", "neo4j");
        config.put(ReleaseNoteAsciidocWriter.FEATURE_NAME, "true");
        config.put(ReleaseNote.REPLACEMENTS, "[{\"from\":\"(.*)(@gmail.com)\",\"to\":\"$1@inugami.io\"},{\"from\":\"(.*)(Wiedza CI)(.*)\",\"to\":\"$1InugamiCi$3\"}]");

        final ReleaseNote                   display       = new ReleaseNote();

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


}