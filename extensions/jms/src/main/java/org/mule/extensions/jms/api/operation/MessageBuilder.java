/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.jms.api.operation;


import static java.lang.String.format;
import static org.apache.commons.lang.StringUtils.isBlank;
import static org.mule.extensions.jms.api.operation.JmsOperationUtils.resolveOverride;
import static org.mule.extensions.jms.internal.message.JMSXDefinedPropertiesNames.JMSX_NAMES;
import static org.mule.extensions.jms.internal.message.JmsMessageUtils.encodeHeader;
import static org.mule.extensions.jms.internal.message.JmsMessageUtils.toMessage;
import org.mule.extensions.jms.api.config.JmsProducerConfig;
import org.mule.extensions.jms.api.destination.JmsDestination;
import org.mule.extensions.jms.api.message.JmsxProperties;
import org.mule.extensions.jms.internal.support.JmsSupport;
import org.mule.runtime.extension.api.annotation.dsl.xml.XmlHints;
import org.mule.runtime.extension.api.annotation.param.NullSafe;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Summary;

import java.util.HashMap;
import java.util.Map;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MessageBuilder {

  private static final Logger logger = LoggerFactory.getLogger(MessageBuilder.class);
  private static final String CONTENT_TYPE_JMS_PROPERTY = "MM_MESSAGE_CONTENT_TYPE";

  @Parameter
  @XmlHints(allowReferences = false)
  private Object content;

  @Parameter
  @Optional
  @XmlHints(allowReferences = false)
  private String jmsType;

  @Parameter
  @Optional
  @XmlHints(allowReferences = false)
  private String correlationId;

  @Parameter
  @Optional(defaultValue = "true")
  private boolean sendContentType;

  @Parameter
  @Optional(defaultValue = "text/plain") //TODO take this from the message
  @DisplayName("ContentType")
  private String contentType;

  @Parameter
  @Optional
  @Summary("The destination where a reply to the message should be sent")
  private JmsDestination replyTo;

  @Parameter
  @Optional
  @NullSafe
  // TODO MULE-10901: Nullsafe bug
  private JmsxProperties jmsxProperties = new JmsxProperties();

  @Parameter
  @Optional
  @NullSafe
  // TODO MULE-10901: Nullsafe bug
  private Map<String, Object> properties = new HashMap<>();

  Message build(JmsSupport jmsSupport, Session session, JmsProducerConfig config) throws JMSException {

    config.getEncoding();

    Message message = toMessage(content, session);

    setJmsCorrelationIdHeader(message);
    setJmsTypeHeader(config, message);
    setJmsReplyToHeader(jmsSupport, session, message);

    setJmsxProperties(message);
    setUserProperties(message);

    if (sendContentType) {
      setContentTypeProperty(config, message);
    }

    return message;
  }

  private void setJmsReplyToHeader(JmsSupport jmsSupport, Session session, Message message) {
    try {
      if (replyTo != null) {
        Destination destination = jmsSupport.createDestination(session, replyTo.getDestination(), replyTo.isTopic());
        message.setJMSReplyTo(destination);
      }
    } catch (JMSException e) {
      logger.error("Unable to set JMSReplyTo header: ", e);
    }
  }

  private void setContentTypeProperty(JmsProducerConfig config, Message message) {
    try {
      message.setStringProperty(CONTENT_TYPE_JMS_PROPERTY, resolveOverride(config.getContentType(), contentType));
    } catch (JMSException e) {
      logger.error(format("Unable to set property [%s] of type String: ", CONTENT_TYPE_JMS_PROPERTY), e);
    }
  }

  private void setJmsxProperties(final Message message) {
    jmsxProperties.asMap().entrySet().stream()
        .filter(e -> e.getValue() != null)
        .forEach(e -> setJmsPropertySanitizeKeyIfNecessary(message, e.getKey(), e.getValue()));
  }

  private void setUserProperties(final Message message) {
    properties.keySet().stream()
        .filter(key -> !isBlank(key) && !JMSX_NAMES.contains(key))
        .forEach(key -> setJmsPropertySanitizeKeyIfNecessary(message, key, properties.get(key)));
  }


  private void setJmsPropertySanitizeKeyIfNecessary(Message msg, String key, Object value) {
    try {
      // sanitize key as JMS Property Name
      key = encodeHeader(key);
      msg.setObjectProperty(key, value);
    } catch (JMSException e) {
      // Various JMS servers have slightly different rules to what
      // can be set as an object property on the message; therefore
      // we have to take a hit n' hope approach
      if (logger.isDebugEnabled()) {
        logger.debug(format("Unable to set property [%s] of type [%s]: ", key, value.getClass().getSimpleName()), e);
      }
    }
  }


  private void setJmsTypeHeader(JmsProducerConfig config, Message message) {
    try {
      String type = resolveOverride(config.getJmsType(), jmsType);
      if (!isBlank(type)) {
        message.setJMSType(type);
      }
    } catch (JMSException e) {
      logger.error("An error occurred while setting the JMSType property: %s", e);
    }
  }

  private void setJmsCorrelationIdHeader(Message message) {
    try {
      if (!isBlank(correlationId)) {
        message.setJMSCorrelationID(correlationId);
      }
    } catch (JMSException e) {
      logger.error("An error occurred while setting the JMSCorrelationID property: %s", e);
    }
  }

  public Object getContent() {
    return content;
  }

  public boolean isSendContentType() {
    return sendContentType;
  }

  public String getContentType() {
    return contentType;
  }

  public Map<String, Object> getProperties() {
    return properties;
  }

  public String getJmsType() {
    return jmsType;
  }

  public String getCorrelationId() {
    return correlationId;
  }

  public JmsxProperties getJmsxProperties() {
    return jmsxProperties;
  }

  public JmsDestination getReplyTo() {
    return replyTo;
  }
}
