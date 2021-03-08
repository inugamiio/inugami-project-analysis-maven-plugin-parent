package org.apache.maven.plugins.dependency;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.dependencies.resolve.DependencyResolver;
import org.apache.maven.shared.dependencies.resolve.DependencyResolverException;
import org.apache.maven.shared.artifact.filter.resolve.AbstractFilter;
import org.apache.maven.shared.artifact.filter.resolve.AndFilter;
import org.apache.maven.shared.artifact.filter.resolve.Node;
import org.apache.maven.shared.artifact.filter.resolve.PatternExclusionsFilter;
import org.apache.maven.shared.artifact.filter.resolve.PatternInclusionsFilter;
import org.apache.maven.shared.artifact.filter.resolve.ScopeFilter;
import org.apache.maven.shared.artifact.filter.resolve.TransformableFilter;
import org.apache.maven.shared.artifact.filter.resolve.transform.ArtifactIncludeFilterTransformer;
import org.apache.maven.shared.artifact.resolve.ArtifactResolver;
import org.apache.maven.shared.artifact.resolve.ArtifactResolverException;
import org.apache.maven.shared.artifact.resolve.ArtifactResult;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringUtils;

/**
 * When run on a project, remove the project dependencies from the local repository, and optionally re-resolve them.
 * Outside of a project, remove the manually given dependencies.
 *
 * @author jdcasey
 * @version $Id: PurgeLocalRepositoryMojo.java 1791694 2017-04-17 15:56:06Z hboutemy $
 * @since 2.0
 */
@Mojo( name = "purge-local-repository", threadSafe = true, requiresProject = false )
public class PurgeLocalRepositoryMojo
    extends AbstractMojo
{

    public static final String FILE_FUZZINESS = "file";

    public static final String VERSION_FUZZINESS = "version";

    public static final String ARTIFACT_ID_FUZZINESS = "artifactId";

    public static final String GROUP_ID_FUZZINESS = "groupId";

    /**
     * The current Maven project.
     */
    @Parameter( defaultValue = "${project}", readonly = true, required = true )
    private MavenProject project;

    @Parameter( defaultValue = "${session}", readonly = true, required = true )
    private MavenSession session;

    /**
     * The list of dependencies in the form of groupId:artifactId which should BE deleted/purged from the local
     * repository. Note that using this parameter will deactivate the normal process for purging the current project
     * dependency tree. If this parameter is used, only the included artifacts will be purged. The manualIncludes
     * parameter should not be used in combination with the includes/excludes parameters.
     *
     * @since 2.6
     */
    @Parameter
    private List<String> manualIncludes;

    /**
     * Comma-separated list of groupId:artifactId entries, which should be used to manually include artifacts for
     * deletion. This is a command-line alternative to the <code>manualIncludes</code> parameter, since List parameters
     * are not currently compatible with CLI specification.
     *
     * @since 2.6
     */
    @Parameter( property = "manualInclude" )
    private String manualInclude;

    /**
     * The list of dependencies in the form of groupId:artifactId which should BE deleted/refreshed.
     *
     * @since 2.6
     */
    @Parameter
    private List<String> includes;

    /**
     * Comma-separated list of groupId:artifactId entries, which should be used to include artifacts for
     * deletion/refresh. This is a command-line alternative to the <code>includes</code> parameter, since List
     * parameters are not currently compatible with CLI specification.
     *
     * @since 2.6
     */
    @Parameter( property = "include" )
    private String include;

    /**
     * The list of dependencies in the form of groupId:artifactId which should NOT be deleted/refreshed.
     */
    @Parameter
    private List<String> excludes;

    /**
     * Comma-separated list of groupId:artifactId entries, which should be used to exclude artifacts from
     * deletion/refresh. This is a command-line alternative to the <code>excludes</code> parameter, since List
     * parameters are not currently compatible with CLI specification.
     */
    @Parameter( property = "exclude" )
    private String exclude;

    /**
     * Whether to re-resolve the artifacts once they have been deleted from the local repository. If you are running
     * this mojo from the command-line, you may want to disable this. By default, artifacts will be re-resolved.
     */
    @Parameter( property = "reResolve", defaultValue = "true" )
    private boolean reResolve;

    /**
     * The local repository, from which to delete artifacts.
     */
    @Parameter( defaultValue = "${localRepository}", readonly = true, required = true )
    private ArtifactRepository localRepository;

    /**
     * The dependency resolver
     */
    @Component
    private DependencyResolver dependencyResolver;

    /**
     * The artifact resolver used to re-resolve dependencies, if that option is enabled.
     */
    @Component
    private ArtifactResolver artifactResolver;

    /**
     * Determines how liberally the plugin will delete an artifact from the local repository. Values are: <br/>
     * <ul>
     * <li><b>file</b> - Eliminate only the artifact's file.</li>
     * <li><b>version</b> <i>(default)</i> - Eliminate all files associated with the version of the artifact.</li>
     * <li><b>artifactId</b> - Eliminate all files associated with the artifact's artifactId.</li>
     * <li><b>groupId</b> - Eliminate all files associated with the artifact's groupId.</li>
     * </ul>
     */
    @Parameter( property = "resolutionFuzziness", defaultValue = "version" )
    private String resolutionFuzziness;

    /**
     * Whether this mojo should act on all transitive dependencies. Default value is true.
     */
    @Parameter( property = "actTransitively", defaultValue = "true" )
    private boolean actTransitively;

    /**
     * Whether this plugin should output verbose messages. Default is false.
     */
    @Parameter( property = "verbose", defaultValue = "false" )
    private boolean verbose;

    /**
     * Whether to purge only snapshot artifacts.
     *
     * @since 2.4
     */
    @Parameter( property = "snapshotsOnly", defaultValue = "false" )
    private boolean snapshotsOnly;

    /**
     * Skip plugin execution completely.
     *
     * @since 2.7
     */
    @Parameter( property = "skip", defaultValue = "false" )
    private boolean skip;

    /**
     * Includes only direct project dependencies.
     */
    private class DirectDependencyFilter
        extends AbstractFilter
    {

        private Artifact projectArtifact;

        private Set<Artifact> directDependencyArtifacts;

        /**
         * Default constructor
         *
         * @param directDependencyArtifacts Set of Artifact objects which represent the direct dependencies of the
         *            project
         */
        public DirectDependencyFilter( Artifact projectArtifact, Set<Artifact> directDependencyArtifacts )
        {
            this.projectArtifact = projectArtifact;
            this.directDependencyArtifacts = directDependencyArtifacts;
        }

        @Override
        public boolean accept( Node node, List<Node> parents )
        {

            if ( artifactsGAMatch( node, projectArtifact ) )
            {
                return true;
            }
            for ( Artifact depArtifact : directDependencyArtifacts )
            {
                if ( this.artifactsGAMatch( node, depArtifact ) )
                {
                    return true;
                }
            }
            return false;
        }

        /*
         * Compare the groupId:artifactId of two artifacts.
         */
        private boolean artifactsGAMatch( Node node, Artifact artifact2 )
        {
            if ( node.getDependency() == null )
            {
                return false;
            }

            if ( !node.getDependency().getGroupId().equals( artifact2.getGroupId() ) )
            {
                getLog().debug( "Different groupId: " + node.getDependency() + "  " + artifact2 );
                return false;
            }
            if ( !node.getDependency().getArtifactId().equals( artifact2.getArtifactId() ) )
            {
                getLog().debug( "Different artifactId: " + node.getDependency() + "  " + artifact2 );
                return false;
            }
            return true;
        }
    }

    /**
     * Includes only snapshot artifacts
     */
    private class SnapshotsFilter extends AbstractFilter
    {
        @Override
        public boolean accept( Node node, List<Node> parents )
        {
            if ( node.getDependency() == null )
            {
                return false;
            }
            else
            {
                return ArtifactUtils.isSnapshot( node.getDependency().getVersion() );
            }
        }
    }

    @Override
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        if ( isSkip() )
        {
            getLog().info( "Skipping plugin execution" );
            return;
        }

        if ( !StringUtils.isEmpty( manualInclude ) )
        {
            manualIncludes = this.parseIncludes( manualInclude );
        }
        // If it's a manual purge, the only step is to delete from the local repo
        if ( manualIncludes != null && manualIncludes.size() > 0 )
        {
            manualPurge( manualIncludes );
            return;
        }

        Set<Artifact> dependencyArtifacts = project.getDependencyArtifacts();

        TransformableFilter dependencyFilter = createPurgeArtifactsFilter( dependencyArtifacts );

        Set<Artifact> resolvedArtifactsToPurge =
            getFilteredResolvedArtifacts( project, dependencyArtifacts, dependencyFilter );

        if ( resolvedArtifactsToPurge.isEmpty() )
        {
            getLog().info( "No artifacts included for purge for project: " + project.getId() );
            return;
        }

        verbose( "Purging dependencies for project: " + project.getId() );
        purgeArtifacts( resolvedArtifactsToPurge );

        if ( reResolve )
        {
            ArtifactFilter artifactFilter = dependencyFilter.transform( new ArtifactIncludeFilterTransformer() );
            try
            {
                reResolveArtifacts( project, resolvedArtifactsToPurge, artifactFilter );
            }
            catch ( ArtifactResolutionException e )
            {
                String failureMessage = "Failed to refresh project dependencies for: " + project.getId();
                MojoFailureException failure = new MojoFailureException( failureMessage );
                failure.initCause( e );

                throw failure;
            }
            catch ( ArtifactNotFoundException e )
            {
                String failureMessage = "Failed to refresh project dependencies for: " + project.getId();
                MojoFailureException failure = new MojoFailureException( failureMessage );
                failure.initCause( e );

                throw failure;
            }
        }
    }

    /**
     * Purge/Delete artifacts from the local repository according to the given patterns.
     *
     * @param inclusionPatterns
     * @throws MojoExecutionException
     */
    private void manualPurge( List<String> includes )
        throws MojoExecutionException
    {
        for ( String gavPattern : includes )
        {
            if ( StringUtils.isEmpty( gavPattern ) )
            {
                getLog().debug( "Skipping empty gav pattern: " + gavPattern );
                continue;
            }

            String relativePath = gavToPath( gavPattern );
            if ( StringUtils.isEmpty( relativePath ) )
            {
                continue;
            }

            File purgeDir = new File( localRepository.getBasedir(), relativePath );
            if ( purgeDir.exists() )
            {
                getLog().debug( "Deleting directory: " + purgeDir );
                try
                {
                    FileUtils.deleteDirectory( purgeDir );
                }
                catch ( IOException e )
                {
                    throw new MojoExecutionException( "Unable to purge directory: " + purgeDir );
                }
            }
        }
    }

    /**
     * Convert a groupId:artifactId:version to a file system path
     *
     * @param gav, the groupId:artifactId:version string
     * @return the corresponding path
     */
    private String gavToPath( String gav )
    {
        if ( StringUtils.isEmpty( gav ) )
        {
            return null;
        }

        String[] pathComponents = gav.split( ":" );

        StringBuilder path = new StringBuilder( pathComponents[0].replace( '.', '/' ) );

        for ( int i = 1; i < pathComponents.length; ++i )
        {
            path.append( "/" ).append( pathComponents[i] );
        }

        return path.toString();
    }

    /**
     * Create the includes exclude filter to use when resolving and purging dependencies Also excludes any "system"
     * scope dependencies
     *
     * @param dependencyArtifacts The dependency artifacts to use as a reference if we're excluding transitive
     *            dependencies
     * @return the created filter
     */
    private TransformableFilter createPurgeArtifactsFilter( Set<Artifact> dependencyArtifacts )
    {
        List<TransformableFilter> subFilters = new ArrayList<TransformableFilter>();

        // System dependencies should never be purged
        subFilters.add( ScopeFilter.excluding( Artifact.SCOPE_SYSTEM ) );

        if ( this.snapshotsOnly )
        {
            subFilters.add( new SnapshotsFilter() );
        }

        // The CLI includes/excludes overrides configuration in the pom
        if ( !StringUtils.isEmpty( this.include ) )
        {
            this.includes = parseIncludes( this.include );
        }
        if ( this.includes != null )
        {
            subFilters.add( new PatternInclusionsFilter( includes ) );
        }

        if ( !StringUtils.isEmpty( this.exclude ) )
        {
            this.excludes = parseIncludes( this.exclude );
        }
        if ( this.excludes != null )
        {
            subFilters.add( new PatternExclusionsFilter( excludes ) );
        }

        if ( !actTransitively )
        {
            subFilters.add( new DirectDependencyFilter( project.getArtifact(), dependencyArtifacts ) );
        }

        return new AndFilter( subFilters );
    }

    /**
     * Convert comma separated list of includes to List object
     *
     * @param include
     * @return the includes list
     */
    private List<String> parseIncludes( String include )
    {
        List<String> includes = new ArrayList<String>();

        if ( include != null )
        {
            String[] elements = include.split( "," );
            includes.addAll( Arrays.asList( elements ) );
        }

        return includes;
    }

    private Set<Artifact> getFilteredResolvedArtifacts( MavenProject project, Set<Artifact> artifacts,
                                                        TransformableFilter filter )
    {
        try
        {
            Iterable<ArtifactResult> results =
                dependencyResolver.resolveDependencies( session.getProjectBuildingRequest(),
                                                        project.getModel(), filter );

            Set<Artifact> resolvedArtifacts = new LinkedHashSet<Artifact>();

            for ( ArtifactResult artResult : results )
            {
                resolvedArtifacts.add( artResult.getArtifact() );
            }

            return resolvedArtifacts;
        }
        catch ( DependencyResolverException e )
        {
            getLog().info( "Unable to resolve all dependencies for : " + project.getGroupId() + ":"
                               + project.getArtifactId() + ":" + project.getVersion()
                               + ". Falling back to non-transitive mode for initial artifact resolution." );
        }

        Set<Artifact> resolvedArtifacts = new LinkedHashSet<Artifact>();

        ArtifactFilter artifactFilter = filter.transform( new ArtifactIncludeFilterTransformer() );

        // Resolve the only poms here instead of the actual artifacts, because the files will be deleted during the
        // purge anyway
        for ( Artifact artifact : artifacts )
        {
            if ( artifactFilter.include( artifact ) )
            {
                try
                {
                    ArtifactResult artifactResult =
                        artifactResolver.resolveArtifact( session.getProjectBuildingRequest(), artifact );

                    resolvedArtifacts.add( artifactResult.getArtifact() );
                }
                catch ( ArtifactResolverException e )
                {
                    getLog().debug( "Unable to resolve artifact: " + artifact );
                }
            }
        }
        return resolvedArtifacts;
    }

    private void purgeArtifacts( Set<Artifact> artifacts )
        throws MojoFailureException
    {
        for ( Artifact artifact : artifacts )
        {
            verbose( "Purging artifact: " + artifact.getId() );

            File deleteTarget = findDeleteTarget( artifact );

            verbose( "Deleting: " + deleteTarget );

            if ( deleteTarget.isDirectory() )
            {
                try
                {
                    FileUtils.deleteDirectory( deleteTarget );
                }
                catch ( IOException e )
                {
                    getLog().warn( "Unable to purge local repository location: " + deleteTarget, e );
                }
            }
            else
            {
                if ( !deleteTarget.delete() )
                {
                    deleteTarget.deleteOnExit();
                    getLog().warn( "Unable to purge local repository location immediately: " + deleteTarget );
                }
            }
            artifact.setResolved( false );
        }
    }

    private void reResolveArtifacts( MavenProject project, Set<Artifact> artifacts, ArtifactFilter filter )
        throws ArtifactResolutionException, ArtifactNotFoundException
    {

        // Always need to re-resolve the poms in case they were purged along with the artifact
        // because Maven 2 will not automatically re-resolve them when resolving the artifact
        for ( Artifact artifact : artifacts )
        {
            try
            {
                artifactResolver.resolveArtifact( session.getProjectBuildingRequest(),
                                     org.apache.maven.shared.artifact.TransferUtils.toArtifactCoordinate( artifact ) );
            }
            catch ( ArtifactResolverException e )
            {
                verbose( e.getMessage() );
            }
        }

        List<Artifact> missingArtifacts = new ArrayList<Artifact>();

        for ( Artifact artifact : artifacts )
        {
            verbose( "Resolving artifact: " + artifact.getId() );

            try
            {
                artifactResolver.resolveArtifact( session.getProjectBuildingRequest(), artifact );
            }
            catch ( ArtifactResolverException e )
            {
                verbose( e.getMessage() );
                missingArtifacts.add( artifact );
            }
        }

        if ( missingArtifacts.size() > 0 )
        {
            StringBuffer message = new StringBuffer( "required artifacts missing:\n" );
            for ( Artifact missingArtifact : missingArtifacts )
            {
                message.append( "  " ).append( missingArtifact.getId() ).append( '\n' );
            }
            message.append( "\nfor the artifact:" );

            throw new ArtifactResolutionException( message.toString(), project.getArtifact(),
                                                   project.getRemoteArtifactRepositories() );
        }
    }

    private File findDeleteTarget( Artifact artifact )
    {
        // Use localRepository.pathOf() in case artifact.getFile() is not set
        File deleteTarget = new File( localRepository.getBasedir(), localRepository.pathOf( artifact ) );

        if ( GROUP_ID_FUZZINESS.equals( resolutionFuzziness ) )
        {
            // get the groupId dir.
            deleteTarget = deleteTarget.getParentFile().getParentFile().getParentFile();
        }
        else if ( ARTIFACT_ID_FUZZINESS.equals( resolutionFuzziness ) )
        {
            // get the artifactId dir.
            deleteTarget = deleteTarget.getParentFile().getParentFile();
        }
        else if ( VERSION_FUZZINESS.equals( resolutionFuzziness ) )
        {
            // get the version dir.
            deleteTarget = deleteTarget.getParentFile();
        }
        // else it's file fuzziness.
        return deleteTarget;
    }

    private void verbose( String message )
    {
        if ( verbose || getLog().isDebugEnabled() )
        {
            getLog().info( message );
        }
    }

    public boolean isSkip()
    {
        return skip;
    }

    public void setSkip( boolean skip )
    {
        this.skip = skip;
    }
}
