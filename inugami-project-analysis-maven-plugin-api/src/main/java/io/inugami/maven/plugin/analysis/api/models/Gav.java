package io.inugami.maven.plugin.analysis.api.models;

import io.inugami.api.models.data.basic.JsonObject;
import lombok.*;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@SuppressWarnings({"java:S107"})
@Setter
@Getter
@NoArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Gav implements JsonObject, Comparable<Gav> {


    // =========================================================================
    // ATTRIBUTES
    // =========================================================================
    private static final long   serialVersionUID = -1609343378801778104L;
    private              String groupId;
    private              String artifactId;
    private              String version;

    @ToString.Include
    @EqualsAndHashCode.Include
    private String   hash;
    private String   type;
    private String   scope;
    private Set<Gav> dependencies;
    private Gav      parent;


    // =========================================================================
    // CONSTRUCTORS
    // =========================================================================
    @Builder(toBuilder = true)
    public Gav(final String groupId, final String artifactId, final String version, final String hash,
               final String type,
               final Set<Gav> dependencies,
               final Gav parent,
               final String scope) {

        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.type = type;
        this.scope = scope;
        this.dependencies = dependencies == null ? new LinkedHashSet<>() : dependencies;
        this.hash = String.join(":", List.of(String.valueOf(groupId),
                                             String.valueOf(artifactId),
                                             String.valueOf(version),
                                             String.valueOf(type)));
        this.parent = parent;
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


    public void addDependencies(final List<Gav> values) {
        if (values != null) {
            dependencies.addAll(values);
        }
    }


}
