package io.inugami.maven.plugin.analysis.plugin.services.scan.git;

import io.inugami.api.processors.ConfigHandler;
import io.inugami.configuration.services.ConfigHandlerHashMap;
import lombok.extern.slf4j.Slf4j;

import java.io.File;

@Slf4j
class GitLogScanIT {

    public static void main(final String... arg) {
        final GitLogScan gitLogScan = new GitLogScan();

        final ConfigHandler<String, String> config = new ConfigHandlerHashMap();
        try {
            gitLogScan.extractGitLogs(new File(System.getProperty("project.path")), System.getProperty("project.version"));
        }
        catch (final Exception e) {
            log.error(e.getMessage(), e);
            System.exit(1);
        }
    }
}