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

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Comparator;

import org.eclipse.birt.data.engine.core.DataException;
import org.eclipse.birt.data.engine.i18n.ResourceConstants;
import org.eclipse.birt.data.engine.odi.IResultClass;
import org.eclipse.birt.data.engine.odi.IResultObject;

/**
 * Memory implementation of ResultSetCache
 */
class MemoryCache implements ResultSetCache
{
	private int countOfResult;
	private int currResultIndex = -1;
	
	private IResultClass  rsMeta;
	private IResultObject currResultObject;
	private IResultObject[] resultObjects;
	
	/**
	 * @param resultObjects
	 * @param comparator
	 */
	MemoryCache( IResultObject[] resultObjects, IResultClass rsMeta,
			Comparator comparator )
	{
		this.resultObjects = resultObjects;
		this.rsMeta = rsMeta;
		this.countOfResult = resultObjects.length;

		if ( comparator != null )
			Arrays.sort( this.resultObjects, comparator );
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
				currResultObject = resultObjects[currResultIndex];
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
		
		currResultIndex = destIndex;
	
		// currResultObject needs to be updated
		if ( currResultIndex == -1 || currResultIndex == countOfResult )
			currResultObject = null;
		else
			currResultObject = resultObjects[currResultIndex];
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
	public void reset( )
	{
		currResultIndex = -1;
		currResultObject = null;
	}

	/*
	 * @see org.eclipse.birt.data.engine.executor.cache.ResultSetCache#close()
	 */
	public void close( )
	{
		reset( );
		resultObjects = null;
	}

	/*
	 * @see org.eclipse.birt.data.engine.executor.cache.ResultSetCache#doSave(java.io.OutputStream)
	 */
	public void doSave( OutputStream outputStream, boolean isSubQuery ) throws DataException
	{
		ResultObjectUtil roUtil = ResultObjectUtil.newInstance( rsMeta );
		try
		{
			if ( isSubQuery == false )
			{
				roUtil.writeData( outputStream,
						resultObjects,
						resultObjects.length );
			}
			else
			{
				int currIndex = this.currResultIndex;				
				this.reset( );
				
				int posIndex = rsMeta.getFieldCount( );
				while ( this.next( ) )
				{
					IOUtil.writeInt( outputStream,
							( (Integer) this.getCurrentResult( )
									.getFieldValue( posIndex ) ).intValue( ) );
				}
				
				this.moveTo(currIndex);
			}
		}
		catch ( IOException e )
		{
			throw new DataException( "save error", e );
		}
	}

}