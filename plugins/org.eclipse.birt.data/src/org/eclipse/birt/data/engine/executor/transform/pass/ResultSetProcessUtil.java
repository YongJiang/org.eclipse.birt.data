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

package org.eclipse.birt.data.engine.executor.transform.pass;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.birt.data.engine.core.DataException;
import org.eclipse.birt.data.engine.executor.ResultClass;
import org.eclipse.birt.data.engine.executor.ResultFieldMetadata;
import org.eclipse.birt.data.engine.executor.aggregation.AggrDefnRoundManager;
import org.eclipse.birt.data.engine.executor.aggregation.AggregationHelper;
import org.eclipse.birt.data.engine.executor.cache.SortSpec;
import org.eclipse.birt.data.engine.executor.transform.IExpressionProcessor;
import org.eclipse.birt.data.engine.executor.transform.OdiResultSetWrapper;
import org.eclipse.birt.data.engine.executor.transform.ResultSetPopulator;
import org.eclipse.birt.data.engine.executor.transform.TransformationConstants;
import org.eclipse.birt.data.engine.impl.ComputedColumnHelper;
import org.eclipse.birt.data.engine.impl.FilterByRow;
import org.eclipse.birt.data.engine.impl.StopSign;
import org.eclipse.birt.data.engine.odi.IResultClass;

/**
 * The class used to process ResultSet data.
 * 
 */
class ResultSetProcessUtil extends RowProcessUtil
{
	/**
	 * 
	 */
	private List cachedSort;

	private boolean groupingDone;
	/**
	 * 
	 * @param populator
	 * @param iccState
	 * @param computedColumnHelper
	 * @param filterByRow
	 * @param psController
	 */
	private ResultSetProcessUtil( ResultSetPopulator populator,
			ComputedColumnsState iccState,
			ComputedColumnHelper computedColumnHelper, FilterByRow filterByRow,
			PassStatusController psController )
	{
		super( populator,
				iccState,
				computedColumnHelper,
				filterByRow,
				psController);
	}

	/**
	 * 
	 * @param populator
	 * @param iccState
	 * @param computedColumnHelper
	 * @param filterByRow
	 * @param psController
	 * @param sortList
	 * @throws DataException
	 */
	public static void doPopulate( ResultSetPopulator populator,
			ComputedColumnsState iccState,
			ComputedColumnHelper computedColumnHelper, FilterByRow filterByRow,
			PassStatusController psController, List sortList, StopSign stopSign )
			throws DataException
	{
		ResultSetProcessUtil instance = new ResultSetProcessUtil( populator,
				iccState,
				computedColumnHelper,
				filterByRow,
				psController);
		instance.cachedSort = sortList;
		instance.populateResultSet( stopSign );

	}

	/**
	 * 
	 * @param stopSign
	 * @throws DataException
	 */
	private void populateResultSet( StopSign stopSign ) throws DataException
	{
		//The computed columns that need multipass
		List aggCCList = prepareComputedColumns( TransformationConstants.RESULT_SET_MODEL );
		
		//Grouping will also be done in this method, for currently we only support simple group keys
		//that is, group keys cannot contain aggregation.
				
		doRowFiltering( stopSign );
		
		//TODO remove me
		populateTempComputedColumns( this.getAggrComputedColumns( aggCCList, false ), stopSign );
		///////////////
		
		List aggrDefns = this.populator.getEventHandler( ).getAggrDefinitions( );
		
		prepareAggregations( aggrDefns, stopSign );
		
		//Filter group instances.
		doGroupFiltering( stopSign );

		//Filter aggregation filters
		doAggrRowFiltering( stopSign );
		
		//Do row sorting
		doRowSorting( stopSign );
		
		//Do group sorting
		doGroupSorting( stopSign );
		
		if ( !groupingDone )
		{
			PassUtil.pass( this.populator,
					new OdiResultSetWrapper( populator.getResultIterator( ) ),
					true,
					stopSign );
			groupingDone = true;
		}
		
		clearTemporaryComputedColumns( iccState, stopSign );
	}

	/**
	 * 
	 * @param aggrDefns
	 * @param stopSign
	 * @throws DataException
	 */
	private void prepareAggregations( List aggrDefns, StopSign stopSign ) throws DataException
	{
		boolean needGroupFiltering = this.needDoGroupFiltering( );
		boolean needGroupSorting = this.needDoGroupSorting( );
		boolean needAggrFiltering = psController.needDoOperation( PassStatusController.AGGR_ROW_FILTERING );
		if ( needPreCalculateForGroupFilterSort( needGroupFiltering,
				needGroupSorting ) || needAggrFiltering )
		{
			if ( !groupingDone )
			{
				PassUtil.pass( this.populator,
						new OdiResultSetWrapper( populator.getResultIterator( ) ),
						true,
						stopSign);
			}
			this.populator.getExpressionProcessor( )
					.setResultIterator( this.populator.getResultIterator( ) );
			AggrDefnRoundManager factory = new AggrDefnRoundManager( aggrDefns );
			this.populator.getResultIterator( ).clearAggrValueHolder( );
			for ( int i = 0; i < factory.getRound( ); i++ )
			{
				AggregationHelper helper = new AggregationHelper( factory.getAggrDefnManager( i ),
						this.populator );
				this.populator.getResultIterator( ).addAggrValueHolder( helper );
			}
		}
	}

	/**
	 * Indicate whether need to pre calculate the aggregations.
	 * @param needGroupFiltering
	 * @param needGroupSorting
	 * @return
	 */
	private boolean needPreCalculateForGroupFilterSort(
			boolean needGroupFiltering, boolean needGroupSorting )
	{
		return needGroupFiltering || needGroupSorting;
	}

	/**
	 * Indicate whether need to do group filtering.
	 * @return
	 */
	private boolean needDoGroupFiltering( )
	{
		for ( int i = 0; i < this.populator.getQuery( ).getGrouping( ).length; i++ )
		{
			List groupFilters = this.populator.getQuery( ).getGrouping( )[i].getFilters( );
			if( groupFilters != null && groupFilters.size( ) > 0 )
				return true;
		}
		return false;
	}
	
	/**
	 * Indicate whether need to do group sorting.
	 * @return
	 */
	private boolean needDoGroupSorting( )
	{
		for ( int i = 0; i < this.populator.getQuery( ).getGrouping( ).length; i++ )
		{
			List groupFilters = this.populator.getQuery( ).getGrouping( )[i].getSorts( );
			if( groupFilters != null && groupFilters.size( ) > 0 )
				return true;
		}
		return false;
	}
	
	/**
	 * 
	 * @param aggCCList
	 * @param stopSign
	 * @throws DataException
	 */
	private void populateTempComputedColumns( List aggCCList, StopSign stopSign ) throws DataException
	{
		if ( psController.needDoOperation( PassStatusController.RESULT_SET_TEMP_COMPUTED_COLUMN_POPULATING ) )
		{
			if ( aggCCList.size( ) != 0
					|| psController.needDoOperation( PassStatusController.GROUP_ROW_FILTERING ) )
			{
				PassUtil.pass( this.populator,
						new OdiResultSetWrapper( populator.getResultIterator( ) ),
						true,
						stopSign);
				this.groupingDone = true;
			}
			if ( aggCCList.size( ) != 0 )
			{				
				computedColumnHelper.getComputedColumnList( ).clear( );
				computedColumnHelper.getComputedColumnList( )
						.addAll( aggCCList );
				computedColumnHelper.setRePrepare( true );
				IExpressionProcessor ep = populator.getExpressionProcessor( );

				ep.setResultIterator( populator.getResultIterator( ) );

				// Populate all temp computed columns ( used for query
				// filtering,sorting )
				while ( !isICCStateFinish( ) )
				{
					ep.evaluateMultiPassExprOnCmp( iccState, false );
				}
			}

			doGroupRowFilter( stopSign );
		}
	}

	/**
	 * @param stopSign
	 * @throws DataException
	 */
	private void doGroupSorting( StopSign stopSign ) throws DataException
	{
		if ( !this.needDoGroupSorting( ))
			return;

		if ( !groupingDone )
		{
			PassUtil.pass( this.populator,
					new OdiResultSetWrapper( populator.getResultIterator( ) ),
					true,
					stopSign );
			groupingDone = true;
		}
		
		//If the aggregation value is subject to change caused by group instance filter and row filter, recalculate the
		//aggregations.
		if( this.needDoGroupFiltering( ) || psController.needDoOperation( PassStatusController.AGGR_ROW_FILTERING ))
			prepareAggregations( this.populator.getEventHandler( ).getAggrDefinitions( ), stopSign );
		
		this.populator.getGroupProcessorManager( )
				.doGroupSorting( this.populator.getCache( ),
						this.populator.getExpressionProcessor( ),
						stopSign );
	}

	/**
	 * @param stopSign
	 * @throws DataException
	 */
	private void doRowSorting( StopSign stopSign ) throws DataException
	{
		this.populator.getQuery( ).setOrdering( this.cachedSort );
		
		SortSpec spec = this.populator.getGroupProcessorManager( )
				.getGroupCalculationUtil( )
				.getSortSpec( );
		if ( spec != null && spec.length( ) > 0 )
		{
			PassUtil.pass( this.populator,
					new OdiResultSetWrapper( populator.getResultIterator( ) ),
					true,
					stopSign );
			this.groupingDone = true;
		}
	}

	/**
	 * @param stopSign
	 * @throws DataException
	 */
	private void doGroupFiltering( StopSign stopSign ) throws DataException
	{
		if ( !this.needDoGroupFiltering( ) )
			return;
		if ( !groupingDone )
		{
			PassUtil.pass( this.populator,
					new OdiResultSetWrapper( populator.getResultIterator( ) ),
					true,
					stopSign );
			groupingDone = true;
		}

		this.populator.getGroupProcessorManager( )
				.doGroupFiltering( this.populator.getCache( ),
						this.populator.getExpressionProcessor( ),
						stopSign );
	}

	/**
	 * @param stopSign
	 * @throws DataException
	 */
	private void doRowFiltering( StopSign stopSign ) throws DataException
	{
		if(!psController.needDoOperation( PassStatusController.RESULT_SET_FILTERING ))
			return;
		
		boolean changeMaxRows = filterByRow.getFilterList( FilterByRow.GROUP_FILTER )
				.size( ) + filterByRow.getFilterList( FilterByRow.AGGR_FILTER )
				.size( )> 0 ;
		applyFilters( FilterByRow.QUERY_FILTER, changeMaxRows, stopSign );
		filterByRow.setWorkingFilterSet( FilterByRow.NO_FILTER );
	}

	/**
	 * @param stopSign
	 * @throws DataException
	 */
	private void doAggrRowFiltering( StopSign stopSign ) throws DataException
	{
		if(!psController.needDoOperation( PassStatusController.AGGR_ROW_FILTERING ))
			return;
		
		applyFilters( FilterByRow.AGGR_FILTER, false, stopSign );
		filterByRow.setWorkingFilterSet( FilterByRow.NO_FILTER );
	}
	
	/**
	 * 
	 * @return
	 */
	private boolean isICCStateFinish( )
	{
		for ( int i = 0; i < iccState.getCount( ); i++ )
		{
			if ( !iccState.isValueAvailable( i ) )
				return false;
		}
		return true;
	}

	/**
	 * @param stopSign
	 * @throws DataException
	 */
	private void doGroupRowFilter( StopSign stopSign ) throws DataException
	{
		if ( !psController.needDoOperation( PassStatusController.GROUP_ROW_FILTERING ) )
			return;
		// Apply group row filters (Total.isTopN, Total.isBottomN..)
		filterByRow.setWorkingFilterSet( FilterByRow.GROUP_FILTER );
		PassUtil.pass( this.populator,
				new OdiResultSetWrapper( populator.getResultIterator( ) ), true, stopSign );

		filterByRow.setWorkingFilterSet( FilterByRow.NO_FILTER );

	}
	
	/**
	 * 
	 * @param iccState
	 * @param stopSign
	 * @throws DataException
	 */
	private void clearTemporaryComputedColumns( ComputedColumnsState iccState, StopSign stopSign )
			throws DataException
	{
		if( !psController.needDoOperation( PassStatusController.RESULT_SET_TEMP_COMPUTED_COLUMN_POPULATING ) )
			return;
		iccState.setModel( TransformationConstants.ALL_MODEL );
		populator.getExpressionProcessor( ).clear( );

		computedColumnHelper.setModel( TransformationConstants.NONE_MODEL );

		//computedColumnHelper.getComputedColumnList( ).clear( );

		// restore computed column helper to its original state. by call this
		// method the computedColumnHelper only contain user defined computed
		// columns
		// and all temporary computed columns are exclued.
		//restoreComputedColumns( iccState, computedColumnHelper );

		cleanTempColumns( stopSign );
	}

	/**
	 * Clean the temporary data.
	 * 
	 * @param stopSign
	 * @throws DataException
	 */
	private void cleanTempColumns( StopSign stopSign ) throws DataException
	{
		IResultClass newMeta = rebuildResultClass( populator.getResultSetMetadata( ) );
		populator.setResultSetMetadata( newMeta );
		populator.getCache( ).setResultClass( newMeta );
		PassUtil.pass( populator,
				new OdiResultSetWrapper( populator.getResultIterator( ) ),
				false, stopSign );

		populator.getCache( ).reset( );
		populator.getCache( ).next( );
		populator.getGroupProcessorManager( )
				.getGroupCalculationUtil( )
				.getGroupInformationUtil( )
				.setLeaveGroupIndex( 0 );
	}

	/**
	 * Build an IResultClass instance excluding temp computed columns.
	 * 
	 * @param meta
	 * @return
	 * @throws DataException
	 */
	private static IResultClass rebuildResultClass( IResultClass meta )
			throws DataException
	{
		List projectedColumns = new ArrayList( );

		for ( int i = 1; i <= meta.getFieldCount( ); i++ )
		{
			if ( !PassUtil.isTemporaryResultSetComputedColumn( meta.getFieldName( i ) ) )
			{
				ResultFieldMetadata field = new ResultFieldMetadata( 0,
						meta.getFieldName( i ),
						meta.getFieldLabel( i ),
						meta.getFieldValueClass( i ),
						meta.getFieldNativeTypeName( i ),
						meta.isCustomField( i ) );
				field.setAlias( meta.getFieldAlias( i ) );

				projectedColumns.add( field );
			}
		}
		IResultClass result = new ResultClass( projectedColumns );
		return result;
	}
}
