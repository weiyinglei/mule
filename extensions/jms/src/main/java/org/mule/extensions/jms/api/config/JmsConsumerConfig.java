/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.jms.api.config;

import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;
import org.mule.extensions.jms.api.destination.ConsumerType;
import org.mule.extensions.jms.api.operation.JmsConsume;
import org.mule.runtime.extension.api.annotation.Configuration;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;

/**
 * //TODO
 */
@Configuration(name = "consumer-config")
@Operations({JmsConsume.class})
//@Sources({JmsSubscriber.class})
public class JmsConsumerConfig extends JmsBaseConfig {

  /**
   * AUTO. Mule acks the message only if the flow is finished successfully.
   * MANUAL. This is JMS client ack mode. The user must do the ack manually within the flow.
   * DUPS_OK. JMS message is ack automatically but in a lazy fashion which may lead to duplicates.
   * NONE. Automatically acks the message upon reception.
   * Can be overridden at the message source level.
   * This attribute has to be NONE if transactionType is LOCAL or MULTI
   */
  @Parameter
  @Optional(defaultValue = "AUTO")
  @Expression(NOT_SUPPORTED)
  private AckMode ackMode;

  @Parameter
  @Optional
  @Expression(NOT_SUPPORTED)
  private ConsumerType consumerType;

  @Parameter
  @Optional
  @Expression(NOT_SUPPORTED)
  private String selector;

  // TODO duplicated in ActiveMQ for default factory creation
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

  public String getSelector() {
    return selector;
  }


  public ConsumerType getConsumerType() {
    return consumerType;
  }

  public AckMode getAckMode() {
    return ackMode;
  }

}
