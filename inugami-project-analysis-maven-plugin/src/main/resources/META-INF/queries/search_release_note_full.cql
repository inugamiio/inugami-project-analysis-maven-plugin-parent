Match (version:Version) where version.groupId= "{{groupId}}" and version.artifactId="{{artifactId}}" and version.version="{{version}}"
OPTIONAL MATCH (version)-[:HAVE_AUTHOR]->(author:Author)
OPTIONAL MATCH (version)-[:HAVE_SCM_INFO]->(scm:Scm)
OPTIONAL MATCH (version)-[:HAVE_TICKET]->(issue:Issue)-[:HAS_ISSUE_LINK*0..10]->(issueLink:Issue)
OPTIONAL MATCH (issue)-[]->(issueLabel:IssueLabel)
OPTIONAL MATCH (issueLink)-[]->(issueLinkLabel:IssueLabel)
OPTIONAL MATCH (version)-[:HAVE_TICKET]->(mergeRequest:MergeRequest)
return version,
       scm,
       issue,
       mergeRequest,
       author,
       issueLabel,
       issueLink,
       issueLinkLabel