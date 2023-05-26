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
import io.inugami.maven.plugin.analysis.front.core.servlet.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import javax.annotation.PostConstruct;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@EnableWebMvc
@Configuration
public class MvcConfiguration implements WebMvcConfigurer {
    public static final AtomicReference<String> CURRENT_PATH = new AtomicReference<>();
    public static final String                  SEP          = "/";
    @Value("${server.servlet.context-path:#{null}}")
    private             String                  contextPath;

    @Value("${server.servlet.html.base.path:#{null}}")
    private String htmlBasePath;


    @Value("${inugami.release.note.enabled:true}")
    private boolean enabled;
    @Value("${inugami.release.note.path:#{null}}")
    private String  path;

    @Value("${inugami.release.note.custom.css.path:#{null}}")
    private String customCss;

    @Value("${inugami.release.note.artifactName:release-note}")
    private String artifactName;
    private String currentPath;


    @PostConstruct
    public void init() {
        if (path == null) {
            path = "/release-note-app/";
        }
        CURRENT_PATH.set(path);
        currentPath = path;
        if (!currentPath.endsWith(SEP)) {
            currentPath = currentPath + SEP;
        }
        if (contextPath == null) {
            contextPath = "";
        } else if (contextPath.endsWith(SEP)) {
            contextPath = contextPath.substring(0, contextPath.length() - 1);
        }
    }


    // =========================================================================
    // BEANS
    // =========================================================================
    @Bean
    public SkipIologOnRelease skipIologOnRelease(@Value("${server.servlet.context-path:#{null}}") final String currentContextPath,
                                                 @Value("${inugami.release.note.path:#{null}}") final String releaseNotePath) {
        return new SkipIologOnRelease(currentContextPath, releaseNotePath);
    }

    @Override
    public void addResourceHandlers(final ResourceHandlerRegistry registry) {
        if (enabled) {
            registry.addResourceHandler(currentPath + "**")
                    .addResourceLocations("classpath:/META-INF/resources/release-note-app/")
                    .resourceChain(true)
                    .addResolver(new PathResourceResolver());
        }
    }

    @Bean
    public ServletRegistrationBean<InugamiServlet> indexHtmlBean() {
        log.info("release note exposed : {}", contextPath + currentPath);
        final String basePath = contextPath + currentPath.substring(0, currentPath.length() - 1);
        final ServletRegistrationBean<InugamiServlet> bean = new ServletRegistrationBean<>(
                InugamiServlet.builder()
                              .contextPath(basePath)
                              .htmlBasePath(htmlBasePath)
                              .customCss(customCss)
                              .build(),
                currentPath, currentPath + "index.html");
        bean.setLoadOnStartup(1);
        return bean;
    }

    @ConditionalOnProperty(value = "inugami.release.note.custom.css.enabled", havingValue = "true")
    @Bean
    public ServletRegistrationBean<BasicRessourceServlet> customCssServlet() {
        final String basePath = contextPath + currentPath.substring(0, currentPath.length() - 1);
        final ServletRegistrationBean<BasicRessourceServlet> bean = new ServletRegistrationBean<>(
                BasicRessourceServlet.builder()
                                     .mediaType("text/css")
                                     .resourcePath(customCss)
                                     .build(),
                currentPath + "css/custom.css");
        bean.setLoadOnStartup(1);
        return bean;
    }

    @Bean
    public ServletRegistrationBean<PluginsModuleServlet> pluginsModulesBean() {
        final ServletRegistrationBean<PluginsModuleServlet> bean = new ServletRegistrationBean<>(
                new PluginsModuleServlet(), currentPath + "app/modules/plugins.module.ts");
        bean.setLoadOnStartup(1);
        return bean;
    }

    @Bean
    public ServletRegistrationBean<ReleaseNoteServlet> releaseNoteDataBean() {
        final ServletRegistrationBean<ReleaseNoteServlet> bean = new ServletRegistrationBean<>(
                new ReleaseNoteServlet(artifactName), currentPath + "data/release-notes.json");
        bean.setLoadOnStartup(1);
        return bean;
    }

    @Bean
    public ServletRegistrationBean<DependenciesCheckServlet> dependenciesCheckData(@Autowired(required = false) final DependenciesCheckService dependenciesCheckService) {
        final ServletRegistrationBean<DependenciesCheckServlet> bean = new ServletRegistrationBean<>(
                new DependenciesCheckServlet(dependenciesCheckService), currentPath + "data/dependencies-check.json");
        bean.setLoadOnStartup(1);
        return bean;
    }
}
