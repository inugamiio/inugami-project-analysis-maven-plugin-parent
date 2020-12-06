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
package io.inugami.maven.plugin.analysis.plugin.services.scan.git;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.time.LocalDateTime;

@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Getter
@Builder
public class GitLog {

    // =========================================================================
    // ATTRIBUTES
    // =========================================================================
    private final int           type;
    private final String        message;
    @EqualsAndHashCode.Include
    private final String        name;
    private final String        author;
    private final String        authorEmail;
    private final LocalDateTime date;

    // =========================================================================
    // OVERRIDES
    // =========================================================================
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append('[').append(date).append(']');
        sb.append('[').append(name).append(']');
        sb.append("(<").append(author).append('>').append(authorEmail).append(')');
        sb.append(message);
        return sb.toString();
    }
}
