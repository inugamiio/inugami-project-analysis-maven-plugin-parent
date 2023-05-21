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
package io.inugami.maven.plugin.analysis.api.actions;

import io.inugami.api.processors.ConfigHandler;
import io.inugami.maven.plugin.analysis.api.exceptions.ConfigurationException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcher;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcherException;

@SuppressWarnings({"java:S112"})
public interface PropertiesInitialization {
    String URL = "url";

    default void initialize(final ConfigHandler<String, String> configuration,
                            final MavenProject project,
                            final Settings settings,
                            final SecDispatcher secDispatcher) {
        final Server server = getServerName() == null ? null : settings.getServer(getServerName());

        if (server != null) {
            final Xpp3Dom serverConfig = getDom(server.getConfiguration());
            try {
                processInitialization(server, configuration, secDispatcher, serverConfig);
            } catch (final Exception e) {
                throw new ConfigurationException(e.getMessage());
            }
        }
    }

    default Xpp3Dom getServerUrl(final Xpp3Dom serverConfig) {
        return serverConfig == null ? null : serverConfig.getChild(URL);
    }

    default Xpp3Dom getDom(final Object value) {
        return value instanceof Xpp3Dom ? (Xpp3Dom) value : null;
    }

    default String getServerName() {
        return null;
    }

    default void processInitialization(final Server server,
                                       final ConfigHandler<String, String> configuration,
                                       final SecDispatcher secDispatcher,
                                       final Xpp3Dom serverConfig) throws Exception {
        //nothing
    }

    default String getDecryptedValue(final String value,
                                     final SecDispatcher secDispatcher) throws SecDispatcherException {
        String result = value;
        if (value != null && value.startsWith("{", 0)) {
            result = secDispatcher.decrypt(value);
        }
        return result;
    }
}
