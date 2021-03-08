Match (version:Version) where version.groupId= "{{groupId}}" and version.artifactId="{{artifactId}}" and version.version="{{version}}"
OPTIONAL MATCH (version)-[:PROJECT_DEPENDENCY*0..10]->(depConsumer:Version)-[consume:CONSUME]->(serviceConsume:Service)-[]->(serviceConsumeType:ServiceType) where serviceConsumeType.name="Rest"
OPTIONAL MATCH (depVersionProducer:Version)-[dependenciesExpose:EXPOSE]->(serviceConsume)
return version,
       depConsumer,
       serviceConsume,
       depVersionProducer,
       dependenciesExpose