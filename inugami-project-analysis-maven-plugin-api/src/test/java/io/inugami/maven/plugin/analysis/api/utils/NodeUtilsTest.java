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
package io.inugami.maven.plugin.analysis.api.utils;

import io.inugami.commons.test.UnitTestData;
import io.inugami.maven.plugin.analysis.api.models.Node;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.inugami.commons.test.UnitTestData.OTHER;
import static io.inugami.commons.test.UnitTestHelper.assertTextRelative;
import static io.inugami.maven.plugin.analysis.api.utils.NodeUtils.*;
import static org.assertj.core.api.Assertions.assertThat;


class NodeUtilsTest {


    public static final String EMPTY_LIST   = "[]";
    public static final String EMPTY_STR    = "";
    public static final String NOMINAL_LIST = "[Smith]";
    public static final String LASTNAME     = "lastname";

    @Test
    void testCleanLines() {
        assertThat(cleanLines("\"test\"")).isEqualTo("\\\"test\\\"");
    }

    @Test
    void sortProperties_nomninal() {
        Map<String, String> map = new HashMap<>();
        map.put(LASTNAME, UnitTestData.LASTNAME);
        map.put("firstname", UnitTestData.FIRSTNAME);

        assertTextRelative(sortProperties(map), "api/utils/nodeUtilsTest/sortProperties_nomninal.json");
        assertThat(sortProperties(null).size()).isEqualTo(0);
    }

    @Test
    void processIfNotNull_nominal() {

        List<String> result    = new ArrayList<>();
        String       nullValue = null;

        processIfNotNull(nullValue, result::add);
        assertThat(result).hasToString(EMPTY_LIST);

        processIfNotNull(UnitTestData.LASTNAME, null);
        assertThat(result).hasToString(EMPTY_LIST);

        processIfNotNull(UnitTestData.LASTNAME, result::add);
        assertThat(result).hasToString(NOMINAL_LIST);

    }


    @Test
    void processIfNotEmpty_nominal() {

        List<String> result    = new ArrayList<>();
        String       nullValue = null;

        processIfNotEmpty(nullValue, result::add);
        assertThat(result).hasToString(EMPTY_LIST);

        processIfNotEmpty(UnitTestData.LASTNAME, null);
        assertThat(result).hasToString(EMPTY_LIST);

        processIfNotEmpty(EMPTY_STR, result::add);
        assertThat(result).hasToString(EMPTY_LIST);

        processIfNotEmpty(UnitTestData.LASTNAME, result::add);
        assertThat(result).hasToString(NOMINAL_LIST);
    }


    @Test
    void processIfNotEmpty_list() {
        List<String> result    = new ArrayList<>();
        List<String> nullValue = null;

        processIfNotEmpty(nullValue, result::addAll);
        assertThat(result).hasToString(EMPTY_LIST);

        processIfNotEmpty(new ArrayList<String>(), result::addAll);
        assertThat(result).hasToString(EMPTY_LIST);

        processIfNotEmpty(List.of(UnitTestData.LASTNAME), null);
        assertThat(result).hasToString(EMPTY_LIST);

        processIfNotEmpty(List.of(UnitTestData.LASTNAME), result::addAll);
        assertThat(result).hasToString(NOMINAL_LIST);
    }

    @Test
    void processIfNotEmptyForce_nominal() {

        List<String> result    = new ArrayList<>();
        String       nullValue = null;

        processIfNotEmptyForce(nullValue, result::add);
        assertThat(result).hasToString(EMPTY_LIST);

        processIfNotEmptyForce(UnitTestData.LASTNAME, null);
        assertThat(result).hasToString(EMPTY_LIST);

        processIfNotEmptyForce(EMPTY_STR, result::add);
        assertThat(result).hasToString(EMPTY_LIST);


        processIfNotEmptyForce(NULL, result::add);
        assertThat(result).hasToString(EMPTY_LIST);

        processIfNotEmptyForce(UnitTestData.LASTNAME, result::add);
        assertThat(result).hasToString(NOMINAL_LIST);
    }

    @Test
    void cleanLines_nominal() {
        assertThat(cleanLines(null)).isNull();
        assertThat(cleanLines("\n\\Foo")).isEqualTo("\\n\\Foo");
    }

    @Test
    void hasText_nominal() {
        assertThat(hasText(null)).isFalse();
        assertThat(hasText(EMPTY_STR)).isFalse();
        assertThat(hasText("         ")).isFalse();
        assertThat(hasText(UnitTestData.LASTNAME)).isTrue();
    }

    @Test
    void getStringValue_nominal() {
        final Node node = Node.builder().properties(Map.of(LASTNAME, UnitTestData.LASTNAME)).build();


        assertThat(getStringValue(null, null)).isNull();
        assertThat(getStringValue(LASTNAME, null)).isNull();
        assertThat(getStringValue(LASTNAME, Node.builder().build())).isNull();
        assertThat(getStringValue(LASTNAME, Node.builder().properties(Map.of()).build())).isNull();

        assertThat(getStringValue(null, node)).isNull();
        assertThat(getStringValue(OTHER, node)).isNull();

        assertThat(getStringValue(LASTNAME, node)).isEqualTo(UnitTestData.LASTNAME);
    }
}