package org.apache.maven.plugins.dependency.resolvers;

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
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.dependency.fromDependencies.AbstractDependencyFilterMojo;
import org.apache.maven.plugins.dependency.utils.DependencyUtil;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.shared.artifact.filter.collection.ArtifactIdFilter;
import org.apache.maven.shared.artifact.filter.collection.ClassifierFilter;
import org.apache.maven.shared.artifact.filter.collection.FilterArtifacts;
import org.apache.maven.shared.artifact.filter.collection.GroupIdFilter;
import org.apache.maven.shared.artifact.filter.collection.TypeFilter;
import org.apache.maven.shared.artifact.resolve.ArtifactResult;
import org.apache.maven.shared.dependencies.DependableCoordinate;
import org.apache.maven.shared.dependencies.resolve.DependencyResolverException;

/**
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 * @version $Id: AbstractResolveMojo.java 1791544 2017-04-15 20:07:11Z michaelo $
 */
public abstract class AbstractResolveMojo
    extends AbstractDependencyFilterMojo
{
    /**
     * If specified, this parameter will cause the dependencies to be written to the path specified, instead of writing
     * to the console.
     *
     * @since 2.0
     */
    @Parameter( property = "outputFile" )
    protected File outputFile;

    /**
     * This method resolves the dependency artifacts from the project.
     *
     * @param theProject
     *            The POM.
     * @return resolved set of dependency artifacts.
     *
     * @throws ArtifactResolutionException
     * @throws ArtifactNotFoundException
     * @throws InvalidDependencyVersionException
     */

    /**
     * Whether to append outputs into the output file or overwrite it.
     *
     * @since 2.2
     */
    @Parameter( property = "appendOutput", defaultValue = "false" )
    protected boolean appendOutput;

    /**
     * Don't resolve plugins that are in the current reactor.
     * Only works for plugins at the moment.
     *
     * @since 2.7
     */
    @Parameter( property = "excludeReactor", defaultValue = "true" )
    protected boolean excludeReactor;

    /**
     * <i>not used in this goal</i>
     */
    @Parameter
    protected boolean useJvmChmod = true;

    /**
     * <i>not used in this goal</i>
     */
    @Parameter
    protected boolean ignorePermissions;

    protected FilterArtifacts getPluginArtifactsFilter()
    {
        if ( excludeReactor )
        {
            final StringBuilder exAids = new StringBuilder();
            if ( this.excludeArtifactIds != null )
            {
                exAids.append( this.excludeArtifactIds );
            }

            for ( final MavenProject rp : reactorProjects )
            {
                if ( !"maven-plugin".equals( rp.getPackaging() ) )
                {
                    continue;
                }

                if ( exAids.length() > 0 )
                {
                    exAids.append( "," );
                }

                exAids.append( rp.getArtifactId() );
            }

            this.excludeArtifactIds = exAids.toString();
        }

        final FilterArtifacts filter = new FilterArtifacts();

        filter.addFilter( new org.apache.maven.shared.artifact.filter.collection.ScopeFilter(
                                                  DependencyUtil.cleanToBeTokenizedString( this.includeScope ),
                                                  DependencyUtil.cleanToBeTokenizedString( this.excludeScope ) ) );

        filter.addFilter( new TypeFilter( DependencyUtil.cleanToBeTokenizedString( this.includeTypes ),
                                          DependencyUtil.cleanToBeTokenizedString( this.excludeTypes ) ) );

        filter.addFilter( new ClassifierFilter( DependencyUtil.cleanToBeTokenizedString( this.includeClassifiers ),
                                                DependencyUtil.cleanToBeTokenizedString( this.excludeClassifiers ) ) );

        filter.addFilter( new GroupIdFilter( DependencyUtil.cleanToBeTokenizedString( this.includeGroupIds ),
                                             DependencyUtil.cleanToBeTokenizedString( this.excludeGroupIds ) ) );

        filter.addFilter( new ArtifactIdFilter( DependencyUtil.cleanToBeTokenizedString( this.includeArtifactIds ),
                                                DependencyUtil.cleanToBeTokenizedString( this.excludeArtifactIds ) ) );

        return filter;
    }

    /**
     * This method resolves all transitive dependencies of an artifact.
     *
     * @param artifact the artifact used to retrieve dependencies
     * @return resolved set of dependencies
     * @throws DependencyResolverException
     */
    protected Set<Artifact> resolveArtifactDependencies( final DependableCoordinate artifact )
        throws DependencyResolverException
    {
        ProjectBuildingRequest buildingRequest = newResolveArtifactProjectBuildingRequest();

        Iterable<ArtifactResult> artifactResults =
            getDependencyResolver().resolveDependencies( buildingRequest, artifact, null );

        Set<Artifact> artifacts = new LinkedHashSet<Artifact>();

        for ( final ArtifactResult artifactResult : artifactResults )
        {
            artifacts.add( artifactResult.getArtifact() );
        }

        return artifacts;

    }
}
