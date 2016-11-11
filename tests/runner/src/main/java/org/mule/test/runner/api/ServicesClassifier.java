/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.runner.api;

import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Maps.newLinkedHashMap;
import static java.util.stream.Collectors.toList;
import static org.eclipse.aether.util.artifact.JavaScopes.COMPILE;
import static org.eclipse.aether.util.filter.DependencyFilterUtils.classpathFilter;
import static org.eclipse.aether.util.filter.DependencyFilterUtils.orFilter;
import static org.mule.runtime.api.util.Preconditions.checkNotNull;
import static org.mule.runtime.core.util.PropertiesUtils.loadProperties;

import org.mule.runtime.module.artifact.classloader.ArtifactClassLoader;
import org.mule.test.runner.classification.PatternExclusionsDependencyFilter;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Predicate;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyFilter;

/**
 * Classifier implementation for Services.
 */
public class ServicesClassifier extends AbstractArtifactClassifier<ArtifactClassification> {

  private static final String MULE_SERVICE_CLASSIFIER = "mule-service";
  private static final String SERVICE_PROPERTIES_FILE_NAME = "service.properties";
  private static final String SERVICE_PROVIDER_CLASS_NAME = "service.className";

  /**
   * Finds direct dependencies declared with classifier {@value #MULE_SERVICE_CLASSIFIER} and {@code provided} scope.
   * Creates a List of {@link ArtifactUrlClassification} for each service including their {@code compile} scope dependencies.
   * <p/>
   * {@value #SERVICE_PROVIDER_CLASS_NAME} will be used as {@link ArtifactClassLoader#getArtifactId()}
   * <p/>
   * Once identified and classified these Maven artifacts will be excluded from container classification.
   *
   * @param context {@link ClassPathClassifierContext} with settings for the classification process
   * @param directDependencies {@link List} of {@link Dependency} with direct dependencies for the rootArtifact
   * @return a {@link List} of {@link ArtifactUrlClassification}s that would be the one used for the plugins class loaders.
   */
  @Override
  public List<ArtifactClassification> classify(DependencyResolver dependencyResolver, ClassPathClassifierContext context,
                                                List<Dependency> directDependencies,
                                                Artifact rootArtifact, ArtifactClassificationType rootArtifactType) {
    final Predicate<Dependency> muleServiceClassifiedDependencyFilter =
        dependency -> dependency.getArtifact().getClassifier().equals(MULE_SERVICE_CLASSIFIER);
    List<Artifact> serviceArtifactsDeclared = filterArtifacts(directDependencies,
                                                              muleServiceClassifiedDependencyFilter);
    logger.debug("{} services defined to be classified", serviceArtifactsDeclared.size());

    final DependencyFilter dependencyFilter = orFilter(classpathFilter(COMPILE),
                                                       new PatternExclusionsDependencyFilter(context.getExcludedArtifacts()));

    Map<String, ArtifactClassificationNode> servicesClassified = newLinkedHashMap();
    serviceArtifactsDeclared.stream()
        .forEach(serviceArtifact -> {
          ArtifactClassificationNodeCollector collector = new ArtifactClassificationNodeCollector();
          collector.setRootArtifact(serviceArtifact);
          collector.setDirectDependenciesFilter(muleServiceClassifiedDependencyFilter);
          collector.setDependencyResolver(dependencyResolver);
          collector.setDependencyFilter(dependencyFilter);
          collector.setRootArtifactDirectDependencies(directDependencies);

          collector.collectNodes(servicesClassified);
        });


    return toServiceUrlClassification(servicesClassified.values());
  }

  /**
   * Transforms the {@link ArtifactClassificationNode} to {@link ArtifactsUrlClassification}.
   *
   * @param classificationNodes the fat object classified that needs to be transformed
   * @return {@link ArtifactsUrlClassification}
   */
  private List<ArtifactUrlClassification> toServiceUrlClassification(Collection<ArtifactClassificationNode> classificationNodes) {
    return classificationNodes.stream().map(node -> {
      InputStream servicePropertiesStream =
          new URLClassLoader(node.getUrls().toArray(new URL[0]), null).getResourceAsStream(SERVICE_PROPERTIES_FILE_NAME);
      checkNotNull(servicePropertiesStream,
                   "Couldn't find " + SERVICE_PROPERTIES_FILE_NAME + " for artifact: " + node.getArtifact());
      try {
        Properties serviceProperties = loadProperties(servicePropertiesStream);
        String serviceProviderClassName = serviceProperties.getProperty(SERVICE_PROVIDER_CLASS_NAME);
        logger.debug("Discover serviceProviderClassName: {} for artifact: {}", serviceProviderClassName, node.getArtifact());
        if (node.getExportClasses() != null && !node.getExportClasses().isEmpty()) {
          logger.warn("exportClasses is not supported for services artifacts, they are going to be ignored");
        }
        return new ArtifactUrlClassification(toClassifierLessId(node.getArtifact()), serviceProviderClassName, node.getUrls());
      } catch (IOException e) {
        throw new IllegalArgumentException("Couldn't read " + SERVICE_PROPERTIES_FILE_NAME + " for artifact: "
                                               + node.getArtifact(), e);
      }
    }).collect(toList());
  }

}
