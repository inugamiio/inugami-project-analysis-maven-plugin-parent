package io.inugami.maven.plugin.analysis.api.models;

import io.inugami.commons.test.dto.AssertDtoContext;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.inugami.commons.test.UnitTestHelper.assertDto;
import static org.assertj.core.api.Assertions.assertThat;

class PropertiesResourcesTest {

    @Test
    void propertiesResources() {
        assertDto(new AssertDtoContext<PropertiesResources>()
                          .toBuilder()
                          .objectClass(PropertiesResources.class)
                          .fullArgConstructorRefPath("api/models/PropertiesResourcesTest/fullArgConstructorRefPath.json")
                          .getterRefPath("api/models/PropertiesResourcesTest/getterRefPath.json")
                          .toStringRefPath("api/models/PropertiesResourcesTest/toStringRefPath.txt")
                          .cloneFunction(instance -> instance.toBuilder().build())
                          .noArgConstructor(() -> new PropertiesResources())
                          .fullArgConstructor(PropertiesResourcesTest::buildDataSet)
                          .noEqualsFunction(PropertiesResourcesTest::notEquals)
                          .checkSetters(true)
                          .build());
    }


    static void notEquals(final PropertiesResources value) {
        assertThat(value).isNotEqualTo(value.toBuilder());
        assertThat(value.hashCode()).isNotEqualTo(value.toBuilder().hashCode());

        assertThat(value).isNotEqualTo(value.toBuilder().type("other").build());
        assertThat(value.hashCode()).isNotEqualTo(value.toBuilder().type("other").build().hashCode());

        assertThat(value).isNotEqualTo(value.toBuilder().propertiesPath("other").build());
        assertThat(value.hashCode()).isNotEqualTo(value.toBuilder().propertiesPath("other").build().hashCode());
    }

    public static PropertiesResources buildDataSet() {
        return PropertiesResources.builder()
                                  .type("Version")
                                  .encoding("UTF-8")
                                  .propertiesPath("property/path")
                                  .propertiesUrl("http://property/path")
                                  .propertiesUrlAuthorization("admin:admin")
                                  .properties(Map.ofEntries(Map.entry("date", "2023-05-21")))
                                  .addProperty("color", "blue")
                                  .build();
    }
}