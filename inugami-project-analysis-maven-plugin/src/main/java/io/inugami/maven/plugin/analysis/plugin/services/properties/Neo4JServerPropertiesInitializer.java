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
package io.inugami.maven.plugin.analysis.plugin.services.properties;

import io.inugami.api.processors.ConfigHandler;
import io.inugami.maven.plugin.analysis.api.actions.PropertiesInitialization;
import io.inugami.maven.plugin.analysis.api.exceptions.ConfigurationException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcher;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcherException;

/*
 * Allow to configure Neo4J from servers definition in maven settings.xml
 * <pre>
 *     <server>
 *         <id>neo4j</id>
 *         <username>neo4j</username>
 *         <password>{Yt5nGluOZ0sHzEiL7Le2IHFjtuTonkfx4yVEG3CYzZ8=}</password>
 *         <configuration>
 *           <url>bolt://localhost:7687</url>
 *         </configuration>
 *     </server>
 * </pre>
 */
public class Neo4JServerPropertiesInitializer implements PropertiesInitialization {


    // =========================================================================
    // CONSTRUCTORS
    // =========================================================================
    @Override
    public void initialize(final ConfigHandler<String, String> configuration, final MavenProject project,
                           final Settings settings,
                           final SecDispatcher secDispatcher) {
        final Server neo4J = settings.getServer("neo4j");
        if (neo4J != null) {
            final Xpp3Dom config = getDom(neo4J.getConfiguration());
            try {
                processInit(configuration, secDispatcher, neo4J, config);
            }
            catch (final SecDispatcherException e) {
                throw new ConfigurationException(e.getMessage());
            }
        }
    }

    private void processInit(final ConfigHandler<String, String> configuration,
                             final SecDispatcher secDispatcher,
                             final Server neo4J,
                             final Xpp3Dom config) throws SecDispatcherException {

        final Xpp3Dom url = config.getChild("url");
        if (url != null) {
            configuration.put("inugami.maven.plugin.analysis.writer.neo4j.url", url.getValue());
        }
        if (neo4J.getUsername() != null && neo4J.getUsername().startsWith("{")) {
            configuration.put("inugami.maven.plugin.analysis.writer.neo4j.user",
                              secDispatcher.decrypt(neo4J.getUsername()));
        }

        configuration.put("inugami.maven.plugin.analysis.writer.neo4j.password",
                          secDispatcher.decrypt(neo4J.getPassword()));
    }

}
