/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.http.internal;

import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import org.mule.extension.http.internal.request.grizzly.GrizzlyHttpClient;
import org.mule.extension.http.internal.server.HttpListenerConnectionManager;
import org.mule.runtime.core.api.lifecycle.Initialisable;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.service.http.api.HttpService;
import org.mule.service.http.api.client.HttpClientFactory;
import org.mule.service.http.api.server.HttpServerFactory;

public class HttpServiceImplementation implements HttpService, Initialisable {

  private final HttpListenerConnectionManager connectionManager = new HttpListenerConnectionManager();

  @Override
  public HttpServerFactory getServerFactory() {
    //TODO: Create logic to make the manager able to distinguish apps (muleContext.getId() for now?)
    return connectionManager;
  }

  @Override
  public HttpClientFactory getClientFactory() {
    //DNS round robin should be achieve by using another client
    return GrizzlyHttpClient::new;
  }

  @Override
  public String getName() {
    return "HTTP Service";
  }

  @Override
  public void initialise() throws InitialisationException {
    initialiseIfNeeded(connectionManager);
  }
}
