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

import java.io.File;
import java.io.IOException;

import org.eclipse.birt.data.engine.executor.cache.ResultObjectUtil;
import org.eclipse.birt.data.engine.impl.StopSign;
import org.eclipse.birt.data.engine.odi.IResultObject;

/**
 * Provide the service of reading/writing objects from one file It makes the
 * reading/writing objects transparent to DiskMergeSort.
 */
class RowFile implements IRowIterator
{
	private File tempFile = null;
	
	private ResultObjectUtil resultObjectUtil;
	
	private int readPos = 0;

	private int rowCount = 0;
	private IResultObject[] memoryRowCache = null;
	
	private DataFileReader dfr = null;
	private DataFileWriter dfw = null;
	
	/**
	 * 
	 * @param file
	 * @param resultObjectUtil
	 * @param cacheSize
	 */
	RowFile( File file, ResultObjectUtil resultObjectUtil, int cacheSize )
	{
		assert file != null;
		
		this.tempFile = file;
		this.resultObjectUtil = resultObjectUtil;
		setCacheSize( cacheSize );
	}
	
	//-------------------------write-----------------------
	/**
	 * Set cache size and initialize cache.
	 * @param cacheSize
	 */
	private void setCacheSize( int cacheSize )
	{
		if ( cacheSize >= 0 )
			memoryRowCache = new IResultObject[cacheSize];
	}

	/**
	 * 
	 * @param resultObject
	 * @throws IOException
	 */
	void write( IResultObject resultObject ) throws IOException
	{
		IResultObject[] resultObjects = new IResultObject[1];
		resultObjects[0] = resultObject;
		writeRows( resultObjects, 1, null );
	}

	/**
	 * Write one object to file.
	 *  
	 * @param resultObjects
	 * @param count
	 * @param stopSign
	 * @throws IOException
	 */
	void writeRows( IResultObject[] resultObjects, int count, StopSign stopSign )
			throws IOException
	{
		int cacheFreeSize = memoryRowCache.length - rowCount;
		if ( cacheFreeSize >= count )
		{
			writeRowsToCache( resultObjects, 0, count );
		}
		else
		{
			if ( cacheFreeSize > 0 )
			{
				writeRowsToCache( resultObjects, 0, cacheFreeSize );
				writeRowsToFile( resultObjects, cacheFreeSize, count
						- cacheFreeSize, stopSign );
			}
			else
			{
				writeRowsToFile( resultObjects, 0, count, stopSign );
			}
		}
	}

	/**
	 * Write objects to cache.
	 * 
	 * @param resultObjects
	 * @param count
	 * @throws IOException
	 */
	private void writeRowsToCache( IResultObject[] resultObjects, int from,
			int count ) throws IOException
	{
		System.arraycopy( resultObjects, from, memoryRowCache, rowCount, count );
		rowCount += count;
	}

	/**
	 * Write objects to file. 
	 * @param resultObjects
	 * @param from
	 * @param count
	 * @throws IOException
	 */
	private void writeRowsToFile( IResultObject[] resultObjects, int from,
			int count, StopSign stopSign ) throws IOException
	{
		if ( dfw == null )
		{
			createWriter( );
		}
		dfw.write( getSubArray( resultObjects, from, count ), count, stopSign );
		rowCount += count;
	}
	
	/**
	 * Get subarray of a object array
	 * @param resultObjects
	 * @param count
	 * @throws IOException
	 */
	private IResultObject[] getSubArray( IResultObject[] resultObjects, int from,
			int count )
	{
		IResultObject[] subArray = new IResultObject[count];
		System.arraycopy( resultObjects, from, subArray, 0, count );
		return subArray;
	}
	
	/**
	 * Create a instance of DataFileWriter
	 *
	 */
	private void createWriter( )
	{
		dfw = DataFileWriter.newInstance( tempFile, resultObjectUtil );
	}

	/**
	 * End write operation. This mothed must be called before fetching row object.
	 */
	void endWrite( )
	{
		closeWriter();
	}
	
	/**
	 * Close current writer object
	 */
	private void closeWriter( )
	{
		if ( dfw != null )
		{
			dfw.close( );
			dfw = null;
		}
	}
	
	//	-------------------------read------------------------
	/*
	 * @see org.eclipse.birt.data.engine.executor.cache.IRowIterator#first()
	 */
	public void reset( )
	{
		readPos = 0;
		createReader( );
	}
	
	/*
	 * @see org.eclipse.birt.data.engine.executor.cache.IRowIterator#next()
	 */
	public IResultObject fetch( ) throws IOException
	{
		IResultObject resultObject = readRowFromCache( );
		if ( resultObject == null )
		{
			resultObject = readRowFromFile( );
		}
		
		return resultObject;
	}

	/**
	 * Read one object from cache. 
	 * 
	 * @return
	 * @throws IOException
	 */
	private IResultObject readRowFromCache( ) throws IOException
	{
		if ( readPos >= memoryRowCache.length )
		{
			return null;
		}
		return memoryRowCache[readPos++];
	}

	/**
	 * Read one object from file. 
	 * 
	 * @return
	 * @throws IOException
	 */
	private IResultObject readRowFromFile( ) throws IOException
	{
		if ( readPos >= rowCount )
		{
			return null;
		}
		if ( dfr == null )
		{
			createReader( );
		}
		readPos++;
		return ( dfr.read( 1, null ) )[0];
	}
	
	/**
	 * Create a instance of DataFileReader
	 *
	 */
	private void createReader( )
	{
		if ( dfr != null )
			dfr.close( );
		
		dfr = DataFileReader.newInstance( tempFile, resultObjectUtil );
	}
	
	/*
	 * @see org.eclipse.birt.data.engine.executor.cache.IRowIterator#close()
	 */
	public void close( )
	{
		closeReader( );

		if ( tempFile != null )
			tempFile.delete( );
		memoryRowCache = null;
	}
	
	/**
	 * Close current reader object
	 */
	private void closeReader( )
	{
		if ( dfr != null )
		{
			dfr.close( );
			dfr = null;
		}
	}
	
}
