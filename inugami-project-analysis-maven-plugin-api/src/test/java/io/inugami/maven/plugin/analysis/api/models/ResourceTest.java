package io.inugami.maven.plugin.analysis.api.models;

import io.inugami.commons.test.dto.AssertDtoContext;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static io.inugami.commons.test.UnitTestData.OTHER;
import static io.inugami.commons.test.UnitTestHelper.assertDto;
import static org.assertj.core.api.Assertions.assertThat;

class ResourceTest {

    @Test
    void resource() {
        assertDto(new AssertDtoContext<Resource>().toBuilder()
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


    static void notEquals(final Resource instance) {
        assertThat(instance).isNotEqualTo(instance.toBuilder());
        assertThat(instance.hashCode()).isNotEqualTo(instance.toBuilder().hashCode());

        //
        assertThat(instance).isNotEqualTo(instance.toBuilder().target(null).build());
        assertThat(instance.toBuilder().target(null).build()).isNotEqualTo(instance);
        assertThat(instance).isNotEqualTo(instance.toBuilder().target(OTHER).build());
        assertThat(instance.toBuilder().target(OTHER).build()).isNotEqualTo(instance);
        assertThat(instance.hashCode()).isNotEqualTo(instance.toBuilder().target(null).build().hashCode());
        assertThat(instance.toBuilder().target(OTHER).build().hashCode()).isNotEqualTo(instance.hashCode());
        //
        assertThat(instance).isNotEqualTo(instance.toBuilder().path(null).build());
        assertThat(instance.toBuilder().path(null).build()).isNotEqualTo(instance);
        assertThat(instance).isNotEqualTo(instance.toBuilder().path(OTHER).build());
        assertThat(instance.toBuilder().path(OTHER).build()).isNotEqualTo(instance);
        assertThat(instance.hashCode()).isNotEqualTo(instance.toBuilder().path(null).build().hashCode());
        assertThat(instance.toBuilder().path(OTHER).build().hashCode()).isNotEqualTo(instance.hashCode());
        //
        assertThat(instance).isNotEqualTo(instance.toBuilder().gav(null).build());
        assertThat(instance.toBuilder().gav(null).build()).isNotEqualTo(instance);
        assertThat(instance).isNotEqualTo(instance.toBuilder().gav(OTHER).build());
        assertThat(instance.toBuilder().gav(OTHER).build()).isNotEqualTo(instance);
        assertThat(instance.hashCode()).isNotEqualTo(instance.toBuilder().gav(null).build().hashCode());
        assertThat(instance.toBuilder().gav(OTHER).build().hashCode()).isNotEqualTo(instance.hashCode());
    }

    public static Resource buildDataSet() {
        return Resource.builder()
                       .target("target")
                       .path("/some/path")
                       .gav("io.inugami:some-artifact")
                       .includes(List.of(IncludeTest.buildDataSet()))
                       .excludes(List.of(ExcludeTest.buildDataSet()))
                       .filtering(true)
                       .properties(null)
                       .properties(Map.ofEntries(Map.entry("date", "2023-05-21")))
                       .property(null, OTHER)
                       .property(OTHER, null)
                       .property("color", "red")
                       .build()
                       .toBuilder()
                       .build();
    }
}