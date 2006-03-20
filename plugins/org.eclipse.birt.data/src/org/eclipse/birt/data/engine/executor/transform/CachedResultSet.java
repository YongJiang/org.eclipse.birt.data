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

package org.eclipse.birt.data.engine.executor.transform;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.eclipse.birt.data.engine.core.DataException;
import org.eclipse.birt.data.engine.executor.BaseQuery;
import org.eclipse.birt.data.engine.executor.ResultClass;
import org.eclipse.birt.data.engine.executor.dscache.DataSetResultCache;
import org.eclipse.birt.data.engine.i18n.ResourceConstants;
import org.eclipse.birt.data.engine.odaconsumer.ResultSet;
import org.eclipse.birt.data.engine.odi.ICustomDataSet;
import org.eclipse.birt.data.engine.odi.IResultClass;
import org.eclipse.birt.data.engine.odi.IResultIterator;
import org.eclipse.birt.data.engine.odi.IResultObject;

/**
 * OdiResultSet is responsible for accessing data sources and some processing
 * like sorting and filtering on the rows returned. It provide APIs for the
 * upper layer to fetch data rows and get group information, etc.
 * 
 */
public class CachedResultSet implements IResultIterator
{

	private ResultSetPopulator resultSetPopulator;

	/**
	 * Constructs and intializes OdiResultSet based on data in a ODA result set
	 */
	public CachedResultSet( BaseQuery query, IResultClass meta,
			ResultSet odaResultSet ) throws DataException
	{
		this.resultSetPopulator = new ResultSetPopulator( query, meta, this );
		resultSetPopulator.populateResultSet( new OdiResultSetWrapper( odaResultSet) );
	}

	/**
	 * @param query
	 * @param meta
	 * @param odaCacheResultSet
	 * @throws DataException
	 */
	public CachedResultSet( BaseQuery query, IResultClass meta,
			DataSetResultCache odaCacheResultSet ) throws DataException
	{
		this.resultSetPopulator = new ResultSetPopulator( query, meta, this );
		resultSetPopulator.populateResultSet( new OdiResultSetWrapper( odaCacheResultSet ));
		odaCacheResultSet.close( );
	}

	/**
	 * @param query
	 * @param customDataSet
	 * @throws DataException
	 */
	public CachedResultSet( BaseQuery query, ICustomDataSet customDataSet )
			throws DataException
	{
		assert customDataSet != null;
		this.resultSetPopulator = new ResultSetPopulator( query,
				customDataSet.getResultClass( ),
				this );
		resultSetPopulator.populateResultSet(new OdiResultSetWrapper( customDataSet));
	}

	/**
	 * @param parentResultIterator
	 * @param resultMetadata
	 * @param groupingLevel
	 * @throws DataException
	 */
	public CachedResultSet( BaseQuery query, IResultClass meta,
			IResultIterator parentResultIterator, int groupLevel )
			throws DataException
	{

		assert parentResultIterator instanceof CachedResultSet;
		CachedResultSet parentResultSet = (CachedResultSet) parentResultIterator;

		// this.resultSetPopulator.getGroupCalculationUtil( ).setResultSetCache(
		// parentResultSet.resultSetPopulator.getCache( ));
		int[] groupInfo = parentResultSet.getCurrentGroupInfo( groupLevel );
		this.resultSetPopulator = new ResultSetPopulator( query, meta, this );
		this.resultSetPopulator.populateResultSet( new OdiResultSetWrapper( new Object[]{
				parentResultSet.resultSetPopulator.getCache( ), groupInfo
		} ));
	}

	/**
	 * Returns all rows in the current group at the specified group level, as an
	 * array of ResultObject objects.
	 * 
	 * @param groupLevel
	 * @return int[], group star index and end index
	 * @throws DataException
	 */
	private int[] getCurrentGroupInfo( int groupLevel ) throws DataException
	{
		return this.resultSetPopulator.getGroupProcessorManager( )
				.getGroupCalculationUtil( )
				.getGroupInformationUtil( )
				.getCurrentGroupInfo( groupLevel );
	}

	/**
	 * @param resultClassStream
	 * @param resultSetStream
	 * @param groupInfoStream
	 * @param isSubQuery
	 * @throws DataException
	 */
	public void doSave( OutputStream resultClassStream,
			OutputStream groupInfoStream, boolean isSubQuery )
			throws DataException
	{
		assert groupInfoStream != null;
		BufferedOutputStream giBos = new BufferedOutputStream( groupInfoStream );
		this.resultSetPopulator.getGroupProcessorManager( )
				.getGroupCalculationUtil( )
				.doSave( giBos );
		try
		{
			giBos.close( );
		}
		catch ( IOException e )
		{
			throw new DataException( ResourceConstants.RD_SAVE_ERROR, e );
		}

		if ( isSubQuery == false )
		{
			assert resultClassStream != null;
			BufferedOutputStream rcBos = new BufferedOutputStream( resultClassStream );
			( (ResultClass) this.resultSetPopulator.getResultSetMetadata( ) ).doSave( rcBos );
			try
			{
				rcBos.close( );
			}
			catch ( IOException e )
			{
				throw new DataException( ResourceConstants.RD_SAVE_ERROR, e );
			}
		}
	}
	
	/*
	 * Close this data set
	 * 
	 * @see org.eclipse.birt.data.engine.odi.IResultIterator#close()
	 */
	public void close( )
	{
		if ( this.resultSetPopulator == null
				|| this.resultSetPopulator.getCache( ) == null )
			return; // already closed

		this.resultSetPopulator.getCache( ).close( );

		resultSetPopulator = null;
	}

	private void checkStarted( ) throws DataException
	{
		if ( this.resultSetPopulator == null
				|| this.resultSetPopulator.getCache( ) == null )
			throw new DataException( ResourceConstants.NO_CURRENT_ROW );
	}
	
	/*
	 * @see org.eclipse.birt.data.engine.odi.IResultIterator#getCurrentResult()
	 */
	public IResultObject getCurrentResult( ) throws DataException
	{
		checkStarted( );
		return this.resultSetPopulator.getCache( ).getCurrentResult( );
	}

	/*
	 * Advances row cursor, return false if no more rows.
	 * 
	 * @see org.eclipse.birt.data.engine.odi.IResultIterator#next()
	 */
	public boolean next( ) throws DataException
	{
		// Make sure that the result set has been opened.
		checkStarted( );
		boolean hasNext = this.resultSetPopulator.getCache( ).next( );

		this.resultSetPopulator.getGroupProcessorManager( )
				.getGroupCalculationUtil( )
				.getGroupInformationUtil( )
				.next( hasNext );

		return hasNext;
	}
	
	/*
	 * @see org.eclipse.birt.data.engine.odi.IResultIterator#getEndingGroupLevel()
	 */
	public int getEndingGroupLevel( ) throws DataException
	{
		return this.resultSetPopulator.getEndingGroupLevel( );
	}

	/**
	 * @see org.eclipse.birt.data.engine.odi.IResultIterator#getStartingGroupLevel()
	 */
	public int getStartingGroupLevel( ) throws DataException
	{
		return this.resultSetPopulator.getStartingGroupLevel( );
	}

	/*
	 * Rewinds row cursor to the first row at the specified group level
	 * 
	 * @see org.eclipse.birt.data.engine.odi.IResultIterator#first(int)
	 */
	public void first( int groupLevel ) throws DataException
	{
		this.resultSetPopulator.first( groupLevel );
	}
	
	/*
	 * Advances row cursor to the last row at the specified group level
	 * 
	 * @see org.eclipse.birt.data.engine.odi.IResultIterator#last(int)
	 */
	public void last( int groupLevel ) throws DataException
	{
		this.resultSetPopulator.last( groupLevel );
	}

	/*
	 * @see org.eclipse.birt.data.engine.odi.IResultIterator#getResultClass()
	 */
	public IResultClass getResultClass( ) throws DataException
	{
		return this.resultSetPopulator.getResultSetMetadata( );
	}

	/*
	 * @see org.eclipse.birt.data.engine.odi.IResultIterator#getCurrentResultIndex()
	 */
	public int getCurrentResultIndex( ) throws DataException
	{
		checkStarted( );
		return this.resultSetPopulator.getCache( ).getCurrentIndex( );
	}

	/*
	 * @see org.eclipse.birt.data.engine.odi.IResultIterator#getCurrentGroupIndex(int)
	 */
	public int getCurrentGroupIndex( int groupLevel ) throws DataException
	{
		return this.resultSetPopulator.getCurrentGroupIndex( groupLevel );
	}

	/*
	 * @see org.eclipse.birt.data.engine.odi.IResultIterator#getGroupStartAndEndIndex(int)
	 */
	public int[] getGroupStartAndEndIndex( int groupLevel )
			throws DataException
	{
		return this.resultSetPopulator.getGroupStartAndEndIndex( groupLevel );
	}
	
	/*
	 * @see org.eclipse.birt.data.engine.odi.IResultIterator#getRowCount()
	 */
	public int getRowCount( ) throws DataException
	{
		return this.resultSetPopulator.getCache( ).getCount( );
	}

}
