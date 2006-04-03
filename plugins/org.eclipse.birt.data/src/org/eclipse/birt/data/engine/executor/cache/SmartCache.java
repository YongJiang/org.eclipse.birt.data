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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;

import org.eclipse.birt.core.data.DataTypeUtil;
import org.eclipse.birt.core.exception.BirtException;
import org.eclipse.birt.data.engine.core.DataException;
import org.eclipse.birt.data.engine.executor.ResultObject;
import org.eclipse.birt.data.engine.executor.dscache.DataSetResultCache;
import org.eclipse.birt.data.engine.executor.transform.OrderingInfo;
import org.eclipse.birt.data.engine.i18n.ResourceConstants;
import org.eclipse.birt.data.engine.odaconsumer.ResultSet;
import org.eclipse.birt.data.engine.odi.ICustomDataSet;
import org.eclipse.birt.data.engine.odi.IDataSetPopulator;
import org.eclipse.birt.data.engine.odi.IEventHandler;
import org.eclipse.birt.data.engine.odi.IResultClass;
import org.eclipse.birt.data.engine.odi.IResultIterator;
import org.eclipse.birt.data.engine.odi.IResultObject;

/**
 * Smart cache implementation of ResultSetCache. It is also the entry to new
 * instance of ResultSetCache. It will decide which concrete implemenation of
 * ResultCache should be used depending on the size of ResultSet. If all data
 * can be accomondated in memory, then MemoryCache will be used. Otherwise
 * DiskCache will be used.
 */
public class SmartCache implements ResultSetCache
{
	/** concrete implementation of ResultSetCache */
	private ResultSetCache resultSetCache;

	/**
	 * Very important parameter, indicates the max number of row which can be
	 * accomodated by available memory. In future, this vale needs to be changed
	 * to the maximum momery can be used, rather than static value of maimum
	 * result object can be loaded into memory. The unit of this value million
	 * bytes.
	 */
	private static int MemoryCacheSize;
	
	// log instance
	private static Logger logger = Logger.getLogger( SmartCache.class.getName( ) );
	
	// open flag
	private boolean isOpen = true;
	
	private IEventHandler eventHandler;
	
	/**
	 * Retrieve data from ODA, used in normal query
	 * 
	 * @param odaResultSet
	 * @param query
	 * @param rsMeta
	 * @param sortSpec
	 * @throws DataException
	 */
	public SmartCache( CacheRequest cacheRequest, ResultSet odaResultSet,
			IResultClass rsMeta ) throws DataException
	{
		assert cacheRequest != null;
		assert odaResultSet != null;
		assert rsMeta != null;

		this.eventHandler = cacheRequest.getEventHandler( );
		OdiAdapter odiAdpater = new OdiAdapter( odaResultSet );
		initInstance( cacheRequest, odiAdpater, rsMeta );
	}
	
	/**
	 * Retrieve data from IJointDataSetPopulator, used in joint data set.
	 * 
	 * @param odaResultSet
	 * @param query
	 * @param rsMeta
	 * @param sortSpec
	 * @throws DataException
	 */
	public SmartCache( CacheRequest cacheRequest, IDataSetPopulator populator,
			IResultClass rsMeta ) throws DataException
	{
		assert cacheRequest != null;
		assert populator != null;
		assert rsMeta != null;

		this.eventHandler = cacheRequest.getEventHandler( );
		OdiAdapter odiAdpater = new OdiAdapter( populator );
		initInstance( cacheRequest, odiAdpater, rsMeta );
	}
	
	/**
	 * Retrieve data from ODA cahce, used in normal query
	 * 
	 * @param odaResultSet
	 * @param query
	 * @param rsMeta
	 * @param sortSpec
	 * @throws DataException
	 */
	public SmartCache( CacheRequest cacheRequest,
			DataSetResultCache odaCacheResultSet, IResultClass rsMeta )
			throws DataException
	{
		assert cacheRequest != null;
		assert odaCacheResultSet != null;
		assert rsMeta != null;

		this.eventHandler = cacheRequest.getEventHandler( );
		OdiAdapter odiAdpater = new OdiAdapter( odaCacheResultSet );
		initInstance( cacheRequest, odiAdpater, rsMeta );
	}

	/**
	 * Retrieve data from ODI, used in multipass query
	 * 
	 * @param odaResultSet
	 * @param query
	 * @param rsMeta
	 * @param sortSpec
	 * @throws DataException
	 */
	public SmartCache( CacheRequest cacheRequest, IResultIterator odaResultSet,
			IResultClass rsMeta ) throws DataException
	{
		assert cacheRequest != null;
		assert odaResultSet != null;
		assert rsMeta != null;

		this.eventHandler = cacheRequest.getEventHandler( );
		OdiAdapter odiAdpater = new OdiAdapter( odaResultSet );
		initInstance( cacheRequest, odiAdpater, rsMeta );
	}
	/**
	 * Retrieve data from ODI, used in candidate query, such as Scripted DataSet
	 * 
	 * @param customDataSet
	 * @param query
	 * @param rsMeta
	 * @param sortSpec
	 * @throws DataException
	 */
	public SmartCache( CacheRequest cacheRequest, ICustomDataSet customDataSet,
			IResultClass rsMeta ) throws DataException
	{
		assert cacheRequest != null;
		assert customDataSet != null;
		assert rsMeta != null;

		this.eventHandler = cacheRequest.getEventHandler( );
		OdiAdapter odiAdpater = new OdiAdapter( customDataSet );
		initInstance( cacheRequest, odiAdpater, rsMeta );
	}

	/**
	 * Retrieve data from ODI, used in sub query
	 * 
	 * @param query
	 * @param resultCache, parent resultSetCache
	 * @param startIndex, included
	 * @param endIndex, excluded
	 * @param rsMeta
	 * @param sortSpec
	 * @throws DataException
	 */
	public SmartCache( CacheRequest cacheRequest, ResultSetCache resultCache,
			int startIndex, int endIndex, IResultClass rsMeta )
			throws DataException
	{
		assert cacheRequest != null;
		assert resultCache != null;
		assert rsMeta != null;

		this.eventHandler = cacheRequest.getEventHandler( );
		OdiAdapter odiAdpater = new OdiAdapter( resultCache );
		initInstance2( cacheRequest,
				resultCache,
				odiAdpater,
				startIndex,
				endIndex,
				rsMeta );
	}
	
	/**
	 * 
	 * @param query
	 * @param resultCache
	 * @param orderingInfo
	 * @param rsMeta
	 * @param sortSpec
	 * @throws DataException
	 */
	public SmartCache( CacheRequest cacheRequest, ResultSetCache resultCache,
			OrderingInfo orderingInfo, IResultClass rsMeta )
			throws DataException
	{
		assert resultCache != null;
		assert rsMeta != null;
	
		this.eventHandler = cacheRequest.getEventHandler( );
		initInstance3( resultCache, rsMeta, orderingInfo );
	}
	
	/**
	 * Init resultSetCache
	 * 
	 * @param odiAdpater
	 * @param query
	 * @param rsMeta
	 * @param sortSpec
	 * @throws DataException
	 */
	private void initInstance( CacheRequest cacheRequest,
			OdiAdapter odiAdpater, IResultClass rsMeta ) throws DataException
	{	
		RowResultSet rowResultSet = new RowResultSet( cacheRequest.getMaxRow( ),
				cacheRequest.getFetchEvents( ),
				odiAdpater,
				rsMeta );
		populateData( rsMeta, rowResultSet, cacheRequest.getSortSpec( ) );
	}
	
	/**
	 * Especially used for sub query
	 * 
	 * @param resultCache
	 * @param odiAdpater
	 * @param query
	 * @param startIndex
	 * @param endIndex
	 * @param rsMeta
	 * @param sortSpec
	 * @throws DataException
	 */
	private void initInstance2( CacheRequest cacheRequest,
			ResultSetCache resultCache, OdiAdapter odiAdpater, int startIndex,
			int endIndex, IResultClass rsMeta )
			throws DataException
	{
		int length = endIndex - startIndex;
		if ( cacheRequest.getMaxRow( ) == 0
				|| length <= cacheRequest.getMaxRow( ) )
			cacheRequest.setMaxRow( length );

		int oldIndex = resultCache.getCurrentIndex( );

		// In OdiAdapter, it fetches the next row, not current row.
		resultCache.moveTo( startIndex - 1 );
		initInstance( cacheRequest, odiAdpater, rsMeta );

		resultCache.moveTo( oldIndex );
	}

	/**
	 * Re-generate the SmartCache using columns defined in OrderingInfo
	 * 
	 * @param rsCache
	 * @param rsMeta
	 * @param orderingInfo
	 * @throws DataException
	 */
	private void initInstance3( ResultSetCache rsCache, IResultClass rsMeta,
			OrderingInfo orderingInfo ) throws DataException
	{
		SmartRowResultSet rowResultSet = new SmartRowResultSet( rsCache,
				rsMeta,
				orderingInfo );

		populateData( rsMeta, rowResultSet, null );
	}
	
	/**
	 * Populate the smartCache.
	 * @param rsMeta
	 * @param rowResultSet
	 * @param sortSpec
	 * @throws DataException
	 */
	private void populateData( IResultClass rsMeta, IRowResultSet rowResultSet,
			SortSpec sortSpec ) throws DataException
	{
		long startTime = System.currentTimeMillis( );

		// compute the number of rows which can be cached in memory
		int memoryCacheRowCount = computeCacheRowCount( rsMeta );
		logger.info( "memoryCacheRowCount is " + memoryCacheRowCount );
		
		IResultObject odaObject;
		IResultObject[] resultObjects;
		List resultObjectsList = new ArrayList( );
		
		int dataCount = 0;
		while ( ( odaObject = rowResultSet.next( ) ) != null )
		{
			dataCount++;
			// notice. it is less than or equal
			if ( dataCount <= memoryCacheRowCount ) 
			{
				//Populate Data according to the given meta data.
				Object[] obs = new Object[rsMeta.getFieldCount()];
				for(int i = 1; i <= rsMeta.getFieldCount(); i++)
				{
					if( i <= odaObject.getResultClass().getFieldCount() )
						obs[i-1] = odaObject.getFieldValue( rsMeta.getFieldName(i));
					else
						obs[i-1] = null;
				}
				resultObjectsList.add( new ResultObject( rsMeta, obs) );
			}
			else
			{
				logger.info( "DisckCache is used" );
				
				resultObjects = (IResultObject[]) resultObjectsList.toArray( new IResultObject[0] );
				// the order is: resultObjects, odaObject, rowResultSet
				resultSetCache = new DiskCache( resultObjects,
						odaObject,
						rowResultSet,
						rsMeta,
						getComparator( sortSpec ),
						memoryCacheRowCount );
				break;
			}
		}

		if ( resultSetCache == null )
		{
			logger.info( "MemoryCache is used" );
			
			resultObjects = (IResultObject[]) resultObjectsList.toArray( new IResultObject[0] );
			
			resultSetCache = new MemoryCache( resultObjects,
					rsMeta,
					getComparator( sortSpec ) );
		}
		
		odaObject = null;
		resultObjects = null;
		resultObjectsList = null;
		rowResultSet = null;
		
		long consumedTime = ( System.currentTimeMillis( ) - startTime ) / 1000;
		logger.info( "Time consumed by cache is: " + consumedTime + " second" );
	}
	
	/**
	 * According to avilable memory and specified rsMeta, the max number of rows
	 * which can be cached in memory. There is a potential issue associate with
	 * this value that current DtE is called in only one thread, but in the
	 * future it is probably that DtE is called by multi threads. Then available
	 * memory is shared by all thread, so the cacheSize needs to be adjusted.
	 * 
	 * @param rsMeta
	 * @return row count
	 */
	private int computeCacheRowCount( IResultClass rsMeta )
	{
		if ( MemoryCacheSize == 0 )
		{
			synchronized ( this )
			{
				if ( MemoryCacheSize == 0 )
				{
					MemoryCacheSize = 10; // default minimum value is 10M.
					String memcachesize = System.getProperty( "birt.data.engine.memcachesize" );
					if ( memcachesize != null )
					{
						try
						{
							MemoryCacheSize = Integer.parseInt( memcachesize );
						}
						catch ( Exception e )
						{
							// ignore it
						}
						
						if ( MemoryCacheSize < 10 )
						{
							throw new IllegalArgumentException( "the value of memcachesize should be at least 10" );
						}
					}
				}
			}
		}

		return computeCacheRowCount( MemoryCacheSize, rsMeta );
	}

	/**
	 * @param cacheSize,
	 *            the size of cache
	 * @param rsMeta,
	 *            the meta data of result set
	 * @return row count of memory cached
	 */
	private int computeCacheRowCount( int cacheSize, IResultClass rsMeta )
	{
		// below code only for unit test
		String memcachesizeOfTest = System.getProperty( "birt.data.engine.test.memcachesize" );
		if ( memcachesizeOfTest != null )
			return Integer.parseInt( memcachesizeOfTest );

		// here a simple assumption, that 1M memory can accomondate 2000 rows
		return cacheSize * 2000;
	}
	
	/*
	 * @see org.eclipse.birt.data.engine.executor.cache.ResultSetCache#getCount()
	 */
	public int getCount( ) throws DataException
	{
		checkOpenStates( );
		
		return resultSetCache.getCount( );
	}
	
	/*
	 * @see org.eclipse.birt.data.engine.executor.cache.ResultSetCache#getCurrentIndex()
	 */
	public int getCurrentIndex( ) throws DataException
	{
		checkOpenStates( );
		
		return resultSetCache.getCurrentIndex( );
	}
	
	/*
	 * @see org.eclipse.birt.data.engine.executor.cache.ResultSetCache#getCurrentResult()
	 */
	public IResultObject getCurrentResult( ) throws DataException
	{
		checkOpenStates( );
		
		return resultSetCache.getCurrentResult( );
	}

	/*
	 * @see org.eclipse.birt.data.engine.executor.cache.ResultSetCache#nextRow()
	 */
	public boolean next( ) throws DataException
	{
		checkOpenStates( );
		
		return resultSetCache.next( );
	}
	
	/*
	 * @see org.eclipse.birt.data.engine.executor.cache.ResultSetCache#fetch()
	 */
	public IResultObject fetch( ) throws DataException
	{
		checkOpenStates( );
		
		return resultSetCache.fetch( );
	}
	
	/*
	 * @see org.eclipse.birt.data.engine.executor.cache.ResultSetCache#moveTo(int)
	 */
	public void moveTo( int destIndex ) throws DataException
	{
		checkOpenStates( );
		
		resultSetCache.moveTo( destIndex );
	}

	/*
	 * @see org.eclipse.birt.data.engine.executor.cache.ResultSetCache#reset()
	 */
	public void reset( ) throws DataException
	{
		checkOpenStates( );
		
		resultSetCache.reset( );
	}

	/*
	 * @see org.eclipse.birt.data.engine.executor.cache.ResultSetCache#close()
	 */
	public void close( )
	{
		if ( isOpen == false )
			return;
		
		resultSetCache.close( );
		resultSetCache = null;
		isOpen = false;
	}
	
	/**
	 * @throws DataException
	 */
	private void checkOpenStates( ) throws DataException
	{
		if ( isOpen == false )
			throw new DataException( ResourceConstants.RESULT_CLOSED );
	}

	/**
	 * @param sortSpec
	 * @return Comparator based on specified sortSpec, null indicates there is
	 *         no need to do sorting
	 */
	private Comparator getComparator( SortSpec sortSpec )
	{
		if ( sortSpec == null )
			return null;

		final int[] sortKeyIndexes = sortSpec.sortKeyIndexes;
		final String[] sortKeyColumns = sortSpec.sortKeyColumns;
		
		if ( sortKeyIndexes == null || sortKeyIndexes.length == 0 )
			return null;

		final boolean[] sortAscending = sortSpec.sortAscending;

		Comparator comparator = new Comparator( ) {

			/**
			 * compares two row indexes, actually compares two rows pointed by
			 * the two row indexes
			 */
			public int compare( Object obj1, Object obj2 )
			{
				IResultObject row1 = (IResultObject) obj1;
				IResultObject row2 = (IResultObject) obj2;

				// compare group keys first
				for ( int i = 0; i < sortKeyIndexes.length; i++ )
				{
					int colIndex = sortKeyIndexes[i];
					String colName = sortKeyColumns[i];
					try
					{
						Object colObj1 = null;
						Object colObj2 = null;
						
						if ( eventHandler != null )
						{
							colObj1 = eventHandler.getValue( row1,
									colIndex,
									colName );
							colObj2 = eventHandler.getValue( row2,
									colIndex,
									colName );
						}
						else
						{
							colObj1 = row1.getFieldValue( colIndex );
							colObj2 = row2.getFieldValue( colIndex );
						}
						
						int result = doCompare( colObj1, colObj2 );
						if ( result != 0 )
						{
							return sortAscending[i] ? result : -result;
						}
					}
					catch ( DataException e )
					{
						// Should never get here
						// colIndex is always valid
					}
				}

				// all equal, so return 0
				return 0;
			}

			/**
			 * Compare two objects
			 * 
			 * @param colObj1
			 * @param colObj2
			 * @return true colObj1 equals colObj2
			 */
			private int doCompare( Object colObj1, Object colObj2 )
			{
				// default value is 0
				int result = 0;

				// 1: the case of reference is the same
				if ( colObj1 == colObj2 )
				{
					return result;
				}

				// 2: the case of one of two object is null
				if ( colObj1 == null || colObj2 == null )
				{
					// keep null value at the first position in ascending order
					if ( colObj1 == null )
					{
						result = -1;
					}
					else
					{
						result = 1;
					}
					return result;
				}

				// 3: other cases
				if ( colObj1.equals( colObj2 ) )
				{
					return result;
				}
				else if ( colObj1 instanceof Comparable
						&& colObj2 instanceof Comparable )
				{
					Comparable comp1 = (Comparable) colObj1;
					Comparable comp2 = (Comparable) colObj2;
					
					// Integer can not be compared with Double.
 					if ( colObj1.getClass( ) != colObj2.getClass( )
							&& colObj1 instanceof Number
							&& colObj2 instanceof Number )
					{
						try
						{
							comp1 = (Comparable) DataTypeUtil.toDouble( colObj1 );
							comp2 = (Comparable) DataTypeUtil.toDouble( colObj2 );
						}
						catch ( BirtException ex )
						{
							// impossible
						}
					}
 					
 					result = comp1.compareTo( comp2 );					
				}
				else if ( colObj1 instanceof Boolean
						&& colObj2 instanceof Boolean )
				{
					// false is less than true
					Boolean bool = (Boolean) colObj1;
					if ( bool.equals( Boolean.TRUE ) )
						result = 1;
					else
						result = -1;
				}
				else
				{
					// Should never get here
					//throw new UnsupportedOperationException( );
				}

				return result;
			}
		};

		return comparator;
	}
	
}