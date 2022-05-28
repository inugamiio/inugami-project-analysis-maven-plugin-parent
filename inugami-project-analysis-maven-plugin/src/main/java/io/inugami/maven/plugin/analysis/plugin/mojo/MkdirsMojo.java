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
package io.inugami.maven.plugin.analysis.plugin.mojo;

import io.inugami.maven.plugin.analysis.plugin.services.build.BasicBuildService;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.util.List;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PUBLIC)
@Mojo(name = "mkdirs", defaultPhase = LifecyclePhase.INSTALL)
public class MkdirsMojo extends AbstractMojo {

    // =========================================================================
    // ATTRIBUTES
    // =========================================================================
    @Parameter
    private List<String> paths;
    @Parameter
    private boolean      failSafe;

    // =========================================================================
    // API
    // =========================================================================
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            new BasicBuildService().mkdirs(paths);
        }
        catch (Exception e) {
            if (failSafe) {
                log.error(e.getMessage(), e);
            }
            else {
                throw new MojoExecutionException(e.getMessage(), e);
            }
        }
    }

}
