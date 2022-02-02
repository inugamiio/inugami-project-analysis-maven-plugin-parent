
export interface Artifact{
   groupId?        : string,
   artifactId?     : string,
   type?           : string,
   version?        : string,
   hash?           : string,
   currentProject? : string
}

export interface ArtifactGraphDependency{
   hash?: string,
   consume?: string[]
}
export interface ArtifactGraph{
   hash?: string,
   dependencies?: ArtifactGraphDependency[]
}

export interface ProjectDependenciesGraph{
   artifacts : Artifact[],
   graph : ArtifactGraph[]
}

export interface Gav{
   groupId : string,
   artifactId : string,
   version : string,
   scanDate : string,
}
export interface Author{
   name: string,
   email?:string
}
export interface Issue{
    date?: string,
    labels?:string[],
    name: string,
    title?: string,
    url: string
 }

 export interface MergeRequest{
    date?: string,
    uid: string,
    title: string,
    url: string
 }

export interface ReleaseNote{
    gav: Gav,
    authors?: Author[],
    commit? : string[],
    differentials?: any,
    issues? : Issue[],
    mergeRequests? : MergeRequest[],
    projectDependenciesGraph?ProjectDependenciesGraph
}