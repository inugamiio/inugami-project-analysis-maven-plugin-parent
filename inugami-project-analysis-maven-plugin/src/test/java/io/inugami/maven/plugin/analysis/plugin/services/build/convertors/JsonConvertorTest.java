package io.inugami.maven.plugin.analysis.plugin.services.build.convertors;

import io.inugami.commons.test.UnitTestHelper;
import org.junit.jupiter.api.Test;

import java.util.Map;

public class JsonConvertorTest {

    @Test
    public void convert_nominal_shouldConvertToMap() {
        String content = UnitTestHelper.loadJsonReference("build/convertors/json.convertor.ref.json");

        final Map<String, String> result = new JsonConvertor().convert(content);

        UnitTestHelper.assertText(result,
                                  UnitTestHelper.loadJsonReference("build/convertors/json.convertor.result.json"));
    }
}