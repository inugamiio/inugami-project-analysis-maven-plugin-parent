package io.inugami.maven.plugin.analysis.plugin.mojo;

import io.inugami.maven.plugin.analysis.api.models.MavenGav;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.OverConstrainedVersionException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;
import org.apache.maven.settings.Settings;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.impl.ArtifactResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcher;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static io.inugami.commons.test.UnitTestHelper.assertTextRelative;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class CheckMojoTest {
    // =========================================================================
    // ATTRIBUTES
    // =========================================================================
    @Mock
    private Set<String>             resourcePackages;
    @Mock
    private File                    basedir;
    @Mock
    private MavenProject            project;
    @Mock
    private RepositorySystem        repoSystem;
    @Mock
    private RepositorySystemSession repoSession;

    @Mock
    private PluginDescriptor   descriptor;
    @Mock
    private ArtifactRepository artifactRepository;
    @Mock
    private ArtifactResolver   artifactResolver;
    @Mock
    private PluginDescriptor   pluginDescriptor;
    @Mock
    private ArtifactHandler    artifactHandler;
    @Mock
    private Settings           settings;
    @Mock
    private SecDispatcher      secDispatcher;
    @Mock
    private Set<String>        excludedJars;

    // =========================================================================
    // TEST
    // =========================================================================
    @Test
    void initExcluded_nominal() {
        final CheckMojo mojo = buildMojo();
        mojo.initExcluded();
        assertTextRelative(mojo.getExcluded(), "plugin/mojo/checkMojoTest/initExcluded_nominal.json");

        mojo.setExcludedJars(Set.of(".*joe.*"));
        mojo.initExcluded();
        assertTextRelative(mojo.getExcluded(), "plugin/mojo/checkMojoTest/initExcluded_nominal.2.json");
    }

    @Test
    void isExcludedJar_nominal() {
        final CheckMojo mojo = buildMojo();
        mojo.initExcluded();
        assertThat(mojo.isExcludedJar(MavenGav.builder()
                                              .groupId("org.springframework")
                                              .artifactId("spring-web")
                                              .build())).isTrue();

        assertThat(mojo.isExcludedJar(MavenGav.builder()
                                              .groupId("io.inugami")
                                              .artifactId("inugami-core")
                                              .build())).isFalse();
    }

    // =========================================================================
    // TOOLS
    // =========================================================================
    CheckMojo buildMojo() {
        return CheckMojo.builder()
                        .resourcePackages(resourcePackages)
                        .basedir(basedir)
                        .project(project)
                        .repoSystem(repoSystem)
                        .repoSession(repoSession)
                        .artifactRepository(artifactRepository)
                        .descriptor(descriptor)
                        .artifactResolver(artifactResolver)
                        .pluginDescriptor(pluginDescriptor)
                        .artifactHandler(artifactHandler)
                        .settings(settings)
                        .secDispatcher(secDispatcher)
                        .build();
    }


}