Match (version:Version) where version.groupId= "com.smartwave.training" and version.artifactId="spring-boot-training-rest" and version.version="0.0.1-SNAPSHOT"
OPTIONAL MATCH (v)-[:PROJECT_DEPENDENCY*0..10]->(depExposer:Version)-[expose:EXPOSE]->(serviceExpose:Service)-[]->(exposeServiceType:ServiceType)
OPTIONAL MATCH (v)-[:PROJECT_DEPENDENCY*0..10]->(depConsumer:Version)-[consume:CONSUME]->(serviceConsume:Service)-[]->(serviceConsumeType:ServiceType)
OPTIONAL MATCH (v)-[:PROJECT_DEPENDENCY*0..10]->(depEntityProducer:Version)-[:HAS_LOCAL_ENTITY]->(localEntity:LocalEntity)-[]->(entity:Entity)
OPTIONAL MATCH (entity)-[:HAS_ENTITY*0..10]->(entityDep:Version)

OPTIONAL MATCH (depVersionConsumer)-[dependenciesConsume:CONSUME]->(serviceExpose)
OPTIONAL MATCH (depVersionProducer)-[dependenciesExpose:EXPOSE]->(serviceConsume)

return version,
       serviceExpose,
       serviceConsume,
       depExposer,
       depConsumer,
       depVersionConsumer,
       depVersionProducer,
       expose,
       consume,
       dependenciesConsume,
       dependenciesExpose,
       localEntity,
       entityDep,
       entity