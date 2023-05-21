package io.inugami.maven.plugin.analysis.api.models;

import io.inugami.commons.test.dto.AssertDtoContext;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.inugami.commons.test.UnitTestHelper.assertDto;
import static org.assertj.core.api.Assertions.assertThat;

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
                          .noArgConstructor(() -> new Relationship())
                          .fullArgConstructor(RelationshipTest::buildDataSet)
                          .noEqualsFunction(RelationshipTest::notEquals)
                          .checkSetters(true)
                          .build());
    }


    static void notEquals(final Relationship value) {
        assertThat(value).isNotEqualTo(value.toBuilder());
        assertThat(value.hashCode()).isNotEqualTo(value.toBuilder().hashCode());

        assertThat(value).isNotEqualTo(value.toBuilder().type("other").build());
        assertThat(value.hashCode()).isNotEqualTo(value.toBuilder().type("other").build().hashCode());

        assertThat(value).isNotEqualTo(value.toBuilder().from("other").build());
        assertThat(value.hashCode()).isNotEqualTo(value.toBuilder().from("other").build().hashCode());

        assertThat(value).isNotEqualTo(value.toBuilder().to("other").build());
        assertThat(value.hashCode()).isNotEqualTo(value.toBuilder().to("other").build().hashCode());
    }

    public static Relationship buildDataSet() {
        return Relationship.builder()
                           .type("has_relationship")
                           .from("nodeA")
                           .to("nodeB")
                           .properties(Map.ofEntries(Map.entry("date", "2023-05-21")))
                           .property("color", "red")
                           .build();
    }
}