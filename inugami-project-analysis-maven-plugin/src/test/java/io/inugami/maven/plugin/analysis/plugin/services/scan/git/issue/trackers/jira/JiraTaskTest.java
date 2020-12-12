package io.inugami.maven.plugin.analysis.plugin.services.scan.git.issue.trackers.jira;

import io.inugami.commons.connectors.HttpBasicConnector;
import io.inugami.commons.connectors.HttpConnectorResult;
import io.inugami.commons.connectors.HttpConnectorResultBuilder;
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

import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class JiraTaskTest {

    @Mock
    private HttpBasicConnector httpConnector;

    @Mock
    private HttpConnectorBuilder httpConnectorBuilder;

    @BeforeEach
    public void setup() throws Exception {
        lenient().when(httpConnectorBuilder.buildHttpConnector()).thenReturn(httpConnector);

        lenient().when(httpConnector.get(anyString(), anyMap())).thenAnswer(new Answer<HttpConnectorResult>() {
            @Override
            public HttpConnectorResult answer(final InvocationOnMock invocationOnMock) throws Throwable {
                HttpConnectorResult result = null;
                final Object        url    = invocationOnMock.getArgument(0);
                if (url != null) {
                    final HttpConnectorResultBuilder builder = new HttpConnectorResultBuilder();

                    final String[] urlParts = String.valueOf(url).split("/");
                    final String   id       = urlParts[urlParts.length - 1];

                    String data = null;
                    try {
                        data = UnitTestHelper
                                .loadJsonReference("services/scan/git/issue/trackers/jira/" + id + ".json");
                    }
                    catch (final Exception e) {
                    }

                    builder.addUrl(String.valueOf(url));
                    builder.addVerbe("GET");
                    builder.addStatusCode(data == null ? 404 : 200);
                    builder.addContenType("application/json");
                    builder.addEncoding("UTF-8");
                    builder.addData(data == null ? null : data.getBytes());
                    result = builder.build();
                }
                return result;
            }
        });
    }

    // =========================================================================
    // TESTS
    // =========================================================================
    @Test
    public void call_withEpic_shouldBuildNodes() throws Exception {
        final JiraTask        task   = buildTask("INU-1");
        final ScanNeo4jResult result = task.call();
        assertThat(result).isNotNull();
        UnitTestHelper.assertTextRelatif(result, "services/scan/git/issue/trackers/jira/epic.json");
    }

    @Test
    public void call_withIssue_shouldBuildNodes() throws Exception {
        final JiraTask        task   = buildTask("INU-2");
        final ScanNeo4jResult result = task.call();
        assertThat(result).isNotNull();
        UnitTestHelper.assertTextRelatif(result, "services/scan/git/issue/trackers/jira/issue.json");
    }

    @Test
    public void call_withSubTask_shouldBuildNodes() throws Exception {
        final JiraTask        task   = buildTask("INU-4");
        final ScanNeo4jResult result = task.call();
        assertThat(result).isNotNull();
        UnitTestHelper.assertTextRelatif(result, "services/scan/git/issue/trackers/jira/subTask.json");
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