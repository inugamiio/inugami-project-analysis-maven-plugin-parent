package io.inugami.maven.plugin.analysis.api.models;

import io.inugami.commons.test.dto.AssertDtoContext;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.inugami.commons.test.UnitTestHelper.assertDto;
import static org.assertj.core.api.Assertions.assertThat;

class NodeTest {

    @Test
    void node() {
        assertDto(new AssertDtoContext<Node>()
                          .toBuilder()
                          .objectClass(Node.class)
                          .fullArgConstructorRefPath("api/models/nodeTest/fullArgConstructorRefPath.json")
                          .getterRefPath("api/models/nodeTest/getterRefPath.json")
                          .toStringRefPath("api/models/nodeTest/toStringRefPath.txt")
                          .cloneFunction(instance -> instance.toBuilder().build())
                          .noArgConstructor(() -> new Node())
                          .fullArgConstructor(NodeTest::buildDataSet)
                          .noEqualsFunction(NodeTest::notEquals)
                          .checkSetters(true)
                          .build());
    }


    @Test
    void compareTo_nominal() {
        assertThat(buildDataSet().compareTo(null)).isEqualTo(-1);
        assertThat(buildDataSet().compareTo(buildDataSet())).isZero();
        assertThat(buildDataSet().compareTo(buildDataSet().toBuilder()
                                                          .type("AA")
                                                          .build()))
                .isOne();

        assertThat(buildDataSet().compareTo(buildDataSet().toBuilder()
                                                          .type("ZZ")
                                                          .build()))
                .isEqualTo(-1);

    }

    static void notEquals(final Node value) {
        assertThat(value).isNotEqualTo(value.toBuilder());
        assertThat(value.hashCode()).isNotEqualTo(value.toBuilder().hashCode());

        assertThat(value).isNotEqualTo(value.toBuilder().uid("other").build());
        assertThat(value.hashCode()).isNotEqualTo(value.toBuilder().uid("other").build().hashCode());
    }

    public static Node buildDataSet() {
        return Node.builder()
                   .type("Version")
                   .name("artifact")
                   .uid("uid")
                   .properties(Map.ofEntries(Map.entry("date", "2023-05-21")))
                   .addProperty("color", "blue")
                   .build();
    }
}