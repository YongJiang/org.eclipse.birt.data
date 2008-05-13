/*
 *************************************************************************
 * Copyright (c) 2008 Actuate Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Actuate Corporation - initial API and implementation
 *  
 *************************************************************************
 */

package org.eclipse.birt.report.data.oda.jdbc.dbprofile.impl;

import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.datatools.connectivity.IConnectionProfile;
import org.eclipse.datatools.connectivity.IManagedConnection;
import org.eclipse.datatools.connectivity.oda.IConnection;
import org.eclipse.datatools.connectivity.oda.IQuery;
import org.eclipse.datatools.connectivity.oda.OdaException;
import org.eclipse.datatools.connectivity.oda.profile.OdaProfileExplorer;

/**
 * Extends the behavior of the oda.jdbc runtime driver to use a database connection profile.
 */
public class Connection extends org.eclipse.birt.report.data.oda.jdbc.Connection 
    implements IConnection
{
    private static final String JDBC_CONN_TYPE = "java.sql.Connection"; //$NON-NLS-1$

    private static final Logger sm_logger = Logger.getLogger( Connection.class.getName() );

    private IConnectionProfile m_dbProfile;
    
	/*
	 * @see org.eclipse.datatools.connectivity.oda.IConnection#open(java.util.Properties)
	 */
	public void open( Properties connProperties ) throws OdaException
	{
        // find and load the db profile defined in connection properties;
        // note: driver class path specified in appContext, if exists, is not relevant
        // when connecting with the properties defined in a connection profile instance
	    // (i.e. a profile instance uses its own jarList property)
	    IConnectionProfile dbProfile = OdaProfileExplorer.getInstance()
                                        .getProfileByName( connProperties, null );
	    try
        {
            open( dbProfile );
        }
        catch( OdaException ex )
        {
            // log warning with connect status
            sm_logger.log( Level.WARNING, "Unable to open connection through profile " + dbProfile.getName(), ex ); //$NON-NLS-1$
        }
        
        // no DTP managed JDBC connection available, try use local properties to connect
        if( ! isOpen() )  
            super.open( connProperties );       
	}
	
	/**
	 * Internal method to open a connection based on the specified database connection profile.
	 * @param dbProfile
	 * @throws OdaException
	 */
	public void open( IConnectionProfile dbProfile ) throws OdaException
	{
	    super.jdbcConn = null;
	    m_dbProfile = dbProfile;
	    if( m_dbProfile == null )
	        return;

        if( m_dbProfile.getConnectionState() != IConnectionProfile.CONNECTED_STATE )
        {
	        // connect via the db profile
	        IStatus connectStatus = m_dbProfile.connect();
	        
            if( connectStatus == null || 
                connectStatus.getSeverity() > IStatus.INFO )
                throw new OdaException( getStatusException( connectStatus ));
        }
        
        super.jdbcConn = getJDBCConnection( m_dbProfile );
 	}

    private java.sql.Connection getJDBCConnection( IConnectionProfile dbProfile )
    {
        if( dbProfile == null )
            return null;
        
        IManagedConnection mgtConn = dbProfile.getManagedConnection( JDBC_CONN_TYPE );
        if( mgtConn == null )
            return null;

        org.eclipse.datatools.connectivity.IConnection connObj = mgtConn.getConnection();
        if( connObj == null ) 
            return null;

        java.sql.Connection jdbcConn = (java.sql.Connection) connObj.getRawConnection();
        return jdbcConn;
    }
    
	/*
	 * @see org.eclipse.datatools.connectivity.oda.IConnection#close()
	 */
	public void close() throws OdaException
	{
        if( m_dbProfile != null )
        {
            m_dbProfile.disconnect( null );
            super.jdbcConn = null;
            return;
        }
        
        super.close();
	}

	/*
	 * @see org.eclipse.datatools.connectivity.oda.IConnection#isOpen()
	 */
	public boolean isOpen() throws OdaException
	{
        if( m_dbProfile != null )
            return ( m_dbProfile.getConnectionState() == IConnectionProfile.CONNECTED_STATE );

        return super.isOpen();
	}

    /* (non-Javadoc)
     * @see org.eclipse.datatools.connectivity.oda.IConnection#newQuery(java.lang.String)
     */
    public IQuery newQuery( String dataSetType ) throws OdaException
    {
        // ignores the specified dataSetType, 
        // as this driver currently supports only one data set type, and
        // the SQB data set type supports Select statements only
        return new DBProfileStatement( super.jdbcConn );
    }

    /**
     * Returns the connection profile instance for this db connection.
     * @return 
     */
    protected IConnectionProfile getDbProfile()
    {
        return m_dbProfile;
    }

    /**
     * Internal method to collect the first exception from the specified status.
     * @param status    may be null
     */
    public static Throwable getStatusException( IStatus status )
    {
        if( status == null )
            return null;
        Throwable ex = status.getException( );
        if( ex != null )
            return ex;

        // find first exception from its children
        IStatus[] childrenStatus = status.getChildren();
        for( int i=0; i < childrenStatus.length && ex == null; i++ )
        {
            ex = childrenStatus[i].getException( );
        }
        return ex;
    }
    
}
