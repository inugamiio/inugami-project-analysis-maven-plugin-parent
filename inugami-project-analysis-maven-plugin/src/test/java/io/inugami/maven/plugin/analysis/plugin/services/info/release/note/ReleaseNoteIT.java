package io.inugami.maven.plugin.analysis.plugin.services.info.release.note;

import io.inugami.api.processors.ConfigHandler;
import io.inugami.configuration.services.ConfigHandlerHashMap;
import org.apache.maven.project.MavenProject;

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
        when(project.getBasedir()).thenReturn(new File(".").getAbsoluteFile().getParentFile());

        final ConfigHandler<String, String> config = new ConfigHandlerHashMap();
        config.put("inugami.maven.plugin.analysis.writer.neo4j.url", "bolt://localhost:7687");
        config.put("inugami.maven.plugin.analysis.writer.neo4j.password", "password");
        config.put("inugami.maven.plugin.analysis.writer.neo4j.user", "neo4j");
        config.put(ReleaseNote.REPLACEMENTS, "[{\"from\":\"(.*)(@gmail.com)\",\"to\":\"$1@inugami.io\"},{\"from\":\"(.*)(Wiedza CI)(.*)\",\"to\":\"$1InugamiCi$3\"}]");

        final ReleaseNote                   display       = new ReleaseNote();
        display.process(project, config);
    }


}