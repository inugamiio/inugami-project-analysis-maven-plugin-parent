MATCH (env:Env)
OPTIONAL MATCH (version:Version)-[deploy:DEPLOY]->(env)
RETURN env,
       version,
       deploy