package io.inugami.maven.plugin.analysis.api.models;

import io.inugami.api.models.data.basic.JsonObject;
import lombok.Builder;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Builder(toBuilder = true)
@Getter
public class ScanNeo4jResult implements JsonObject {
    private static final long serialVersionUID = -1885576008301619055L;
    private final String type;
    private final List<String>       nodesToDeletes;
    private final List<Node>         nodes;
    private final List<Relationship> relationships;

    public ScanNeo4jResult(final String type, final List<String> nodesToDeletes, final List<Node> nodes,
                           final List<Relationship> relationships) {
        this.type           = type;
        this.nodesToDeletes = nodesToDeletes == null ? new ArrayList<>() : nodesToDeletes;
        this.nodes          = nodes == null ? new ArrayList<>() : nodes;
        this.relationships  = relationships == null ? new ArrayList<>() : relationships;
    }

    public ScanNeo4jResult addNode(final List<Node> values) {
        if (values != null) {
            addNode(values.toArray(new Node[]{}));
        }
        return this;
    }

    public ScanNeo4jResult addNode(final Node... values) {
        if (values.length > 0) {
            for (int i = 0; i < values.length; i++) {
                if (values[i] != null) {
                    nodes.add(values[i]);
                }
            }

        }
        return this;
    }

    public ScanNeo4jResult addRelationship(final List<Relationship> values) {
        if (values != null) {
            addRelationship(values.toArray(new Relationship[]{}));
        }
        return this;
    }

    public ScanNeo4jResult addRelationship(final Relationship... values) {
        if (values.length > 0) {
            for (int i = 0; i < values.length; i++) {
                if (values[i] != null) {
                    relationships.add(values[i]);
                }
            }

        }
        return this;
    }

    public ScanNeo4jResult addNodeToDelete(final List<String> uids) {
        if (uids != null) {
            addNodeToDelete(uids.toArray(new String[]{}));
        }
        return this;
    }

    public ScanNeo4jResult addNodeToDelete(final String... uids) {
        if (uids.length > 0) {
            for (int i = 0; i < uids.length; i++) {
                if (uids[i] != null) {
                    nodesToDeletes.add(uids[i]);
                }
            }

        }
        return this;
    }

}
