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
package io.inugami.maven.plugin.analysis.plugin.services.scan.analyzers;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.inugami.api.processors.ConfigHandler;
import io.inugami.configuration.services.ConfigHandlerHashMap;
import io.inugami.maven.plugin.analysis.api.models.ScanConext;
import io.inugami.maven.plugin.analysis.api.models.rest.RestApi;
import io.inugami.maven.plugin.analysis.api.models.rest.RestEndpoint;
import io.swagger.annotations.Api;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static io.inugami.commons.test.UnitTestHelper.assertTextRelatif;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.lenient;

@Slf4j
@ExtendWith(MockitoExtension.class)
class SpringRestControllersAnalyzerTest {

    @Mock
    private ScanConext context;

    @BeforeEach
    public void setup() {
        final ConfigHandler<String, String> configuration = new ConfigHandlerHashMap(
                Map.ofEntries(Map.entry(SpringRestControllersAnalyzer.FEATURE, "true"))
        );
        lenient().when(context.getConfiguration()).thenReturn(configuration);
    }

    @Test
    void testAccept() {
        final SpringRestControllersAnalyzer analyzer = new SpringRestControllersAnalyzer();
        assertThat(analyzer.accept(BasicRestController.class, context)).isTrue();
        assertThat(analyzer.accept(String.class, context)).isFalse();
    }

    @Test
    void analyze() {
        final SpringRestControllersAnalyzer analyzer = new SpringRestControllersAnalyzer();
        final RestApi                       api      = analyzer.analyseClass(BasicRestController.class);
        assertThat(api).isNotNull();
        assertThat(api.getName()).isEqualTo("USERS");
        assertThat(api.getBaseContext()).isEqualTo("/v1/api");

        assertThat(api.getEndpoints()).isNotNull();
        assertThat(api.getEndpoints().size()).isEqualTo(7);

        api.getEndpoints().sort((val,ref)->{
            return buildPath(val).compareTo(buildPath(ref));
        });

        for(int i=0;i<api.getEndpoints().size(); i++){
            log.info("check api  : /services/scan/analyzers/api_{}.json",i);
            assertTextRelatif(api.getEndpoints().get(i), "/services/scan/analyzers/api_"+i+".json");
        }

    }

    private String buildPath(final RestEndpoint value) {
        return String.join("_", value.getVerb(),value.getUri());
    }


    // =========================================================================
    // TEST CONTENT
    // =========================================================================
    @Api("USERS")
    @RequestMapping("v1/api")
    @RestController("simpleRest")
    public static class BasicRestController {
        @GetMapping(path = "/users/headers/{name}", produces = MediaType.APPLICATION_JSON_VALUE)
        public List<Dto> retrieveUserInformation(@PathVariable("name") final String name,
                                                 @RequestHeader("correlationId") final String correlationId,
                                                 @RequestHeader("requestId") final String requestId) {
            return null;
        }
        @GetMapping(path = "/uids", produces = MediaType.APPLICATION_JSON_VALUE)
        public List<Long> getAllUids() {
            return null;
        }

        @PostMapping(path = "/users/{name}/comments", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
        public ResponseEntity<Comment> addComment(@PathVariable("name") final String name,
                                                  @RequestBody final Comment comment) {
            return null;
        }

        @RequestMapping(path = "/users/{name}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
        public Dto retrieveInformation(@PathVariable("name") final String name) {
            return null;
        }

        @GetMapping(path = "/users/{name}/process")
        public void process(@PathVariable("name") final String name) {
        }
        @GetMapping(path = "/health")
        public String process() {
            return "hello";
        }


        @RequestMapping(path = "/users", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
        public List<Dto> retrieveAllInformation() {
            return null;
        }

    }

    @AllArgsConstructor
    @Getter
    public static class ParentDto {
        private final String uid;
    }

    @Getter
    public static class Dto extends ParentDto {
        @NotNull
        private final String            name;
        private final List<Comment>     comments;
        private final Map<String, Data> data;

        public Dto(final String uid, final String name,
                   final List<Comment> comments,
                   final Map<String, Data> data) {
            super(uid);
            this.name     = name;
            this.comments = comments;
            this.data     = data;
        }
    }

    @AllArgsConstructor
    @Getter
    public static class Comment {
        @NotNull
        private final String        title;
        private final String        description;
        private final List<Comment> comments;

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        private final LocalDateTime publishDate;
    }

    @AllArgsConstructor
    @Getter
    public static class Data {
        private final String            title;
        @JsonFormat(shape = JsonFormat.Shape.NUMBER)
        private final LocalDateTime     publishedDate;
        private final Map<String, Data> data;
        private final Data              parent;
    }
}