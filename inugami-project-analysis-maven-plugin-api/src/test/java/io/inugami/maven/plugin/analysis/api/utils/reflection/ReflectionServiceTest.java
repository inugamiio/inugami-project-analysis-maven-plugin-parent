package io.inugami.maven.plugin.analysis.api.utils.reflection;

import io.inugami.commons.test.UnitTestHelper;
import io.inugami.commons.test.dto.UserDataDTO;
import io.inugami.maven.plugin.analysis.annotations.ExposedAs;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static io.inugami.maven.plugin.analysis.api.utils.reflection.ReflectionService.renderReturnType;
import static io.inugami.maven.plugin.analysis.api.utils.reflection.ReflectionService.searchMethod;

class ReflectionServiceTest {

    @Test
    void renderReturnType_nominal(){
        final Method method =searchMethod(UserRestService.class, "getUser");
        UnitTestHelper.assertTextRelative(renderReturnType(method, true).convertToJson(),
                                          "api/utils/reflection/ReflectionServiceTest/renderReturnType_nominal.json");
    }

    @Test
    void renderReturnType_withResponseEntity(){
        final Method method = searchMethod(UserRestService.class, "updateUser");
        final JsonNode result = renderReturnType(method, true);
        UnitTestHelper.assertTextRelative(result.convertToJson(),
                                          "api/utils/reflection/ReflectionServiceTest/renderReturnType_nominal.json");
    }


    @Test
    void renderReturnType_withResponseEntityAndList(){
        final Method method = searchMethod(UserRestService.class, "createUsers");
        UnitTestHelper.assertTextRelative(renderReturnType(method, true).convertToJson(),
                                          "api/utils/reflection/ReflectionServiceTest/renderReturnType_withResponseEntityAndList.json");
    }

    @Test
    void renderReturnType_withResponseEntityAndMap(){
        final Method method = searchMethod(UserRestService.class, "updateUsers");
        UnitTestHelper.assertTextRelative(renderReturnType(method, true).convertToJson(),
                                          "api/utils/reflection/ReflectionServiceTest/renderReturnType_withResponseEntityAndMap.json");
    }

    @Test
    void renderReturnType_withExposeAsAnnotation(){
        final Method method = searchMethod(UserRestService.class, "annotatedUsers");
        UnitTestHelper.assertTextRelative(renderReturnType(method, true).convertToJson(),
                                          "api/utils/reflection/ReflectionServiceTest/renderReturnType_withExposeAsAnnotation.json");
    }
    // =================================================================================================================
    // TOOLS
    // =================================================================================================================
    static class UserRestService{
        @GetMapping(path = "{id}")
        public UserDataDTO getUser(@PathVariable final long id){
            return null;
        }

        @PatchMapping(path = "{id}")
        public ResponseEntity<UserDataDTO> updateUser(@RequestBody final UserDataDTO user){
            return null;
        }


        @PostMapping
        public ResponseEntity<List<UserDataDTO>> createUsers(@RequestBody final List<UserDataDTO> users){
            return null;
        }

        @PostMapping
        public ResponseEntity<Map<String,UserDataDTO>> updateUsers(@RequestBody final List<UserDataDTO> users){
            return null;
        }


        @ExposedAs("{\n" +
                "  \"<String>\":\n" +
                "              {\n" +
                "                \"id\":\"long\",\n" +
                "                \"firstName\":\"String\",\n" +
                "                \"lastName\":\"String\",\n" +
                "                \"email\":\"String\",\n" +
                "                \"sex\":\"Sex\",\n" +
                "                \"phoneNumber\":\"String\",\n" +
                "                \"old\":\"int\",\n" +
                "                \"birthday\":\"LocalDate\",\n" +
                "                \"socialId\":\"String\",\n" +
                "                \"nationality\":\"String\",\n" +
                "                \"streetNumber\":\"String\",\n" +
                "                \"streetName\":\"String\",\n" +
                "                \"streetType\":\"String\",\n" +
                "                \"zipCode\":\"String\",\n" +
                "                \"city\":\"String\",\n" +
                "                \"district\":\"String\",\n" +
                "                \"department\":\"String\",\n" +
                "                \"canton\":\"String\",\n" +
                "                \"region\":\"String\",\n" +
                "                \"state\":\"String\",\n" +
                "                \"country\":\"String\",\n" +
                "                \"deviceIdentifier\":\"String\"\n" +
                "              }\n" +
                "}")
        @PostMapping
        public ResponseEntity<Map<String,UserDataDTO>> annotatedUsers(@RequestBody final List<UserDataDTO> users){
            return null;
        }
    }

}