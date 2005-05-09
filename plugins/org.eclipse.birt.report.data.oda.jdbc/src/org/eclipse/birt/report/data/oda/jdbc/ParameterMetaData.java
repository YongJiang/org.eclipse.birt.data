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

import org.eclipse.birt.data.oda.IParameterMetaData;
import org.eclipse.birt.data.oda.OdaException;
import org.eclipse.birt.report.data.oda.i18n.ResourceConstants;

/**
 * 
 * This class implements the org.eclipse.birt.data.oda.IParameterMetaData
 * interface.
 *  
 */
public class ParameterMetaData implements IParameterMetaData
{

	/** JDBC ParameterMetaData instance */
	private java.sql.ParameterMetaData paraMetadata;

	private static Logger logger = Logger.getLogger( ParameterMetaData.class.getName( ) );	

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
			throw new DriverException( DriverException.ERRMSG_NO_PARAMETERMETADATA,
					DriverException.ERROR_NO_PARAMETERMETADATA );

		}
	}

	/**
	 * 
	 * Constructor ParameterMetaData(java.sql.ParameterMetaData paraMeta) use
	 * JDBC's ParameterMetaData to construct it.
	 *  
	 */

	public ParameterMetaData( java.sql.ParameterMetaData jparaMeta )
			throws OdaException
	{
		this.paraMetadata = jparaMeta;

	}

	/*
	 * 
	 * @see org.eclipse.birt.data.oda.IParameterMetaData#getParameterCount()
	 */
	public int getParameterCount( ) throws OdaException
	{
		logger.logp( java.util.logging.Level.FINE,
				ParameterMetaData.class.getName( ),
				"getParameterCount",
				"ParameterMetaData.getParameterCount( )" );
		assertNotNull( paraMetadata );
		try
		{
			/* redirect the call to JDBC ParameterMetaData.getParameterCount() */
			return paraMetadata.getParameterCount( );
		}
		catch ( SQLException e )
		{
			throw new JDBCException( ResourceConstants.PARAMETER_COUNT_CANNOT_GET,
					e );
		}

	}

	/*
	 * 
	 * @see org.eclipse.birt.data.oda.IParameterMetaData#getParameterMode(int)
	 */
	public int getParameterMode( int param ) throws OdaException
	{
		logger.logp( java.util.logging.Level.FINE,
				ParameterMetaData.class.getName( ),
				"getParameterMode",
				"ParameterMetaData.getParameterMode( )" );
		assertNotNull( paraMetadata );
		try
		{
			int result = IParameterMetaData.parameterModeUnknown;
			if ( paraMetadata.getParameterMode( param ) == java.sql.ParameterMetaData.parameterModeIn )
				result = IParameterMetaData.parameterModeIn;
			else if ( paraMetadata.getParameterMode( param ) == java.sql.ParameterMetaData.parameterModeOut )
				result = IParameterMetaData.parameterModeOut;
			else if ( paraMetadata.getParameterMode( param ) == java.sql.ParameterMetaData.parameterModeInOut )
				result = IParameterMetaData.parameterModeInOut;
			return result;
		}
		catch ( SQLException e )
		{
			throw new JDBCException( ResourceConstants.PARAMETER_MODE_CANNOT_GET,
					e );
		}

	}

	/*
	 * 
	 * @see org.eclipse.birt.data.oda.IParameterMetaData#getParameterType(int)
	 */
	public int getParameterType( int param ) throws OdaException
	{
		logger.logp( java.util.logging.Level.FINE,
				ParameterMetaData.class.getName( ),
				"getParameterType",
				"ParameterMetaData.getParameterType( )" );
		assertNotNull( paraMetadata );
		try
		{
			/* redirect the call to JDBC ParameterMetaData.getParameterType(int) */
			return paraMetadata.getParameterType( param );
		}
		catch ( SQLException e )
		{
			throw new JDBCException( ResourceConstants.PARAMETER_TYPE_CANNOT_GET,
					e );
		}

	}

	/*
	 * 
	 * @see org.eclipse.birt.data.oda.IParameterMetaData#getParameterTypeName(int)
	 */
	public String getParameterTypeName( int param ) throws OdaException
	{
		logger.logp( java.util.logging.Level.FINE,
				ParameterMetaData.class.getName( ),
				"getParameterTypeName",
				"ParameterMetaData.getParameterTypeName( )" );
		assertNotNull( paraMetadata );
		try
		{
			/*
			 * redirect the call to JDBC
			 * ParameterMetaData.getParameterTypeName(int)
			 */
			return paraMetadata.getParameterTypeName( param );
		}
		catch ( SQLException e )
		{
			throw new JDBCException( ResourceConstants.PARAMETER_TYPE_NAME_CANNOT_GET,
					e );
		}

	}

	/*
	 * 
	 * @see org.eclipse.birt.data.oda.IParameterMetaData#getPrecision(int)
	 */
	public int getPrecision( int param ) throws OdaException
	{
		logger.logp( java.util.logging.Level.FINE,
				ParameterMetaData.class.getName( ),
				"getPrecision",
				"ParameterMetaData.getPrecision( )" );
		assertNotNull( paraMetadata );
		try
		{
			/* redirect the call to JDBC ParameterMetaData.getPrecision(int) */
			return paraMetadata.getPrecision( param );
		}
		catch ( SQLException e )
		{
			throw new JDBCException( ResourceConstants.PARAMETER_PRECISION_CANNOT_GET,
					e );
		}

	}

	/*
	 * 
	 * @see org.eclipse.birt.data.oda.IParameterMetaData#getScale(int)
	 */
	public int getScale( int param ) throws OdaException
	{
		logger.logp( java.util.logging.Level.FINE,
				ParameterMetaData.class.getName( ),
				"getScale",
				"ParameterMetaData.getScale( )" );
		assertNotNull( paraMetadata );
		try
		{
			/* redirect the call to JDBC ParameterMetaData.getScale(int) */
			return paraMetadata.getScale( param );
		}
		catch ( SQLException e )
		{
			throw new JDBCException( ResourceConstants.PARAMETER_SCALE_CANNOT_GET,
					e );
		}

	}

	/*
	 * 
	 * @see org.eclipse.birt.data.oda.IParameterMetaData#isNullable(int)
	 */
	public int isNullable( int param ) throws OdaException
	{
		logger.logp( java.util.logging.Level.FINE,
				ParameterMetaData.class.getName( ),
				"isNullable",
				"ParameterMetaData.isNullable( )" );
		assertNotNull( paraMetadata );
		try
		{
			int result = IParameterMetaData.parameterNullableUnknown;
			if ( paraMetadata.isNullable( param ) == java.sql.ParameterMetaData.parameterNullable )
				result = IParameterMetaData.parameterNullable;
			else if ( paraMetadata.isNullable( param ) == java.sql.ParameterMetaData.parameterNoNulls )
				result = IParameterMetaData.parameterNoNulls;
			return result;
		}
		catch ( SQLException e )
		{
			throw new JDBCException( ResourceConstants.PARAMETER_NULLABILITY_CANNOT_DETERMINE,
					e );
		}

	}
}