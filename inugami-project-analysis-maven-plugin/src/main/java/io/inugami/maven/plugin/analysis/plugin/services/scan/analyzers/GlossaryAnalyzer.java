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

import io.inugami.api.documentation.Glossaries;
import io.inugami.api.documentation.Glossary;
import io.inugami.api.models.data.basic.JsonObject;
import io.inugami.maven.plugin.analysis.api.actions.ClassAnalyzer;
import io.inugami.maven.plugin.analysis.api.models.Node;
import io.inugami.maven.plugin.analysis.api.models.Relationship;
import io.inugami.maven.plugin.analysis.api.models.ScanConext;
import io.inugami.maven.plugin.analysis.api.models.ScanNeo4jResult;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import static io.inugami.maven.plugin.analysis.api.tools.BuilderTools.buildNodeVersion;
import static io.inugami.maven.plugin.analysis.api.utils.NodeUtils.processIfNotEmpty;
import static io.inugami.maven.plugin.analysis.api.utils.reflection.ReflectionService.hasAnnotation;
import static io.inugami.maven.plugin.analysis.api.utils.reflection.ReflectionService.loadAllFields;

@Slf4j
public class GlossaryAnalyzer implements ClassAnalyzer {


    // =========================================================================
    // ATTRIBUTES
    // =========================================================================
    public static final String FEATURE_NAME = "inugami.maven.plugin.analysis.analyzer.glossary";
    public static final String FEATURE      = FEATURE_NAME + ".enable";
    public static final String VALUE        = "value";
    public static final String LABEL        = "label";
    public static final String LANGUAGE     = "language";
    public static final String DESCRIPTION  = "description";
    public static final String GLOSSARY     = "Glossary";
    public static final String TYPE         = "type";


    // =========================================================================
    // ACCEPT
    // =========================================================================
    @Override
    public boolean accept(final Class<?> clazz, final ScanConext context) {
        return isEnable(FEATURE, context, true) && containsAnnotation(clazz);
    }

    private boolean containsAnnotation(final Class<?> clazz) {
        boolean result = clazz.getDeclaredAnnotation(Glossary.class) != null || clazz.getDeclaredAnnotation(Glossaries.class) != null;

        if (!result) {
            final Set<Field> fields = loadAllFields(clazz);

            for (final Field field : fields) {
                result = field.getDeclaredAnnotation(Glossary.class) != null || field.getDeclaredAnnotation(Glossaries.class) != null;
                if (result) {
                    break;
                }
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
        final ScanNeo4jResult result      = ScanNeo4jResult.builder().build();
        final Node            projectNode = buildNodeVersion(context.getProject());
        final Set<Field>      fields      = loadAllFields(clazz);


        final List<Node> nodes = buildClassNodes(clazz);


        for (final Field field : fields) {
            if (hasAnnotation(field, Glossary.class)) {
                nodes.add(buildNode(field, field.getAnnotation(Glossary.class)));

            } else if (hasAnnotation(field, Glossaries.class)) {
                final Glossaries glossaries = field.getAnnotation(Glossaries.class);
                if (glossaries.value() != null) {
                    for (final Glossary glossary : glossaries.value()) {
                        nodes.add(buildNode(field, field.getAnnotation(Glossary.class)));
                    }
                }
            }
        }


        result.addNode(nodes);
        result.addRelationship(buildRelationships(projectNode, nodes));

        return List.of(result);
    }


    // =========================================================================
    // BUILDER
    // =========================================================================
    private List<Node> buildClassNodes(final Class<?> clazz) {
        final List<Node> result = new ArrayList<>();

        if (hasAnnotation(clazz, Glossary.class)) {
            final Glossary glossary = clazz.getAnnotation(Glossary.class);
            result.add(buildNode(clazz, glossary));

        } else if (hasAnnotation(clazz, Glossaries.class)) {
            final Glossaries glossaries = clazz.getAnnotation(Glossaries.class);
            if (glossaries.value() != null) {
                for (final Glossary glossary : glossaries.value()) {
                    result.add(buildNode(clazz, glossary));
                }
            }
        }
        return result;
    }


    private Node buildNode(final Class<?> clazz, final Glossary glossary) {
        final String                              uid            = String.join("_", clazz.getSimpleName(), glossary.value());
        final LinkedHashMap<String, Serializable> additionalInfo = new LinkedHashMap<>();
        additionalInfo.put(VALUE, clazz.getSimpleName());
        processIfNotEmpty(glossary.value(), value -> additionalInfo.put(LABEL, value));
        processIfNotEmpty(glossary.language(), value -> additionalInfo.put(LANGUAGE, value));
        processIfNotEmpty(glossary.description(), value -> additionalInfo.put(DESCRIPTION, value));

        return Node.builder()
                   .type(GLOSSARY)
                   .name(uid)
                   .uid(uid)
                   .properties(additionalInfo)
                   .build();

    }

    private Node buildNode(final Field field, final Glossary glossary) {
        final String                              uid            = String.join("_", field.getName(), glossary.value());
        final LinkedHashMap<String, Serializable> additionalInfo = new LinkedHashMap<>();
        additionalInfo.put(VALUE, field.getName());
        processIfNotEmpty(glossary.value(), value -> additionalInfo.put(LABEL, value));
        processIfNotEmpty(glossary.language(), value -> additionalInfo.put(LANGUAGE, value));
        processIfNotEmpty(glossary.description(), value -> additionalInfo.put(DESCRIPTION, value));
        processIfNotEmpty(field.getType().getName(), value -> additionalInfo.put(TYPE, value));

        return Node.builder()
                   .type(GLOSSARY)
                   .name(uid)
                   .uid(uid)
                   .properties(additionalInfo)
                   .build();

    }

    private List<Relationship> buildRelationships(final Node projectNode,
                                                  final List<Node> glossaries) {

        final List<Relationship> result = new ArrayList<>();

        for (final Node glossary : glossaries) {
            result.add(Relationship.builder()
                                   .from(projectNode.getUid())
                                   .to(glossary.getUid())
                                   .type("GLOSSARY")
                                   .build());

            result.add(Relationship.builder()
                                   .to(projectNode.getUid())
                                   .from(glossary.getUid())
                                   .type("HAS_GLOSSARY")
                                   .build());
        }
        return result;
    }


}
