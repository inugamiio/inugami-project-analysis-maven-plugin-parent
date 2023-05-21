package io.inugami.maven.plugin.analysis.api.models;

import io.inugami.commons.test.dto.AssertDtoContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.inugami.commons.test.UnitTestHelper.assertDto;

class ScanNeo4jResultTest {

    @Test
    void scanNeo4jResult() {
        assertDto(new AssertDtoContext<ScanNeo4jResult>()
                          .toBuilder()
                          .objectClass(ScanNeo4jResult.class)
                          .fullArgConstructorRefPath("api/models/scanNeo4jResultTest/fullArgConstructorRefPath.json")
                          .getterRefPath("api/models/scanNeo4jResultTest/getterRefPath.json")
                          .toStringRefPath("api/models/scanNeo4jResultTest/toStringRefPath.txt")
                          .cloneFunction(instance -> instance.toBuilder().build())
                          .fullArgConstructor(ScanNeo4jResultTest::buildDataSet)
                          .noArgConstructor(() -> new ScanNeo4jResult())
                          .checkEquals(false)
                          .checkSetters(true)
                          .build());
    }


    public static ScanNeo4jResult buildDataSet() {
        return ScanNeo4jResult.builder()
                              .type("neo4j")
                              .nodesToDeletes(List.of("nodeD"))
                              .nodes(List.of(NodeTest.buildDataSet()))
                              .createScripts(List.of("create script"))
                              .relationships(List.of(RelationshipTest.buildDataSet()))
                              .relationshipsToDeletes(List.of(RelationshipTest.buildDataSet().toBuilder().type("to_delete").build()))
                              .deleteScripts(List.of("deleteScripts"))
                              .build()
                              .sort()
                              .addNode(NodeTest.buildDataSet().toBuilder().type("addNode").build())
                              .addRelationship(RelationshipTest.buildDataSet().toBuilder().type("other_relationship").build())
                              .addCreateScript("other create script")
                              .addDeleteScript("other delete script");
    }
}