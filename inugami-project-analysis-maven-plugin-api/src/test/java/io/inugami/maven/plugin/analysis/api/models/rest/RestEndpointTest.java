package io.inugami.maven.plugin.analysis.api.models.rest;

import io.inugami.commons.test.dto.AssertDtoContext;
import org.junit.jupiter.api.Test;

import static io.inugami.commons.test.UnitTestData.OTHER;
import static io.inugami.commons.test.UnitTestHelper.assertDto;
import static io.inugami.maven.plugin.analysis.api.models.rest.EndpointUtils.buildRestEndpoint;
import static org.assertj.core.api.Assertions.assertThat;

class RestEndpointTest {


    @Test
    void restEndpoint() {
        assertDto(new AssertDtoContext<RestEndpoint>()
                          .toBuilder()
                          .objectClass(RestEndpoint.class)
                          .fullArgConstructorRefPath("api/models/rest/restEndpoint/model.json")
                          .getterRefPath("api/models/rest/restEndpoint/getter.json")
                          .toStringRefPath("api/models/rest/restEndpoint/toString.txt")
                          .cloneFunction(instance -> instance.toBuilder().build())
                          .noArgConstructor(RestEndpoint::new)
                          .fullArgConstructor(EndpointUtils::buildRestEndpoint)
                          .noEqualsFunction(this::notEquals)
                          .checkSetters(true)
                          .build());
    }

    void notEquals(final RestEndpoint instance) {
        assertThat(instance).isNotEqualTo(instance.toBuilder());
        assertThat(instance.hashCode()).isNotEqualTo(instance.toBuilder().hashCode());
        //
        assertThat(instance).isNotEqualTo(instance.toBuilder().uri(null).build());
        assertThat(instance.toBuilder().uri(null).build()).isNotEqualTo(instance);
        assertThat(instance).isNotEqualTo(instance.toBuilder().uri(OTHER).build());
        assertThat(instance.toBuilder().uri(OTHER).build()).isNotEqualTo(instance);
        assertThat(instance.hashCode()).isNotEqualTo(instance.toBuilder().uri(null).build().hashCode());
        assertThat(instance.toBuilder().uri(OTHER).build().hashCode()).isNotEqualTo(instance.hashCode());
        //
        assertThat(instance).isNotEqualTo(instance.toBuilder().verb(null).build());
        assertThat(instance.toBuilder().verb(null).build()).isNotEqualTo(instance);
        assertThat(instance).isNotEqualTo(instance.toBuilder().verb(OTHER).build());
        assertThat(instance.toBuilder().verb(OTHER).build()).isNotEqualTo(instance);
        assertThat(instance.hashCode()).isNotEqualTo(instance.toBuilder().verb(null).build().hashCode());
        assertThat(instance.toBuilder().verb(OTHER).build().hashCode()).isNotEqualTo(instance.hashCode());
    }

    @Test
    void compareTo_nominal() {
        assertThat(buildRestEndpoint().compareTo(null)).isEqualTo(-1);
        assertThat(buildRestEndpoint().compareTo(buildRestEndpoint())).isEqualTo(0);
        assertThat(buildRestEndpoint().compareTo(buildRestEndpoint().toBuilder().verb("aaa").build())).isEqualTo(-1);
        assertThat(buildRestEndpoint().compareTo(buildRestEndpoint().toBuilder().verb("zzz").build())).isEqualTo(-1);
    }
}