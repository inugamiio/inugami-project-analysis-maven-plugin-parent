package io.inugami.maven.plugin.analysis.api.models;

import io.inugami.commons.test.dto.AssertDtoContext;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.VersionRange;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.util.List;

import static io.inugami.commons.test.UnitTestData.OTHER;
import static io.inugami.commons.test.UnitTestHelper.assertDto;
import static io.inugami.commons.test.UnitTestHelper.assertTextRelative;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class MavenGavTest {
    @Mock
    private ArtifactMetadata   artifactMetadata;
    @Mock
    private ArtifactRepository artifactRepository;
    @Mock
    private ArtifactFilter     artifactFilter;
    @Mock
    private ArtifactHandler    artifactHandler;
    @Mock
    private VersionRange       versionRange;
    @Mock
    private ArtifactVersion    artifactVersion;

    @Test
    void mavenGav() {
        assertDto(new AssertDtoContext<MavenGav>().toBuilder()
                                                  .objectClass(MavenGav.class)
                                                  .fullArgConstructorRefPath("api/models/mavenGav/model.json")
                                                  .getterRefPath("api/models/mavenGav/getters.json")
                                                  .toStringRefPath("api/models/mavenGav/toString.txt")
                                                  .cloneFunction(instance -> instance.toBuilder().build())
                                                  .noArgConstructor(MavenGav::new)
                                                  .fullArgConstructor(this::buildDataSet)
                                                  .noEqualsFunction(this::notEquals)
                                                  .checkSetters(true)
                                                  .build());
    }

    @Test
    void hasClassifier_nominal() {
        assertThat(buildDataSet().hasClassifier()).isTrue();
    }

    @Test
    void addMetadata_nominal() {
        final MavenGav result = buildDataSet().toBuilder().metadataList(null).build();
        result.addMetadata(artifactMetadata);
        assertTextRelative(result, "api/models/mavenGav/model.json");
        result.addMetadata(null);
        assertTextRelative(result, "api/models/mavenGav/model.json");
    }


    @Test
    void unimplementedMethod_nominal() {
        final MavenGav value = buildDataSet();
        value.updateVersion(null, null);
        value.selectVersion(null);
        value.setResolvedVersion(null);
        value.setRelease(true);
        assertTextRelative(value, "api/models/mavenGav/model.json");
    }

    @Test
    void isSnapshot_nominal() {
        final MavenGav value = buildDataSet();
        assertThat(value.isSnapshot()).isFalse();
        assertThat(value.isRelease()).isTrue();

        assertThat(value.toBuilder().version("1.0.0-SNAPSHOT").build().isSnapshot()).isTrue();
        assertThat(value.toBuilder().version("1.0.0-SNAPSHOT").build().isRelease()).isFalse();

        assertThat(value.toBuilder().version(null).build().isSnapshot()).isFalse();
        assertThat(value.toBuilder().version(null).build().isRelease()).isTrue();
    }

    @Test
    void compareTo_nominal() {
        final MavenGav value = buildDataSet();

        assertThat(value.compareTo(null)).isEqualTo(-1);
        assertThat(value.compareTo(buildDataSet())).isEqualTo(-4);
        assertThat(value.compareTo(buildDataSet().toBuilder().groupId("aaa").build())).isEqualTo(8);
        assertThat(value.compareTo(buildDataSet().toBuilder().groupId("zzzs").build())).isEqualTo(-17);
    }

    void notEquals(final MavenGav instance) {
        assertThat(instance).isNotEqualTo(instance.toBuilder());
        assertThat(instance.hashCode()).isNotEqualTo(instance.toBuilder().hashCode());

        //
        assertThat(instance).isNotEqualTo(instance.toBuilder().groupId(null).build());
        assertThat(instance.toBuilder().groupId(null).build()).isNotEqualTo(instance);
        assertThat(instance).isNotEqualTo(instance.toBuilder().groupId(OTHER).build());
        assertThat(instance.toBuilder().groupId(OTHER).build()).isNotEqualTo(instance);
        assertThat(instance.hashCode()).isNotEqualTo(instance.toBuilder().groupId(null).build().hashCode());
        assertThat(instance.toBuilder().groupId(OTHER).build().hashCode()).isNotEqualTo(instance.hashCode());
        //
        assertThat(instance).isNotEqualTo(instance.toBuilder().artifactId(null).build());
        assertThat(instance.toBuilder().artifactId(null).build()).isNotEqualTo(instance);
        assertThat(instance).isNotEqualTo(instance.toBuilder().artifactId(OTHER).build());
        assertThat(instance.toBuilder().artifactId(OTHER).build()).isNotEqualTo(instance);
        assertThat(instance.hashCode()).isNotEqualTo(instance.toBuilder().artifactId(null).build().hashCode());
        assertThat(instance.toBuilder().artifactId(OTHER).build().hashCode()).isNotEqualTo(instance.hashCode());
        //
        assertThat(instance).isNotEqualTo(instance.toBuilder().version(null).build());
        assertThat(instance.toBuilder().version(null).build()).isNotEqualTo(instance);
        assertThat(instance).isNotEqualTo(instance.toBuilder().version(OTHER).build());
        assertThat(instance.toBuilder().version(OTHER).build()).isNotEqualTo(instance);
        assertThat(instance.hashCode()).isNotEqualTo(instance.toBuilder().version(null).build().hashCode());
        assertThat(instance.toBuilder().version(OTHER).build().hashCode()).isNotEqualTo(instance.hashCode());
        //
        assertThat(instance).isNotEqualTo(instance.toBuilder().packaging(null).build());
        assertThat(instance.toBuilder().packaging(null).build()).isNotEqualTo(instance);
        assertThat(instance).isNotEqualTo(instance.toBuilder().packaging(OTHER).build());
        assertThat(instance.toBuilder().packaging(OTHER).build()).isNotEqualTo(instance);
        assertThat(instance.hashCode()).isNotEqualTo(instance.toBuilder().packaging(null).build().hashCode());
        assertThat(instance.toBuilder().packaging(OTHER).build().hashCode()).isNotEqualTo(instance.hashCode());
        //
        assertThat(instance).isNotEqualTo(instance.toBuilder().classifier(null).build());
        assertThat(instance.toBuilder().classifier(null).build()).isNotEqualTo(instance);
        assertThat(instance).isNotEqualTo(instance.toBuilder().classifier(OTHER).build());
        assertThat(instance.toBuilder().classifier(OTHER).build()).isNotEqualTo(instance);
        assertThat(instance.hashCode()).isNotEqualTo(instance.toBuilder().classifier(null).build().hashCode());
        assertThat(instance.toBuilder().classifier(OTHER).build().hashCode()).isNotEqualTo(instance.hashCode());
    }

    private MavenGav buildDataSet() {
        return MavenGav.builder()
                       .groupId("io.inugami")
                       .artifactId("unit-test-artifact")
                       .version("1.0.0")
                       .packaging("jar")
                       .type("jar")
                       .scope("compile")
                       .classifier("uber")
                       .file(new File("/dev/artifact.jar"))
                       .baseVersion("1.0.0")
                       .id("io.inugami:unit-test-artifact:1.0.0:uber:jar")
                       .metadataList(List.of(artifactMetadata))
                       .repository(artifactRepository)
                       .dependencyConflictId("none")
                       .downloadUrl("http://mock.inugami.io")
                       .dependencyFilter(artifactFilter)
                       .artifactHandler(artifactHandler)
                       .dependencyTrail(List.of("trail"))
                       .versionRange(versionRange)
                       .resolved(true)
                       .optional(true)
                       .availableVersions(List.of(artifactVersion))
                       .selectedVersion(artifactVersion)
                       .build()
                       .toBuilder()
                       .build();

    }
}