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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.birt.core.script.ScriptContext;
import org.eclipse.birt.data.engine.api.IBaseExpression;
import org.eclipse.birt.data.engine.api.IBinding;
import org.eclipse.birt.data.engine.api.IConditionalExpression;
import org.eclipse.birt.data.engine.api.IExpressionCollection;
import org.eclipse.birt.data.engine.api.IFilterDefinition;
import org.eclipse.birt.data.engine.api.IScriptExpression;
import org.eclipse.birt.data.engine.api.ISortDefinition;
import org.eclipse.birt.data.engine.api.querydefn.FilterDefinition;
import org.eclipse.birt.data.engine.core.DataException;
import org.eclipse.birt.data.engine.olap.api.query.ICubeOperation;
import org.eclipse.birt.data.engine.olap.api.query.ICubeQueryDefinition;
import org.eclipse.birt.data.engine.olap.api.query.IDimensionDefinition;
import org.eclipse.birt.data.engine.olap.api.query.IEdgeDefinition;
import org.eclipse.birt.data.engine.olap.api.query.IHierarchyDefinition;
import org.eclipse.birt.data.engine.olap.api.query.ILevelDefinition;
import org.eclipse.birt.data.engine.olap.api.query.IMeasureDefinition;
import org.eclipse.birt.data.engine.olap.data.api.DimLevel;
import org.eclipse.birt.data.engine.olap.data.api.IDimensionSortDefn;
import org.eclipse.birt.data.engine.olap.data.impl.AggregationDefinition;
import org.eclipse.birt.data.engine.olap.data.impl.AggregationFunctionDefinition;
import org.eclipse.birt.data.engine.olap.impl.query.LevelDefiniton;
import org.eclipse.birt.data.engine.olap.impl.query.MeasureDefinition;
import org.eclipse.birt.data.engine.olap.util.ICubeAggrDefn;
import org.eclipse.birt.data.engine.olap.util.OlapExpressionCompiler;
import org.eclipse.birt.data.engine.olap.util.OlapExpressionUtil;
import org.eclipse.birt.data.engine.olap.util.filter.IJSMeasureFilterEvalHelper;
import org.eclipse.birt.data.engine.olap.util.filter.JSMeasureFilterEvalHelper;
import org.eclipse.birt.data.engine.script.ScriptConstants;
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
		List measureList = queryDefn.getMeasures( );
		ICubeAggrDefn[] cubeAggrs = OlapExpressionUtil.getAggrDefns( queryDefn.getBindings( ) );
		
		populateMeasureFromBinding( queryDefn, cx );
		populateMeasureFromFilter( queryDefn, cx );
		populateMeasureFromSort( queryDefn, cx );

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
			calculatedMembers1[index] = new CalculatedMember( innerName,
					measureDefn.getName( ),
					measureAggrOns,
					adaptAggrFunction( measureDefn ),
					0 );
			index++;
		}
		
		int startRsId = calculatedMembers1.length == 0 ? 0 : 1;
		
		CalculatedMember[] calculatedMembers2 = createCalculatedMembersByAggrOnList(startRsId,
				cubeAggrs, scope, cx);
		
		return uniteCalculatedMember(calculatedMembers1, calculatedMembers2);
		
	}
	
	public static CalculatedMember[] createCalculatedMembersByAggrOnList (int startRsId, ICubeAggrDefn[] cubeAggrs,
			Scriptable scope, ScriptContext cx ) throws DataException
	{
		if (cubeAggrs == null)
		{
			return new CalculatedMember[0];
		}
		
		assert startRsId >= 0;
		
		int preparedRsId = startRsId;
		CalculatedMember[] result = new CalculatedMember[cubeAggrs.length];
		List<CalculatedMember> withDistinctRsIds = new ArrayList<CalculatedMember>();
		int index = 0;
		for (ICubeAggrDefn cubeAggrDefn : cubeAggrs)
		{
			int id = getResultSetIndex( withDistinctRsIds,
					cubeAggrDefn.getAggrLevels( ) );
			if ( id == -1 )
			{
				result[index] = new CalculatedMember( cubeAggrDefn,
						preparedRsId );
				withDistinctRsIds.add( result[index] );
				preparedRsId++;
			}
			else
			{
				result[index] = new CalculatedMember( cubeAggrDefn,
						id );
			}

			if ( cubeAggrDefn.getFilter( ) != null )
			{
				IJSMeasureFilterEvalHelper filterEvalHelper = new JSMeasureFilterEvalHelper( scope, cx,
						new FilterDefinition( cubeAggrDefn.getFilter( ) ) );
				result[index].setFilterEvalHelper( filterEvalHelper );
			}
			index++;
		}
		return result;
	}
	
	public static CalculatedMember[] createCalculatedMembersByAggrOnListAndMeasureName (int startRsId, ICubeAggrDefn[] cubeAggrs,
			Scriptable scope, ScriptContext cx ) throws DataException
	{
		if (cubeAggrs == null)
		{
			return new CalculatedMember[0];
		}
		
		assert startRsId >= 0;
		
		int preparedRsId = startRsId;
		CalculatedMember[] result = new CalculatedMember[cubeAggrs.length];
		List<CalculatedMember> withDistinctRsIds = new ArrayList<CalculatedMember>();
		int index = 0;
		for (ICubeAggrDefn cubeAggrDefn : cubeAggrs)
		{
			int id = getResultSetIndex( withDistinctRsIds,
					cubeAggrDefn.getAggrLevels( ), cubeAggrDefn.getMeasure( ) );
			if ( id == -1 )
			{
				result[index] = new CalculatedMember( cubeAggrDefn,
						preparedRsId );
				withDistinctRsIds.add( result[index] );
				preparedRsId++;
			}
			else
			{
				result[index] = new CalculatedMember( cubeAggrDefn,
						id );
			}

			if ( cubeAggrDefn.getFilter( ) != null )
			{
				IJSMeasureFilterEvalHelper filterEvalHelper = new JSMeasureFilterEvalHelper( scope, cx,
						new FilterDefinition( cubeAggrDefn.getFilter( ) ));
				result[index].setFilterEvalHelper( filterEvalHelper );
			}
			index++;
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
			ICubeQueryDefinition query) throws DataException
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
				String[] dimInfo = ( (CalculatedMember) list.get( index ) ).getFirstArgumentInfo( );
				String dimName = null;
				String levelName = null;
				String attributeName = null;
				DimLevel dimLevel = null;
				if ( dimInfo != null && dimInfo.length == 3 )
				{
					dimName = ( (CalculatedMember) list.get( index ) ).getFirstArgumentInfo( )[0];
					levelName = ( (CalculatedMember) list.get( index ) ).getFirstArgumentInfo( )[1];
					attributeName = ( (CalculatedMember) list.get( index ) ).getFirstArgumentInfo( )[2];
					dimLevel = new DimLevel( dimName, levelName );
				}
				funcitons[index] = new AggregationFunctionDefinition( list.get( index ).getName( ),
						list.get( index ).getMeasureName( ),
						dimLevel,
						attributeName,
						list.get( index ).getAggrFunction( ),
						list.get( index ).getFilterEvalHelper( ) );
			}

			DimLevel[] levels = new DimLevel[calculatedMembers[i].getAggrOnList( )
					.size( )];
			int[] sortType = new int[calculatedMembers[i].getAggrOnList( ).size( )];
			for ( int index = 0; index < calculatedMembers[i].getAggrOnList( )
					.size( ); index++ )
			{
				Object obj = calculatedMembers[i].getAggrOnList( )
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
				String expr = sortDfn.getExpression( ).getText( );
			
				DimLevel info = getDimLevel( expr, query.getBindings( ) );

				if ( level.equals( info ) )
				{
					return sortDfn.getSortDirection( );
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
		if ( OlapExpressionUtil.isReferenceToDimLevel( expr ) == false )
			return null;
		else 
			return OlapExpressionUtil.getTargetDimLevel( expr );
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
	
	private static int getResultSetIndex( List aggrList, List levelList, String measureName )
	{
		for ( int i = 0; i < aggrList.size( ); i++ )
		{
			CalculatedMember member = (CalculatedMember) aggrList.get( i );
			if ( member.getAggrOnList( ).equals( levelList ) 
					&& isSameString(member.getMeasureName( ), measureName))
			{
				return member.getRsID( );
			}
		}		
		return -1;
	}
	
	private static boolean isSameString(String s1, String s2)
	{
		if (s1 == null)
		{
			return s2 == null;
		}
		else
		{
			return s1.equals( s2 );
		}
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
	 * @param measureMapping 
	 * @return
	 * @throws DataException 
	 */
	public static Map getRelationWithMeasure( ICubeQueryDefinition queryDefn, Map measureMapping ) throws DataException
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
		ICubeAggrDefn[] cubeAggrs1 = OlapExpressionUtil.getAggrDefns( orignalBindings );
		ICubeAggrDefn[] cubeAggrs2 = OlapExpressionUtil.getAggrDefnsByNestBinding( newBindings );
		ICubeAggrDefn[] cubeAggrs = new ICubeAggrDefn[cubeAggrs1.length + cubeAggrs2.length];
		System.arraycopy( cubeAggrs1, 0, cubeAggrs, 0, cubeAggrs1.length );
		System.arraycopy( cubeAggrs2, 0, cubeAggrs, cubeAggrs1.length, cubeAggrs2.length );
		 if ( cubeAggrs != null && cubeAggrs.length > 0 )
		{
			for ( int i = 0; i < cubeAggrs.length; i++ )
			{
				if ( cubeAggrs[i].getAggrName( ) == null )
					continue;
				List aggrOns = cubeAggrs[i].getAggrLevels( );
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
