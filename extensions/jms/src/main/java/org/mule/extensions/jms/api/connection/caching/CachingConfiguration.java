/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.jms.api.connection.caching;

import javax.jms.ConnectionFactory;

/**
 *
 */
public interface CachingConfiguration {

  boolean isEnabled();

  int getSessionCacheSize();

  boolean isProducersCache();

  boolean isConsumersCache();

  boolean appliesTo(ConnectionFactory target);
}
