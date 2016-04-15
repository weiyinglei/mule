/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.metadata.extension.resolver;

import org.mule.api.connection.ConnectionException;
import org.mule.api.metadata.MetadataContext;
import org.mule.api.metadata.MetadataKey;
import org.mule.api.metadata.MetadataResolvingException;
import org.mule.api.metadata.resolving.MetadataOutputResolver;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.module.extension.internal.metadata.TestMetadataUtils;

public class TestOutputResolverWithoutKeyResolver implements MetadataOutputResolver
{

    @Override
    public MetadataType getOutputMetadata(MetadataContext context, MetadataKey key) throws MetadataResolvingException, ConnectionException
    {
        return TestMetadataUtils.getMetadata(TestMetadataUtils.getKeys(context).get(0));
    }
}