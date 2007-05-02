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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.eclipse.birt.core.data.DataTypeUtil;
import org.eclipse.birt.core.exception.BirtException;
import org.eclipse.birt.data.engine.api.IBaseExpression;
import org.eclipse.birt.data.engine.core.DataException;
import org.eclipse.birt.data.engine.expression.ExprEvaluateUtil;
import org.eclipse.birt.data.engine.impl.ResultIterator.RDSaveHelper;
import org.eclipse.birt.data.engine.impl.document.viewing.ExprMetaUtil;
import org.eclipse.birt.data.engine.odi.IResultClass;
import org.eclipse.birt.data.engine.odi.IResultIterator;
import org.mozilla.javascript.Scriptable;

/**
 * Evaluate a row of bound columns and meantime do something related with saving
 * the value of bound columns.
 * 
 * There is different behavior when query is running on dataset or report
 * document. In the latter case, original binding column name is reserved and
 * only the binding column can be added, not allowed for delete or change. So it
 * is reasonable to assume that if the binding name is the same as one of
 * original binding columns, the binding expression is also the same as that of
 * the orignial one.
 */
class BindingColumnsEvalUtil
{
	// 
	private IResultIterator odiResult;
	private Scriptable scope;
	private RDSaveHelper saveHelper;

	private List allManualBindingExprs;
	private List allAutoBindingExprs;

	private boolean isBasedOnRD;
	private EvalHelper evalHelper;
	
	private final static int MANUAL_BINDING = 1;
	private final static int AUTO_BINDING = 2;

	/**
	 * @param ri
	 * @param scope
	 * @param saveUtil
	 * @param serviceForResultSet
	 * @throws DataException 
	 */
	BindingColumnsEvalUtil( IResultIterator ri, Scriptable scope,
			RDSaveHelper saveUtil, List manualBindingExprs, Map autoBindingExprs ) throws DataException
	{
		this.odiResult = ri;
		this.scope = scope;
		this.saveHelper = saveUtil;
		
		try
		{
			this.isBasedOnRD = ExprMetaUtil.isBasedOnRD( ri.getResultClass( ) );
			if ( this.isBasedOnRD == true )
				this.evalHelper = new EvalHelper( ri );
		}
		catch ( DataException e )
		{
			// ignore, impossible
		}

		this.initBindingColumns( manualBindingExprs, autoBindingExprs );
	}

	/**
	 * @param serviceForResultSet
	 * @throws DataException 
	 */
	private void initBindingColumns( List manualBindingExprs,
			Map autoBindingExprs ) throws DataException
	{
		// put the expressions of array into a list
		int size = manualBindingExprs.size( );
		GroupBindingColumn[] groupBindingColumns = new GroupBindingColumn[size];
		Iterator itr = manualBindingExprs.iterator( );
		while ( itr.hasNext( ) )
		{
			GroupBindingColumn temp = (GroupBindingColumn) itr.next( );
			groupBindingColumns[temp.getGroupLevel( )] = temp;
		}

		allManualBindingExprs = new ArrayList( );
		for ( int i = 0; i < size; i++ )
		{
			List groupBindingExprs = new ArrayList( );
			itr = groupBindingColumns[i].getColumnNames( ).iterator( );
			while ( itr.hasNext( ) )
			{
				String exprName = (String) itr.next( );
				IBaseExpression baseExpr = groupBindingColumns[i].getExpression( exprName );
				groupBindingExprs.add( new BindingColumn( exprName,
						baseExpr,
						groupBindingColumns[i].getBinding( exprName )
								.getAggrFunction( ) != null,
						groupBindingColumns[i].getBinding( exprName )
								.getDataType( ) ) );
			}

			allManualBindingExprs.add( groupBindingExprs );
		}
		
		// put the auto binding expressions into a list
		allAutoBindingExprs = new ArrayList( );
		itr = autoBindingExprs.entrySet( ).iterator( );
		while ( itr.hasNext( ) )
		{
			Map.Entry entry = (Entry) itr.next( );
			String exprName = (String) entry.getKey( );
			IBaseExpression baseExpr = (IBaseExpression) entry.getValue( );
			
			allAutoBindingExprs.add( new BindingColumn( exprName, baseExpr, false, baseExpr.getDataType( ) ) );
		}
	}

	/**
	 * @return
	 * @throws DataException
	 *             save error
	 */
	Map getColumnsValue( ) throws DataException
	{
		Map exprValueMap = new HashMap( );

		for ( int i = 0; i < allManualBindingExprs.size( ); i++ )
		{
			List list = (List) allManualBindingExprs.get( i );
			Iterator it = list.iterator( );
			while ( it.hasNext( ) )
			{
				BindingColumn bindingColumn = (BindingColumn) it.next( );
				Object exprValue = evaluateValue( bindingColumn, MANUAL_BINDING );
				exprValueMap.put( bindingColumn.columnName, exprValue );
			}
		}

		Iterator itr = this.allAutoBindingExprs.iterator( );
		while ( itr.hasNext( ) )
		{
			BindingColumn bindingColumn = (BindingColumn) itr.next( );
			Object exprValue = evaluateValue( bindingColumn, AUTO_BINDING );
			if ( exprValueMap.get( bindingColumn.columnName ) == null )
				exprValueMap.put( bindingColumn.columnName, exprValue );
		}

		saveHelper.doSaveExpr( exprValueMap );
		return exprValueMap;
	}

	/**
	 * @param baseExpr
	 * @param exprType
	 * @param valueMap
	 * @throws DataException
	 */
	private Object evaluateValue( BindingColumn bindingColumn, int exprType )
			throws DataException
	{
		Object exprValue = null;
		try
		{
			boolean getValue = false;
			if ( isBasedOnRD == true )
			{
				String columnName = bindingColumn.columnName;
				if ( evalHelper.contains( columnName ) )
				{
					getValue = true;
					exprValue = evalHelper.getValue( columnName );
				}
			}
			
			if ( getValue == false )
			{
				if ( exprType == MANUAL_BINDING )
				{	
					if ( bindingColumn.isAggregation )
						exprValue = this.odiResult.getAggrValue( bindingColumn.columnName );
					else
						exprValue = ExprEvaluateUtil.evaluateExpression( bindingColumn.baseExpr,
							odiResult,
							scope );
				}
				else
					exprValue = ExprEvaluateUtil.evaluateRawExpression( bindingColumn.baseExpr,
							scope );
				
				if ( exprValue!= null )
					exprValue = DataTypeUtil.convert( exprValue, bindingColumn.type );
			}
		}
		catch ( BirtException e )
		{
			exprValue = e;
		}
		
		return exprValue;
	}
	
	/**
	 * A simple wrapper for binding column
	 */
	private class BindingColumn
	{

		//
		private String columnName;
		private IBaseExpression baseExpr;
		private boolean isAggregation;
		private int type;
		
		private BindingColumn( String columnName, IBaseExpression baseExpr, boolean isAggregation, int type )
		{
			this.columnName = columnName;
			this.baseExpr = baseExpr;
			this.isAggregation = isAggregation;
			this.type = type;
		}
	}
	
	/**
	 * Special case for query running based on report document. A life cycle
	 * might be below:
	 * 
	 * 1: create an original report design
	 * 
	 * 2: running this design and save the result into a report document
	 * 
	 * 3: only modify the transformation of the report design 1 and continue
	 * running the new report design based on the report document of step 2
	 * 
	 * 4: the column binding of the original report design can also used in the
	 * new query result of step 3
	 */
	private class EvalHelper
	{
		private IResultIterator ri;
		private Set columnNameSet;
		
		/**
		 * @param ri
		 */
		private EvalHelper( IResultIterator ri )
		{
			this.ri = ri;
			try
			{
				IResultClass resultClass = ri.getResultClass( );
				
				columnNameSet = new HashSet( );
				for ( int i = 0; i < resultClass.getFieldCount( ); i++ )
					columnNameSet.add( resultClass.getFieldName( i + 1 ) );
			}
			catch ( DataException e )
			{
				// impossible
			}
		}
		
		/**
		 * @param columnName
		 * @return
		 */
		private boolean contains( String columnName )
		{
			return columnNameSet.contains( columnName );
		}
		
		/**
		 * @param columnName
		 * @return
		 * @throws DataException
		 */
		private Object getValue( String columnName ) throws DataException
		{
			return ri.getCurrentResult( ).getFieldValue( columnName );
		}
	}

}
