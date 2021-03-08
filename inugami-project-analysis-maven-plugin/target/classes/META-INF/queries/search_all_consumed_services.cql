Match (version:Version) where version.groupId= "{{groupId}}" and version.artifactId="{{artifactId}}" and version.version="{{version}}"
OPTIONAL MATCH (version)-[:PROJECT_DEPENDENCY*0..10]->(consumer:Version)
OPTIONAL MATCH (consumer)-[:CONSUME]->(service:Service)-[]->(serviceType:ServiceType)
OPTIONAL MATCH (producer:Version)-[:EXPOSE]->(service)
OPTIONAL MATCH (service)-[:USE_BY]->(method:Method)
OPTIONAL MATCH (methodArtifact:Version)-[:HAS_METHOD]->(method)
return version,producer,service,serviceType,consumer,method,methodArtifact