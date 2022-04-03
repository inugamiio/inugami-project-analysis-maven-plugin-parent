/* --------------------------------------------------------------------
 *  Inugami
 * --------------------------------------------------------------------
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package io.inugami.maven.plugin.analysis.front.core.services;

import io.inugami.maven.plugin.analysis.front.api.models.*;
import io.inugami.maven.plugin.analysis.front.api.services.DependenciesCheckService;

import java.util.List;

public class DefaultDependenciesCheckService implements DependenciesCheckService {

    @Override
    public DependenciesCheck getDependenciesCheckData() {
        return DependenciesCheck.builder()
                                .deprecated(buildDeprecated())
                                .securityIssue(buildSecurityIssue())
                                .ban(buildBan())
                                .build();
    }

    private List<DependencyRule> buildDeprecated() {
        return List.of(
                DependencyRule.builder()
                              .groupId("io.inugami.maven.plugin.analysis")
                              .comment("Please update to inugami maven plugin version 1.5.2 or higher")
                              .link("https://search.maven.org/artifact/io.inugami.maven.plugin.analysis/inugami-project-analysis-maven-plugin-parent/1.5.2/pom")
                              .rules(VersionRules.builder()
                                                 .major(Rule.builder()
                                                            .version(1)
                                                            .ruleType(RuleType.EQUALS)
                                                            .build())
                                                 .minor(Rule.builder()
                                                            .version(5)
                                                            .ruleType(RuleType.LESS)
                                                            .build())
                                                 .build())
                              .build(),

                DependencyRule.builder()
                              .groupId("org.apache.logging.log4j")
                              .link("https://search.maven.org/search?q=g:org.apache.logging.log4j")
                              .rules(VersionRules.builder()
                                                 .major(Rule.builder()
                                                            .version(2)
                                                            .ruleType(RuleType.EQUALS)
                                                            .build())
                                                 .minor(Rule.builder()
                                                            .version(17)
                                                            .ruleType(RuleType.LESS)
                                                            .build())
                                                 .build())
                              .build()
                      );
    }

    private List<DependencyRule> buildBan() {
        return List.of(
                DependencyRule.builder()
                              .groupId("org.apache.logging.log4j")
                              .link("https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2021-4483")
                              .comment("CVE-2021-44832 : Log4j2 contains major security issue")
                              .level(Level.critical)
                              .rules(VersionRules.builder()
                                                 .major(Rule.builder()
                                                            .version(2)
                                                            .ruleType(RuleType.EQUALS)
                                                            .build())
                                                 .minor(Rule.builder()
                                                            .version(17)
                                                            .ruleType(RuleType.LESS)
                                                            .build())
                                                 .build())
                              .build()
                      );
    }

    private List<DependencyRule> buildSecurityIssue() {
        return List.of(
                DependencyRule.builder()
                              .groupId("org.apache.logging.log4j")
                              .link("https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2021-4483")
                              .comment("banished because of CVE-2021-44832")
                              .level(Level.critical)
                              .rules(VersionRules.builder()
                                                 .major(Rule.builder()
                                                            .version(2)
                                                            .ruleType(RuleType.EQUALS)
                                                            .build())
                                                 .minor(Rule.builder()
                                                            .version(17)
                                                            .ruleType(RuleType.LESS)
                                                            .build())
                                                 .build())
                              .build()
                      );
    }


}
