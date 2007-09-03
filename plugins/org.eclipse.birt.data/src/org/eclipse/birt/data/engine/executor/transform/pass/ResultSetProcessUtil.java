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
import org.eclipse.birt.data.engine.executor.transform.IExpressionProcessor;
import org.eclipse.birt.data.engine.executor.transform.OdiResultSetWrapper;
import org.eclipse.birt.data.engine.executor.transform.ResultSetPopulator;
import org.eclipse.birt.data.engine.executor.transform.TransformationConstants;
import org.eclipse.birt.data.engine.impl.ComputedColumnHelper;
import org.eclipse.birt.data.engine.impl.DataEngineSession;
import org.eclipse.birt.data.engine.impl.FilterByRow;
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
			PassStatusController psController, DataEngineSession session )
	{
		super( populator,
				iccState,
				computedColumnHelper,
				filterByRow,
				psController, session );
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
			PassStatusController psController, List sortList, DataEngineSession session )
			throws DataException
	{
		ResultSetProcessUtil instance = new ResultSetProcessUtil( populator,
				iccState,
				computedColumnHelper,
				filterByRow,
				psController, session );
		instance.cachedSort = sortList;
		instance.populateResultSet( );

	}

	/**
	 * 
	 * @throws DataException
	 */
	private void populateResultSet( ) throws DataException
	{
		//The computed columns that need multipass
		List aggCCList = prepareComputedColumns( TransformationConstants.RESULT_SET_MODEL );
		
		//Grouping will also be done in this method, for currently we only support simple group keys
		//that is, group keys cannot contain aggregation.
				
		doRowFiltering( );
		
		//TODO remove me
		populateTempComputedColumns( this.getAggrComputedColumns( aggCCList, false ) );
		///////////////
		
		List aggrDefns = this.populator.getEventHandler( ).getAggrDefinitions( );
		
		prepareAggregations( aggrDefns );
		
		//Filter group instances.
		doGroupFiltering( );

		//Do row sorting
		doRowSorting( );
		
		//Do group sorting
		doGroupSorting( );
		
		clearTemporaryComputedColumns( iccState );
	}

	/**
	 * 
	 * @param aggrDefns
	 * @throws DataException
	 */
	private void prepareAggregations( List aggrDefns ) throws DataException
	{
		boolean needGroupFiltering = this.needDoGroupFiltering( );
		boolean needGroupSorting = this.needDoGroupSorting( );
		
		if ( needPreCalculateForGroupFilterSort( needGroupFiltering,
				needGroupSorting ) )
		{
			if ( !groupingDone )
			{
				PassUtil.pass( this.populator,
						new OdiResultSetWrapper( populator.getResultIterator( ) ),
						true,
						session );
			}
			this.populator.getExpressionProcessor( )
					.setResultIterator( this.populator.getResultIterator( ) );
			AggrDefnRoundManager factory = new AggrDefnRoundManager( aggrDefns );
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
	 * @throws DataException
	 */
	private void populateTempComputedColumns( List aggCCList ) throws DataException
	{
		if ( psController.needDoOperation( PassStatusController.RESULT_SET_TEMP_COMPUTED_COLUMN_POPULATING ) )
		{
			if ( aggCCList.size( ) != 0
					|| psController.needDoOperation( PassStatusController.GROUP_ROW_FILTERING ) )
			{
				PassUtil.pass( this.populator,
						new OdiResultSetWrapper( populator.getResultIterator( ) ),
						true,
						session );
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

			doGroupRowFilter( );
		}
	}

	/**
	 * 
	 * @throws DataException
	 */
	private void doGroupSorting( ) throws DataException
	{
		if( !groupingDone )
		{
			PassUtil.pass( this.populator,
					new OdiResultSetWrapper( populator.getResultIterator( ) ),
					true, session );
			groupingDone = true;
		}
		if ( this.populator.getQuery( ).getGrouping( ) != null
				&& this.populator.getQuery( ).getGrouping( ).length > 0 )
		{
			this.populator.getGroupProcessorManager( )
					.doGroupSorting( this.populator.getCache( ),
							this.populator.getExpressionProcessor( ) );
		}
	}

	/**
	 * 
	 * @throws DataException
	 */
	private void doRowSorting( ) throws DataException
	{
		this.populator.getQuery( ).setOrdering( this.cachedSort );
		
		if ( this.populator.getGroupProcessorManager( )
				.getGroupCalculationUtil( )
				.getSortSpec( ) != null
				&& this.populator.getGroupProcessorManager( )
						.getGroupCalculationUtil( )
						.getSortSpec( )
						.length( ) > 0 )
		{
			PassUtil.pass( this.populator,
					new OdiResultSetWrapper( populator.getResultIterator( ) ),
					true,
					session );
			this.groupingDone = true;
		}
	}

	/**
	 * 
	 * @throws DataException
	 */
	private void doGroupFiltering( ) throws DataException
	{
		if ( ! groupingDone )
		{
			PassUtil.pass( this.populator,
					new OdiResultSetWrapper( populator.getResultIterator( ) ),
					true, session );
			groupingDone = true;
		}
		if ( this.populator.getQuery( ).getGrouping( ) != null
				&& this.populator.getQuery( ).getGrouping( ).length > 0 )
		{
			this.populator.getGroupProcessorManager( )
					.doGroupFiltering( this.populator.getCache( ),
							this.populator.getExpressionProcessor( ) );
		}
	}

	/**
	 * 
	 * @throws DataException
	 */
	private void doRowFiltering( ) throws DataException
	{
		if(!psController.needDoOperation( PassStatusController.RESULT_SET_FILTERING ))
			return;
		
		boolean changeMaxRows = filterByRow.getFilterList( FilterByRow.GROUP_FILTER )
				.size( ) > 0;
		applyFilters( FilterByRow.QUERY_FILTER, changeMaxRows );
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
	 * 
	 * @throws DataException
	 */
	private void doGroupRowFilter( ) throws DataException
	{
		if ( !psController.needDoOperation( PassStatusController.GROUP_ROW_FILTERING ) )
			return;
		// Apply group row filters (Total.isTopN, Total.isBottomN..)
		filterByRow.setWorkingFilterSet( FilterByRow.GROUP_FILTER );
		PassUtil.pass( this.populator,
				new OdiResultSetWrapper( populator.getResultIterator( ) ), true, session );

		filterByRow.setWorkingFilterSet( FilterByRow.NO_FILTER );

	}
	
	/**
	 * 
	 * @param iccState
	 * @throws DataException
	 */
	private void clearTemporaryComputedColumns( ComputedColumnsState iccState )
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

		cleanTempColumns( );
	}

	/**
	 * Clean the temporary data.
	 * 
	 * @throws DataException
	 */
	private void cleanTempColumns( ) throws DataException
	{
		IResultClass newMeta = rebuildResultClass( populator.getResultSetMetadata( ) );
		populator.setResultSetMetadata( newMeta );
		populator.getCache( ).setResultClass( newMeta );
		PassUtil.pass( populator,
				new OdiResultSetWrapper( populator.getResultIterator( ) ),
				false, session );

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
