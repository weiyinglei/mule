/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.runner.api;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newLinkedHashMap;
import static java.util.stream.Collectors.toList;
import static org.eclipse.aether.util.artifact.JavaScopes.COMPILE;
import static org.eclipse.aether.util.artifact.JavaScopes.PROVIDED;
import static org.eclipse.aether.util.filter.DependencyFilterUtils.classpathFilter;
import static org.eclipse.aether.util.filter.DependencyFilterUtils.orFilter;
import static org.mule.test.runner.api.ArtifactClassificationType.PLUGIN;

import org.mule.runtime.module.extension.internal.manager.DefaultExtensionManager;
import org.mule.test.runner.classification.PatternExclusionsDependencyFilter;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.resolution.ArtifactDescriptorException;

/**
 * Classification for plugins.
 *
 * @since 4.0
 */
public class PluginsClassifier extends AbstractArtifactClassifier<PluginUrlClassification> {

  private static final String MULE_PLUGIN_CLASSIFIER = "mule-plugin";
  private DefaultExtensionManager extensionManager = new DefaultExtensionManager();
  private PluginResourcesResolver pluginResourcesResolver = new PluginResourcesResolver(extensionManager);

  /**
   * Plugin classifications are being done by resolving the dependencies for each plugin coordinates defined at
   * {@link ClassPathClassifierContext#getPluginCoordinates()}. These artifacts should be defined as
   * {@value org.eclipse.aether.util.artifact.JavaScopes#PROVIDED} in the rootArtifact and if these coordinates don't have a
   * version the rootArtifact version would be used to look for the Maven plugin artifact.
   * <p/>
   * While resolving the dependencies for the plugin artifact, only {@value org.eclipse.aether.util.artifact.JavaScopes#COMPILE}
   * dependencies will be selected. {@link ClassPathClassifierContext#getExcludedArtifacts()} will be exluded too.
   * <p/>
   * The resulting {@link PluginUrlClassification} for each plugin will have as name the Maven artifact id coordinates:
   * {@code <groupId>:<artifactId>:<extension>[:<classifier>]:<version>}.
   *
   * @param dependencyResolver {@link DependencyResolver} to resolve dependencies
   * @param context {@link ClassPathClassifierContext} with settings for the classification process
   * @param directDependencies {@link List} of {@link Dependency} with direct dependencies for the rootArtifact
   * @param rootArtifact {@link ArtifactClassificationType} for rootArtifact
   * @param rootArtifactType {@link ArtifactClassificationType} defines the type of root artifact
   * @return {@link ArtifactClassification} with list of {@link URL}s to be added to runtime shared libraries.
   */
  @Override
  public List<PluginUrlClassification> classify(DependencyResolver dependencyResolver, ClassPathClassifierContext context, List<Dependency> directDependencies,
                                                         Artifact rootArtifact, ArtifactClassificationType rootArtifactType) {
    Map<String, ArtifactClassificationNode> pluginsClassified = newLinkedHashMap();

    List<Artifact> pluginsArtifacts = context.getPluginCoordinates().stream()
        .map(pluginCoords -> createPluginArtifact(pluginCoords, rootArtifact, directDependencies))
        .collect(toList());

    logger.debug("{} plugins defined to be classified", pluginsArtifacts.size());

    Predicate<Dependency> mulePluginDependencyFilter =
        dependency -> dependency.getArtifact().getClassifier().equals(MULE_PLUGIN_CLASSIFIER);
    if (PLUGIN.equals(rootArtifactType)) {
      logger.debug("rootArtifact '{}' identified as Mule plugin", rootArtifact);
      buildPluginUrlClassification(dependencyResolver, rootArtifact, context, directDependencies, mulePluginDependencyFilter, pluginsClassified);

      pluginsArtifacts = pluginsArtifacts.stream()
          .filter(pluginArtifact -> !(rootArtifact.getGroupId().equals(pluginArtifact.getGroupId())
              && rootArtifact.getArtifactId().equals(pluginArtifact.getArtifactId())))
          .collect(toList());
    }

    pluginsArtifacts.stream()
        .forEach(pluginArtifact -> buildPluginUrlClassification(dependencyResolver, pluginArtifact, context, directDependencies,
                                                                mulePluginDependencyFilter,
                                                                pluginsClassified));

    if (context.isExtensionMetadataGenerationEnabled()) {
      ExtensionPluginMetadataGenerator extensionPluginMetadataGenerator =
          new ExtensionPluginMetadataGenerator(context.getPluginResourcesFolder());

      for (ArtifactClassificationNode pluginClassifiedNode : pluginsClassified.values()) {
        List<URL> resourcesUrls =
            generateExtensionMetadata(pluginClassifiedNode.getArtifact(), context, extensionPluginMetadataGenerator,
                                      pluginClassifiedNode.getUrls());
        pluginClassifiedNode.setUrls(resourcesUrls);
      }

      extensionPluginMetadataGenerator.generateDslResources();
    }
    return toPluginUrlClassification(pluginsClassified.values());
  }

  /**
   * If enabled generates the Extension metadata and returns the {@link List} of {@link URL}s with the folder were metadata is
   * generated as first entry in the list.
   *
   * @param plugin plugin {@link Artifact} to generate its Extension metadata
   * @param context {@link ClassPathClassifierContext} with settings for the classification process
   * @param extensionPluginGenerator {@link ExtensionPluginMetadataGenerator} extensions metadata generator
   * @param urls current {@link List} of {@link URL}s classified for the plugin
   * @return {@link List} of {@link URL}s classified for the plugin
   */
  private List<URL> generateExtensionMetadata(Artifact plugin, ClassPathClassifierContext context,
                                              ExtensionPluginMetadataGenerator extensionPluginGenerator, List<URL> urls) {
    Class extensionClass = extensionPluginGenerator.scanForExtensionAnnotatedClasses(plugin, urls);
    if (extensionClass != null) {
      logger.debug("Plugin '{}' has been discovered as Extension", plugin);
      if (context.isExtensionMetadataGenerationEnabled()) {
        File generatedMetadataFolder = extensionPluginGenerator.generateExtensionManifest(plugin, extensionClass);
        URL generatedTestResources = toUrl(generatedMetadataFolder);

        List<URL> appendedTestResources = newArrayList(generatedTestResources);
        appendedTestResources.addAll(urls);
        urls = appendedTestResources;
      }
    }
    return urls;
  }

  /**
   * Classifies an {@link Artifact} recursively. {@value org.eclipse.aether.util.artifact.JavaScopes#COMPILE} dependencies will be
   * resolved for building the {@link URL}'s for the class loader. Once classified the node is added to {@link Map} of artifactsClassified.
   *
   * @param artifactToClassify {@link Artifact} that represents the artifact to be classified
   * @param context {@link ClassPathClassifierContext} with settings for the classification process
   * @param rootArtifactDirectDependencies {@link List} of {@link Dependency} with direct dependencies for the rootArtifact
   * @param artifactsClassified {@link Map} that contains already classified plugins
   */
  private void buildPluginUrlClassification(DependencyResolver dependencyResolver, Artifact artifactToClassify, ClassPathClassifierContext context,
                                            List<Dependency> rootArtifactDirectDependencies,
                                            Predicate<Dependency> directDependenciesFilter,
                                            Map<String, ArtifactClassificationNode> artifactsClassified) {
    checkPluginDeclaredAsDirectDependency(artifactToClassify, context, rootArtifactDirectDependencies);

    List<URL> urls;
    try {
      List<Dependency> managedDependencies =
          dependencyResolver.readArtifactDescriptor(artifactToClassify).getManagedDependencies();

      final DependencyFilter dependencyFilter = orFilter(classpathFilter(COMPILE),
                                                         new PatternExclusionsDependencyFilter(context.getExcludedArtifacts()));
      urls = toUrl(dependencyResolver.resolveDependencies(new Dependency(artifactToClassify, COMPILE),
                                                          Collections.<Dependency>emptyList(), managedDependencies,
                                                          dependencyFilter));
    } catch (Exception e) {
      throw new IllegalStateException("Couldn't resolve dependencies for artifact: '" + artifactToClassify + "' classification",
                                      e);
    }

    List<Dependency> directDependencies;
    List<ArtifactClassificationNode> artifactDependencies = newArrayList();
    try {
      directDependencies = dependencyResolver.getDirectDependencies(artifactToClassify);
    } catch (ArtifactDescriptorException e) {
      throw new IllegalStateException("Couldn't get direct dependencies for artifact: '" + artifactToClassify + "'", e);
    }
    logger.debug("Searching for dependencies on direct dependencies of artifact {}", artifactToClassify);
    List<Artifact> pluginArtifactDependencies = filterArtifacts(directDependencies, directDependenciesFilter);
    logger.debug("Artifacts {} identified a plugin dependencies for plugin {}", pluginArtifactDependencies, artifactToClassify);
    pluginArtifactDependencies.stream()
        .map(artifact -> {
          String artifactClassifierLessId = toClassifierLessId(artifact);
          if (!artifactsClassified.containsKey(artifactClassifierLessId)) {
            buildPluginUrlClassification(dependencyResolver, artifact, context, rootArtifactDirectDependencies, directDependenciesFilter,
                                         artifactsClassified);
          }
          return artifactsClassified.get(artifactClassifierLessId);
        })
        .forEach(artifactDependencies::add);

    final ArrayList<Class> exportClasses = newArrayList(context.getExportPluginClasses(artifactToClassify));
    ArtifactClassificationNode artifactUrlClassification = new ArtifactClassificationNode(artifactToClassify,
                                                                                          urls,
                                                                                          exportClasses,
                                                                                          artifactDependencies);

    artifactsClassified.put(toClassifierLessId(artifactToClassify), artifactUrlClassification);
  }

  /**
   * Checks if the pluginArtifact {@link Artifact} is declared as direct dependency of the rootArtifact or if the pluginArtifact
   * is the same rootArtifact. In case if it is a dependency and is not declared it throws an {@link IllegalStateException}.
   *
   * @param pluginArtifact plugin {@link Artifact} to be checked
   * @param context {@link ClassPathClassifierContext} with settings for the classification process
   * @param rootArtifactDirectDependencies {@link List} of {@link Dependency} with direct dependencies for the rootArtifact
   * @throws {@link IllegalStateException} if the plugin is a dependency not declared in rootArtifact directDependencies
   */
  private void checkPluginDeclaredAsDirectDependency(Artifact pluginArtifact, ClassPathClassifierContext context,
                                                     List<Dependency> rootArtifactDirectDependencies) {
    if (!context.getRootArtifact().equals(pluginArtifact)) {
      if (!findDirectDependency(pluginArtifact.getGroupId(), pluginArtifact.getArtifactId(), rootArtifactDirectDependencies)
          .isPresent()) {
        throw new IllegalStateException("Plugin '" + pluginArtifact
                                            + "' has to be defined as direct dependency of your Maven project (" + context.getRootArtifact() + ")");
      }
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

  /**
   * Creates the plugin {@link Artifact}, if no version is {@value org.eclipse.aether.util.artifact.JavaScopes#PROVIDED} it will
   * be obtained from the direct dependencies for the rootArtifact or if the same rootArtifact is the plugin declared it will take
   * its version.
   *
   * @param pluginCoords Maven coordinates that define the plugin
   * @param rootArtifact {@link Artifact} that defines the current artifact that requested to build this class loaders
   * @param directDependencies {@link List} of {@link Dependency} with direct dependencies for the rootArtifact
   * @return {@link Artifact} representing the plugin
   */
  private Artifact createPluginArtifact(String pluginCoords, Artifact rootArtifact, List<Dependency> directDependencies) {
    Optional<Dependency> pluginDependency = discoverDependency(pluginCoords, rootArtifact, directDependencies);
    if (!pluginDependency.isPresent() || !pluginDependency.get().getScope().equals(PROVIDED)) {
      throw new IllegalStateException("Plugin '" + pluginCoords + "' in order to be resolved has to be declared as " + PROVIDED +
                                          " dependency of your Maven project (" + rootArtifact + ")");
    }

    return pluginDependency.get().getArtifact();
  }

  /**
   * Transforms the {@link ArtifactClassificationNode} to {@link PluginUrlClassification}.
   *
   * @param classificationNodes the fat object classified that needs to be transformed
   * @return {@link PluginUrlClassification}
   */
  private List<PluginUrlClassification> toPluginUrlClassification(Collection<ArtifactClassificationNode> classificationNodes) {

    Map<String, PluginUrlClassification> classifiedPluginUrls = newLinkedHashMap();

    for (ArtifactClassificationNode node : classificationNodes) {
      final List<String> pluginDependencies = node.getArtifactDependencies().stream()
          .map(dependency -> toClassifierLessId(dependency.getArtifact()))
          .collect(toList());
      final String classifierLessId = toClassifierLessId(node.getArtifact());
      final PluginUrlClassification pluginUrlClassification =
          pluginResourcesResolver.resolvePluginResourcesFor(
              new PluginUrlClassification(classifierLessId, node.getUrls(),
                                          node.getExportClasses(),
                                          pluginDependencies));

      classifiedPluginUrls.put(classifierLessId, pluginUrlClassification);
    }

    for (PluginUrlClassification pluginUrlClassification : classifiedPluginUrls.values()) {
      for (String dependency : pluginUrlClassification.getPluginDependencies()) {
        final PluginUrlClassification dependencyPlugin = classifiedPluginUrls.get(dependency);
        if (dependencyPlugin == null) {
          throw new IllegalStateException("Unable to find a plugin dependency: " + dependency);
        }

        pluginUrlClassification.getExportedPackages().removeAll(dependencyPlugin.getExportedPackages());
      }
    }

    return newArrayList(classifiedPluginUrls.values());
  }

}
