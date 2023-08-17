package io.inugami.maven.plugin.analysis.api.utils;

import io.inugami.commons.test.UnitTestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.inugami.commons.test.UnitTestHelper.assertUtilityClassLombok;
import static org.assertj.core.api.Assertions.assertThat;

class CacheUtilsTest {

    public static final String KEY       = "key";
    public static final String EMPTY_MAP = "{}";

    @BeforeEach
    void init() {
        CacheUtils.clear();
    }

    @Test
    void cacheUtils() {
        assertUtilityClassLombok(CacheUtils.class);
    }

    @Test
    void put_nominal() {
        CacheUtils.put(null, null);
        assertThat(CacheUtils.getCaches()).hasToString(EMPTY_MAP);

        CacheUtils.put(KEY, null);
        assertThat(CacheUtils.getCaches()).hasToString(EMPTY_MAP);

        CacheUtils.put(null, UnitTestData.LASTNAME);
        assertThat(CacheUtils.getCaches()).hasToString(EMPTY_MAP);

        CacheUtils.put(KEY, UnitTestData.LASTNAME);
        assertThat(CacheUtils.getCaches()).hasToString("{key=Smith}");

        String resultNull = CacheUtils.get(null);
        assertThat(resultNull).isNull();

        String result = CacheUtils.get(KEY);
        assertThat(result).hasToString("Smith");
    }
}