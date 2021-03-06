/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.vm.functional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.mule.functional.extensions.CompatibilityFunctionalTestCase;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.runtime.core.api.message.InternalMessage;

import org.junit.Test;

public class PersistentUnaddressedVmQueueTestCase extends CompatibilityFunctionalTestCase {

  private static final int RECEIVE_TIMEOUT = 5000;

  @Override
  protected String getConfigFile() {
    return "vm/persistent-unaddressed-vm-queue-test-flow.xml";
  }

  @Test
  public void testAsynchronousDispatching() throws Exception {
    MuleClient client = muleContext.getClient();
    client.dispatch("vm://receiver1?connector=Connector1", "Test", null);
    InternalMessage result = client.request("vm://out?connector=Connector2", RECEIVE_TIMEOUT).getRight().get();
    assertNotNull(result);
    assertEquals(getPayloadAsString(result), "Test");
  }
}
