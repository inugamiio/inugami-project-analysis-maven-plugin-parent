match (version:Version) where version.groupId= "{{groupId}}" and version.artifactId="{{artifactId}}" and version.version="{{version}}"
optional match (version)-[deploy:DEPLOY]->(env:Env)
optional match (env:Env)-[haveArtifactVersion:HAVE_ARTIFACT_VERSION]->(version)
return version,
       env,
       deploy,
       haveArtifactVersion