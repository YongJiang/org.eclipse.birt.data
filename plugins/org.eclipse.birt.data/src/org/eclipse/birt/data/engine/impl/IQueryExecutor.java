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
package org.eclipse.birt.data.engine.impl;

import org.eclipse.birt.data.engine.api.IResultMetaData;
import org.eclipse.birt.data.engine.api.script.IDataSourceInstanceHandle;
import org.eclipse.birt.data.engine.core.DataException;
import org.eclipse.birt.data.engine.odi.IResultIterator;
import org.mozilla.javascript.Scriptable;

/**
 * 
 */
public interface IQueryExecutor
{
	/**
	 * Execute query
	 * 
	 * @throws DataException
	 */
	void execute( ) throws DataException;

	/**
	 * @return shard scope
	 */
	Scriptable getSharedScope( );

	/**
	 * @return scope of JS aggregation object
	 */
	Scriptable getJSAggrValueObject( );
	
	/**
	 * @return the nested level of this executor
	 */
	int getNestedLevel( );

	/**
	 * @return
	 */
	IDataSourceInstanceHandle getDataSourceInstanceHandle( );

	/**
	 * @return the data set associated with this executor
	 */
	DataSetRuntime getDataSet( );

	/**
	 * @param nestedCount
	 * @return nested data set
	 */
	DataSetRuntime[] getNestedDataSets( int nestedCount );

	/**
	 * @return result set of underlying ODI layer
	 */
	IResultIterator getOdiResultSet( );

	/**
	 * @return meta data of data set
	 * @throws DataException
	 */
	IResultMetaData getResultMetaData( ) throws DataException;

	/**
	 * close
	 */
	void close( );
	
}
