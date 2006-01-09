/*******************************************************************************
 * Copyright (c) 2004 Actuate Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Actuate Corporation  - initial API and implementation
 *******************************************************************************/

package org.eclipse.birt.data.engine.executor.cache;

import org.eclipse.birt.data.engine.core.DataException;
import org.eclipse.birt.data.engine.executor.dscache.DataSetResultCache;
import org.eclipse.birt.data.engine.odaconsumer.ResultSet;
import org.eclipse.birt.data.engine.odi.ICustomDataSet;
import org.eclipse.birt.data.engine.odi.IResultIterator;
import org.eclipse.birt.data.engine.odi.IResultObject;

/**
 * Adapt Oda and Odi interface to a single class, which will provide a uniform
 * method to retrieve data.
 */
class OdiAdapter
{
	// from Oda
	private ResultSet odaResultSet;
	
	// from oda cache
	private DataSetResultCache datasetCache;

	// from odi
	private ICustomDataSet customDataSet;
	
	// from IResultIterator
	private IResultIterator resultIterator;

	//The behavior of "next" method in IResultIterator is slightly
	//different from that of "fetch" method.To mimic the behavior of 
	//fetch method we define a boolean to mark the beginning of an IResultIterator
	boolean riStarted = false;
	
	// from parent query in sub query
	private ResultSetCache resultSetCache;

	// from input stream
	private ResultObjectReader roReader;
	
	/**
	 * Construction
	 * 
	 * @param odaResultSet
	 */
	OdiAdapter( ResultSet odaResultSet )
	{
		assert odaResultSet != null;
		this.odaResultSet = odaResultSet;
	}
	
	/**
	 * Construction
	 * 
	 * @param datasetCacheResultSet
	 */
	OdiAdapter( DataSetResultCache datasetCache )
	{
		assert datasetCache != null;
		this.datasetCache = datasetCache;
	}

	/**
	 * Construction
	 * 
	 * @param customDataSet
	 */
	OdiAdapter( ICustomDataSet customDataSet )
	{
		assert customDataSet != null;
		this.customDataSet = customDataSet;
	}

	/**
	 * Construction
	 * 
	 * @param customDataSet
	 */
	OdiAdapter( ResultSetCache resultSetCache )
	{
		assert resultSetCache != null;
		this.resultSetCache = resultSetCache;
	}

	/**
	 * Construction
	 * 
	 * @param customDataSet
	 */
	OdiAdapter( IResultIterator resultSetCache )
	{
		assert resultSetCache != null;
		this.resultIterator = resultSetCache;
	}
	
	/**
	 * Construction
	 * 
	 * @param roReader
	 */
	OdiAdapter( ResultObjectReader roReader )
	{
		assert roReader != null;
		this.roReader = roReader;
	}
	
	
	/**
	 * Fetch data from Oda or Odi. After the fetch is done, the cursor
	 * must stay at the row which is fetched.
	 * @return IResultObject
	 * @throws DataException
	 */
	IResultObject fetch( ) throws DataException
	{
		if ( odaResultSet != null )
		{
			return odaResultSet.fetch( );
		}
		if ( datasetCache != null )
		{
			return datasetCache.fetch( );
		}
		else if ( customDataSet != null )
		{
			return customDataSet.fetch( );
		}
		else if ( resultIterator != null )
		{
			if(!riStarted)
				riStarted = true;
			else
				this.resultIterator.next();
			
			return this.resultIterator.getCurrentResult();
		}
		else if ( roReader != null )
		{
			return roReader.fetch( );
		}
		else
		{
			return resultSetCache.fetch( );
		}
	}

}