/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.jms.api.message;

import static org.mule.extensions.jms.internal.message.JMSXDefinedPropertiesNames.JMSXAppID;
import static org.mule.extensions.jms.internal.message.JMSXDefinedPropertiesNames.JMSXConsumerTXID;
import static org.mule.extensions.jms.internal.message.JMSXDefinedPropertiesNames.JMSXDeliveryCount;
import static org.mule.extensions.jms.internal.message.JMSXDefinedPropertiesNames.JMSXGroupID;
import static org.mule.extensions.jms.internal.message.JMSXDefinedPropertiesNames.JMSXGroupSeq;
import static org.mule.extensions.jms.internal.message.JMSXDefinedPropertiesNames.JMSXProducerTXID;
import static org.mule.extensions.jms.internal.message.JMSXDefinedPropertiesNames.JMSXRcvTimestamp;
import static org.mule.extensions.jms.internal.message.JMSXDefinedPropertiesNames.JMSXUserID;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;

import com.google.common.collect.ImmutableMap;

import java.util.Map;

/**
 * //TODO
 */
public class JmsxProperties {

  @Parameter
  @Optional
  private String jmsxUserID;

  @Parameter
  @Optional
  private String jmsxAppID;

  @Parameter
  @Optional
  private Integer jmsxDeliveryCount;

  @Parameter
  @Optional
  private String jmsxGroupID;

  @Parameter
  @Optional
  private Integer jmsxGroupSeq;

  @Parameter
  @Optional
  private String jmsxProducerTXID;

  @Parameter
  @Optional
  private String jmsxConsumerTXID;

  @Parameter
  @Optional
  private Long jmsxRcvTimestamp;

  public JmsxProperties() {}

  public JmsxProperties(String JMSXUserID, String JMSXAppID, Integer JMSXDeliveryCount, String JMSXGroupID, Integer JMSXGroupSeq,
                        String JMSXProducerTXID, String JMSXConsumerTXID, Long JMSXRcvTimestamp) {
    this.jmsxUserID = JMSXUserID;
    this.jmsxAppID = JMSXAppID;
    this.jmsxDeliveryCount = JMSXDeliveryCount;
    this.jmsxGroupID = JMSXGroupID;
    this.jmsxGroupSeq = JMSXGroupSeq;
    this.jmsxProducerTXID = JMSXProducerTXID;
    this.jmsxConsumerTXID = JMSXConsumerTXID;
    this.jmsxRcvTimestamp = JMSXRcvTimestamp;
  }

  public Map<String, Object> asMap() {
    ImmutableMap.Builder<String, Object> builder = new ImmutableMap.Builder<>();

    if (this.jmsxUserID != null) {
      builder.put(JMSXUserID, this.jmsxUserID);
    }
    if (this.jmsxAppID != null) {
      builder.put(JMSXAppID, this.jmsxAppID);
    }
    if (this.jmsxDeliveryCount != null) {
      builder.put(JMSXDeliveryCount, this.jmsxDeliveryCount);
    }
    if (this.jmsxGroupID != null) {
      builder.put(JMSXGroupID, this.jmsxGroupID);
    }
    if (this.jmsxGroupSeq != null) {
      builder.put(JMSXGroupSeq, this.jmsxGroupSeq);
    }
    if (this.jmsxProducerTXID != null) {
      builder.put(JMSXProducerTXID, this.jmsxProducerTXID);
    }
    if (this.jmsxConsumerTXID != null) {
      builder.put(JMSXConsumerTXID, this.jmsxConsumerTXID);
    }
    if (this.jmsxRcvTimestamp != null) {
      builder.put(JMSXRcvTimestamp, this.jmsxRcvTimestamp);
    }

    return builder.build();

  }

  public String getJMSXUserID() {
    return jmsxUserID;
  }

  public String getJMSXAppID() {
    return jmsxAppID;
  }

  public int getJMSXDeliveryCount() {
    return jmsxDeliveryCount;
  }

  public String getJMSXGroupID() {
    return jmsxGroupID;
  }

  public int getJMSXGroupSeq() {
    return jmsxGroupSeq;
  }

  public String getJMSXProducerTXID() {
    return jmsxProducerTXID;
  }

  public String getJMSXConsumerTXID() {
    return jmsxConsumerTXID;
  }

  public long getJMSXRcvTimestamp() {
    return jmsxRcvTimestamp;
  }

}
