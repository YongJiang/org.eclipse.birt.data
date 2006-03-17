/*
 *************************************************************************
 * Copyright (c) 2004 Actuate Corporation.
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

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.eclipse.birt.core.exception.BirtException;
import org.eclipse.birt.data.engine.api.IScriptDataSetDesign;
import org.eclipse.birt.data.engine.api.script.IScriptDataSetEventHandler;
import org.eclipse.birt.data.engine.api.script.IScriptDataSetMetaDataDefinition;
import org.eclipse.birt.data.engine.core.DataException;
import org.eclipse.birt.data.engine.executor.ResultFieldMetadata;
import org.eclipse.birt.data.engine.i18n.ResourceConstants;

/**
 * Encapulates the runtime definition of a scripted data set.
 */
public class ScriptDataSetRuntime extends DataSetRuntime 
	implements IScriptDataSetMetaDataDefinition
{
	private IScriptDataSetEventHandler scriptEventHandler;

	/** Columns defined by the describe event handler. 
	 * A list of ResultFieldMetadata objects*/
	private List describedColumns;
	
    ScriptDataSetRuntime( IScriptDataSetDesign dataSet, IQueryExecutor executor )
    {
        super( dataSet, executor);
    	if ( getEventHandler() instanceof IScriptDataSetEventHandler )
    		scriptEventHandler = (IScriptDataSetEventHandler) getEventHandler();
		logger.log(Level.FINER,"ScriptDataSetRuntime starts up");
    }

    public IScriptDataSetDesign getSubdesign()
	{
		return (IScriptDataSetDesign) getDesign();
	}

    public ScriptDataSourceRuntime getScriptDataSource()
    {
        assert getDataSource() instanceof ScriptDataSourceRuntime;
        return (ScriptDataSourceRuntime) getDataSource();
    }

    
	/** Executes the open script */
	public void open() throws DataException
	{
		if ( scriptEventHandler != null )
		{
			try
			{
				scriptEventHandler.handleOpen( this );
			}
			catch (BirtException e)
			{
				throw DataException.wrap(e);
			}
		}
	}

	/** Executes the fetch script; returns the result */
	public boolean fetch() throws DataException
	{
		if ( scriptEventHandler != null )
		{
			try
			{
				return scriptEventHandler.handleFetch( this, this.getDataRow() );
			}
			catch (BirtException e)
			{
				throw DataException.wrap(e);
			}
		}
		return false;
	}
	
    
	/** Executes the close script*/
	public void close() throws DataException
	{
		if ( scriptEventHandler != null )
		{
			try
			{
				scriptEventHandler.handleClose(this );
			}
			catch (BirtException e)
			{
				throw DataException.wrap(e);
			}
		}
		super.close();
	}

	/**
	 * Adds a dynamically described script data set column
	 * @see org.eclipse.birt.data.engine.api.script.IScriptDataSetMetaDataDefinition#addColumn(java.lang.String, java.lang.Class)
	 */
	public void addColumn( String name, Class dataType) throws BirtException 
	{
		if ( describedColumns == null )
			describedColumns = new ArrayList();
		if ( name == null || name.length() == 0 )
		{
			throw new DataException( ResourceConstants.CUSTOM_FIELD_EMPTY);
		}
		if ( dataType == null )
		{
			throw new DataException ( ResourceConstants.BAD_DATA_TYPE);
		}
		
		int nextIndex = describedColumns.size() + 1;
		// All script data set columns are "custom", to allow setting
		// values later
		ResultFieldMetadata c = new ResultFieldMetadata(
				nextIndex,name, name, dataType, dataType.getName(), true );
		describedColumns.add( c );
	}
	
	/** Executes the describe script*/
	public boolean describe() throws DataException
	{
		if ( scriptEventHandler != null )
		{
			try
			{
				return scriptEventHandler.handleDescribe(this, 
						this );
			}
			catch (BirtException e)
			{
				throw DataException.wrap(e);
			}
		}
		return false;
	}
	
	/**
	 * @see org.eclipse.birt.data.engine.api.script.IDataSetInstanceHandle#getExtensionID()
	 */
	public String getExtensionID()
	{
		// Not an ODA data set and has no extension. Use a fixed string
		return "SCRIPT";
	}


	/** 
	 * Gets columns defined by the describe event handler. 
	 * @Returns A list of ResultFieldMetadata objects. 
	 */
	List getDescribedMetaData()
	{
		return this.describedColumns;
	}
}
