package io.inugami.maven.plugin.analysis.api.models;

import lombok.*;

import java.util.List;

@Getter
@Setter
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public final class QueryDefinition {
    @ToString.Include
    private String       name;
    @ToString.Include
    @EqualsAndHashCode.Include
    private String       type;
    @ToString.Include
    @EqualsAndHashCode.Include
    private String       path;
    private String       description;
    private List<String> parameters;
}
