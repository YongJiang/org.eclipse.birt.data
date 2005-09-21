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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.logging.Logger;

import org.eclipse.birt.report.data.oda.i18n.ResourceConstants;
import org.eclipse.datatools.connectivity.oda.IBlob;
import org.eclipse.datatools.connectivity.oda.IClob;
import org.eclipse.datatools.connectivity.oda.IResultSet;
import org.eclipse.datatools.connectivity.oda.IResultSetMetaData;
import org.eclipse.datatools.connectivity.oda.OdaException;
import org.eclipse.datatools.connectivity.oda.impl.Blob;
import org.eclipse.datatools.connectivity.oda.impl.Clob;

/**
 * 
 * The class implements the org.eclipse.datatools.connectivity.oda.IResultSet interface.
 *  
 */
public class ResultSet implements IResultSet
{

	/** the JDBC ResultSet object */
	private java.sql.ResultSet rs;

	/** the variable to remember the max rows that the resultset can return */
	private int maxRows;

	/** the variable to indicate the current row number */
	private int currentRow;

	private static Logger logger = Logger.getLogger( ResultSet.class.getName( ) );	

	/**
	 * assertNotNull(Object o)
	 * 
	 * @param o
	 *            the object that need to be tested null or not. if null, throw
	 *            exception
	 */
	private void assertNotNull( Object o ) throws OdaException
	{
		if ( o == null )
		{
			throw new JDBCException( ResourceConstants.DRIVER_NO_RESULTSET,
					ResourceConstants.ERROR_NO_RESULTSET );

		}
	}

	/**
	 * 
	 * Constructor ResultSet(java.sql.ResultSet jrs) use JDBC's ResultSet to
	 * construct it.
	 *  
	 */
	public ResultSet( java.sql.ResultSet jrs ) throws OdaException
	{

		/* record down the JDBC ResultSet object */
		this.rs = jrs;

		/* set the maxrows variable, default is 0 - no limit */
		maxRows = Integer.MAX_VALUE;

		currentRow = 0;

	}

	/*
	 * 
	 * @see org.eclipse.datatools.connectivity.oda.IResultSet#getMetaData()
	 */
	public IResultSetMetaData getMetaData( ) throws OdaException
	{
		logger.logp( java.util.logging.Level.FINE,
				ResultSet.class.getName( ),
				"getMetaData",
				"ResultSet.getMetaData( )" );
		assertNotNull( rs );

		try
		{
			/* redirect the call to JDBC ResultSet.getMetaData() */
			ResultSetMetaData rsMeta = new ResultSetMetaData( rs.getMetaData( ) );
			return rsMeta;
		}
		catch ( SQLException e )
		{
			throw new JDBCException( ResourceConstants.RESULTSET_METADATA_CANNOT_GET,
					e );
		}

	}

	/*
	 *  (non-Javadoc)
	 * @see org.eclipse.datatools.connectivity.oda.IResultSet#close()
	 */
	public void close( ) throws OdaException
	{
		logger.logp( java.util.logging.Level.FINE,
				ResultSet.class.getName( ),
				"close",
				"ResultSet.close()" );		
		assertNotNull( rs );
		try
		{
			/* redirect the call to JDBC ResultSet.close() */
			rs.close( );

		}
		catch ( SQLException e )
		{
			throw new JDBCException( ResourceConstants.RESULTSET_CANNOT_CLOSE,
					e );
		}

	}

	/*
	 * 
	 * @see org.eclipse.datatools.connectivity.oda.IResultSet#setMaxRows(int)
	 */
	public void setMaxRows( int max )
	{
		logger.logp( java.util.logging.Level.FINE,
				ResultSet.class.getName( ),
				"setMaxRows",
				"ResultSet.setMaxRows( " + max + " )" );
		if ( max > 0 )
			maxRows = max;
		else
			maxRows = Integer.MAX_VALUE;
		//if the max is positive, reset it,
		// otherwise, ignore this operation and keep the
		// previous value

	}

	/*
	 * 
	 * @see org.eclipse.datatools.connectivity.oda.IResultSet#next()
	 */
	public boolean next( ) throws OdaException
	{
		logger.logp( java.util.logging.Level.FINE,
				ResultSet.class.getName( ),
				"next",
				"ResultSet.next( )" );

		assertNotNull( rs );

		try
		{
			/* redirect the call to JDBC ResultSet.next() */
			if ( currentRow < maxRows && rs.next( ) )
			{
				currentRow++;
				return true;
			}
			return false;
		}
		catch ( SQLException e )
		{
			throw new JDBCException(ResourceConstants.RESULTSET_CURSOR_DOWN_ERROR , e );
		}
	}

	/*
	 * 
	 * @see org.eclipse.datatools.connectivity.oda.IResultSet#getRow()
	 */
	public int getRow( ) throws OdaException
	{
		logger.logp( java.util.logging.Level.FINE,
				ResultSet.class.getName( ),
				"getRow",
				"ResultSet.getRow( )" );
		assertNotNull( rs );
		return this.currentRow;
	}

	/*
	 * 
	 * @see org.eclipse.datatools.connectivity.oda.IResultSet#getString(int)
	 */
	public String getString( int index ) throws OdaException
	{
		assertNotNull( rs );
		try
		{
			/* redirect the call to JDBC ResultSet.getString(int) */
			return rs.getString( index );

		}
		catch ( SQLException e )
		{
			throw new JDBCException( ResourceConstants.RESULTSET_CANNOT_GET_STRING_VALUE,
					e );
		}
	}

	/*
	 * 
	 * @see org.eclipse.datatools.connectivity.oda.IResultSet#getString(java.lang.String)
	 */
	public String getString( String columnName ) throws OdaException
	{
		assertNotNull( rs );
		try
		{
			/* redirect the call to JDBC ResultSet.getString(string) */
			return rs.getString( columnName );

		}
		catch ( SQLException e )
		{
			throw new JDBCException( ResourceConstants.RESULTSET_CANNOT_GET_STRING_VALUE,
					e );
		}
	}

	/*
	 * 
	 * @see org.eclipse.datatools.connectivity.oda.IResultSet#getInt(int)
	 */
	public int getInt( int index ) throws OdaException
	{
		assertNotNull( rs );
		try
		{
			/* redirect the call to JDBC ResultSet.getInt(int) */
			return rs.getInt( index );

		}
		catch ( SQLException e )
		{
			throw new JDBCException( ResourceConstants.RESULTSET_CANNOT_GET_INT_VALUE,
					e );
		}
	}

	/*
	 * 
	 * @see org.eclipse.datatools.connectivity.oda.IResultSet#getInt(java.lang.String)
	 */
	public int getInt( String columnName ) throws OdaException
	{
		assertNotNull( rs );
		try
		{
			/* redirect the call to JDBC ResultSet.getInt(String) */
			return rs.getInt( columnName );

		}
		catch ( SQLException e )
		{
			throw new JDBCException( ResourceConstants.RESULTSET_CANNOT_GET_INT_VALUE,
					e );
		}
	}

	/*
	 * 
	 * @see org.eclipse.datatools.connectivity.oda.IResultSet#getDouble(int)
	 */
	public double getDouble( int index ) throws OdaException
	{
		assertNotNull( rs );
		try
		{
			/* redirect the call to JDBC ResultSet.getDouble(int) */
			return rs.getDouble( index );

		}
		catch ( SQLException e )
		{
			throw new JDBCException( ResourceConstants.RESULTSET_CANNOT_GET_DOUBLE_VALUE,
					e );
		}
	}

	/*
	 * 
	 * @see org.eclipse.datatools.connectivity.oda.IResultSet#getDouble(java.lang.String)
	 */
	public double getDouble( String columnName ) throws OdaException
	{
		assertNotNull( rs );
		try
		{
			/* redirect the call to JDBC ResultSet.getDouble(String) */
			return rs.getDouble( columnName );

		}
		catch ( SQLException e )
		{
			throw new JDBCException( ResourceConstants.RESULTSET_CANNOT_GET_DOUBLE_VALUE,
					e );
		}
	}

	/*
	 * 
	 * @see org.eclipse.datatools.connectivity.oda.IResultSet#getBigDecimal(int)
	 */
	public BigDecimal getBigDecimal( int index ) throws OdaException
	{
		assertNotNull( rs );
		try
		{
			/* redirect the call to JDBC ResultSet.getBigDecimal(int) */
			return rs.getBigDecimal( index );

		}
		catch ( SQLException e )
		{
			throw new JDBCException( ResourceConstants.RESULTSET_CANNOT_GET_BIGDECIMAL_VALUE,
					e );
		}
	}

	/*
	 * 
	 * @see org.eclipse.datatools.connectivity.oda.IResultSet#getBigDecimal(java.lang.String)
	 */
	public BigDecimal getBigDecimal( String columnName ) throws OdaException
	{
		assertNotNull( rs );
		try
		{
			/* redirect the call to JDBC ResultSet.getBigDecimal(String) */
			return rs.getBigDecimal( columnName );

		}
		catch ( SQLException e )
		{
			throw new JDBCException( ResourceConstants.RESULTSET_CANNOT_GET_BIGDECIMAL_VALUE,
					e );
		}
	}

	/*
	 * 
	 * @see org.eclipse.datatools.connectivity.oda.IResultSet#getDate(int)
	 */
	public Date getDate( int index ) throws OdaException
	{
		assertNotNull( rs );

		try
		{
			/* redirect the call to JDBC ResultSet.getDate(int) */
			return rs.getDate( index );

		}
		catch ( SQLException e )
		{
			throw new JDBCException( ResourceConstants.RESULTSET_CANNOT_GET_DATE_VALUE,
					e );
		}
	}

	/*
	 * 
	 * @see org.eclipse.datatools.connectivity.oda.IResultSet#getDate(java.lang.String)
	 */
	public Date getDate( String columnName ) throws OdaException
	{
		assertNotNull( rs );
		try
		{
			/* redirect the call to JDBC ResultSet.getDate(String) */
			return rs.getDate( columnName );

		}
		catch ( SQLException e )
		{
			throw new JDBCException( ResourceConstants.RESULTSET_CANNOT_GET_DATE_VALUE,
					e );
		}
	}

	/*
	 * 
	 * @see org.eclipse.datatools.connectivity.oda.IResultSet#getTime(int)
	 */
	public Time getTime( int index ) throws OdaException
	{
		assertNotNull( rs );
		try
		{
			/* redirect the call to JDBC ResultSet.getTime(int) */
			return rs.getTime( index );

		}
		catch ( SQLException e )
		{
			throw new JDBCException( ResourceConstants.RESULTSET_CANNOT_GET_TIME_VALUE,
					e );
		}
	}

	/*
	 * 
	 * @see org.eclipse.datatools.connectivity.oda.IResultSet#getTime(java.lang.String)
	 */
	public Time getTime( String columnName ) throws OdaException
	{
		assertNotNull( rs );
		try
		{
			/* redirect the call to JDBC ResultSet.getTime(String) */
			return rs.getTime( columnName );

		}
		catch ( SQLException e )
		{
			throw new JDBCException( ResourceConstants.RESULTSET_CANNOT_GET_TIME_VALUE,
					e );
		}
	}

	/*
	 * 
	 * @see org.eclipse.datatools.connectivity.oda.IResultSet#getTimestamp(int)
	 */
	public Timestamp getTimestamp( int index ) throws OdaException
	{
		assertNotNull( rs );
		try
		{
			/* redirect the call to JDBC ResultSet.getTimestamp(int) */
			return rs.getTimestamp( index );

		}
		catch ( SQLException e )
		{
			throw new JDBCException( ResourceConstants.RESULTSET_CANNOT_GET_TIMESTAMP_VALUE,
					e );
		}
	}

	/*
	 * 
	 * @see org.eclipse.datatools.connectivity.oda.IResultSet#getTimestamp(java.lang.String)
	 */
	public Timestamp getTimestamp( String columnName ) throws OdaException
	{
		assertNotNull( rs );
		try
		{
			/* redirect the call to JDBC ResultSet.getTimestamp(String) */
			return rs.getTimestamp( columnName );

		}
		catch ( SQLException e )
		{
			throw new JDBCException( ResourceConstants.RESULTSET_CANNOT_GET_TIMESTAMP_VALUE,
					e );
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.datatools.connectivity.oda.IAdvancedQuery#getBlob(java.lang.String)
	 */
	public IBlob getBlob( String parameterName ) throws OdaException
	{
		assertNotNull( rs );
		try
		{
			java.sql.Blob blob = rs.getBlob( parameterName );

			if ( blob == null )
				return new Blob( null );
			BufferedInputStream inputStream = null;
			inputStream = new BufferedInputStream( blob.getBinaryStream( ) );
			byte[] bytes = new byte[(int) blob.length( )];
			inputStream.read( bytes );
			inputStream.close( );
			return new Blob( bytes );
		}
		catch ( SQLException e )
		{
			throw new JDBCException( ResourceConstants.RESULTSET_CANNOT_GET_BLOB_VALUE,
					e );
		}
		catch ( IOException e )
		{
			throw new OdaException( ResourceConstants.RESULTSET_CANNOT_GET_BLOB_VALUE );
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.datatools.connectivity.oda.IAdvancedQuery#getBlob(int)
	 */
	public IBlob getBlob( int parameterId ) throws OdaException
	{
		assertNotNull( rs );
		try
		{
			java.sql.Blob blob = rs.getBlob( parameterId );

			if ( blob == null )
				return new Blob( null );
			BufferedInputStream inputStream = null;
			inputStream = new BufferedInputStream( blob.getBinaryStream( ) );
			byte[] bytes = new byte[(int) blob.length( )];
			inputStream.read( bytes );
			inputStream.close( );
			return new Blob( bytes );
		}
		catch ( SQLException e )
		{
			throw new JDBCException( ResourceConstants.RESULTSET_CANNOT_GET_BLOB_VALUE,
					e );
		}
		catch ( IOException e )
		{
			throw new OdaException( ResourceConstants.RESULTSET_CANNOT_GET_BLOB_VALUE );
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.datatools.connectivity.oda.IAdvancedQuery#getClob(java.lang.String)
	 */
	public IClob getClob( String parameterName ) throws OdaException
	{
		assertNotNull( rs );
		try
		{
			java.sql.Clob clob = rs.getClob( parameterName );

			if ( clob == null )
				return new Clob( null );
			BufferedReader in = new BufferedReader( clob.getCharacterStream( ) );
			StringBuffer buffer = new StringBuffer( );
			String str;
			while ( ( str = in.readLine( ) ) != null )
			{
				buffer.append( str );
			}
			in.close( );
			return new Clob( buffer.toString( ) );
		}
		catch ( SQLException e )
		{
			throw new JDBCException( ResourceConstants.RESULTSET_CANNOT_GET_BLOB_VALUE,
					e );
		}
		catch ( IOException e )
		{
			throw new OdaException( ResourceConstants.RESULTSET_CANNOT_GET_BLOB_VALUE );
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.datatools.connectivity.oda.IAdvancedQuery#getClob(int)
	 */
	public IClob getClob( int parameterId ) throws OdaException
	{
		assertNotNull( rs );
		try
		{
			java.sql.Clob clob = rs.getClob( parameterId );

			if ( clob == null )
				return new Clob( null );
			BufferedReader in = new BufferedReader( clob.getCharacterStream( ) );
			StringBuffer buffer = new StringBuffer( );
			String str;
			while ( ( str = in.readLine( ) ) != null )
			{
				buffer.append( str );
			}
			in.close( );
			return new Clob( buffer.toString( ) );
		}
		catch ( SQLException e )
		{
			throw new JDBCException( ResourceConstants.RESULTSET_CANNOT_GET_BLOB_VALUE,
					e );
		}
		catch ( IOException e )
		{
			throw new OdaException( ResourceConstants.RESULTSET_CANNOT_GET_BLOB_VALUE );
		}
	}

	/*
	 * 
	 * @see org.eclipse.datatools.connectivity.oda.IResultSet#wasNull()
	 */
	public boolean wasNull( ) throws OdaException
	{
		logger.logp( java.util.logging.Level.FINE,
				ResultSet.class.getName( ),
				"getMetaData",
				"ResultSet.wasNull( )" );
		assertNotNull( rs );

		try
		{
			/* redirect the call to JDBC ResultSet.wasNull() */
			return rs.wasNull( );

		}
		catch ( SQLException e )
		{
			throw new JDBCException( ResourceConstants.RESULTSET_DETERMINE_NULL, e );
		}
	}

	/*
	 * 
	 * @see org.eclipse.datatools.connectivity.oda.IResultSet#findColumn(java.lang.String)
	 */
	public int findColumn( String columnName ) throws OdaException
	{
		logger.logp( java.util.logging.Level.FINE,
				ResultSet.class.getName( ),
				"findColumn",
				"ResultSet.findColumn( \"" + columnName + "\" )" );
		assertNotNull( rs );
		try
		{
			/* redirect the call to JDBC ResultSet.findColumn(String) */
			return rs.findColumn( columnName );

		}
		catch ( SQLException e )
		{
			throw new JDBCException( ResourceConstants.RESULTSET_CANNOT_FIND_COLUMN,
					e );
		}
	}

}