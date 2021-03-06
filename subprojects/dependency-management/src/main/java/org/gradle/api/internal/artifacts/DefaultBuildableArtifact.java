/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gradle.api.internal.artifacts;

import org.gradle.api.Buildable;
import org.gradle.api.artifacts.ResolvedModuleVersion;
import org.gradle.api.tasks.TaskDependency;
import org.gradle.internal.Factory;
import org.gradle.internal.component.model.IvyArtifactName;

import java.io.File;

public class DefaultBuildableArtifact extends DefaultResolvedArtifact implements Buildable {
    private final Buildable buildableArtifact;

    public DefaultBuildableArtifact(ResolvedModuleVersion owner, IvyArtifactName artifact, Factory<File> artifactSource, Buildable buildableArtifact) {
        super(owner, artifact, artifactSource);
        this.buildableArtifact = buildableArtifact;
    }

    @Override
    public TaskDependency getBuildDependencies() {
        return buildableArtifact.getBuildDependencies();
    }
}
