package org.apache.maven.plugins.dependency.utils.translators;

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

import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.shared.artifact.ArtifactCoordinate;

/**
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 * @version $Id: ArtifactTranslator.java 1694794 2015-08-08 12:36:03Z rfscholte $
 */
public interface ArtifactTranslator
{
    Set<ArtifactCoordinate> translate( Set<Artifact> artifacts, Log log );
}
