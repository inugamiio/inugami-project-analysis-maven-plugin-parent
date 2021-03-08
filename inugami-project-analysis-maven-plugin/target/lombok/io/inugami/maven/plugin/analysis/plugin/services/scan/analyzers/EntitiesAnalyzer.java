// Generated by delombok at Mon Mar 08 22:38:55 CET 2021
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
import io.inugami.commons.security.EncryptionUtils;
import io.inugami.maven.plugin.analysis.annotations.EntityDatabase;
import io.inugami.maven.plugin.analysis.api.actions.ClassAnalyzer;
import io.inugami.maven.plugin.analysis.api.models.Node;
import io.inugami.maven.plugin.analysis.api.models.Relationship;
import io.inugami.maven.plugin.analysis.api.models.ScanConext;
import io.inugami.maven.plugin.analysis.api.models.ScanNeo4jResult;
import io.inugami.maven.plugin.analysis.api.utils.reflection.JsonNode;
import io.inugami.maven.plugin.analysis.api.utils.reflection.ReflectionService;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.List;
import static io.inugami.maven.plugin.analysis.api.tools.BuilderTools.buildNodeVersion;
import static io.inugami.maven.plugin.analysis.api.utils.reflection.ReflectionService.hasAnnotation;

public class EntitiesAnalyzer implements ClassAnalyzer {
    @java.lang.SuppressWarnings("all")
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(EntitiesAnalyzer.class);
    // =========================================================================
    // ATTRIBUTES
    // =========================================================================
    public static final String FEATURE_NAME = "inugami.maven.plugin.analysis.analyzer.jms";
    public static final String FEATURE = FEATURE_NAME + ".enable";
    public static final String LOCAL_ENTITY = "local@";

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
        log.info("{} : {}", FEATURE_NAME, clazz);
        final ScanNeo4jResult result = ScanNeo4jResult.builder().build();
        final Node artifactNode = buildNodeVersion(context.getProject());
        String entityName = clazz.getSimpleName();
        if (hasAnnotation(clazz, Table.class)) {
            entityName = clazz.getAnnotation(Table.class).name();
        }
        if (hasAnnotation(clazz, EntityDatabase.class)) {
            entityName = clazz.getAnnotation(EntityDatabase.class).value() + "_" + entityName;
        }
        final LinkedHashMap<String, Serializable> localAdditionalInfo = new LinkedHashMap<>();
        final JsonNode payloadNode = ReflectionService.renderType(clazz, null, null, false);
        final String payload = payloadNode == null ? null : payloadNode.convertToJson();
        if (payload != null) {
            localAdditionalInfo.put("payload", payload);
        }
        final String entityLocalUid = LOCAL_ENTITY + entityName;
        final Node localEntityNode = Node.builder().type("LocalEntity").name(entityLocalUid).uid(encodeSha1(entityLocalUid + ":" + payload)).properties(localAdditionalInfo).build();
        final LinkedHashMap<String, Serializable> additionalInfo = new LinkedHashMap<>();
        final JsonNode payloadLightNode = ReflectionService.renderType(clazz, null, null, true);
        final String payloadLight = payloadLightNode == null ? null : payloadLightNode.convertToJson();
        if (payloadLight != null) {
            additionalInfo.put("payload", payloadLight);
        }
        final Node entityNode = Node.builder().type("Entity").name(entityName).uid(encodeSha1(entityName + ":" + payload)).properties(additionalInfo).build();
        result.addNode(localEntityNode, entityNode, artifactNode);
        result.addRelationship(Relationship.builder().from(artifactNode.getUid()).to(localEntityNode.getUid()).type("HAS_LOCAL_ENTITY").build());
        result.addRelationship(Relationship.builder().from(artifactNode.getUid()).to(entityNode.getUid()).type("HAS_ENTITY").build());
        result.addRelationship(Relationship.builder().from(localEntityNode.getUid()).to(entityNode.getUid()).type("HAS_ENTITY_REFERENCE").build());
        return List.of(result);
    }

    private String encodeSha1(final String value) {
        return value == null ? null : new EncryptionUtils().encodeSha1(value);
    }
}