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

import io.inugami.maven.plugin.analysis.front.core.servlet.InugamiServlet;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.WebApplicationInitializer;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import javax.annotation.PostConstruct;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;

@EnableWebMvc
@Configuration
public class MvcConfiguration implements WebMvcConfigurer , WebApplicationInitializer {

    @Value("${inugami.release.note.enabled:true}")
    private boolean mapping;
    @Value("${inugami.release.note.path:#{null}}")
    private String  path;

    private String currentPath;


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

    @Override
    public void onStartup(final ServletContext servletContext) throws ServletException {
        AnnotationConfigWebApplicationContext ctx = new AnnotationConfigWebApplicationContext();
        ctx.register(WebMvcConfigurer.class);
        ctx.setServletContext(servletContext);

        ServletRegistration.Dynamic servlet = servletContext.addServlet("dispatcherExample", new InugamiServlet(currentPath));
        servlet.setLoadOnStartup(1);
        servlet.addMapping(currentPath.substring(0, currentPath.length()-1));
    }
}
