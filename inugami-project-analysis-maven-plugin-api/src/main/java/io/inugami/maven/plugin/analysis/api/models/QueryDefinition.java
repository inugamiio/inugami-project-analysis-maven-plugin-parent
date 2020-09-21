package io.inugami.maven.plugin.analysis.api.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.List;

@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@AllArgsConstructor
public class QueryDefinition {

    private final String       name;
    private final String       type;
    @EqualsAndHashCode.Include
    private final String       path;
    private final String       description;
    private final List<String> parameters;
}
