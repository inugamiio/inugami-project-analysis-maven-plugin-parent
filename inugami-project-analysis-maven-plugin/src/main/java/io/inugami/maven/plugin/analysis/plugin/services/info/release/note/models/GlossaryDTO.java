package io.inugami.maven.plugin.analysis.plugin.services.info.release.note.models;

import io.inugami.api.models.data.basic.JsonObject;
import lombok.*;

@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
@Getter
public class GlossaryDTO implements JsonObject {
    private String value;
    private String label;
    private String description;
    private String type;
}
