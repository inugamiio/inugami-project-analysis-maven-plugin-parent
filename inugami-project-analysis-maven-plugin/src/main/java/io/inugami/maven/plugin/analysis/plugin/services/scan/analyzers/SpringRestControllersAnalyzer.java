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

import io.inugami.api.models.data.basic.JsonObject;
import io.inugami.maven.plugin.analysis.api.actions.ClassAnalyzer;
import io.inugami.maven.plugin.analysis.api.models.Node;
import io.inugami.maven.plugin.analysis.api.models.Relationship;
import io.inugami.maven.plugin.analysis.api.models.ScanConext;
import io.inugami.maven.plugin.analysis.api.models.ScanNeo4jResult;
import io.inugami.maven.plugin.analysis.api.models.rest.RestApi;
import io.inugami.maven.plugin.analysis.api.models.rest.RestEndpoint;
import io.inugami.maven.plugin.analysis.api.tools.RestAnalyzerUtils;
import io.inugami.maven.plugin.analysis.api.utils.reflection.JsonNode;
import io.inugami.maven.plugin.analysis.api.utils.reflection.ReflectionService;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;

import static io.inugami.maven.plugin.analysis.api.tools.BuilderTools.buildNodeVersion;
import static io.inugami.maven.plugin.analysis.api.tools.RestAnalyzerUtils.*;
import static io.inugami.maven.plugin.analysis.api.utils.Constants.HAS_INPUT_DTO;
import static io.inugami.maven.plugin.analysis.api.utils.reflection.ReflectionService.*;

@Slf4j
public class SpringRestControllersAnalyzer implements ClassAnalyzer {
    public static final String FEATURE_NAME = "inugami.maven.plugin.analysis.analyzer.restControllers";
    public static final String FEATURE      = FEATURE_NAME + ".enable";


    public static final String GET    = "GET";
    public static final String POST   = "POST";
    public static final String PUT    = "PUT";
    public static final String DELETE = "DELETE";

    private static final Class<? extends Annotation> restControllerAnnotation = null;
    private static final Boolean                     springContext            = null;
    public static final  String                      UNDERSCORE               = "_";

    // =========================================================================
    // API
    // =========================================================================
    @Override
    public boolean accept(final Class<?> clazz, final ScanConext context) {
        return isEnable(FEATURE, context, true) && clazz.getAnnotation(RestController.class) != null;
    }


    @Override
    public List<JsonObject> analyze(final Class<?> clazz, final ScanConext context) {
        log.info("{} : {}", FEATURE_NAME, clazz);

        final RestApi restApi = analyseClass(clazz);

        final List<String> existingNodes = new ArrayList<>();

        final ScanNeo4jResult result = ScanNeo4jResult.builder().build();
        if (restApi != null && restApi.getEndpoints() != null) {
            for (final RestEndpoint endpoint : restApi.getEndpoints()) {

                final Node node = RestAnalyzerUtils.convertEndpointToNeo4j(endpoint);
                if (node != null) {
                    if (RestAnalyzerUtils.existingNode(node, context.getNeo4jDao(), getRelationshipType())) {
                        existingNodes.add(node.getUid());
                    }
                    else {
                        result.addNode(node);
                    }

                    final List<Node> inputDto = ReflectionService.extractInputDto(endpoint.getJavaMethod());
                    result.addNode(inputDto);
                    for (Node input : inputDto) {
                        result.addRelationship(Relationship.builder()
                                                           .from(input.getUid())
                                                           .to(node.getUid())
                                                           .type(HAS_INPUT_DTO)
                                                           .build());
                    }
                    final Node outputDto = ReflectionService.extractOutputDto(endpoint.getJavaMethod());
                    if (outputDto != null) {
                        result.addNode(outputDto);
                        result.addRelationship(Relationship.builder()
                                                           .from(outputDto.getUid())
                                                           .to(node.getUid())
                                                           .type(HAS_INPUT_DTO)
                                                           .build());
                    }
                }


            }
        }

        final Node node = buildNodeVersion(context.getProject());
        if (!result.getNodes().isEmpty()) {
            final Node serviceType = Node.builder().type(SERVICE_TYPE).uid(REST).name(REST).build();
            for (final Node service : result.getNodes()) {
                result.addRelationship(Relationship.builder()
                                                   .from(node.getUid())
                                                   .to(service.getUid())
                                                   .type(getRelationshipType())
                                                   .build(),

                                       Relationship.builder()
                                                   .from(service.getUid())
                                                   .to(serviceType.getUid())
                                                   .type(SERVICE_TYPE_RELATIONSHIP)
                                                   .build());
            }

            result.addNode(node, serviceType);
        }

        if (!existingNodes.isEmpty()) {
            for (final String service : existingNodes) {
                result.addRelationship(Relationship.builder()
                                                   .from(node.getUid())
                                                   .to(service)
                                                   .type(getRelationshipType())
                                                   .build());
            }
        }
        return List.of(result);
    }


    protected String getRelationshipType() {
        return EXPOSE;
    }


    // =========================================================================
    // INTERNAL
    // =========================================================================
    protected RestApi analyseClass(final Class<?> clazz) {
        final String name        = getApiName(clazz);
        final String baseContext = getBaseContext(clazz);
        return RestApi.builder()
                      .name(name)
                      .baseContext(URI_SEP + baseContext)
                      .endpoints(resolveEndpoints(clazz, baseContext, true))
                      .build()
                      .orderEndPoint();
    }

    protected String getBaseContext(final Class<?> clazz) {
        String result = RestAnalyzerUtils.EMPTY;

        RequestMapping annotation = clazz.getDeclaredAnnotation(RequestMapping.class);
        if (annotation != null) {
            if (annotation.value() != null && annotation.value().length > 0) {
                result = annotation.value()[0];
            }
            if (result.isEmpty() && annotation.path() != null && annotation.path().length > 0) {
                result = annotation.path()[0];
            }
        }
        return result;
    }

    protected String getApiName(final Class<?> clazz) {
        return ifHasAnnotation(clazz, Api.class, Api::value, () -> clazz.getSimpleName());
    }


    private List<RestEndpoint> resolveEndpoints(final Class<?> clazz, final String baseContext, final boolean strict) {
        final List<RestEndpoint> result = new ArrayList<>();
        for (final Method method : clazz.getMethods()) {
            if (hasAnnotation(method, RequestMapping.class, GetMapping.class, PostMapping.class, PutMapping.class,
                              DeleteMapping.class)) {
                result.add(resolveEndpoint(method, baseContext, clazz, strict));
            }
        }
        return result;
    }

    private RestEndpoint resolveEndpoint(final Method method, final String baseContext, final Class<?> clazz,
                                         final boolean strict) {
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

        final JsonNode payload = renderReturnType(method, true);
        builder.responseType(payload == null ? null : payload.convertToJson());

        final JsonNode payloadRequireOnly = renderReturnType(method, false);
        builder.responseTypeRequireOnly(payloadRequireOnly == null ? null : payloadRequireOnly.convertToJson());
        return builder.build();
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
        final List<String> result  = new ArrayList<>();
        final String       context = baseContext == null ? EMPTY : URI_SEP + baseContext;
        result.add(context);
        for (final String path : paths) {
            result.add(path);
        }
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
