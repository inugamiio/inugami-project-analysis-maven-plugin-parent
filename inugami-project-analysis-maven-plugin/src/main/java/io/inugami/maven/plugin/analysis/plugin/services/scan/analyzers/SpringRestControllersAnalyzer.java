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

import io.inugami.api.models.JsonBuilder;
import io.inugami.api.models.data.basic.JsonObject;
import io.inugami.commons.security.EncryptionUtils;
import io.inugami.maven.plugin.analysis.annotations.Description;
import io.inugami.maven.plugin.analysis.api.actions.ClassAnalyzer;
import io.inugami.maven.plugin.analysis.api.models.Node;
import io.inugami.maven.plugin.analysis.api.models.Relationship;
import io.inugami.maven.plugin.analysis.api.models.ScanConext;
import io.inugami.maven.plugin.analysis.api.models.ScanNeo4jResult;
import io.inugami.maven.plugin.analysis.api.models.rest.DescriptionDTO;
import io.inugami.maven.plugin.analysis.api.models.rest.PotentialErrorDTO;
import io.inugami.maven.plugin.analysis.api.models.rest.RestApi;
import io.inugami.maven.plugin.analysis.api.models.rest.RestEndpoint;
import io.inugami.maven.plugin.analysis.api.services.neo4j.Neo4jDao;
import io.inugami.maven.plugin.analysis.api.utils.reflection.JsonNode;
import io.inugami.maven.plugin.analysis.api.utils.reflection.ReflectionService;
import io.inugami.maven.plugin.analysis.plugin.services.scan.analyzers.errors.ErrorCodeAnalyzer;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

import static io.inugami.api.functionnals.FunctionalUtils.applyIfNotNull;
import static io.inugami.maven.plugin.analysis.api.constant.Constants.HAS_INPUT_DTO;
import static io.inugami.maven.plugin.analysis.api.tools.BuilderTools.PATCH;
import static io.inugami.maven.plugin.analysis.api.tools.BuilderTools.buildNodeVersion;
import static io.inugami.maven.plugin.analysis.api.utils.NodeUtils.*;
import static io.inugami.maven.plugin.analysis.api.utils.reflection.ReflectionService.*;

@SuppressWarnings({"java:S1845", "java:S5361", "java:S115"})
@Slf4j
public class SpringRestControllersAnalyzer implements ClassAnalyzer {
    public static final String FEATURE_NAME = "inugami.maven.plugin.analysis.analyzer.restControllers";
    public static final String FEATURE      = FEATURE_NAME + ".enable";

    public static final  String SEPARATOR                 = ",";
    public static final  String URI_SEP                   = "/";
    public static final  String SERVICE                   = "Service";
    public static final  String REST                      = "Rest";
    public static final  String SERVICE_TYPE              = "ServiceType";
    public static final  String EXPOSE                    = "EXPOSE";
    public static final  String SERVICE_TYPE_RELATIONSHIP = "SERVICE_TYPE";
    public static final  String VERB                      = "verb";
    public static final  String URI                       = "uri";
    public static final  String HEADER                    = "header";
    public static final  String ACCEPT                    = "accept";
    public static final  String CONTENT_TYPE              = "contentType";
    public static final  String REQUEST_PAYLOAD           = "requestPayload";
    public static final  String RESPONSE_PAYLOAD          = "responsePayload";
    public static final  String DESCRIPTION               = "description";
    public static final  String NICKNAME                  = "nickname";
    private static final String METHOD                    = "method";

    public static final String GET    = "GET";
    public static final String POST   = "POST";
    public static final String PUT    = "PUT";
    public static final String DELETE = "DELETE";

    public static final String IDENTIFIER          = "identifier";
    public static final String EMPTY               = "";
    public static final String DOUBLE_URL_SEP      = "//";
    public static final String UNDERSCORE          = "_";
    public static final String QUOT                = "\"";
    public static final String LINE                = "\n";
    public static final String TAB                 = "\t";
    public static final String SIMPLE_QUOT         = "'";
    public static final String MESSAGE             = "message";
    public static final String MESSAGE_DETAIL      = "messageDetail";
    public static final String ERROR_DESCRIPTION   = "description";
    public static final String EXAMPLE             = "example";
    public static final String STATUS_CODE         = "statusCode";
    public static final String PAYLOAD             = "payload";
    public static final String HAS_ERROR_POTENTIAL = "HAS_ERROR_POTENTIAL";
    public static final String HAS_ENDPOINT        = "HAS_ENDPOINT";
    public static final String HAS_ERROR           = "HAS_ERROR";
    public static final String HAS_POTENTIAL_ERROR = "HAS_POTENTIAL_ERROR";

    // =========================================================================
    // API
    // =========================================================================
    @Override
    public boolean accept(final Class<?> clazz, final ScanConext context) {
        return isEnable(FEATURE, context, true) && getAnnotation(clazz, RestController.class) != null;
    }


    @Override
    public List<JsonObject> analyze(final Class<?> clazz, final ScanConext context) {
        log.info("{} : {}", FEATURE_NAME, clazz);

        final RestApi restApi = analyseClass(clazz);

        final List<String> existingNodes   = new ArrayList<>();
        final List<Node>   potentialErrors = new ArrayList<>();

        final ScanNeo4jResult result = ScanNeo4jResult.builder().build();
        if (restApi != null && restApi.getEndpoints() != null) {
            for (final RestEndpoint endpoint : restApi.getEndpoints()) {
                analyzeOnEndpoint(context, existingNodes, potentialErrors, result, endpoint);
            }
        }

        final Node versionNode = buildNodeVersion(context.getProject());
        if (!result.getNodes().isEmpty()) {
            final Node serviceType = Node.builder().type(SERVICE_TYPE).uid(REST).name(REST).build();
            for (final Node service : result.getNodes()) {
                createServiceRelationship(result, versionNode, serviceType, service);
            }

            result.addNode(versionNode, serviceType);
        }


        for (final String service : existingNodes) {
            result.addRelationship(Relationship.builder()
                                               .from(versionNode.getUid())
                                               .to(service)
                                               .type(getRelationshipType())
                                               .build());
        }
        result.addNode(potentialErrors);
        return List.of(result);
    }

    protected void createServiceRelationship(final ScanNeo4jResult result, final Node versionNode, final Node serviceType, final Node service) {
        if (service.getType().equals(SERVICE)) {
            result.addRelationship(Relationship.builder()
                                               .from(versionNode.getUid())
                                               .to(service.getUid())
                                               .type(getRelationshipType())
                                               .build(),

                                   Relationship.builder()
                                               .from(service.getUid())
                                               .to(serviceType.getUid())
                                               .type(SERVICE_TYPE_RELATIONSHIP)
                                               .build());
        }
    }

    protected void analyzeOnEndpoint(final ScanConext context, final List<String> existingNodes, final List<Node> potentialErrors, final ScanNeo4jResult result, final RestEndpoint endpoint) {
        final Node endpointNode = convertEndpointToNeo4j(endpoint);
        if (endpointNode != null) {
            if (existingNode(endpointNode, context.getNeo4jDao())) {
                existingNodes.add(endpointNode.getUid());
            } else {
                result.addNode(endpointNode);
            }

            if (endpoint.getDescriptionDetail() != null && endpoint.getDescriptionDetail().getPotentialErrors() != null) {
                for (final PotentialErrorDTO potentialError : endpoint.getDescriptionDetail().getPotentialErrors()) {
                    final Node potentialErrorNode = buildPotentialErrorNode(potentialError);
                    final Node errorCode          = ErrorCodeAnalyzer.buildErrorCodeNode(potentialError.getErrorCode());
                    potentialErrors.add(potentialErrorNode);
                    potentialErrors.add(errorCode);

                    result.addRelationship(Relationship.builder()
                                                       .from(endpointNode.getUid())
                                                       .to(potentialErrorNode.getUid())
                                                       .type(HAS_ERROR_POTENTIAL)
                                                       .build());
                    result.addRelationship(Relationship.builder()
                                                       .from(potentialErrorNode.getUid())
                                                       .to(endpointNode.getUid())
                                                       .type(HAS_ENDPOINT)
                                                       .build());

                    result.addRelationship(Relationship.builder()
                                                       .from(potentialErrorNode.getUid())
                                                       .to(errorCode.getUid())
                                                       .type(HAS_ERROR)
                                                       .build());
                    result.addRelationship(Relationship.builder()
                                                       .from(errorCode.getUid())
                                                       .to(potentialErrorNode.getUid())
                                                       .type(HAS_POTENTIAL_ERROR)
                                                       .build());
                }
            }

            final List<Node> inputDto = ReflectionService.extractInputDto(endpoint.getJavaMethod());
            result.addNode(inputDto);
            for (final Node input : inputDto) {
                result.addRelationship(Relationship.builder()
                                                   .from(input.getUid())
                                                   .to(endpointNode.getUid())
                                                   .type(HAS_INPUT_DTO)
                                                   .build());
            }
            final Node outputDto = ReflectionService.extractOutputDto(endpoint.getJavaMethod());
            if (outputDto != null) {
                result.addNode(outputDto);
                result.addRelationship(Relationship.builder()
                                                   .from(outputDto.getUid())
                                                   .to(endpointNode.getUid())
                                                   .type(HAS_INPUT_DTO)
                                                   .build());
            }
        }
    }

    private Node buildPotentialErrorNode(final PotentialErrorDTO potentialError) {
        final Node.NodeBuilder node = Node.builder();

        node.type("PotentialError");
        node.uid(potentialError.getErrorCode());
        node.name(potentialError.getErrorCode());

        final LinkedHashMap<String, Serializable> properties = new LinkedHashMap<>();
        applyIfNotNull(potentialError.getErrorMessage(), value -> properties.put(MESSAGE, value));
        applyIfNotNull(potentialError.getErrorMessageDetail(), value -> properties.put(MESSAGE_DETAIL, value));
        applyIfNotNull(potentialError.getDescription(), value -> properties.put(ERROR_DESCRIPTION, value));
        applyIfNotNull(potentialError.getExample(), value -> properties.put(EXAMPLE, value));
        applyIfNotNull(potentialError.getHttpStatus(), value -> properties.put(STATUS_CODE, value));
        applyIfNotNull(potentialError.getPayload(), value -> properties.put(PAYLOAD, value));

        node.properties(properties);
        return node.build();

    }

    private boolean existingNode(final Node node,
                                 final Neo4jDao neo4jDao) {
        boolean result = false;
        if (node != null && !EXPOSE.equals(getRelationshipType()) && neo4jDao != null) {
            final org.neo4j.driver.types.Node savedNode = neo4jDao.getNode(node.getUid(), node.getType());
            result = savedNode != null;
        }
        return result;
    }

    protected String getRelationshipType() {
        return EXPOSE;
    }

    private Node convertEndpointToNeo4j(final RestEndpoint endpoint) {
        final String uid = buildServiceUid(endpoint);
        return Node.builder()
                   .uid(encodeSha1(uid))
                   .name(buildName(endpoint))
                   .type(SERVICE)
                   .properties(buildProperties(endpoint, uid))
                   .build();
    }


    private String buildServiceUid(final RestEndpoint endpoint) {
        final JsonBuilder json = new JsonBuilder();
        json.addField(VERB).valueQuot(endpoint.getVerb()).addSeparator();
        json.addField(URI).valueQuot(endpoint.getUri()).addSeparator();

        //@formatter:off
        processIfNotNull(endpoint.getHeaders(), value -> json.addField(HEADER).valueQuot(value).addSeparator());
        processIfNotNull(endpoint.getConsume(), value -> json.addField(ACCEPT).valueQuot(value).addSeparator());
        processIfNotNull(endpoint.getProduce(), value -> json.addField(CONTENT_TYPE).valueQuot(value).addSeparator());
        processIfNotNull(endpoint.getBodyRequireOnly(), value -> json.addField(REQUEST_PAYLOAD).valueQuot(value).addSeparator());
        processIfNotNull(endpoint.getResponseTypeRequireOnly(), value -> json.addField(RESPONSE_PAYLOAD).valueQuot(value).addSeparator());
        //@formatter:on

        return json.toString()
                   .replaceAll(LINE, EMPTY)
                   .replaceAll(TAB, EMPTY)
                   .replaceAll(DOUBLE_URL_SEP, URI_SEP)
                   .replaceAll(QUOT, SIMPLE_QUOT);
    }

    private String encodeSha1(final String value) {
        return value == null ? null : new EncryptionUtils().encodeSha1(value);
    }

    private String buildName(final RestEndpoint endpoint) {
        String result = endpoint.getNickname();

        if (result == null || result.trim().isEmpty()) {
            result = String.format("[%s]%s", endpoint.getVerb(), endpoint.getUri());
        }
        return result;
    }

    private LinkedHashMap<String, Serializable> buildProperties(final RestEndpoint endpoint,
                                                                final String identifier) {
        final LinkedHashMap<String, Serializable> result = new LinkedHashMap<>();
        result.put(VERB, cleanLines(endpoint.getVerb()));
        result.put(URI, cleanLines(endpoint.getUri()));
        result.put(IDENTIFIER, identifier);

        //@formatter:off
        processIfNotEmpty(endpoint.getNickname(), value -> result.put(NICKNAME, value));
        processIfNotEmpty(endpoint.getMethod(), value -> result.put(METHOD, value));
        processIfNotEmpty(endpoint.getHeaders(), value -> result.put(HEADER, value));
        processIfNotEmpty(endpoint.getConsume(), value -> result.put(ACCEPT, value));
        processIfNotEmpty(endpoint.getProduce(), value -> result.put(CONTENT_TYPE, value));
        processIfNotEmpty(endpoint.getBody(), value -> result.put(REQUEST_PAYLOAD, value));
        processIfNotEmpty(endpoint.getResponseType(), value -> result.put(RESPONSE_PAYLOAD, value));
        processIfNotEmpty(endpoint.getDescription(), value -> result.put(DESCRIPTION, value));
        //@formatter:on

        return result;
    }

    // =========================================================================
    // INTERNAL
    // =========================================================================
    protected RestApi analyseClass(final Class<?> clazz) {
        final String name        = getApiName(clazz);
        final String rootContext = getBaseContext(clazz);
        return RestApi.builder()
                      .name(name)
                      .baseContext(rootContext == null ? URI_SEP : URI_SEP + rootContext)
                      .endpoints(resolveEndpoints(clazz, rootContext == null ? EMPTY : rootContext))
                      .build()
                      .orderEndPoint();
    }

    protected String getBaseContext(final Class<?> clazz) {
        String result = EMPTY;

        RequestMapping annotation = getAnnotation(clazz, RequestMapping.class);
        if (annotation == null) {
            annotation = searchRequestMappingInInterface(clazz.getInterfaces());
        }

        if (annotation != null) {
            if (annotation.value().length > 0) {
                result = annotation.value()[0];
            }
            if (result.isEmpty() && annotation.path().length > 0) {
                result = annotation.path()[0];
            }
        }
        return result;
    }

    protected RequestMapping searchRequestMappingInInterface(final Class<?>[] interfaces) {
        RequestMapping result = null;
        if (interfaces != null) {
            for (final Class<?> interfaceClass : interfaces) {
                result = getAnnotation(interfaceClass, RequestMapping.class);
                if (result != null) {
                    break;
                }
            }
        }
        return result;
    }

    protected String getApiName(final Class<?> clazz) {
        return ifHasAnnotation(clazz, Api.class, Api::value, () -> clazz.getSimpleName());
    }


    private List<RestEndpoint> resolveEndpoints(final Class<?> clazz, final String baseContext) {
        final List<RestEndpoint> result = new ArrayList<>();
        for (final Method method : extractMethods(clazz)) {
            if (hasAnnotation(method,
                              RequestMapping.class,
                              GetMapping.class,
                              PostMapping.class,
                              PutMapping.class,
                              PatchMapping.class,
                              DeleteMapping.class)) {
                result.add(resolveEndpoint(method, baseContext, clazz));
            }
        }
        return result;
    }

    private Method[] extractMethods(final Class<?> clazz) {
        final List<Method> result = new ArrayList<>();

        result.addAll(Arrays.asList(clazz.getDeclaredMethods()));
        if (clazz.getInterfaces() != null) {
            for (final Class<?> interfaceClass : clazz.getInterfaces()) {
                result.addAll(Arrays.asList(interfaceClass.getDeclaredMethods()));
            }
        }

        return result.toArray(new Method[]{});
    }

    private RestEndpoint resolveEndpoint(final Method method, final String baseContext, final Class<?> clazz) {
        final RestEndpoint.RestEndpointBuilder builder = RestEndpoint.builder();

        processOnAnnotation(method, RequestMapping.class, (annotation) -> {
            builder.verb(renderVerb(annotation.method()));
            builder.consume(String.join(SEPARATOR, annotation.consumes()));
            builder.produce(String.join(SEPARATOR, annotation.consumes()));
            builder.uri(renderUri(baseContext, annotation.path()));
        });

        processOnAnnotation(method, GetMapping.class, (annotation) -> {
            builder.verb(GET);
            builder.consume(String.join(SEPARATOR, annotation.consumes()));
            builder.produce(String.join(SEPARATOR, annotation.consumes()));
            builder.uri(renderUri(baseContext, annotation.path()));
        });

        processOnAnnotation(method, PostMapping.class, (annotation) -> {
            builder.verb(POST);
            builder.consume(String.join(SEPARATOR, annotation.consumes()));
            builder.produce(String.join(SEPARATOR, annotation.consumes()));
            builder.uri(renderUri(baseContext, annotation.path()));
        });

        processOnAnnotation(method, PutMapping.class, (annotation) -> {
            builder.verb(PUT);
            builder.consume(String.join(SEPARATOR, annotation.consumes()));
            builder.produce(String.join(SEPARATOR, annotation.consumes()));
            builder.uri(renderUri(baseContext, annotation.path()));
        });

        processOnAnnotation(method, PatchMapping.class, (annotation) -> {
            builder.verb(PATCH);
            builder.consume(String.join(SEPARATOR, annotation.consumes()));
            builder.produce(String.join(SEPARATOR, annotation.consumes()));
            builder.uri(renderUri(baseContext, annotation.path()));
        });


        processOnAnnotation(method, DeleteMapping.class, (annotation) -> {
            builder.verb(DELETE);
            builder.consume(String.join(SEPARATOR, annotation.consumes()));
            builder.produce(String.join(SEPARATOR, annotation.consumes()));
            builder.uri(renderUri(baseContext, annotation.path()));
        });

        builder.method(String.join(".", clazz.getName(), method.getName()));
        builder.javaMethod(method);
        builder.headers(extractHeader(method.getParameters()));
        builder.body(extractBody(method.getParameters(), true));
        builder.bodyRequireOnly(extractBody(method.getParameters(), false));
        builder.descriptionDetail(resolveDescription(method));
        final JsonNode payload = renderReturnType(method, true);
        builder.responseType(payload == null ? null : payload.convertToJson());

        final JsonNode payloadRequireOnly = renderReturnType(method, false);
        builder.responseTypeRequireOnly(payloadRequireOnly == null ? null : payloadRequireOnly.convertToJson());
        return builder.build();
    }

    private DescriptionDTO resolveDescription(final Method method) {
        final Description description = getAnnotation(method, Description.class);
        DescriptionDTO    result      = null;
        if (description != null) {
            result = buildDescription(description);
        }
        return result;
    }

    private DescriptionDTO buildDescription(final Description description) {
        final String example = null;

        return DescriptionDTO.builder()
                             .url(description.url())
                             .content(description.value())
                             .example(example)
                             .potentialErrors(ReflectionService.convertPotentialErrors(description.potentialErrors()))
                             .build();
    }


    private String extractHeader(final Parameter[] parameters) {
        final List<String> result = new ArrayList<>();
        for (final Parameter parameter : parameters) {
            processOnAnnotation(parameter, RequestHeader.class, (header) -> result.add(header.value()));
        }
        return String.join(SEPARATOR, result);
    }

    private String extractBody(final Parameter[] parameters, final boolean strict) {
        String result = null;
        for (final Parameter parameter : parameters) {
            if (hasAnnotation(parameter, RequestBody.class)) {
                final JsonNode node = renderParameterType(parameter, strict);
                result = node == null ? null : node.convertToJson();
                break;
            }
        }
        return result;
    }


    private String renderUri(final String baseContext, final String[] paths) {
        final List<String> result             = new ArrayList<>();
        String             currentBaseContext = baseContext == null || baseContext.isEmpty() ? EMPTY : URI_SEP + baseContext;
        if (baseContext != null && !baseContext.endsWith(URI_SEP)) {
            currentBaseContext = currentBaseContext + URI_SEP;
        }

        result.add(currentBaseContext);
        result.addAll(Arrays.asList(paths));

        return String.join(EMPTY, result).replaceAll(DOUBLE_URL_SEP, URI_SEP);
    }

    private String renderVerb(final RequestMethod[] method) {
        final List<String> result = new ArrayList<>();
        if (method != null) {
            for (final RequestMethod requestMethod : method) {
                result.add(requestMethod.name());
            }
        }
        return String.join(UNDERSCORE, result);
    }

}
