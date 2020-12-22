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
package io.inugami.maven.plugin.analysis.plugin.services.info.release.note.writers.asciidoc;

import io.inugami.maven.plugin.analysis.api.services.info.release.note.writers.asciidoc.AsciidocInfoWriter;
import io.inugami.maven.plugin.analysis.plugin.services.info.release.note.extractors.DependenciesExtractor;

public class DependenciesAsciidocWriter extends DependenciesProjectAsciidocWriter implements AsciidocInfoWriter {

    // =========================================================================
    // ATTRIBUTES
    // =========================================================================
    @Override
    public String getParagraphName() {
        return DependenciesExtractor.TYPE;
    }

    @Override
    protected String getParagraphBaseName() {
        return "dependencies";
    }

    @Override
    protected String getParagraphBaseTitle() {
        return "Dependencies";
    }

}
