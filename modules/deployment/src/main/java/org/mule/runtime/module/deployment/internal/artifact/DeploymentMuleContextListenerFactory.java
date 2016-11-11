/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.internal.artifact;

import org.mule.runtime.core.api.context.notification.MuleContextListener;
import org.mule.runtime.module.deployment.api.DeploymentListener;
import org.mule.runtime.module.deployment.internal.artifact.MuleContextDeploymentListener;
import org.mule.runtime.module.deployment.internal.artifact.MuleContextListenerFactory;

public class DeploymentMuleContextListenerFactory implements MuleContextListenerFactory {

  private final DeploymentListener deploymentListener;

  public DeploymentMuleContextListenerFactory(DeploymentListener deploymentListener) {
    this.deploymentListener = deploymentListener;
  }

  @Override
  public MuleContextListener create(String artifactName) {
    return new MuleContextDeploymentListener(artifactName, deploymentListener);
  }
}
