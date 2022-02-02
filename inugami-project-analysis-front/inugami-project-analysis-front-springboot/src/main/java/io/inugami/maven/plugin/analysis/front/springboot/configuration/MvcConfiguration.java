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
package io.inugami.maven.plugin.analysis.front.springboot.configuration;

import io.inugami.maven.plugin.analysis.front.api.services.DependenciesCheckService;
import io.inugami.maven.plugin.analysis.front.core.servlet.DependenciesCheckServlet;
import io.inugami.maven.plugin.analysis.front.core.servlet.InugamiServlet;
import io.inugami.maven.plugin.analysis.front.core.servlet.PluginsModuleServlet;
import io.inugami.maven.plugin.analysis.front.core.servlet.ReleaseNoteServlet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import javax.annotation.PostConstruct;

@EnableWebMvc
@Configuration
public class MvcConfiguration implements WebMvcConfigurer {

    @Value("${inugami.release.note.enabled:true}")
    private boolean mapping;
    @Value("${inugami.release.note.path:#{null}}")
    private String  path;
    @Value("${inugami.release.note.artifactName:release-note}")
    private String  artifactName;
    private String  currentPath;

    @Autowired(required = false)
    private DependenciesCheckService dependenciesCheckService;

    @PostConstruct
    public void init() {
        if (path == null) {
            path = "/release-note-app/";
        }

        currentPath = path;
        if (!currentPath.endsWith("/")) {
            currentPath = currentPath + "/";
        }
    }


    // =========================================================================
    // API
    // =========================================================================
    @Override
    public void addResourceHandlers(final ResourceHandlerRegistry registry) {
        if (mapping) {
            registry.addResourceHandler(currentPath + "**")
                    .addResourceLocations("classpath:/META-INF/resources/release-note-app/")
                    .resourceChain(true)
                    .addResolver(new PathResourceResolver());
        }
    }

    @Bean
    public ServletRegistrationBean indexHtmlBean() {
        final String basePath = currentPath.substring(0, currentPath.length() - 1);
        final ServletRegistrationBean bean = new ServletRegistrationBean(
                new InugamiServlet(basePath), currentPath, currentPath + "index.html");
        bean.setLoadOnStartup(1);
        return bean;
    }

    @Bean
    public ServletRegistrationBean pluginsModulesBean() {
        final String basePath = currentPath.substring(0, currentPath.length() - 1);
        final ServletRegistrationBean bean = new ServletRegistrationBean(
                new PluginsModuleServlet(), currentPath + "app/modules/plugins.module.ts");
        bean.setLoadOnStartup(1);
        return bean;
    }

    @Bean
    public ServletRegistrationBean releaseNoteDataBean() {
        final String basePath = currentPath.substring(0, currentPath.length() - 1);
        final ServletRegistrationBean bean = new ServletRegistrationBean(
                new ReleaseNoteServlet(artifactName), currentPath + "data/release-notes.json");
        bean.setLoadOnStartup(1);
        return bean;
    }

    @Bean
    public ServletRegistrationBean dependenciesCheckData() {
        final String basePath = currentPath.substring(0, currentPath.length() - 1);
        final ServletRegistrationBean bean = new ServletRegistrationBean(
                new DependenciesCheckServlet(dependenciesCheckService), currentPath + "data/dependencies-check.json");
        bean.setLoadOnStartup(1);
        return bean;
    }
}
