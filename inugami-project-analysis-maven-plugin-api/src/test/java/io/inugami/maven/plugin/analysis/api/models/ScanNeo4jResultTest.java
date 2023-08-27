package io.inugami.maven.plugin.analysis.api.models;

import io.inugami.commons.test.dto.AssertDtoContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.inugami.commons.test.UnitTestHelper.assertDto;
import static io.inugami.commons.test.UnitTestHelper.assertTextRelative;

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
                          .noArgConstructor(ScanNeo4jResult::new)
                          .checkEquals(false)
                          .checkSetters(true)
                          .build());
    }


    @Test
    void addNode_nominal() {
        final ScanNeo4jResult result    = buildDataSet();
        List<Node>            nullValue = null;
        result.addNode(nullValue);
        assertTextRelative(result, "api/models/scanNeo4jResultTest/addNode_nominal.1.json");

        result.addNode(List.of(Node.builder().name("simpleNode").build()));
        assertTextRelative(result, "api/models/scanNeo4jResultTest/addNode_nominal.2.json");
    }

    @Test
    void addCreateScript_nominal() {
        final ScanNeo4jResult result    = buildDataSet();
        List<String>          nullValue = null;
        result.addCreateScript(nullValue);
        assertTextRelative(result, "api/models/scanNeo4jResultTest/addCreateScript_nominal.1.json");
        result.addCreateScript(List.of("requests"));
        assertTextRelative(result, "api/models/scanNeo4jResultTest/addCreateScript_nominal.2.json");
    }


    @Test
    void addNodeToDelete_nominal() {
        final ScanNeo4jResult result    = buildDataSet();
        List<String>          nullValue = null;
        result.addNodeToDelete(nullValue);
        assertTextRelative(result, "api/models/scanNeo4jResultTest/addNodeToDelete_nominal.1.json");
        result.addNodeToDelete(List.of("requests"));
        assertTextRelative(result, "api/models/scanNeo4jResultTest/addNodeToDelete_nominal.2.json");
        result.addNodeToDelete("uid");
        assertTextRelative(result, "api/models/scanNeo4jResultTest/addNodeToDelete_nominal.3.json");
    }

    @Test
    void addRelationship_nominal() {
        final ScanNeo4jResult result    = buildDataSet();
        List<Relationship>    nullValue = null;
        result.addRelationship(nullValue);
        assertTextRelative(result, "api/models/scanNeo4jResultTest/addRelationship.1.json");
        result.addRelationship(List.of(Relationship.builder().from("1").to("2").type("has").build()));
        assertTextRelative(result, "api/models/scanNeo4jResultTest/addRelationship.2.json");
    }

    @Test
    void addRelationshipToDelete_nominal() {
        final ScanNeo4jResult result    = buildDataSet();
        List<Relationship>    nullValue = null;
        result.addRelationshipToDelete(nullValue);
        assertTextRelative(result, "api/models/scanNeo4jResultTest/addRelationshipToDelete.1.json");
        result.addRelationshipToDelete(List.of(Relationship.builder().from("1").to("2").type("has").build()));
        assertTextRelative(result, "api/models/scanNeo4jResultTest/addRelationshipToDelete.2.json");
        result.addRelationshipToDelete(Relationship.builder().from("3").to("4").type("has").build());
        assertTextRelative(result, "api/models/scanNeo4jResultTest/addRelationshipToDelete.3.json");
    }


    @Test
    void merge_nominal() {
        final ScanNeo4jResult result = buildDataSet();
        ScanNeo4jResult.merge(result, null);
        assertTextRelative(result, "api/models/scanNeo4jResultTest/merge_nominal.1.json");
        ScanNeo4jResult.merge(null, result);
        assertTextRelative(result, "api/models/scanNeo4jResultTest/merge_nominal.1.json");
        ScanNeo4jResult.merge(result, ScanNeo4jResult.builder().build());
        assertTextRelative(result, "api/models/scanNeo4jResultTest/merge_nominal.1.json");
        ScanNeo4jResult.merge(result, result);
        assertTextRelative(result, "api/models/scanNeo4jResultTest/merge_nominal.2.json");
    }

    public static ScanNeo4jResult buildDataSet() {
        return ScanNeo4jResult.builder()
                              .type("neo4j")
                              .nodesToDeletes(List.of("nodeD"))
                              .nodes(List.of(NodeTest.buildDataSet()))
                              .createScripts(List.of("create script"))
                              .relationships(List.of(RelationshipTest.buildDataSet()))
                              .relationshipsToDeletes(List.of(RelationshipTest.buildDataSet()
                                                                              .toBuilder()
                                                                              .type("to_delete")
                                                                              .build()))
                              .deleteScripts(List.of("deleteScripts"))
                              .build()
                              .sort()
                              .addNode(NodeTest.buildDataSet().toBuilder().type("addNode").build())
                              .addRelationship(RelationshipTest.buildDataSet()
                                                               .toBuilder()
                                                               .type("other_relationship")
                                                               .build())
                              .addCreateScript("other create script")
                              .addDeleteScript("other delete script");
    }
}