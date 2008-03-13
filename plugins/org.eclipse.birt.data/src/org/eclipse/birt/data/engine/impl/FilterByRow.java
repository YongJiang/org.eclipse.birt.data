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

package org.eclipse.birt.data.engine.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.birt.core.data.DataTypeUtil;
import org.eclipse.birt.core.exception.BirtException;
import org.eclipse.birt.data.engine.api.IBaseExpression;
import org.eclipse.birt.data.engine.api.IConditionalExpression;
import org.eclipse.birt.data.engine.api.IFilterDefinition;
import org.eclipse.birt.data.engine.api.IScriptExpression;
import org.eclipse.birt.data.engine.core.DataException;
import org.eclipse.birt.data.engine.expression.ExprEvaluateUtil;
import org.eclipse.birt.data.engine.i18n.ResourceConstants;
import org.eclipse.birt.data.engine.odi.FilterUtil;
import org.eclipse.birt.data.engine.odi.IResultObject;
import org.eclipse.birt.data.engine.odi.IResultObjectEvent;

/**
 * Implementation of IFilter, which will do filtering on row data.
 */
public class FilterByRow implements IResultObjectEvent
{

	//
	public static final int DATASET_FILTER = 1;
	public static final int QUERY_FILTER = 2;
	public static final int ALL_ROW_FILTER = 3;
	public static final int NO_FILTER = 4;
	public static final int GROUP_FILTER = 5;
	public static final int AGGR_FILTER = 6;
	
	//
	private DataSetRuntime dataSet;
	private List currentFilters;
	private List dataSetFilters;
	private List queryFilters;
	private List groupFilters;
	private List allRowFilters;
	private List aggrFilters;
	
	private int currentWorkingFilters;

	protected static Logger logger = Logger.getLogger( FilterByRow.class.getName( ) );

	/**
	 * 
	 * @param dataSetFilters
	 * @param queryFilters
	 * @param dataSet
	 * @throws DataException
	 */
	FilterByRow( List dataSetFilters, List queryFilters, List groupFilters, List aggrFilters,
			DataSetRuntime dataSet ) throws DataException
	{
		Object[] params = {
				dataSetFilters, queryFilters, groupFilters, dataSet
		};
		logger.entering( FilterByRow.class.getName( ), "FilterByRow", params );

		this.dataSet = dataSet;

		this.dataSetFilters = FilterUtil.sortFilters( dataSetFilters );
		this.queryFilters = FilterUtil.sortFilters( queryFilters );
		this.groupFilters = groupFilters;
		this.allRowFilters = getAllRowFilters( dataSetFilters, queryFilters );
		this.aggrFilters = aggrFilters;
		this.currentWorkingFilters = ALL_ROW_FILTER;

		logger.exiting( FilterByRow.class.getName( ), "FilterByRow" );
		logger.log( Level.FINER, "FilterByRow starts up" );
	}

	/**
	 * @param dataSetFilters
	 * @param queryFilters
	 */
	private List getAllRowFilters( List dataSetFilters, List queryFilters )
	{
		//When the all filters need to be processed at same time,that is, no multi-pass filters exists,
		//the order of filters becomes not important.
		List temp = new ArrayList( );
		temp.addAll( dataSetFilters );
		temp.addAll( queryFilters );
		return temp;
	}

	/**
	 * Set the working filter set. The working filter set might be one of followings:
	 * 1. ALL_FILTER
	 * 2. DATASET_FILTER
	 * 3. QUERY_FILTER
	 * 4. NO_FILTER
	 * 5. GROUP_FILTER
	 * 
	 * @param filterSetType
	 * @throws DataException
	 */
	public void setWorkingFilterSet( int filterSetType ) throws DataException
	{
		this.validateFilterType( filterSetType );
		this.currentWorkingFilters = filterSetType;
	}

	/**
	 * Return the working filter set.
	 * 
	 * @throws DataException
	 */
	public int getWorkingFilterSet( ) throws DataException
	{
		return this.currentWorkingFilters;
	}

	/**
	 * Reset the current working filter set to the default value.
	 *
	 */
	public void restoreWorkingFilterSet( )
	{
		this.currentWorkingFilters = ALL_ROW_FILTER;
	}

	/**
	 * 
	 * @param filterSetType
	 * @return
	 * @throws DataException
	 */
	public boolean isFilterSetExist( int filterSetType ) throws DataException
	{
		this.validateFilterType( filterSetType );
		if ( DATASET_FILTER == filterSetType )
		{
			return this.dataSetFilters.size( ) > 0;
		}
		else if ( QUERY_FILTER == filterSetType )
		{
			return this.queryFilters.size( ) > 0;
		}
		else if ( GROUP_FILTER == filterSetType )
		{
			return this.groupFilters.size( ) > 0;
		}
		else if ( AGGR_FILTER == filterSetType )
		{
			return this.aggrFilters.size( ) > 0;
		}
		else
		{
			return this.allRowFilters.size( ) > 0;
		}
	}

	/*
	 * @see org.eclipse.birt.data.engine.odi.IResultObjectEvent#process(org.eclipse.birt.data.engine.odi.IResultObject)
	 */
	public boolean process( IResultObject row, int rowIndex )
			throws DataException
	{
		logger.entering( FilterByRow.class.getName( ), "process" );
		boolean isAccepted = true;
		this.currentFilters = this.getFilterList( currentWorkingFilters );
		Iterator filterIt = currentFilters.iterator( );
		dataSet.setRowObject( row, false );
		dataSet.setCurrentRowIndex( rowIndex );

		while ( filterIt.hasNext( ) )
		{
			IFilterDefinition filter = (IFilterDefinition) filterIt.next( );
			IBaseExpression expr = filter.getExpression( );

			Object result = null;
			try
			{
				/*if ( helper!= null)
				 result = helper.evaluate( expr );
				 else
				 result = ScriptEvalUtil.evalExpr( expr, cx,dataSet.getScriptScope(), "Filter", 0 ); */
				result = ExprEvaluateUtil.evaluateRawExpression2( expr,
						dataSet.getScriptScope( ) );
			}
			catch ( BirtException e2 )
			{
				DataException dataEx = DataException.wrap( e2 );

				Object info = null;
				if ( expr instanceof IConditionalExpression )
					info = ( (IConditionalExpression) expr ).getExpression( )
							.getText( );
				else
					info = expr;

				throw new DataException( ResourceConstants.INVALID_DEFINITION_IN_FILTER,
						dataEx,
						info );
			}

			if ( result == null )
			{
				Object info = null;
				if ( expr instanceof IScriptExpression )
					info = ( (IScriptExpression) expr ).getText( );
				else
					info = expr;
				throw new DataException( ResourceConstants.INVALID_EXPRESSION_IN_FILTER,
						info );
			}

			try
			{
				// filter in
				if ( DataTypeUtil.toBoolean( result ).booleanValue( ) == false )
				{
					isAccepted = false;
					break;
				}
			}
			catch ( BirtException e )
			{
				DataException e1 = new DataException( ResourceConstants.DATATYPEUTIL_ERROR,
						e );
				logger.logp( Level.FINE,
						FilterByRow.class.getName( ),
						"process",
						"An error is thrown by DataTypeUtil.",
						e1 );
				throw e1;
			}
		}
		return isAccepted;

	}

	/**
	 * Get the current working filter list.
	 * 
	 * @return
	 * @throws DataException
	 */
	public List getFilterList( ) throws DataException
	{
		return this.getFilterList( this.currentWorkingFilters );
	}

	/**
	 * Get the filter list according to the given filter set type.
	 * 
	 * @param filterSetType
	 * @return
	 * @throws DataException
	 */
	public List getFilterList( int filterSetType ) throws DataException
	{
		validateFilterType( filterSetType );
		if ( DATASET_FILTER == filterSetType )
		{
			return this.dataSetFilters;
		}
		else if ( QUERY_FILTER == filterSetType )
		{
			return this.queryFilters;
		}
		else if ( GROUP_FILTER == filterSetType )
		{
			return this.groupFilters;
		}
		else if ( ALL_ROW_FILTER == filterSetType )
		{
			return this.allRowFilters;
		}
		else if ( AGGR_FILTER == filterSetType )
		{
			return this.aggrFilters;
		}
		else
		{
			return new ArrayList( );
		}
	}

	/**
	 * 
	 * @param filterSetType
	 */
	private void validateFilterType( int filterSetType )
	{
		if ( filterSetType != NO_FILTER
				&& filterSetType != DATASET_FILTER
				&& filterSetType != ALL_ROW_FILTER
				&& filterSetType != QUERY_FILTER
				&& filterSetType != GROUP_FILTER
				&& filterSetType != AGGR_FILTER )
		{
			assert false;
		}
	}

}
