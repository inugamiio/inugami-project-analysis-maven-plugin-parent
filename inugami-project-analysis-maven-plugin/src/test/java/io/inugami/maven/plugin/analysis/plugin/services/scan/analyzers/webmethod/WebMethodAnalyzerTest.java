package io.inugami.maven.plugin.analysis.plugin.services.scan.analyzers.webmethod;

import io.inugami.api.exceptions.DefaultErrorCode;
import io.inugami.api.exceptions.ErrorCode;
import io.inugami.api.models.data.basic.JsonObject;
import io.inugami.api.processors.ConfigHandler;
import io.inugami.configuration.services.ConfigHandlerHashMap;
import io.inugami.maven.plugin.analysis.annotations.Description;
import io.inugami.maven.plugin.analysis.annotations.PotentialError;
import io.inugami.maven.plugin.analysis.api.models.ScanConext;
import io.inugami.maven.plugin.analysis.api.models.ScanNeo4jResult;
import io.inugami.maven.plugin.analysis.api.models.rest.RestEndpoint;
import io.inugami.maven.plugin.analysis.api.utils.reflection.DescriptionDTO;
import io.inugami.maven.plugin.analysis.api.utils.reflection.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.apache.maven.project.MavenProject;
import org.jboss.ws.api.annotation.WebContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.annotation.security.RolesAllowed;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import java.lang.reflect.Method;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import static io.inugami.api.exceptions.DefaultErrorCode.builder;
import static io.inugami.commons.test.UnitTestHelper.*;
import static io.inugami.maven.plugin.analysis.api.utils.reflection.ReflectionService.extractDescription;
import static io.inugami.maven.plugin.analysis.api.utils.reflection.ReflectionService.loadAllMethods;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
public class WebMethodAnalyzerTest {


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

        final ConfigHandler<String, String> configuration = new ConfigHandlerHashMap(
                Map.ofEntries(Map.entry(WebMethodAnalyzer.FEATURE, "true"))
        );
        lenient().when(context.getConfiguration()).thenReturn(configuration);
    }

    // =========================================================================
    // TEST
    // =========================================================================
    @Test
    public void accept_withEJBWebservice() {
        final WebMethodAnalyzer analyzer = new WebMethodAnalyzer();
        assertThat(analyzer.accept(MyWebservice.class, context)).isTrue();
    }

    @Test
    public void analyze_withEJBWebservice() {
        final WebMethodAnalyzer analyzer = new WebMethodAnalyzer();
        final List<JsonObject>  result   = analyzer.analyze(MyWebservice.class, context);
        assertThat(result).isNotNull();

        final ScanNeo4jResult neo4jResult = (ScanNeo4jResult) result.get(0);
        neo4jResult.sort();
        assertTextRelatif(neo4jResult, "services/scan/analyzers/webmethod/webmethod_result.json");
    }


    @Test
    public void resolveBody_withSimpleTypes() {
        final WebMethodAnalyzer analyzer          = new WebMethodAnalyzer();
        final Method            method            = searchMethod("sayHello");
        final DescriptionDTO    methodDescription = extractDescription(method.getDeclaredAnnotation(Description.class));
        final boolean           deprecated        = method.getAnnotation(Deprecated.class) != null;
        final List<JsonNode>    nodes             = analyzer.resolveBodyNode(method.getParameters(), true);

        final String result = analyzer.resolveBody(nodes,
                                                   true,
                                                   deprecated,
                                                   true,
                                                   "sayHello",
                                                   "http://inugami.io",
                                                   methodDescription);
        assertText(loadJsonReference("services/scan/analyzers/webmethod/resolveBody_withSimpleTypes.xml"),
                   result);
    }


    @Test
    public void resolveBody_withComplexTypes() {
        final WebMethodAnalyzer analyzer          = new WebMethodAnalyzer();
        final Method            method            = searchMethod("createUser");
        final DescriptionDTO    methodDescription = extractDescription(method.getDeclaredAnnotation(Description.class));
        final boolean           deprecated        = method.getAnnotation(Deprecated.class) != null;
        final List<JsonNode>    nodes             = analyzer.resolveBodyNode(method.getParameters(), true);


        final String result = analyzer.resolveBody(nodes,
                                                   true,
                                                   deprecated,
                                                   true,
                                                   "createUser",
                                                   "http://inugami.io",
                                                   methodDescription);
        assertText(loadJsonReference("services/scan/analyzers/webmethod/resolveBody_withComplexTypes.xml"),
                   result);
    }


    @Test
    public void resolveRestEndpoint_withComplexTypes() {
        final WebMethodAnalyzer analyzer = new WebMethodAnalyzer();
        final Method            method   = searchMethod("createUser");
        final WebServiceInfoDTO info     = analyzer.extractWebServiceInformation(MyWebservice.class);
        final RestEndpoint result = analyzer.resolveRestEndpoint(method,
                                                                 info,
                                                                 MyWebservice.class);
        assertTextRelatif(result, "services/scan/analyzers/webmethod/resolveRestEndpoint_withComplexTypes.json");
    }

    // =========================================================================
    // EJB Webservice
    // =========================================================================
    @WebService(targetNamespace = "http://inugami.io", serviceName = "Sample", name = "MyWebservice")
    @SOAPBinding(style = SOAPBinding.Style.DOCUMENT)
    @WebContext(contextRoot = "/webservice", authMethod = "BASIC", urlPattern = "MyWebserviceEJB")
    @RolesAllowed({"ADMIN", "SERVICE"})
    public static class MyWebservice {


        @Description(
                value = "This service allow say hello to specific user",
                potentialErrors = {
                        @PotentialError(errorCode = "USER_NOT_EXISTS", errorCodeClass = SerivceErrorCode.class, throwsAs = BusinessException.class)
                }
        )
        @RolesAllowed("GUEST")
        @WebMethod(exclude = false)
        public String sayHello(@WebParam(name = "userName")
                               @Description("user's name \n" +
                                       "You are allows to specify also the user's uuid") final String name,

                               @WebParam(name = "country")
                               @Description(value = "user's country, this field is mandatory\ncountry code should be ISO code",
                                            potentialErrors = {
                                                    @PotentialError(errorCode = "COUNTRY_NOT_MANAGED",
                                                                    errorCodeClass = SerivceErrorCode.class,
                                                                    throwsAs = BusinessException.class,
                                                                    url = "http://inugami.io/documentation/errors#COUNTRY_NOT_MANAGED"),
                                                    @PotentialError(errorCode = "FUN-002", errorMessage = "country is missing", type = "functional", httpStatus = 400)
                                            }) final String country) throws BusinessException,
                                                                            TechnicalException {
            return "hello " + name;
        }


        // =========================================================================
        // createUser
        // =========================================================================
        @Description(
                value = "This service allow to create new customer",
                potentialErrors = {
                        @PotentialError(errorCode = "USER_ALREADY_EXISTS",
                                        errorCodeClass = SerivceErrorCode.class,
                                        throwsAs = BusinessException.class),
                        @PotentialError(errorCode = "DB_CONNECTION_ERROR",
                                        errorCodeClass = SerivceErrorCode.class,
                                        throwsAs = TechnicalException.class),
                        @PotentialError(errorCode = "ERR-0000",
                                        errorMessage = "undefine error occurs",
                                        throwsAs = TechnicalException.class)
                }
        )
        @Deprecated
        @RolesAllowed("GUEST")
        @WebMethod(exclude = false)
        public UserDTO createUser(@WebParam(name = "user") final UserDTO user) throws BusinessException,
                                                                                      TechnicalException {
            return user.toBuilder().id(1L).build();
        }


        // =========================================================================
        // createUserV2
        // =========================================================================
        @Description(
                value = "This service allow to create new customer",
                potentialErrors = {
                        @PotentialError(errorCode = "USER_ALREADY_EXISTS", errorCodeClass = SerivceErrorCode.class, throwsAs = BusinessException.class)
                }
        )
        @RolesAllowed("GUEST")
        @WebMethod(exclude = false)
        public UserDTO createUserV2(@WebParam(name = "user") final UserDTO user) throws BusinessException,
                                                                                        TechnicalException {
            return user.toBuilder().id(1L).build();
        }
    }


    // =========================================================================
    // DTO
    // =========================================================================
    @Builder(toBuilder = true)
    @Setter
    @Getter
    @AllArgsConstructor
    public static class UserDTO {
        private Long             id;
        private String           firstName;
        private String           lastName;
        private String           email;
        private List<String>     roles;
        private GlobalPreference globalPreference;
    }

    @Setter
    @Getter
    @AllArgsConstructor
    public static class GlobalPreference {
        private Consents         consents;
        private List<Preference> preferences;
    }

    @Setter
    @Getter
    @AllArgsConstructor
    public static class Consents {
        private boolean newsletter;
        private boolean email;
        private boolean phone;
    }

    @Setter
    @Getter
    @AllArgsConstructor
    public static class Preference {
        @Description(value = "key is mandatory",
                     potentialErrors = @PotentialError(errorCode = "ERR-003", errorMessage = "Preference key is require"))
        private String        key;
        private String        value;
        private List<History> histories;
    }

    @Setter
    @Getter
    @AllArgsConstructor
    public static class BusinessException extends Exception {
        private String message;
        private int    errorCode;
    }

    @Setter
    @Getter
    @AllArgsConstructor
    public static class TechnicalException extends Exception {
        private String message;
        private int    errorCode;
    }

    @Setter
    @Getter
    @AllArgsConstructor
    public static class History {
        private Calendar date;
        private String   user;
        List<Tag> tags;
    }

    @Setter
    @Getter
    @AllArgsConstructor
    public static class Tag {
        private String name;
        private int    level;
    }

    public static enum SerivceErrorCode implements ErrorCode {
        COUNTRY_NOT_MANAGED(
                builder()
                        .errorCode("S-001")
                        .message("the country isn't managed")
                        .statusCode(400)
                        .errorTypeFunctional()),

        USER_ALREADY_EXISTS(
                builder()
                        .errorCode("S-002")
                        .message("User already exists, please choose another login to create a newer customer")
                        .statusCode(409)
                        .errorTypeFunctional()),
        DB_CONNECTION_ERROR(
                builder()
                        .errorCode("S-003")
                        .message("can't connect to database")),

        USER_NOT_EXISTS(
                builder()
                        .errorCode("S-004")
                        .message("current user doesn't exists")
                        .statusCode(404)
                        .errorTypeFunctional());

        private final ErrorCode errorCode;

        private SerivceErrorCode(DefaultErrorCode.DefaultErrorCodeBuilder builder) {
            errorCode = builder.build();
        }

        @Override
        public ErrorCode getCurrentErrorCode() {
            return errorCode;
        }
    }

    // =========================================================================
    // TOOLS
    // =========================================================================
    private Method searchMethod(final String methodName) {
        return loadAllMethods(MyWebservice.class)
                .stream()
                .filter(method -> method.getName().equals(methodName))
                .findFirst()
                .orElse(null);
    }
}