CREATE INDEX ON :Artifact(artifactId, groupId, version, shortName, packaging);

CREATE INDEX ON :Author(email, name, shortName);

CREATE INDEX ON :Entity(name, shortName);

CREATE INDEX ON :LocalEntity(name, shortName, identity);

CREATE INDEX ON :ErrorCode(name, shortName, errorType, errorCode, mesage, statusCode);

CREATE INDEX ON :INPUT_DTO(name, shortName);

CREATE INDEX ON :OUTPUT_DTO(name, shortName);

CREATE INDEX ON :Issue(name, shortName, project_id, description, id, title, url, created_at, labels);

CREATE INDEX ON :IssueLabel(name, shortName, identity);

CREATE INDEX ON :MergeRequest(
  identity, start_sha, base_sha, merge_at, name, description, created_at, title, shortName, url, head_sha);

CREATE INDEX ON :Method(method, name, class, parameters, returnType);

CREATE INDEX ON :Property(name, shortName, mandatory, defautValue, propertyType);

CREATE INDEX ON :Scm(name, commit, shortName);

CREATE INDEX ON :Service(
  name, containerFactory, description, descriptionDetail, descriptionUrl, descriptionExample, contentType, accept,
  header, selector, subscription, shortName, queuesToDeclare, converterWinsContentType, bindings, autoStartup,
  listenerId, errorHandler, uri, deprecated, verbe);

CREATE INDEX ON :Version(
  name, major, minor, patch, groupId, artifactId, tag, scm, shortName, packaging, version, url, projectType);