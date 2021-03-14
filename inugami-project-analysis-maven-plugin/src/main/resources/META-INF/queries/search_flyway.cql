Match (version:Version) where version.groupId= "{{groupId}}" and version.artifactId="{{artifactId}}" and version.version="{{version}}"
MATCH (version)-[:PROJECT_DEPENDENCY*0..10]->(artifact:Version)
MATCH (artifact)-[:HAS_FLYWAY]->(flyway:Flyway)-[:HAS_FLYWAY_CONTENT]->(flywayContent:FlywayContent)
OPTIONAL MATCH (dependency)-[:HAS_FLYWAY]->(:Flyway)-[:HAS_FLYWAY_CONTENT]->(flywayContent)

return artifact,
       flyway,
       flywayContent,
       dependency