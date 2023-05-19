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
package io.inugami.maven.plugin.analysis.plugin.services.postAnalyzers;

import io.inugami.api.models.data.basic.JsonObject;
import io.inugami.maven.plugin.analysis.api.actions.ProjectPostAnalyzer;
import io.inugami.maven.plugin.analysis.api.models.ScanConext;

import java.util.List;

public class HiddenDependenciesAnalyzer implements ProjectPostAnalyzer {


    // =========================================================================
    // CONSTRUCTORS
    // =========================================================================
    @Override
    public boolean accept(final ScanConext context) {
        return true;
    }

    @Override
    public void shutdown() {
        // nothing
    }

    // =========================================================================
    // API
    // =========================================================================
    @Override
    public void postAnalyze(final ScanConext context, final List<JsonObject> result) {
        // nothing
    }
}
