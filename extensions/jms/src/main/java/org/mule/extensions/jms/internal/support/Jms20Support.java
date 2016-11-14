/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.jms.internal.support;

import static java.lang.String.format;
import static javax.jms.DeliveryMode.NON_PERSISTENT;
import static javax.jms.DeliveryMode.PERSISTENT;
import static javax.jms.Session.SESSION_TRANSACTED;
import static org.apache.commons.lang.StringUtils.isBlank;
import static org.mule.extensions.jms.api.connection.JmsSpecification.JMS_2_0;
import static org.mule.extensions.jms.api.connection.LookupJndiDestination.ALWAYS;
import static org.mule.extensions.jms.api.connection.LookupJndiDestination.TRY_ALWAYS;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import org.mule.extensions.jms.api.connection.JmsSpecification;
import org.mule.extensions.jms.api.connection.LookupJndiDestination;
import org.mule.extensions.jms.api.destination.ConsumerType;
import org.mule.extensions.jms.api.destination.TopicConsumer;

import java.text.MessageFormat;
import java.util.Optional;
import java.util.function.Function;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.Topic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>Jms20Support</code> is a template class to provide an abstraction to to
 * the JMS 2.0 API specification.
 */

public class Jms20Support implements JmsSupport {

  private Logger logger = LoggerFactory.getLogger(Jms20Support.class);
  private final static String PROTOCOL = "jms";

  private final Function<String, Optional<Destination>> jndiObjectSupplier;
  private final LookupJndiDestination lookupJndiDestination;

  public Jms20Support() {
    this(LookupJndiDestination.NEVER, (name) -> null);
  }

  public Jms20Support(LookupJndiDestination lookupJndiDestination, Function<String, Optional<Destination>> jndiObjectSupplier) {
    checkArgument(lookupJndiDestination != null, "The LookupJndiDestination cannot be null");
    checkArgument(jndiObjectSupplier != null, "The JndiObjectSupplier cannot be null");
    this.lookupJndiDestination = lookupJndiDestination;
    this.jndiObjectSupplier = jndiObjectSupplier;
  }

  @Override
  public Connection createConnection(ConnectionFactory connectionFactory, String username, String password)
      throws JMSException {
    if (connectionFactory == null) {
      throw new IllegalArgumentException("connectionFactory cannot be null");
    }
    return connectionFactory.createConnection(username, password);
  }

  @Override
  public Connection createConnection(ConnectionFactory connectionFactory) throws JMSException {
    if (connectionFactory == null) {
      throw new IllegalArgumentException("connectionFactory cannot be null");
    }
    return connectionFactory.createConnection();
  }

  @Override
  public Session createSession(Connection connection, boolean topic, boolean transacted, int ackMode)
      throws JMSException {
    return connection.createSession(transacted, (transacted ? SESSION_TRANSACTED : ackMode));
  }

  @Override
  public MessageProducer createProducer(Session session, Destination destination, boolean topic)
      throws JMSException {
    return session.createProducer(destination);
  }

  @Override
  public MessageConsumer createConsumer(Session session, Destination destination, String messageSelector, ConsumerType type)
      throws JMSException {
    if (!type.isTopic()) {
      return session.createConsumer(destination, messageSelector);
    }

    TopicConsumer topicConsumer = (TopicConsumer) type;
    if (topicConsumer.isDurable() && topicConsumer.isShared()) {
      return session.createSharedDurableConsumer((Topic) destination, topicConsumer.getSubscriptionName(), messageSelector);
    }

    if (topicConsumer.isDurable()) {
      return session.createDurableSubscriber((Topic) destination, topicConsumer.getSubscriptionName(), messageSelector,
                                             topicConsumer.isNoLocal());
    }

    if (topicConsumer.isShared()) {
      return session.createSharedConsumer((Topic) destination, topicConsumer.getSubscriptionName(), messageSelector);
    }

    return session.createConsumer(destination, messageSelector, topicConsumer.isNoLocal());
  }

  @Override
  public Destination createDestination(Session session, String name, boolean topic) throws JMSException {
    if (getLookupJndiDestination().equals(ALWAYS) || getLookupJndiDestination().equals(TRY_ALWAYS)) {
      Optional<Destination> destination = createDestinationFromJndi(name);
      if (destination.isPresent()) {
        return destination.get();
      }
    }

    if (logger.isDebugEnabled()) {
      logger.debug(format("Using non-JNDI destination [%s], will create one now", name));
    }

    if (session == null) {
      throw new IllegalArgumentException("Session cannot be null when creating a destination");
    }
    if (isBlank(name)) {
      throw new IllegalArgumentException("Destination name cannot be null when creating a destination");
    }

    if (topic) {
      if (logger.isDebugEnabled()) {
        logger.debug(format("Creating Topic Destination with name: [%s]", name));
      }

      return session.createTopic(name);
    } else {
      if (logger.isDebugEnabled()) {
        logger.debug(format("Creating Queue Destination with name: [%s]", name));
      }
      return session.createQueue(name);
    }
  }

  protected Optional<Destination> createDestinationFromJndi(String name) throws JMSException {
    try {
      Optional<Destination> dest = getJndiDestination(name);
      if (dest.isPresent()) {
        if (logger.isDebugEnabled()) {
          logger.debug(MessageFormat.format("Destination {0} located in JNDI, will use it now", name));
        }
        return dest;
      } else {
        throw new JMSException("JNDI destination not found with name: " + name);
      }
    } catch (JMSException e) {
      if (getLookupJndiDestination().equals(ALWAYS)) {
        throw e;
      } else {
        logger.warn("Unable to look up JNDI destination " + name + ": " + e.getMessage());
      }
    }
    return Optional.empty();
  }

  protected Optional<Destination> getJndiDestination(String name) throws JMSException {
    Optional<Destination> temp;
    try {
      if (logger.isDebugEnabled()) {
        logger.debug(MessageFormat.format("Looking up {0} from JNDI", name));
      }
      temp = jndiObjectSupplier.apply(name);
    } catch (Exception e) {
      if (logger.isDebugEnabled()) {
        logger.debug(e.getMessage(), e);
      }
      String message = MessageFormat.format("Failed to look up destination {0}. Reason: {1}",
                                            name, e.getMessage());
      throw new JMSException(message);
    }

    return temp;
  }

  @Override
  public Destination createTemporaryDestination(Session session, boolean topic) throws JMSException {
    if (session == null) {
      throw new IllegalArgumentException("Session cannot be null when creating a destination");
    }

    if (topic) {
      return session.createTemporaryTopic();
    } else {
      return session.createTemporaryQueue();
    }
  }

  @Override
  public void send(MessageProducer producer, Message message, Destination dest, boolean persistent, int priority,
                   long ttl, boolean topic)
      throws JMSException {
    if (logger.isDebugEnabled()) {
      logger.debug(format("Sending message to [%s], persistent:[%s], with priority:[%s] and ttl:[%s]",
                          dest instanceof Queue ? ((Queue) dest).getQueueName() : ((Topic) dest).getTopicName(),
                          persistent, priority, ttl));
    }
    producer.send(dest, message,
                  (persistent ? PERSISTENT : NON_PERSISTENT),
                  priority, ttl);
  }

  @Override
  public JmsSpecification getSpecification() {
    return JMS_2_0;
  }


  public Function<String, Optional<Destination>> getJndiObjectSupplier() {
    return jndiObjectSupplier;
  }

  public LookupJndiDestination getLookupJndiDestination() {
    return lookupJndiDestination;
  }
}
