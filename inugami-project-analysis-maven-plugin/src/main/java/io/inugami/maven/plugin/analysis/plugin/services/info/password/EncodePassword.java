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
package io.inugami.maven.plugin.analysis.plugin.services.info.password;

import io.inugami.api.exceptions.Asserts;
import io.inugami.api.processors.ConfigHandler;
import io.inugami.maven.plugin.analysis.api.actions.ProjectInformation;
import io.inugami.maven.plugin.analysis.api.tools.ConsoleTools;
import io.inugami.maven.plugin.analysis.api.tools.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.project.MavenProject;

@Slf4j
public class EncodePassword implements ProjectInformation {


    // =========================================================================
    // API
    // =========================================================================
    @Override
    public void process(final MavenProject project, final ConfigHandler<String, String> configuration) {
        final String password = ConsoleTools.askPassword("Password : ", null);

        String secret = configuration.grabOrDefault("inugami.maven.plugin.analysis.secret", null);
        if (secret == null) {
            secret = ConsoleTools.askPassword("Secret passphrase (16 chars) : ", null);
        }
        Asserts.notEmpty("password shouldn't be null", secret);
        Asserts.notEmpty("secret passphrase shouldn't be null", secret);
        final String result = SecurityUtils.encodeAes(password, secret);
        log.info("encoded password : {}", result);
    }

}
