package io.inugami.maven.plugin.analysis.api.models.rest;

import io.inugami.commons.test.dto.AssertDtoContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.inugami.commons.test.UnitTestHelper.assertDto;
import static io.inugami.commons.test.UnitTestHelper.assertTextRelative;
import static io.inugami.maven.plugin.analysis.api.models.rest.EndpointUtils.*;
import static org.assertj.core.api.Assertions.assertThat;

class RestApiTest {
    @Test
    void restApi() {
        assertDto(new AssertDtoContext<RestApi>()
                          .toBuilder()
                          .objectClass(RestApi.class)
                          .fullArgConstructorRefPath("api/models/rest/restApi/model.json")
                          .getterRefPath("api/models/rest/restApi/getter.json")
                          .toStringRefPath("api/models/rest/restApi/toString.txt")
                          .cloneFunction(instance -> instance.toBuilder().build())
                          .noArgConstructor(RestApi::new)
                          .fullArgConstructor(EndpointUtils::buildRestApi)
                          .noEqualsFunction(this::notEquals)
                          .checkSetters(true)
                          .build());
    }

    @Test
    void orderEndPoint_nominal() {
        assertTextRelative(buildRestApi().orderEndPoint(), "api/models/rest/restApi/orderEndPoint_nominal.1.json");
        assertTextRelative(buildRestApi().toBuilder()
                                         .endpoints(null)
                                         .build()
                                         .orderEndPoint(), "api/models/rest/restApi/orderEndPoint_nominal.2.json");
    }

    void notEquals(final RestApi instance) {
        assertThat(instance).isNotEqualTo(instance.toBuilder());
        assertThat(instance.hashCode()).isNotEqualTo(instance.toBuilder().hashCode());
        //
        assertThat(instance).isNotEqualTo(instance.toBuilder().endpoints(null).build());
        assertThat(instance.toBuilder().endpoints(null).build()).isNotEqualTo(instance);
        assertThat(instance).isNotEqualTo(instance.toBuilder().endpoints(List.of()).build());
        assertThat(instance).isNotEqualTo(instance.toBuilder().endpoints(List.of(buildRestEndpointOther())).build());
        assertThat(instance.toBuilder().endpoints(List.of()).build()).isNotEqualTo(instance);
        assertThat(instance.toBuilder().endpoints(List.of(buildRestEndpointOther())).build()).isNotEqualTo(instance);
        assertThat(instance.hashCode()).isNotEqualTo(instance.toBuilder().endpoints(null).build().hashCode());
        assertThat(instance.toBuilder().endpoints(List.of()).build().hashCode()).isNotEqualTo(instance.hashCode());
        assertThat(instance.toBuilder()
                           .endpoints(List.of(buildRestEndpointOther()))
                           .build()
                           .hashCode()).isNotEqualTo(instance.hashCode());

    }

    @Test
    void compareTo_nominal() {
        assertThat(buildRestEndpoint().compareTo(null)).isEqualTo(-1);
        assertThat(buildRestEndpoint().compareTo(buildRestEndpoint())).isEqualTo(0);
        assertThat(buildRestEndpoint().compareTo(buildRestEndpoint().toBuilder().verb("aaa").build())).isEqualTo(-1);
        assertThat(buildRestEndpoint().compareTo(buildRestEndpoint().toBuilder().verb("zzz").build())).isEqualTo(-1);
    }
}