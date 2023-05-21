package io.inugami.maven.plugin.analysis.api.models;

import io.inugami.commons.test.dto.AssertDtoContext;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.inugami.commons.test.UnitTestHelper.assertDto;
import static org.assertj.core.api.Assertions.assertThat;

class FileResourcesTest {


    @Test
    void fileResources() {
        assertDto(new AssertDtoContext<FileResources>()
                          .toBuilder()
                          .objectClass(FileResources.class)
                          .fullArgConstructorRefPath("api/models/fileResourcesTest/fullArgConstructorRefPath.json")
                          .getterRefPath("api/models/fileResourcesTest/getterRefPath.json")
                          .toStringRefPath("api/models/fileResourcesTest/toStringRefPath.txt")
                          .cloneFunction(instance -> instance.toBuilder().build())
                          .noArgConstructor(() -> new FileResources())
                          .fullArgConstructor(FileResourcesTest::buildDataSet)
                          .noEqualsFunction(FileResourcesTest::notEquals)
                          .checkSetters(true)
                          .build());
    }

    static void notEquals(final FileResources value) {
        assertThat(value).isNotEqualTo(value.toBuilder());
        assertThat(value.hashCode()).isNotEqualTo(value.toBuilder().hashCode());

        assertThat(value).isNotEqualTo(value.toBuilder().target("other").build());
        assertThat(value.hashCode()).isNotEqualTo(value.toBuilder().target("other").build().hashCode());
    }

    public static FileResources buildDataSet() {
        return FileResources.builder()
                            .target("target")
                            .template("basic template")
                            .templatePath("/path/for/template.txt")
                            .properties(Map.ofEntries(Map.entry("name", "joe")))
                            .build();
    }
}