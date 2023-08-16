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
package io.inugami.maven.plugin.analysis.api.tools;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import io.inugami.api.exceptions.UncheckedException;
import io.inugami.api.loggers.Loggers;
import io.inugami.api.processors.ConfigHandler;
import io.inugami.maven.plugin.analysis.api.models.Gav;
import io.inugami.maven.plugin.analysis.api.models.Node;
import io.inugami.maven.plugin.analysis.api.models.Relationship;
import io.inugami.maven.plugin.analysis.api.utils.ObjectMapperBuilder;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.project.MavenProject;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.inugami.maven.plugin.analysis.api.utils.NodeUtils.processIfNotNull;
import static io.inugami.maven.plugin.analysis.api.utils.reflection.ReflectionService.isBasicType;

@SuppressWarnings({"java:S6395","java:S6353","java:S1125"})
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Slf4j
public final class BuilderTools {

    // =========================================================================
    // ATTRIBUTES
    // =========================================================================
    public static final String PARENT = "PARENT";

    public static final String  MINOR         = "minor";
    public static final String  PATCH         = "patch";
    public static final String  TAG           = "tag";
    public static final Pattern VERSION_REGEX = Pattern.compile(
            "(?<major>[0-9]+)((?:[.])(?<minor>[0-9]+)){0,1}((?:[.])(?<patch>[0-9]+)){0,1}((?:[-._])(?<tag>.*)){0,1}");
    public static final String  RELEASE       = "RELEASE";
    public static final String  SNAPSHOT      = "SNAPSHOT";

    public static final String RELATION_HAS_METHOD = "HAS_METHOD";
    public static final String RELATION_USE_BY     = "USE_BY";
    public static final String SEP                 = "_";
    public static final String GROUP_ID            = "groupId";
    public static final String ARTIFACT_ID         = "artifactId";
    public static final String VERSION             = "version";
    public static final String PACKAGING           = "packaging";
    public static final String MAJOR               = "major";
    public static final String URL                 = "url";
    public static final String DESCRIPTION         = "description";
    public static final String SCM                 = "scm";
    public static final String DELIMITER           = ":";
    public static final String VERSION_TYPE        = "Version";
    public static final String CLASS               = "class";
    public static final String METHOD              = "method";
    public static final String METHOD_TYPE         = "Method";

    // =========================================================================
    // API
    // =========================================================================
    public static boolean isSnapshotNode(final Node node) {
        return node == null || node.getProperties() == null ? false : SNAPSHOT.equals(node.getProperties().get(TAG));

    }

    public static Node buildNodeVersion(final Gav gav) {
        final LinkedHashMap<String, Serializable> additionalInfo = new LinkedHashMap<>();
        additionalInfo.put(GROUP_ID, gav.getGroupId());
        additionalInfo.put(ARTIFACT_ID, gav.getArtifactId());
        additionalInfo.put(VERSION, gav.getVersion());
        additionalInfo.put(PACKAGING, gav.getType());
        additionalInfo.put(MAJOR, extractMajorVersion(gav.getVersion()));
        additionalInfo.put(MINOR, extractMinorVersion(gav.getVersion()));
        additionalInfo.put(PATCH, extractPatchVersion(gav.getVersion()));
        additionalInfo.put(TAG, extractTag(gav.getVersion()));

        return Node.builder()
                   .type(VERSION_TYPE)
                   .uid(String.join(DELIMITER, gav.getGroupId(), gav.getArtifactId(), gav.getVersion(),
                                    gav.getType()))
                   .name(gav.getVersion())
                   .properties(additionalInfo)
                   .build();
    }

    public static Node buildNodeVersion(final MavenProject project) {
        final LinkedHashMap<String, Serializable> additionalInfo = new LinkedHashMap<>();
        additionalInfo.put(GROUP_ID, project.getGroupId());
        additionalInfo.put(ARTIFACT_ID, project.getArtifactId());
        additionalInfo.put(VERSION, project.getVersion());
        additionalInfo.put(PACKAGING, project.getPackaging());
        additionalInfo.put(MAJOR, extractMajorVersion(project.getVersion()));
        additionalInfo.put(MINOR, extractMinorVersion(project.getVersion()));
        additionalInfo.put(PATCH, extractPatchVersion(project.getVersion()));
        additionalInfo.put(TAG, extractTag(project.getVersion()));

        return Node.builder()
                   .type(VERSION_TYPE)
                   .uid(String.join(DELIMITER, project.getGroupId(), project.getArtifactId(), project.getVersion(),
                                    project.getPackaging()))
                   .name(project.getVersion())
                   .properties(additionalInfo)
                   .build();
    }

    public static Node buildNodeVersionFull(final MavenProject project, final ConfigHandler<String, String> config) {
        final LinkedHashMap<String, Serializable> additionalInfo = new LinkedHashMap<>();
        additionalInfo.put(GROUP_ID, project.getGroupId());
        additionalInfo.put(ARTIFACT_ID, project.getArtifactId());
        additionalInfo.put(VERSION, project.getVersion());
        additionalInfo.put(PACKAGING, project.getPackaging());
        additionalInfo.put(MAJOR, extractMajorVersion(project.getVersion()));
        additionalInfo.put(MINOR, extractMinorVersion(project.getVersion()));
        additionalInfo.put(PATCH, extractPatchVersion(project.getVersion()));
        additionalInfo.put(TAG, extractTag(project.getVersion()));

        processIfNotNull(project.getDescription(), value -> additionalInfo.put(DESCRIPTION, value));
        processIfNotNull(project.getUrl(), value -> additionalInfo.put(URL, value));
        if (project.getScm() != null) {
            processIfNotNull(project.getScm().getUrl(), value -> additionalInfo.put(SCM, value));
        }

        final String otherInfo = config.grabOrDefault("inugami.maven.plugin.analysis.additional.info", null);
        if (otherInfo != null) {
            additionalInfo.putAll(buildMoreInformation(otherInfo));
        }
        return Node.builder()
                   .type(VERSION_TYPE)
                   .uid(String.join(DELIMITER, project.getGroupId(), project.getArtifactId(), project.getVersion(),
                                    project.getPackaging()))
                   .name(project.getVersion())
                   .properties(additionalInfo)
                   .build();
    }

    protected static Map<String, Serializable> buildMoreInformation(final String json) {
        final LinkedHashMap<String, Serializable> result       = new LinkedHashMap<>();
        final ObjectMapper                        objectMapper = ObjectMapperBuilder.build();

        try {
            final JsonNode tree = objectMapper.readTree(json);

            result.putAll(flattenJson(tree, null));
        } catch (final JsonProcessingException e) {
            Loggers.CONFIG.error("invalid inugami.maven.plugin.analysis.additional.info format : \n{}", json);
            throw new UncheckedException(e.getMessage(), e);
        }
        return result;
    }

    public static Map<String, Serializable> flattenJson(final JsonNode tree, final String parentPath) {
        final Map<String, Serializable> result = new LinkedHashMap<>();
        if (tree == null) {
            return result;
        }
        switch (tree.getNodeType()) {
            case STRING:
                processIfNotNull(parentPath, path -> result.put(path, tree.textValue()));
                break;
            case ARRAY:
                flatJsonArray(tree, parentPath, result);
                break;
            case BOOLEAN:
                processIfNotNull(parentPath, path -> result.put(path, tree.booleanValue()));
                break;
            case NUMBER:
                processIfNotNull(parentPath, path -> result.put(path, tree.numberValue()));
                break;
            case OBJECT:
                flatJsonObject(tree, parentPath, result);
                break;
            case POJO:
                log.info("{}", tree);
                break;
            default:
                break;
        }
        return result;
    }

    private static void flatJsonArray(final JsonNode tree, final String parentPath, final Map<String, Serializable> result) {
        for (int i = 0; i < tree.size(); i++) {
            String path = (parentPath == null ? "" : parentPath + SEP) + i;
            if (tree.get(i).getNodeType() != JsonNodeType.OBJECT && tree.get(i).getNodeType() != JsonNodeType.ARRAY) {
                path = path + SEP;
            }
            result.putAll(flattenJson(tree.get(i), path));
        }
    }

    private static void flatJsonObject(final JsonNode tree, final String parentPath, final Map<String, Serializable> result) {
        final Iterator<Map.Entry<String, JsonNode>> iterator = tree.fields();
        while (iterator.hasNext()) {
            final Map.Entry<String, JsonNode> field = iterator.next();
            final String                      path  = (parentPath == null ? "" : parentPath + SEP);
            result.putAll(flattenJson(field.getValue(), path + field.getKey()));
        }
    }

    public static Node buildGavNodeArtifact(final Gav gav) {
        final LinkedHashMap<String, Serializable> additionalInfo = new LinkedHashMap<>();
        additionalInfo.put(GROUP_ID, gav.getGroupId());
        additionalInfo.put(ARTIFACT_ID, gav.getArtifactId());
        additionalInfo.put(VERSION, gav.getVersion());
        additionalInfo.put(PACKAGING, gav.getType());

        return Node.builder()
                   .type("Artifact")
                   .uid(String.join(DELIMITER, gav.getGroupId(), gav.getArtifactId(), gav.getType()))
                   .name(gav.getArtifactId())
                   .properties(additionalInfo)
                   .build();
    }

    public static Node buildArtifactNode(final MavenProject project) {
        final LinkedHashMap<String, Serializable> additionalInfo = new LinkedHashMap<>();
        additionalInfo.put(GROUP_ID, project.getGroupId());
        additionalInfo.put(ARTIFACT_ID, project.getArtifactId());
        additionalInfo.put(VERSION, project.getVersion());
        additionalInfo.put(PACKAGING, project.getPackaging());
        return Node.builder()
                   .type("Artifact")
                   .uid(String.join(DELIMITER, project.getGroupId(), project.getArtifactId(), project.getPackaging()))
                   .name(project.getArtifactId())
                   .properties(additionalInfo)
                   .build();
    }


    public static Relationship buildRelationshipArtifact(final Node node, final Node artifactNode) {
        return Relationship.builder()
                           .from(artifactNode.getUid())
                           .to(node.getUid())
                           .type(RELEASE)
                           .build();
    }

    public static Node buildMethodNode(final Class<?> clazz, final Method method) {
        final LinkedHashMap<String, Serializable> additionalInfo = new LinkedHashMap<>();
        additionalInfo.put(CLASS, clazz.getName());
        additionalInfo.put(METHOD, method.getName());
        processIfNotNull(method.getReturnType(), value -> additionalInfo.put("returnType", value.getName()));
        processIfNotNull(method.getParameters(), value -> additionalInfo.put("parameters",
                                                                               buildArgsType(value, false, true)));


        final String uid = String.join(".",
                                       clazz.getName(),
                                       method.getName() + buildArgsType(method.getParameters(), true, false));
        return Node.builder()
                   .type(METHOD_TYPE)
                   .uid(uid)
                   .name(method.getName())
                   .properties(additionalInfo)
                   .build();
    }

    private static String buildArgsType(final Parameter[] parameters, final boolean encapsulate,
                                        final boolean withName) {
        final StringBuilder result = new StringBuilder();
        if (encapsulate) {
            result.append('(');
        }
        if (parameters != null && parameters.length > 0) {
            processBuildArgsType(parameters, withName, result);
        }

        if (encapsulate) {
            result.append(')');
        }
        return result.toString();
    }

    private static void processBuildArgsType(final Parameter[] parameters, final boolean withName, final StringBuilder result) {
        final Iterator<Parameter> iterator = Arrays.asList(parameters).iterator();
        while (iterator.hasNext()) {
            final Parameter param = iterator.next();

            if (withName) {
                result.append(param.getName());
            }
            result.append('<');
            if (isBasicType(param.getType())) {
                result.append(param.getType().getSimpleName());
            } else {
                result.append(param.getType().getName());
            }
            result.append('>');
            if (iterator.hasNext()) {
                result.append(',');
            }
        }
    }


    public static Integer extractMajorVersion(final String version) {
        return extractIntGroup(version, MAJOR);
    }


    public static Integer extractMinorVersion(final String version) {
        return extractIntGroup(version, MINOR);
    }

    public static Integer extractPatchVersion(final String version) {
        return extractIntGroup(version, PATCH);
    }

    public static String extractTag(final String version) {
        String        result  = null;
        final Matcher matcher = VERSION_REGEX.matcher(version);
        if (matcher.matches()) {
            result = matcher.group(TAG);
        }
        return result == null ? "" : result;
    }

    public static Integer extractIntGroup(final String version, final String groupName) {
        Integer       result  = 0;
        final Matcher matcher = VERSION_REGEX.matcher(version);
        if (matcher.matches()) {
            final String value = matcher.group(groupName);
            if (value != null) {
                result = Integer.parseInt(value);
            }
        }
        return result;
    }
}
