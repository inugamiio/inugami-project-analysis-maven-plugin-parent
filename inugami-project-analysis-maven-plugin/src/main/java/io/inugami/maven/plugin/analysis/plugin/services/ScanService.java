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
package io.inugami.maven.plugin.analysis.plugin.services;


import io.inugami.api.loggers.Loggers;
import io.inugami.api.models.data.basic.JsonObject;
import io.inugami.maven.plugin.analysis.api.actions.ProjectScanner;
import io.inugami.maven.plugin.analysis.api.models.ScanConext;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@AllArgsConstructor
public class ScanService {

    // =========================================================================
    // ATTRIBUTES
    // =========================================================================
    private final ScanConext           context;
    private final List<ProjectScanner> projectScanners;

    // =========================================================================
    // API
    // =========================================================================
    public List<JsonObject> scan() {
        final List<JsonObject> result = new ArrayList<>();
        if (projectScanners != null) {

            for (final ProjectScanner scan : projectScanners) {
                if(scan.enable(context)){
                    Loggers.APPLICATION.info("start scan : {}", scan.getClass().getName());
                    try {
                        final List<JsonObject> stepResult = scan.scan(context);
                        if (stepResult != null) {
                            result.addAll(stepResult);
                        }
                    }
                    catch (final Exception error) {
                        log.error(error.getMessage(), error);
                    }
                }
            }
        }
        return result;
    }

}
