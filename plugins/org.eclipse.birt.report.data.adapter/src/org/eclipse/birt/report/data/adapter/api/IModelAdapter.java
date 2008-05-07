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

package org.eclipse.birt.report.data.adapter.api;

import org.eclipse.birt.core.exception.BirtException;
import org.eclipse.birt.data.engine.api.IBinding;
import org.eclipse.birt.data.engine.api.querydefn.BaseDataSetDesign;
import org.eclipse.birt.data.engine.api.querydefn.BaseDataSourceDesign;
import org.eclipse.birt.data.engine.api.querydefn.ColumnDefinition;
import org.eclipse.birt.data.engine.api.querydefn.ComputedColumn;
import org.eclipse.birt.data.engine.api.querydefn.ConditionalExpression;
import org.eclipse.birt.data.engine.api.querydefn.FilterDefinition;
import org.eclipse.birt.data.engine.api.querydefn.GroupDefinition;
import org.eclipse.birt.data.engine.api.querydefn.InputParameterBinding;
import org.eclipse.birt.data.engine.api.querydefn.ParameterDefinition;
import org.eclipse.birt.data.engine.api.querydefn.ScriptExpression;
import org.eclipse.birt.data.engine.api.querydefn.SortDefinition;
import org.eclipse.birt.report.model.api.ComputedColumnHandle;
import org.eclipse.birt.report.model.api.DataSetHandle;
import org.eclipse.birt.report.model.api.DataSetParameterHandle;
import org.eclipse.birt.report.model.api.DataSourceHandle;
import org.eclipse.birt.report.model.api.FilterConditionHandle;
import org.eclipse.birt.report.model.api.GroupHandle;
import org.eclipse.birt.report.model.api.ParamBindingHandle;
import org.eclipse.birt.report.model.api.ResultSetColumnHandle;
import org.eclipse.birt.report.model.api.SortKeyHandle;

/**
 * 
 */

public interface IModelAdapter
{

	/**
	 * Adapts a Model data source handle to an equivalent BaseDataSourceDesign.
	 */
	public BaseDataSourceDesign adaptDataSource( DataSourceHandle handle )
			throws BirtException;

	/**
	 * Adapts a Model data set handle to an equivalent BaseDataSetDesign.
	 */
	public BaseDataSetDesign adaptDataSet( DataSetHandle handle )
			throws BirtException;

	/**
	 * Adapts a Model parameter definition
	 */
	public ParameterDefinition adaptParameter(
			DataSetParameterHandle paramHandle );

	/**
	 * Adapts a Model input parameter binding definition
	 */
	public InputParameterBinding adaptInputParamBinding(
			ParamBindingHandle modelHandle );

	/**
	 * Adapts a Model column definition
	 */
	public ColumnDefinition ColumnAdaptor( ResultSetColumnHandle modelColumn );

	/**
	 * Adapts a Model computed column definition
	 * @throws AdapterException 
	 * 
	 */
	public ComputedColumn adaptComputedColumn( ComputedColumnHandle modelHandle ) throws AdapterException;

	/**
	 * Adapts a Model condition
	 * 
	 * @param mainExpr
	 *            Main expression; must not be null
	 * @param operator
	 *            Operator
	 */
	public ConditionalExpression adaptConditionalExpression( String mainExpr,
			String operator, String operand1, String operand2 );

	/**
	 * Constructs an expression with provided text and return data type Data
	 * type is defined as a Model data type string
	 */
	public ScriptExpression adaptExpression( String exprText, String dataType );

	/**
	 * Adapts an expression based on Model computed column handle
	 */
	public ScriptExpression adaptExpression( ComputedColumnHandle ccHandle );

	/**
	 * Adapts a model filter handle
	 */
	public FilterDefinition adaptFilter( FilterConditionHandle modelFilter );

	/**
	 * Adapts a model Group handle
	 */
	public GroupDefinition adaptGroup( GroupHandle groupHandle );

	/**
	 * Adapts a model Sort handle
	 */
	public SortDefinition adaptSort( SortKeyHandle sortHandle );

	/**
	 * Adapts a model Sort based on a sort key expression and a Model sort
	 * direction string
	 */
	public SortDefinition adaptSort( String sortKeyExpr, String direction );

	/**
	 * Adapt a model computed column handle to an IBinding instance.
	 * @param handle
	 * @return
	 * @throws AdapterException 
	 */
	public IBinding adaptBinding( ComputedColumnHandle handle ) throws AdapterException;
}