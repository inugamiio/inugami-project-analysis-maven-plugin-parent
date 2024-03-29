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

import io.inugami.api.loggers.Loggers;
import io.inugami.api.models.data.basic.JsonObject;
import io.inugami.api.processors.ConfigHandler;
import io.inugami.api.spi.SpiLoader;
import io.inugami.api.tools.ConsoleColors;
import io.inugami.commons.files.FilesUtils;
import io.inugami.configuration.services.ConfigHandlerHashMap;
import io.inugami.maven.plugin.analysis.api.actions.*;
import io.inugami.maven.plugin.analysis.api.constant.Constants;
import io.inugami.maven.plugin.analysis.api.models.Gav;
import io.inugami.maven.plugin.analysis.api.models.ScanConext;
import io.inugami.maven.plugin.analysis.api.scan.issue.tracker.IssueTrackerProvider;
import io.inugami.maven.plugin.analysis.api.services.neo4j.Neo4jDao;
import io.inugami.maven.plugin.analysis.api.utils.reflection.ReflectionService;
import io.inugami.maven.plugin.analysis.plugin.services.ArtifactResolverListener;
import io.inugami.maven.plugin.analysis.plugin.services.ScanService;
import io.inugami.maven.plugin.analysis.plugin.services.WriterService;
import io.inugami.maven.plugin.analysis.plugin.services.neo4j.DefaultNeo4jDao;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;
import org.apache.maven.settings.Settings;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.impl.ArtifactResolver;
import org.eclipse.aether.repository.RemoteRepository;
import org.sonatype.plexus.components.sec.dispatcher.DefaultSecDispatcher;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcher;
import org.xeustechnologies.jcl.JarClassLoader;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static io.inugami.api.exceptions.Asserts.assertNotEmpty;

@SuppressWarnings({"java:S1854"})
@Slf4j
@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PUBLIC)
@Mojo(name = "check", defaultPhase = LifecyclePhase.INSTALL)
public class CheckMojo extends AbstractMojo {


    // =========================================================================
    // ATTRIBUTES
    // =========================================================================
    @Parameter
    private Set<String> resourcePackages;

    @Parameter
    private Set<String> excludedJars;

    @Parameter(defaultValue = "${project.basedir}", readonly = true)
    private File basedir;

    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject project;

    @Component
    private RepositorySystem repoSystem;

    @Parameter(defaultValue = "${repositorySystemSession}", readonly = true, required = true)
    private RepositorySystemSession repoSession;

    @Parameter(defaultValue = "${project.remoteProjectRepositories}", readonly = true, required = true)
    private List<RemoteRepository> repositories;

    @Component
    private PluginDescriptor descriptor;

    @Parameter(defaultValue = "${localRepository}", readonly = true, required = true)
    private ArtifactRepository artifactRepository;

    @Component
    private ArtifactResolver artifactResolver;

    @Component
    private PluginDescriptor pluginDescriptor;

    @Component
    private ArtifactHandler artifactHandler;

    @Parameter(defaultValue = "${settings}", readonly = true, required = true)
    private Settings settings;

    @Component
    private SecDispatcher secDispatcher;

    private List<Pattern> excluded;

    // =========================================================================
    // API
    // =========================================================================
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        log.info("scan project : {}:{}:{}:", project.getGroupId(), project.getArtifactId(), project.getVersion());
        log.info("building scan context");
        initExcluded();
        final ArtifactResolverListener listener = new ArtifactResolverListener();
        final ScanConext               context  = buildContext(listener);

        try {
            preAnalyze(context);
            final List<JsonObject> result = analyze(context);
            writeResult(context, result);
            postAnalyse(context, result);
            log.info(ConsoleColors.renderState(ConsoleColors.State.SUCCESS, "scan complete"));
        } catch (final Throwable error) {
            log.error(error.getMessage(), error);
            log.info(ConsoleColors.renderState(ConsoleColors.State.ERROR, "scan complete with error"));
            throw new MojoFailureException(error.getMessage(), error);
        } finally {
            if (context.getNeo4jDao() != null) {
                context.getNeo4jDao().shutdown();
            }
        }
    }

    protected void initExcluded() {
        final Set<String> excludedPattern = new LinkedHashSet<>();
        if (excludedJars != null) {
            excludedPattern.addAll(excludedJars);
        }
        excludedPattern.add(".*spring-web.*");
        excludedPattern.add(".*spring-context.*");
        excludedPattern.add(".*spring-cloud-starter-openfeign.*");
        excludedPattern.add(".*javax.servlet-api.*");
        excludedPattern.add(".*swagger-annotations.*");
        excludedPattern.add(".*spring-jms.*");
        excludedPattern.add(".*spring-rabbit.*");

        excluded = new ArrayList<>();
        for (final String pattern : excludedPattern) {
            excluded.add(Pattern.compile(pattern, Pattern.CASE_INSENSITIVE));
        }
    }


    // =========================================================================
    // PROCESS
    // =========================================================================
    private void preAnalyze(final ScanConext context) {
        log.info("launching pre analyze ...");
        final List<ProjectPreAnalyzer> scanners = SpiLoader.getInstance()
                                                           .loadSpiServicesByPriority(ProjectPreAnalyzer.class);

        for (final ProjectPreAnalyzer analyzer : scanners) {
            if (analyzer.accept(context)) {
                try {
                    analyzer.preAnalyze(context);
                } finally {
                    analyzer.shutdown();
                }
            }
        }
        log.info(ConsoleColors.renderState(ConsoleColors.State.SUCCESS, "pre analyze done"));
    }

    private List<JsonObject> analyze(final ScanConext context) {
        log.info("launching scan ...");
        final List<JsonObject>     result;
        final List<ProjectScanner> scanners = SpiLoader.getInstance().loadSpiServicesByPriority(ProjectScanner.class);
        result = new ScanService(context, scanners).scan();
        log.info(ConsoleColors.renderState(ConsoleColors.State.SUCCESS, "scan done"));
        return result;
    }

    private void writeResult(final ScanConext context, final List<JsonObject> result) {
        log.info("launching writing result ...");
        final List<ResultWriter> writers = SpiLoader.getInstance().loadSpiServicesByPriority(ResultWriter.class);
        writers.forEach(writer -> writer.init(context));
        new WriterService(result, context, writers).write();
        writers.forEach(writer -> writer.shutdown(context));
        log.info(ConsoleColors.renderState(ConsoleColors.State.SUCCESS, "write result done"));
    }


    private void postAnalyse(final ScanConext context, final List<JsonObject> result) {
        log.info("launching pre analyze ...");
        final List<ProjectPostAnalyzer> scanners = SpiLoader.getInstance()
                                                            .loadSpiServicesByPriority(ProjectPostAnalyzer.class);

        for (final ProjectPostAnalyzer analyzer : scanners) {
            if (analyzer.accept(context)) {
                try {
                    analyzer.postAnalyze(context, result);
                } finally {
                    analyzer.shutdown();
                }
            }
        }
        log.info(ConsoleColors.renderState(ConsoleColors.State.SUCCESS, "post analyze done"));
    }

    // =========================================================================
    // PRIVATE
    // =========================================================================
    private ScanConext buildContext(final ArtifactResolverListener listener) throws MojoExecutionException {

        final ConfigHandler<String, String> configuration = new ConfigHandlerHashMap();
        configuration.putAll(extractProperties(project.getProperties()));
        configuration.putAll(extractProperties(System.getProperties()));
        configuration.put(Constants.PROJECT_BASE_DIR, project.getBasedir().getAbsolutePath());
        configuration.put(Constants.PROJECT_BUILD_DIR, FilesUtils.buildFile(project.getBasedir(), "target")
                                                                 .getAbsolutePath());
        configuration.put(Constants.INTERACTIVE, "false");
        initProperties(configuration);

        final Neo4jDao neo4jDao = new DefaultNeo4jDao(configuration);
        return ScanConext.builder()
                         .basedir(basedir)
                         .project(project)
                         .repoSystem(repoSystem)
                         .repoSession(repoSession)
                         .repositories(repositories)
                         .classLoader(buildClassloader(listener, configuration))
                         .dependencies(listener.getArtifacts())
                         .directDependencies(extractDirectDependencies())
                         .pluginDescriptor(pluginDescriptor)
                         .configuration(configuration)
                         .secDispatcher(secDispatcher)
                         .artifactHandler(artifactHandler)
                         .settings(settings)
                         .neo4jDao(neo4jDao)
                         .build();
    }

    private Map<String, String> extractProperties(final Properties properties) {
        final Map<String, String> result = new LinkedHashMap<>();

        if (properties != null) {
            for (final Map.Entry<Object, Object> entry : properties.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    result.put(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
                }
            }
        }
        return result;
    }

    private void initProperties(final ConfigHandler<String, String> configuration) {

        if (project.getIssueManagement() != null) {
            assertNotEmpty("no issue management system defined!", project.getIssueManagement().getSystem());
            assertNotEmpty("no issue management url defined!", project.getIssueManagement().getUrl());
            configuration.put(IssueTrackerProvider.SYSTEM, project.getIssueManagement().getSystem());
            configuration.put(IssueTrackerProvider.URL, project.getIssueManagement().getUrl());
        }
        if (secDispatcher instanceof DefaultSecDispatcher) {
            final String securityPath = configuration.getOrDefault("settings.security",
                                                                   new File(System.getProperty("user.home")
                                                                                    + File.separator
                                                                                    + ".m2/settings-security.xml")
                                                                           .getAbsolutePath());

            ((DefaultSecDispatcher) secDispatcher).setConfigurationFile(securityPath);
        }

        final List<PropertiesInitialization> propertiesInitializers = SpiLoader.getInstance()
                                                                               .loadSpiServicesByPriority(PropertiesInitialization.class);
        for (final PropertiesInitialization propsInitializer : propertiesInitializers) {
            propsInitializer.initialize(configuration, project, settings, secDispatcher);
        }
    }

    private Set<Gav> extractDirectDependencies() {
        final List<Gav> dependenciesGav = Optional.ofNullable((List<Dependency>) project.getDependencies())
                                                  .orElse(new ArrayList<>())
                                                  .stream()
                                                  .map(this::convertToGav)
                                                  .collect(Collectors.toList());
        return new LinkedHashSet<>(dependenciesGav);
    }

    private Gav convertToGav(final Dependency dependency) {
        return Gav.builder()
                  .groupId(dependency.getGroupId())
                  .artifactId(dependency.getArtifactId())
                  .version(dependency.getVersion())
                  .type(dependency.getType())
                  .scope(dependency.getScope())
                  .build();
    }


    private JarClassLoader buildClassloader(final ArtifactResolverListener listener,
                                            final ConfigHandler<String, String> configuration) throws MojoExecutionException {
        try {

            final JarClassLoader dependenciesClassLoader = buildDependenciesClassLoader(listener);
            final Set<String>    dependencies            = new LinkedHashSet<>(getDependentClasspathElements());
            
            for (final String dependency : dependencies) {
                dependenciesClassLoader.add(Paths.get(dependency).toUri().toURL());
            }

            final JarClassLoader currentClassloader = new JarClassLoader(dependenciesClassLoader);
            currentClassloader.add(project.getBuild().getOutputDirectory());

            final String additionalFolders = configuration
                    .get("inugami.maven.plugin.analysis.additional.output.folders");
            if (additionalFolders != null) {
                for (final String additionalPath : additionalFolders.split(";")) {
                    currentClassloader.add(new File(additionalPath.trim()).getAbsoluteFile().toURI().toURL());
                }
            }
            ReflectionService.initializeClassloader(currentClassloader);
            return currentClassloader;
        } catch (final MalformedURLException e) {
            throw new MojoExecutionException("Unable to create class loader with compiled classes", e);
        } catch (final DependencyResolutionRequiredException e) {
            throw new MojoExecutionException("Dependency resolution (runtime + compile) is required");
        }
    }

    private JarClassLoader buildDependenciesClassLoader(final ArtifactResolverListener listener) {
        final JarClassLoader result = new JarClassLoader();


        final Set<Artifact> artifacts = repoSystem.resolve(
                new ArtifactResolutionRequest()
                        .setArtifact(this.project.getArtifact())
                        .setLocalRepository(artifactRepository)
                        .setRemoteRepositories(this.project.getRemoteArtifactRepositories())
                        .setResolveTransitively(true)
                        .addListener(listener)).getArtifacts();

        for (final Artifact artifact : artifacts) {
            if (isExcludedJar(artifact)) {
                continue;
            }
            try {
                final URL url = artifact.getFile().toURI().toURL();
                Loggers.DEBUG.info("load jar : {}", url);
                result.add(url);
            } catch (final Exception e) {
                if (Loggers.DEBUG.isDebugEnabled()) {
                    Loggers.DEBUG.error(e.getMessage(), e);
                }
            }
        }
        return result;
    }

    protected boolean isExcludedJar(final Artifact artifact) {
        final String gav = String.join(":", artifact.getGroupId(), artifact.getArtifactId(), artifact.getType(), artifact.getVersion());
        for (final Pattern regex : excluded) {
            if (regex.matcher(gav).matches()) {
                return true;
            }
        }
        return false;
    }


    private Collection<String> getDependentClasspathElements() throws DependencyResolutionRequiredException {
        final Set<String> dependencies = new LinkedHashSet<>();
        appendIfNotNull(project.getCompileClasspathElements(), dependencies::addAll);
        appendIfNotNull(project.getRuntimeClasspathElements(), dependencies::addAll);
        appendIfNotNull(project.getSystemClasspathElements(), dependencies::addAll);
        return dependencies;
    }


    // =========================================================================
    // UTILITY TOOLS
    // =========================================================================
    private void appendIfNotNull(final Collection<String> values, final Consumer<Collection<String>> consumer) {
        if (values != null) {
            consumer.accept(values);
        }
    }
}
