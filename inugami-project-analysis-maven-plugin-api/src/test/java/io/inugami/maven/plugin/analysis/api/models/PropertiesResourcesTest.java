package io.inugami.maven.plugin.analysis.api.models;

import io.inugami.commons.test.dto.AssertDtoContext;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.inugami.commons.test.UnitTestData.OTHER;
import static io.inugami.commons.test.UnitTestHelper.assertDto;
import static io.inugami.commons.test.UnitTestHelper.assertTextRelative;
import static org.assertj.core.api.Assertions.assertThat;

class PropertiesResourcesTest {

    @Test
    void propertiesResources() {
        assertDto(new AssertDtoContext<PropertiesResources>().toBuilder()
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


    static void notEquals(final PropertiesResources instance) {
        assertThat(instance).isNotEqualTo(instance.toBuilder());
        assertThat(instance.hashCode()).isNotEqualTo(instance.toBuilder().hashCode());

        //
        assertThat(instance).isNotEqualTo(instance.toBuilder().type(null).build());
        assertThat(instance.toBuilder().type(null).build()).isNotEqualTo(instance);
        assertThat(instance).isNotEqualTo(instance.toBuilder().type(OTHER).build());
        assertThat(instance.toBuilder().type(OTHER).build()).isNotEqualTo(instance);
        assertThat(instance.hashCode()).isNotEqualTo(instance.toBuilder().type(null).build().hashCode());
        assertThat(instance.toBuilder().type(OTHER).build().hashCode()).isNotEqualTo(instance.hashCode());
        //
        assertThat(instance).isNotEqualTo(instance.toBuilder().propertiesPath(null).build());
        assertThat(instance.toBuilder().propertiesPath(null).build()).isNotEqualTo(instance);
        assertThat(instance).isNotEqualTo(instance.toBuilder().propertiesPath(OTHER).build());
        assertThat(instance.toBuilder().propertiesPath(OTHER).build()).isNotEqualTo(instance);
        assertThat(instance.hashCode()).isNotEqualTo(instance.toBuilder().propertiesPath(null).build().hashCode());
        assertThat(instance.toBuilder().propertiesPath(OTHER).build().hashCode()).isNotEqualTo(instance.hashCode());
    }

    @Test
    void properties_nominal() {
        final PropertiesResources result = PropertiesResources.builder()
                                                              .properties(Map.ofEntries(Map.entry("date", "2023-05-21")))
                                                              .properties(Map.ofEntries(Map.entry("date", "2023-05-21")))
                                                              .build();
        assertTextRelative(result, "api/models/PropertiesResourcesTest/properties_nominal.1.json");


        assertTextRelative(PropertiesResources.builder()
                                              .properties(null)
                                              .build(),
                           "api/models/PropertiesResourcesTest/addProperty_nominal.2.json");
    }


    @Test
    void addProperty_nominal() {

        assertTextRelative(PropertiesResources.builder()
                                              .addProperty("date", "2023-05-21")
                                              .build(),
                           "api/models/PropertiesResourcesTest/addProperty_nominal.1.json");

        assertTextRelative(PropertiesResources.builder()
                                              .addProperty(null, "2023-05-21")
                                              .build(),
                           "api/models/PropertiesResourcesTest/addProperty_nominal.2.json");
        assertTextRelative(PropertiesResources.builder()
                                              .addProperty("date", null)
                                              .build(),
                           "api/models/PropertiesResourcesTest/addProperty_nominal.2.json");
    }

    public static PropertiesResources buildDataSet() {
        return PropertiesResources.builder()
                                  .type("Version")
                                  .encoding("UTF-8")
                                  .propertiesPath("property/path")
                                  .propertiesUrl("http://property/path")
                                  .propertiesUrlAuthorization("admin:admin")
                                  .properties(null)
                                  .properties(Map.ofEntries(Map.entry("date", "2023-05-21")))
                                  .addProperty(null, null)
                                  .addProperty(null, OTHER)
                                  .addProperty(OTHER, null)
                                  .addProperty("color", "blue")
                                  .build()
                                  .toBuilder()
                                  .build();
    }
}