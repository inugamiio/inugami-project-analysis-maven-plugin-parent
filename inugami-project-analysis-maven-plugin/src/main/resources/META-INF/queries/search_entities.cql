Match (version:Version) where version.groupId= "{{groupId}}" and version.artifactId="{{artifactId}}" and version.version="{{version}}"
OPTIONAL MATCH (version)-[:PROJECT_DEPENDENCY*0..10]->(artifact:Version)-[:HAS_LOCAL_ENTITY]->(localEntity:LocalEntity)
OPTIONAL MATCH (localEntity)-[:HAS_ENTITY_REFERENCE]->(entity:Entity)<-[:HAS_ENTITY*0..10]-(versionDeps:Version)
return artifact,
       localEntity,
       entity,
       versionDeps