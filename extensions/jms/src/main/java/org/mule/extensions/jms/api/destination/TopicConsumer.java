/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.jms.api.destination;

import static java.lang.String.format;
import static org.apache.commons.lang.StringUtils.isBlank;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.dsl.xml.XmlHints;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;

/**
 * //TODO
 */
@Alias("topic-consumer")
public class TopicConsumer implements ConsumerType, Initialisable {

  @Parameter
  @Optional(defaultValue = "false")
  //TODO do we need to use the config value if not defined here?
  private boolean isDurable;

  @Parameter
  @Optional(defaultValue = "false")
  //TODO do we need to use the config value if not defined here?
  private boolean isShared;

  @Parameter
  @Optional(defaultValue = "false")
  //TODO do we need to use the config value if not defined here?
  private boolean noLocal;

  @Parameter
  @Optional
  @XmlHints(allowReferences = false)
  private String subscriptionName;

  @Override
  public boolean isTopic() {
    return true;
  }

  public boolean isDurable() {
    return isDurable;
  }

  public boolean isShared() {
    return isShared;
  }

  public String getSubscriptionName() {
    return subscriptionName;
  }

  public boolean isNoLocal() {
    return noLocal;
  }

  @Override
  public void initialise() throws InitialisationException {
    if (!isBlank(subscriptionName)) {
      checkArgument(isShared() || isDurable(),
                    format("A [subscriptionName] was provided, but the subscription is neither [durable] nor [shared]"));
    } else {
      checkArgument(!isShared() && !isDurable(),
                    format("No [subscriptionName] was provided, but one is required to create a [durable] or [shared] subscriber"));
    }

    checkArgument(!(isShared() && isNoLocal()), "A [shared] topic consumer can't be [noLocal]");

  }


}
