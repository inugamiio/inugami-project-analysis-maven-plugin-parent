package io.inugami.maven.plugin.analysis.api.models;

import io.inugami.commons.test.dto.AssertDtoContext;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static io.inugami.commons.test.UnitTestHelper.assertDto;
import static org.assertj.core.api.Assertions.assertThat;

class ResourceTest {

    @Test
    void resource() {
        assertDto(new AssertDtoContext<Resource>()
                          .toBuilder()
                          .objectClass(Resource.class)
                          .fullArgConstructorRefPath("api/models/resourceTest/fullArgConstructorRefPath.json")
                          .getterRefPath("api/models/resourceTest/getterRefPath.json")
                          .toStringRefPath("api/models/resourceTest/toStringRefPath.txt")
                          .cloneFunction(instance -> instance.toBuilder().build())
                          .noArgConstructor(() -> new Resource())
                          .fullArgConstructor(ResourceTest::buildDataSet)
                          .noEqualsFunction(ResourceTest::notEquals)
                          .checkSetters(true)
                          .build());
    }


    static void notEquals(final Resource value) {
        assertThat(value).isNotEqualTo(value.toBuilder());
        assertThat(value.hashCode()).isNotEqualTo(value.toBuilder().hashCode());

        assertThat(value).isNotEqualTo(value.toBuilder().target("other").build());
        assertThat(value.hashCode()).isNotEqualTo(value.toBuilder().target("other").build().hashCode());

        assertThat(value).isNotEqualTo(value.toBuilder().path("other").build());
        assertThat(value.hashCode()).isNotEqualTo(value.toBuilder().path("other").build().hashCode());

        assertThat(value).isNotEqualTo(value.toBuilder().gav("other").build());
        assertThat(value.hashCode()).isNotEqualTo(value.toBuilder().gav("other").build().hashCode());
    }

    public static Resource buildDataSet() {
        return Resource.builder()
                       .target("target")
                       .path("/some/path")
                       .gav("io.inugami:some-artifact")
                       .includes(List.of(IncludeTest.buildDataSet()))
                       .excludes(List.of(ExcludeTest.buildDataSet()))
                       .filtering(true)
                       .properties(Map.ofEntries(Map.entry("date", "2023-05-21")))
                       .property("color", "red")
                       .build();
    }
}