/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.jms.api.connection.factory.jndi;

import static java.text.MessageFormat.format;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mule.extensions.jms.internal.i18n.JmsMessages.invalidResourceType;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import org.mule.extensions.jms.api.JmsConnectorException;
import org.mule.extensions.jms.api.connection.LookupJndiDestination;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.core.api.transaction.Transaction;
import org.mule.runtime.core.api.transaction.TransactionException;
import org.mule.runtime.core.transaction.TransactionCoordination;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;

import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSContext;
import javax.naming.CommunicationException;
import javax.naming.NamingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.connection.DelegatingConnectionFactory;

/**
 * A {@link ConnectionFactory} that wraps a {@link ConnectionFactory delegate}
 * that is discovered using a {@link JndiNameResolver}
 *
 * @since 4.0
 */
public class JndiConnectionFactory extends DelegatingConnectionFactory implements Lifecycle {

  private static final Logger logger = LoggerFactory.getLogger(JndiConnectionFactory.class);

  /**
   * Name of the ConnectionFactory to be discovered using Jndi
   * and used as a delegate of {@code this} {@link ConnectionFactory}
   */
  @Parameter
  private String connectionFactoryJndiName;

  /**
   * NEVER. Will never lookup for jndi destinations.
   * ALWAYS. Will always lookup the destinations through JNDI. It will fail if the destination does not exists.
   * TRY_ALWAYS. Will always try to lookup the destinations through JNDI but if it does not exists it will create a new one.
   */
  @Parameter
  @Optional(defaultValue = "NEVER")
  private LookupJndiDestination lookupDestination;

  @ParameterGroup
  private JndiNameResolverProvider nameResolverProvider;


  private JndiNameResolver nameResolver;


  public java.util.Optional<Destination> getJndiDestination(String name) {

    try {
      if (logger.isDebugEnabled()) {
        logger.debug(format("Looking up {0} from JNDI", name));
      }

      Object temp = lookupFromJndi(name);

      return temp instanceof Destination ? of((Destination) temp) : empty();

    } catch (NamingException e) {
      if (logger.isDebugEnabled()) {
        logger.debug(e.getMessage(), e);
      }
      String message = format("Failed to look up destination {0}. Reason: {1}", name, e.getMessage());
      throw new JmsConnectorException(message);
    }

  }


  @Override
  public void initialise() throws InitialisationException {

    try {
      setupNameResolver();

      Object temp = getJndiNameResolver().lookup(connectionFactoryJndiName);
      if (temp instanceof ConnectionFactory) {
        this.setTargetConnectionFactory((ConnectionFactory) temp);
      } else {
        throw new IllegalArgumentException(invalidResourceType(ConnectionFactory.class, temp).getMessage());
      }
    } catch (NamingException e) {
      e.printStackTrace();
      throw new InitialisationException(e, this);
    }
  }

  @Override
  public void start() throws MuleException {
    startIfNeeded(getJndiNameResolver());
  }

  @Override
  public void stop() throws MuleException {
    stopIfNeeded(getJndiNameResolver());
  }

  @Override
  public void dispose() {
    disposeIfNeeded(getJndiNameResolver(), logger);
  }

  private void setupNameResolver() throws InitialisationException {
    JndiNameResolver customJndiNameResolver = nameResolverProvider.getCustomJndiNameResolver();
    if (customJndiNameResolver != null) {
      nameResolver = customJndiNameResolver;
    } else {
      nameResolver = nameResolverProvider.createDefaultJndiResolver();
    }

    initialiseIfNeeded(nameResolver);
  }

  private Object lookupFromJndi(String jndiName) throws NamingException {
    try {
      return getJndiNameResolver().lookup(jndiName);
    } catch (CommunicationException ce) {
      try {
        final Transaction tx = TransactionCoordination.getInstance().getTransaction();
        if (tx != null) {
          tx.setRollbackOnly();
        }
      } catch (TransactionException e) {
        throw new MuleRuntimeException(createStaticMessage("Failed to mark transaction for rollback: "), e);
      }

      throw ce;
    }
  }

  private JndiNameResolver getJndiNameResolver() {
    return nameResolver;
  }

  @Override
  public JMSContext createContext() {
    // We'll use the classic API
    return null;
  }

  @Override
  public JMSContext createContext(String userName, String password) {
    // We'll use the classic API
    return null;
  }

  @Override
  public JMSContext createContext(String userName, String password, int sessionMode) {
    // We'll use the classic API
    return null;
  }

  @Override
  public JMSContext createContext(int sessionMode) {
    // We'll use the classic API
    return null;
  }

  public String getConnectionFactoryJndiName() {
    return connectionFactoryJndiName;
  }

  public LookupJndiDestination getLookupDestination() {
    return lookupDestination;
  }

  public JndiNameResolverProvider getNameResolverProvider() {
    return nameResolverProvider;
  }

}
