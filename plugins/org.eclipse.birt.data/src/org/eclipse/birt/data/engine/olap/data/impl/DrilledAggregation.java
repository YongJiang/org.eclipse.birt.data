/*******************************************************************************
 * Copyright (c) 2010 Actuate Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Actuate Corporation  - initial API and implementation
 *******************************************************************************/
package org.eclipse.birt.data.engine.olap.data.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.birt.data.engine.core.DataException;
import org.eclipse.birt.data.engine.olap.api.query.ICubeQueryDefinition;
import org.eclipse.birt.data.engine.olap.data.api.DimLevel;
import org.eclipse.birt.data.engine.olap.data.api.IDimensionSortDefn;
import org.eclipse.birt.data.engine.olap.query.view.CubeQueryDefinitionUtil;

public class DrilledAggregation
{

	private int[] sortType;
	private List<AggregationDefinition> originalAggregationList;
	private DimLevel[] targetLevels;

	public DrilledAggregation( DimLevel[] targetLevels,
			ICubeQueryDefinition cubeQueryDefinition )
	{
		this.targetLevels = targetLevels;
		originalAggregationList = new ArrayList<AggregationDefinition>( );
		sortType = new int[targetLevels.length];
		for ( int index = 0; index < targetLevels.length; index++ )
		{
			try
			{
				sortType[index] = CubeQueryDefinitionUtil.getSortDirection( targetLevels[index],
						cubeQueryDefinition );
			}
			catch ( DataException e )
			{
				sortType[index] = IDimensionSortDefn.SORT_UNDEFINED;
			}
		}
	}

	public void addOriginalAggregation( AggregationDefinition aggregation )
	{
		originalAggregationList.add( aggregation );
	}

	public boolean usedByAggregation( AggregationDefinition aggregation )
	{
		return this.originalAggregationList.contains( aggregation );
	}
	
	public boolean matchTargetlevels( DimLevel[] levels )
	{
		if ( levels == targetLevels )
			return true;
		if ( levels == null )
			return false;
		if ( targetLevels.length != levels.length )
			return false;
		for ( int i = 0; i < targetLevels.length; i++ )
		{
			if ( !targetLevels[i].equals( levels[i] ) )
				return false;
		}
		return true;
	}
	
	public DimLevel[] getTargetLevels( )
	{
		return this.targetLevels;
	}
	
	public int[] getSortType( )
	{
		return this.sortType;
	}

	/**
	 * Get the drilled AggregationFunctionDefinition on this target dimlevel
	 * @return
	 */
	public AggregationFunctionDefinition[] getAggregationFunctionDefinition( )
	{
		Map<String, AggregationFunctionDefinition> functionMap = new HashMap<String, AggregationFunctionDefinition>( );
		for ( int i = 0; i < this.originalAggregationList.size( ); i++ )
		{
			AggregationDefinition aggr = originalAggregationList.get( i );
			for ( int j = 0; j < aggr.getAggregationFunctions( ).length; j++ )
			{
				if ( !functionMap.containsKey( aggr.getAggregationFunctions( )[j].getName( ) ) )
					functionMap.put( aggr.getAggregationFunctions( )[j].getName( ),
							aggr.getAggregationFunctions( )[j] );
			}
		}
		Iterator<Entry<String, AggregationFunctionDefinition>> iter = functionMap.entrySet( )
				.iterator( );
		AggregationFunctionDefinition[] aggr = new AggregationFunctionDefinition[functionMap.size( )];
		int index = 0;
		while ( iter.hasNext( ) )
		{
			aggr[index] = iter.next( ).getValue( );
			index++;
		}
		return aggr;
	}

}
