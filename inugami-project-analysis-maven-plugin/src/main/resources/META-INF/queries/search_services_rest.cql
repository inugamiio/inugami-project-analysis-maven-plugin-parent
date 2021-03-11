Match (version:Version) where version.groupId= "{{groupId}}" and version.artifactId="{{artifactId}}" and version.version="{{version}}"
OPTIONAL MATCH (version)-[:PROJECT_DEPENDENCY*0..10]->(depExposer:Version)-[expose:EXPOSE]->(serviceExpose:Service)-[]->(exposeServiceType:ServiceType) where  exposeServiceType.name="{{serviceType}}"
OPTIONAL MATCH (version)-[:PROJECT_DEPENDENCY*0..10]->(depConsumer:Version)-[consume:CONSUME]->(serviceConsume:Service)-[]->(serviceConsumeType:ServiceType) where serviceConsumeType.name="{{serviceType}}"

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
       dependenciesExpose