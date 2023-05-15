package io.inugami.maven.plugin.analysis.front.springboot.configuration;

import io.inugami.monitoring.api.resolvers.Interceptable;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SkipIologOnRelease implements Interceptable {

    public static final String SEP              = "/";
    public static final String RELEASE_NOTE_APP = "release-note-app";
    private final       String currentContextPath;
    private final       String releaseNotePath;

    @Override
    public boolean isInterceptable(final String uri) {
        if (releaseNotePath == null) {
            return !uri.contains(RELEASE_NOTE_APP);
        } else {
            String fullPath = uri;

            if (currentContextPath != null) {
                final StringBuilder builder = new StringBuilder(currentContextPath);
                if (!uri.startsWith(SEP)) {
                    builder.append(SEP);
                }
                builder.append(uri);
                fullPath = builder.toString();
            }
            return !(fullPath.contains(releaseNotePath) || (fullPath + SEP).contains(releaseNotePath));
        }

    }
}
