package io.inugami.maven.plugin.analysis.plugin.services.scan.analyzers;

import io.inugami.api.models.data.basic.JsonObject;
import io.inugami.api.processors.ConfigHandler;
import io.inugami.configuration.services.ConfigHandlerHashMap;
import io.inugami.maven.plugin.analysis.annotations.FeignClientDefinition;
import io.inugami.maven.plugin.analysis.annotations.UsingFeignClient;
import io.inugami.maven.plugin.analysis.api.models.ScanConext;
import io.swagger.annotations.Api;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import static io.inugami.commons.test.UnitTestHelper.assertTextRelative;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;

@SuppressWarnings({"java:S1607"})
@ExtendWith(MockitoExtension.class)
class SpringFeignClientAnalyzerTest {
    @Mock
    private MavenProject mavenProject;
    @Mock
    private ScanConext   context;

    @BeforeEach
    public void setup() {
        lenient().when(mavenProject.getGroupId()).thenReturn("io.inugami.test");
        lenient().when(mavenProject.getArtifactId()).thenReturn("basic-artifact");
        lenient().when(mavenProject.getVersion()).thenReturn("1.0.0-SNAPSHOT");
        lenient().when(mavenProject.getPackaging()).thenReturn("jar");
        lenient().when(context.getProject()).thenReturn(mavenProject);

        final ConfigHandler<String, String> configuration = new ConfigHandlerHashMap(
                Map.ofEntries(Map.entry(SpringRestControllersAnalyzer.FEATURE, "true"))
        );
        lenient().when(context.getConfiguration()).thenReturn(configuration);
    }

    // =========================================================================
    // TEST
    // =========================================================================
    @Disabled
    @Test
    void analyze_withConfigurationBean() {
        MDC.clear();
        final SpringFeignClientAnalyzer analyzer = new SpringFeignClientAnalyzer();
        assertThat(analyzer.accept(AppConfiguration.class, context)).isTrue();


        final List<JsonObject> result = analyzer.analyze(AppConfiguration.class, context);
        assertTextRelative(AnalyzerTestUtils.extractResult(result), "services/scan/analyzers/springFeignClientAnalyzerTest/analyze_withConfigurationBean.json");

    }

    // =========================================================================
    // TOOLS
    // =========================================================================
    @UsingFeignClient(feignConfigurationBean = RestClientConfiguration.class)
    @Configuration
    public static class AppConfiguration {

    }

    @Configuration
    public static class RestClientConfiguration {

        @FeignClientDefinition(RestClient.class)
        @Bean
        public RestClient buildRestClient() {
            return null;
        }

    }

    @Api("USERS")
    @RequestMapping("v1/api")
    public static interface RestClient {
        @GetMapping(path = "/users/headers/{name}", produces = MediaType.APPLICATION_JSON_VALUE)
        List<SpringRestControllersAnalyzerTest.Dto> retrieveUserInformation(@PathVariable("name") final String name,
                                                                            @RequestHeader("correlationId") final String correlationId,
                                                                            @RequestHeader("requestId") final String requestId);

        @GetMapping(path = "/uids", produces = MediaType.APPLICATION_JSON_VALUE)
        List<Long> getAllUids();

        @PostMapping(path = "/users/{name}/comments", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
        ResponseEntity<SpringRestControllersAnalyzerTest.Comment> addComment(@PathVariable("name") final String name,
                                                                             @RequestBody final SpringRestControllersAnalyzerTest.Comment comment);

        @RequestMapping(path = "/users/{name}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
        SpringRestControllersAnalyzerTest.Dto retrieveInformation(@PathVariable("name") final String name);

        @GetMapping(path = "/users/{name}/process")
        void process(@PathVariable("name") final String name);

        @GetMapping(path = "/health")
        String process();


        @RequestMapping(path = "/users", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
        List<SpringRestControllersAnalyzerTest.Dto> retrieveAllInformation();

    }
}