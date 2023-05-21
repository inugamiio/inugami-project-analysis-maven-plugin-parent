package io.inugami.maven.plugin.analysis.api.models;

import io.inugami.commons.test.dto.AssertDtoContext;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static io.inugami.commons.test.UnitTestHelper.assertDto;
import static org.assertj.core.api.Assertions.assertThat;

class GavTest {

    @Test
    void gav() {
        assertDto(new AssertDtoContext<Gav>()
                          .toBuilder()
                          .objectClass(Gav.class)
                          .fullArgConstructorRefPath("api/models/gavTest/fullArgConstructorRefPath.json")
                          .getterRefPath("api/models/gavTest/getterRefPath.json")
                          .toStringRefPath("api/models/gavTest/toStringRefPath.txt")
                          .cloneFunction(instance -> instance.toBuilder().build())
                          .noArgConstructor(() -> new Gav())
                          .fullArgConstructor(GavTest::buildDataSet)
                          .noEqualsFunction(GavTest::notEquals)
                          .checkSetters(true)
                          .build());
    }

    static void notEquals(final Gav value) {
        assertThat(value).isNotEqualTo(value.toBuilder());
        assertThat(value.hashCode()).isNotEqualTo(value.toBuilder().hashCode());

        assertThat(value).isNotEqualTo(value.toBuilder().groupId("other").build());
        assertThat(value.hashCode()).isNotEqualTo(value.toBuilder().groupId("other").build().hashCode());

        assertThat(value).isNotEqualTo(value.toBuilder().artifactId("other").build());
        assertThat(value.hashCode()).isNotEqualTo(value.toBuilder().artifactId("other").build().hashCode());

        assertThat(value).isNotEqualTo(value.toBuilder().version("other").build());
        assertThat(value.hashCode()).isNotEqualTo(value.toBuilder().version("other").build().hashCode());

        assertThat(value).isNotEqualTo(value.toBuilder().type("other").build());
        assertThat(value.hashCode()).isNotEqualTo(value.toBuilder().type("other").build().hashCode());
    }

    public static Gav buildDataSet() {
        return Gav.builder()
                  .groupId("io.inugami")
                  .artifactId("some-artifact")
                  .version("1.0.0")
                  .type("jar")
                  .scope("compile")
                  .dependencies(Set.of(Gav.builder()
                                          .groupId("io.inugami")
                                          .artifactId("other-artifact")
                                          .version("1.0.0")
                                          .type("jar")
                                          .scope("compile")
                                          .build()))
                  .build();
    }
}