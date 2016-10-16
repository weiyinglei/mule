/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.transformers;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import org.mule.runtime.core.transformer.simple.StringAppendTransformer;
import org.mule.test.AbstractIntegrationTestCase;

import org.junit.Test;

public class CustomTransformerTwoInstancesOfSameClass extends AbstractIntegrationTestCase {

  @Override
  protected String getConfigFile() {
    return "org/mule/test/transformers/custom-transformer-two-instances-same-class.xml";
  }

  @Test
  public void differentValues() throws Exception {
    final StringAppendTransformer appendStringA = muleContext.getRegistry().get("appendStringA");
    final StringAppendTransformer appendStringB = muleContext.getRegistry().get("appendStringB");

    assertThat(appendStringA.getMessage(), is(" A"));
    assertThat(appendStringB.getMessage(), is(" B"));
  }
}
