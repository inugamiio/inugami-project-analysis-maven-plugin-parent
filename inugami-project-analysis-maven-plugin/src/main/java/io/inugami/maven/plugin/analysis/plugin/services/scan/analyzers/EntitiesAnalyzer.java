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
import io.inugami.maven.plugin.analysis.annotations.EntityDatabase;
import io.inugami.maven.plugin.analysis.api.actions.ClassAnalyzer;
import io.inugami.maven.plugin.analysis.api.models.Node;
import io.inugami.maven.plugin.analysis.api.models.Relationship;
import io.inugami.maven.plugin.analysis.api.models.ScanConext;
import io.inugami.maven.plugin.analysis.api.models.ScanNeo4jResult;
import io.inugami.maven.plugin.analysis.api.utils.reflection.ReflectionService;

import javax.persistence.Entity;
import javax.persistence.Table;
import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.inugami.maven.plugin.analysis.api.tools.BuilderTools.buildNodeVersion;
import static io.inugami.maven.plugin.analysis.api.utils.reflection.ReflectionService.hasAnnotation;

public class EntitiesAnalyzer implements ClassAnalyzer {

    // =========================================================================
    // ATTRIBUTES
    // =========================================================================
    public static final String FEATURE = "inugami.maven.plugin.analysis.analyzer.jms.enable";


    // =========================================================================
    // ACCEPT
    // =========================================================================
    @Override
    public boolean accept(final Class<?> clazz, final ScanConext context) {
        return isEnable(FEATURE, context, true) && hasAnnotation(clazz, Entity.class);
    }


    // =========================================================================
    // ANALYSE
    // =========================================================================
    @Override
    public List<JsonObject> analyze(final Class<?> clazz, final ScanConext context) {
        final ScanNeo4jResult result = ScanNeo4jResult.builder().build();
        final Node artifactNode = buildNodeVersion(context.getProject());

        String entityName = clazz.getSimpleName();
        if (hasAnnotation(clazz, Table.class)) {
            entityName = clazz.getAnnotation(Table.class).name();
        }
        if (hasAnnotation(clazz, EntityDatabase.class)) {
            entityName = clazz.getAnnotation(EntityDatabase.class).value() + "_" + entityName;
        }

        final Map<String, Serializable> localAdditionalInfo = new LinkedHashMap<>();
        localAdditionalInfo.put("payload", ReflectionService.renderType(clazz,null,null).convertToJson());
        final String entityLocalUid = "local@" + entityName;
        final Node localEntityNode = Node.builder()
                                    .type("LocalEntity")
                                    .name(entityLocalUid)
                                    .uid(entityLocalUid)
                                    .properties(localAdditionalInfo)
                                    .build();


        final Map<String, Serializable> additionalInfo = new LinkedHashMap<>();
        additionalInfo.put("payload", ReflectionService.renderType(clazz,null,null,false).convertToJson());
        final Node entityNode = Node.builder()
                                    .type("Entity")
                                    .name(entityName)
                                    .uid(entityName)
                                    .properties(additionalInfo)
                                    .build();

        result.addNode(localEntityNode,entityNode,artifactNode);

        result.addRelationship(Relationship.builder()
                               .from(artifactNode.getUid())
                               .to(localEntityNode.getUid())
                               .type("HAS_LOCAL_ENTITY")
                               .build());

        result.addRelationship(Relationship.builder()
                                           .from(artifactNode.getUid())
                                           .to(entityNode.getUid())
                                           .type("HAS_ENTITY")
                                           .build());


        result.addRelationship(Relationship.builder()
                                           .from(localEntityNode.getUid())
                                           .to(entityNode.getUid())
                                           .type("HAS_ENTITY_REFERENCE")
                                           .build());
        return List.of(result);
    }

}
