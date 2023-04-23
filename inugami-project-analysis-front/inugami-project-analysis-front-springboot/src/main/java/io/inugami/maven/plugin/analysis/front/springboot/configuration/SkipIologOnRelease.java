package io.inugami.maven.plugin.analysis.front.springboot.configuration;

import io.inugami.monitoring.api.resolvers.Interceptable;

public class SkipIologOnRelease implements Interceptable {

    @Override
    public boolean isInterceptable(final String uri) {
        return !uri.contains(MvcConfiguration.CURRENT_PATH.get());
    }
}
