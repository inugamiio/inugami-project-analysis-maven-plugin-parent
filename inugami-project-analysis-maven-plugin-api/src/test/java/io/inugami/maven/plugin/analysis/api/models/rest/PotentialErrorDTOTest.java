package io.inugami.maven.plugin.analysis.api.models.rest;

import io.inugami.api.exceptions.UncheckedException;
import io.inugami.commons.test.dto.AssertDtoContext;
import org.junit.jupiter.api.Test;

import static io.inugami.commons.test.UnitTestHelper.assertDto;
import static org.assertj.core.api.Assertions.assertThat;

class PotentialErrorDTOTest {

    @Test
    void potentialErrorDTO() {
        assertDto(new AssertDtoContext<PotentialErrorDTO>()
                          .toBuilder()
                          .objectClass(PotentialErrorDTO.class)
                          .fullArgConstructorRefPath("api/models/rest/potentialErrorDTOTest/fullArgConstructorRefPath.json")
                          .getterRefPath("api/models/rest/potentialErrorDTOTest/getterRefPath.json")
                          .toStringRefPath("api/models/rest/potentialErrorDTOTest/toStringRefPath.txt")
                          .cloneFunction(instance -> instance.toBuilder().build())
                          .noArgConstructor(() -> new PotentialErrorDTO())
                          .fullArgConstructor(PotentialErrorDTOTest::buildDataSet)
                          .noEqualsFunction(PotentialErrorDTOTest::notEquals)
                          .checkSetters(true)
                          .build());
    }


    static void notEquals(final PotentialErrorDTO value) {
        assertThat(value).isNotEqualTo(value.toBuilder());
        assertThat(value.hashCode()).isNotEqualTo(value.toBuilder().hashCode());

    }

    public static PotentialErrorDTO buildDataSet() {
        return PotentialErrorDTO.builder()
                                .errorCode("ERR-0")
                                .type("technical")
                                .errorCodeClass(UncheckedException.class)
                                .throwsAs(UncheckedException.class)
                                .httpStatus(500)
                                .errorMessage("some error")
                                .errorMessageDetail("can't process action")
                                .payload("[]")
                                .description("some description")
                                .example("use other value")
                                .url("http://mock")
                                .build();
    }
}