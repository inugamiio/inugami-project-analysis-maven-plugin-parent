package io.inugami.maven.plugin.analysis.api.models;

import io.inugami.commons.test.dto.AssertDtoContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.inugami.commons.test.UnitTestData.OTHER;
import static io.inugami.commons.test.UnitTestHelper.assertDto;
import static org.assertj.core.api.Assertions.assertThat;

class QueryDefinitionTest {

    @Test
    void queryDefinition() {
        assertDto(new AssertDtoContext<QueryDefinition>().toBuilder()
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


    static void notEquals(final QueryDefinition instance) {
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
        assertThat(instance).isNotEqualTo(instance.toBuilder().path(null).build());
        assertThat(instance.toBuilder().path(null).build()).isNotEqualTo(instance);
        assertThat(instance).isNotEqualTo(instance.toBuilder().path(OTHER).build());
        assertThat(instance.toBuilder().path(OTHER).build()).isNotEqualTo(instance);
        assertThat(instance.hashCode()).isNotEqualTo(instance.toBuilder().path(null).build().hashCode());
        assertThat(instance.toBuilder().path(OTHER).build().hashCode()).isNotEqualTo(instance.hashCode());
    }

    public static QueryDefinition buildDataSet() {
        return QueryDefinition.builder()
                              .type("cql")
                              .name("search_release_note_simple")
                              .path("META-INF/queries/search_release_note_simple.cql")
                              .description("Allow to retrieve release note information")
                              .parameters(List.of("param"))
                              .build()
                              .toBuilder()
                              .build();
    }
}