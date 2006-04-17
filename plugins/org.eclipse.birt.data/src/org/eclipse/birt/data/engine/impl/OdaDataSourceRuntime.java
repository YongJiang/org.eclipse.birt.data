/*
 *************************************************************************
 * Copyright (c) 2004, 2005 Actuate Corporation.
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

package org.eclipse.birt.data.engine.impl;

import org.eclipse.birt.data.engine.api.IOdaDataSourceDesign;
import org.mozilla.javascript.Scriptable;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * Encapulates the runtime definition of a generic extended data source.
 */
public class OdaDataSourceRuntime extends DataSourceRuntime
{
	private String		extensionID;
	private	Map			publicProperties;
	
    OdaDataSourceRuntime( IOdaDataSourceDesign dataSource,
			Scriptable sharedScope )
	{
		super( dataSource, sharedScope );

		// Copy updatable properties
		publicProperties = new HashMap( );
		publicProperties.putAll( dataSource.getPublicProperties( ) );

		extensionID = dataSource.getExtensionID( );
		logger.log( Level.FINER, "OdaDataSourceRuntime starts up" );
	}

	/**
	 * 
	 */
	public IOdaDataSourceDesign getSubdesign( )
	{
		return (IOdaDataSourceDesign) getDesign( );
	}

	/*
	 * @see org.eclipse.birt.data.engine.api.script.IDataSourceInstanceHandle#getExtensionID()
	 */
	public String getExtensionID( )
	{
		return extensionID;
	}

	/**
	 * @return
	 */
	public Map getPublicProperties( )
	{
		// Return runtime copy of public properties, which may have been updated
		return this.publicProperties;
	}

	/**
	 * @return
	 */
	public Map getPrivateProperties( )
	{
		return getSubdesign( ).getPrivateProperties( );
	}

	/*
	 * @see org.eclipse.birt.data.engine.api.script.IDataSourceInstanceHandle#getAllExtensionProperties()
	 */
	public Map getAllExtensionProperties( )
	{
		return this.publicProperties;
	}

	/*
	 * @see org.eclipse.birt.data.engine.api.script.IDataSourceInstanceHandle#getExtensionProperty(java.lang.String)
	 */
	public String getExtensionProperty( String name )
	{
		return (String) publicProperties.get( name );
	}

	/*
	 * @see org.eclipse.birt.data.engine.api.script.IDataSourceInstanceHandle#setExtensionProperty(java.lang.String,
	 *      java.lang.String)
	 */
	public void setExtensionProperty( String name, String value )
	{
		publicProperties.put( name, value );
	}
	
}
