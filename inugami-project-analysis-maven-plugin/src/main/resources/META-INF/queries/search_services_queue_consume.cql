Match (version:Version) where version.groupId= "{{groupId}}" and version.artifactId="{{artifactId}}" and version.version="{{version}}"
OPTIONAL MATCH (v)-[:PROJECT_DEPENDENCY*0..10]->(dep:Version)-[:CONSUME]->(service:Service)-[]->(serviceType:ServiceType) where  serviceType.name="JMS" OR serviceType.name="rabbitMq"
OPTIONAL MATCH (depExposer:Version)-[:EXPOSE]->(service)

OPTIONAL MATCH (service)-[:USE_BY]->(method:Method) WHERE (method)-[:HAS_METHOD]->(dep)

return version,
       service,
       serviceType,
       dep,
       depExposer,
       method
