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
import org.eclipse.birt.data.engine.impl.DataSetRuntime.Mode;
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
	private FilterByRowHelper currentFilters;
	private FilterByRowHelper dataSetFilters;
	private FilterByRowHelper queryFilters;
	private FilterByRowHelper groupFilters;
	private FilterByRowHelper allRowFilters;
	private FilterByRowHelper aggrFilters;

	protected static Logger logger = Logger.getLogger( FilterByRow.class.getName( ) );

	/**
	 * 
	 * @param dataSetFilters
	 * @param queryFilters
	 * @param dataSet
	 * @throws DataException
	 */
	FilterByRow( List dataSetFilters, List queryFilters, List groupFilters,
			List aggrFilters, DataSetRuntime dataSet ) throws DataException
	{
		Object[] params = {
				dataSetFilters, queryFilters, groupFilters, dataSet
		};
		logger.entering( FilterByRow.class.getName( ), "FilterByRow", params );

		this.dataSet = dataSet;

		if ( dataSetFilters != null && dataSetFilters.size( ) > 0 )
			this.dataSetFilters = new FilterByRowHelper( dataSet,
					Mode.DataSet,
					FilterUtil.sortFilters( dataSetFilters ) );
		if ( queryFilters != null && queryFilters.size( ) > 0 )
			this.queryFilters = new FilterByRowHelper( dataSet,
					Mode.Query,
					FilterUtil.sortFilters( queryFilters ) );
		if ( groupFilters != null && groupFilters.size( ) > 0 )
			this.groupFilters = new FilterByRowHelper( dataSet,
					Mode.Query,
					groupFilters );
		if ( this.dataSetFilters != null || this.queryFilters != null )
			this.allRowFilters = new FilterByRowHelper( dataSet,
					Mode.DataSet,
					getAllRowFilters( dataSetFilters, queryFilters ) );
		if ( aggrFilters != null )
			this.aggrFilters = new FilterByRowHelper( dataSet,
					Mode.Query,
					aggrFilters );
		this.currentFilters = this.allRowFilters;

		logger.exiting( FilterByRow.class.getName( ), "FilterByRow" );
		logger.log( Level.FINER, "FilterByRow starts up" );
	}

	/**
	 * @param dataSetFilters
	 * @param queryFilters
	 */
	private List getAllRowFilters( List dataSetFilters, List queryFilters )
	{
		// When the all filters need to be processed at same time,that is, no
		// multi-pass filters exists,
		// the order of filters becomes not important.
		List temp = new ArrayList( );
		temp.addAll( dataSetFilters );
		temp.addAll( queryFilters );
		return temp;
	}

	/**
	 * Set the working filter set. The working filter set might be one of
	 * followings: 1. ALL_FILTER 2. DATASET_FILTER 3. QUERY_FILTER 4. NO_FILTER
	 * 5. GROUP_FILTER
	 * 
	 * @param filterSetType
	 * @throws DataException
	 */
	public void setWorkingFilterSet( int filterSetType ) throws DataException
	{
		this.validateFilterType( filterSetType );
		switch ( filterSetType )
		{
			case DATASET_FILTER :
				this.currentFilters = this.dataSetFilters;
				break;
			case QUERY_FILTER :
				this.currentFilters = this.queryFilters;
				break;
			case ALL_ROW_FILTER :
				this.currentFilters = this.allRowFilters;
				break;
			case GROUP_FILTER :
				this.currentFilters = this.groupFilters;
				break;
			case AGGR_FILTER :
				this.currentFilters = this.aggrFilters;
				break;
			default :
				this.currentFilters = null;
		}
	}

	/**
	 * Reset the current working filter set to the default value.
	 * 
	 */
	public void restoreWorkingFilterSet( )
	{
		this.currentFilters = this.allRowFilters;
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
			return this.dataSetFilters != null;
		}
		else if ( QUERY_FILTER == filterSetType )
		{
			return this.queryFilters != null;
		}
		else if ( GROUP_FILTER == filterSetType )
		{
			return this.groupFilters != null;
		}
		else if ( AGGR_FILTER == filterSetType )
		{
			return this.aggrFilters != null;
		}
		else
		{
			return this.allRowFilters != null;
		}
	}

	/*
	 * @see org.eclipse.birt.data.engine.odi.IResultObjectEvent#process(org.eclipse.birt.data.engine.odi.IResultObject)
	 */
	public boolean process( IResultObject row, int rowIndex )
			throws DataException
	{
		if ( this.currentFilters != null )
			return this.currentFilters.process( row, rowIndex );
		return true;
	}

	/**
	 * Get the current working filter list.
	 * 
	 * @return
	 * @throws DataException
	 */
	public List getFilterList( ) throws DataException
	{
		if ( currentFilters != null )
			return this.currentFilters.getFilters( );
		return new ArrayList( );
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
		switch ( filterSetType )
		{
			case DATASET_FILTER :
				return this.dataSetFilters != null
						? this.dataSetFilters.getFilters( ) : new ArrayList( );
			case QUERY_FILTER :
				return this.queryFilters != null
						? this.queryFilters.getFilters( ) : new ArrayList( );
			case ALL_ROW_FILTER :
				return this.allRowFilters != null
						? this.allRowFilters.getFilters( ) : new ArrayList( );
			case GROUP_FILTER :
				return this.groupFilters != null
						? this.groupFilters.getFilters( ) : new ArrayList( );
			case AGGR_FILTER :
				return this.aggrFilters != null ? this.aggrFilters.getFilters( )
						: new ArrayList( );
			default :
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

	private class FilterByRowHelper
	{

		private DataSetRuntime dataSet;
		private List currentFilters;
		private Mode mode;

		FilterByRowHelper( DataSetRuntime dataSet, Mode mode, List filters )
		{
			this.dataSet = dataSet;
			this.currentFilters = filters;
			this.mode = mode;
		}

		public List getFilters( )
		{
			return this.currentFilters;
		}

		public boolean process( IResultObject row, int rowIndex )
				throws DataException
		{
			logger.entering( FilterByRow.class.getName( ), "process" );
			boolean isAccepted = true;
			Iterator filterIt = currentFilters.iterator( );
			dataSet.setRowObject( row, false );
			dataSet.setCurrentRowIndex( rowIndex );
			Mode temp = dataSet.getMode( );
			dataSet.setMode( this.mode );
			try
			{
				while ( filterIt.hasNext( ) )
				{
					IFilterDefinition filter = (IFilterDefinition) filterIt.next( );
					IBaseExpression expr = filter.getExpression( );

					Object result = null;
					try
					{
						/*
						 * if ( helper!= null) result = helper.evaluate( expr );
						 * else result = ScriptEvalUtil.evalExpr( expr,
						 * cx,dataSet.getScriptScope(), "Filter", 0 );
						 */
						result = ExprEvaluateUtil.evaluateRawExpression2( expr,
								dataSet.getScriptScope( ), dataSet.getSession( ).getEngineContext( ).getScriptContext( ) );
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
			finally
			{
				dataSet.setMode( temp );
			}
		}

	}
}
