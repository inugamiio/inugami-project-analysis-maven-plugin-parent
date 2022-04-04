import { Injectable, Inject } from '@angular/core';
import { HttpService } from '../../inugami-api/services/http/http.service';
import {
    DependenciesCheck,
    Dependency,
    DependencyRule,
    Version,
    VersionRules,
    DependencyInfo,
    Rule
} from '../models/dependencies.check';
import { isNotNull } from '../../inugami-api/services/utils';
import {CONFIG} from '../../../env'

@Injectable({
    providedIn: 'root'
})
export class DependenciesCheckService {

    /**************************************************************************
    * ATTRIBUTES
    **************************************************************************/
    private dependenciesChecks: DependenciesCheck;
    private types : string[] = [];

    /**************************************************************************
    * CONSTRUCTOR
    **************************************************************************/
    constructor(@Inject(HttpService) private httpService: HttpService) {
        this.httpService.get(`${CONFIG.CONTEXT_PATH}/data/dependencies-check.json`)
            .then(response => {
                this.dependenciesChecks = response.body;

                if (isNotNull(this.dependenciesChecks)) {
                    this.types = Object.keys(this.dependenciesChecks);
                }
            })
    }

    /**************************************************************************
    * API
    **************************************************************************/
    public getDependencyInfo(dependency: Dependency, type: string): DependencyInfo {
        let result = null;
        if (isNotNull(this.dependenciesChecks)) {
            let dependencyRule = this.resolveDependencyRule(dependency, this.dependenciesChecks[type]);
            if (isNotNull(dependencyRule)) {
                result = {
                    comment: dependencyRule.comment,
                    link: dependencyRule.link,
                    level: dependencyRule.level
                };
            }
        }
        return result;
    }

    public getType(): string[] {
        return this.types;
    }
    /**************************************************************************
    * PRIVATE
    **************************************************************************/
    private resolveDependencyRule(dependency: Dependency, rules: DependencyRule[]): DependencyRule {
        let result = null;
        if (isNotNull(rules)) {
            for (let rule of rules) {
                if (this.matchArtifact(dependency, rule)) {
                    result = this.matchVersion(dependency, rule) ? rule : null;
                    if (isNotNull(result)) {
                        break;
                    }
                }
            }
        }

        return result;
    }


    private matchArtifact(dependency: Dependency, rule: DependencyRule): boolean {
        let result = dependency.groupId == rule.groupId;

        if (rule && isNotNull(rule.artifactId)) {
            result = dependency.artifactId == rule.artifactId;
        }

        return result;
    }

    private matchVersion(dependency: Dependency, rule: DependencyRule): boolean {
        let result = false;
        if (isNotNull(rule.rules)) {
            let version = this.extractVersion(dependency.version);

            let strategies = [];

            if (isNotNull(rule.rules.major)) {
                strategies.push(this.buildMatchingStrategy(version.major, rule.rules.major));
            }
            if (isNotNull(rule.rules.minor)) {
                strategies.push(this.buildMatchingStrategy(version.minor, rule.rules.minor));
            }
            if (isNotNull(rule.rules.patch)) {
                strategies.push(this.buildMatchingStrategy(version.patch, rule.rules.patch));
            }

            result = true;
            for (let strategy of strategies) {
                result = strategy();
                if (!result) {
                    break;
                }
            }
        }


        return result;
    }

    private buildMatchingStrategy(value: number, rule: Rule): Function {
        let result = null;
        if (isNotNull(value)) {
            if (rule.ruleType == '=') {
                result = () => value == rule.version;
            } else if (rule.ruleType == '<') {
                result = () => value < rule.version;
            } else if (rule.ruleType == '<=') {
                result = () => value <= rule.version;
            } else if (rule.ruleType == '>') {
                result = () => value > rule.version;
            } else if (rule.ruleType == '>=') {
                result = () => value > rule.version;
            }
        } else {
            result = () => false;
        }

        return result;
    }

    private extractVersion(version: string): Version {
        let major = null;
        let minor = null;
        let patch = null;
        let qualifier = null;

        let qualiersParts = version.split('-');

        let parts = qualiersParts[0].split('.');
        let size = parts.length;

        major = parts[0];
        if (size > 1) {
            minor = parts[1];
        }
        if (size > 2) {
            patch = parts[2];
        }

        if (qualiersParts.length > 1) {
            qualifier = qualiersParts[1];
        }
        let result = {
            "major": major,
            "minor": minor,
            "patch": patch,
            "qualifier": qualifier
        }
        return result;
    }
}