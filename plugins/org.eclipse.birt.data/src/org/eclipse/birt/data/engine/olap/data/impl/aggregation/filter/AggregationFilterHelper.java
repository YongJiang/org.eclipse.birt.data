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

package org.eclipse.birt.data.engine.olap.data.impl.aggregation.filter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.birt.data.engine.core.DataException;
import org.eclipse.birt.data.engine.olap.api.query.ICubeFilterDefinition;
import org.eclipse.birt.data.engine.olap.api.query.ILevelDefinition;
import org.eclipse.birt.data.engine.olap.data.api.DimLevel;
import org.eclipse.birt.data.engine.olap.data.api.IAggregationResultSet;
import org.eclipse.birt.data.engine.olap.data.api.ILevel;
import org.eclipse.birt.data.engine.olap.data.api.ISelection;
import org.eclipse.birt.data.engine.olap.data.api.cube.IDimension;
import org.eclipse.birt.data.engine.olap.data.impl.AggregationDefinition;
import org.eclipse.birt.data.engine.olap.data.impl.AggregationFunctionDefinition;
import org.eclipse.birt.data.engine.olap.data.impl.Constants;
import org.eclipse.birt.data.engine.olap.data.impl.Cube;
import org.eclipse.birt.data.engine.olap.data.impl.SelectionFactory;
import org.eclipse.birt.data.engine.olap.data.impl.dimension.Member;
import org.eclipse.birt.data.engine.olap.data.util.BufferedPrimitiveDiskArray;
import org.eclipse.birt.data.engine.olap.data.util.CompareUtil;
import org.eclipse.birt.data.engine.olap.data.util.IDiskArray;
import org.eclipse.birt.data.engine.olap.data.util.ObjectArrayUtil;
import org.eclipse.birt.data.engine.olap.data.util.OrderedDiskArray;
import org.eclipse.birt.data.engine.olap.data.util.SetUtil;
import org.eclipse.birt.data.engine.olap.util.filter.IJSDimensionFilterHelper;
import org.eclipse.birt.data.engine.olap.util.filter.IJSFilterHelper;
import org.eclipse.birt.data.engine.olap.util.filter.IJSTopBottomFilterHelper;

/**
 * 
 */

public class AggregationFilterHelper
{

	private Map dimensionMap;
	private List aggrFilters;
	private List topbottomFilters;
	private boolean isEmptyXtab;

	/**
	 * 
	 * @param cube
	 * @param jsFilterHelpers
	 */
	public AggregationFilterHelper( Cube cube, List jsFilterHelpers )
	{
		populateDimensionLevels( cube );
		// populate the filter helpers to aggrFilters and topbottomFilters
		populateFilters( jsFilterHelpers );
	}

	/**
	 * transform the specified filter helpers to level filters for another
	 * aggregation calculation. Note: if the returned list is null, which means
	 * the final aggregation result will be empty, and no more calculations are
	 * needed.
	 * 
	 * @param aggregations
	 * @param resultSet
	 * @return
	 * @throws DataException
	 */
	public List generateLevelFilters( AggregationDefinition[] aggregations,
			IAggregationResultSet[] resultSet ) throws DataException
	{
		List levelFilterList = new ArrayList( );
		try
		{
			applyAggrFilters( aggregations, resultSet, levelFilterList );
			if ( isEmptyXtab == true )
			{
				return null;
			}
			applyTopBottomFilters( aggregations, resultSet, levelFilterList );
		}
		catch ( IOException e )
		{
			throw new DataException( "", e );//$NON-NLS-1$
		}
		return levelFilterList;
	}

	/**
	 * 
	 * @param jsFilterHelpers
	 */
	private void populateFilters( List jsFilterHelpers )
	{
		aggrFilters = new ArrayList( );
		topbottomFilters = new ArrayList( );
		for ( Iterator i = jsFilterHelpers.iterator( ); i.hasNext( ); )
		{
			IJSFilterHelper filterHelper = (IJSFilterHelper) i.next( );
			if ( filterHelper instanceof IJSDimensionFilterHelper )
			{
				IJSDimensionFilterHelper dimFilterHelper = (IJSDimensionFilterHelper) filterHelper;
				aggrFilters.add( new AggrFilterDefinition( dimFilterHelper ) );
			}
			else if ( filterHelper instanceof IJSTopBottomFilterHelper )
			{
				IJSTopBottomFilterHelper tbFilterHelper = (IJSTopBottomFilterHelper) filterHelper;
				topbottomFilters.add( new TopBottomFilterDefinition( tbFilterHelper ) );
			}
		}
	}

	/**
	 * 
	 * @param cube
	 */
	private void populateDimensionLevels( Cube cube )
	{
		dimensionMap = new HashMap( );
		IDimension[] dimensions = cube.getDimesions( );
		for ( int i = 0; i < dimensions.length; i++ )
		{
			dimensionMap.put( dimensions[i].getName( ),
					dimensions[i].getHierarchy( ).getLevels( ) );
		}
	}

	/**
	 * 
	 * @param aggregations
	 * @param resultSet
	 * @param levelFilterList
	 * @throws DataException
	 * @throws IOException
	 */
	private void applyAggrFilters( AggregationDefinition[] aggregations,
			IAggregationResultSet[] resultSet, List levelFilterList )
			throws DataException, IOException
	{
		for ( Iterator i = aggrFilters.iterator( ); i.hasNext( ); )
		{
			AggrFilterDefinition filter = (AggrFilterDefinition) i.next( );
			for ( int j = 0; !isEmptyXtab && j < aggregations.length; j++ )
			{
				if ( aggregations[j].getAggregationFunctions( ) != null
						&& FilterUtil.isEqualLevels( aggregations[j].getLevels( ),
								filter.getAggrLevels( ) ) )
				{
					applyAggrFilter( aggregations,
							resultSet,
							j,
							filter,
							levelFilterList );
				}
			}
		}
	}

	/**
	 * @param aggregations
	 * @param resultSet
	 * @param j
	 * @param filter
	 * @param levelFilters
	 * @throws IOException
	 * @throws DataException
	 */
	private void applyAggrFilter( AggregationDefinition[] aggregations,
			IAggregationResultSet[] resultSet, int j,
			AggrFilterDefinition filter, List levelFilters )
			throws DataException, IOException
	{
		AggregationFunctionDefinition[] aggrFuncs = aggregations[j].getAggregationFunctions( );
		DimLevel[] aggrLevels = filter.getAggrLevels( );
		// currently we just support one level key
		// generate a row against levels and aggrNames
		String[] fields = getAllFieldNames( aggrLevels, resultSet[j] );
		String[] aggrNames = new String[aggrFuncs.length];
		for ( int k = 0; k < aggrFuncs.length; k++ )
		{
			aggrNames[k] = aggrFuncs[k].getName( );
		}

		DimLevel targetLevel = filter.getTargetLevel( );
		ILevel[] levelsOfDimension = getLevelsOfDimension( targetLevel.getDimensionName( ) );
		int targetIndex = FilterUtil.getTargetLevelIndex( levelsOfDimension,
				targetLevel.getLevelName( ) );
		// template key values' list that have been filtered in the same
		// qualified level
		List selKeyValueList = new ArrayList( );
		// to remember the members of the dimension that consists of the
		// previous aggregation result's target level
		Member[] preMembers = null;
		IJSDimensionFilterHelper filterHelper = (IJSDimensionFilterHelper) filter.getFilterHelper( );
		for ( int k = 0; k < resultSet[j].length( ); k++ )
		{
			resultSet[j].seek( k );
			int fieldIndex = 0;
			Object[] fieldValues = new Object[fields.length];
			Object[] aggrValues = new Object[aggrFuncs.length];
			// fill field values
			for ( int m = 0; m < aggrLevels.length; m++ )
			{
				int levelIndex = resultSet[j].getLevelIndex( aggrLevels[m] );
				if ( levelIndex < 0
						|| levelIndex >= resultSet[j].getLevelCount( ) )
					continue;
				fieldValues[fieldIndex++] = resultSet[j].getLevelKeyValue( levelIndex )[0];

			}
			// fill aggregation names and values
			for ( int m = 0; m < aggrFuncs.length; m++ )
			{
				int aggrIndex = resultSet[j].getAggregationIndex( aggrNames[m] );
				aggrValues[m] = resultSet[j].getAggregationValue( aggrIndex );
			}
			RowForFilter row = new RowForFilter( fields, aggrNames );
			row.setFieldValues( fieldValues );
			row.setAggrValues( aggrValues );
			boolean isSelect = filterHelper.evaluateFilter( row );
			if ( isSelect )
			{// generate level filter here
				Member[] members = getTargetDimMembers( targetLevel.getDimensionName( ),
						resultSet[j] );
				if ( preMembers != null
						&& !FilterUtil.shareParentLevels( members,
								preMembers,
								targetIndex ) )
				{
					LevelFilter levelFilter = toLevelFilter( targetLevel,
							selKeyValueList,
							preMembers,
							filterHelper );
					levelFilters.add( levelFilter );
					selKeyValueList.clear( );
				}
				int levelIndex = resultSet[j].getLevelIndex( targetLevel );
				// select aggregation row
				Object[] levelKeyValue = resultSet[j].getLevelKeyValue( levelIndex );
				if ( levelKeyValue != null && levelKeyValue[0] != null )
					selKeyValueList.add( levelKeyValue );
				preMembers = members;
			}
		}
		// ---------------------------------------------------------------------------------
		if ( preMembers == null )
		{// filter is empty, so that the final x-Tab will be empty
			isEmptyXtab = true;
			return;
		}
		// generate the last level filter
		if ( !selKeyValueList.isEmpty( ) )
		{
			LevelFilter levelFilter = toLevelFilter( targetLevel,
					selKeyValueList,
					preMembers,
					filterHelper );
			levelFilters.add( levelFilter );
		}
	}

	/**
	 * get the members of the specified dimension from the aggregation result
	 * set. Note: only the members of the levels which reside in the result set
	 * will be fetched, otherwise the corresponding member value is null.
	 * 
	 * 
	 * @param dimensionName
	 * @param resultSet
	 * @return
	 */
	private Member[] getTargetDimMembers( String dimensionName,
			IAggregationResultSet resultSet )
	{
		ILevel[] levels = getLevelsOfDimension( dimensionName );
		Member[] members = new Member[levels.length];
		for ( int i = 0; i < levels.length; i++ )
		{
			int levelIndex = resultSet.getLevelIndex( new DimLevel( dimensionName,
					levels[i].getName( ) ) );
			if ( levelIndex >= 0 )
			{
				Object[] values = resultSet.getLevelKeyValue( levelIndex );
				Object[] fieldValues = ObjectArrayUtil.convert( new Object[][]{
						values, null
				} );
				members[i] = (Member) Member.getCreator( )
						.createInstance( fieldValues );
			}
		}
		return members;
	}

	/**
	 * @param targetLevel
	 * @param selKeyValueList
	 * @param dimMembers
	 * @param filterHelper
	 * @return
	 */
	private LevelFilter toLevelFilter( DimLevel targetLevel,
			List selKeyValueList, Member[] dimMembers,
			IJSFilterHelper filterHelper )
	{
		Object[][] keyValues = new Object[selKeyValueList.size( )][];
		for ( int i = 0; i < selKeyValueList.size( ); i++ )
		{
			keyValues[i] = (Object[]) selKeyValueList.get( i );
		}
		ISelection selection = SelectionFactory.createMutiKeySelection( keyValues );
		LevelFilter levelFilter = new LevelFilter( targetLevel,
				new ISelection[]{
					selection
				} );
		levelFilter.setDimMembers( dimMembers );
		levelFilter.setFilterHelper( filterHelper );
		return levelFilter;
	}

	/**
	 * 
	 * @param aggregations
	 * @param resultSet
	 * @param levelFilterList
	 * @throws IOException
	 * @throws DataException
	 * @throws IOException
	 */
	private void applyTopBottomFilters( AggregationDefinition[] aggregations,
			IAggregationResultSet[] resultSet, List levelFilterList )
			throws DataException, IOException
	{
		for ( int i = 0; i < aggregations.length; i++ )
		{
			if ( aggregations[i].getAggregationFunctions( ) == null )
				continue;
			Map levelFilterMap = new HashMap( );
			for ( Iterator j = topbottomFilters.iterator( ); j.hasNext( ); )
			{
				TopBottomFilterDefinition filter = (TopBottomFilterDefinition) j.next( );
				if ( filter.getFilterHelper( ).isAggregationFilter( ) )
				{// aggregation top/bottom filter
					if ( FilterUtil.isEqualLevels( aggregations[i].getLevels( ),
							filter.getAggrLevels( ) ) )
					{
						IDiskArray levelKeyList = populateLevelKeyList( aggregations[i],
								resultSet[i],
								filter );
						IDiskArray selectedLevelKeys = null;
						if ( levelFilterMap.containsKey( filter.getTargetLevel( ) ) )
						{
							Object[] valueObjs = (Object[]) levelFilterMap.get( filter.getTargetLevel( ) );
							selectedLevelKeys = (IDiskArray) valueObjs[0];
							selectedLevelKeys = SetUtil.getIntersection( selectedLevelKeys,
									levelKeyList );
						}
						else
						{
							selectedLevelKeys = levelKeyList;
						}
						levelFilterMap.put( filter.getTargetLevel( ),
								new Object[]{
										selectedLevelKeys,
										filter.getFilterHelper( )
								} );
					}
				}
			}
			// generate level filters according to the selected level keys
			for ( Iterator j = levelFilterMap.keySet( ).iterator( ); j.hasNext( ); )
			{
				DimLevel target = (DimLevel) j.next( );
				Object[] valueObjs = (Object[]) levelFilterMap.get( target );
				IDiskArray selectedKeyArray = (IDiskArray) valueObjs[0];
				IJSFilterHelper filterHelper = (IJSFilterHelper) valueObjs[1];
				if ( selectedKeyArray.size( ) == 0 )
					continue;
				ILevel[] levels = getLevelsOfDimension( target.getDimensionName( ) );
				int index = FilterUtil.getTargetLevelIndex( levels,
						target.getLevelName( ) );
				Map keyMap = new HashMap( );
				for ( int k = 0; k < selectedKeyArray.size( ); k++ )
				{
					MultiKey multiKey = (MultiKey) selectedKeyArray.get( k );
					String parentKey = getParentKey( multiKey.dimMembers, index );
					List keyList = (List) keyMap.get( parentKey );
					if ( keyList == null )
					{
						keyList = new ArrayList( );
						keyMap.put( parentKey, keyList );
					}
					keyList.add( multiKey );
				}
				for ( Iterator keyItr = keyMap.values( ).iterator( ); keyItr.hasNext( ); )
				{
					List keyList = (List) keyItr.next( );
					ISelection selections = toMultiKeySelection( keyList );
					LevelFilter levelFilter = new LevelFilter( target,
							new ISelection[]{
								selections
							} );
					// use the first key's dimension members since all them
					// share same parent levels (with the same parent key)
					levelFilter.setDimMembers( ( (MultiKey) keyList.get( 0 ) ).dimMembers );
					levelFilter.setFilterHelper( filterHelper );
					levelFilterList.add( levelFilter );
				}
			}
		}
	}

	/**
	 * 
	 * @param dimensionName
	 * @return
	 */
	private ILevel[] getLevelsOfDimension( String dimensionName )
	{
		return (ILevel[]) dimensionMap.get( dimensionName );
	}

	/**
	 * 
	 * @param aggregation
	 * @param resultSet
	 * @param filter
	 * @param levelFilters
	 * @return
	 */
	private IDiskArray populateLevelKeyList( AggregationDefinition aggregation,
			IAggregationResultSet resultSet, TopBottomFilterDefinition filter )
			throws DataException
	{
		IJSTopBottomFilterHelper filterHelper = (IJSTopBottomFilterHelper) filter.getFilterHelper( );
		int n = -1;
		if ( filterHelper.isPercent( ) == false )
		{
			n = (int) filterHelper.getN( );
		}

		IDiskArray aggrValueArray = new OrderedDiskArray( n,
				filterHelper.isTop( ) );
		AggregationFunctionDefinition[] aggrFuncs = aggregation.getAggregationFunctions( );
		DimLevel[] aggrLevels = filter.getAggrLevels( );
		String dimensionName = filter.getTargetLevel( ).getDimensionName( );
		// currently we just support one level key
		// generate a row against levels and aggrNames
		String[] fields = getAllFieldNames( aggrLevels, resultSet );
		String[] aggrNames = new String[aggrFuncs.length];
		for ( int k = 0; k < aggrFuncs.length; k++ )
		{
			aggrNames[k] = aggrFuncs[k].getName( );
		}
		try
		{
			for ( int k = 0; k < resultSet.length( ); k++ )
			{
				resultSet.seek( k );
				int fieldIndex = 0;
				Object[] fieldValues = new Object[fields.length];
				Object[] aggrValues = new Object[aggrFuncs.length];
				// fill field values
				for ( int m = 0; m < aggrLevels.length; m++ )
				{
					int levelIndex = resultSet.getLevelIndex( aggrLevels[m] );
					if ( levelIndex < 0
							|| levelIndex >= resultSet.getLevelCount( ) )
						continue;
					fieldValues[fieldIndex++] = resultSet.getLevelKeyValue( levelIndex )[0];
				}
				// fill aggregation names and values
				for ( int m = 0; m < aggrFuncs.length; m++ )
				{
					int aggrIndex = resultSet.getAggregationIndex( aggrNames[m] );
					aggrValues[m] = resultSet.getAggregationValue( aggrIndex );
				}
				RowForFilter row = new RowForFilter( fields, aggrNames );
				row.setFieldValues( fieldValues );
				row.setAggrValues( aggrValues );

				int levelIndex = resultSet.getLevelIndex( filter.getTargetLevel( ) );
				Object[] levelKey = resultSet.getLevelKeyValue( levelIndex );
				if ( levelKey != null && filterHelper.isQualifiedRow( row ) )
				{
					Object aggrValue = filterHelper.evaluateFilterExpr( row );
					Member[] members = getTargetDimMembers( dimensionName,
							resultSet );
					aggrValueArray.add( new ValueObject( aggrValue,
							new MultiKey( levelKey, members ) ) );
				}
			}
			return fetchLevelKeys( aggrValueArray, filterHelper );
		}
		catch ( IOException e )
		{
			throw new DataException( "", e );//$NON-NLS-1$
		}

	}

	/**
	 * @param aggrValueArray
	 * @param filterHelper
	 * @return
	 * @throws IOException
	 */
	private IDiskArray fetchLevelKeys( IDiskArray aggrValueArray,
			IJSTopBottomFilterHelper filterHelper ) throws IOException
	{
		IDiskArray levelKeyArray = new BufferedPrimitiveDiskArray( Constants.LIST_BUFFER_SIZE );
		int start = 0; // level key start index in aggrValueArray
		int end = aggrValueArray.size( ); // level key end index (not
		// including) in aggrValueArray
		if ( filterHelper.isPercent( ) )
		{// top/bottom percentage filter
			int size = aggrValueArray.size( ); // target level member size
			int n = FilterUtil.getTargetN( size, filterHelper.getN( ) );
			if ( filterHelper.isTop( ) )
				start = size - n;
			else
				end = n;
		}
		for ( int i = start; i < end; i++ )
		{
			ValueObject aggrValue = (ValueObject) aggrValueArray.get( i );
			levelKeyArray.add( aggrValue.index );
		}
		return levelKeyArray;
	}

	/**
	 * 
	 * @param members
	 * @param index
	 * @return
	 */
	private String getParentKey( Member[] members, int index )
	{
		assert index >= 0 && index < members.length;
		StringBuffer buf = new StringBuffer( );
		for ( int i = 0; i < index; i++ )
		{
			if ( members[i] == null )
			{
				buf.append( '?' );
			}
			else
			{
				Object[] keyValues = members[i].getKeyValues( );
				if ( keyValues != null && keyValues.length > 0 )
				{
					for ( int j = 0; j < keyValues.length; j++ )
					{
						buf.append( keyValues[j].toString( ) );
						buf.append( ',' );
					}
					buf.deleteCharAt( buf.length( ) - 1 );
				}
			}
			buf.append( '-' );
		}
		if ( buf.length( ) > 0 )
		{
			buf.deleteCharAt( buf.length( ) - 1 );
		}
		return buf.toString( );
	}

	/**
	 * @param keyList
	 * @return
	 */
	private ISelection toMultiKeySelection( List keyList )
	{
		Object[][] keys = new Object[keyList.size( )][];
		for ( int i = 0; i < keyList.size( ); i++ )
		{
			MultiKey multiKey = (MultiKey) keyList.get( i );
			keys[i] = multiKey.values;
		}
		return SelectionFactory.createMutiKeySelection( keys );
	}

	/**
	 * get all field names of a level, including key column names and attribute
	 * column names. TODO: we just get all the field names, and will further
	 * support key names and attributes as field names.
	 * 
	 * @param levels
	 * @param resultSet
	 * @return
	 */
	private String[] getAllFieldNames( DimLevel[] levels,
			IAggregationResultSet resultSet )
	{
		List fieldNameList = new ArrayList( );
		for ( int i = 0; i < levels.length; i++ )
		{
			int levelIndex = resultSet.getLevelIndex( levels[i] );
			if ( levelIndex < 0 || levelIndex >= resultSet.getLevelCount( ) )
				continue;
			fieldNameList.add( levels[i].getDimensionName( )
					+ '/' + levels[i].getLevelName( ) );
		}
		String[] fieldNames = new String[fieldNameList.size( )];
		fieldNameList.toArray( fieldNames );
		return fieldNames;
	}

}

/**
 * 
 */
class MultiKey implements Comparable
{

	Object[] values;
	Member[] dimMembers; // dimension members associate with this level key

	MultiKey( Object[] values, Member[] dimensionMembers )
	{
		this.values = values;
		this.dimMembers = dimensionMembers;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Comparable#compareTo(T)
	 */
	public int compareTo( Object obj )
	{
		if ( obj == null )
			return -1;
		else if ( obj instanceof MultiKey )
		{
			MultiKey key = (MultiKey) obj;
			return CompareUtil.compare( values, key.values );
		}
		return -1;
	}

	/**
	 * @return the dimensionMembers
	 */
	Member[] getDimMembers( )
	{
		return dimMembers;
	}

	/**
	 * @param dimensionMembers
	 *            the dimensionMembers to set
	 */
	void setDimMembers( Member[] dimensionMembers )
	{
		this.dimMembers = dimensionMembers;
	}
}

/**
 * 
 * 
 */
class AggrFilterDefinition
{

	protected DimLevel[] aggrLevels;
	protected IJSFilterHelper filterHelper;
	protected DimLevel targetLevel;
	protected DimLevel[] axisQualifierLevels;
	protected Object[] axisQualifierValues;

	AggrFilterDefinition( IJSFilterHelper filterEvalHelper )
	{
		filterHelper = filterEvalHelper;
		ICubeFilterDefinition cubeFilter = filterEvalHelper.getCubeFilterDefinition( );
		targetLevel = new DimLevel( cubeFilter.getTargetLevel( ) );
		aggrLevels = filterEvalHelper.getAggrLevels( );
		ILevelDefinition[] axisLevels = cubeFilter.getAxisQualifierLevels( );
		if ( axisLevels != null )
		{
			axisQualifierLevels = new DimLevel[axisLevels.length];
			for ( int i = 0; i < axisLevels.length; i++ )
			{
				axisQualifierLevels[i] = new DimLevel( axisLevels[i] );
			}
		}
		axisQualifierValues = cubeFilter.getAxisQualifierValues( );
	}

	/**
	 * @return the axisQualifierLevelNames
	 */
	DimLevel[] getAxisQualifierLevels( )
	{
		return axisQualifierLevels;
	}

	/**
	 * @return the axisQualifierLevelValues
	 */
	Object[] getAxisQualifierValues( )
	{
		return axisQualifierValues;
	}

	/**
	 * @return the aggrLevels
	 */
	DimLevel[] getAggrLevels( )
	{
		return aggrLevels;
	}

	/**
	 * @return the aggrFilter
	 */
	IJSFilterHelper getFilterHelper( )
	{
		return filterHelper;
	}

	/**
	 * @return the targetLevel
	 */
	DimLevel getTargetLevel( )
	{
		return targetLevel;
	}
}

/**
 * 
 */
class TopBottomFilterDefinition extends AggrFilterDefinition
{

	double n;
	int filterType;

	/**
	 * 
	 * @param filterHelper
	 */
	TopBottomFilterDefinition( IJSFilterHelper filterHelper )
	{
		super( filterHelper );
		this.filterHelper = filterHelper;
		IJSTopBottomFilterHelper topBottomFilterHelper = ( (IJSTopBottomFilterHelper) filterHelper );
		this.filterType = topBottomFilterHelper.getFilterType( );
		this.n = topBottomFilterHelper.getN( );
	}

	/**
	 * @return the n, which will be greater than zero.
	 */
	double getN( )
	{
		return n;
	}

	/**
	 * @return the filterType
	 */
	int getFilterType( )
	{
		return filterType;
	}
}
