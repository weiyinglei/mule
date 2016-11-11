/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.runner.api;

import java.util.List;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.graph.Dependency;

/**
 * Strategy to classify a {@link ClassPathClassifierContext} for a root {@link Artifact} and its {@link List} of direct {@link Dependency}.
 *
 * @since 4.0
 */
public interface ArtifactClassifier<T extends ArtifactClassification> {

  /**
   * Does the classification and generates the {@link T} classification result.
   *
   * @param dependencyResolver {@link DependencyResolver} to resolve dependencies
   * @param context {@link ClassPathClassifierContext} with settings for the classification process
   * @param directDependencies {@link List} of {@link Dependency} with direct dependencies for the rootArtifact
   * @param rootArtifact {@link ArtifactClassificationType} for rootArtifact
   * @param rootArtifactType {@link ArtifactClassificationType} defines the type of root artifact
   * @return the result of the classification {@link ArtifactClassification}
   */
  List<T> classify(DependencyResolver dependencyResolver, ClassPathClassifierContext context, List<Dependency> directDependencies, Artifact rootArtifact, ArtifactClassificationType rootArtifactType);
}
