/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.jms.api.connection;

import javax.jms.Session;

/**
 * //TODO
 */
public class JmsSession {

  private final Session session;
  private final String ackId;

  public JmsSession(Session session, String ackId) {
    this.session = session;
    this.ackId = ackId;
  }

  public Session get() {
    return session;
  }

  public String getAckId() {
    return ackId;
  }
}
