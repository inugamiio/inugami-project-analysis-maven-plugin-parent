export interface Dependency{
    artifactId?:string,
    groupId?:string,
    hash?:string,
    type?:string,
    version?:string,
    dependencies?:Dependency[]
}

export interface Version{
    major?:number,
    minor?:number,
    patch?:number,
    qualifier?:string
}

export interface DependencyInfo{
    comment?:string,
    link?:string,
    level?:string
}


export interface Rule{
    version: number,
    ruleType: string    
}

export interface VersionRules{
    major?: Rule,
    minor?: Rule,
    patch?: Rule
 }

export interface DependencyRule{
    groupId?: string,
    artifactId?: string,
    rules?: VersionRules,
    comment?:string,
    link?:string,
    level?:string
 }

export interface DependenciesCheck{
    deprecated?: DependencyRule[],
    securityIssue?: DependencyRule[]
 }