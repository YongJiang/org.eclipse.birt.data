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
package org.eclipse.birt.data.engine.executor.cache.disk;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.eclipse.birt.core.util.IOUtil;
import org.eclipse.birt.data.engine.core.DataException;
import org.eclipse.birt.data.engine.executor.cache.CacheUtil;
import org.eclipse.birt.data.engine.executor.cache.IRowResultSet;
import org.eclipse.birt.data.engine.executor.cache.ResultSetCache;
import org.eclipse.birt.data.engine.executor.cache.ResultSetUtil;
import org.eclipse.birt.data.engine.i18n.ResourceConstants;
import org.eclipse.birt.data.engine.impl.DataEngineSession;
import org.eclipse.birt.data.engine.impl.StopSign;
import org.eclipse.birt.data.engine.odi.IResultClass;
import org.eclipse.birt.data.engine.odi.IResultObject;

/**
 * Disk cache implementation of ResultSetCache
 */
public class DiskCache implements ResultSetCache
{	
	// current result data
	private int currResultIndex = -1;
	private IResultObject currResultObject;
	
	// the count of result
	private int countOfResult;
	
	// how many rows can be accomondated
	private int MemoryCacheRowCount;

	// goal file of this session
	private String goalFileStr;	
	private String sessionRootDirStr;
	
	// temporary root folder shared by all sessions
	private static String tempRootDirStr;
	
	// disk result set
	private DiskCacheResultSet diskBasedResultSet;
	
	// metadata
	private IResultClass rsMeta;
	
	// log instance
	private static Logger logger = Logger.getLogger( DiskCache.class.getName( ) );
	
	private DataEngineSession session;
	
	/**
	 * The MemoryCacheRowCount indicates the upper limitation of how many rows
	 * can be loaded into memory. Note this value is included as well. Look at
	 * the start three parameters of the parameter list, the first is the result
	 * object array which length is MemoryCacheRowCount, and the second is one
	 * result object which follows the object array according to the position
	 * sequence of data source. The last is the RowResultSet, and it might have
	 * or not have more result object.
	 * 
	 * @param resultObjects
	 * @param nextResultObject
	 * @param rowResultSet
	 * @param rsMeta
	 * @param comparator
	 * @param MemoryCacheRowCount
	 * @param stopSign
	 * @throws DataException
	 */
	public DiskCache( IResultObject[] resultObjects, IResultObject resultObject,
			IRowResultSet rowResultSet, IResultClass rsMeta,
			Comparator comparator, int MemoryCacheRowCount, DataEngineSession session, StopSign stopSign )
			throws DataException
	{
		//this.rsMeta = rsMeta;
		this.MemoryCacheRowCount = MemoryCacheRowCount;
		this.rsMeta = rsMeta;
		this.session = session;
		this.diskBasedResultSet = new DiskCacheResultSet( getInfoMap( ) );
		
		try
		{
			logger.info( "Start processStartResultObjects" );
			diskBasedResultSet.processStartResultObjects( resultObjects,
					comparator, stopSign );
			
			logger.info( "Start processRestResultObjects" );
			diskBasedResultSet.processRestResultObjects( resultObject,
					rowResultSet, stopSign );
		}
		catch ( IOException e )
		{
			throw new DataException( ResourceConstants.WRITE_TEMPFILE_ERROR, e );
		}
		countOfResult = diskBasedResultSet.getCount( );
		
		logger.info( "End of process, and the count of data is "
				+ countOfResult );
	}
	
	/*
	 * @see org.eclipse.birt.data.engine.executor.cache.ResultSetCache#getCurrentIndex()
	 */
	public int getCurrentIndex( ) throws DataException
	{
		return currResultIndex;
	}
	
	/*
	 * @see org.eclipse.birt.data.engine.executor.cache.ResultSetCache#getCurrentResult()
	 */
	public IResultObject getCurrentResult( ) throws DataException
	{
		return currResultObject;
	}

	/*
	 * @see org.eclipse.birt.data.engine.executor.cache.ResultSetCache#next()
	 */
	public boolean next( ) throws DataException
	{
		if ( currResultIndex > countOfResult - 1 )
		{
			currResultObject = null;
		}
		else
		{
			currResultIndex++;
			if ( currResultIndex == countOfResult )
				currResultObject = null;
			else
				try
				{
					currResultObject = diskBasedResultSet.nextRow( );
				}
				catch ( IOException e )
				{
					throw new DataException( ResourceConstants.READ_TEMPFILE_ERROR,
							e );
				}
		}
		
		return currResultObject != null;
	}
	
	/*
	 * @see org.eclipse.birt.data.engine.executor.cache.ResultSetCache#fetch()
	 */
	public IResultObject fetch( ) throws DataException
	{
		next( );
		IResultObject resultObject = getCurrentResult( );
		return resultObject;
	}
	
	/*
	 * @see org.eclipse.birt.data.engine.executor.cache.ResultSetCache#moveTo(int)
	 */
	public void moveTo( int destIndex ) throws DataException
	{
		checkValid( destIndex );
		
		int advancedStep;
		if ( destIndex >= currResultIndex )
		{
			advancedStep = destIndex - currResultIndex;
		}
		else
		{
			reset( );
			advancedStep = destIndex + 1;
		}

		for ( int i = 0; i < advancedStep; i++ )
			next( );

		currResultIndex = destIndex;
		
		// currResultObject needs to be updated
		if ( currResultIndex == -1 || currResultIndex == countOfResult )
			currResultObject = null;
	}

	/**
	 * Validate the value of destIndex
	 * 
	 * @param destIndex
	 * @throws DataException
	 */
	private void checkValid( int destIndex ) throws DataException
	{
		if ( destIndex < -1 || destIndex > countOfResult )
			throw new DataException( ResourceConstants.DESTINDEX_OUTOF_RANGE,
					new Object[]{
							new Integer( -1 ),
							new Integer( countOfResult )
					} );
	}
	
	/*
	 * @see org.eclipse.birt.data.engine.executor.cache.ResultSetCache#getCount()
	 */
	public int getCount( )
	{
		return countOfResult;
	}

	/*
	 * @see org.eclipse.birt.data.engine.executor.cache.ResultSetCache#reset()
	 */
	public void reset( ) throws DataException
	{		
		diskBasedResultSet.reset( );
		
		currResultIndex = -1;
		currResultObject = null;
	}

	/*
	 * @see org.eclipse.birt.data.engine.executor.cache.ResultSetCache#close()
	 */
	public void close( )
	{		
		diskBasedResultSet.close( );
		
		File goalFile = new File( goalFileStr );
		goalFile.delete( );
		File tempDir = new File( sessionRootDirStr );
		tempDir.delete( );
		
		currResultIndex = -1;
		currResultObject = null;
	}
	
	/**
	 * @return infoMap, including below information
	 * 		tempDir, to generated temp file in DiskMergeSort
	 * 		goalFile, to generate the end result file
	 * 		dataCountOfUnit, to indicate how many rows can be loaded into memory
	 * @throws DataException 
	 */
	private Map getInfoMap( ) throws DataException
	{
		Map infoMap = new HashMap( );

		infoMap.put( "tempDir", getTempDirStr( ) );
		goalFileStr = getGoalFileStr( );
		infoMap.put( "goalFile", goalFileStr );
		infoMap.put( "dataCountOfUnit", "" + MemoryCacheRowCount );

		return infoMap;
	}
	
	/**
	 * @return temp directory string, this folder is used to store the temporary
	 *         result in merge sort
	 * @throws DataException 
	 */
	private String getTempDirStr( ) throws DataException
	{
		return getSessionTempDirStr( ) + File.separator + "temp";
	}
	
	/**
	 * @return goal file of the end result, 
	 * @throws DataException 
	 */
	private String getGoalFileStr( ) throws DataException
	{
		return getSessionTempDirStr( ) + File.separator + "goalFile";
	}	

	/**
	 * @return temp directory string, this folder name is unique and then
	 *         different session will not influence each other, which can
	 *         support multi-thread
	 * @throws DataException 
	 */
	private String getSessionTempDirStr( ) throws DataException
	{
		if ( sessionRootDirStr != null )
			return sessionRootDirStr;

		// first create the root temp directory
		if ( tempRootDirStr == null )
			tempRootDirStr = createTempRootDir( );

		sessionRootDirStr = CacheUtil.createSessionTempDir( tempRootDirStr );
		return sessionRootDirStr; 
	}
	
	/**
	 * @return temp root dir directory
	 */
	private String createTempRootDir( )
	{
		if ( tempRootDirStr == null )
		{
			synchronized ( DiskCache.class )
			{
				if ( tempRootDirStr == null )
				{
					// tempDir is user specified temporary directory
					String tempDir = session.getTempDir( );
					tempRootDirStr = CacheUtil.createTempRootDir( tempDir );
				}
			}
		}
		return tempRootDirStr;
	}
	
	/*
	 * @see org.eclipse.birt.data.engine.executor.cache.ResultSetCache#saveToStream(java.io.OutputStream)
	 */
	public void doSave( DataOutputStream outputStream,
			DataOutputStream rowLensStream, Map cacheRequestMap )
			throws DataException
	{
		DataOutputStream dos = new DataOutputStream( outputStream );
		Set resultSetNameSet = ResultSetUtil.getRsColumnRequestMap( cacheRequestMap );
		try
		{
			// save data
			int rowCount = this.diskBasedResultSet.getCount( );
			int colCount = this.rsMeta.getFieldCount( );
			
			IOUtil.writeInt( dos, rowCount );
			
			int currIndex = this.currResultIndex;
			this.reset( );
			long offset = 4;
			for ( int i = 0; i < rowCount; i++ )
			{
				IOUtil.writeLong( rowLensStream, offset );
				offset += ResultSetUtil.writeResultObject( dos,
						this.diskBasedResultSet.nextRow( ),
						colCount,
						resultSetNameSet );
			}
			
			this.reset( );
			this.moveTo( currIndex );

			dos.close( );
		}
		catch ( IOException e )
		{
			throw new DataException( ResourceConstants.RD_SAVE_ERROR, e );
		}
	}
	
	/**
	 * 
	 * @param rsMeta
	 * @throws DataException
	 */
	public void setResultClass( IResultClass rsMeta ) throws DataException
	{
		this.rsMeta = rsMeta;
	}
}
