/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.jms.api.connection.caching;

import org.mule.extensions.jms.internal.connection.JmsCachingConnectionFactory;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;

import javax.jms.ConnectionFactory;
import javax.jms.XAConnectionFactory;

import org.springframework.jms.connection.CachingConnectionFactory;

/**
 * //TODO
 */
@Alias("default-caching")
public class DefaultCachingConfiguration implements CachingConfiguration {

  @Parameter
  @Optional(defaultValue = "1")
  int sessionCacheSize;

  @Parameter
  @Alias("cacheProducers")
  @Optional(defaultValue = "true")
  boolean producersCache;

  @Parameter
  @Alias("cacheConsumers")
  @Optional(defaultValue = "true")
  boolean consumersCache;

  @Override
  public boolean isEnabled() {
    return sessionCacheSize > 0;
  }

  @Override
  public int getSessionCacheSize() {
    return sessionCacheSize;
  }

  @Override
  public boolean isProducersCache() {
    return producersCache;
  }

  @Override
  public boolean isConsumersCache() {
    return false;
  }

  @Override
  public boolean appliesTo(ConnectionFactory target) {
    // We only wrap connection factories that i) aren't instances of XAConnectionFactory ii) haven't already been
    // wrapped.
    return !(target instanceof XAConnectionFactory
        || target instanceof CachingConnectionFactory
        || target instanceof JmsCachingConnectionFactory);
  }
}
