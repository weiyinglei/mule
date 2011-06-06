/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.jdbc.config;

import org.mule.tck.FunctionalTestCase;

import java.sql.Connection;

import org.enhydra.jdbc.standard.StandardDataSource;

public class JdbcDataSourceNamespaceHandlerTestCase extends FunctionalTestCase
{
    public JdbcDataSourceNamespaceHandlerTestCase()
    {
        super();
        setStartContext(false);
    }

    @Override
    protected String getConfigResources()
    {
        return "jdbc-data-source-namespace-config.xml";
    }

    public void testSingleton()
    {
        StandardDataSource ds1 = lookupDataSource("default-oracle");
        StandardDataSource ds2 = lookupDataSource("default-oracle");
        assertSame(ds1, ds2);
    }

    public void testCustomDataSourceProperties()
    {
        StandardDataSource source = lookupDataSource("custom-ds-properties");
        assertEquals(Connection.TRANSACTION_SERIALIZABLE, source.getTransactionIsolation());
        assertEquals(42, source.getLoginTimeout());
    }

    public void testOracleDefaults()
    {
        StandardDataSource source = lookupDataSource("default-oracle");
        assertEquals("jdbc:oracle:thin:@localhost:1521:orcl", source.getUrl());
        assertEquals("oracle.jdbc.driver.OracleDriver", source.getDriverName());
        assertEquals(-1, source.getTransactionIsolation());
        assertEquals("scott", source.getUser());
        assertEquals("tiger", source.getPassword());
    }

    public void testOracleCustomUrl()
    {
        StandardDataSource source = lookupDataSource("custom-url-oracle");
        assertEquals("jdbc:oracle:thin:@some-other-host:1522:mule", source.getUrl());
    }

    public void testOracleCustomHost()
    {
        StandardDataSource source = lookupDataSource("custom-host-oracle");
        assertEquals("jdbc:oracle:thin:@some-other-host:1521:orcl", source.getUrl());
    }

    public void testOracleCustomPort()
    {
        StandardDataSource source = lookupDataSource("custom-port-oracle");
        assertEquals("jdbc:oracle:thin:@localhost:1522:orcl", source.getUrl());
    }

    public void testOracleCustomInstance()
    {
        StandardDataSource source = lookupDataSource("custom-instance-oracle");
        assertEquals("jdbc:oracle:thin:@localhost:1521:mule", source.getUrl());
    }

    public void testMysqlDefaults()
    {
        StandardDataSource source = lookupDataSource("default-mysql");
        assertEquals("jdbc:mysql://localhost/mule", source.getUrl());
        assertEquals("com.mysql.jdbc.Driver", source.getDriverName());
        assertEquals("mysql", source.getUser());
        assertEquals("secret", source.getPassword());
    }

    public void testMysqlCustomUrl()
    {
        StandardDataSource source = lookupDataSource("custom-url-mysql");
        assertEquals("jdbc:mysql://mule-db-host:3306/mule", source.getUrl());
    }

    public void testMysqlCustomHost()
    {
        StandardDataSource source = lookupDataSource("custom-host-mysql");
        assertEquals("jdbc:mysql://some-other-host/mule", source.getUrl());
    }

    public void testMysqlCustomPort()
    {
        StandardDataSource source = lookupDataSource("custom-port-mysql");
        assertEquals("jdbc:mysql://localhost:4242/mule", source.getUrl());
    }

    private StandardDataSource lookupDataSource(String key)
    {
        Object object = muleContext.getRegistry().lookupObject(key);
        assertNotNull(object);
        assertTrue(object instanceof StandardDataSource);

        return (StandardDataSource) object;
    }
}
