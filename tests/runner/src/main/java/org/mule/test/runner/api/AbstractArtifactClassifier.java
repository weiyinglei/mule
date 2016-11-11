/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.runner.api;

import static java.util.stream.Collectors.toList;
import static org.eclipse.aether.util.artifact.ArtifactIdUtils.toId;
import static org.eclipse.aether.util.artifact.JavaScopes.COMPILE;
import static org.eclipse.aether.util.artifact.JavaScopes.TEST;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for {@link ArtifactClassifier} implementations that provide some utility methods.
 *
 * @since 4.0
 */
public abstract class AbstractArtifactClassifier<T extends ArtifactClassification> implements ArtifactClassifier<T> {

  private static final String JAR_EXTENSION = "jar";
  private static final String MAVEN_COORDINATES_SEPARATOR = ":";

  protected final Logger logger = LoggerFactory.getLogger(this.getClass());

  /**
   * Finds the plugin shared lib {@link Dependency} from the direct dependencies of the  rootArtifact.
   *
   * @param pluginSharedLibCoords Maven coordinates that define the plugin shared lib artifact
   * @param rootArtifact {@link Artifact} that defines the current artifact that requested to build this class loaders
   * @param directDependencies {@link List} of {@link Dependency} with direct dependencies for the rootArtifact
   * @return {@link Artifact} representing the plugin shared lib artifact
   */
  protected Dependency findPluginSharedLibArtifact(String pluginSharedLibCoords, Artifact rootArtifact,
                                                 List<Dependency> directDependencies) {
    Optional<Dependency> pluginSharedLibDependency = discoverDependency(pluginSharedLibCoords, rootArtifact, directDependencies);
    if (!pluginSharedLibDependency.isPresent() || !pluginSharedLibDependency.get().getScope().equals(TEST)) {
      throw new IllegalStateException("Plugin shared lib artifact '" + pluginSharedLibCoords +
                                          "' in order to be resolved has to be declared as " + TEST + " dependency of your Maven project (" + rootArtifact + ")");
    }

    return pluginSharedLibDependency.get();
  }

  /**
   * Discovers the {@link Dependency} from the list of directDependencies using the artifact coordiantes in format of:
   *
   * <pre>
   * groupId:artifactId
   * </pre>
   * <p/>
   * If the coordinates matches to the rootArtifact it will return a {@value org.eclipse.aether.util.artifact.JavaScopes#COMPILE}
   * {@link Dependency}.
   *
   * @param artifactCoords Maven coordinates that define the artifact dependency
   * @param rootArtifact {@link Artifact} that defines the current artifact that requested to build this class loaders
   * @param directDependencies {@link List} of {@link Dependency} with direct dependencies for the rootArtifact
   * @return {@link Dependency} representing the artifact if declared as direct dependency or rootArtifact if they match it or
   *         {@link Optional#EMPTY} if couldn't found the dependency.
   * @throws {@link IllegalArgumentException} if artifactCoords are not in the expected format
   */
  protected Optional<Dependency> discoverDependency(String artifactCoords, Artifact rootArtifact,
                                                 List<Dependency> directDependencies) {
    final String[] artifactCoordsSplit = artifactCoords.split(MAVEN_COORDINATES_SEPARATOR);
    if (artifactCoordsSplit.length != 2) {
      throw new IllegalArgumentException("Artifact coordinates should be in format of groupId:artifactId, '" + artifactCoords +
                                             "' is not a valid format");
    }
    String groupId = artifactCoordsSplit[0];
    String artifactId = artifactCoordsSplit[1];

    if (rootArtifact.getGroupId().equals(groupId) && rootArtifact.getArtifactId().equals(artifactId)) {
      logger.debug("'{}' artifact coordinates matched with rootArtifact '{}', resolving version from rootArtifact",
                   artifactCoords, rootArtifact);
      final DefaultArtifact artifact = new DefaultArtifact(groupId, artifactId, JAR_EXTENSION, rootArtifact.getVersion());
      logger.debug("'{}' artifact coordinates resolved to: '{}'", artifactCoords, artifact);
      return Optional.of(new Dependency(artifact, COMPILE));

    } else {
      logger.debug("Resolving version for '{}' from direct dependencies", artifactCoords);
      return findDirectDependency(groupId, artifactId, directDependencies);
    }
  }

  /**
   * Finds the direct {@link Dependency} from rootArtifact for the given groupId and artifactId.
   *
   * @param groupId of the artifact to be found
   * @param artifactId of the artifact to be found
   * @param directDependencies the rootArtifact direct {@link Dependency}s
   * @return {@link Optional} {@link Dependency} to the dependency. Could be empty it if not present in the list of direct
   *         dependencies
   */
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
  protected List<Artifact> filterArtifacts(List<Dependency> directDependencies, Predicate<Dependency> filter) {
    return directDependencies.stream()
        .filter(dependency -> filter.test(dependency))
        .map(dependency -> dependency.getArtifact())
        .collect(toList());
  }
}
