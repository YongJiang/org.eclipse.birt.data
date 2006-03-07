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

package org.eclipse.birt.data.engine.executor.transform;

import java.util.List;

import org.eclipse.birt.data.engine.core.DataException;
import org.eclipse.birt.data.engine.executor.BaseQuery;
import org.eclipse.birt.data.engine.odi.IResultClass;
import org.eclipse.birt.data.engine.odi.IResultIterator;
import org.mozilla.javascript.Scriptable;

/**
 * The instance of this interface handles the compilation of ROM JavaScript
 * expressions of the multiple pass level Each expression is compiled to
 * generate a handle, which is an instance of CompiledExpression or its derived
 * class. The expression handle is used by the factory to evaluate the
 * expression after the report query is executed. <br>
 * ExpressionProcessor compiles the expression into Rhino byte code for faster
 * evaluation at runtime.
 * 
 */
public interface IExpressionProcessor
{

	/**
	 * computed column expression
	 */
	public final static int COMPUTED_COLUMN_EXPR = 0;
	/**
	 * filter column expression
	 */
	public final static int FILTER_COLUMN_EXPR = 1;
	/**
	 * group expression
	 */
	public final static int GROUP_COLUMN_EXPR = 2;
	/**
	 * filter expression on group
	 */
	public final static int FILTER_ON_GROUP_EXPR = 3;
	/**
	 * sort expression on group
	 */
	public final static int SORT_ON_GROUP_EXPR = 4;

	/**
	 * Get the scope on which the expression is compiled
	 * 
	 * @return
	 */
	public Scriptable getScope( );

	/**
	 * Compile the compilable expression in the given IComputedColumnState
	 * instance
	 * 
	 * @param computedColumns
	 */
	public void compileComputedColumn( IComputedColumnsState computedColumns );

	/**
	 * Compile the filter expression. If the filter contains the indirect nested
	 * row reference, this is, it refers to the row object in computed columns,
	 * then replace the field of expression and recompile the filter expression
	 * 
	 * @param filterState
	 * @param computedColumns
	 * @throws DataException
	 */
	public void compileFilter( List filterState,
			IComputedColumnsState computedColumns );

	/**
	 * Calculate the aggregations.If the aggregate pass level is less than or
	 * equal to 1, the value of this aggregate is available. And the value will
	 * be register in aggregate table.If the pass level is more than 1, the
	 * value is not accessed. The calculation will push to next time.
	 * 
	 */
	public void calculate( ) throws DataException;

	/**
	 * Calculate the aggregation list on group.The aggragate type may be
	 * FILTER_ON_GROUP or SORT_ON_GROUP. groupLevel array contains every
	 * aggragate group level.It can be referred to the aggregate's rationality
	 * and calculation.
	 * 
	 */
	public void calculate( Object[] aggrList, int[] groupLevel, int type )
			throws DataException;

	/**
	 * whether the expression list contains aggregate, if yes, return true. else
	 * return false
	 * 
	 * @param list
	 * @return
	 */
	public boolean hasAggregateExpr( List list ) throws DataException;

	/**
	 * Set the result iterator against which the value of aggregations will be
	 * calculated.
	 * 
	 * @param it
	 */
	public void setResultIterator( IResultIterator it );

	/**
	 * Set the result iterator against which the value of aggregations will be
	 * calculated.
	 * 
	 * @param it
	 */
	public void setResultSetMetaData( IResultClass rsMetaData );

	/**
	 * Set the query to be used by ExpressionProcessor
	 * 
	 * @param query
	 */
	public void setQuery( BaseQuery query );

	/**
	 * Clear the temp variables when the IExpressionProcessor is no longer used.
	 * 
	 */
	public void clear( );
}