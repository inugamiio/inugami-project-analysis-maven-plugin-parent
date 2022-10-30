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
package io.inugami.maven.plugin.analysis.api.utils.reflection;

import lombok.*;

@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString
@Getter
@Builder(toBuilder = true)
@AllArgsConstructor
public class PotentialErrorDTO implements Comparable<PotentialErrorDTO> {
    @EqualsAndHashCode.Include
    private final String                     errorCode;
    private final String                     type;
    private final Class<?>                   errorCodeClass;
    private final Class<? extends Exception> throwsAs;
    private final int                        httpStatus;
    private final String                     errorMessage;
    private final String                     errorMessageDetail;
    private final String                     payload;
    private final String                     description;
    private final String                     example;
    private final String                     url;

    @Override
    public int compareTo(final PotentialErrorDTO other) {
        return String.valueOf(errorCode).compareTo(String.valueOf(other.getErrorCode()));
    }
}
