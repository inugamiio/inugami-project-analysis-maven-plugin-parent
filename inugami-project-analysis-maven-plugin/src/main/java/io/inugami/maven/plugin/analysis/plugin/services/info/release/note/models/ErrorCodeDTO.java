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
package io.inugami.maven.plugin.analysis.plugin.services.info.release.note.models;

import io.inugami.api.models.data.basic.JsonObject;
import lombok.*;

import static io.inugami.maven.plugin.analysis.api.utils.Constants.UNDERSCORE;

@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
@Getter
public class ErrorCodeDTO implements Comparable<ErrorCodeDTO>, JsonObject {
    private static final long serialVersionUID = -5469719611823975510L;
    @EqualsAndHashCode.Include
    String errorCode;
    Integer statusCode;
    String  message;
    String  messageDetail;
    String  payload;
    String  type;

    Boolean exploitationError;
    Boolean rollback;
    Boolean retryable;

    String field;
    String url;
    String errorDomain;
    String errorSubDomain;
    String artifact;

    @Override
    public int compareTo(final ErrorCodeDTO other) {
        final String currentHash = String.join(UNDERSCORE, String.valueOf(artifact), String.valueOf(errorCode));
        final String otherHash = String.join(UNDERSCORE,
                                             String.valueOf(other == null ? null : other.getArtifact()),
                                             String.valueOf(other == null ? null : other.getErrorCode()));
        return currentHash.compareTo(otherHash);
    }
}
