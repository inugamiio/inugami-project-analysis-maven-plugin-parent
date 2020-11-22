package io.inugami.maven.plugin.analysis.api.models;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Setter
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder(toBuilder = true)
public class Gav {

    // =========================================================================
    // ATTRIBUTES
    // =========================================================================
    private final String groupId;
    private final String artifactId;
    private final String version;

    @EqualsAndHashCode.Include
    private final String   hash;
    private final String   type;
    private final Set<Gav> dependencies;
    private       Gav      parent;

    // =========================================================================
    // CONSTRUCTORS
    // =========================================================================
    public Gav(final String groupId, final String artifactId, final String version, final String hash, final String type,
               final Set<Gav> dependencies, final Gav parent) {
        this.groupId      = groupId;
        this.artifactId   = artifactId;
        this.version      = version;
        this.type         = type;
        this.dependencies = dependencies == null ? new LinkedHashSet<>() : dependencies;
        this.hash         = String.join(":", List.of(groupId, artifactId, version, type));
        this.parent       = parent;
    }

    public Gav addDependency(final Gav gav) {
        if (gav != null) {
            dependencies.add(gav);
        }

        return this;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Gav{");
        sb.append(hash).append('\'');
        sb.append('}');
        return sb.toString();
    }

    public void addDependencies(final List<Gav> values) {
        if(values!=null){
            dependencies.addAll(values);
        }
    }
}
