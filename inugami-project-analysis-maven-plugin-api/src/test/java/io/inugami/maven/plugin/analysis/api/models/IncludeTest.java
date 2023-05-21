package io.inugami.maven.plugin.analysis.api.models;

import io.inugami.commons.test.dto.AssertDtoContext;
import org.junit.jupiter.api.Test;

import static io.inugami.commons.test.UnitTestHelper.assertDto;
import static org.assertj.core.api.Assertions.assertThat;

class IncludeTest {

    @Test
    void include() {
        assertDto(new AssertDtoContext<Include>()
                          .toBuilder()
                          .objectClass(Include.class)
                          .fullArgConstructorRefPath("api/models/IncludeTest/fullArgConstructorRefPath.json")
                          .getterRefPath("api/models/IncludeTest/getterRefPath.json")
                          .toStringRefPath("api/models/IncludeTest/toStringRefPath.txt")
                          .cloneFunction(instance -> instance.toBuilder().build())
                          .noArgConstructor(() -> new Include())
                          .fullArgConstructor(IncludeTest::buildDataSet)
                          .noEqualsFunction(IncludeTest::notEquals)
                          .checkSetters(true)
                          .build());
    }

    static void notEquals(final Include value) {
        assertThat(value).isNotEqualTo(value.toBuilder());
        assertThat(value.hashCode()).isNotEqualTo(value.toBuilder().hashCode());

        assertThat(value).isNotEqualTo(value.toBuilder().include("other").build());
        assertThat(value.hashCode()).isNotEqualTo(value.toBuilder().include("other").build().hashCode());
    }

    public static Include buildDataSet() {
        return Include.builder()
                      .include("value")
                      .build();
    }
}