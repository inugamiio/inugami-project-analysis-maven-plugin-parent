package io.inugami.maven.plugin.analysis.plugin.services.scan.classes;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

class ClassesScanTest {

    @Test
    void testBuildClassNameNormilized() {
        assertThat(ClassesScan.buildClassNameNormilized("io/inugami/project/service/Service.class",
                                                        "io.inugami",
                                                        "/"))
                .isEqualTo("project.service.Service");

        assertThat(ClassesScan.buildClassNameNormilized("io\\inugami\\project/service\\Service.class",
                                                        "io.inugami",
                                                        "\\"))
                .isEqualTo("project.service.Service");
    }
}