/*******************************************************************************
 * Copyright (c) 2004, 2008 Actuate Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Actuate Corporation  - initial API and implementation
 *******************************************************************************/

package org.eclipse.birt.data.aggregation.impl;

import org.eclipse.birt.core.data.DataType;
import org.eclipse.birt.data.engine.api.aggregation.IParameterDefn;

/**
 * 
 */

public class ParameterDefn implements IParameterDefn
{

	private String name;
	private boolean isOptional = false;
	private boolean isDataField = false;
	private String displayName = "";//$NON-NLS-1$
	private String description = "";//$NON-NLS-1$
	private int[] supportedDataTypes = new int[]{
			DataType.ANY_TYPE
	};
	

	/**
	 * 
	 * @param name
	 * @param displayName
	 * @param isOptional
	 * @param isDataField
	 */
	public ParameterDefn( String name, String displayName, boolean isOptional,
			boolean isDataField )
	{
		this.name = name;
		this.displayName = displayName;
		this.isOptional = isOptional;
		this.isDataField = isDataField;
	}

	/**
	 * 
	 * @param name
	 * @param displayName
	 * @param isOptional
	 * @param isDataField
	 * @param supportedDataTypes
	 * @param description
	 */
	public ParameterDefn( String name, String displayName, boolean isOptional,
			boolean isDataField, int[] supportedDataTypes, String description )
	{
		this.name = name;
		this.isOptional = isOptional;
		this.isDataField = isDataField;
		this.displayName = displayName;
		this.supportedDataTypes = supportedDataTypes;
		this.description = description;
	}

	/**
	 * @param isOptional
	 *            the isOptional to set
	 */
	public void setOptional( boolean isOptional )
	{
		this.isOptional = isOptional;
	}

	/**
	 * @param isDataField
	 *            the isDataField to set
	 */
	public void setDataField( boolean isDataField )
	{
		this.isDataField = isDataField;
	}

	/**
	 * @param displayName
	 *            the displayName to set
	 */
	public void setDisplayName( String displayName )
	{
		this.displayName = displayName;
	}

	

	/**
	 * @param description
	 *            the description to set
	 */
	public void setDescription( String description )
	{
		this.description = description;
	}

	
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.birt.data.engine.api.aggregation.IParameterDefn#getName()
	 */
	public String getName( )
	{
		return name;
	}

	
	/**
	 * @param name the name to set
	 */
	public void setName( String name )
	{
		this.name = name;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.birt.data.engine.api.aggregation.IParameterDefn#getDescription()
	 */
	public String getDescription( )
	{
		return description;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.birt.data.engine.api.aggregation.IParameterDefn#getDisplayName()
	 */
	public String getDisplayName( )
	{
		return displayName;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.birt.data.engine.api.aggregation.IParameterDefn#isDataField()
	 */
	public boolean isDataField( )
	{
		return isDataField;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.birt.data.engine.api.aggregation.IParameterDefn#isOptional()
	 */
	public boolean isOptional( )
	{
		return isOptional;
	}
	
	

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.birt.data.engine.api.aggregation.IParameterDefn#supportDataType(int)
	 */
	public boolean supportDataType( int dataType )
	{
		for ( int i = 0; i < supportedDataTypes.length; i++ )
		{
			if ( supportedDataTypes[i] == DataType.ANY_TYPE
					|| supportedDataTypes[i] == dataType )
			{
				return true;
			}
		}
		return false;
	}

}
