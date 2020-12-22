package io.inugami.maven.plugin.analysis.api.models;

import io.inugami.api.models.data.basic.JsonObject;
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
public class Gav implements JsonObject, Comparable<Gav> {


    // =========================================================================
    // ATTRIBUTES
    // =========================================================================
    private static final long   serialVersionUID = -1609343378801778104L;
    private final        String groupId;
    private final        String artifactId;
    private final        String version;

    @EqualsAndHashCode.Include
    private final String   hash;
    private final String   type;
    private final Set<Gav> dependencies;
    private       Gav      parent;

    // =========================================================================
    // CONSTRUCTORS
    // =========================================================================
    public Gav(final String groupId, final String artifactId, final String version, final String hash,
               final String type,
               final Set<Gav> dependencies, final Gav parent) {
        this.groupId      = groupId;
        this.artifactId   = artifactId;
        this.version      = version;
        this.type         = type;
        this.dependencies = dependencies == null ? new LinkedHashSet<>() : dependencies;
        this.hash         = String.join(":", List.of(String.valueOf(groupId),
                                                     String.valueOf(artifactId),
                                                     String.valueOf(version),
                                                     String.valueOf(type)));
        this.parent       = parent;
    }

    @Override
    public int compareTo(final Gav other) {
        return hash.compareTo(String.valueOf(other == null ? null : other.getHash()));
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
        if (values != null) {
            dependencies.addAll(values);
        }
    }


}
