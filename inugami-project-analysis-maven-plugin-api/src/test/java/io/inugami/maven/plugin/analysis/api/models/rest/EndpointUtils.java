/* --------------------------------------------------------------------
 *  Inugami
 * --------------------------------------------------------------------
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package io.inugami.maven.plugin.analysis.api.models.rest;

import io.inugami.api.exceptions.UncheckedException;
import io.inugami.api.tools.ReflectionUtils;
import io.inugami.commons.test.UnitTestData;
import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.List;

@UtilityClass
public class EndpointUtils {
    public static RestApi buildRestApi() {
        return RestApi.builder()
                      .name(UnitTestData.LOREM_IPSUM)
                      .description(UnitTestData.LOREM_IPSUM_2)
                      .baseContext("http://mock.inmugami.io/api")
                      .endpoints(new ArrayList<>(List.of(buildRestEndpoint())))
                      .build()
                      .toBuilder()
                      .build();
    }

    public static RestEndpoint buildRestEndpoint() {
        return RestEndpoint.builder()
                           .nickname("create")
                           .uri("http://mock.inmugami.io")
                           .verb("POST")
                           .headers("x-correlation-id")
                           .body("{\"name\":\"String\"}")
                           .bodyRequireOnly("{}")
                           .consume("application/json")
                           .produce("application/json")
                           .description(UnitTestData.LOREM_IPSUM)
                           .responseType("{\"id\":\"long\",\"name\":\"String\"}")
                           .responseTypeRequireOnly("{\"id\":\"long\"}")
                           .uid(UnitTestData.UID)
                           .method("POST")
                           .descriptionDetail(buildDescription())
                           .deprecated(true)
                           .javaMethod(ReflectionUtils.searchMethodByName(Service.class, "create"))
                           .build()
                           .toBuilder()
                           .build();
    }

    public static RestEndpoint buildRestEndpointOther() {
        return RestEndpoint.builder()
                           .nickname("other")
                           .uri("http://mock.inmugami.io/other")
                           .verb("GETs")
                           .build();
    }

    private static DescriptionDTO buildDescription() {
        return DescriptionDTO.builder()
                             .content(UnitTestData.LOREM_IPSUM_2)
                             .example(UnitTestData.LOREM_IPSUM_3)
                             .url("http://mock.inmugami.io/doc")
                             .potentialErrors(List.of(buildPotentialError()))
                             .build()
                             .toBuilder()
                             .build();
    }

    private static PotentialErrorDTO buildPotentialError() {
        return PotentialErrorDTO.builder()
                                .errorCode("ERR-1")
                                .type("functional")
                                .errorCodeClass(UncheckedException.class)
                                .throwsAs(RuntimeException.class)
                                .httpStatus(400)
                                .errorMessage("sorry some error occurs")
                                .errorMessageDetail(UnitTestData.LOREM_IPSUM)
                                .payload("{\"error\":\"String\"}")
                                .description(UnitTestData.LOREM_IPSUM_2)
                                .example(UnitTestData.LOREM_IPSUM_3)
                                .url("http://mock.inmugami.io/doc/errors#ERR-1")
                                .build()
                                .toBuilder()
                                .build();

    }


    private static class Service {
        public void create() {
        }
    }
}
