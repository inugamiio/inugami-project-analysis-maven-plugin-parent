Match (version:Version) where version.groupId= "{{groupId}}" and version.artifactId="{{artifactId}}" and version.version="{{version}}"
OPTIONAL MATCH (version)-[:PROJECT_DEPENDENCY*0..10]->(dep:Version)
OPTIONAL MATCH (dep)-[:CONSUME]->(service:Service)-[:SERVICE_TYPE]->(serviceType:ServiceType) where NOT ()-[:EXPOSE]->(service)
return service,
       serviceType