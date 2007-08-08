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

package org.eclipse.birt.data.engine.olap.query.view;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.birt.data.engine.api.aggregation.IBuildInAggregation;
import org.eclipse.birt.data.engine.api.querydefn.FilterDefinition;
import org.eclipse.birt.data.engine.core.DataException;
import org.eclipse.birt.data.engine.olap.api.query.ICubeQueryDefinition;
import org.eclipse.birt.data.engine.olap.api.query.IDimensionDefinition;
import org.eclipse.birt.data.engine.olap.api.query.IEdgeDefinition;
import org.eclipse.birt.data.engine.olap.api.query.IHierarchyDefinition;
import org.eclipse.birt.data.engine.olap.api.query.ILevelDefinition;
import org.eclipse.birt.data.engine.olap.api.query.IMeasureDefinition;
import org.eclipse.birt.data.engine.olap.data.api.DimLevel;
import org.eclipse.birt.data.engine.olap.impl.query.LevelDefiniton;
import org.eclipse.birt.data.engine.olap.impl.query.MeasureDefinition;
import org.eclipse.birt.data.engine.olap.util.ICubeAggrDefn;
import org.eclipse.birt.data.engine.olap.util.OlapExpressionUtil;
import org.eclipse.birt.data.engine.olap.util.filter.IJSMeasureFilterEvalHelper;
import org.eclipse.birt.data.engine.olap.util.filter.JSMeasureFilterEvalHelper;
import org.mozilla.javascript.Scriptable;

/**
 * Utility class
 *
 */
class CubeQueryDefinitionUtil
{

	/**
	 * Populate all aggregation member in CubeQueryDefinition. For initial
	 * implementation: we only consider IMeasureDefintion we will take into
	 * consider to handle the aggregation definition in binding expression;
	 * 
	 * @param queryDefn
	 * @return
	 * @throws DataException 
	 */
	static CalculatedMember[] getCalculatedMembers(
			ICubeQueryDefinition queryDefn, Scriptable scope ) throws DataException
	{
		List measureList = queryDefn.getMeasures( );
		ICubeAggrDefn[] cubeAggrs = OlapExpressionUtil.getAggrDefns( queryDefn.getBindings( ) );
		
		List cubeAggrBindingList = new ArrayList();
		for( int i=0; i< cubeAggrs.length; i++ )
		{
			if( cubeAggrs[i].getAggrName( ) != null )
				cubeAggrBindingList.add( cubeAggrs[i] );
		}
		

		if ( measureList == null )
			return new CalculatedMember[0];

		CalculatedMember[] calculatedMember = new CalculatedMember[measureList.size( )
				+ cubeAggrBindingList.size( )];
		int index = 0;
		
		List calculatedMemberList = new ArrayList();
		if ( !measureList.isEmpty( ) )
		{
			List levelList = populateMeasureAggrOns( queryDefn );

			Iterator measureIter = measureList.iterator( );
			while ( measureIter.hasNext( ) )
			{
				MeasureDefinition measureDefn = (MeasureDefinition) measureIter.next( );

				calculatedMember[index] = new CalculatedMember( measureDefn.getName( ),
						measureDefn.getName( ),
						levelList,
						measureDefn.getAggrFunction( ) == null
								? IBuildInAggregation.TOTAL_SUM_FUNC
								: measureDefn.getAggrFunction( ),
						0 );
				calculatedMemberList.add( calculatedMember[index] );
				index++;
			}
		}
		
		if ( !cubeAggrBindingList.isEmpty( ) )
		{
			int rsID = 1;
			for ( int i = 0; i < cubeAggrBindingList.size( ); i++ )
			{
				int id = getResultSetIndex( calculatedMemberList,
						( (ICubeAggrDefn) cubeAggrBindingList.get( i ) ).getAggrLevels( ) );
				if ( id == -1 )
				{
					calculatedMember[index] = new CalculatedMember( (ICubeAggrDefn) cubeAggrBindingList.get( i ),
							rsID );
					calculatedMemberList.add( calculatedMember[index] );
					rsID++;
				}
				else
				{
					calculatedMember[index] = new CalculatedMember( (ICubeAggrDefn) cubeAggrBindingList.get( i ),
							id );
				}

				if ( ( (ICubeAggrDefn) cubeAggrBindingList.get( i ) ).getFilter( ) != null )
				{
					IJSMeasureFilterEvalHelper filterEvalHelper = new JSMeasureFilterEvalHelper( scope,
							new FilterDefinition( ( (ICubeAggrDefn) cubeAggrBindingList.get( i ) ).getFilter( ) ) );
					calculatedMember[index].setFilterEvalHelper( filterEvalHelper );
				}
				index++;
			}
		}

		return calculatedMember;
	}

	/**
	 * Populate the list of measure aggregation ons.
	 * @param queryDefn
	 * @return
	 */
	public static List populateMeasureAggrOns( ICubeQueryDefinition queryDefn )
	{
		List levelList = new ArrayList( );
		ILevelDefinition[] rowLevels = getLevelsOnEdge( queryDefn.getEdge( ICubeQueryDefinition.ROW_EDGE ) );
		ILevelDefinition[] columnLevels = getLevelsOnEdge( queryDefn.getEdge( ICubeQueryDefinition.COLUMN_EDGE ) );

		for ( int i = 0; i < rowLevels.length; i++ )
		{
			levelList.add( new DimLevel( rowLevels[i] ) );
		}
		for ( int i = 0; i < columnLevels.length; i++ )
		{
			levelList.add( new DimLevel( columnLevels[i] ) );
		}
		return levelList;
	}
	
	/**
	 * 
	 * @param aggrList
	 * @param levelList
	 * @return
	 */
	private static int getResultSetIndex( List aggrList, List levelList )
	{
		for ( int i = 0; i < aggrList.size( ); i++ )
		{
			CalculatedMember member = (CalculatedMember) aggrList.get( i );
			if ( member.getAggrOnList( ).equals( levelList ) )
			{
				return member.getRsID( );
			}
		}		
		return -1;
	}
	
	/**
	 * get all ILevelDefinition from certain IEdgeDefinition
	 * 
	 * @param edgeDefn
	 * @return
	 */
	static ILevelDefinition[] getLevelsOnEdge( IEdgeDefinition edgeDefn )
	{
		if ( edgeDefn == null )
			return new ILevelDefinition[0];

		List levelList = new ArrayList( );
		Iterator dimIter = edgeDefn.getDimensions( ).iterator( );
		while ( dimIter.hasNext( ) )
		{
			IDimensionDefinition dimDefn = (IDimensionDefinition) dimIter.next( );
			Iterator hierarchyIter = dimDefn.getHierarchy( ).iterator( );
			while ( hierarchyIter.hasNext( ) )
			{
				IHierarchyDefinition hierarchyDefn = (IHierarchyDefinition) hierarchyIter.next( );
				levelList.addAll( hierarchyDefn.getLevels( ) );
			}
		}

		ILevelDefinition[] levelDefn = new LevelDefiniton[levelList.size( )];
		for ( int i = 0; i < levelList.size( ); i++ )
		{
			levelDefn[i] = (ILevelDefinition) levelList.get( i );
		}

		return levelDefn;
	}

	/**
	 * Get related level's info for all measure.
	 * 
	 * @param queryDefn
	 * @return
	 * @throws DataException 
	 */
	public static Map getRelationWithMeasure( ICubeQueryDefinition queryDefn ) throws DataException
	{
		Map measureRelationMap = new HashMap( );
		List rowLevelList = new ArrayList( );
		List columnLevelList = new ArrayList( );

		if ( queryDefn.getEdge( ICubeQueryDefinition.COLUMN_EDGE ) != null )
		{
			ILevelDefinition[] levels = getLevelsOnEdge( queryDefn.getEdge( ICubeQueryDefinition.COLUMN_EDGE ) );
			for ( int i = 0; i < levels.length; i++ )
			{
				columnLevelList.add( new DimLevel( levels[i] ) );
			}
		}

		if ( queryDefn.getEdge( ICubeQueryDefinition.ROW_EDGE ) != null )
		{
			ILevelDefinition[] levels = getLevelsOnEdge( queryDefn.getEdge( ICubeQueryDefinition.ROW_EDGE ) );
			for ( int i = 0; i < levels.length; i++ )
			{
				rowLevelList.add( new DimLevel( levels[i] ) );
			}
		}

		if ( queryDefn.getMeasures( ) != null
				&& !queryDefn.getMeasures( ).isEmpty( ) )
		{
			Iterator measureIter = queryDefn.getMeasures( ).iterator( );
			while ( measureIter.hasNext( ) )
			{
				IMeasureDefinition measure = (MeasureDefinition) measureIter.next( );
				measureRelationMap.put( measure.getName( ),
						new RelationShip( rowLevelList, columnLevelList ) );
			}
		}
		ICubeAggrDefn[] cubeAggrs = OlapExpressionUtil.getAggrDefns( queryDefn.getBindings( ) );
		
		 if ( cubeAggrs != null && cubeAggrs.length > 0 )
		{
			for ( int i = 0; i < cubeAggrs.length; i++ )
			{
				if ( cubeAggrs[i].getAggrName( ) == null )
					continue;
				List aggrOns = cubeAggrs[i].getAggrLevels( );
				List usedLevelOnRow = new ArrayList( );
				List usedLevelOnColumn = new ArrayList( );
				for ( int j = 0; j < aggrOns.size( ); j++ )
				{
					if ( rowLevelList.contains( aggrOns.get( j ) ) )
						usedLevelOnRow.add( aggrOns.get( j ) );
					else if ( columnLevelList.contains( aggrOns.get( j ) ) )
						usedLevelOnColumn.add( aggrOns.get( j ) );
				}

				measureRelationMap.put( cubeAggrs[i].getName( ),
						new RelationShip( usedLevelOnRow, usedLevelOnColumn ) );
			}
		}
		return measureRelationMap;
	}
}
