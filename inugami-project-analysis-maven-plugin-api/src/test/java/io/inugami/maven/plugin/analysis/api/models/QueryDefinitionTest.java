package io.inugami.maven.plugin.analysis.api.models;

import io.inugami.commons.test.dto.AssertDtoContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.inugami.commons.test.UnitTestHelper.assertDto;
import static org.assertj.core.api.Assertions.assertThat;

class QueryDefinitionTest {

    @Test
    void queryDefinition() {
        assertDto(new AssertDtoContext<QueryDefinition>()
                          .toBuilder()
                          .objectClass(QueryDefinition.class)
                          .fullArgConstructorRefPath("api/models/queryDefinitionTest/fullArgConstructorRefPath.json")
                          .getterRefPath("api/models/queryDefinitionTest/getterRefPath.json")
                          .toStringRefPath("api/models/queryDefinitionTest/toStringRefPath.txt")
                          .cloneFunction(instance -> instance.toBuilder().build())
                          .noArgConstructor(() -> new QueryDefinition())
                          .fullArgConstructor(QueryDefinitionTest::buildDataSet)
                          .noEqualsFunction(QueryDefinitionTest::notEquals)
                          .checkSetters(true)
                          .build());
    }


    static void notEquals(final QueryDefinition value) {
        assertThat(value).isNotEqualTo(value.toBuilder());
        assertThat(value.hashCode()).isNotEqualTo(value.toBuilder().hashCode());

        assertThat(value).isNotEqualTo(value.toBuilder().type("other").build());
        assertThat(value.hashCode()).isNotEqualTo(value.toBuilder().type("other").build().hashCode());

        assertThat(value).isNotEqualTo(value.toBuilder().path("other").build());
        assertThat(value.hashCode()).isNotEqualTo(value.toBuilder().path("other").build().hashCode());
    }

    public static QueryDefinition buildDataSet() {
        return QueryDefinition.builder()
                              .type("cql")
                              .name("search_release_note_simple")
                              .path("META-INF/queries/search_release_note_simple.cql")
                              .description("Allow to retrieve release note information")
                              .parameters(List.of("param"))
                              .build();
    }
}