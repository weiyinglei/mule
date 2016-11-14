/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.jms.api.connection.factory.activemq;

import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;

/**
 * //TODO
 */
public class ActiveMQConnectionFactoryConfiguration {

  private static final String DEFAULT_BROKER_URL = "vm://localhost?broker.persistent=false&broker.useJmx=false";

  @Parameter
  @Optional(defaultValue = DEFAULT_BROKER_URL)
  @Expression(NOT_SUPPORTED)
  private String brokerUrl;

  @Parameter
  @Alias("enable-xa")
  @Optional(defaultValue = "false")
  @Expression(NOT_SUPPORTED)
  private boolean enableXA;

  @Parameter
  @Optional(defaultValue = "1000")
  @Expression(NOT_SUPPORTED)
  private long initialRedeliveryDelay;

  @Parameter
  @Optional(defaultValue = "1000")
  @Expression(NOT_SUPPORTED)
  private long redeliveryDelay;

  /**
   * No redelivery. -1 mean infinite re deliveries accepted.
   * Can be overridden at the message source level.
   */
  @Parameter
  @Optional(defaultValue = "0")
  @Expression(NOT_SUPPORTED)
  private int maxRedelivery;

  public int getMaxRedelivery() {
    return maxRedelivery;
  }

  public boolean isEnableXA() {
    return enableXA;
  }

  public String getBrokerUrl() {
    return brokerUrl;
  }

  public long getInitialRedeliveryDelay() {
    return initialRedeliveryDelay;
  }

  public long getRedeliveryDelay() {
    return redeliveryDelay;
  }
}
