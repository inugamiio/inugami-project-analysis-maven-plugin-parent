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
package io.inugami.maven.plugin.analysis.front.core.renderers;

import io.inugami.api.models.JsonBuilder;
import io.inugami.api.spi.SpiLoader;
import io.inugami.maven.plugin.analysis.front.api.FrontPluginSpi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@RequiredArgsConstructor
public class PluginsModuleRenderer {

    // =========================================================================
    // ATTRIBUTES
    // =========================================================================
    private static final AtomicReference<String> CACHE   = new AtomicReference<>();
    private static final List<FrontPluginSpi>    PLUGINS = loadPlugins();

    // =========================================================================
    // INIT
    // =========================================================================
    private static List<FrontPluginSpi> loadPlugins() {
        final List<FrontPluginSpi> result = SpiLoader.getInstance().loadSpiServicesByPriority(FrontPluginSpi.class);
        return result == null ? new ArrayList<>() : result;
    }

    // =========================================================================
    // RENDERING
    // =========================================================================
    public synchronized String render() {
        String result = CACHE.get();
        if (result == null) {
            final JsonBuilder buffer = new JsonBuilder();

            writeImport(buffer);
            buffer.line();
            writeNgModule(buffer);
            buffer.line();
            writeClass(buffer);

            result = buffer.toString();
            CACHE.set(result);
        }
        return result;
    }


    // =========================================================================
    // PRIVATE
    // =========================================================================
    private void writeImport(final JsonBuilder buffer) {
        final Map<String, String> modules = extractModulesImports();

        for (final Map.Entry<String, String> module : modules.entrySet()) {
            buffer.write("import { ").write(module.getKey()).write(" }");
            buffer.write(" from '").write(module.getValue()).write("'").addEndLine();
        }

    }

    private Map<String, String> extractModulesImports() {
        final Map<String, String> result = new LinkedHashMap<>();
        result.put("NgModule", "@angular/core");
        result.put("CommonModule", "@angular/common");
        result.put("BrowserModule", "@angular/platform-browser");
        result.put("InugamiApiModule", "./inugami-api/inugami-api.module");

        for (final FrontPluginSpi plugin : PLUGINS) {
            if (plugin.getNgModuleName() != null && plugin.getModuleFolder() != null && plugin.getNgModuleFileName() != null) {
                result.put(plugin.getNgModuleName(),
                           String.join("/", ".", plugin.getModuleFolder(), plugin.getNgModuleFileName()));
            }
        }
        return result;
    }

    private void writeNgModule(final JsonBuilder buffer) {
        final List<String> modules = new ArrayList<>(List.of("CommonModule", "BrowserModule", "InugamiApiModule"));

        for (final FrontPluginSpi plugin : PLUGINS) {
            if (plugin.getNgModuleName() != null) {
                modules.add(plugin.getNgModuleName());
            }
        }
        final Iterator<String> iterator = modules.iterator();

        buffer.write("@NgModule({").line();
        buffer.tab().write("imports: ").openList().line();
        while (iterator.hasNext()) {
            final String module = iterator.next();
            buffer.tab().tab().write(module);
            if (iterator.hasNext()) {
                buffer.addSeparator().line();
            } else {
                buffer.line();
            }
        }
        buffer.tab().closeList().line();
        buffer.write("})");

    }

    private void writeClass(final JsonBuilder buffer) {
        buffer.write("export class PluginsModule { }");
    }

}
