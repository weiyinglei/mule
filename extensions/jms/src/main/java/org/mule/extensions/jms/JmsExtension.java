package org.mule.extensions.jms;

/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */


import org.mule.extensions.jms.api.config.JmsConsumerConfig;
import org.mule.extensions.jms.api.config.JmsProducerConfig;
import org.mule.extensions.jms.api.connection.caching.CachingConfiguration;
import org.mule.extensions.jms.api.connection.caching.DefaultCachingConfiguration;
import org.mule.extensions.jms.api.connection.caching.NoCachingConfiguration;
import org.mule.extensions.jms.api.connection.factory.jndi.JndiConnectionFactory;
import org.mule.extensions.jms.api.destination.ConsumerType;
import org.mule.extensions.jms.api.destination.QueueConsumer;
import org.mule.extensions.jms.api.destination.TopicConsumer;
import org.mule.extensions.jms.internal.connection.provider.GenericConnectionProvider;
import org.mule.extensions.jms.internal.connection.provider.activemq.ActiveMQConnectionProvider;
import org.mule.runtime.extension.api.annotation.Configurations;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.SubTypeMapping;
import org.mule.runtime.extension.api.annotation.connectivity.ConnectionProviders;
import org.mule.runtime.extension.api.annotation.dsl.xml.Xml;

import javax.jms.ConnectionFactory;

/**
 * @since 4.0
 */
@Extension(name = "JMS Extension")
@Xml(namespace = "jmsn")
@Configurations({JmsConsumerConfig.class, JmsProducerConfig.class})
@ConnectionProviders({GenericConnectionProvider.class, ActiveMQConnectionProvider.class})
@SubTypeMapping(
    baseType = ConsumerType.class, subTypes = {QueueConsumer.class, TopicConsumer.class})
@SubTypeMapping(
    baseType = CachingConfiguration.class, subTypes = {DefaultCachingConfiguration.class, NoCachingConfiguration.class})
@SubTypeMapping(
    baseType = ConnectionFactory.class, subTypes = {JndiConnectionFactory.class})
public class JmsExtension {

}
