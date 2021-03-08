package io.inugami.maven.plugin.analysis.plugin.mojo;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import io.inugami.maven.plugin.analysis.plugin.services.MojoHelper;

@Mojo(name = "handlerInformation")
public class LifeCycleInformationHandler extends AbstractMojo {
    public void execute() throws MojoExecutionException {
        new MojoHelper().drawDeco("Information", ">");
    }
}