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
package io.inugami.maven.plugin.analysis.api.tools;

import org.junit.jupiter.api.Test;

import static io.inugami.maven.plugin.analysis.api.tools.BuilderTools.*;
import static org.assertj.core.api.Assertions.assertThat;

class BuilderToolsTest {
    @Test
    void testExtractMajorVersion() {
        assertThat(extractMajorVersion("3.17.1")).isEqualTo(3);
        assertThat(extractMajorVersion("3-FINAL")).isEqualTo(3);
    }

    @Test
    void testExtractMinorVersion() {
        assertThat(extractMinorVersion("3.17.1")).isEqualTo(17);
        assertThat(extractMinorVersion("3-FINAL")).isEqualTo(0);
        ;
    }

    @Test
    void testExtractPatchVersion() {
        assertThat(extractPatchVersion("3.17.1")).isEqualTo(1);
        assertThat(extractPatchVersion("3.17")).isEqualTo(0);
    }


    @Test
    void testExtractTag() {
        assertThat(extractTag("3.17.1")).isEqualTo("");
        assertThat(extractTag("2.2.4.RELEASE")).isEqualTo("RELEASE");
        assertThat(extractTag("2.2.4.RC3")).isEqualTo("RC3");

    }
}