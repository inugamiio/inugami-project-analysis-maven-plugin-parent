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
}