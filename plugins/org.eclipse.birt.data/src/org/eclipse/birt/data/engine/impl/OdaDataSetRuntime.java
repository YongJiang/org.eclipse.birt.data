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

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import org.eclipse.birt.data.engine.api.IOdaDataSetDesign;

/**
 * Encapulates the runtime definition of a generic extended (ODA) data set.
 */
public class OdaDataSetRuntime extends DataSetRuntime
{
	private String 	queryText;
	
	/** Public properties as a (String -> String) map */
	private Map		publicProperties;
	
    OdaDataSetRuntime( IOdaDataSetDesign dataSet, QueryExecutor executor )
    {
        super( dataSet, executor );
        
        // Copy from design all properties that may change at runtime
        queryText = dataSet.getQueryText();
        publicProperties = new HashMap();
        publicProperties.putAll( dataSet.getPublicProperties() );
		logger.log( Level.FINER, "OdaDataSetRuntime starts up" );
    }

    public IOdaDataSetDesign getSubdesign()
	{
		return (IOdaDataSetDesign) getDesign();
	}

    public OdaDataSourceRuntime getExtendedDataSource()
    {
        assert getDataSource() instanceof OdaDataSourceRuntime;
        return (OdaDataSourceRuntime) getDataSource();
    }

    public String getQueryText()
    {
    	return queryText;
    }
    
    public void setQueryText( String queryText )
    {
    	this.queryText = queryText;
    }

    
    public String getExtensionID()
    {
        return getSubdesign().getExtensionID();
    }

    public String getPrimaryResultSetName()
    {
        return getSubdesign().getPrimaryResultSetName();
    }

	public Map getPublicProperties( ) 
	{
		return this.publicProperties;
	}

	public Map getPrivateProperties( ) 
	{
        return getSubdesign().getPrivateProperties();
	}

	/**
	 * @see org.eclipse.birt.data.engine.api.script.IDataSetInstanceHandle#getAllExtensionProperties()
	 */
	public Map getAllExtensionProperties()
	{
		return this.publicProperties;
	}

	/**
	 * @see org.eclipse.birt.data.engine.api.script.IDataSetInstanceHandle#getExtensionProperty(java.lang.String)
	 */
	public String getExtensionProperty(String name)
	{
		return (String) this.publicProperties.get( name );
	}

	/**
	 * @see org.eclipse.birt.data.engine.api.script.IDataSetInstanceHandle#setExtensionProperty(java.lang.String, java.lang.String)
	 */
	public void setExtensionProperty(String name, String value)
	{
		this.publicProperties.put( name, value );
	}
	
}
