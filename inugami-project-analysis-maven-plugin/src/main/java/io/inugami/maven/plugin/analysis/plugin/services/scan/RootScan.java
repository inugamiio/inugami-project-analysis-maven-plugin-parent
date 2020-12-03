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
package io.inugami.maven.plugin.analysis.plugin.services.scan;

import io.inugami.api.models.data.basic.JsonObject;
import io.inugami.api.spi.SpiPriority;
import io.inugami.maven.plugin.analysis.api.actions.ProjectScanner;
import io.inugami.maven.plugin.analysis.api.models.ScanConext;
import io.inugami.maven.plugin.analysis.api.models.ScanNeo4jResult;

import java.util.List;

import static io.inugami.maven.plugin.analysis.api.tools.BuilderTools.buildNodeVersionFull;

@SpiPriority(1)
public class RootScan implements ProjectScanner {


    // =========================================================================
    // API
    // =========================================================================
    @Override
    public List<JsonObject> scan(final ScanConext context) {
        final ScanNeo4jResult result = ScanNeo4jResult.builder().build();
        result.addNode(buildNodeVersionFull(context.getProject(), context.getConfiguration()));
        return List.of(result);
    }


}
