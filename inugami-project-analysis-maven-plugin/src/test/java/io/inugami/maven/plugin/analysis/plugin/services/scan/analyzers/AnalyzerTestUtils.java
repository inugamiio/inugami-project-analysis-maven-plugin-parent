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
package io.inugami.maven.plugin.analysis.plugin.services.scan.analyzers;

import io.inugami.api.models.data.basic.JsonObject;
import io.inugami.maven.plugin.analysis.api.models.ScanNeo4jResult;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AnalyzerTestUtils {

    public static ScanNeo4jResult extractResult(final List<JsonObject> data) {
        assertThat(data).size().isOne();
        assertThat(data.get(0)).isInstanceOf(ScanNeo4jResult.class);
        final ScanNeo4jResult neo4jResult = (ScanNeo4jResult) data.get(0);
        neo4jResult.sort();
        return neo4jResult;
    }
}
