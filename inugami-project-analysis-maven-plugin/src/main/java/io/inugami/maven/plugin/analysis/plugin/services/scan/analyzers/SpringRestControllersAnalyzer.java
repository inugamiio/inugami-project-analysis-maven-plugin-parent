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
import io.inugami.maven.plugin.analysis.api.actions.ClassAnalyzer;
import io.inugami.maven.plugin.analysis.api.models.Node;
import io.inugami.maven.plugin.analysis.api.models.Relationship;
import io.inugami.maven.plugin.analysis.api.models.ScanConext;
import io.inugami.maven.plugin.analysis.api.models.ScanNeo4jResult;
import io.inugami.maven.plugin.analysis.api.models.rest.RestApi;
import io.inugami.maven.plugin.analysis.api.models.rest.RestEndpoint;
import io.inugami.maven.plugin.analysis.api.utils.reflection.JsonNode;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import static io.inugami.maven.plugin.analysis.api.tools.BuilderTools.buildNodeVersion;
import static io.inugami.maven.plugin.analysis.api.utils.NodeUtils.*;
import static io.inugami.maven.plugin.analysis.api.utils.reflection.ReflectionService.*;

@Slf4j
public class SpringRestControllersAnalyzer implements ClassAnalyzer {
    public static final String FEATURE_NAME = "inugami.maven.plugin.analysis.analyzer.restControllers";
    public static final String FEATURE      = FEATURE_NAME + ".enable";
    public static final String STRICT       = "inugami.maven.plugin.analysis.analyzer.restControllers.strict";

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

    private static final Class<? extends Annotation> restControllerAnnotation = null;
    private static final Boolean                     springContext            = null;

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
        final boolean strict  = context.getConfiguration().grabBoolean(STRICT, true);
        final RestApi restApi = analyseClass(clazz, strict);

        final ScanNeo4jResult result = ScanNeo4jResult.builder().build();
        if (restApi != null && restApi.getEndpoints() != null) {
            for (final RestEndpoint endpoint : restApi.getEndpoints()) {

                final Node node = convertEndpointToNeo4j(endpoint);
                if (node != null) {
                    result.addNode(node);
                }


            }
        }

        if (!result.getNodes().isEmpty()) {
            final Node node        = buildNodeVersion(context.getProject());
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
        return List.of(result);
    }

    protected String getRelationshipType() {
        return EXPOSE;
    }

    private Node convertEndpointToNeo4j(final RestEndpoint endpoint) {
        return Node.builder()
                   .uid(buildServiceUid(endpoint))
                   .name(buildName(endpoint))
                   .type(SERVICE)
                   .properties(buildProperties(endpoint))
                   .build()
                ;
    }


    private String buildServiceUid(final RestEndpoint endpoint) {
        final JsonBuilder json = new JsonBuilder();
        json.addField(VERB).valueQuot(endpoint.getVerb()).addSeparator();
        json.addField(URI).valueQuot(endpoint.getUri()).addSeparator();

        //@formatter:off
        processIfNotNull(endpoint.getHeaders(),     (value)-> json.addField(HEADER).valueQuot(endpoint.getHeaders()).addSeparator());
        processIfNotNull(endpoint.getConsume(),     (value)-> json.addField(ACCEPT).valueQuot(endpoint.getConsume()).addSeparator());
        processIfNotNull(endpoint.getProduce(),     (value)-> json.addField(CONTENT_TYPE).valueQuot(endpoint.getProduce()).addSeparator());
        processIfNotNull(endpoint.getBody(),        (value)-> json.addField(REQUEST_PAYLOAD).valueQuot(endpoint.getBody()).addSeparator());
        processIfNotNull(endpoint.getResponseType(),(value)-> json.addField(RESPONSE_PAYLOAD).valueQuot(endpoint.getResponseType()).addSeparator());
        processIfNotNull(endpoint.getMethod(),     (value)-> json.addField(METHOD).valueQuot(endpoint.getHeaders()).addSeparator());
        //@formatter:on

        return new EncryptionUtils().encodeSha1(json.toString());
    }

    private String buildName(final RestEndpoint endpoint) {
        String result = endpoint.getNickname();

        if (result == null || result.trim().isEmpty()) {
            result = String.format("[%s]%s", endpoint.getVerb(), endpoint.getUri());
        }
        return result;
    }

    private LinkedHashMap<String, Serializable> buildProperties(final RestEndpoint endpoint) {
        final LinkedHashMap<String, Serializable> result = new LinkedHashMap<>();
        result.put(VERB, cleanLines(endpoint.getVerb()));
        result.put(URI, cleanLines(endpoint.getUri()));

        //@formatter:off
        processIfNotEmpty(endpoint.getNickname(),     (value)->result.put(NICKNAME, endpoint.getNickname()));
         processIfNotEmpty(endpoint.getMethod(),      (value)->result.put(METHOD, value));
        processIfNotEmpty(endpoint.getHeaders(),      (value)->result.put(HEADER, endpoint.getHeaders()));
        processIfNotEmpty(endpoint.getConsume(),      (value)->result.put(ACCEPT, endpoint.getConsume()));
        processIfNotEmpty(endpoint.getProduce(),      (value)->result.put(CONTENT_TYPE, endpoint.getProduce()));
        processIfNotEmpty(endpoint.getBody(),         (value)->result.put(REQUEST_PAYLOAD, endpoint.getBody()));
        processIfNotEmpty(endpoint.getResponseType(), (value)->result.put(RESPONSE_PAYLOAD, endpoint.getResponseType()));
        processIfNotEmpty(endpoint.getDescription(),  (value)->result.put(DESCRIPTION, endpoint.getDescription()));
        //@formatter:on

        return result;
    }

    // =========================================================================
    // INTERNAL
    // =========================================================================
    protected RestApi analyseClass(final Class<?> clazz, final boolean strict) {
        final String name        = getApiName(clazz);
        final String baseContext = getBaseContext(clazz);
        return RestApi.builder()
                      .name(name)
                      .baseContext(URI_SEP + baseContext)
                      .endpoints(resolveEndpoints(clazz, baseContext, strict))
                      .build()
                      .orderEndPoint();
    }

    protected String getBaseContext(final Class<?> clazz) {
        return ifHasAnnotation(clazz, RestController.class, RestController::value);
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
        builder.headers(extractHeader(method.getParameters()));
        builder.body(extractBody(method.getParameters(), strict));

        final JsonNode payload = renderReturnType(method, strict);
        builder.responseType(payload == null ? null : payload.convertToJson());

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
        final String       context = baseContext == null ? "" : URI_SEP + baseContext;
        result.add(context);
        for (final String path : paths) {
            result.add(path);
        }
        return String.join("", result);
    }

    private String renderVerb(final RequestMethod[] method) {
        final List<String> result = new ArrayList<>();
        if (method != null) {
            for (final RequestMethod requestMethod : method) {
                result.add(requestMethod.name());
            }
        }
        return String.join("_", result);
    }

}
