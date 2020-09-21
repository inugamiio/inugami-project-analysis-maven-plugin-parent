package io.inugami.maven.plugin.analysis.api.actions;


import io.inugami.api.processors.ConfigHandler;
import io.inugami.api.spi.NamedSpi;
import io.inugami.maven.plugin.analysis.api.models.Gav;
import org.apache.maven.project.MavenProject;

public interface ProjectInformation extends NamedSpi {

    void process(MavenProject project, ConfigHandler<String, String> configuration);

    default void shutdown() {
    }

    default Gav convertMavenProjectToGav(final MavenProject project) {
        return project == null ? null : Gav.builder()
                                           .groupId(project.getGroupId())
                                           .artifactId(project.getArtifactId())
                                           .version(project.getVersion())
                                           .type(project.getPackaging())
                                           .build();
    }
}
