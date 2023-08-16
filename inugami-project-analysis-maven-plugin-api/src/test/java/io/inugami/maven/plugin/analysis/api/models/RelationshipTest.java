package io.inugami.maven.plugin.analysis.api.models;

import io.inugami.commons.test.dto.AssertDtoContext;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.inugami.commons.test.UnitTestData.OTHER;
import static io.inugami.commons.test.UnitTestHelper.assertDto;
import static io.inugami.commons.test.UnitTestHelper.assertTextRelative;
import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings({"java:S5838"})
class RelationshipTest {

    @Test
    void relationship() {
        assertDto(new AssertDtoContext<Relationship>()
                          .toBuilder()
                          .objectClass(Relationship.class)
                          .fullArgConstructorRefPath("api/models/relationshipTest/fullArgConstructorRefPath.json")
                          .getterRefPath("api/models/relationshipTest/getterRefPath.json")
                          .toStringRefPath("api/models/relationshipTest/toStringRefPath.txt")
                          .cloneFunction(instance -> instance.toBuilder().build())
                          .noArgConstructor(Relationship::new)
                          .fullArgConstructor(RelationshipTest::buildDataSet)
                          .noEqualsFunction(RelationshipTest::notEquals)
                          .checkSetters(true)
                          .build());
    }

    @Test
    void compareTo_nominal() {
        assertThat(buildDataSet().compareTo(null)).isEqualTo(-1);
        assertThat(buildDataSet().compareTo(buildDataSet())).isEqualTo(0);
        assertThat(buildDataSet().compareTo(buildDataSet().toBuilder().to("aaa").build())).isEqualTo(1);
        assertThat(buildDataSet().compareTo(buildDataSet().toBuilder().to("zzz").build())).isEqualTo(-1);
    }

    @Test
    void buildHash_nominal() {
        assertThat(buildDataSet().buildHash()).isEqualTo("nodeA-[has_relationship]->nodeB");
    }

    static void notEquals(final Relationship instance) {
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
        assertThat(instance).isNotEqualTo(instance.toBuilder().from(null).build());
        assertThat(instance.toBuilder().from(null).build()).isNotEqualTo(instance);
        assertThat(instance).isNotEqualTo(instance.toBuilder().from(OTHER).build());
        assertThat(instance.toBuilder().from(OTHER).build()).isNotEqualTo(instance);
        assertThat(instance.hashCode()).isNotEqualTo(instance.toBuilder().from(null).build().hashCode());
        assertThat(instance.toBuilder().from(OTHER).build().hashCode()).isNotEqualTo(instance.hashCode());
        //
        assertThat(instance).isNotEqualTo(instance.toBuilder().to(null).build());
        assertThat(instance.toBuilder().to(null).build()).isNotEqualTo(instance);
        assertThat(instance).isNotEqualTo(instance.toBuilder().to(OTHER).build());
        assertThat(instance.toBuilder().to(OTHER).build()).isNotEqualTo(instance);
        assertThat(instance.hashCode()).isNotEqualTo(instance.toBuilder().to(null).build().hashCode());
        assertThat(instance.toBuilder().to(OTHER).build().hashCode()).isNotEqualTo(instance.hashCode());
    }

    @Test
    void properties_nominal() {
        final Relationship result = Relationship.builder()
                                                .properties(Map.ofEntries(Map.entry("date", "2023-05-21")))
                                                .properties(Map.ofEntries(Map.entry("date", "2023-05-21")))
                                                .build();
        assertTextRelative(result, "api/models/relationshipTest/properties_nominal.1.json");

        assertTextRelative(Relationship.builder()
                                       .properties(null)
                                       .build(),
                           "api/models/relationshipTest/properties_nominal.2.json");
    }


    @Test
    void addProperty_nominal() {

        assertTextRelative(Relationship.builder()
                                       .property("date", "2023-05-21")
                                       .build(),
                           "api/models/relationshipTest/addProperty_nominal.1.json");

        assertTextRelative(Relationship.builder()
                                       .property(null, "2023-05-21")
                                       .build(),
                           "api/models/relationshipTest/addProperty_nominal.2.json");
        assertTextRelative(Relationship.builder()
                                       .property("date", null)
                                       .build(),
                           "api/models/relationshipTest/addProperty_nominal.2.json");
    }


    public static Relationship buildDataSet() {
        return Relationship.builder()
                           .type("has_relationship")
                           .from("nodeA")
                           .to("nodeB")
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