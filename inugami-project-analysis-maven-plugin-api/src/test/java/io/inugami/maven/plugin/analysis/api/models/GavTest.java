package io.inugami.maven.plugin.analysis.api.models;

import io.inugami.commons.test.dto.AssertDtoContext;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static io.inugami.commons.test.UnitTestData.OTHER;
import static io.inugami.commons.test.UnitTestHelper.assertDto;
import static io.inugami.commons.test.UnitTestHelper.assertTextRelative;
import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings({"java:S5838"})
class GavTest {

    public static final String VERSION    = "1.0.0";
    public static final String IO_INUGAMI = "io.inugami";

    @Test
    void gav() {
        assertDto(new AssertDtoContext<Gav>()
                          .toBuilder()
                          .objectClass(Gav.class)
                          .fullArgConstructorRefPath("api/models/gavTest/fullArgConstructorRefPath.json")
                          .getterRefPath("api/models/gavTest/getterRefPath.json")
                          .toStringRefPath("api/models/gavTest/toStringRefPath.txt")
                          .cloneFunction(instance -> instance.toBuilder().build())
                          .noArgConstructor(() -> new Gav())
                          .fullArgConstructor(GavTest::buildDataSet)
                          .noEqualsFunction(GavTest::notEquals)
                          .checkSetters(true)
                          .build());
    }

    static void notEquals(final Gav instance) {
        assertThat(instance).isNotEqualTo(instance.toBuilder());
        assertThat(instance.hashCode()).isNotEqualTo(instance.toBuilder().hashCode());

        //
        assertThat(instance).isNotEqualTo(instance.toBuilder().groupId(null).build());
        assertThat(instance.toBuilder().groupId(null).build()).isNotEqualTo(instance);
        assertThat(instance).isNotEqualTo(instance.toBuilder().groupId(OTHER).build());
        assertThat(instance.toBuilder().groupId(OTHER).build()).isNotEqualTo(instance);
        assertThat(instance.hashCode()).isNotEqualTo(instance.toBuilder().groupId(null).build().hashCode());
        assertThat(instance.toBuilder().groupId(OTHER).build().hashCode()).isNotEqualTo(instance.hashCode());
        //
        assertThat(instance).isNotEqualTo(instance.toBuilder().artifactId(null).build());
        assertThat(instance.toBuilder().artifactId(null).build()).isNotEqualTo(instance);
        assertThat(instance).isNotEqualTo(instance.toBuilder().artifactId(OTHER).build());
        assertThat(instance.toBuilder().artifactId(OTHER).build()).isNotEqualTo(instance);
        assertThat(instance.hashCode()).isNotEqualTo(instance.toBuilder().artifactId(null).build().hashCode());
        assertThat(instance.toBuilder().artifactId(OTHER).build().hashCode()).isNotEqualTo(instance.hashCode());
        //
        assertThat(instance).isNotEqualTo(instance.toBuilder().version(null).build());
        assertThat(instance.toBuilder().version(null).build()).isNotEqualTo(instance);
        assertThat(instance).isNotEqualTo(instance.toBuilder().version(OTHER).build());
        assertThat(instance.toBuilder().version(OTHER).build()).isNotEqualTo(instance);
        assertThat(instance.hashCode()).isNotEqualTo(instance.toBuilder().version(null).build().hashCode());
        assertThat(instance.toBuilder().version(OTHER).build().hashCode()).isNotEqualTo(instance.hashCode());
        //
        assertThat(instance).isNotEqualTo(instance.toBuilder().type(null).build());
        assertThat(instance.toBuilder().type(null).build()).isNotEqualTo(instance);
        assertThat(instance).isNotEqualTo(instance.toBuilder().type(OTHER).build());
        assertThat(instance.toBuilder().type(OTHER).build()).isNotEqualTo(instance);
        assertThat(instance.hashCode()).isNotEqualTo(instance.toBuilder().type(null).build().hashCode());
        assertThat(instance.toBuilder().type(OTHER).build().hashCode()).isNotEqualTo(instance.hashCode());
    }


    @Test
    void compareTo_nominal() {
        assertThat(buildDataSet().compareTo(null)).isEqualTo(-5);
        assertThat(buildDataSet().compareTo(Gav.builder().build())).isEqualTo(-5);
        assertThat(buildDataSet().compareTo(buildDataSet())).isEqualTo(0);
        assertThat(buildDataSet().compareTo(Gav.builder()
                                               .groupId(IO_INUGAMI)
                                               .artifactId("aaa-artifact")
                                               .version(VERSION)
                                               .build())).isEqualTo(18);
        assertThat(buildDataSet().compareTo(Gav.builder()
                                               .groupId(IO_INUGAMI)
                                               .artifactId("zzz-artifact")
                                               .version(VERSION)
                                               .build())).isEqualTo(-7);
    }

    @Test
    void addDependency_nominal() {
        final Gav gav = buildDataSet();
        gav.addDependency(Gav.builder()
                             .groupId(IO_INUGAMI)
                             .artifactId("common-artifact")
                             .version(VERSION)
                             .build());
        assertTextRelative(gav, "api/models/gavTest/addDependency_nominal.json");
        gav.addDependency(null);
        assertTextRelative(gav, "api/models/gavTest/addDependency_nominal.json");
    }

    @Test
    void addDependencies_nominal() {
        final Gav gav = buildDataSet();
        gav.addDependencies(List.of(Gav.builder()
                                       .groupId(IO_INUGAMI)
                                       .artifactId("common-artifact")
                                       .version(VERSION)
                                       .build()));
        assertTextRelative(gav, "api/models/gavTest/addDependency_nominal.json");
        gav.addDependencies(null);
        assertTextRelative(gav, "api/models/gavTest/addDependency_nominal.json");
    }

    public static Gav buildDataSet() {
        return Gav.builder()
                  .groupId(IO_INUGAMI)
                  .artifactId("some-artifact")
                  .version(VERSION)
                  .type("jar")
                  .scope("compile")
                  .dependencies(new LinkedHashSet<>(Set.of(Gav.builder()
                                                              .groupId(IO_INUGAMI)
                                                              .artifactId("other-artifact")
                                                              .version(VERSION)
                                                              .type("jar")
                                                              .scope("compile")
                                                              .build())))
                  .build();
    }
}
