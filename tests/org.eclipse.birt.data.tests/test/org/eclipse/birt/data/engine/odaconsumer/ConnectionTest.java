/*
 * ************************************************************************
 * Copyright (c) 2004, 2005 Actuate Corporation. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Actuate Corporation - initial API and implementation
 * 
 * ************************************************************************
 */

package org.eclipse.birt.data.engine.odaconsumer;

import java.util.Properties;
import org.eclipse.birt.data.engine.core.DataException;
import org.eclipse.datatools.connectivity.oda.OdaException;
import java.sql.SQLException;

import testutil.JDBCOdaDataSource;

public class ConnectionTest extends ConnectionManagerTest
{

	private Connection m_connection;

	Connection getConnection( )
	{
		return m_connection;
	}

	protected void setUp( ) throws Exception
	{
		super.setUp( );

		Properties connProperties = getJdbcConnProperties();		
		m_connection = getManager( ).openConnection( JDBCOdaDataSource.DATA_SOURCE_TYPE, connProperties );
	}

	protected void tearDown( ) throws Exception
	{
		m_connection.close( );

		super.tearDown( );
	}

	Connection getMySqlConnection( ) throws DataException
	{
		Properties connProperties = new Properties( );
		connProperties.setProperty( "odaURL", "jdbc:mysql://qacat:3306/test" );
		connProperties.setProperty( "odaUser", "root" );
		connProperties.setProperty( "odaPassword", "root" );
		connProperties.setProperty( "odaDriverClass", "com.mysql.jdbc.Driver" );

		Connection connection = ConnectionManager.getInstance( )
				.openConnection( JDBCOdaDataSource.DATA_SOURCE_TYPE, connProperties );
		return connection;
	}

	public final void testGetMetaData( ) throws DataException
	{
		DataSetCapabilities capabilities = m_connection.getMetaData( "JDBC" );
		assertNotNull( capabilities );
	}

	public final void testMetaDataCache( ) throws DataException
	{
		DataSetCapabilities c1 = m_connection.getMetaData( "JDBC" );
		assertNotNull( c1 );

		DataSetCapabilities c2 = m_connection.getMetaData( "JDBC" );
		assertNotNull( c2 );

		assertSame( c1, c2 );
	}

	public final void testCreateStatement( ) throws DataException
	{
		String command = "select * from \"testtable\"";
		PreparedStatement stmt = m_connection.prepareStatement( command,
				JDBCOdaDataSource.DATA_SET_TYPE );
		assertNotNull( stmt );
		stmt.close( );
	}

	public final void testPrepareStatement( ) throws DataException
	{
		String command = "select * from \"testtable\" where \"intColumn\" > ?";
		PreparedStatement stmt = m_connection.prepareStatement( command,
				JDBCOdaDataSource.DATA_SET_TYPE );
		assertNotNull( stmt );
		stmt.close( );
	}

	public final void testPrepareStatementWithNullQuery( )
	{
        // since the oda consumer manager would have converted the 
        // null queryText to an empty string,
        // expects JDBC ODA driver to throw an OdaException with
	    // a SQLException cause
		String queryText = null;
		PreparedStatement stmt = null;
        try
        {
            stmt = m_connection.prepareStatement( queryText,
            		JDBCOdaDataSource.DATA_SET_TYPE );
        }
        catch( DataException ex )
        {
            assertNotNull( ex.getCause() );
            assertTrue( ex.getCause() instanceof OdaException );
            if( ex.getCause() instanceof OdaException )
            {
                OdaException driverException = (OdaException) ex.getCause();
                assertNotNull( driverException.getCause() );                
                assertTrue( driverException.getCause() instanceof SQLException );
            }       
        }

        assertNull( stmt );
	}

	public final void testClose( ) throws DataException
	{
		m_connection.close( );
	}

	public final void testGetMaxQueries( ) throws DataException
	{
		assertEquals( m_connection.getMaxQueries( ), 0 );
	}
}