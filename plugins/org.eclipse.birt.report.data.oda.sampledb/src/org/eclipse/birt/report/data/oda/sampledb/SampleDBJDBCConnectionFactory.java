/*
 *************************************************************************
 * Copyright (c) 2005 Actuate Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Actuate Corporation  - initial API and implementation
 *  
 *************************************************************************
 */
package org.eclipse.birt.report.data.oda.sampledb;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.birt.report.data.oda.jdbc.IConnectionFactory;
import org.eclipse.birt.report.data.oda.jdbc.JDBCDriverManager;
import org.eclipse.datatools.connectivity.oda.OdaException;

public class SampleDBJDBCConnectionFactory implements IConnectionFactory
{
	private static final Logger logger = Logger.getLogger( SampleDBJDBCConnectionFactory.class.getName( ) );
	
	/**
	 * Creates a new JDBC connection to the embedded sample database.
	 * 
	 * @see org.eclipse.birt.report.data.oda.jdbc.IConnectionFactory#getConnection(java.lang.String,
	 *      java.lang.String, java.util.Properties)
	 */
	public Connection getConnection( String driverClass, String url,
			Properties connectionProperties ) throws SQLException
	{
		if ( ! driverClass.equals( SampleDBConstants.DRIVER_CLASS) )
		{
			// This is unexpected; we shouldn't be getting this call
			logger.log( Level.SEVERE, "Unexpected driverClass: " + driverClass );
			throw new SQLException("Unexpected driverClass " + driverClass);
		}
		if ( ! url.equals(SampleDBConstants.DRIVER_URL) )
		{
			// Wrong url
			logger.log( Level.WARNING, "Unexpected url: " + url );
			throw new SQLException("Classic Models Inc. Sample Database Driver does not recognize url: " + driverClass);
		}

		String dbUrl = getDbUrl();
		
		// Copy connection properties and replace user and password with fixed value
		Properties props;
		if ( connectionProperties != null )
			props = (Properties)connectionProperties.clone();
		else 
			props = new Properties(); 
		props.put( "user", SampleDBConstants.SAMPLE_DB_SCHEMA);
		props.put( "password", "" );
		
		try
		{
			return JDBCDriverManager.getInstance().getConnection( 
					SampleDBConstants.DERBY_DRIVER_CLASS, dbUrl, props );
		}
		catch (OdaException e)
		{
			throw new SQLException(e.getLocalizedMessage());
		}
	}
	
	/**
	 * @return Url to be used with Derby Embedded driver to connect to embedded
	 *         database
	 */
	private static String getDbUrl( )
	{
		return SampleDBConstants.dbUrl;
	}

	/**
	 * @return user name for db connection
	 */
	public static String getDbUser( )
	{
		return SampleDBConstants.SAMPLE_DB_SCHEMA;
	}
	
}
