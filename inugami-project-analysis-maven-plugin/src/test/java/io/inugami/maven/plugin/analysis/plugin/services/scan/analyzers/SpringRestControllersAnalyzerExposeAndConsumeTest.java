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
import io.inugami.api.models.data.basic.JsonObject;
import io.inugami.maven.plugin.analysis.api.models.Node;
import io.inugami.maven.plugin.analysis.api.models.ScanConext;
import io.inugami.maven.plugin.analysis.api.models.ScanNeo4jResult;
import io.swagger.annotations.Api;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SpringRestControllersAnalyzerExposeAndConsumeTest {

    // =========================================================================
    // ATTRIBUTES
    // =========================================================================
    @Mock
    private ScanConext   scanContextExposer;
    @Mock
    private MavenProject exposer;
    @Mock
    private ScanConext   scanContextConsumer;
    @Mock
    private MavenProject consumer;

    // =========================================================================
    // API
    // =========================================================================
    @Test
    void analyze_withPost() {
        when(scanContextExposer.getProject()).thenReturn(exposer);
        when(exposer.getGroupId()).thenReturn("io.inugami");
        when(exposer.getArtifactId()).thenReturn("exposer");
        when(exposer.getVersion()).thenReturn("1.2.0");
        final SpringRestControllersAnalyzer exposerAnalyzer = new SpringRestControllersAnalyzer();
        final Node                          exposerNode     = extractService(
                exposerAnalyzer.analyze(ExposerController.class, scanContextExposer));

        when(scanContextConsumer.getProject()).thenReturn(exposer);
        final SpringFeignClientAnalyzer consumerAnalyzer = new SpringFeignClientAnalyzer();
        final Node                      consumerNode     = extractService(
                consumerAnalyzer.analyze(ConsumerFeignClient.class, scanContextConsumer));

        assertThat(exposerNode).isNotNull();
        assertThat(consumerNode).isNotNull();

        assertThat(extractValue("uri", exposerNode)).isEqualTo(extractValue("uri", consumerNode));
        assertThat(extractValue("verb", exposerNode)).isEqualTo(extractValue("verb", consumerNode));

        assertThat(exposerNode.getUid()).isEqualTo(consumerNode.getUid());
    }


    @Test
    void analyze_withGet() {
        when(scanContextExposer.getProject()).thenReturn(exposer);
        when(exposer.getGroupId()).thenReturn("io.inugami");
        when(exposer.getArtifactId()).thenReturn("exposer");
        when(exposer.getVersion()).thenReturn("1.2.0");
        final SpringRestControllersAnalyzer exposerAnalyzer = new SpringRestControllersAnalyzer();
        final Node                          exposerNode     = extractService(
                exposerAnalyzer.analyze(Exposer2Controller.class, scanContextExposer));

        when(scanContextConsumer.getProject()).thenReturn(exposer);
        final SpringFeignClientAnalyzer consumerAnalyzer = new SpringFeignClientAnalyzer();
        final Node                      consumerNode     = extractService(
                consumerAnalyzer.analyze(Consumer2FeignClient.class, scanContextConsumer));

        assertThat(exposerNode).isNotNull();
        assertThat(consumerNode).isNotNull();

        assertThat(extractValue("uri", exposerNode)).isEqualTo(extractValue("uri", consumerNode));
        assertThat(extractValue("verb", exposerNode)).isEqualTo(extractValue("verb", consumerNode));

        assertThat(exposerNode.getUid()).isEqualTo(consumerNode.getUid());
    }


    private Node extractService(final List<JsonObject> values) {
        Node       result     = null;
        JsonObject jsonObject = null;
        if (values != null && !values.isEmpty()) {
            jsonObject = values.get(0);
        }
        List<Node> nodes = null;
        if (jsonObject instanceof ScanNeo4jResult) {
            nodes = ((ScanNeo4jResult) jsonObject).getNodes();
        }

        if (nodes != null) {
            for (Node node : nodes) {
                if ("Service".equals(node.getType())) {
                    result = node;
                    break;
                }
            }
        }
        return result;
    }

    private String extractValue(final String key, final Node node) {
        String result = null;
        if (node.getProperties() != null) {
            final Serializable value = node.getProperties().get(key);
            result = value == null ? null : String.valueOf(value);
        }
        return result;
    }

    // =========================================================================
    // EXPOSER
    // =========================================================================
    @RequestMapping("v1/api")
    @RestController
    public static class ExposerController {
        @PostMapping(path = "/users/{name}/comments", produces = MediaType.APPLICATION_JSON_VALUE)
        public ResponseEntity<Comment> addComment(@PathVariable("name") final String name,
                                                  @RequestBody final Comment comment) {
            return null;
        }
    }

    @RequestMapping("v2/api")
    @RestController
    public static class Exposer2Controller {
        @RequestMapping(path = "/comments", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
        public List<Comment> getComments() {
            return null;
        }
    }


    @AllArgsConstructor
    @Getter
    public static class Comment {
        @NotNull
        private final String                                          title;
        private final String                                          description;
        private final List<SpringRestControllersAnalyzerTest.Comment> comments;

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        private final LocalDateTime publishDate;
    }

    // =========================================================================
    // CONSUMER
    // =========================================================================

    @FeignClient(url = "localhost:8080", path = "v1/api")
    public interface ConsumerFeignClient {

        @PostMapping(path = "/users/{name}/comments", produces = MediaType.APPLICATION_JSON_VALUE)
        public CommentConsumer addComment(@PathVariable final String name, @RequestBody final CommentConsumer comment);
    }

    @FeignClient(name = "comments", url = "localhost:8080", path = "v2/api")
    public interface Consumer2FeignClient {

        @GetMapping(path = "/comments", produces = MediaType.APPLICATION_JSON_VALUE)
        public List<CommentConsumer> getComments();
    }

    @AllArgsConstructor
    @Getter
    public static class CommentConsumer {
        @NotNull
        private final String title;

        private final List<Comment> comments;
    }


}
