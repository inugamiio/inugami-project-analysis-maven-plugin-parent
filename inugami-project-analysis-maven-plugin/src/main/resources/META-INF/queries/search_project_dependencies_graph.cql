MATCH (version:Version)
  WHERE version.groupId = '{{groupId}}' AND version.artifactId = '{{artifactId}}' AND version.version = '{{version}}'
OPTIONAL MATCH (version)-[dependencyLink:PROJECT_DEPENDENCY*0..10]->(dependency:Version)
OPTIONAL MATCH (dependency)-[:CONSUME]->(serviceConsumed:Service)<-[:EXPOSE]->(depProducer:Version)
OPTIONAL MATCH (serviceConsumed)-[]->(serviceConsumedType:ServiceType)
OPTIONAL MATCH (dependency)-[:EXPOSE]->(serviceProduced:Service)<-[:CONSUME]->(depConsumer:Version)
OPTIONAL MATCH (serviceProduced)-[]->(serviceProducedType:ServiceType)
RETURN dependency, dependencyLink, serviceConsumed, depProducer, serviceProduced, depConsumer, serviceConsumedType,
       serviceProducedType