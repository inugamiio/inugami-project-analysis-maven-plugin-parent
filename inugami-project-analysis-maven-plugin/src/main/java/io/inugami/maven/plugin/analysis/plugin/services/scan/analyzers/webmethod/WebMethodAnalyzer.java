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
package io.inugami.maven.plugin.analysis.plugin.services.scan.analyzers.webmethod;

import io.inugami.api.models.JsonBuilder;
import io.inugami.api.models.data.basic.JsonObject;
import io.inugami.maven.plugin.analysis.annotations.Description;
import io.inugami.maven.plugin.analysis.annotations.WebServiceContextInfo;
import io.inugami.maven.plugin.analysis.api.actions.ClassAnalyzer;
import io.inugami.maven.plugin.analysis.api.models.Node;
import io.inugami.maven.plugin.analysis.api.models.Relationship;
import io.inugami.maven.plugin.analysis.api.models.ScanConext;
import io.inugami.maven.plugin.analysis.api.models.ScanNeo4jResult;
import io.inugami.maven.plugin.analysis.api.models.rest.RestApi;
import io.inugami.maven.plugin.analysis.api.models.rest.RestEndpoint;
import io.inugami.maven.plugin.analysis.api.tools.RestAnalyzerUtils;
import io.inugami.maven.plugin.analysis.api.utils.reflection.*;
import io.inugami.maven.plugin.analysis.functional.CheckUtils;
import lombok.extern.slf4j.Slf4j;
import org.jboss.ws.api.annotation.WebContext;

import javax.annotation.security.RolesAllowed;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static io.inugami.maven.plugin.analysis.api.tools.BuilderTools.buildNodeVersion;
import static io.inugami.maven.plugin.analysis.api.tools.RestAnalyzerUtils.*;
import static io.inugami.maven.plugin.analysis.api.utils.Constants.HAS_INPUT_DTO;
import static io.inugami.maven.plugin.analysis.api.utils.reflection.ReflectionService.*;
import static io.inugami.maven.plugin.analysis.functional.FunctionalUtils.applyIfEmpty;
import static io.inugami.maven.plugin.analysis.functional.FunctionalUtils.applyIfNotEmpty;

@Slf4j
public class WebMethodAnalyzer implements ClassAnalyzer {

    // =========================================================================
    // ATTRIBUTES
    // =========================================================================
    public static final String SEPARATOR       = ",";
    public static final String FEATURE_NAME    = "inugami.maven.plugin.analysis.analyzer.webMethod";
    public static final String FEATURE         = FEATURE_NAME + ".enable";
    public static final String POST            = "POST";
    public static final String UTF_8           = "UTF-8";
    public static final String CHARSET         = "; charset=";
    public static final String APPLICATION_XML = "application/xml";
    public static final String URL_SEPARATOR   = "/";

    // =========================================================================
    // ACCEPT
    // =========================================================================
    @Override
    public boolean accept(final Class<?> clazz, final ScanConext context) {
        return isEnable(FEATURE, context, true) && containsWebMethod(clazz);
    }

    private boolean containsWebMethod(final Class<?> clazz) {
        boolean result = hasAnnotation(clazz, WebService.class);
        if (!result) {
            return false;
        }

        final List<Method> methods = loadAllMethods(clazz);

        for (final Method method : methods) {
            result = ReflectionService.hasAnnotation(method, WebMethod.class);
            if (result) {
                break;
            }
        }

        return result;
    }

    // =========================================================================
    // ANALYSE
    // =========================================================================
    @Override
    public List<JsonObject> analyze(final Class<?> clazz, final ScanConext context) {
        log.info("{} : {}", FEATURE_NAME, clazz);
        final ScanNeo4jResult result = ScanNeo4jResult.builder().build();

        final WebServiceInfoDTO webServiceInfo = extractWebServiceInformation(clazz);
        final List<Method>      methods        = loadAllMethods(clazz);
        final Node              projectNode    = buildNodeVersion(context.getProject());
        final List<String>      existingNodes  = new ArrayList<>();

        final RestApi restApi = analyseRestApi(clazz, methods, webServiceInfo);


        for (final RestEndpoint endpoint : Optional.ofNullable(restApi.getEndpoints()).orElse(new ArrayList<>())) {
            final Node node = RestAnalyzerUtils.convertEndpointToNeo4j(endpoint,
                                                                       restEndPoint -> restEndPoint.getNickname());

            if (RestAnalyzerUtils.existingNode(node, context.getNeo4jDao(), EXPOSE)) {
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

        final Node node = buildNodeVersion(context.getProject());
        if (!result.getNodes().isEmpty()) {
            final Node serviceType = Node.builder().type(SERVICE_TYPE).uid(REST).name(REST).build();
            for (final Node service : result.getNodes()) {
                result.addRelationship(Relationship.builder()
                                                   .from(node.getUid())
                                                   .to(service.getUid())
                                                   .type(EXPOSE)
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
                                                   .type(EXPOSE)
                                                   .build());
            }
        }
        return List.of(result);
    }

    protected RestApi analyseRestApi(final Class<?> clazz,
                                     final List<Method> methods,
                                     final WebServiceInfoDTO webServiceInfo) {

        List<RestEndpoint> endpoints = new ArrayList<>();


        for (Method method : methods) {
            final RestEndpoint endpoint = analyseMethod(method, webServiceInfo, clazz);
            if (endpoint != null) {
                endpoints.add(endpoint);
            }
        }


        return RestApi.builder()
                      .name(webServiceInfo.getName())
                      .baseContext(buildURI(webServiceInfo))
                      .endpoints(endpoints)
                      .build()
                      .orderEndPoint();
    }


    private RestEndpoint analyseMethod(final Method method,
                                       final WebServiceInfoDTO webServiceInfo,
                                       final Class<?> clazz) {
        final WebMethod webMethod = method.getAnnotation(WebMethod.class);
        if (webMethod.exclude()) {
            return null;
        }

        final WebServiceInfoDTO currentWebServiceInfo = resolveMethodWebServiceInfo(webServiceInfo, method);
        return resolveRestEndpoint(method, currentWebServiceInfo, clazz);
    }


    // =========================================================================
    // extractors
    // =========================================================================
    protected WebServiceInfoDTO extractWebServiceInformation(final Class<?> clazz) {
        final WebService            webService        = clazz.getAnnotation(WebService.class);
        final SOAPBinding           soapBinding       = clazz.getAnnotation(SOAPBinding.class);
        final RolesAllowed          rolesAllowed      = clazz.getAnnotation(RolesAllowed.class);
        final WebServiceContextInfo webServiceContext = clazz.getAnnotation(WebServiceContextInfo.class);
        final WebContext            webContext        = clazz.getAnnotation(WebContext.class);

        final WebServiceInfoDTO.WebServiceInfoDTOBuilder builder = WebServiceInfoDTO.builder();

        builder.name(webService.name())
               .targetNamespace(webService.targetNamespace())
               .serviceName(webService.serviceName())
               .portName(webService.portName())
               .wsdlLocation(webService.wsdlLocation())
               .endpointInterface(webService.endpointInterface())
               .soap(soapBinding != null || CheckUtils.notEmpty(webService.wsdlLocation()));


        if (rolesAllowed != null && CheckUtils.isEmpty(rolesAllowed.value())) {
            builder.roles(Arrays.asList(rolesAllowed.value()));
        }

        if (webServiceContext != null) {
            builder.rootContext(webServiceContext.rootContext())
                   .authMethod(webServiceContext.authMethod())
                   .urlPattern(webServiceContext.urlPattern())
                   .virtualHost(webServiceContext.virtualHost())
                   .transportGuarantee(webServiceContext.transportGuarantee())
                   .secureWSDLAccess(webServiceContext.secureWSDLAccess())
                   .realmName(webServiceContext.realmName())
                   .consume(webServiceContext.consume())
                   .produce(webServiceContext.produce())
                   .encoding(webServiceContext.encoding())
                   .description(webServiceContext.description());
        }

        if (webContext != null) {
            final WebServiceInfoDTO ref = builder.build();
            applyIfEmpty(ref.getRootContext(), () -> builder.rootContext(webContext.contextRoot()));
            applyIfEmpty(ref.getAuthMethod(), () -> builder.authMethod(webContext.authMethod()));
            applyIfEmpty(ref.getUrlPattern(), () -> builder.urlPattern(webContext.urlPattern()));

            applyIfEmpty(ref.getVirtualHost(), () -> builder.virtualHost(webContext.virtualHost()));
            applyIfEmpty(ref.getTransportGuarantee(),
                         () -> builder.transportGuarantee(webContext.transportGuarantee()));

            applyIfEmpty(ref.getRealmName(), () -> builder.realmName(webContext.realmName()));
            if (!ref.isSecureWSDLAccess()) {
                builder.secureWSDLAccess(webContext.secureWSDLAccess());
            }

        }

        return builder.build();
    }


    // =========================================================================
    // RESOLVER
    // =========================================================================
    protected WebServiceInfoDTO resolveMethodWebServiceInfo(final WebServiceInfoDTO webServiceInfo,
                                                            final Method method) {
        WebServiceInfoDTO           result                = webServiceInfo;
        final WebServiceContextInfo webServiceContextInfo = method.getAnnotation(WebServiceContextInfo.class);
        if (webServiceContextInfo != null) {
            final WebServiceInfoDTO.WebServiceInfoDTOBuilder builder = webServiceInfo.toBuilder();

            applyIfNotEmpty(webServiceContextInfo.rootContext(), builder::rootContext);
            applyIfNotEmpty(webServiceContextInfo.urlPattern(), builder::urlPattern);
            applyIfNotEmpty(webServiceContextInfo.virtualHost(), builder::virtualHost);
            applyIfNotEmpty(webServiceContextInfo.transportGuarantee(), builder::transportGuarantee);
            applyIfNotEmpty(webServiceContextInfo.realmName(), builder::realmName);
            applyIfNotEmpty(webServiceContextInfo.consume(), builder::consume);
            applyIfNotEmpty(webServiceContextInfo.produce(), builder::produce);
            applyIfNotEmpty(webServiceContextInfo.encoding(), builder::encoding);
            applyIfNotEmpty(webServiceContextInfo.description(), builder::description);

            if (webServiceContextInfo.secureWSDLAccess()) {
                builder.secureWSDLAccess(true);
            }

            result = builder.build();
        }
        return result;
    }


    protected RestEndpoint resolveRestEndpoint(final Method method,
                                               final WebServiceInfoDTO info,
                                               final Class<?> clazz) {

        final RestEndpoint.RestEndpointBuilder builder    = RestEndpoint.builder();
        final String                           methodName = method.getName();
        final String                           namespace  = info.getTargetNamespace();
        final boolean                          isSoap     = info.isSoap();
        final Parameter[]                      parameters = method.getParameters();


        final boolean deprecated = method.getAnnotation(Deprecated.class) != null;
        builder.deprecated(deprecated);
        builder.verb(POST);
        builder.description(info.getDescription());
        builder.nickname(String.join(RestAnalyzerUtils.DOT, info.getName(), method.getName()));
        builder.uri(buildURI(info));

        final String encoding = CHARSET + orElse(info.getEncoding(), UTF_8);
        builder.consume(orElse(info.getConsume(), APPLICATION_XML) + encoding);
        builder.produce(orElse(info.getProduce(), APPLICATION_XML) + encoding);
        builder.uri(orElse(info.getRootContext(), URL_SEPARATOR) + info.getUrlPattern());

        builder.method(String.join(".", clazz.getName(), method.getName()));
        builder.javaMethod(method);

        builder.headers(resolveHeaders(method, info.getHeaders()));

        final DescriptionDTO methodDescription = extractDescription(method.getDeclaredAnnotation(Description.class));

        final List<JsonNode> nodes = resolveBodyNode(parameters, true);
        builder.body(resolveBody(nodes, true, deprecated, isSoap, methodName, namespace, methodDescription));

        final List<JsonNode> nodesNonStrict = resolveBodyNode(parameters, false);
        builder.bodyRequireOnly(
                resolveBody(nodesNonStrict, false, deprecated, isSoap, methodName, namespace, methodDescription));
        builder.responseType(
                resolveResponse(method, true, isSoap, deprecated, methodName, namespace, methodDescription));
        builder.responseTypeRequireOnly(
                resolveResponse(method, false, isSoap, deprecated, methodName, namespace, methodDescription));


        builder.descriptionDetail(resolveDescriptionDetail(methodDescription, nodes));
        return builder.build();
    }

    private DescriptionDTO resolveDescriptionDetail(final DescriptionDTO methodDescription,
                                                    final List<JsonNode> nodes) {

        final DescriptionDTO.DescriptionDTOBuilder result = methodDescription == null
                                                            ? DescriptionDTO.builder()
                                                            : methodDescription.toBuilder();

        Set<PotentialErrorDTO>        errors      = new LinkedHashSet<>();
        final List<PotentialErrorDTO> methodError = result.build().getPotentialErrors();
        if (methodError != null) {
            errors.addAll(methodError);
        }


        if (nodes != null) {
            for (JsonNode node : nodes) {
                errors.addAll(extractErrorFromNode(node));
            }
        }

        List<PotentialErrorDTO> potentialErrors = new ArrayList<>(errors);
        Collections.sort(potentialErrors);
        result.potentialErrors(potentialErrors);
        return result.build();
    }

    private List<PotentialErrorDTO> extractErrorFromNode(final JsonNode node) {
        List<PotentialErrorDTO> result = new ArrayList<>();
        if (node == null) {
            return result;
        }

        if (node.getDescription() != null && node.getDescription().getPotentialErrors() != null) {
            result.addAll(node.getDescription().getPotentialErrors());
        }
        if (node.getChildren() != null) {
            for (JsonNode child : node.getChildren()) {
                result.addAll(extractErrorFromNode(child));
            }
        }
        return result;
    }


    private String resolveHeaders(final Method method, final Set<WebServiceHeaderDTO> headers) {
        return null;
    }


    protected List<JsonNode> resolveBodyNode(final Parameter[] parameters, final boolean strict) {
        List<JsonNode> nodes = new ArrayList<>();
        for (Parameter parameter : parameters) {
            final JsonNode node = renderParameterType(parameter, strict);
            if (node == null) {
                continue;
            }
            nodes.add(node.toBuilder()
                          .fieldName(resolveFieldName(parameter))
                          .build());
        }
        return nodes;
    }


    protected String resolveBody(final List<JsonNode> nodes,
                                 final boolean strict,
                                 final boolean deprecated,
                                 final boolean isXML,
                                 final String methodName,
                                 final String namespace,
                                 final DescriptionDTO methodDescription) {

        Function<JsonNode, String> mapper = isXML ? (n) -> n.toXML(5, strict)
                                                  : JsonNode::convertToJson;

        final String content = String.join("\n", nodes.stream().map(mapper).collect(Collectors.toList()));

        return isXML ? buildEnvelope(content, methodName, namespace, methodDescription, strict, nodes, deprecated)
                     : wrapJson(content);
    }


    private String resolveResponse(final Method method,
                                   final boolean strict,
                                   final boolean isXML,
                                   final boolean deprecated,
                                   final String methodName,
                                   final String namespace,
                                   final DescriptionDTO methodDescription) {

        final JsonNode node = renderReturnType(method, strict);
        final String content = node == null ? "null"
                                            : (isXML ? node.toXML(5, strict) : node.convertToJson());

        return isXML ? buildEnvelope(content, methodName, namespace, methodDescription, strict, new ArrayList<>(),
                                     deprecated)
                     : wrapJson(content);
    }


    private String buildEnvelope(final String content,
                                 final String methodName,
                                 final String namespace,
                                 final DescriptionDTO methodDescription,
                                 final boolean strict,
                                 final List<JsonNode> nodes,
                                 final boolean deprecated) {
        JsonBuilder xml = new JsonBuilder();
        xml.write(JsonNodeXmlRenderer.renderRootDescription(methodDescription, strict, nodes, deprecated));
        xml.write("<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" ");
        xml.write("xmlns:ns=\"").write(namespace).write("\">\"");
        xml.line();

        xml.tab().write("<soapenv:Header/>").line();

        xml.tab().write("<soapenv:Body>").line();
        xml.tab().tab().write("<ns:").write(methodName).write(">").line();
        xml.write(content).line();
        xml.tab().tab().write("</ns:").write(methodName).write(">").line();
        xml.tab().write("</soapenv:Body>").line();
        xml.write("</soapenv:Envelope>").line();

        return xml.toString();
    }

    private String wrapJson(final String content) {
        JsonBuilder json = new JsonBuilder();
        json.openList().line();
        json.write(content);
        json.closeList().line();
        return json.toString();
    }

    private String resolveFieldName(final Parameter parameter) {
        WebParam webParam = parameter.getAnnotation(WebParam.class);
        return webParam == null ? parameter.getName() : webParam.name();
    }

    // =========================================================================
    // TOOLS
    // =========================================================================
    private String orElse(String value, String defaultValue) {
        return CheckUtils.notEmpty(value) ? value : defaultValue;
    }

    private String buildURI(final WebServiceInfoDTO webServiceInfo) {
        final StringBuilder result = new StringBuilder(orElse(webServiceInfo.getRootContext(), URL_SEPARATOR));
        result.append(orElse(webServiceInfo.getUrlPattern(), EMPTY));
        return result.toString();
    }

}
