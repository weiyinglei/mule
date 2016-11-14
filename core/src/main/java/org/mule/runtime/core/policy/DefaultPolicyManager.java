/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.policy;

import org.mule.runtime.core.api.policy.PolicyOperationParametersTransformer;
import org.mule.runtime.core.api.policy.PolicySourceParametersTransformer;
import org.mule.runtime.core.api.registry.RegistrationException;
import org.mule.runtime.dsl.api.component.ComponentIdentifier;

import java.util.Collection;
import java.util.Optional;

import javax.annotation.processing.SupportedOptions;
import javax.inject.Inject;

public class DefaultPolicyManager implements PolicyManager {

  //TODO change because it won't work if there's no policy at all in the registry
  @Inject
  private Collection<PolicyOperationParametersTransformer> policyOperationParametersTransformerCollection;

  //TODO change because it won't work if there's no policy at all in the registry
  @Inject
  private Collection<PolicySourceParametersTransformer> policySourceParametersTransformerCollection;

  @Inject
  private Collection<Policy> policies;

  @Override
  public Optional<Policy> lookupPolicy(ComponentIdentifier componentIdentifier, Optional<Object> messagePolicyDescriptor)
      throws RegistrationException {
    return Optional.ofNullable(policies.iterator().next());
  }

  @Override
  public Optional<PolicyOperationParametersTransformer> lookupOperationParametersTransformer(ComponentIdentifier componentIdentifier) {
    return policyOperationParametersTransformerCollection.stream()
        .filter(policyOperationParametersTransformer -> policyOperationParametersTransformer.supports(componentIdentifier))
        .findAny();
  }

  @Override
  public Optional<PolicySourceParametersTransformer> lookupSourceParametersTransformer(ComponentIdentifier componentIdentifier) {
    return policySourceParametersTransformerCollection.stream()
            .filter(policyOperationParametersTransformer -> policyOperationParametersTransformer.supports(componentIdentifier))
            .findAny();
  }


}
