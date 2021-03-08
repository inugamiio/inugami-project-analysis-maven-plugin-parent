package io.inugami.maven.plugin.analysis.plugin.mojo;

import io.inugami.maven.plugin.analysis.plugin.services.MojoHelper;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;

@Mojo(name = "handlerAnalyse")
public class LifeCycleAnalyseHandler extends AbstractMojo {
    @Override
    public void execute() throws MojoExecutionException {
        new MojoHelper().drawDeco("Analyse", ">");
    }
}