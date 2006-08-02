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

package org.eclipse.birt.data.engine.impl;

import java.util.List;

import org.eclipse.birt.core.data.DataType;
import org.eclipse.birt.data.engine.api.IParameterDefinition;
import org.eclipse.birt.data.engine.odaconsumer.DataTypeUtil;
import org.eclipse.datatools.connectivity.oda.IParameterMetaData;
import org.eclipse.datatools.connectivity.oda.OdaException;

/**
 * The parameter definition from UI layer. This class is added as a work-around
 * class to handle the registerOutputParameter in stored procedure
 */
public class UserDefinedParamMetaData implements IParameterMetaData
{

	private Object[] paraDefnList;

	/**
	 * 
	 * @param parameterDefnList
	 *            The collection of <code>ParameterDefinition</code>
	 */
	public UserDefinedParamMetaData( List parameterDefnList )
	{
		if ( parameterDefnList == null || parameterDefnList.size( ) == 0 )
			paraDefnList = new Object[0];
		else
			paraDefnList = parameterDefnList.toArray( );
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.datatools.connectivity.oda.IParameterMetaData#getParameterCount()
	 */
	public int getParameterCount( ) throws OdaException
	{
		return paraDefnList.length;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.datatools.connectivity.oda.IParameterMetaData#getParameterMode(int)
	 */
	public int getParameterMode( int param ) throws OdaException
	{
		IParameterDefinition defn = (IParameterDefinition) paraDefnList[param];
		if ( defn.isInputMode( ) && defn.isOutputMode( ) )
			return IParameterMetaData.parameterModeInOut;
		else if ( defn.isInputMode( ) )
			return IParameterMetaData.parameterModeIn;
		else if ( defn.isOutputMode( ) )
			return IParameterMetaData.parameterModeOut;
		else
			return IParameterMetaData.parameterModeUnknown;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.datatools.connectivity.oda.IParameterMetaData#getParameterType(int)
	 */
	public int getParameterType( int param ) throws OdaException
	{
		IParameterDefinition defn = (IParameterDefinition) paraDefnList[param];
		Class dataTypeClass = DataType.getClass( defn.getType( ) );
		return DataTypeUtil.toOdaType( dataTypeClass );
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.datatools.connectivity.oda.IParameterMetaData#getParameterTypeName(int)
	 */
	public String getParameterTypeName( int param ) throws OdaException
	{
		return "Unknown";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.datatools.connectivity.oda.IParameterMetaData#getPrecision(int)
	 */
	public int getPrecision( int param ) throws OdaException
	{
		return 0;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.datatools.connectivity.oda.IParameterMetaData#getScale(int)
	 */
	public int getScale( int param ) throws OdaException
	{
		return 0;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.datatools.connectivity.oda.IParameterMetaData#isNullable(int)
	 */
	public int isNullable( int param ) throws OdaException
	{
		return 0;
	}

}
