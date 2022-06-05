package io.inugami.maven.plugin.analysis.plugin.services.build;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;

public class BasicBuildServiceTest {


    @Test
    public void isTextFile_withTextFile_shouldReturnTrue() {
        BasicBuildService service = new BasicBuildService();
        assertThat(service.isTextFile(new File("/User/smith/joe.txt"), new ArrayList<>())).isTrue();
        assertThat(service.isTextFile(new File("/User/smith/joe.css"), new ArrayList<>())).isTrue();
    }

    @Test
    public void isNotAllowDeleteFile_nominal_shouldReturnFalse() {
        BasicBuildService service = new BasicBuildService();
        assertThat(service.isNotAllowDeleteFile(new File("/joe"))).isFalse();
        assertThat(service.isNotAllowDeleteFile(new File("/"))).isTrue();
    }

    @Test
    public void windowsRootPath_withWindowsPath_shouldReturnTrue() {
        BasicBuildService service = new BasicBuildService();
        assertThat(service.windowsRootPath("c:\\")).isTrue();
        assertThat(service.windowsRootPath("c:/")).isTrue();
    }

}