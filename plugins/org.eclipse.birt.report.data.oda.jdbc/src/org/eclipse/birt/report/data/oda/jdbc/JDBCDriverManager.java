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

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.CodeSource;
import java.security.PermissionCollection;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.birt.core.framework.URLClassLoader;
import org.eclipse.birt.report.data.oda.i18n.JdbcResourceHandle;
import org.eclipse.birt.report.data.oda.i18n.ResourceConstants;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.datatools.connectivity.oda.OdaException;
import org.osgi.framework.Bundle;

import com.ibm.icu.util.ULocale;

/**
 * Utility classs that manages the JDBC drivers available to this bridge driver.
 * Deals with dynamic discovery of JDBC drivers and some annoying class loader
 * issues. 
 * This class is not to be instantiated by the user. Use the getInstance() method
 * to obtain an instance
 */
public class JDBCDriverManager
{    
    private static final int MAX_WORD_LENGTH = 20;
	private static final int MAX_MSG_LENGTH = 300;
	
	public static final String JDBC_USER_PROP_NAME = "user"; //$NON-NLS-1$
    public static final String JDBC_PASSWORD_PROP_NAME = "password"; //$NON-NLS-1$
    public static final String DRIVER_REGISTERED = "registered";
    public static final String DRIVER_DEREGISTERED = "deregistered";
    public static final String DRIVER_RELOAD = "registerAfterDeregistered";

    // Driver classes that we have registered with JDBC DriverManager
	private  HashMap registeredDrivers = new HashMap();
	
	//
	private HashMap cachedDriversMap = new HashMap();
	
	// A HashMap of JDBC driver instances
	private HashMap<String, Driver> cachedJdbcDriver = new HashMap<String, Driver>( );

	// A HashMap of driverinfo extensions which provides IConnectionFactory implementation
	// Map is from driverClass (String) to either IConfigurationElement or IConnectionFactory 
	private HashMap driverExtensions = null;
	
	private DriverClassLoader extraDriverLoader = null;
	
	//The resource handle.
	private JdbcResourceHandle resourceHandle = new JdbcResourceHandle(ULocale
			.getDefault());
	
	private static JDBCDriverManager instance;
	
	private static Logger logger = Logger.getLogger( JDBCDriverManager.class.getName() );
	
	private JDBCDriverManager()
	{
		logger.logp( java.util.logging.Level.FINE,
				OdaJdbcDriver.class.getName( ),
				"JDBCDriverManager",
				"JDBCDriverManager starts up" );
	}
	
	public synchronized static JDBCDriverManager getInstance()
	{
		if ( instance == null )
			instance = new JDBCDriverManager();
		return instance;
	}
	
	public Driver getDriverInstance( Class driver, boolean refreshDriver ) throws OdaException
	{
		String driverName = driver.getName( );
		if ( refreshDriver || !this.cachedJdbcDriver.containsKey( driverName ) )
		{
			Driver instance = null;
			try
			{
				instance = (Driver) driver.newInstance( );
			}
			catch ( Exception e )
			{
				throw new OdaException( e );
			}
			this.cachedJdbcDriver.put( driverName, instance );
			return instance;
		}
		else
		{
			return this.cachedJdbcDriver.get( driverName );
		}
	}
	
	/**
	 * Release all the resources 
	 */
	public void close()
	{
		if( this.extraDriverLoader != null )
		{
			this.extraDriverLoader.close();
			this.extraDriverLoader = null;
		}
		this.cachedJdbcDriver.clear( );
		this.cachedDriversMap.clear( );
		this.registeredDrivers.clear( );
	}
	
	/**
	 * Gets a JDBC connection 
	 * @param driverClass Class name of JDBC driver
	 * @param url Connection URL
	 * @param connectionProperties Properties for establising connection
	 * @return new JDBC Connection
	 * @throws SQLException
	 */
	public Connection getConnection( String driverClass, String url, 
			Properties connectionProperties, Collection<String> driverClassPath ) throws SQLException, OdaException
	{
		validateConnectionProperties( driverClass, url, null );
		if ( logger.isLoggable( Level.FINE ) )
		{
			logger.fine( "Request JDBC Connection: driverClass="
					+ ( driverClass == null ? "" : driverClass ) + "; url="
					+ LogUtil.encryptURL( url ) );
		}
		return doConnect( driverClass, url, null, connectionProperties, driverClassPath );
	}

	/**
	 * Gets a JDBC connection 
	 * @param driverClass Class name of JDBC driver
	 * @param url Connection URL
	 * @param user connection user name
	 * @param password connection password
	 * @return new JDBC connection
	 * @throws SQLException
	 */
	public  Connection getConnection( String driverClass, String url, 
			String user, String password, Collection<String> driverClassPath ) throws SQLException, OdaException
	{
		validateConnectionProperties( driverClass, url, null );
		if ( logger.isLoggable( Level.FINE ) )
			logger.fine( "Request JDBC Connection: driverClass="
					+ ( driverClass == null ? "" : driverClass ) + "; url="
					+ LogUtil.encryptURL( url ) + "; user="
					+ ( ( user == null ) ? "" : user ) );
		
		// Construct a Properties list with user/password properties
		Properties props = addUserAuthenticationProperties( null, user, password );
        
        return doConnect( driverClass, url, null, props, driverClassPath );
	}

    /**
     * Gets a JDBC connection from the specified JNDI data source URL, or
     * if not available, directly from the specified driver and JDBC driver url.
     * @param driverClass   the class name of JDBC driver
     * @param url           JDBC connection URL
     * @param jndiNameUrl   the JNDI name to look up a Data Source name service; 
	 *						may be null or empty
     * @param connectionProperties  properties for establising connection
     * @return              a JDBC connection
     * @throws SQLException
     * @throws OdaException
     */
    public Connection getConnection( String driverClass, String url, 
                                String jndiNameUrl,
                                Properties connectionProperties, Collection<String> driverClassPath ) 
        throws SQLException, OdaException
    {
		validateConnectionProperties( driverClass, url, jndiNameUrl );
        if ( logger.isLoggable( Level.FINE ) )
            logger.fine( "Request JDBC Connection: driverClass=" + driverClass +  //$NON-NLS-1$
                        "; url=" + LogUtil.encryptURL( url )+            //$NON-NLS-1$
                        "; jndi name url=" + jndiNameUrl );  //$NON-NLS-1$
        
        return doConnect( driverClass, url, jndiNameUrl, connectionProperties, driverClassPath );
    }
	
	/**
	 * Implementation of getConnection() methods. Gets connection from either java.sql.DriverManager, 
	 * or from IConnectionFactory defined in the extension
	 */    
    private synchronized Connection doConnect( String driverClass, String url, 
            String jndiNameUrl,
            Properties connectionProperties, Collection<String> driverClassPath ) throws SQLException, OdaException
    {
		// JNDI should take priority when getting connection.If JNDI Data Source
		// URL is defined, try use name service to get connection
		Connection jndiDSConnection = getJndiDSConnection( driverClass,
				jndiNameUrl,
				connectionProperties );

		if ( jndiDSConnection != null ) // successful
			return jndiDSConnection; // done
		
		IConnectionFactory factory = getDriverConnectionFactory (driverClass);
		if ( factory != null )
		{
			// Use connection factory for connection
			if ( logger.isLoggable( Level.FINER ))
				logger.finer( "Calling IConnectionFactory.getConnection. driverClass=" + driverClass + //$NON-NLS-1$
						", url=" + LogUtil.encryptURL( url ) ); //$NON-NLS-1$
			return factory.getConnection( driverClass, url, connectionProperties );
		}
        
        // no driverinfo extension for driverClass connectionFactory       
        // no JNDI Data Source URL defined, or 
        // not able to get a JNDI data source connection, 
        // use the JDBC DriverManager instead to get a JDBC connection
		loadAndRegisterDriver( driverClass, driverClassPath );
		if ( logger.isLoggable( Level.FINER ))
			logger.finer( "Calling DriverManager.getConnection. url=" + LogUtil.encryptURL( url ) ); //$NON-NLS-1$
		try
		{
			return DriverManager.getConnection( url, connectionProperties );
		}
		catch ( Exception e )
		{
			registerDriver( driverClass, null, true, true );
			try
			{
				return DriverManager.getConnection( url, connectionProperties );
			}
			catch ( Exception ex )
			{
				throw new JDBCException( ResourceConstants.CONN_GET_ERROR,
						null,
						truncate( ex.getLocalizedMessage( ) ) );
			}
		}
	}

    /**
     * 
     * @param message
     * @return
     */
	private String truncate( String message )
	{
		if ( message == null )
			return null;
		if ( message.length( ) > MAX_MSG_LENGTH )
		{
			int maxLength = MAX_MSG_LENGTH + MAX_WORD_LENGTH;
			if ( maxLength > message.length( ) )
			{
				maxLength = message.length( );
			}
			boolean findBoundary = false;
			int i = MAX_MSG_LENGTH;
			for ( ; !findBoundary && i < maxLength; i++ )
			{
				char c = message.charAt( i );
				switch ( c )
				{
					case ' ' : findBoundary = true; break;
					case '\t': findBoundary = true; break;
					case '.' : findBoundary = true; break;
					case ',' : findBoundary = true; break;
					case ':' : findBoundary = true; break;
					case ';' : findBoundary = true; break;
				}
			}
			message = message.substring( 0, i ) + " ...";
		}
		return message;
	}

    /**
     * Obtain a JDBC connection from a Data Source connection factory
     * via the specified JNDI name service. 
     * May return null if no JNDI Name URL is specified, or not able to obtain
     * a connection from the JNDI name service.
     */
    private Connection getJndiDSConnection( String driverClass, 
                                            String jndiNameUrl, 
                                            Properties connectionProperties )
    {
        if ( jndiNameUrl == null || jndiNameUrl.length() == 0 )
            return null;    // no JNDI Data Source URL defined
        
        if ( logger.isLoggable( Level.FINER ))
            logger.finer( "Calling getJndiDSConnection: JNDI name url=" + jndiNameUrl ); //$NON-NLS-1$

        IConnectionFactory factory = new JndiDataSource();            
        Connection jndiDSConnection = null;
        try
        {
            jndiDSConnection = factory.getConnection( driverClass, jndiNameUrl, 
                                        connectionProperties );
        }
        catch( SQLException e )
        {
            // log and ignore exception
            if ( logger.isLoggable( Level.FINE ))
                logger.info( "getJndiDSConnection: Unable to get JNDI data source connection; " + e.toString() ); //$NON-NLS-1$
        }
        
        return jndiDSConnection;
    }
    
    /**
     * Adds the specified user name and password to the
     * specified collection as Jdbc driver properties.
     * Creates a new collection if none is specified.
     */
    static Properties addUserAuthenticationProperties( Properties connProps,
			String user, String password )
	{
		if ( connProps == null )
			connProps = new Properties( );
		if ( user != null )
			connProps.setProperty( JDBC_USER_PROP_NAME, user );
		if ( password != null )
			connProps.setProperty( JDBC_PASSWORD_PROP_NAME, password );
		return connProps;
	}       
    
   /**
	 * Validate the driver class name, URL & Jndi properties.
	 * 
	 * @param url
	 * @param jndiNameUrl
	 */
	private void validateConnectionProperties( String driverClass, String url, String jndiNameUrl )
	{
		if ( isBlank( jndiNameUrl) && isBlank( driverClass ) )
			throw new NullPointerException( this.resourceHandle.getMessage( ResourceConstants.EMPTYDRIVERCLASS ) );
		
		if ( isBlank( url ) && isBlank( jndiNameUrl ) )
			throw new NullPointerException( this.resourceHandle.getMessage( ResourceConstants.MISSEDURLANDJNDI ) );
	}

    /**
	 * 
	 * @param url
	 * @return
	 */
	private boolean isBlank(String url) {
		return url == null || url.trim().toString().length() == 0;
	}
    
	/** 
	 * Searches extension registry for connection factory defined for driverClass. Returns an 
	 * instance of the factory if there is a connection factory for the driver class. Returns null
	 * otherwise.
	 */
	public IConnectionFactory getDriverConnectionFactory( String driverClass ) throws OdaException
	{
		loadDriverExtensions();
		
		IConnectionFactory factory = null;
		Object driverInfo = null;
		if ( driverClass != null )
			driverInfo = driverExtensions.get( driverClass);
		
		if ( driverInfo != null )
		{
			// Driver has own connection factory; use it
			if ( driverInfo instanceof IConfigurationElement )
			{
				// connectionFactory not yet created; do it now
				String factoryClass = ( (IConfigurationElement) driverInfo ).getAttribute( OdaJdbcDriver.Constants.DRIVER_INFO_ATTR_CONNFACTORY );
				try
				{
					factory = (IConnectionFactory) ( (IConfigurationElement) driverInfo ).createExecutableExtension( OdaJdbcDriver.Constants.DRIVER_INFO_ATTR_CONNFACTORY );

					logger.fine( "Created connection factory class "
							+ factoryClass + " for driverClass " + driverClass );
				}
				catch ( CoreException e )
				{
					JDBCException ex = new JDBCException( ResourceConstants.CANNOT_INSTANTIATE_FACTORY,
							null,
							new Object[]{
									factoryClass, driverClass
							} );
					logger.log( Level.WARNING,
							"Failed to instantiate connection factory for driverClass "
									+ driverClass,
							ex );
					throw ex;
				}
				assert ( factory != null );
				// Cache factory instance
				driverExtensions.put( driverClass, factory);
			}
			else
			{
				// connectionFactory already created
				assert driverInfo instanceof IConnectionFactory;
				factory = (IConnectionFactory) driverInfo;
			}
		}
		
		return factory;
	}
	
	private void loadDriverExtensions()
	{
		if ( driverExtensions != null )
			// Already loaded
			return;
		
		// First time: load all driverinfo extensions
		driverExtensions = new HashMap();
		IExtensionRegistry extReg = Platform.getExtensionRegistry();

		/* 
		 * getConfigurationElementsFor is not working for server Platform. 
		 * I have to work around this by walking the extension list
		IConfigurationElement[] configElems = 
			extReg.getConfigurationElementsFor( OdaJdbcDriver.Constants.DRIVER_INFO_EXTENSION );
		*/
		IExtensionPoint extPoint = 
			extReg.getExtensionPoint( OdaJdbcDriver.Constants.DRIVER_INFO_EXTENSION );
		
		if ( extPoint == null )
			return;
		
		IExtension[] exts = extPoint.getExtensions();
		if ( exts == null )
			return;
		
		for ( int e = 0; e < exts.length; e++)
		{
			IConfigurationElement[] configElems = exts[e].getConfigurationElements(); 
			if ( configElems == null )
				continue;
			
			for ( int i = 0; i < configElems.length; i++ )
			{
				if ( configElems[i].getName().equals( 
						OdaJdbcDriver.Constants.DRIVER_INFO_ELEM_JDBCDRIVER) )
				{
					String driverClass = configElems[i].getAttribute( 
							OdaJdbcDriver.Constants.DRIVER_INFO_ATTR_DRIVERCLASS );
					String connectionFactory = configElems[i].getAttribute( 
							OdaJdbcDriver.Constants.DRIVER_INFO_ATTR_CONNFACTORY );
					logger.info("Found JDBC driverinfo extension: driverClass=" + driverClass +
							", connectionFactory=" + connectionFactory );
					if ( driverClass != null && driverClass.length() > 0 &&
						 connectionFactory != null && connectionFactory.length() > 0 )
					{
						// This driver class has its own connection factory; cache it
						// Note that the instantiation of the connection factory can wait
						// until we actually need it
						driverExtensions.put( driverClass, configElems[i] );
					}
				}
			}
		}
	}
	
	public void updateStatusForRegister( String className )
	{
		if ( isDeregistered( className ) )
		{
			registeredDrivers.put( className, DRIVER_RELOAD );
		}
	}

	/**
	 * The method which test whether the give connection properties can be used
	 * to create a connection
	 * 
	 * @param driverClassName
	 *            the name of driver class
	 * @param connectionString
	 *            the connection URL
	 * @param userId
	 *            the user id
	 * @param password
	 *            the pass word
	 * @return boolean whether could the connection being created
	 * @throws OdaException
	 */
	public boolean testConnection( String driverClassName,
			String connectionString, String userId, String password )
			throws OdaException
	{
        return testConnection( driverClassName, connectionString, null,
                                userId, password );
    }
    
    /**
     * Tests whether the given connection properties can be used to obtain a connection.
     * @param driverClassName the name of driver class
     * @param connectionString the JDBC driver connection URL
     * @param jndiNameUrl   the JNDI name to look up a Data Source name service; 
	 *						may be null or empty
     * @param userId        the login user id
     * @param password      the login password
     * @return  true if the the specified properties are valid to obtain a connection;
     *          false otherwise
     * @throws OdaException 
     */
    public boolean testConnection( String driverClassName,
            String connectionString, String jndiNameUrl,
            String userId, String password )
        throws OdaException
    {		
		boolean canConnect = false;
		try
		{
			//	If connection is provided by driverInfo extension, use it to create connection
			if ( getDriverConnectionFactory( driverClassName ) != null )
			{
				tryCreateConnection( driverClassName, connectionString, userId, password);
				return true;
			}
            
            // no driverinfo extension for driverClass connectionFactory
            
            // if JNDI Data Source URL is defined, try use name service to get connection
            if ( jndiNameUrl != null )
            {
                Connection jndiDSConnection = 
                    getJndiDSConnection( driverClassName, jndiNameUrl, 
                            addUserAuthenticationProperties( null, userId, password ) );            

                if ( jndiDSConnection != null ) // test connection successful
				{
					closeConnection( jndiDSConnection );
					return true;
				}
				else if ( connectionString != null
						&& connectionString.trim( ).length( ) > 0 )
				{
					return testConnection( driverClassName,
							connectionString,
							userId,
							password );
				}
				else
				{
					throw new JDBCException( ResourceConstants.CANNOT_PARSE_JNDI,
							null );
				}
            }
			
            // no JNDI Data Source URL defined, or
            // not able to get a JNDI data source connection,
            // use the JDBC DriverManager instead to get a JDBC connection
			loadAndRegisterDriver( driverClassName, null );

			// If the connections built upon the given driver has been tested
			// once,
			// it will be add to cachedDrivers hashmap so that next time we can
			// directly
			// test the connection using specific driver rather than iterate all
			// available
			// drivers in DriverManager

			if ( cachedDriversMap.get( driverClassName ) == null )
			{
				Enumeration enumeration = DriverManager.getDrivers( );
				while ( enumeration.hasMoreElements( ) )
				{
					Driver driver = (Driver) enumeration.nextElement( );

					// The driver might be a wrapped driver. The toString()
					// method
					// of a wrapped driver is overriden
					// so that the name of driver being wrapped is returned.
					if ( isExpectedDriver( driver, driverClassName ) )
					{
						if ( driver.acceptsURL( connectionString ) )
						{
							cachedDriversMap.put( driverClassName, driver );
							// if connection can be built then the test
							// connection
							// succeed. Otherwise Exception would be thrown. The
							// source
							// of the exception is
							tryCreateConnection( driverClassName,
									connectionString,
									userId,
									password );
							canConnect = true;
							break;
						}
					}
				}
				// If the test url can be accepted by DriverManager (because the
				// driver which can pass
				// that url has been registered.) but a connection
				// cannot be built using driver whose name is given, throw a
				// exception.
				if ( !canConnect )
					throw new JDBCException( ResourceConstants.CANNOT_PARSE_URL,
							null );
			}
			else
			{
				if ( ( (Driver) this.cachedDriversMap.get( driverClassName ) ).acceptsURL( connectionString ) )
				{
					tryCreateConnection( driverClassName,
							connectionString,
							userId,
							password );
					canConnect = true;
				}
			}
		}
		catch ( SQLException e )
		{
			throw new JDBCException( e.getLocalizedMessage( ), null );
		}
		// If the given url cannot be parsed.
		if ( canConnect == false )
			throw new JDBCException( ResourceConstants.NO_SUITABLE_DRIVER, null );

		return true;
	}
	
    private void closeConnection( Connection conn )
    {
        if ( conn == null )
            return;     // nothing to close
        
        try
        {
            conn.close();
        }
        catch( SQLException e )
        { 
            /* ok to ignore */ 
        }    
    }
    
	private boolean isExpectedDriver( Driver driver, String className )
	{
		String actual;
		if ( driver instanceof WrappedDriver )
			actual = driver.toString();
		else
			actual = driver.getClass( ).getName( );
		return isExpectedDriverClass( actual, className); 
	}
	
	private boolean isExpectedDriverClass( String actual, String expected)
	{
		// Normally, a driver class registers itself with DriverManager
		// when it is loaded. However, at least one driver (Derby Embedded)
		// registers a different driver class with DriverManager (Driver30) than the 
		// documented driver class (EmbeddedDriver). We have to relaxed
		// the rules here to determin if driver is registered by className.
		// As long as driver's class and className has same package name
		// we consider them compatible.
		// Not a perfect solution but so far works with drivers we've tested
		String actualPkg = actual.substring(0, actual.lastIndexOf('.') );
		String expectedPkg = expected.substring(0, expected.lastIndexOf('.') );
		return actualPkg.equals( expectedPkg );
	}
	
	/**
	 * Try to create a connection based on given connection properties.
	 * @param driverClassName
	 * @param connectionString
	 * @param userId
	 * @param password
	 * @throws SQLException
	 * @throws OdaException
	 */
	private void tryCreateConnection( String driverClassName,
			String connectionString, String userId, String password )
			throws SQLException, OdaException
	{
		Connection testConn = this.getConnection( driverClassName, 
				connectionString, userId, password, null );
		assert ( testConn != null );
        closeConnection( testConn );
	}
	
	/**
	 * Look for the Driver from drivers directory if it not in plugin class path
	 * 
	 * @param className
	 * @return Driver instance
	 * @throws OdaException 
	 */
	private Driver findDriver( String className, Collection<String> driverClassPath, boolean refreshClassLoader, boolean refreshDriver ) throws OdaException
	{
		Class driverClass = null;
		try
		{
			driverClass = Class.forName( className );
			// Driver class in class path
			logger.info( "Loaded JDBC driver class in class path: " + className );
		}
		catch ( ClassNotFoundException e )
		{
			if ( logger.isLoggable( Level.FINE ) )
			{
				logger.info( "Driver class not in class path: "
						+ className
						+ ". Trying to locate driver in drivers directory" );
			}

			// Driver not in plugin class path; find it in drivers directory
			driverClass = loadExtraDriver( className, true, refreshClassLoader, driverClassPath );

			// if driver class still cannot be found,
			if ( driverClass == null )
			{
				ClassLoader loader = Thread.currentThread( ).getContextClassLoader( );
				if ( loader != null )
				{
					try
					{
						driverClass = Class.forName( className, true, loader );
					}
					catch ( ClassNotFoundException e1 )
					{
						driverClass = null;
					}
				}
			}
		}
		if ( driverClass == null )
		{
			logger.warning( "Failed to load JDBC driver class: " + className );
			throw new JDBCException( ResourceConstants.CANNOT_LOAD_DRIVER,
					null,
					className );
		}
		Driver driver = null;
		try
		{
			driver = this.getDriverInstance( driverClass, refreshDriver );
		}
		catch ( Exception e )
		{
			logger.log( Level.WARNING, "Failed to create new instance of JDBC driver:" + className, e);
			throw new JDBCException( ResourceConstants.CANNOT_INSTANTIATE_DRIVER, null, className );
		}
		return driver;
	}
	
	
	/**
	 * Deregister the driver by the given class name from DriverManager
	 * 
	 * @param className
	 * @return true if deregister the driver successfully
	 * @throws OdaException
	 */
	public boolean deregisterDriver( String className ) throws OdaException
	{
		if ( className == null || className.length() == 0)
			// no driver class; assume class already deregistered
			return false;
		if ( isDeregistered( className ) )
		{
			return true;
		}
		Driver driver = findDriver( className, null, false, false );
		if ( driver != null )
		{
			try
			{
				if (logger.isLoggable(Level.FINER))
					logger.finer("Registering with DriverManager: wrapped driver for " + className );
				if ( registeredDrivers.containsKey( className ) )
				{
					DriverManager.deregisterDriver( new WrappedDriver( driver, className ) );
					registeredDrivers.remove( className );
				}
				registeredDrivers.put( className, DRIVER_DEREGISTERED );
			}
			catch ( SQLException e)
			{
				// This shouldn't happen
				logger.log( Level.WARNING, 
						"Failed to deRegister wrapped driver instance.", e);
			}
		}
		return true;
	}
	
	/**
	 * Update the driver by the given class name from DriverManager
	 * 
	 * @param String className
	 */
	public void updateStatus( String className )
	{
		if ( isDeregistered( className ) )
		{
			registeredDrivers.remove( className );
		}
	}
	
	/**
	 * Check whether the driver class name is deregistered
	 * 
	 * @param String className
	 * @return true if the driver class name is deregistered
	 */
	private boolean isDeregistered( String className )
	{
		return this.registeredDrivers.containsKey( className )
				&& registeredDrivers.get( className ).equals( DRIVER_DEREGISTERED );
	}
	
	public void loadAndRegisterDriver( String className, Collection<String> driverClassPath ) 
		throws OdaException
	{
		if ( className == null || className.length() == 0)
			// no driver class; assume class already loaded
			return;
		if ( isDeregistered( className ) )
		{
			throw new JDBCException( ResourceConstants.CANNOT_LOAD_DRIVER,
					null,
					className );
		}
		else if ( this.registeredDrivers.containsKey( className ) )
		{
			return;
		}
		if ( logger.isLoggable( Level.FINE ))
		{
			logger.info( "Loading JDBC driver class: " + className );
		}		
		registerDriver( className, driverClassPath, false, false );
	}
	
	/**
	 * If driver is found in the drivers directory, its class is not accessible
	 * in this class's ClassLoader. DriverManager will not allow this class to create
	 * connections using such driver. To solve the problem, we create a wrapper Driver in 
	 * our class loader, and register it with DriverManager
	 * 
	 * @param className
	 * @param driverClassPath
	 * @param refreshClassLoader
	 * @throws OdaException
	 */
	private void registerDriver( String className,
			Collection<String> driverClassPath, boolean refreshClassLoader,
			boolean refreshDriver ) throws OdaException
	{
		Driver driver = findDriver( className,
				driverClassPath,
				refreshClassLoader,
				refreshDriver );
		if ( driver != null )
		{
			try
			{
				if ( logger.isLoggable( Level.FINER ) )
					logger.finer( "Registering with DriverManager: wrapped driver for "
							+ className );
				DriverManager.registerDriver( new WrappedDriver( driver,
						className ) );
			}
			catch ( SQLException e )
			{
				// This shouldn't happen
				logger.log( Level.WARNING,
						"Failed to register wrapped driver instance.",
						e );
			}
		}
		registeredDrivers.put( className, DRIVER_REGISTERED );
	}

	/**
	 * Search driver in the "drivers" directory and load it if found
	 * @param className
	 * @return
	 * @throws OdaException 
	 * @throws DriverException
	 * @throws OdaException
	 */
	private Class loadExtraDriver(String className, boolean refreshUrlsWhenFail, boolean refreshClassLoader, Collection<String> driverClassPath) throws OdaException
	{
		assert className != null;
		
		if ( extraDriverLoader == null || refreshClassLoader )
		{
			if( extraDriverLoader!= null )
				extraDriverLoader.close( );
			extraDriverLoader = new DriverClassLoader( driverClassPath );
		}

		try
		{
			return extraDriverLoader.loadClass(className);
		}
		catch ( ClassNotFoundException e )
		{
			logger.log( Level.SEVERE, "DriverClassLoader failed to load class: " + className, e );
			logger.log( Level.SEVERE, "refreshUrlsWhenFail: " +  refreshUrlsWhenFail);
			logger.log( Level.SEVERE, "driverClassPath: " +  driverClassPath);
			
			StringBuffer sb = new StringBuffer();
			for (URL url : extraDriverLoader.getURLs( ))
			{
				sb.append( "[" ).append( url ).append( "]" );
			}
			logger.log( Level.SEVERE, "Registered URLs: " + sb.toString( ) );
			
			//re-scan the driver directory. This re-scan is added for users would potentially 
			//set their own jdbc drivers, which would be copied to driver directory as well
			if(  refreshUrlsWhenFail && extraDriverLoader.refreshURLs() )
			{
				// New driver found; try loading again
				return loadExtraDriver( className, false, false, driverClassPath );
			}
			
			// no new driver found; give up
			logger.log( Level.FINER, "Driver class not found in drivers directory: " + className );
			return null;
		}
	}
	
	private static class DriverClassLoader extends URLClassLoader
	{
		private HashSet fileSet = new HashSet();
		private Collection<String> driverClassPath;
		public DriverClassLoader( Collection<String> driverClassPath ) throws OdaException
		{
			super( new URL[0], DriverClassLoader.class.getClassLoader() );
			logger.entering( DriverClassLoader.class.getName(), "constructor()" );
			this.driverClassPath = driverClassPath;
			refreshURLs();
		}

		protected PermissionCollection getPermissions( CodeSource codesource )
		{
			return this.getClass( ).getProtectionDomain( ).getPermissions( );
		}

		/**
		 * Refresh the URL list of DriverClassLoader
		 * @return if the refreshURL is different than the former one then return true otherwise
		 * 			return false
		 * @throws OdaException 
		 */
		public boolean refreshURLs() throws OdaException
		{
			boolean foundNewUnderSpecifiedDIR = refreshFileURLsUnderSpecifiedDIR( );

			boolean foundNewUnderDefaultDIR = refreshFileURLsUnderDefaultDIR( );

			return foundNewUnderSpecifiedDIR || foundNewUnderDefaultDIR;
		}

		private boolean refreshFileURLsUnderSpecifiedDIR( ) throws OdaException
		{
			boolean hasNewDriver = false;
			if ( driverClassPath != null && driverClassPath.size( ) > 0 )
			{
				for ( String classPath : driverClassPath )
				{
					if ( refreshFileURL( classPath ) )
					{
						hasNewDriver = true;
					}
				}
			}
			return hasNewDriver;
		}

		private boolean refreshFileURL( String classPath )
		{
			File driverClassFile = new File( classPath );
			if ( !driverClassFile.exists( ) )
			{
				return false;
			}
			boolean hasNewDriver = false;
			try
			{
				this.addURL( driverClassFile.toURI( ).toURL( ) );
			}
			catch ( MalformedURLException ex )
			{
			}

			if ( driverClassFile.isDirectory( ) )
			{
				File[] driverFiles = driverClassFile
						.listFiles( new FileFilter( ) {

							public boolean accept( File pathname )
							{
								if ( pathname.isFile( )
										&& OdaJdbcDriver.isDriverFile( pathname
												.getName( ) ) )
								{
									return true;
								}
								return false;
							}
						} );

				for ( File driverFile : driverFiles )
				{
					String fileName = driverFile.getName( );
					if ( !fileSet.contains( fileName ) )
					{
						fileSet.add( fileName );
						try
						{
							hasNewDriver = true;
							URL driverUrl = driverFile.toURI( ).toURL( );
							addURL( driverUrl );
							logger.info( "JDBCDriverManager: found JAR file "
									+ fileName + ". URL=" + driverUrl );
						}
						catch ( MalformedURLException ex )
						{
						}
					}
				}
			}
			return hasNewDriver;
		}

		private boolean refreshFileURLsUnderDefaultDIR( ) throws OdaException
		{
			Bundle bundle = Platform
					.getBundle( "org.eclipse.birt.report.data.oda.jdbc" );
			if ( bundle == null )
			{
				return false;
			}
			try
			{
				URL url = bundle
						.getEntry( OdaJdbcDriver.Constants.DRIVER_DIRECTORY );
				if ( url != null )
				{
					url = FileLocator.resolve( url );
					if ( url != null )
					{
						if ( "file".equals( url.getProtocol( ) ) )
						{
							String driverFolder = url.getFile( );
							return refreshFileURL( driverFolder );
						}
					}
				}
				return false;
			}
			catch ( IOException ex )
			{
				throw new OdaException( ex );
			}
		}
	}

//	The classloader of a driver (jtds driver, etc.) is
//	 "java.net.FactoryURLClassLoader", whose parent is
//	 "sun.misc.Launcher$AppClassLoader".
//	The classloader of class Connection (the caller of
//	 DriverManager.getConnection(url, props)) is
//	 "sun.misc.Launcher$AppClassLoader". As the classes loaded by a child
//	 classloader are always not visible to its parent classloader,
//	 DriverManager.getConnection(url, props), called by class Connection, actually
//	 has no access to driver classes, which are loaded by
//	 "java.net.FactoryURLClassLoader". The invoking of this method would return a
//	 "no suitable driver" exception.
//	On the other hand, if we use class WrappedDriver to wrap drivers. The DriverExt
//	 class is loaded by "sun.misc.Launcher$AppClassLoader", which is same as the
//	 classloader of Connection class. So DriverExt class is visible to
//	 DriverManager.getConnection(url, props). And the invoking of the very method
//	 would success.

	private static class WrappedDriver implements Driver
	{
		private Driver driver;
		private String driverClass;
		
		WrappedDriver( Driver d, String driverClass )
		{
			logger.entering( WrappedDriver.class.getName(), "WrappedDriver", driverClass );
			this.driver = d;
			this.driverClass = driverClass;
		}

		/*
		 * @see java.sql.Driver#acceptsURL(java.lang.String)
		 */
		public boolean acceptsURL( String u ) throws SQLException
		{
			boolean res = this.driver.acceptsURL( u );
			if ( logger.isLoggable( Level.FINER ))
				logger.log( Level.FINER, "WrappedDriver(" + driverClass + 
						").acceptsURL(" + LogUtil.encryptURL( u )+ ")returns: " + res);
			return res;
		}

		/*
		 * @see java.sql.Driver#connect(java.lang.String, java.util.Properties)
		 */
		public java.sql.Connection connect( String u, Properties p ) throws SQLException
		{
			logger.entering( WrappedDriver.class.getName() + ":" + driverClass, 
					"connect", LogUtil.encryptURL( u ) );
			try
			{
				return this.driver.connect( u, p );
			}
			catch ( RuntimeException e )
			{
				throw new SQLException( e.getMessage( ) );
			}
		}

		/*
		 * @see java.sql.Driver#getMajorVersion()
		 */
		public int getMajorVersion( )
		{
			return this.driver.getMajorVersion( );
		}

		/*
		 * @see java.sql.Driver#getMinorVersion()
		 */
		public int getMinorVersion( )
		{
			return this.driver.getMinorVersion( );
		}

		/*
		 * @see java.sql.Driver#getPropertyInfo(java.lang.String, java.util.Properties)
		 */
		public DriverPropertyInfo[] getPropertyInfo( String u, Properties p )
				throws SQLException
		{
			return this.driver.getPropertyInfo( u, p );
		}

		/*
		 * @see java.sql.Driver#jdbcCompliant()
		 */
		public boolean jdbcCompliant( )
		{
			return this.driver.jdbcCompliant( );
		}
		
		/*
		 * @see java.lang.Object#toString()
		 */
		public String toString( )
		{
			return driverClass;
		}
	}
}
