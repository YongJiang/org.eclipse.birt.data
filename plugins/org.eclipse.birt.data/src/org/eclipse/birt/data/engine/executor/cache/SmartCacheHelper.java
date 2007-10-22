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

import org.eclipse.birt.data.engine.core.DataException;
import org.eclipse.birt.data.engine.executor.ResultObject;
import org.eclipse.birt.data.engine.executor.cache.disk.DiskCache;
import org.eclipse.birt.data.engine.impl.DataEngineSession;
import org.eclipse.birt.data.engine.impl.StopSign;
import org.eclipse.birt.data.engine.odaconsumer.ResultSet;
import org.eclipse.birt.data.engine.odi.IEventHandler;
import org.eclipse.birt.data.engine.odi.IResultClass;
import org.eclipse.birt.data.engine.odi.IResultObject;
import org.eclipse.birt.data.engine.olap.data.util.CompareUtil;

/**
 * Help SmartCache to get the ResultSetCache, the real data cache.
 */
class SmartCacheHelper
{
	/** concrete implementation of ResultSetCache */
	private ResultSetCache resultSetCache;

	private IEventHandler eventHandler;

	// log instance
	private static Logger logger = Logger.getLogger( SmartCache.class.getName( ) );

	private DataEngineSession session;
	
	SmartCacheHelper( DataEngineSession session )
	{
		this.session = session;
	}
	/**
	 * Retrieve data from ODA, used in normal query
	 * 
	 * @param odaResultSet
	 * @param query
	 * @param rsMeta
	 * @param sortSpec
	 * @param stopSign
	 * @throws DataException
	 */
	ResultSetCache getResultSetCache( CacheRequest cacheRequest,
			ResultSet odaResultSet, IResultClass rsMeta, StopSign stopSign ) throws DataException
	{
		assert cacheRequest != null;
		assert odaResultSet != null;
		assert rsMeta != null;

		if ( cacheRequest.getDistinctValueFlag( ) == true )
		{
			SmartCacheHelper smartCacheHelper = new SmartCacheHelper( session );
			ResultSetCache smartCache = smartCacheHelper.getDistinctResultSetCache( cacheRequest,
					odaResultSet,
					rsMeta,
					stopSign);

			cacheRequest.setDistinctValueFlag( false );
			initInstance( cacheRequest, new OdiAdapter( smartCache ), rsMeta, stopSign );
		}
		else
		{
			initOdaResult( cacheRequest, odaResultSet, rsMeta, stopSign );
		}

		return this.resultSetCache;
	}

	/**
	 * @param cacheRequest
	 * @param odaResultSet
	 * @param rsMeta
	 * @param ob
	 * @param stopSign
	 * @throws DataException
	 */
	private ResultSetCache getDistinctResultSetCache(
			CacheRequest cacheRequest, ResultSet odaResultSet,
			IResultClass rsMeta, StopSign stopSign ) throws DataException
	{
		SmartCacheHelper smartCacheHelper = new SmartCacheHelper( this.session );
		ResultSetCache smartCache = smartCacheHelper.getSortedResultSetCache( new CacheRequest( 0,
				null,
				getSortSpec( rsMeta ),
				cacheRequest.getEventHandler( ),
				true ),
				odaResultSet,
				rsMeta,
				stopSign);

		initInstance( cacheRequest, new OdiAdapter( smartCache ), rsMeta, stopSign );
		return this.resultSetCache;
	}
	
	/**
	 * @param rsMeta
	 * @return
	 */
	private static SortSpec getSortSpec( IResultClass rsMeta )
	{
		int fieldCount = rsMeta.getFieldCount( );
		int[] sortKeyIndexs = new int[fieldCount];
		String[] sortKeyNames = new String[fieldCount];
		boolean[] ascending = new boolean[fieldCount];
		for ( int i = 0; i < fieldCount; i++ )
		{
			sortKeyIndexs[i] = i + 1; // 1-based
			ascending[i] = true;
		}
		return new SortSpec( sortKeyIndexs, sortKeyNames, ascending );
	}

	/**
	 * @param cacheRequest
	 * @param odaResultSet
	 * @param rsMeta
	 * @param ob
	 * @param ob1
	 * @throws DataException
	 */
	private ResultSetCache getSortedResultSetCache( CacheRequest cacheRequest,
			ResultSet odaResultSet, IResultClass rsMeta, StopSign stopSign ) throws DataException
	{
		initOdaResult( cacheRequest, odaResultSet, rsMeta, stopSign );
		return this.resultSetCache;
	}

	/**
	 * @param cacheRequest
	 * @param odaResultSet
	 * @param rsMeta
	 * @param stopSign
	 * @throws DataException
	 */
	private void initOdaResult( CacheRequest cacheRequest,
			ResultSet odaResultSet, IResultClass rsMeta, StopSign stopSign ) throws DataException
	{
		OdiAdapter odiAdpater = new OdiAdapter( odaResultSet );
		initInstance( cacheRequest, odiAdpater, rsMeta, stopSign );
	}

	/**
	 * Retrieve data from ODI, used in sub query
	 * 
	 * @param query
	 * @param resultCache,
	 *            parent resultSetCache
	 * @param startIndex,
	 *            included
	 * @param endIndex,
	 *            excluded
	 * @param rsMeta
	 * @param sortSpec
	 * @param stopSign
	 * @throws DataException
	 */
	ResultSetCache getResultSetCache( CacheRequest cacheRequest,
			ResultSetCache resultCache, int startIndex, int endIndex,
			IResultClass rsMeta, StopSign stopSign ) throws DataException
	{
		assert cacheRequest != null;
		assert resultCache != null;
		assert rsMeta != null;

		OdiAdapter odiAdpater = new OdiAdapter( resultCache );
		initSubResult( cacheRequest,
				resultCache,
				odiAdpater,
				startIndex,
				endIndex,
				rsMeta,
				stopSign);

		return this.resultSetCache;
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
	 * @param stopSign
	 * @throws DataException
	 */
	private void initSubResult( CacheRequest cacheRequest,
			ResultSetCache resultCache, OdiAdapter odiAdpater, int startIndex,
			int endIndex, IResultClass rsMeta, StopSign stopSign ) throws DataException
	{
		int length = endIndex - startIndex;
		if ( cacheRequest.getMaxRow( ) <= 0
				|| length <= cacheRequest.getMaxRow( ) )
			cacheRequest.setMaxRow( length );

		int oldIndex = resultCache.getCurrentIndex( );

		// In OdiAdapter, it fetches the next row, not current row.
		resultCache.moveTo( startIndex - 1 );
		initInstance( cacheRequest, odiAdpater, rsMeta, stopSign );

		resultCache.moveTo( oldIndex );
	}

	/**
	 * @param cacheRequest
	 * @param odiAdapter
	 * @param rsMeta
	 * @throws DataException
	 */
	ResultSetCache getResultSetCache( CacheRequest cacheRequest,
			OdiAdapter odiAdapter, IResultClass rsMeta, StopSign stopSign ) throws DataException
	{
		initInstance( cacheRequest, odiAdapter, rsMeta, stopSign );
		return this.resultSetCache;
	}

	/**
	 * @param cacheRequest
	 * @param rowResultSet
	 * @param rsMeta
	 * @throws DataException
	 */
	ResultSetCache getResultSetCache( CacheRequest cacheRequest,
			IRowResultSet rowResultSet, IResultClass rsMeta, StopSign stopSign )
			throws DataException
	{
		this.eventHandler = cacheRequest.getEventHandler( );
		populateData( rowResultSet, rsMeta, cacheRequest.getSortSpec( ), stopSign );
		return this.resultSetCache;
	}

	/**
	 * Init resultSetCache
	 * 
	 * @param odiAdpater
	 * @param query
	 * @param rsMeta
	 * @param sortSpec
	 * @param stopSign
	 * @throws DataException
	 */
	private void initInstance( CacheRequest cacheRequest,
			OdiAdapter odiAdpater, IResultClass rsMeta, StopSign stopSign ) throws DataException
	{
		this.eventHandler = cacheRequest.getEventHandler( );
		IRowResultSet rowResultSet = new ExpandableRowResultSet( new SmartCacheRequest( cacheRequest.getMaxRow( ),
				cacheRequest.getFetchEvents( ),
				odiAdpater,
				rsMeta,
				cacheRequest.getDistinctValueFlag( ) ) );
		populateData( rowResultSet, rsMeta, cacheRequest.getSortSpec( ), stopSign );
	}

	/**
	 * Populate the smartCache.
	 * 
	 * @param rsMeta
	 * @param rowResultSet
	 * @param sortSpec
	 * @param stopSign
	 * @throws DataException
	 */
	private void populateData( IRowResultSet rowResultSet, IResultClass rsMeta,
			SortSpec sortSpec, StopSign stopSign ) throws DataException
	{
		long startTime = System.currentTimeMillis( );
		SizeOfUtil sizeOfUtil = new SizeOfUtil( rsMeta );

		// compute the number of rows which can be cached in memory
		long memoryCacheSize = CacheUtil.computeMemoryBufferSize( eventHandler.getAppContext() );
		
		IResultObject odaObject;
		IResultObject[] resultObjects;
		List resultObjectsList = new ArrayList( );

		int dataCount = 0;
		long usedMemorySize = 0;

		while ( ( odaObject = rowResultSet.next( stopSign ) ) != null && !stopSign.isStopped( ) )
		{
			if ( usedMemorySize < memoryCacheSize )
			{
				dataCount++;
				
				//the followed variable is for performance
				int odaObjectFieldCount = odaObject.getResultClass( ).getFieldCount( );
				int metaFieldCount = rsMeta.getFieldCount( );
				if(odaObjectFieldCount < metaFieldCount)
				{
					//Populate Data according to the given meta data.
					Object[] obs = new Object[metaFieldCount];
					for ( int i = 1; i <= odaObjectFieldCount; i++ )
					{
						obs[i - 1] = odaObject.getFieldValue( i );
					}
					ResultObject temp = new ResultObject( rsMeta, obs );
					resultObjectsList.add( temp );
					usedMemorySize += sizeOfUtil.sizeOf( temp );
				}
				else
				{
					resultObjectsList.add( odaObject );
					usedMemorySize += sizeOfUtil.sizeOf( odaObject );
				}
				
			}
			else
			{
				logger.fine( "DisckCache is used" );

				resultObjects = (IResultObject[]) resultObjectsList.toArray( new IResultObject[0] );
				// the order is: resultObjects, odaObject, rowResultSet
				resultSetCache = new DiskCache( resultObjects,
						odaObject,
						rowResultSet,
						rsMeta,
						getComparator( sortSpec, eventHandler ),
						dataCount,
						this.session,
						stopSign );
				break;
			}
		}

		if ( resultSetCache == null )
		{
			logger.fine( "MemoryCache is used" );

			resultObjects = (IResultObject[]) resultObjectsList.toArray( new IResultObject[0] );

			resultSetCache = new MemoryCache( resultObjects,
					rsMeta,
					getComparator( sortSpec, eventHandler ) );
		}

		odaObject = null;
		resultObjects = null;
		resultObjectsList = null;
		rowResultSet = null;

		long consumedTime = ( System.currentTimeMillis( ) - startTime ) / 1000;
		logger.fine( "Time consumed by cache is: " + consumedTime + " second" );
	}
	
	/**
	 * @param sortSpec
	 * @return Comparator based on specified sortSpec, null indicates there is
	 *         no need to do sorting
	 */
	private static Comparator getComparator( SortSpec sortSpec,
			final IEventHandler eventHandler )
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

						int result = CompareUtil.compare( colObj1, colObj2 );
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
		};

		return comparator;
	}
}
