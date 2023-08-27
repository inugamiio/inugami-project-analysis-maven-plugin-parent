package io.inugami.maven.plugin.analysis.functional;

import io.inugami.commons.test.UnitTestHelper;
import org.junit.jupiter.api.Test;

import java.util.*;

import static io.inugami.commons.test.UnitTestData.*;
import static io.inugami.commons.test.UnitTestHelper.assertUtilityClassLombok;
import static io.inugami.maven.plugin.analysis.functional.FunctionalUtils.applyIfNotEmpty;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class FunctionalUtilsTest {


    public static final String RESULT      = "[Smith]";
    public static final String RESULT_LIST = "[[Smith]]";
    public static final String RESULT_MAP = "{key=John}";

    @Test
    void utilityClass(){
        assertUtilityClassLombok(FunctionalUtils.class);
    }

    @Test
    void applyIfNotEmpty_withString() {

        List<String> values = new ArrayList<>();
        applyIfNotEmpty(LASTNAME, values::add);
        assertThat(values).hasToString(RESULT);

        applyIfNotEmpty("", values::add);
        assertThat(values).hasToString(RESULT);

        String nullValue = null;
        applyIfNotEmpty(nullValue, values::add);
        assertThat(values).hasToString(RESULT);

        applyIfNotEmpty(LASTNAME, null);
        assertThat(values).hasToString(RESULT);
    }

    @Test
    void applyIfNotEmpty_withCollection() {

        Collection<Collection<String>> values = new ArrayList<>();
        applyIfNotEmpty(List.of(LASTNAME), values::add);
        assertThat(values).hasToString(RESULT_LIST);

        applyIfNotEmpty(List.of(), values::add);
        assertThat(values).hasToString(RESULT_LIST);

        List<String> nullValue = null;
        applyIfNotEmpty(nullValue, values::add);
        assertThat(values).hasToString(RESULT_LIST);

        applyIfNotEmpty(List.of(LASTNAME), null);
        assertThat(values).hasToString(RESULT_LIST);
    }


    @Test
    void applyIfNotEmpty_withMap() {
        Map<String, String> map    = Map.ofEntries(Map.entry("key", FIRSTNAME));
        Map<String, String> mapEmpty    = new LinkedHashMap<>();
        Map<String, String> values = new LinkedHashMap<>();

        applyIfNotEmpty(map,  values::putAll);
        assertThat(values).hasToString(RESULT_MAP);

        applyIfNotEmpty(mapEmpty, values::putAll);
        assertThat(values).hasToString(RESULT_MAP);

        Map<String,String> nullValue = null;
        applyIfNotEmpty(nullValue, values::putAll);
        assertThat(values).hasToString(RESULT_MAP);

        applyIfNotEmpty(map, null);
        assertThat(values).hasToString(RESULT_MAP);
    }
}