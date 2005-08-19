/*******************************************************************************
 * Copyright (c) 2004, 2005 Actuate Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Actuate Corporation  - initial API and implementation
 *******************************************************************************/

package org.eclipse.birt.report.data.oda.jdbc;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.birt.data.oda.IConnection;
import org.eclipse.birt.data.oda.IDataSetMetaData;
import org.eclipse.birt.data.oda.IQuery;
import org.eclipse.birt.data.oda.OdaException;
import org.eclipse.birt.report.data.oda.i18n.ResourceConstants;

/**
 * Connection implements IConnection interface of ODA. It is a wrapper of JDBC
 * Connection.
 * 
 */
public class Connection implements IConnection
{
	/** The JDBC Connection instance. */
	private java.sql.Connection jdbcConn = null;
	
	private static Logger logger = Logger.getLogger( Connection.class.getName( ) );	
	
	// TODO:
	private final String simpleDataType = "org.eclipse.birt.report.data.oda.jdbc.JdbcSelectDataSet";
	private final String advancedDataType = "org.eclipse.birt.report.data.oda.jdbc.SPSelectDataSet";

	public void close( ) throws OdaException
	{
		logger.logp( java.util.logging.Level.FINE,
				Connection.class.getName( ),
				"close",
				"Connection closed." );
		if ( jdbcConn == null )
		{
			return;
		}
		try
		{
			jdbcConn.close( );
		}
		catch ( SQLException e )
		{
			throw new JDBCException( ResourceConstants.CONN_CANNOT_CLOSE, e );
		}
		jdbcConn = null;
	}

	public void commit( ) throws OdaException
	{
		logger.logp( java.util.logging.Level.FINE,
				Connection.class.getName( ),
				"commit",
				"Connection.commit()" );
		assertOpened( );
		try
		{
			jdbcConn.commit( );
		}
		catch ( SQLException e )
		{
			throw new JDBCException( ResourceConstants.CONN_COMMIT_ERROR, e );
		}
	}

	public IQuery newQuery( String dataSourceType )
			throws OdaException
	{
		logger.logp( java.util.logging.Level.FINE,
				Connection.class.getName( ),
				"createStatement",
				"Connection.createStatement(" + dataSourceType + ")" );
		// only one data source type, ignoring the argument.
		assertOpened( );
		if ( dataSourceType.equalsIgnoreCase( advancedDataType ) )
			return new CallStatement( jdbcConn );
		else
			return new Statement( jdbcConn );
	}


	/**
	 * @see org.eclipse.birt.data.oda.IConnection#getMetaData(java.lang.String)
	 */
	public IDataSetMetaData getMetaData( String dataSetType )
			throws OdaException
	{
		logger.logp( java.util.logging.Level.FINE,
				Connection.class.getName( ),
				"getMetaData",
				"Connection.getMetaData(" + dataSetType + ")" );
		// Only one data source type, ignoring the argument.
		DatabaseMetaData dbMetadata = null;
		if ( jdbcConn != null )
		{
			try
			{
				dbMetadata = jdbcConn.getMetaData( );
			}
			catch ( SQLException e )
			{
				throw new JDBCException( ResourceConstants.CONN_CANNOT_GET_METADATA, e );
			}
		}
		return new DataSourceMetaData( (IConnection) this, dbMetadata );
	}

	public boolean isOpen( ) throws OdaException
	{
		return ( jdbcConn != null );
	}

	public void open( Properties connProperties ) throws OdaException
	{
		if ( connProperties == null ){
			IllegalArgumentException e = new IllegalArgumentException("connProperties cannot be null");
			logger.logp( java.util.logging.Level.FINE,
					Connection.class.getName( ),
					"open",
					e.getMessage( ),
					e );
			throw e;
		}
		// Log connection information
		if ( logger.isLoggable( Level.INFO ) )
		{
			String logMsg = "Connection.open(Properties). connProperties = ";
			for ( Enumeration enumeration = connProperties.propertyNames( ); enumeration
					.hasMoreElements( ); )
			{
				String propName = (String) enumeration.nextElement( );
				// Don't log value of any property that looks like a password
				String lcPropName = propName.toLowerCase();
				String propVal; 
				if ( lcPropName.indexOf( "password" ) >= 0 ||
					 lcPropName.indexOf("pwd") >= 0 )
					propVal = "***";
				else
					propVal = connProperties.getProperty( propName );
				logMsg += propName + "=" + propVal + ";";
			}
			logger.logp( Level.FINE,
					Connection.class.getName( ),
					"open",
					logMsg );
		}
		
		close( );
		
		String dataSource = connProperties.getProperty( Constants.ODADataSource );
		if ( dataSource != null )
		{
			//TODO connect by DataSource
			UnsupportedOperationException e = new UnsupportedOperationException( "Oda-jdbc:connect by data source" );
			logger.logp( java.util.logging.Level.FINE,
					Connection.class.getName( ),
					"open",
					e.getMessage(),
					e );
			throw e; 
		}
		else
		{
			String url = connProperties.getProperty( Constants.ODAURL );
			if ( url == null || url.length() == 0 )
			{
				throw new JDBCException(
						ResourceConstants.DRIVER_MISSING_PROPERTIES,
						ResourceConstants.ERROR_MISSING_PROPERTIES );
			}
			connectByUrl( url, connProperties );
		}
	}
	
	/**
	 * Opens a JDBC connection using the specified url and connection properties 
	 * @param connProperies
	 */
	private void connectByUrl( String url, Properties connProperties ) throws OdaException
	{
		assert connProperties != null;
		assert url != null;
		
		// Copy connProperties to props; skip property starting with
		// "oda"; those are properties read by this driver
		Properties props = new Properties( );
		for ( Enumeration enumeration = connProperties.propertyNames( ); 
				enumeration.hasMoreElements( ); )
		{
			String propName = (String) enumeration.nextElement( );
			if ( !propName.startsWith( "oda" ) )
			{
				props.setProperty( propName, connProperties
						.getProperty( propName ) );
			}
		}
				
		// Read user name and password
		String user = connProperties.getProperty( Constants.ODAUser );
		if ( user != null )
			props.setProperty("user", user);
		String pwd = connProperties.getProperty( Constants.ODAPassword );
		if ( pwd != null )
				props.setProperty("password", pwd);

		String driverClass = connProperties.getProperty( Constants.ODADriverClass );
		
		try
		{
			jdbcConn = JDBCDriverManager.getInstance().
					getConnection( driverClass, url, props );
		}
		catch ( SQLException e )
		{
			throw new JDBCException( ResourceConstants.CONN_CANNOT_GET, e );
		}
	}

	/**
	 * 
	 */
	public void rollback( ) throws OdaException
	{
		logger.logp( java.util.logging.Level.FINE,
				Connection.class.getName( ),
				"rollback",
				"Connection.rollback()" );
		assertOpened( );
		try
		{
			jdbcConn.rollback( );
		}
		catch ( SQLException e )
		{
			throw new JDBCException( ResourceConstants.CONN_ROLLBACK_ERROR, e );
		}
	}

	
	/** 
	 * @see org.eclipse.birt.data.oda.IConnection#getMaxQueries()
	 */
	public int getMaxQueries() throws OdaException 
	{
		if ( jdbcConn != null )
		{
			try
			{
				DatabaseMetaData dbMetadata = jdbcConn.getMetaData( );
				return dbMetadata.getMaxStatements();
			}
			catch ( SQLException e )
			{
				throw new JDBCException( ResourceConstants.CANNOT_GET_MAXQUERIES, e );
			}
		}
		
		return 0;
	}
	
	/**
	 * Assert the connection is opened.
	 * 
	 * @throws JDBCException
	 */
	private void assertOpened( ) throws OdaException
	{
		if ( jdbcConn == null )
		{
			throw new JDBCException( ResourceConstants.DRIVER_NO_CONNECTION,
					ResourceConstants.ERROR_NO_CONNECTION );
		}
	}
	
	/**
	 *	define constants  ODAURL, ODAPassword, ODAUser, ODADriverClass, ODADataSource
	 */
	public static class Constants
	{
	    public static final String ODAURL = "odaURL";
	    public static final String ODAPassword = "odaPassword";
	    public static final String ODAUser = "odaUser";
	    public static final String ODADriverClass = "odaDriverClass";
	    public static final String ODADataSource ="odaDataSource";
	}
	
}
