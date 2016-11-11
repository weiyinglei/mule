/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.runner.api;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.stream.Collectors.toList;
import static org.eclipse.aether.util.artifact.ArtifactIdUtils.toId;
import static org.eclipse.aether.util.artifact.JavaScopes.COMPILE;
import static org.mule.test.runner.utils.FileUtils.toUrl;

import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.resolution.ArtifactDescriptorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Collects {@link URL}s for a root artifact dependencies filtered by {@link #dependencyFilter} and for each direct
 * dependencies selected by {@link #directDependenciesFilter} does the same and adds the dependency to the {@link ArtifactClassificationNode}.
 *
 * @since 4.0
 */
public class ArtifactClassificationNodeCollector {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private DependencyResolver dependencyResolver;
  private Artifact rootArtifact;
  private List<Dependency> rootArtifactDirectDependencies;
  private DependencyFilter dependencyFilter;
  private Predicate<Dependency> directDependenciesFilter;

  /**
   * Sets the root artifact to start collecting the dependencies.
   *
   * @param rootArtifact {@link Artifact} to begin collecting dependencies.
   */
  public void setRootArtifact(Artifact rootArtifact) {
    this.rootArtifact = rootArtifact;
  }

  /**
   * Sets the root artifact direct dependencies to check if that any recursive dependency analised is declared initially in the
   * root node.
   *
   * @param rootArtifactDirectDependencies root artifact direct dependencies to check if that any recursive dependency analised is declared initially in the
   * root node.
   */
  public void setRootArtifactDirectDependencies(List<Dependency> rootArtifactDirectDependencies) {
    this.rootArtifactDirectDependencies = rootArtifactDirectDependencies;
  }

  /**
   * Filter for dependencies.
   *
   * @param dependencyFilter filters the dependencies.
   */
  public void setDependencyFilter(DependencyFilter dependencyFilter) {
    this.dependencyFilter = dependencyFilter;
  }

  /**
   * Resolver for Maven dependencies.
   *
   * @param dependencyResolver the {@link DependencyResolver} that knows how to resolve dependencies.
   */
  public void setDependencyResolver(DependencyResolver dependencyResolver) {
    this.dependencyResolver = dependencyResolver;
  }

  /**
   * {@link Predicate} that defines which direct dependency from the artifact processed should be used to obtain its dependencies and build a node too.
   *
   * @param directDependenciesFilter {@link Predicate} that defines which direct dependency from the artifact processed should be used to obtain its dependencies and build a node too.
   */
  public void setDirectDependenciesFilter(
      Predicate<Dependency> directDependenciesFilter) {
    this.directDependenciesFilter = directDependenciesFilter;
  }

  /**
   * Collects the {@link ArtifactClassificationNode} for the artifact set with the given filters defined by adding it to the {@link Map}.
   *
   * @param nodes {@link Map} that has as key the artifactId and the node (each node also has the list of dependencies nodes).
   */
  public void collectNodes(Map<String, ArtifactClassificationNode> nodes) {
    collectNodes(rootArtifact, nodes);
  }

  private void collectNodes(Artifact artifact, Map<String, ArtifactClassificationNode> artifactsClassified) {
    checkArtifactDeclaredAsDirectDependency(artifact);

    List<URL> urls;
    try {
      List<Dependency> managedDependencies =
          dependencyResolver.readArtifactDescriptor(artifact).getManagedDependencies();

      urls = toUrl(dependencyResolver.resolveDependencies(new Dependency(artifact, COMPILE),
                                                          Collections.<Dependency>emptyList(), managedDependencies,
                                                          dependencyFilter));
    } catch (Exception e) {
      throw new IllegalStateException("Couldn't resolve dependencies for artifact: '" + artifact + "' classification",
                                      e);
    }

    List<Dependency> directDependencies;
    List<ArtifactClassificationNode> artifactDependencies = newArrayList();
    try {
      directDependencies = dependencyResolver.getDirectDependencies(artifact);
    } catch (ArtifactDescriptorException e) {
      throw new IllegalStateException("Couldn't get direct dependencies for artifact: '" + artifact + "'", e);
    }
    logger.debug("Searching for dependencies on direct dependencies of artifact {}", artifact);
    List<Artifact> pluginArtifactDependencies = filterArtifacts(directDependencies, directDependenciesFilter);
    logger.debug("Artifacts {} identified a plugin dependencies for plugin {}", pluginArtifactDependencies, artifact);
    pluginArtifactDependencies.stream()
        .map(nextArtifact -> {
          String artifactClassifierLessId = toClassifierLessId(nextArtifact);
          if (!artifactsClassified.containsKey(artifactClassifierLessId)) {
            collectNodes(nextArtifact, artifactsClassified);
          }
          return artifactsClassified.get(artifactClassifierLessId);
        })
        .forEach(artifactDependencies::add);

    ArtifactClassificationNode artifactUrlClassification = new ArtifactClassificationNode(artifact,
                                                                                          urls,
                                                                                          artifactDependencies);

    artifactsClassified.put(toClassifierLessId(artifact), artifactUrlClassification);
  }

  private void checkArtifactDeclaredAsDirectDependency(Artifact artifact) {
    if (!this.rootArtifact.equals(artifact)) {
      if (!findDirectDependency(artifact.getGroupId(), artifact.getArtifactId(), rootArtifactDirectDependencies)
          .isPresent()) {
        throw new IllegalStateException("Plugin '" + artifact
                                            + "' has to be defined as direct dependency of your Maven project (" + rootArtifact + ")");
      }
    }
  }

  private Optional<Dependency> findDirectDependency(String groupId, String artifactId, List<Dependency> directDependencies) {
    return directDependencies.isEmpty() ? Optional.<Dependency>empty()
        : directDependencies.stream().filter(dependency -> dependency.getArtifact().getGroupId().equals(groupId)
        && dependency.getArtifact().getArtifactId().equals(artifactId)).findFirst();
  }

  protected String toClassifierLessId(Artifact pluginArtifact) {
    return toId(pluginArtifact.getGroupId(), pluginArtifact.getArtifactId(), pluginArtifact.getExtension(), null,
                pluginArtifact.getVersion());
  }

  /**
   * Collects from the list of directDependencies {@link Dependency} those that are classified with classifier
   * especified.
   *
   * @param directDependencies {@link List} of direct {@link Dependency}
   * @return {@link List} of {@link Artifact}s for those dependencies classified as with the give classifier, can be
   *         empty.
   */
  private List<Artifact> filterArtifacts(List<Dependency> directDependencies, Predicate<Dependency> filter) {
    return directDependencies.stream()
        .filter(dependency -> filter.test(dependency))
        .map(dependency -> dependency.getArtifact())
        .collect(toList());
  }

}
