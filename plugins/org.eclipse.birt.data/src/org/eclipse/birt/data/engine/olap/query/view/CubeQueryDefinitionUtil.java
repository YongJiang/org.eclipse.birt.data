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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.birt.core.data.DataType;
import org.eclipse.birt.core.data.DataTypeUtil;
import org.eclipse.birt.core.data.ExpressionUtil;
import org.eclipse.birt.core.exception.BirtException;
import org.eclipse.birt.core.script.ScriptContext;
import org.eclipse.birt.core.script.ScriptExpression;
import org.eclipse.birt.data.engine.api.IBaseExpression;
import org.eclipse.birt.data.engine.api.IBinding;
import org.eclipse.birt.data.engine.api.IConditionalExpression;
import org.eclipse.birt.data.engine.api.IExpressionCollection;
import org.eclipse.birt.data.engine.api.IFilterDefinition;
import org.eclipse.birt.data.engine.api.IScriptExpression;
import org.eclipse.birt.data.engine.api.ISortDefinition;
import org.eclipse.birt.data.engine.api.aggregation.AggregationManager;
import org.eclipse.birt.data.engine.api.aggregation.IAggrFunction;
import org.eclipse.birt.data.engine.api.querydefn.Binding;
import org.eclipse.birt.data.engine.api.querydefn.FilterDefinition;
import org.eclipse.birt.data.engine.core.DataException;
import org.eclipse.birt.data.engine.olap.api.query.IComputedMeasureDefinition;
import org.eclipse.birt.data.engine.olap.api.query.ICubeFilterDefinition;
import org.eclipse.birt.data.engine.olap.api.query.ICubeOperation;
import org.eclipse.birt.data.engine.olap.api.query.ICubeQueryDefinition;
import org.eclipse.birt.data.engine.olap.api.query.IDimensionDefinition;
import org.eclipse.birt.data.engine.olap.api.query.IEdgeDefinition;
import org.eclipse.birt.data.engine.olap.api.query.IEdgeDrillFilter;
import org.eclipse.birt.data.engine.olap.api.query.IHierarchyDefinition;
import org.eclipse.birt.data.engine.olap.api.query.ILevelDefinition;
import org.eclipse.birt.data.engine.olap.api.query.IMeasureDefinition;
import org.eclipse.birt.data.engine.olap.api.query.LevelDefiniton;
import org.eclipse.birt.data.engine.olap.data.api.DimLevel;
import org.eclipse.birt.data.engine.olap.data.api.IDimensionSortDefn;
import org.eclipse.birt.data.engine.olap.data.api.ILevel;
import org.eclipse.birt.data.engine.olap.data.api.ISelection;
import org.eclipse.birt.data.engine.olap.data.api.cube.ICube;
import org.eclipse.birt.data.engine.olap.data.api.cube.IDimension;
import org.eclipse.birt.data.engine.olap.data.api.cube.IHierarchy;
import org.eclipse.birt.data.engine.olap.data.impl.AggregationDefinition;
import org.eclipse.birt.data.engine.olap.data.impl.AggregationFunctionDefinition;
import org.eclipse.birt.data.engine.olap.data.impl.SelectionFactory;
import org.eclipse.birt.data.engine.olap.data.impl.aggregation.filter.LevelFilter;
import org.eclipse.birt.data.engine.olap.impl.query.MeasureDefinition;
import org.eclipse.birt.data.engine.olap.util.CubeAggrDefn;
import org.eclipse.birt.data.engine.olap.util.CubeAggrDefnOnMeasure;
import org.eclipse.birt.data.engine.olap.util.CubeRunningNestAggrDefn;
import org.eclipse.birt.data.engine.olap.util.OlapExpressionCompiler;
import org.eclipse.birt.data.engine.olap.util.OlapExpressionUtil;
import org.eclipse.birt.data.engine.olap.util.filter.IJSFacttableFilterEvalHelper;
import org.eclipse.birt.data.engine.olap.util.filter.JSFacttableFilterEvalHelper;
import org.eclipse.birt.data.engine.script.ScriptConstants;
import org.eclipse.birt.data.engine.script.ScriptEvalUtil;
import org.mozilla.javascript.Scriptable;
/**
 * Utility class
 *
 */
public class CubeQueryDefinitionUtil
{

	/**
	 * Populate all aggregation member in CubeQueryDefinition. For initial
	 * implementation: we only consider IMeasureDefintion we will take into
	 * consider to handle the aggregation definition in binding expression;
	 * 
	 * @param queryDefn
	 * @param measureMapping 
	 * @return
	 * @throws DataException 
	 */
	static CalculatedMember[] getCalculatedMembers(
			ICubeQueryDefinition queryDefn, Scriptable scope, Map measureMapping, ScriptContext cx ) throws DataException
	{
		CubeAggrDefn[] cubeAggrs = OlapExpressionUtil.getAggrDefns( queryDefn.getBindings( ) );

		populateMeasureFromBinding( queryDefn, cx );
		populateMeasureFromFilter( queryDefn, cx );
		populateMeasureFromSort( queryDefn, cx );

		CalculatedMember[] calculatedMembers1 = extractMeasure( queryDefn,
				measureMapping );

		int startRsId = calculatedMembers1.length == 0 ? 0 : 1;

		CalculatedMember[] calculatedMembers2 = createCalculatedMembersByAggrOnList( startRsId,
				cubeAggrs,
				scope,
				cx );

		return uniteCalculatedMember( calculatedMembers1, calculatedMembers2 );
		
	}

	private static CalculatedMember[] extractMeasure(
			ICubeQueryDefinition queryDefn, Map measureMapping )
			throws DataException
	{
		List measureList = queryDefn.getMeasures( );

		if ( measureList == null )
			return new CalculatedMember[0];
		List measureAggrOns = populateMeasureAggrOns( queryDefn );

		List unreferencedMeasures = getUnreferencedMeasures( queryDefn,
				measureList,
				measureMapping,
				measureAggrOns );
		CalculatedMember[] calculatedMembers1 = new CalculatedMember[unreferencedMeasures.size( )];
		int index = 0;

		Iterator measureIter = unreferencedMeasures.iterator( );
		while ( measureIter.hasNext( ) )
		{
			MeasureDefinition measureDefn = (MeasureDefinition) measureIter.next( );
			String innerName = OlapExpressionUtil.createMeasureCalculateMemeberName( measureDefn.getName( ) );
			measureMapping.put( measureDefn.getName( ), innerName );
			// all the measures will consume one result set, and the default
			// rsID is 0. If no unreferenced measures are found, the
			// bindings' start index of rsID will be 0
			calculatedMembers1[index] = new CalculatedMember( new CubeAggrDefnOnMeasure( innerName,
					measureDefn.getName( ),
					measureAggrOns,
					adaptAggrFunction( measureDefn ),
					null,
					null ),
					0 );
			index++;
		}
		return calculatedMembers1;
	}
	
	public static CalculatedMember[] createCalculatedMembersByAggrOnList(
			int startRsId, CubeAggrDefn[] cubeAggrs, Scriptable scope,
			ScriptContext cx ) throws DataException
	{
		if ( cubeAggrs == null )
		{
			return new CalculatedMember[0];
		}

		assert startRsId >= 0;

		int preparedRsId = startRsId;
		CalculatedMember[] result = new CalculatedMember[cubeAggrs.length];
		List<CalculatedMember> withDistinctRsIds = new ArrayList<CalculatedMember>( );
		int index = 0;
		for ( CubeAggrDefn cubeAggrDefn : cubeAggrs )
		{
			int id = getResultSetIndex( withDistinctRsIds,
					cubeAggrDefn.getAggrLevelsInAggregationResult( ) );
			if ( id == -1 )
			{
				result[index] = new CalculatedMember( cubeAggrDefn,
						preparedRsId );
				withDistinctRsIds.add( result[index] );
				preparedRsId++;
			}
			else
			{
				result[index] = new CalculatedMember( cubeAggrDefn, id );
			}

			if ( cubeAggrDefn.getFilter( ) != null )
			{
				IJSFacttableFilterEvalHelper filterEvalHelper = new JSFacttableFilterEvalHelper( scope, cx,
						new FilterDefinition( cubeAggrDefn.getFilter( ) ) , null, null );
				result[index].setFilterEvalHelper( filterEvalHelper );
			}
			index++;
		}
		return result;
	}

	public static boolean isRunnnigAggr( CubeAggrDefn cubeAggr ) throws DataException
	{
		return isRunnnigAggr( cubeAggr.getAggrName( ) );

	}
	
	public static boolean isRunnnigAggr( String aggrFunc ) throws DataException
	{
		IAggrFunction af = AggregationManager.getInstance( ).getAggregation( aggrFunc );
		return af != null && af.getType( ) == IAggrFunction.RUNNING_AGGR;
	}
	

	public static ICubeQueryDefinition[] getCubeQueryFromDrills(
			ICubeQueryDefinition query, ICube cube, int edgeType ) throws DataException
	{
		List queryDefinition = new ArrayList( );
		IEdgeDefinition edge = query.getEdge( edgeType );

		if ( edge != null && edge.getDrillFilter( ).size( ) > 0 )
		{
			IEdgeDrillFilter rowDrill = null, columnDrill = null;
			for ( int i = 0; i < edge.getDrillFilter( ).size( ); i++ )
			{
				if ( edgeType == ICubeQueryDefinition.COLUMN_EDGE )
					columnDrill = (IEdgeDrillFilter) edge.getDrillFilter( )
							.get( i );
				else
					rowDrill = (IEdgeDrillFilter) edge.getDrillFilter( )
							.get( i );
				queryDefinition.add( cloneCubeQueryDefinition( query,
						columnDrill,
						rowDrill,
						cube ) );
			}
		}
		ICubeQueryDefinition[] drillQuery = new ICubeQueryDefinition[queryDefinition.size( )];
		for ( int i = 0; i < drillQuery.length; i++ )
		{
			drillQuery[i] = (ICubeQueryDefinition) queryDefinition.get( i );
		}
		return drillQuery;
	}
	
	public static ICubeQueryDefinition[] getCrossCubeQueryFromDrills(
			ICubeQueryDefinition query, ICube cube ) throws DataException
	{
		IEdgeDefinition columnEdge = query.getEdge( ICubeQueryDefinition.COLUMN_EDGE );
		IEdgeDefinition rowEdge = query.getEdge( ICubeQueryDefinition.ROW_EDGE );
		List<ICubeQueryDefinition> queryDefinition = new ArrayList<ICubeQueryDefinition>( );

		if ( columnEdge != null
				&& columnEdge.getDrillFilter( ).size( ) > 0 && rowEdge != null
				&& rowEdge.getDrillFilter( ).size( ) > 0 )
		{
			IEdgeDrillFilter rowDrill = null, columnDrill = null;
			for ( int i = 0; i < columnEdge.getDrillFilter( ).size( ); i++ )
			{
				columnDrill = (IEdgeDrillFilter) columnEdge.getDrillFilter( )
						.get( i );
				for ( int j = 0; j < rowEdge.getDrillFilter( ).size( ); j++ )
				{
					rowDrill = (IEdgeDrillFilter) rowEdge.getDrillFilter( )
							.get( j );

					queryDefinition.add( cloneCubeQueryDefinition( query,
							columnDrill,
							rowDrill,
							cube ) );
				}
			}
		}
		ICubeQueryDefinition[] drillQuery = new ICubeQueryDefinition[queryDefinition.size( )];
		for ( int i = 0; i < drillQuery.length; i++ )
		{
			drillQuery[i] = (ICubeQueryDefinition) queryDefinition.get( i );
		}
		return drillQuery;
	}

	private static ICubeQueryDefinition cloneCubeQueryDefinition(
			ICubeQueryDefinition query, IEdgeDrillFilter columnDrill,
			IEdgeDrillFilter rowDrill, ICube cube ) throws DataException
	{
		DrillCubeQueryDefinition cloneQuery = null;
		if( query!= null )
		{
			cloneQuery = new DrillCubeQueryDefinition( query.getName( ) );
			if ( query.getEdge( ICubeQueryDefinition.COLUMN_EDGE ) != null )
			{
				if ( columnDrill != null )
				{
					( (DrillCubeQueryDefinition) cloneQuery ).setTupleOnColumn( columnDrill.getTuple( ) );					
				}

				cloneEdgeDefinition( cloneQuery,
						query.getEdge( ICubeQueryDefinition.COLUMN_EDGE ),
						ICubeQueryDefinition.COLUMN_EDGE,
						columnDrill,
						cube );
			}
			if ( query.getEdge( ICubeQueryDefinition.ROW_EDGE ) != null )
			{
				if ( rowDrill != null )
				{
					( (DrillCubeQueryDefinition) cloneQuery ).setTupleOnRow( rowDrill.getTuple( ) );
				}

				cloneEdgeDefinition( cloneQuery,
						query.getEdge( ICubeQueryDefinition.ROW_EDGE ),
						ICubeQueryDefinition.ROW_EDGE,
						rowDrill,
						cube );
			}
			for ( int i = 0; i < query.getMeasures( ).size( ); i++ )
			{
				IMeasureDefinition measure = ( (IMeasureDefinition) query.getMeasures( )
						.get( i ) );
				IMeasureDefinition cloneMeasure = cloneQuery.createMeasure( measure.getName( ) );
				cloneMeasure.setAggrFunction( measure.getAggrFunction( ) );
			}
			for ( int i = 0; i < query.getComputedMeasures( ).size( ); i++ )
			{
				IComputedMeasureDefinition measure = ( (IComputedMeasureDefinition) query.getComputedMeasures( )
						.get( i ) );
				IMeasureDefinition cloneMeasure = cloneQuery.createComputedMeasure( measure.getName( ),
						measure.getType( ),
						measure.getExpression( ) );
				cloneMeasure.setAggrFunction( measure.getAggrFunction( ) );
			}

			List<String> levelDefnOnColumn = new ArrayList<String>();
			List<String> levelDefnOnRow = new ArrayList<String>();

			if ( columnDrill != null )
			{
				levelDefnOnColumn = findLevelExpressionFromQuery( query.getEdge( ICubeQueryDefinition.COLUMN_EDGE ),
						columnDrill, cube );
			}
			if ( rowDrill != null )
			{
				levelDefnOnRow = findLevelExpressionFromQuery( query.getEdge( ICubeQueryDefinition.ROW_EDGE ),
						rowDrill, cube );
			}

			for ( int i = 0; i < query.getBindings( ).size( ); i++ )
			{
				IBinding binding = (IBinding) query.getBindings( ).get( i );
				if ( binding.getAggrFunction( ) != null )
				{
					List dimLevelOnColumn = getReferenceDimLevelOnEdge( query, ICubeQueryDefinition.COLUMN_EDGE );
					List dimLevelOnRow = getReferenceDimLevelOnEdge( query, ICubeQueryDefinition.ROW_EDGE );
					
					IBinding newBinding = new Binding( binding.getBindingName( ),
							binding.getExpression( ) );
					newBinding.setDataType( binding.getDataType( ) );
					newBinding.setAggrFunction( binding.getAggrFunction( ) );
					newBinding.setDisplayName( binding.getDisplayName( ) );
					newBinding.setExportable( binding.exportable( ) );
					newBinding.setFilter( newBinding.getFilter( ) );
					for ( int k = 0; k < binding.getArguments( ).size( ); k++ )
					{
						newBinding.addArgument( (IBaseExpression) binding.getArguments( )
								.get( k ) );
					}

					List aggrOns = binding.getAggregatOns( );
					
					if ( aggrOns != null && !aggrOns.isEmpty( ) )
					{
						boolean columnExist = false, rowExist = false;
						
						boolean detailLevelOnColumn = isGrandTotalOnEdge( aggrOns,
								dimLevelOnColumn );
						boolean detailLevelOnRow = isGrandTotalOnEdge( aggrOns,
								dimLevelOnRow );
						if ( !detailLevelOnColumn && !detailLevelOnRow )
						{
							continue;
						}

						if ( ( detailLevelOnColumn && columnDrill != null )
								|| ( detailLevelOnRow && rowDrill != null ) )
						{
							for ( int k = 0; k < aggrOns.size( ); k++ )
							{
								String aggrExpr = aggrOns.get( k ).toString( );
								DimLevel target = OlapExpressionUtil.getTargetDimLevel( aggrExpr );

								String dimensionNameOnColumn = columnDrill != null
										? columnDrill.getTargetHierarchy( )
												.getDimension( )
												.getName( ) : null;
								String dimensionNameOnRow = rowDrill != null
										? rowDrill.getTargetHierarchy( )
												.getDimension( )
												.getName( ) : null;
								if ( !columnExist
										&& target.getDimensionName( )
												.equals( dimensionNameOnColumn ) )
								{
									if ( !detailLevelOnColumn )
									{
										newBinding.addAggregateOn( aggrExpr );
									}
									else
									{
										columnExist = true;
										for ( int t = 0; t < levelDefnOnColumn.size( ); t++ )
											newBinding.addAggregateOn( (String) levelDefnOnColumn.get( t ) );
									}
								}
								else if ( !rowExist
										&& target.getDimensionName( )
												.equals( dimensionNameOnRow ) )
								{
									if ( !detailLevelOnRow )
									{
										newBinding.addAggregateOn( aggrExpr );
									}
									else
									{
										rowExist = true;
										for ( int t = 0; t < levelDefnOnRow.size( ); t++ )
											newBinding.addAggregateOn( (String) levelDefnOnRow.get( t ) );
									}
								}
								else if ( !target.getDimensionName( )
										.equals( dimensionNameOnColumn )
										&& !target.getDimensionName( )
												.equals( dimensionNameOnRow ) )
									newBinding.addAggregateOn( aggrExpr );
							}
							cloneQuery.addBinding( newBinding );
						}
					}
				}
				else
					cloneQuery.addBinding( (IBinding) query.getBindings( )
							.get( i ) );
			}
			for ( int i = 0; i < query.getFilters( ).size( ); i++ )
			{
				if ( canAddFilterToDrill( query, rowDrill, columnDrill, i ) )
				{
					cloneQuery.addFilter( (IFilterDefinition) query.getFilters( )
							.get( i ) );
				}
			}
			for ( int i = 0; i < query.getSorts( ).size( ); i++ )
			{
				cloneQuery.addSort( (ISortDefinition) query.getSorts( ).get( i ) );
			}
		}
		return cloneQuery;
	}

	private static boolean canAddFilterToDrill( ICubeQueryDefinition query,
			IEdgeDrillFilter rowDrill, IEdgeDrillFilter columnDrill, int i )
	{
		List<IDimensionDefinition> columnEdgeDimensions = null;

		if ( query.getEdge( ICubeQueryDefinition.COLUMN_EDGE ) != null )
			columnEdgeDimensions = query.getEdge( ICubeQueryDefinition.COLUMN_EDGE )
					.getDimensions( );

		List<IDimensionDefinition> rowEdgeDimensions = null;

		if ( query.getEdge( ICubeQueryDefinition.ROW_EDGE ) != null )
			rowEdgeDimensions = query.getEdge( ICubeQueryDefinition.ROW_EDGE )
					.getDimensions( );
		if( ! ( query.getFilters( ).get( i ) instanceof ICubeFilterDefinition ) )
		{
			return true;
		}
			
		ICubeFilterDefinition filter = (ICubeFilterDefinition) query.getFilters( )
				.get( i );

		if ( filter.getTargetLevel( ) == null )
			return false;

		int filterIndex = -1;
		int rowDrillIndex = -1;
		int columnDrillIndex = -1;
		int edgeTag = -1;
		boolean canAddFilter = false;

		if ( rowEdgeDimensions != null && rowDrill != null )
		{
			for ( int p = 0; p < rowEdgeDimensions.size( ); p++ )
			{
				IDimensionDefinition rowEdgeDimension = rowEdgeDimensions.get( p );
				String hierarchyName = rowEdgeDimension.getHierarchy( )
						.get( 0 )
						.getName( );

				if ( hierarchyName.equals( filter.getTargetLevel( )
						.getHierarchy( )
						.getName( ) ) )
				{
					filterIndex = p;
					edgeTag = ICubeQueryDefinition.ROW_EDGE;
				}
				if ( rowDrill.getTargetHierarchy( )
						.getName( )
						.equals( hierarchyName ) )
					rowDrillIndex = p;
			}
		}

		if ( columnEdgeDimensions != null && columnDrill != null )
		{
			for ( int p = 0; p < columnEdgeDimensions.size( ); p++ )
			{
				IDimensionDefinition columnEdgeDimension = columnEdgeDimensions.get( p );
				String hierarchyName = columnEdgeDimension.getHierarchy( )
						.get( 0 )
						.getName( );

				if ( hierarchyName.equals( filter.getTargetLevel( )
						.getHierarchy( )
						.getName( ) ) )
				{
					filterIndex = p;
					edgeTag = ICubeQueryDefinition.COLUMN_EDGE;
				}
				if ( columnDrill.getTargetHierarchy( )
						.getName( )
						.equals( hierarchyName ) )
					columnDrillIndex = p;
			}
		}

		if ( edgeTag == ICubeQueryDefinition.ROW_EDGE )
		{
			if ( rowDrillIndex < filterIndex )
			{
				canAddFilter = false;
			}
			else if ( rowDrillIndex > filterIndex )
			{
				canAddFilter = true;
			}
			else
			{
				String filterTargetLevel = filter.getTargetLevel( ).getName( );
				String drillTargetLevel = rowDrill.getTargetLevelName( );

				List<ILevelDefinition> levelDefinition = filter.getTargetLevel( )
						.getHierarchy( )
						.getLevels( );
				int drillLevelIndex = 0;
				int filterLevelIndex = 0;
				for ( int j = 0; j < levelDefinition.size( ); j++ )
				{
					ILevelDefinition level = levelDefinition.get( j );
					if ( level.getName( ).equals( drillTargetLevel ) )
					{
						drillLevelIndex = j;
					}
					if ( level.getName( ).equals( filterTargetLevel ) )
					{
						filterLevelIndex = j;
					}
				}

				if ( drillLevelIndex >= filterLevelIndex )
					canAddFilter = true;
			}
		}
		else if ( edgeTag == ICubeQueryDefinition.COLUMN_EDGE )
		{
			if ( columnDrillIndex < filterIndex )
			{
				canAddFilter = false;
			}
			else if ( columnDrillIndex > filterIndex )
			{
				canAddFilter = true;
			}
			else
			{
				String filterTargetLevel = filter.getTargetLevel( ).getName( );
				String drillTargetLevel = columnDrill.getTargetLevelName( );

				List<ILevelDefinition> levelDefinition = filter.getTargetLevel( )
						.getHierarchy( )
						.getLevels( );
				int drillLevelIndex = 0;
				int filterLevelIndex = 0;
				for ( int j = 0; j < levelDefinition.size( ); j++ )
				{
					ILevelDefinition level = levelDefinition.get( j );
					if ( level.getName( ).equals( drillTargetLevel ) )
					{
						drillLevelIndex = j;
					}
					if ( level.getName( ).equals( filterTargetLevel ) )
					{
						filterLevelIndex = j;
					}
				}

				if ( drillLevelIndex >= filterLevelIndex )
					canAddFilter = true;
			}
		}

		return canAddFilter;
	}
	
	private static boolean isGrandTotalOnEdge( List aggrOns, List dimLevels )
			throws DataException
	{
		boolean flag = false;
		for ( int i = 0; i < aggrOns.size( ); i++ )
		{
			String aggrExpr = aggrOns.get( i ).toString( );
			DimLevel target = OlapExpressionUtil.getTargetDimLevel( aggrExpr );
			if ( !dimLevels.isEmpty( )
					&& dimLevels.get( dimLevels.size( ) - 1 )
							.equals( target ) )
			{
				flag = true;
				break;
			}
		}
		return flag;
	}


	private static void cloneEdgeDefinition( DrillCubeQueryDefinition cloneQuery,
			IEdgeDefinition edge, int type, IEdgeDrillFilter drill, ICube cube ) throws DataException
	{
		IEdgeDefinition cloneEdge = cloneQuery.createEdge( type );
		Iterator<IDimensionDefinition> dimension = edge.getDimensions( )
				.iterator( );
		List<ILevelDefinition> levelDefnList = new ArrayList<ILevelDefinition>( );

		while ( dimension.hasNext( ) )
		{
			IDimensionDefinition dim = (IDimensionDefinition) dimension.next( );
			IDimensionDefinition cloneDim = cloneEdge.createDimension( dim.getName( ) );
			Iterator<IHierarchyDefinition> hierarchyIter = dim.getHierarchy( ).iterator( );
			while ( hierarchyIter.hasNext( ) )
			{
				IHierarchyDefinition hierarchy = (IHierarchyDefinition) hierarchyIter.next( );
				IHierarchyDefinition cloneHier = cloneDim.createHierarchy( hierarchy.getName( ) );
				if ( drill!= null && hierarchy.getName( ).equals( drill.getTargetHierarchy( )
						.getName( ) ) )
				{
					{
						IHierarchy cubeHierarchy = findHierarchyFromCube( cube,
								hierarchy );

						for ( ILevel cubeLevel : cubeHierarchy.getLevels( ) )
						{
							ILevelDefinition cloneLevel;
							if ( cubeLevel.getName( )
									.equals( drill.getTargetLevelName( ) ) )
							{
								cloneLevel = cloneHier.createLevel( cubeLevel.getName( ) );
								levelDefnList.add( cloneLevel );
								break;
							}
							cloneLevel = cloneHier.createLevel( cubeLevel.getName( ) );
							levelDefnList.add( cloneLevel );
						}
					}
				}
				else
				{
					Iterator<ILevelDefinition> levels = hierarchy.getLevels( )
							.iterator( );
					while ( levels.hasNext( ) )
					{
						ILevelDefinition level = (ILevelDefinition) levels.next( );
						ILevelDefinition cloneLevel = cloneHier.createLevel( level.getName( ) );
						levelDefnList.add( cloneLevel );
					}
				}
			}
		}
 		if ( drill != null )
		{
			Iterator members = drill.getTuple( ).iterator( );
			Iterator levels = levelDefnList.iterator( );
			
			Collection<Object[]> qulifiedTuple = new ArrayList<Object[]>( );
		
			while ( members.hasNext( ) && levels.hasNext( ) )
			{
				Object[] key = (Object[]) members.next( );
				if ( key != null && key.length > 0 && key[0] != null )
				{
					ILevelDefinition level = (ILevelDefinition) levels.next( );
					int dataType = getLevelDataType( level, cube );

					Object[] qualifiedKey = new Object[key.length];
					Object[][] multipleKey = new Object[key.length][];

					for ( int i = 0; i < key.length; i++ )
					{
						try
						{
							Object value = DataTypeUtil.convert( key[i],
									dataType );
							qualifiedKey[i] = value;
							multipleKey[i] = new Object[]{
								value
							};
						}
						catch ( BirtException e )
						{
						}
					}
					
					qulifiedTuple.add( qualifiedKey );
					ISelection selection = SelectionFactory.createMutiKeySelection( multipleKey );
					( (DrillCubeQueryDefinition) cloneQuery ).addLevelFilter( new LevelFilter( new DimLevel( (ILevelDefinition) level ),
							new ISelection[]{
								selection
							} ) );
				}
			}
			
			if ( type == ICubeQueryDefinition.COLUMN_EDGE )
				cloneQuery.setTupleOnColumn( qulifiedTuple );
			else
				cloneQuery.setTupleOnRow( qulifiedTuple );
			drill.setTuple( qulifiedTuple );
		}
	}
	
	private static int getLevelDataType( ILevelDefinition level, ICube cube )
	{
		IHierarchy hierarchy = findHierarchyFromCube( cube,
				level.getHierarchy( ) );
		int dataType = DataType.ANY_TYPE;
		if ( hierarchy != null )
		{
			ILevel[] levelArray = hierarchy.getLevels( );
			for ( int i = 0; i < levelArray.length; i++ )
			{
				if ( levelArray[i].getName( ).equals( level.getName( ) ) )
				{
					dataType = levelArray[i].getKeyDataType( level.getName( ) );
					break;
				}
			}
		}
		return dataType;
	}

	private static List findLevelExpressionFromQuery( IEdgeDefinition edge,
			IEdgeDrillFilter drill, ICube cube ) throws DataException
	{
		List<String> levelList = new ArrayList<String>( );
		List<IDimensionDefinition> dimensionList = edge.getDimensions( );

		for ( int i = 0; i < dimensionList.size( ); i++ )
		{
			IDimensionDefinition dim = (IDimensionDefinition) dimensionList.get( i );
			List<IHierarchyDefinition> hierarchyList = dim.getHierarchy( );
			IHierarchyDefinition hier = (IHierarchyDefinition) hierarchyList.get( 0 );
			if ( hier.getName( )
					.equals( drill.getTargetHierarchy( ).getName( ) ) )
			{
//				if ( DrillType.DRILL_TO_ROOTS.equals( drill.getDrillOperation( ) )
//						|| DrillType.DRILL_TO_ANCESTORS.equals( drill.getDrillOperation( ) )
//						|| DrillType.DRILL_TO_PARENT.equals( drill.getDrillOperation( ) ) )
//				{
//					for ( int k = 0; k < hier.getLevels( ).size( ); k++ )
//					{
//						ILevelDefinition level = (ILevelDefinition) hier.getLevels( )
//								.get( k );
//						levelList.add( ExpressionUtil.createJSDimensionExpression( dim.getName( ),
//								level.getName( ) ) );
//
//						if ( level.getName( )
//								.equals( drill.getTargetLevel( ) ) )
//						{
//							break;
//						}
//					}
//				}
//				else
				{
					IHierarchy cubeHierarchy = findHierarchyFromCube( cube,
							drill.getTargetHierarchy( ) );
					for ( ILevel cubeLevel : cubeHierarchy.getLevels( ) )
					{
						if ( cubeLevel.getName( )
								.equals( drill.getTargetLevelName( ) ) )
						{
							levelList.add( ExpressionUtil.createJSDimensionExpression( dim.getName( ),
									cubeLevel.getName( ) ) );
							break;
						}
						levelList.add( ExpressionUtil.createJSDimensionExpression( dim.getName( ),
								cubeLevel.getName( ) ) );
					}
				}
			}
			else
			{
				for ( int k = 0; k < hier.getLevels( ).size( ); k++ )
				{
					ILevelDefinition level = (ILevelDefinition) hier.getLevels( )
							.get( k );
					levelList.add( ExpressionUtil.createJSDimensionExpression( dim.getName( ),
							level.getName( ) ) );
				}
			}
		}
		return levelList;
	}
	
	private static IHierarchy findHierarchyFromCube( ICube cube,
			IHierarchyDefinition hierarchy )
	{
		for ( IDimension dim : cube.getDimesions( ) )
		{
			if ( dim.getName( ).equals( hierarchy.getDimension( ).getName( ) )
					&& dim.getHierarchy( )
							.getName( )
							.equals( hierarchy.getName( ) ) )
			{
				return dim.getHierarchy( );
			}
		}
		return null;
	}
	
	public static CalculatedMember[] addCalculatedMembers (CubeAggrDefn[] cubeAggrs, AggregationRegisterTable manager,
			Scriptable scope, ScriptContext cx ) throws DataException
	{
		if (cubeAggrs == null)
		{
			return new CalculatedMember[0];
		}
		CalculatedMember[] result = new CalculatedMember[cubeAggrs.length];
		int newRsId = manager.getBasedRsIndex( ) + 1;
		CalculatedMember newCm = null;
		int index = 0;
		for (CubeAggrDefn cubeAggrDefn : cubeAggrs)
		{
			int id = -1;
			if ( !isRunnnigAggr( cubeAggrDefn ) ) //Currently, no aggregation compose step for running type aggregation
			{
				for ( CalculatedMember cm : manager.getCalculatedMembers( ) )
				{
					if ( cm.getCubeAggrDefn( ).getAggrLevelsInAggregationResult( ).equals( cubeAggrDefn.getAggrLevelsInAggregationResult( ) ))
					{
						id = cm.getRsID( );
						break;
					}
				}
			}
			if ( id == -1 )
			{
				newCm = new CalculatedMember( cubeAggrDefn,
						newRsId );
				newRsId++;
			}
			else
			{
				newCm = new CalculatedMember( cubeAggrDefn,
						id );
			}

			if ( cubeAggrDefn.getFilter( ) != null )
			{
				IJSFacttableFilterEvalHelper filterEvalHelper = new JSFacttableFilterEvalHelper( scope, cx,
						new FilterDefinition( cubeAggrDefn.getFilter( ) ), null, null );
				newCm.setFilterEvalHelper( filterEvalHelper );
			}
			manager.addCalculatedMembersFromCubeOperation( new CalculatedMember[]{newCm} );
			result[index++] = newCm;
		}
		return result;
	}
	
	public static CalculatedMember[] uniteCalculatedMember(CalculatedMember[] array1, CalculatedMember[] array2)
	{
		assert array1 != null && array2 != null;
		int size = array1.length + array2.length;
		CalculatedMember[] result = new CalculatedMember[size];
		System.arraycopy( array1, 0, result, 0, array1.length );
		System.arraycopy( array2, 0, result, array1.length, array2.length );
		return result;
	}
	
	public static AggregationDefinition[] createAggregationDefinitons(CalculatedMember[] calculatedMembers,
			ICubeQueryDefinition query, Scriptable scope, ScriptContext cx) throws DataException
	{
		if (calculatedMembers == null)
		{
			return new AggregationDefinition[0];
		}
		List<AggregationDefinition> result = new ArrayList<AggregationDefinition>();
		Set<Integer> rsIDSet = new HashSet<Integer>( );
		for ( int i = 0; i < calculatedMembers.length; i++ )
		{
			if ( rsIDSet.contains( Integer.valueOf( calculatedMembers[i].getRsID( ) )))
			{
				continue;
			}
			List<CalculatedMember> list = getCalculatedMemberWithSameRSId( calculatedMembers, i );
			AggregationFunctionDefinition[] funcitons = new AggregationFunctionDefinition[list.size( )];
			for ( int index = 0; index < list.size( ); index++ )
			{
				String[] dimInfo = ( (CalculatedMember) list.get( index ) ).getCubeAggrDefn( ).getFirstArgumentInfo( );
				String dimName = null;
				String levelName = null;
				String attributeName = null;
				DimLevel dimLevel = null;
				if ( dimInfo != null && dimInfo.length == 3 )
				{
					dimName = dimInfo[0];
					levelName = dimInfo[1];
					attributeName = dimInfo[2];
					dimLevel = new DimLevel( dimName, levelName );
				}
				funcitons[index] = new AggregationFunctionDefinition( list.get( index ).getCubeAggrDefn( ).getName( ),
						list.get( index ).getCubeAggrDefn( ).getMeasure( ),
						dimLevel,
						attributeName,
						list.get( index ).getCubeAggrDefn( ).getAggrName( ),
						list.get( index ).getFilterEvalHelper( ) );
				
				CubeAggrDefn cad = ( (CalculatedMember) list.get( index ) ).getCubeAggrDefn( );
				if ( cad instanceof CubeRunningNestAggrDefn )
				{
					//Special process to pass aggregation arguments for running type aggregation
					CubeRunningNestAggrDefn crnad = (CubeRunningNestAggrDefn) cad;
					if ( crnad.getNotLevelArguments( ) != null && !crnad.getNotLevelArguments( ).isEmpty( ))
					{
						IScriptExpression se = crnad.getNotLevelArguments( ).get( 0 );
						Object argumentValue = ScriptEvalUtil.evalExpr( se,
								cx.newContext( scope ),
								ScriptExpression.defaultID,
								0 );
						funcitons[index].setParaValue( argumentValue );
					}
				}
			}

			DimLevel[] levels = new DimLevel[calculatedMembers[i].getCubeAggrDefn( ).getAggrLevelsInDefinition( )
					.size( )];
			int[] sortType = new int[levels.length];
			for ( int index = 0; index < levels.length; index++ )
			{
				Object obj = calculatedMembers[i].getCubeAggrDefn( ).getAggrLevelsInDefinition( )
						.get( index );
				levels[index] = (DimLevel) obj;
				sortType[index] = getSortDirection( levels[index], query );
			}

			rsIDSet.add( Integer.valueOf(calculatedMembers[i].getRsID( ) ) );
			result.add(new AggregationDefinition( levels,
						sortType,
						funcitons ));
		}
		return result.toArray( new AggregationDefinition[0] );
	}

	/**
	 * 
	 * @param levelDefn
	 * @param query
	 * @return
	 * @throws DataException 
	 */
	public static int getSortDirection( DimLevel level, ICubeQueryDefinition query ) throws DataException
	{
		if ( query.getSorts( ) != null && !query.getSorts( ).isEmpty( ) )
		{
			for ( int i = 0; i < query.getSorts( ).size( ); i++ )
			{
				ISortDefinition sortDfn = ( (ISortDefinition) query.getSorts( )
						.get( i ) );
				DimLevel info = null;
				if( sortDfn.getExpression( ) == null )
				{
					String colName = sortDfn.getColumn( );
					info = getDimLevelByBindingName( colName, query.getBindings( ) );
					if ( level.equals( info ) )
					{
						return sortDfn.getSortDirection( );
					}
				}
				else
				{
					String expr = null;
					expr = sortDfn.getExpression( ).getText( );
					info = getDimLevel( expr, query.getBindings( ) );

					if ( level.equals( info ) )
					{
						return sortDfn.getSortDirection( );
					}
				}	
			}
		}
		return IDimensionSortDefn.SORT_UNDEFINED;
	}
	
	/**
	 * Get dim level from an expression.
	 * @param expr
	 * @param bindings
	 * @return
	 * @throws DataException
	 */
	private static DimLevel getDimLevel( String expr, List bindings ) throws DataException
	{
		String bindingName = OlapExpressionUtil.getBindingName( expr );
		DimLevel dimLevel = getDimLevelByBindingName( bindingName, bindings );
		if( dimLevel != null )
			return null;
		if ( OlapExpressionUtil.isReferenceToDimLevel( expr ) == false )
			return null;
		else 
			return OlapExpressionUtil.getTargetDimLevel( expr );
	}

	private static DimLevel getDimLevelByBindingName( String bindingName, List bindings ) throws DataException
	{
		if( bindingName != null )
		{
			for( int j = 0; j < bindings.size( ); j++ )
			{
				IBinding binding = (IBinding)bindings.get( j );
				if( binding.getBindingName( ).equals( bindingName ))
				{
					if (! (binding.getExpression( ) instanceof IScriptExpression))
						return null;
					return getDimLevel( ((IScriptExpression)binding.getExpression( )).getText( ), bindings );
				}
			}
		}
		return null;
	}
	
	/**
	 * 
	 * @param calMember
	 * @param index
	 * @return
	 */
	private static List<CalculatedMember> getCalculatedMemberWithSameRSId( CalculatedMember[] calMember,
			int index )
	{
		CalculatedMember member = calMember[index];
		List<CalculatedMember> list = new ArrayList<CalculatedMember>( );
		list.add( member );

		for ( int i = index + 1; i < calMember.length; i++ )
		{
			if ( calMember[i].getRsID( ) == member.getRsID( ) )
				list.add( calMember[i] );
		}
		return list;
	}
	
	/**
	 * 
	 * @param queryDefn
	 * @param levelExpr
	 * @param type
	 * @return
	 * @throws DataException
	 */
	static int getLevelIndex( ICubeQueryDefinition queryDefn, String levelExpr,
			int type ) throws DataException
	{
		int index = -1;
		DimLevel dimLevel = OlapExpressionUtil.getTargetDimLevel( levelExpr );
		IEdgeDefinition edgeDefn = queryDefn.getEdge( type );
		Iterator dimIter = edgeDefn.getDimensions( ).iterator( );
		while ( dimIter.hasNext( ) )
		{
			IDimensionDefinition dimDefn = (IDimensionDefinition) dimIter.next( );
			Iterator hierarchyIter = dimDefn.getHierarchy( ).iterator( );
			while ( hierarchyIter.hasNext( ) )
			{
				IHierarchyDefinition hierarchyDefn = (IHierarchyDefinition) hierarchyIter.next( );
				for ( int i = 0; i < hierarchyDefn.getLevels( ).size( ); i++ )
				{
					index++;
					ILevelDefinition levelDefn = (ILevelDefinition) hierarchyDefn.getLevels( )
							.get( i );
					if ( dimDefn.getName( )
							.equals( dimLevel.getDimensionName( ) ) &&
							levelDefn.getName( )
									.equals( dimLevel.getLevelName( ) ) )
					{
						return index;
					}
				}
			}
		}
		return -1;
	}
	
	/**
	 * used for backward capability.
	 * 
	 * @param measureDefn
	 * @return
	 */
	private static String adaptAggrFunction( MeasureDefinition measureDefn )
	{
		return measureDefn.getAggrFunction( ) == null
				? "SUM"
				: measureDefn.getAggrFunction( );
	}

	/**
	 * 
	 * @param queryDefn
	 * @param measureList
	 * @param measureMapping 
	 * @param measureAggrOns 
	 * @return
	 * @throws DataException 
	 */
	private static List getUnreferencedMeasures(
			ICubeQueryDefinition queryDefn, List measureList,
			Map measureMapping, List measureAggrOns ) throws DataException
	{
		List result = new ArrayList( );
		List bindings = queryDefn.getBindings( );
		for ( Iterator i = measureList.iterator( ); i.hasNext( ); )
		{
			MeasureDefinition measure = (MeasureDefinition) i.next( );
			IBinding referenceBinding = getMeasureDirectReferenceBinding( measure,
					bindings,
					measureAggrOns );
			if ( referenceBinding != null )
			{
				measureMapping.put( measure.getName( ),
						referenceBinding.getBindingName( ) );
			}
			else
			{
				result.add( measure );
			}
		}
		return result;
	}

	/**
	 * get the binding that directly reference to the specified measure.
	 * 
	 * @param measure
	 * @param bindings
	 * @param measureAggrOns
	 * @return
	 * @throws DataException
	 */
	private static IBinding getMeasureDirectReferenceBinding( MeasureDefinition measure,
			List bindings, List measureAggrOns ) throws DataException
	{
		
		for ( Iterator i = bindings.iterator( ); i.hasNext( ); )
		{
			IBinding binding = (IBinding) i.next( );
			if ( binding.getAggregatOns( ).size( ) == measureAggrOns.size( ) )
			{
				String aggrFunction = adaptAggrFunction( measure );
				String funcName = binding.getAggrFunction( );
				if ( aggrFunction.equals( funcName ) )
				{
					IBaseExpression expression = binding.getExpression( );
					if ( expression instanceof IScriptExpression )
					{
						IScriptExpression expr = (IScriptExpression) expression;
						String measureName = OlapExpressionUtil.getMeasure( expr.getText( ) );
						if ( measure.getName( ).equals( measureName ) )
						{
							int t = 0;
							for ( ; t < measureAggrOns.size( ); t++ )
							{
								DimLevel dimLevel = OlapExpressionUtil.getTargetDimLevel( binding.getAggregatOns( )
										.get( t )
										.toString( ) );

								if ( !dimLevel.equals( measureAggrOns.get( t ) ) )
								{
									break;
								}
							}
							if ( t == measureAggrOns.size( ) )
								return binding;
						}
					}
				}
			}
		}
		return null;
	}

	/**
	 * To populate the relational measures from Sort list of queryDefn
	 * 
	 * @param queryDefn
	 */
	private static void populateMeasureFromSort( ICubeQueryDefinition queryDefn, ScriptContext cx )
			throws DataException
	{
		for ( int i = 0; i < queryDefn.getSorts( ).size( ); i++ )
		{
			createRelationalMeasures( queryDefn,
					(IBaseExpression) ( (ISortDefinition) queryDefn.getSorts( )
							.get( i ) ).getExpression( ), cx );
		}
	}

	/**
	 * To populate the relational measures from filter list of queryDefn
	 * 
	 * @param queryDefn
	 */
	private static void populateMeasureFromFilter(
			ICubeQueryDefinition queryDefn, ScriptContext cx ) throws DataException
	{
		for ( int i = 0; i < queryDefn.getFilters( ).size( ); i++ )
		{
			createRelationalMeasures( queryDefn,
					(IBaseExpression) ( (IFilterDefinition) queryDefn.getFilters( )
							.get( i ) ).getExpression( ), cx );
		}
	}

	/**
	 * To populate the relational measures from binding list of queryDefn
	 * 
	 * @param queryDefn
	 */
	private static void populateMeasureFromBinding(
			ICubeQueryDefinition queryDefn, ScriptContext cx ) throws DataException
	{
		for ( int i = 0; i < queryDefn.getBindings( ).size( ); i++ )
		{
			createRelationalMeasures( queryDefn,
					(IBaseExpression) ( (IBinding) queryDefn.getBindings( )
							.get( i ) ).getExpression( ), cx );
		}
	}

	/**
	 * To create all the rational measures for CubeQueryDefinition according to the expression
	 * 
	 * @param queryDefn, expression
	 * @return List
	 * @throws DataException 
	 */
	private static List createRelationalMeasures(
			ICubeQueryDefinition queryDefn, IBaseExpression expression, ScriptContext cx )
			throws DataException
	{
		List measures = new ArrayList( );
		List exprTextList = getExprTextList( expression );
		for ( int i = 0; i < exprTextList.size( ); i++ )
		{
			String exprText = (String) exprTextList.get( i );
			String measureName = OlapExpressionCompiler.getReferencedScriptObject( exprText,
					ScriptConstants.MEASURE_SCRIPTABLE  );
			if ( measureName != null && measureName.trim( ).length( ) > 0 )
			{
				//if current measure list doesn't contain this measure, then add it to the list
				List existMeasures = queryDefn.getMeasures( );
				boolean exist = false;
				for ( int j = 0; j < existMeasures.size( ); j++ )
				{
					if ( ( (IMeasureDefinition) existMeasures.get( j ) ).getName( )
							.equals( measureName ) )
					{
						exist = true;
						break;
					}
				}				
				if ( !exist )
				{
					measures.add( queryDefn.createMeasure( measureName ) );
				}
			}
		}
		return measures;
	}
	
	/**
	 * To get all the sub expressions' text list of the given expression
	 * 
	 * @param queryDefn, expression
	 * @return List
	 */
	private static List getExprTextList( IBaseExpression expression )
	{
		List textList = new ArrayList( );
		if ( expression instanceof IScriptExpression )
		{
			textList.add( ( (IScriptExpression) expression ).getText( ) );
		}
		else if ( expression instanceof IExpressionCollection )
		{
			List exprList = (List) ( (IExpressionCollection) expression ).getExpressions( );
			for ( int i = 0; i < exprList.size( ); i++ )
			{
				IBaseExpression baseExpr = (IBaseExpression) exprList.get( i );
				textList.addAll( getExprTextList( baseExpr ) );
			}
		}
		else if ( expression instanceof IConditionalExpression )
		{
			textList.add( ( (IScriptExpression) ( (IConditionalExpression) expression ).getExpression( ) ).getText( ) );
			textList.addAll( getExprTextList( ( (IConditionalExpression) expression ).getOperand1( ) ) );
			textList.addAll( getExprTextList( ( (IConditionalExpression) expression ).getOperand2( ) ) );
		}
		return textList;
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
		ILevelDefinition[] pageLevels = getLevelsOnEdge( queryDefn.getEdge( ICubeQueryDefinition.PAGE_EDGE ) );

		for ( int i = 0; i < rowLevels.length; i++ )
		{
			levelList.add( new DimLevel( rowLevels[i] ) );
		}
		for ( int i = 0; i < columnLevels.length; i++ )
		{
			levelList.add( new DimLevel( columnLevels[i] ) );
		}
		for ( int i = 0; i < pageLevels.length; i++ )
		{
			levelList.add( new DimLevel( pageLevels[i] ) );
		}
		return levelList;
	}
	
	private static List getReferenceDimLevelOnEdge(
			ICubeQueryDefinition queryDefn, int type )
	{
		List levelList = new ArrayList( );
		ILevelDefinition[] rowLevels = getLevelsOnEdge( queryDefn.getEdge( type ) );

		for ( int i = 0; i < rowLevels.length; i++ )
		{
			levelList.add( new DimLevel( rowLevels[i] ) );
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
			if ( member.getCubeAggrDefn( ).getAggrLevelsInAggregationResult( ).equals( levelList ) )
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
	public static ILevelDefinition[] getLevelsOnEdge( IEdgeDefinition edgeDefn )
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
	 * @param measureMapping 
	 * @return
	 * @throws DataException 
	 */
	public static Map getRelationWithMeasure( ICubeQueryDefinition queryDefn, Map measureMapping,
			CubeAggrDefn[] aggrsFromCubeOperations ) throws DataException
	{
		Map measureRelationMap = new HashMap( );
		List pageLevelList = new ArrayList( );
		List rowLevelList = new ArrayList( );
		List columnLevelList = new ArrayList( );

		if( queryDefn.getEdge( ICubeQueryDefinition.PAGE_EDGE )!= null )
		{
			ILevelDefinition[] levels = getLevelsOnEdge( queryDefn.getEdge( ICubeQueryDefinition.PAGE_EDGE ) );
			for ( int i = 0; i < levels.length; i++ )
			{
				pageLevelList.add( new DimLevel( levels[i] ) );
			}
		}
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
				measureRelationMap.put( measureMapping.get( measure.getName( ) ),
						new Relationship( rowLevelList, columnLevelList, pageLevelList ) );
			}
		}
		List orignalBindings = queryDefn.getBindings( );
		List newBindings =  getNewBindingsFromCubeOperations(queryDefn);
		CubeAggrDefn[] cubeAggrs1 = OlapExpressionUtil.getAggrDefns( orignalBindings );
		CubeAggrDefn[] cubeAggrs = new CubeAggrDefn[cubeAggrs1.length + aggrsFromCubeOperations.length];
		System.arraycopy( cubeAggrs1, 0, cubeAggrs, 0, cubeAggrs1.length );
		System.arraycopy( aggrsFromCubeOperations, 0, cubeAggrs, cubeAggrs1.length, aggrsFromCubeOperations.length );
		 if ( cubeAggrs != null && cubeAggrs.length > 0 )
		{
			for ( int i = 0; i < cubeAggrs.length; i++ )
			{
				if ( cubeAggrs[i].getAggrName( ) == null )
					continue;
				List aggrOns = cubeAggrs[i].getAggrLevelsInAggregationResult( );
				List usedLevelOnRow = new ArrayList( );
				List usedLevelOnColumn = new ArrayList( );
				List usedLevelOnPage = new ArrayList( );
				for ( int j = 0; j < aggrOns.size( ); j++ )
				{
					if ( pageLevelList.contains( aggrOns.get( j ) ) )
					{
						usedLevelOnPage.add( aggrOns.get( j ) );
					}
					if ( rowLevelList.contains( aggrOns.get( j ) ) )
					{
						usedLevelOnRow.add( aggrOns.get( j ) );
					}
					else if ( columnLevelList.contains( aggrOns.get( j ) ) )
					{
						usedLevelOnColumn.add( aggrOns.get( j ) );
					}
				}
				measureRelationMap.put( cubeAggrs[i].getName( ),
						new Relationship( usedLevelOnRow, usedLevelOnColumn, usedLevelOnPage ) );
			}
		}
		return measureRelationMap;
	}
	
	public static List<IBinding> getNewBindingsFromCubeOperations(ICubeQueryDefinition cubeQueryDefn)
	{
		List<IBinding> list = new ArrayList<IBinding>();
		for (ICubeOperation co : cubeQueryDefn.getCubeOperations( ))
		{
			IBinding[] newBindings = co.getNewBindings( );
			list.addAll( Arrays.asList( newBindings ) );
		}
		return list;
	}
	
	@SuppressWarnings("unchecked")
	public static List<IBinding> getAllBindings(ICubeQueryDefinition cubeQueryDefn)
	{
		List<IBinding> result = new ArrayList();
		result.addAll( cubeQueryDefn.getBindings( ) );
		result.addAll( getNewBindingsFromCubeOperations(cubeQueryDefn)  );
		return result;
	}
}
