/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.jms.api.operation;

import static org.mule.extensions.jms.api.operation.JmsOperationUtils.resolveOverride;
import static org.mule.extensions.jms.internal.function.JmsSupplier.fromJmsSupplier;
import org.mule.extensions.jms.api.config.AckMode;
import org.mule.extensions.jms.api.config.JmsConsumerConfig;
import org.mule.extensions.jms.api.connection.JmsConnection;
import org.mule.extensions.jms.api.connection.JmsSession;
import org.mule.extensions.jms.api.destination.ConsumerType;
import org.mule.extensions.jms.api.destination.QueueConsumer;
import org.mule.extensions.jms.api.message.JmsAttributes;
import org.mule.extensions.jms.internal.message.JmsResultFactory;
import org.mule.extensions.jms.internal.metadata.JmsOutputResolver;
import org.mule.extensions.jms.internal.support.JmsSupport;
import org.mule.runtime.extension.api.annotation.dsl.xml.XmlHints;
import org.mule.runtime.extension.api.annotation.metadata.OutputResolver;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.UseConfig;
import org.mule.runtime.extension.api.annotation.param.display.Summary;
import org.mule.runtime.extension.api.runtime.operation.Result;

import java.util.function.Supplier;

import javax.jms.Destination;
import javax.jms.Message;
import javax.jms.MessageConsumer;


public class JmsConsume {

  private JmsResultFactory messageFactory = new JmsResultFactory();

  @OutputResolver(output = JmsOutputResolver.class)
  public Result<Object, JmsAttributes> consume(@Connection JmsConnection connection, @UseConfig JmsConsumerConfig config,
                                               @XmlHints(
                                                 allowReferences = false) @Summary("The name of the Destination from where the Message should be consumed")
                                                 String destination,
                                               @Optional ConsumerType consumerType,
                                               @Optional AckMode ackMode,
                                               @Optional String selector,
                                               @Optional String contentType,
                                               @Optional String encoding,
                                               @Optional(defaultValue = "10000") Long maximumWaitTime) {
    try {
      JmsSession session = null;
      MessageConsumer consumer = null;
      try {
        ackMode = resolveOverride(config.getAckMode(), ackMode);

        consumerType = resolveConsumerType(consumerType);

        session = connection.createSession(ackMode, consumerType.isTopic());
        consumerType = resolveConsumerType(consumerType);
        JmsSupport jmsSupport = connection.getJmsSupport();

        Destination jmsDestination = jmsSupport.createDestination(session.get(), destination, consumerType.isTopic());

        consumer = jmsSupport.createConsumer(session.get(), jmsDestination,
                                             resolveOverride(config.getSelector(), selector),
                                             consumerType);

        Message received = resolveConsumeMessage(maximumWaitTime, consumer).get();

        return messageFactory.createResult(received, jmsSupport.getSpecification(),
                                           resolveOverride(config.getContentType(), contentType),
                                           resolveOverride(config.getEncoding(), encoding),
                                           session.getAckId());

      } finally {
        connection.closeQuietly(consumer);
        connection.closeQuietly(session);
      }
    } catch (Exception e) {
      //TODO throw proper exception
      throw new RuntimeException(e);
    }
  }

  private Supplier<Message> resolveConsumeMessage(Long maximumWaitTime, MessageConsumer consumer) {
    if (maximumWaitTime == -1) {
      return fromJmsSupplier(consumer::receive);
    } else if (maximumWaitTime == 0) {
      return fromJmsSupplier(consumer::receiveNoWait);
    } else {
      return fromJmsSupplier(() -> consumer.receive(maximumWaitTime));
    }
  }

  private ConsumerType resolveConsumerType(ConsumerType consumerType) {
    return consumerType != null ? consumerType : new QueueConsumer();
  }

}
