/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.core.endpoint;

import org.mule.compatibility.core.api.endpoint.EndpointMessageProcessorChainFactory;
import org.mule.compatibility.core.api.endpoint.EndpointURI;
import org.mule.compatibility.core.api.endpoint.OutboundEndpoint;
import org.mule.compatibility.core.api.security.EndpointSecurityFilter;
import org.mule.compatibility.core.api.transport.Connector;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.core.MessageExchangePattern;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.exception.MessagingExceptionHandler;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.retry.RetryPolicyTemplate;
import org.mule.runtime.core.api.routing.filter.Filter;
import org.mule.runtime.core.api.transaction.TransactionConfig;
import org.mule.runtime.core.processor.AbstractRedeliveryPolicy;

import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

/**
 * Allows EndpointURI to be set and changed dynamically by wrapping up an immutable endpoint instance.
 */
public class DynamicURIOutboundEndpoint implements OutboundEndpoint {

  private static final long serialVersionUID = -2814979100270307813L;

  protected OutboundEndpoint endpoint;
  private EndpointURI dynamicEndpointURI;

  private FlowConstruct flowConstruct;

  public DynamicURIOutboundEndpoint(OutboundEndpoint endpoint) {
    this.endpoint = endpoint;
  }

  public DynamicURIOutboundEndpoint(OutboundEndpoint endpoint, EndpointURI dynamicEndpointURI) {
    this.endpoint = endpoint;
    setEndpointURI(dynamicEndpointURI);
  }

  @Override
  public EndpointURI getEndpointURI() {
    if (dynamicEndpointURI != null) {
      return dynamicEndpointURI;
    } else {
      return endpoint.getEndpointURI();
    }
  }

  @Override
  public String getAddress() {
    EndpointURI uri = getEndpointURI();
    if (uri != null) {
      return uri.getUri().toString();
    } else {
      return null;
    }
  }

  public void setEndpointURI(EndpointURI dynamicEndpointURI) {
    this.dynamicEndpointURI = dynamicEndpointURI;
  }

  @Override
  public RetryPolicyTemplate getRetryPolicyTemplate() {
    return endpoint.getRetryPolicyTemplate();
  }

  @Override
  public AbstractRedeliveryPolicy getRedeliveryPolicy() {
    return endpoint.getRedeliveryPolicy();
  }

  @Override
  public Connector getConnector() {
    return endpoint.getConnector();
  }

  @Override
  public Charset getEncoding() {
    return endpoint.getEncoding();
  }

  @Override
  public MediaType getMimeType() {
    return endpoint.getMimeType();
  }

  @Override
  public Filter getFilter() {
    return endpoint.getFilter();
  }

  @Override
  public String getInitialState() {
    return endpoint.getInitialState();
  }

  @Override
  public MuleContext getMuleContext() {
    return endpoint.getMuleContext();
  }

  @Override
  public String getName() {
    return endpoint.getName();
  }

  @Override
  public Map<String, Serializable> getProperties() {
    return endpoint.getProperties();
  }

  @Override
  public Serializable getProperty(Object key) {
    return endpoint.getProperty(key);
  }

  @Override
  public String getProtocol() {
    return endpoint.getProtocol();
  }

  @Override
  public int getResponseTimeout() {
    return endpoint.getResponseTimeout();
  }

  @Override
  public EndpointMessageProcessorChainFactory getMessageProcessorsFactory() {
    return endpoint.getMessageProcessorsFactory();
  }

  @Override
  public List<Processor> getMessageProcessors() {
    return endpoint.getMessageProcessors();
  }

  @Override
  public List<Processor> getResponseMessageProcessors() {
    return endpoint.getResponseMessageProcessors();
  }

  @Override
  public EndpointSecurityFilter getSecurityFilter() {
    return endpoint.getSecurityFilter();
  }

  @Override
  public TransactionConfig getTransactionConfig() {
    return endpoint.getTransactionConfig();
  }

  @Override
  public boolean isDeleteUnacceptedMessages() {
    return endpoint.isDeleteUnacceptedMessages();
  }

  @Override
  public boolean isReadOnly() {
    return endpoint.isReadOnly();
  }

  @Override
  public MessageExchangePattern getExchangePattern() {
    return endpoint.getExchangePattern();
  }

  @Override
  public List<String> getResponseProperties() {
    return endpoint.getResponseProperties();
  }

  @Override
  public boolean isDynamic() {
    return true;
  }

  @Override
  public String getEndpointBuilderName() {
    return endpoint.getEndpointBuilderName();
  }

  @Override
  public boolean isDisableTransportTransformer() {
    return endpoint.isDisableTransportTransformer();
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((dynamicEndpointURI == null) ? 0 : dynamicEndpointURI.hashCode());
    result = prime * result + ((endpoint == null) ? 0 : endpoint.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final DynamicURIOutboundEndpoint other = (DynamicURIOutboundEndpoint) obj;
    if (dynamicEndpointURI == null) {
      if (other.dynamicEndpointURI != null) {
        return false;
      }
    } else if (!dynamicEndpointURI.equals(other.dynamicEndpointURI)) {
      return false;
    }
    if (endpoint == null) {
      if (other.endpoint != null) {
        return false;
      }
    } else if (!endpoint.equals(other.endpoint)) {
      return false;
    }
    return true;
  }

  @Override
  public boolean isProtocolSupported(String protocol) {
    return getConnector().supportsProtocol(protocol);
  }

  @Override
  public Event process(Event event) throws MuleException {
    return endpoint.process(event);
  }

  @Override
  public void setMessagingExceptionHandler(MessagingExceptionHandler messagingExceptionHandler) {
    endpoint.setMessagingExceptionHandler(messagingExceptionHandler);
  }

  @Override
  public void setFlowConstruct(FlowConstruct flowConstruct) {
    this.flowConstruct = flowConstruct;
  }

  @Override
  public FlowConstruct getFlowConstruct() {
    return flowConstruct;
  }
}
