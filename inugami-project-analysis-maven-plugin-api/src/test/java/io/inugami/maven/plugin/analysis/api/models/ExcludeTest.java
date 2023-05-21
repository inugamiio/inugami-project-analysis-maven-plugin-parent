package io.inugami.maven.plugin.analysis.api.models;

import io.inugami.commons.test.dto.AssertDtoContext;
import org.junit.jupiter.api.Test;

import static io.inugami.commons.test.UnitTestHelper.assertDto;
import static org.assertj.core.api.Assertions.assertThat;

class ExcludeTest {

    @Test
    void exclude() {
        assertDto(new AssertDtoContext<Exclude>()
                          .toBuilder()
                          .objectClass(Exclude.class)
                          .fullArgConstructorRefPath("api/models/excludeTest/fullArgConstructorRefPath.json")
                          .getterRefPath("api/models/excludeTest/getterRefPath.json")
                          .toStringRefPath("api/models/excludeTest/toStringRefPath.txt")
                          .cloneFunction(instance -> instance.toBuilder().build())
                          .noArgConstructor(() -> new Exclude())
                          .fullArgConstructor(ExcludeTest::buildDataSet)
                          .noEqualsFunction(ExcludeTest::notEquals)
                          .checkSetters(true)
                          .build());
    }

    static void notEquals(final Exclude value) {
        assertThat(value).isNotEqualTo(value.toBuilder());
        assertThat(value.hashCode()).isNotEqualTo(value.toBuilder().hashCode());

        assertThat(value).isNotEqualTo(value.toBuilder().exclude("other").build());
        assertThat(value.hashCode()).isNotEqualTo(value.toBuilder().exclude("other").build().hashCode());
    }

    public static Exclude buildDataSet() {
        return Exclude.builder()
                      .exclude("some value")
                      .build();
    }

}