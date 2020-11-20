Match (version:Version) where version.groupId= "{{groupId}}" and version.artifactId="{{artifactId}}" and version.version="{{version}}"
OPTIONAL MATCH (version)-[:PROJECT_DEPENDENCY*0..10]->(dep:Version)-[:EXPOSE]->(service:Service)-[]->(serviceType:ServiceType) where  serviceType.name="JMS" OR serviceType.name="rabbitMq"
OPTIONAL MATCH (depConsumer:Version)-[:CONSUME]->(service)

OPTIONAL MATCH (service)-[:USE_BY]->(method:Method) WHERE (method)-[:HAS_METHOD]->(dep)

return version,
       service,
       serviceType,
       dep,
       depConsumer,
       method
