Match (version:Version) where version.groupId= "{{groupId}}" and version.artifactId="{{artifactId}}" and version.version="{{version}}"
OPTIONAL MATCH (version)-[deploy:DEPLOY]->(env:Env)
OPTIONAL MATCH (version)-[:PROJECT_DEPENDENCY*0..10]->(dep:Version)
OPTIONAL MATCH (dep)-[:CONSUME]->(service:Service)
OPTIONAL MATCH (dep)-[deployDep:DEPLOY]->(envDep:Env)
OPTIONAL MATCH (depProducer:Version)-[:EXPOSE]->(service)
OPTIONAL MATCH (depProducer)-[deployProducer:DEPLOY]->(envProducer:Env)
return version,
       deploy,
       env,
       dep,
       deployDep,
       envDep,
       depProducer,
       deployProducer,
       envProducer