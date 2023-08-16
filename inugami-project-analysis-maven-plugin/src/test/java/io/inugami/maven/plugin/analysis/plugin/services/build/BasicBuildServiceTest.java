package io.inugami.maven.plugin.analysis.plugin.services.build;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;

class BasicBuildServiceTest {


    @Test
    void isTextFile_withTextFile_shouldReturnTrue() {
        final BasicBuildService service = new BasicBuildService();
        assertThat(service.isTextFile(new File("/User/smith/joe.txt"), new ArrayList<>())).isTrue();
        assertThat(service.isTextFile(new File("/User/smith/joe.css"), new ArrayList<>())).isTrue();
    }

    @Test
    void isNotAllowDeleteFile_nominal_shouldReturnFalse() {
        final BasicBuildService service = new BasicBuildService();
        assertThat(service.isNotAllowDeleteFile(new File("/joe"))).isFalse();
        assertThat(service.isNotAllowDeleteFile(new File("/"))).isTrue();
    }

    @Test
    void windowsRootPath_withWindowsPath_shouldReturnTrue() {
        final BasicBuildService service = new BasicBuildService();
        assertThat(service.windowsRootPath("c:\\")).isTrue();
        assertThat(service.windowsRootPath("c:/")).isTrue();
    }

}