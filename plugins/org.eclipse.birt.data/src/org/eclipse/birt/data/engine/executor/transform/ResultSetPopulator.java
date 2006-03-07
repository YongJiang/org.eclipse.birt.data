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

package org.eclipse.birt.data.engine.executor.transform;

import org.eclipse.birt.data.engine.core.DataException;
import org.eclipse.birt.data.engine.executor.BaseQuery;
import org.eclipse.birt.data.engine.executor.cache.ResultSetCache;
import org.eclipse.birt.data.engine.executor.cache.SmartCache;
import org.eclipse.birt.data.engine.executor.transform.group.GroupProcessorManager;
import org.eclipse.birt.data.engine.odi.IResultClass;
import org.eclipse.birt.data.engine.odi.IResultIterator;

/**
 * This is the entry class for population of ResultSet. The instance of this
 * class maintains several critical objects which may used during the whole
 * population process.
 * 
 */
public class ResultSetPopulator
{

	/** data rows holds real data */
	private ResultSetCache smartCache;

	/** the expression processor which is used to parse JS expressions */
	private IExpressionProcessor exprProcessor = null;

	/** the query against which the result set is populated */
	private BaseQuery query;

	/** the result set meta data of the result set */
	private IResultClass rsMeta;

	/**
	 * the IResultIterator instance to which the final populated ResultSet is
	 * output
	 */
	private IResultIterator ri;

	/**
	 * the instance of GroupProcessorManager, which is used to do group-related
	 * jobs.
	 */
	private GroupProcessorManager groupProcessorManager;

	/**
	 * 
	 * @param query
	 * @param rsMeta
	 * @param ri
	 * @throws DataException
	 */
	ResultSetPopulator( BaseQuery query, IResultClass rsMeta, IResultIterator ri )
			throws DataException
	{
		this.query = query;
		this.rsMeta = rsMeta;
		this.ri = ri;
		this.groupProcessorManager = new GroupProcessorManager( query,
				this.ri,
				this.rsMeta,
				this );
		// Initialize the ExpressionProcessor.
		this.exprProcessor = query.getExprProcessor( );
		// Set the query which is used by IExpressionProcessor
		this.exprProcessor.setQuery( this.query );
	}

	/**
	 * 
	 * @return
	 */
	public GroupProcessorManager getGroupProcessorManager( )
	{
		return this.groupProcessorManager;
	}

	/**
	 * 
	 * @return
	 */
	public BaseQuery getQuery( )
	{
		return this.query;
	}

	/**
	 * 
	 * @param query
	 */
	void setQuery( BaseQuery query )
	{
		this.query = query;
	}

	/**
	 * Actually this cache is not supposed to be visible. But in report document
	 * presentation, sub query needs to find its result objects from its parent
	 * result set, so its parent cache has to be known. A better solution needs
	 * to be thought out for this issue.
	 * 
	 * TODO: enhance me
	 * 
	 * @return smartCache
	 */
	public ResultSetCache getCache( )
	{
		return this.smartCache;
	}

	/**
	 * Set the ResultSetCache of this ResultSetPopulator.
	 * 
	 * @param cache
	 */
	public void setCache( ResultSetCache cache )
	{
		smartCache = cache;
	}

	/**
	 * Return the ResultSetMetadata of this ResultSetPopulator.
	 * 
	 * @return
	 */
	public IResultClass getResultSetMetadata( )
	{
		return this.rsMeta;
	}

	/**
	 * 
	 * @param resultMeta
	 */
	public void setResultSetMetadata( IResultClass resultMeta )
	{
		this.rsMeta = resultMeta;
	}

	/**
	 * Return the IResultIterator instance this ResultSetPopulator servers with.
	 * 
	 * @return
	 */
	public IResultIterator getResultIterator( )
	{
		return this.ri;
	}

	public IExpressionProcessor getExpressionProcessor( )
	{
		return this.exprProcessor;
	}

	/**
	 * Populate the result set. In this method we would firstly prepare the data
	 * needed to be used in population process, then call doPopulate method to
	 * carry out the actual population job.
	 * 
	 * @param odaResultSet
	 * @throws DataException
	 */
	public void populateResultSet( OdiResultSetWrapper odaResultSet ) throws DataException
	{

		Object[] fetchEventsList = ResultSetPopulatorUtil.getFetchEventListFromQuery( this );

		// If there are aggregations in Computed Columns, then the group
		// filtering should not
		// be supported for that the aggregation result would be affected by
		// group filtering.
		boolean allowGroupFiltering = !ResultSetPopulatorUtil.hasAggregationsInComputedColumns( fetchEventsList,
				this );

		// If there are some aggregations in computed columns, or there are some
		// multipass filters
		// then dealing with those aggregations/multipass filters. Else start
		// population directly
		boolean needMultipassRowProcessing = ( !allowGroupFiltering )
				|| FilterUtil.hasMultiPassFilters( fetchEventsList );

		RowProcessorFactory.getRowProcessor( this, needMultipassRowProcessing )
				.pass( odaResultSet );

		if ( query.getGrouping( ) != null )
			groupProcessorManager.doGroupFilteringAndSorting( this.smartCache,
					exprProcessor,
					allowGroupFiltering );
	}

	/**
	 * Use the given OrderingInfo, re-set the smartCache
	 * 
	 * @param odInfo
	 * @throws DataException
	 */
	public void reSetSmartCacheUsingOrderingInfo( OrderingInfo odInfo )
			throws DataException
	{
		reSetCache( odInfo );
		this.groupProcessorManager.getGroupCalculationUtil( )
				.getGroupInformationUtil( )
				.doGrouping( );
		this.getCache( ).next( );
	}

	/**
	 * @param odInfo
	 * @throws DataException
	 */
	public void reSetCache( OrderingInfo odInfo ) throws DataException
	{
		this.getCache( ).reset( );
		this.getCache( ).next( );
		this.setCache( new SmartCache( query,
				this.getCache( ),
				odInfo,
				this.rsMeta,
				null ) );
		this.groupProcessorManager.getGroupCalculationUtil( )
				.setResultSetCache( this.getCache( ) );
	}

	/**
	 * 
	 * @param groupLevel
	 * @throws DataException
	 */
	void first( int groupLevel ) throws DataException
	{
		this.groupProcessorManager.getGroupCalculationUtil( )
				.getGroupInformationUtil( )
				.first( groupLevel );
	}

	/**
	 * 
	 * @param groupLevel
	 * @throws DataException
	 */
	void last( int groupLevel ) throws DataException
	{
		this.groupProcessorManager.getGroupCalculationUtil( )
				.getGroupInformationUtil( )
				.last( groupLevel );
	}

	/**
	 * @see org.eclipse.birt.data.engine.odi.IResultIterator#getEndingGroupLevel()
	 */
	int getEndingGroupLevel( ) throws DataException
	{
		return this.groupProcessorManager.getGroupCalculationUtil( )
				.getGroupInformationUtil( )
				.getEndingGroupLevel( );
	}

	/**
	 * @see org.eclipse.birt.data.engine.odi.IResultIterator#getStartingGroupLevel()
	 */
	int getStartingGroupLevel( ) throws DataException
	{
		return this.getGroupProcessorManager( )
				.getGroupCalculationUtil( )
				.getGroupInformationUtil( )
				.getStartingGroupLevel( );
	}

	/**
	 * @see org.eclipse.birt.data.engine.odi.IResultIterator#getCurrentGroupIndex(int)
	 */
	int getCurrentGroupIndex( int groupLevel ) throws DataException
	{
		return this.getGroupProcessorManager( )
				.getGroupCalculationUtil( )
				.getGroupInformationUtil( )
				.getCurrentGroupIndex( groupLevel );
	}

	/**
	 * @see org.eclipse.birt.data.engine.odi.IResultIterator#getGroupStartAndEndIndex(int)
	 */
	public int[] getGroupStartAndEndIndex( int groupLevel )
			throws DataException
	{
		return this.getGroupProcessorManager( )
				.getGroupCalculationUtil( )
				.getGroupInformationUtil( )
				.getGroupStartAndEndIndex( groupLevel );
	}

}
