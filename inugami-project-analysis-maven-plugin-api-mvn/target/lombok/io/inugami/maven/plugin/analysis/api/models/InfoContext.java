// Generated by delombok at Mon Mar 08 22:38:49 CET 2021
/* --------------------------------------------------------------------
 *  Inugami
 * --------------------------------------------------------------------
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package io.inugami.maven.plugin.analysis.api.models;

import io.inugami.api.processors.ConfigHandler;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcher;
import java.io.File;

public class InfoContext {
    private final File basedir;
    private final File buildDir;
    private final MavenProject project;
    private final ConfigHandler<String, String> configuration;
    private final SecDispatcher secDispatcher;
    private final ArtifactHandler artifactHandler;
    private final Settings settings;


    @java.lang.SuppressWarnings("all")
    public static class InfoContextBuilder {
        @java.lang.SuppressWarnings("all")
        private File basedir;
        @java.lang.SuppressWarnings("all")
        private File buildDir;
        @java.lang.SuppressWarnings("all")
        private MavenProject project;
        @java.lang.SuppressWarnings("all")
        private ConfigHandler<String, String> configuration;
        @java.lang.SuppressWarnings("all")
        private SecDispatcher secDispatcher;
        @java.lang.SuppressWarnings("all")
        private ArtifactHandler artifactHandler;
        @java.lang.SuppressWarnings("all")
        private Settings settings;

        @java.lang.SuppressWarnings("all")
        InfoContextBuilder() {
        }

        @java.lang.SuppressWarnings("all")
        public InfoContext.InfoContextBuilder basedir(final File basedir) {
            this.basedir = basedir;
            return this;
        }

        @java.lang.SuppressWarnings("all")
        public InfoContext.InfoContextBuilder buildDir(final File buildDir) {
            this.buildDir = buildDir;
            return this;
        }

        @java.lang.SuppressWarnings("all")
        public InfoContext.InfoContextBuilder project(final MavenProject project) {
            this.project = project;
            return this;
        }

        @java.lang.SuppressWarnings("all")
        public InfoContext.InfoContextBuilder configuration(final ConfigHandler<String, String> configuration) {
            this.configuration = configuration;
            return this;
        }

        @java.lang.SuppressWarnings("all")
        public InfoContext.InfoContextBuilder secDispatcher(final SecDispatcher secDispatcher) {
            this.secDispatcher = secDispatcher;
            return this;
        }

        @java.lang.SuppressWarnings("all")
        public InfoContext.InfoContextBuilder artifactHandler(final ArtifactHandler artifactHandler) {
            this.artifactHandler = artifactHandler;
            return this;
        }

        @java.lang.SuppressWarnings("all")
        public InfoContext.InfoContextBuilder settings(final Settings settings) {
            this.settings = settings;
            return this;
        }

        @java.lang.SuppressWarnings("all")
        public InfoContext build() {
            return new InfoContext(this.basedir, this.buildDir, this.project, this.configuration, this.secDispatcher, this.artifactHandler, this.settings);
        }

        @java.lang.Override
        @java.lang.SuppressWarnings("all")
        public java.lang.String toString() {
            return "InfoContext.InfoContextBuilder(basedir=" + this.basedir + ", buildDir=" + this.buildDir + ", project=" + this.project + ", configuration=" + this.configuration + ", secDispatcher=" + this.secDispatcher + ", artifactHandler=" + this.artifactHandler + ", settings=" + this.settings + ")";
        }
    }

    @java.lang.SuppressWarnings("all")
    public static InfoContext.InfoContextBuilder builder() {
        return new InfoContext.InfoContextBuilder();
    }

    @java.lang.SuppressWarnings("all")
    public File getBasedir() {
        return this.basedir;
    }

    @java.lang.SuppressWarnings("all")
    public File getBuildDir() {
        return this.buildDir;
    }

    @java.lang.SuppressWarnings("all")
    public MavenProject getProject() {
        return this.project;
    }

    @java.lang.SuppressWarnings("all")
    public ConfigHandler<String, String> getConfiguration() {
        return this.configuration;
    }

    @java.lang.SuppressWarnings("all")
    public SecDispatcher getSecDispatcher() {
        return this.secDispatcher;
    }

    @java.lang.SuppressWarnings("all")
    public ArtifactHandler getArtifactHandler() {
        return this.artifactHandler;
    }

    @java.lang.SuppressWarnings("all")
    public Settings getSettings() {
        return this.settings;
    }

    @java.lang.SuppressWarnings("all")
    private InfoContext(final File basedir, final File buildDir, final MavenProject project, final ConfigHandler<String, String> configuration, final SecDispatcher secDispatcher, final ArtifactHandler artifactHandler, final Settings settings) {
        this.basedir = basedir;
        this.buildDir = buildDir;
        this.project = project;
        this.configuration = configuration;
        this.secDispatcher = secDispatcher;
        this.artifactHandler = artifactHandler;
        this.settings = settings;
    }
}
