Match (version:Version) where version.groupId= "{{groupId}}" and version.artifactId="{{artifactId}}" and version.version="{{version}}"
OPTIONAL MATCH (version)-[:PROJECT_DEPENDENCY*0..10]->(depProducer:Version)-[expose:EXPOSE]->(service:Service)-[]->(serviceConsumeType:ServiceType) where  serviceConsumeType.name="Rest"
OPTIONAL MATCH (depConsumer:Version)-[dependenciesConsume:CONSUME]->(service)
return service,
       depProducer,
       depConsumer