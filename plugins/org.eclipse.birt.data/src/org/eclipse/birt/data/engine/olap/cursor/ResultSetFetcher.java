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

package org.eclipse.birt.data.engine.olap.cursor;

import org.eclipse.birt.data.engine.olap.data.api.IAggregationResultSet;

/**
 * 
 *
 */
public class ResultSetFetcher
{

	private IAggregationResultSet rs;
	private int[] levelKeyColCount = null;

	/**
	 * 
	 * @param rs
	 */
	public ResultSetFetcher( IAggregationResultSet rs )
	{
		this.rs = rs;
		int levelCount = rs.getLevelCount( );
		levelKeyColCount = new int[levelCount];
		for ( int i = 0; i < levelCount; i++ )
		{
			levelKeyColCount[i] = rs.getLevelKeyColCount( i );
		}
	}

	/**
	 * 
	 * @param levelIndex
	 * @param attr
	 * @return
	 */
	public Object getValue( int levelIndex, int attr )
	{
		if ( attr >= levelKeyColCount[levelIndex] )
		{
			return rs.getLevelAttribute( levelIndex, attr
					- levelKeyColCount[levelIndex] );
		}
		else
		{
			return rs.getLevelKeyValue( levelIndex )[attr];
		}
	}

	/**
	 * 
	 * @param levelIndex
	 * @param attrName
	 * @return
	 */
	public int getAttributeIndex( int levelIndex, String attrName )
	{
		int index = rs.getLevelKeyIndex( levelIndex, attrName );
		if ( index >= 0 )
		{
			return index;
		}
		index = rs.getLevelAttributeIndex( levelIndex, attrName );
		if ( index >= 0 )
		{
			return levelKeyColCount[levelIndex] + index;
		}
		return -1;
	}

	/**
	 * 
	 * @param levelIndex
	 * @return
	 */
	public Object[] getLevelKeyValue( int levelIndex )
	{
		return rs.getLevelKeyValue( levelIndex );
	}
}
