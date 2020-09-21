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
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;
import org.xeustechnologies.jcl.JarClassLoader;

import java.io.File;
import java.util.List;
import java.util.Set;

@Builder
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ScanConext {
    private final File                          basedir;
    private final MavenProject                  project;
    private final RepositorySystem              repoSystem;
    private final RepositorySystemSession       repoSession;
    private final List<RemoteRepository>        repositories;
    private final JarClassLoader                classLoader;
    private final Set<Gav>                      dependencies;
    private final Set<Gav>                      directDependencies;
    private final PluginDescriptor              pluginDescriptor;
    private final ConfigHandler<String, String> configuration;

    public <T> T getProperty(final String key) {
        T result = null;
        if (project != null && project.getProperties() != null) {
            result = (T) project.getProperties().get(key);
        }
        return result;
    }

    public <T> T getProperty(final String key, final T defaultValue) {
        T result = null;
        if (project != null && project.getProperties() != null) {
            result = (T) project.getProperties().getOrDefault(key, defaultValue);
        }
        return result;
    }
}
