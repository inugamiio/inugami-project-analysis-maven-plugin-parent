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
import io.inugami.maven.plugin.analysis.api.actions.ResultWriter;
import io.inugami.maven.plugin.analysis.api.models.ScanConext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class WriterService {

    // =========================================================================
    // ATTRIBUTES
    // =========================================================================
    private final List<JsonObject>   data;
    private final ScanConext         context;
    private final List<ResultWriter> writers;

    // =========================================================================
    // API
    // =========================================================================
    public void write() {
        Loggers.APPLICATION.info("writing result");
        if (data != null && writers != null) {

            for (final ResultWriter writer : writers) {
                for (final JsonObject value : data) {
                    if (value != null) {
                        writer.accept(value, context);
                    }
                }
            }

        }
        try {
            for (final ResultWriter writer : writers) {
                writer.write();
            }

        }
        catch (final Exception error) {
            log.error(error.getMessage(), error);
        }
    }

}
