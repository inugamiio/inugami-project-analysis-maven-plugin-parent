package io.inugami.maven.plugin.analysis.api.models.rest;


import io.inugami.api.tools.StringComparator;
import lombok.*;

import java.io.Serializable;

@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@Getter
@Setter
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class PotentialErrorDTO implements Comparable<PotentialErrorDTO>, Serializable {
    private static final long                       serialVersionUID = -8956873841090054913L;
    @EqualsAndHashCode.Include
    @ToString.Include
    private              String                     errorCode;
    private              String                     type;
    private transient    Class<?>                   errorCodeClass;
    private transient    Class<? extends Exception> throwsAs;
    private              int                        httpStatus;
    private              String                     errorMessage;
    private              String                     errorMessageDetail;
    private              String                     payload;
    private              String                     description;
    private              String                     example;
    private              String                     url;

    @Override
    public int compareTo(final PotentialErrorDTO other) {
        return StringComparator.compareTo(errorCode, other == null ? null : other.getErrorCode());
    }
}