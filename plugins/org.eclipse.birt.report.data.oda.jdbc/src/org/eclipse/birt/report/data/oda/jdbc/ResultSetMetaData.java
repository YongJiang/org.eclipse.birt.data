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

import java.sql.SQLException;
import java.util.logging.Logger;

import org.eclipse.birt.data.oda.IResultSetMetaData;
import org.eclipse.birt.data.oda.OdaException;
import org.eclipse.birt.report.data.oda.i18n.ResourceConstants;

/**
 * 
 * This class implements the org.eclipse.birt.data.oda.IResultSetMetaData
 * interface.
 *  
 */
public class ResultSetMetaData implements IResultSetMetaData
{

	/** the JDBC ResultSetMetaData object */
	private java.sql.ResultSetMetaData rsMetadata;

	private static Logger logger = Logger.getLogger( ResultSetMetaData.class.getName( ) );

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
			throw new DriverException( DriverException.ERRMSG_NO_RESULTSETMETADATA,
					DriverException.ERROR_NO_RESULTSETMETADATA );

		}
	}

	/**
	 * 
	 * Constructor ResultSetMetaData(java.sql.ResultSetMetaData rsMeta) use
	 * JDBC's ResultSetMetaData to construct it.
	 *  
	 */
	public ResultSetMetaData( java.sql.ResultSetMetaData rsMeta )
			throws OdaException
	{
		/* record down the JDBC ResultSetMetaData object */
		this.rsMetadata = rsMeta;

	}

	/*
	 * @see org.eclipse.birt.data.oda.IResultSetMetaData#getColumnCount()
	 */
	public int getColumnCount( ) throws OdaException
	{
		logger.logp( java.util.logging.Level.FINE,
				ResultSetMetaData.class.getName( ),
				"getColumnCount",
				"ResultSetMetaData.getColumnCount( )" );
		assertNotNull( rsMetadata );
		try
		{
			/* redirect the call to JDBC ResultSetMetaData.getColumnCount() */
			return rsMetadata.getColumnCount( );
		}
		catch ( SQLException e )
		{
			throw new JDBCException( ResourceConstants.COLUMN_COUNT_CANNOT_GET,
					e );
		}

	}

	/*
	 * @see org.eclipse.birt.data.oda.IResultSetMetaData#getColumnName(int)
	 */
	public String getColumnName( int index ) throws OdaException
	{
		logger.logp( java.util.logging.Level.FINE,
				ResultSetMetaData.class.getName( ),
				"getColumnName",
				"ResultSetMetaData.getColumnName( )" );
		assertNotNull( rsMetadata );
		try
		{
			/* redirect the call to JDBC ResultSetMetaData.getColumnName(int) */
			return rsMetadata.getColumnName( index );
		}
		catch ( SQLException e )
		{
			throw new JDBCException( ResourceConstants.COLUMN_NAME_CANNOT_GET,
					e );
		}

	}

	/*
	 * @see org.eclipse.birt.data.oda.IResultSetMetaData#getColumnLabel(int)
	 */
	public String getColumnLabel( int index ) throws OdaException
	{
		logger.logp( java.util.logging.Level.FINE,
				ResultSetMetaData.class.getName( ),
				"getColumnLabel",
				"ResultSetMetaData.getColumnLabel( )" );
		assertNotNull( rsMetadata );
		try
		{
			/* redirect the call to JDBC ResultSetMetaData.getColumnLabel(int) */
			return rsMetadata.getColumnLabel( index );
		}
		catch ( SQLException e )
		{
			throw new JDBCException( ResourceConstants.COLUMN_LABEL_CANNOT_GET,
					e );
		}

	}

	/*
	 * @see org.eclipse.birt.data.oda.IResultSetMetaData#getColumnType(int)
	 */
	public int getColumnType( int index ) throws OdaException
	{
		logger.logp( java.util.logging.Level.FINE,
				ResultSetMetaData.class.getName( ),
				"getColumnType",
				"ResultSetMetaData.getColumnType( )" );
		assertNotNull( rsMetadata );
		try
		{
			/* redirect the call to JDBC ResultSetMetaData.getColumnType(int) */
			return rsMetadata.getColumnType( index );
		}
		catch ( SQLException e )
		{
			throw new JDBCException( ResourceConstants.COLUMN_TYPE_CANNOT_GET,
					e );
		}

	}

	/*
	 * @see org.eclipse.birt.data.oda.IResultSetMetaData#getColumnTypeName(int)
	 */
	public String getColumnTypeName( int index ) throws OdaException
	{
		logger.logp( java.util.logging.Level.FINE,
				ResultSetMetaData.class.getName( ),
				"getColumnTypeName",
				"ResultSetMetaData.getColumnTypeName( )" );
		assertNotNull( rsMetadata );
		try
		{
			/*
			 * redirect the call to JDBC
			 * ResultSetMetaData.getColumnTypeName(int)
			 */
			return rsMetadata.getColumnTypeName( index );
		}
		catch ( SQLException e )
		{
			throw new JDBCException( ResourceConstants.COLUMN_TYPE_NAME_CANNOT_GET,
					e );
		}

	}

	/*
	 * @see org.eclipse.birt.data.oda.IResultSetMetaData#getColumnDisplayLength(int)
	 */
	public int getColumnDisplayLength( int index ) throws OdaException
	{
		logger.logp( java.util.logging.Level.FINE,
				ResultSetMetaData.class.getName( ),
				"getColumnDisplayLength",
				"ResultSetMetaData.getColumnDisplayLength( )" );
		assertNotNull( rsMetadata );
		try
		{
			/*
			 * redirect the call to JDBC
			 * ResultSetMetaData.getColumnDisplaySize(int)
			 */
			return rsMetadata.getColumnDisplaySize( index );
		}
		catch ( SQLException e )
		{
			throw new JDBCException( ResourceConstants.COLUMN_DISPLAY_SIZE_CANNOT_GET,
					e );
		}

	}

	/*
	 * @see org.eclipse.birt.data.oda.IResultSetMetaData#getPrecision(int)
	 */
	public int getPrecision( int index ) throws OdaException
	{
		logger.logp( java.util.logging.Level.FINE,
				ResultSetMetaData.class.getName( ),
				"getPrecision",
				"ResultSetMetaData.getPrecision( )" );
		assertNotNull( rsMetadata );
		try
		{
			/* redirect the call to JDBC ResultSetMetaData.getPrecision(int) */
			return rsMetadata.getPrecision( index );
		}
		catch ( SQLException e )
		{
			throw new JDBCException( ResourceConstants.RESULTSET_METADATA_PRECISION_CANNOT_GET,
					e );
		}

	}

	/*
	 * @see org.eclipse.birt.data.oda.IResultSetMetaData#getScale(int)
	 */
	public int getScale( int index ) throws OdaException
	{
		logger.logp( java.util.logging.Level.FINE,
				ResultSetMetaData.class.getName( ),
				"getScale",
				"ResultSetMetaData.getScale( )" );
		assertNotNull( rsMetadata );
		try
		{
			/* redirect the call to JDBC ResultSetMetaData.getScale(int) */
			return rsMetadata.getScale( index );
		}
		catch ( SQLException e )
		{
			throw new JDBCException( ResourceConstants.RESULTSET_MEATADATA_SCALE_CANNOT_GET,
					e );
		}

	}

	/*
	 * @see org.eclipse.birt.data.oda.IResultSetMetaData#isNullable(int)
	 */
	public int isNullable( int index ) throws OdaException
	{
		logger.logp( java.util.logging.Level.FINE,
				ResultSetMetaData.class.getName( ),
				"isNullable",
				"ResultSetMetaData.isNullable( )" );
		assertNotNull( rsMetadata );
		try
		{
			/* redirect the call to JDBC ResultSetMetaData.isNullable(int) */
			return rsMetadata.isNullable( index );
		}
		catch ( SQLException e )
		{
			throw new JDBCException( ResourceConstants.RESULTSET_NULLABILITY_CANNOT_DETERMINE,
					e );
		}

	}

}