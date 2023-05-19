package io.inugami.maven.plugin.analysis.api.utils.reflection;


import lombok.*;

import java.io.Serializable;

@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString
@Getter
@Builder(toBuilder = true)
@AllArgsConstructor
public class PotentialErrorDTO implements Comparable<PotentialErrorDTO>, Serializable {
    private static final    long                       serialVersionUID = -8956873841090054913L;
    @EqualsAndHashCode.Include
    private final           String                     errorCode;
    private final           String                     type;
    private transient final Class<?>                   errorCodeClass;
    private transient final Class<? extends Exception> throwsAs;
    private final           int                        httpStatus;
    private final           String                     errorMessage;
    private final           String                     errorMessageDetail;
    private final           String                     payload;
    private final           String                     description;
    private final           String                     example;
    private final           String                     url;

    @Override
    public int compareTo(final PotentialErrorDTO other) {
        return String.valueOf(errorCode).compareTo(String.valueOf(other.getErrorCode()));
    }
}