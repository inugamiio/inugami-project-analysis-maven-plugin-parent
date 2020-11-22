package io.inugami.maven.plugin.analysis.plugin.services.scan.analyzers.errors;

import io.inugami.api.models.data.basic.JsonObject;
import io.inugami.api.processors.ConfigHandler;
import io.inugami.commons.security.EncryptionUtils;
import io.inugami.configuration.services.ConfigHandlerHashMap;
import io.inugami.maven.plugin.analysis.api.models.Node;
import io.inugami.maven.plugin.analysis.api.models.Relationship;
import io.inugami.maven.plugin.analysis.api.models.ScanConext;
import io.inugami.maven.plugin.analysis.api.models.ScanNeo4jResult;
import lombok.Builder;
import lombok.Getter;
import org.apache.maven.project.MavenProject;
import org.assertj.core.api.AssertionsForInterfaceTypes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static io.inugami.commons.test.UnitTestHelper.assertTextRelatif;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class ErrorCodeAnalyzerTest {
    // =========================================================================
    // ATTRIBUTES
    // =========================================================================
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


        final ConfigHandler<String, String> configuration = new ConfigHandlerHashMap();
        configuration.put(ErrorCodeAnalyzer.FEATURE, "true");
        configuration.put(ErrorCodeAnalyzer.ERROR_CODE_INTERFACE, CustomErrorCode.class.getName());

        lenient().when(context.getConfiguration()).thenReturn(configuration);
    }

    // =========================================================================
    // TESTS
    // =========================================================================
    @Test
    public void accept_withEnumOrClass_shouldAccept() {
        final ErrorCodeAnalyzer analyzer = new ErrorCodeAnalyzer();
        analyzer.initialize(context);
        assertThat(analyzer.accept(ErrorCodeAnalyzerTest.EnumErrors.class, context)).isTrue();
        assertThat(analyzer.accept(ErrorCodeAnalyzerTest.ClassErrors.class, context)).isTrue();
    }

    @Test
    public void analyze_withEnum_shouldFoundErrors() {
        final ErrorCodeAnalyzer analyzer    = new ErrorCodeAnalyzer();
        analyzer.initialize(context);
        final ScanNeo4jResult   neo4jResult = extractResult(
                analyzer.analyze(ErrorCodeAnalyzerTest.EnumErrors.class, context));
        neo4jResult.getNodes().sort((value, ref) -> compareNodes(value, ref));
        neo4jResult.getRelationships().sort((value, ref) -> sortRelationship(value, ref));
        assertTextRelatif(neo4jResult.getNodes(), "services/scan/analyzers/errors/enum_result_nodes.json");
        assertTextRelatif(neo4jResult.getRelationships(), "services/scan/analyzers/errors/enum_result_relationship.json");
    }

    @Test
    public void analyze_withClass_shouldFoundErrors() {
        final ErrorCodeAnalyzer analyzer    = new ErrorCodeAnalyzer();
        analyzer.initialize(context);
        final ScanNeo4jResult   neo4jResult = extractResult(
                analyzer.analyze(ErrorCodeAnalyzerTest.ClassErrors.class, context));
        neo4jResult.getNodes().sort((value, ref) -> compareNodes(value, ref));
        neo4jResult.getRelationships().sort((value, ref) -> sortRelationship(value, ref));
        assertTextRelatif(neo4jResult, "services/scan/analyzers/errors/class_result.json");
    }

    private int compareNodes(final Node value, final Node ref) {
        final EncryptionUtils sha1 = new EncryptionUtils();
        return sha1.encodeSha1(value.convertToJson()).compareTo(sha1.encodeSha1(ref.convertToJson()));
    }
    private int sortRelationship(final Relationship value, final Relationship ref) {
        final EncryptionUtils sha1 = new EncryptionUtils();
        return sha1.encodeSha1(value.convertToJson()).compareTo(sha1.encodeSha1(ref.convertToJson()));
    }
    // =========================================================================
    // CLASSES
    // =========================================================================
    public static enum EnumErrors implements CustomErrorCode {
        INVALID_ARGUMENTS(CustomError.builder()
                                     .errorCode("ERR-1")
                                     .message("invalid arguments")
                                     .messageDetail("given arguments aren't valid")
                                     .errorType("technical")
                                     .statusCode(500)
                                     .payload("[]"));

        CustomErrorCode error;

        private EnumErrors(final CustomError.CustomErrorBuilder builder) {
            error = builder.build();
        }

        @Override
        public CustomErrorCode getCurrentErrorCode() {
            return error;
        }
    }

    public static class ClassErrors {
        public static final String DATA = "some data";
        public static final CustomErrorCode USER_REQUIRE = CustomError.builder()
                                                                      .errorCode("ERR-2")
                                                                      .message("user require")
                                                                      .messageDetail("user is require for this ation")
                                                                      .errorType("functional")
                                                                      .statusCode(400)
                                                                      .payload("null")
                                                                      .build();
    }

    @Builder
    @Getter
    public static class CustomError implements CustomErrorCode {
        int    statusCode;
        String errorCode;
        String message;
        String messageDetail;
        String errorType;
        String payload;

        @Override
        public CustomErrorCode getCurrentErrorCode() {
            return this;
        }
    }

    public static interface CustomErrorCode {


        public CustomErrorCode getCurrentErrorCode();

        default int getStatusCode() {
            return getCurrentErrorCode() == null ? 500 : getCurrentErrorCode().getStatusCode();
        }


        default String getErrorCode() {
            return getCurrentErrorCode() == null ? "undefine" : getCurrentErrorCode().getErrorCode();
        }


        default String getMessage() {
            return getCurrentErrorCode() == null ? "error" : getCurrentErrorCode().getMessage();
        }


        default String getMessageDetail() {
            return getCurrentErrorCode() == null ? null : getCurrentErrorCode().getMessageDetail();
        }


        default String getErrorType() {
            return getCurrentErrorCode() == null ? "technical" : getCurrentErrorCode().getErrorType();
        }


        default String getPayload() {
            return getCurrentErrorCode() == null ? null : getCurrentErrorCode().getPayload();
        }
    }

    // =========================================================================
    // TOOLS
    // =========================================================================
    private ScanNeo4jResult extractResult(final List<JsonObject> result) {
        assertThat(result).isNotNull();
        AssertionsForInterfaceTypes.assertThat(result).size().isEqualTo(1);
        final ScanNeo4jResult neo4jResult = (ScanNeo4jResult) result.get(0);
        neo4jResult.getNodes().sort((value, ref) -> value.getUid().compareTo(ref.getUid()));

        neo4jResult.getRelationships().sort((value, ref) -> String.join("->", value.getFrom(), value.getTo()).compareTo(
                String.join("->", ref.getFrom(), ref.getTo())));
        return neo4jResult;
    }

}