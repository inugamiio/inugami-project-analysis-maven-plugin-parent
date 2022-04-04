MATCH (version:Version)
  WHERE version.groupId = '{{groupId}}' AND version.artifactId = '{{artifactId}}' AND version.version = '{{version}}'
OPTIONAL MATCH(version)-[:PROJECT_DEPENDENCY*0..10]->(depExposer:Version)-[expose:EXPOSE]->(serviceExpose:Service)
                -[]->(exposeServiceType:ServiceType)
OPTIONAL MATCH(version)-[:PROJECT_DEPENDENCY*0..10]->(depConsumer:Version)-[consume:CONSUME]->(serviceConsume:Service)
                -[]->(serviceConsumeType:ServiceType)
OPTIONAL MATCH
  (version)-[:PROJECT_DEPENDENCY*0..10]->(depEntityProducer:Version)-[:HAS_LOCAL_ENTITY]->(localEntity:LocalEntity)
    -[]->(entity:Entity)
OPTIONAL MATCH (entity)-[:HAS_ENTITY*0..10]->(entityDep:Version)

OPTIONAL MATCH (depVersionConsumer)-[dependenciesConsume:CONSUME]->(serviceExpose)
OPTIONAL MATCH (depVersionProducer)-[dependenciesExpose:EXPOSE]->(serviceConsume)

RETURN version,
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