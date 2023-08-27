package io.inugami.maven.plugin.analysis.plugin.services.scan.git.issue.trackers.jira;

import io.inugami.commons.connectors.HttpBasicConnector;
import io.inugami.commons.connectors.HttpConnectorResult;
import io.inugami.commons.connectors.HttpConnectorResultBuilder;
import io.inugami.commons.connectors.HttpRequest;
import io.inugami.commons.test.UnitTestHelper;
import io.inugami.maven.plugin.analysis.api.connectors.HttpConnectorBuilder;
import io.inugami.maven.plugin.analysis.api.models.ScanNeo4jResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.lenient;

@SuppressWarnings({"java:S5976"})
@ExtendWith(MockitoExtension.class)
class JiraTaskTest {

    @Mock
    private HttpBasicConnector httpConnector;

    @Mock
    private HttpConnectorBuilder httpConnectorBuilder;

    @BeforeEach
    public void setup() throws Exception {
        lenient().when(httpConnectorBuilder.buildHttpConnector()).thenReturn(httpConnector);

        lenient().when(httpConnector.get(any(HttpRequest.class))).thenAnswer(invocationOnMock-> {
                HttpConnectorResult result = null;
                final HttpRequest        request    = invocationOnMock.getArgument(0);
                if (request.getUrl() != null) {
                    final HttpConnectorResultBuilder builder = new HttpConnectorResultBuilder();

                    final String[] urlParts = String.valueOf(request.getUrl()).split("/");
                    final String   id       = urlParts[urlParts.length - 1];

                    String data = null;
                    try {
                        data = UnitTestHelper
                                .readFileRelative("services/scan/git/issue/trackers/jira/" + id + ".json");
                    } catch (final Exception e) {
                    }

                    builder.addUrl(String.valueOf(request.getUrl()));
                    builder.addVerb("GET");
                    builder.addStatusCode(data == null ? 404 : 200);
                    builder.addContentType("application/json");
                    builder.addEncoding("UTF-8");
                    builder.addData(data == null ? null : data.getBytes());
                    result = builder.build();
                }
                return result;
        });
    }

    // =========================================================================
    // TESTS
    // =========================================================================
    @Test
    void call_withEpic_shouldBuildNodes() throws Exception {
        final JiraTask        task   = buildTask("INU-1");
        final ScanNeo4jResult result = task.call();
        assertThat(result).isNotNull();
        UnitTestHelper.assertTextRelative(result, "services/scan/git/issue/trackers/jira/epic.json");
    }

    @Test
    void call_withIssue_shouldBuildNodes() throws Exception {
        final JiraTask        task   = buildTask("INU-2");
        final ScanNeo4jResult result = task.call();
        assertThat(result).isNotNull();
        UnitTestHelper.assertTextRelative(result, "services/scan/git/issue/trackers/jira/issue.json");
    }

    @Test
    void call_withSubTask_shouldBuildNodes() throws Exception {
        final JiraTask        task   = buildTask("INU-4");
        final ScanNeo4jResult result = task.call();
        assertThat(result).isNotNull();
        UnitTestHelper.assertTextRelative(result, "services/scan/git/issue/trackers/jira/subTask.json");
    }


    @Test
    void extractResponseCharset_withCharset() {
        final JiraTask task    = buildTask("INU-4");
        final Charset  charset = task.extractResponseCharset(Map.ofEntries(Map.entry("content-type", "text/html; charset=iso-8859-1")));
        assertThat(charset).isNotNull()
                           .hasToString("ISO-8859-1");
    }

    // =========================================================================
    // TOOLS
    // =========================================================================
    private JiraTask buildTask(final String issueId) {
        return new JiraTask(issueId,
                            "user",
                            "password",
                            "http://localhost",
                            httpConnectorBuilder,
                            "io.inugami:project-consumer:1.0.0-SNAPSHOT",
                            new ArrayList<>());
    }
}