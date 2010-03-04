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

package org.eclipse.birt.report.data.adapter.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.birt.core.exception.BirtException;
import org.eclipse.birt.data.engine.api.DataEngine;
import org.eclipse.birt.data.engine.api.IBaseDataSourceDesign;
import org.eclipse.birt.data.engine.api.IBinding;
import org.eclipse.birt.data.engine.api.IComputedColumn;
import org.eclipse.birt.data.engine.api.IFilterDefinition;
import org.eclipse.birt.data.engine.api.IQueryDefinition;
import org.eclipse.birt.data.engine.api.IQueryResults;
import org.eclipse.birt.data.engine.api.querydefn.BaseDataSetDesign;
import org.eclipse.birt.data.engine.api.querydefn.Binding;
import org.eclipse.birt.data.engine.api.querydefn.ComputedColumn;
import org.eclipse.birt.data.engine.api.querydefn.InputParameterBinding;
import org.eclipse.birt.data.engine.api.querydefn.ScriptExpression;
import org.eclipse.birt.data.engine.core.DataException;
import org.eclipse.birt.data.engine.impl.DataEngineImpl;
import org.eclipse.birt.report.data.adapter.api.AdapterException;
import org.eclipse.birt.report.data.adapter.api.DataRequestSession;
import org.eclipse.birt.report.data.adapter.api.DataSessionContext;
import org.eclipse.birt.report.data.adapter.api.IModelAdapter;
import org.eclipse.birt.report.model.api.ComputedColumnHandle;
import org.eclipse.birt.report.model.api.DataSetHandle;
import org.eclipse.birt.report.model.api.DataSourceHandle;
import org.eclipse.birt.report.model.api.DerivedDataSetHandle;
import org.eclipse.birt.report.model.api.FilterConditionHandle;
import org.eclipse.birt.report.model.api.JointDataSetHandle;
import org.eclipse.birt.report.model.api.ModuleHandle;
import org.eclipse.birt.report.model.api.OdaDataSetParameterHandle;
import org.eclipse.birt.report.model.api.ParamBindingHandle;
import org.mozilla.javascript.Scriptable;

/**
 * 
 */
class QueryExecutionHelper
{

	//
	private DataEngine dataEngine;
	private IModelAdapter modelAdaptor;
	private DataSessionContext sessionContext;
	
	private boolean useResultHints;
	
	//The major data set handle this QueryExecutionHelper deal with.
	private DataSetHandle major;
	private DataRequestSession session;
	/**
	 * @param dataEngine
	 * @param modelAdaptor
	 * @param moduleHandle
	 */
	QueryExecutionHelper( DataEngine dataEngine, IModelAdapter modelAdaptor,
			DataSessionContext sessionContext, DataRequestSession session )
	{
		this( dataEngine, modelAdaptor, sessionContext, true, session );
	}
	
	/**
	 * @param dataEngine
	 * @param modelAdaptor
	 * @param moduleHandle
	 * @param useResultHints
	 */
	QueryExecutionHelper( DataEngine dataEngine, IModelAdapter modelAdaptor,
			DataSessionContext sessionContext, boolean useResultHints, DataRequestSession session )
	{
		this.dataEngine = dataEngine;
		this.modelAdaptor = modelAdaptor;
		this.sessionContext = sessionContext;
		this.useResultHints = useResultHints;
		this.session = session;
	}

	/**
	 * @param queryDefn
	 * @return
	 * @throws BirtException
	 */
	IQueryResults executeQuery( IQueryDefinition queryDefn )
			throws BirtException
	{
		return executeQuery( queryDefn, null, null, null, null );
	}
	
	/**
	 * 
	 * @param queryDefn
	 * @param paramBindingIt
	 * @param filterIt
	 * @param bindingIt
	 * @return
	 * @throws BirtException
	 */
	IQueryResults executeQuery( IQueryDefinition queryDefn,
			Iterator paramBindingIt, Iterator filterIt, Iterator bindingIt, Scriptable scope )
			throws BirtException
	{
		return executeQuery( queryDefn,
				paramBindingIt,
				filterIt,
				bindingIt,
				true,
				false,
				scope );
	}

	IQueryResults executeQuery( IQueryDefinition queryDefn,
			Iterator paramBindingIt, Iterator filterIt, Iterator bindingIt,
			boolean keepDataSetFilter, boolean disAllowAggregation, Scriptable scope ) throws BirtException
	{
		defineDataSourceDataSet( queryDefn, keepDataSetFilter, disAllowAggregation );

		populateQueryDefn( queryDefn, paramBindingIt, filterIt, bindingIt, disAllowAggregation );

		return dataEngine.prepare( queryDefn, sessionContext.getAppContext( ) )
				.execute( scope );
	}
	
	/**
	 * @param queryDefn
	 * @throws AdapterException
	 * @throws BirtException
	 */
	private void defineDataSourceDataSet( IQueryDefinition queryDefn, boolean keepDataSetFilter, boolean allowAggregation )
			throws AdapterException, BirtException
	{
		String dataSetName = queryDefn.getDataSetName( );

		ModuleHandle module = sessionContext.getModuleHandle();
		if ( module != null  )
		{
			List l = module.getAllDataSets( );
			DataSetHandle handle = null;
			for ( int i = 0; i < l.size( ); i++ )
			{
				if ( ( (DataSetHandle) l.get( i ) ).getQualifiedName( ) != null
						&& ( (DataSetHandle) l.get( i ) ).getQualifiedName( )
								.equals( dataSetName ) )
				{
					handle = (DataSetHandle) l.get( i );
					break;
				}
			}
			major = handle;
			defineDataSet( handle, keepDataSetFilter, allowAggregation );
		}
	}

	/**
	 * @param queryDefn
	 * @param paramBindingIt
	 * @param filterIt
	 * @param bindingIt
	 * @throws AdapterException 
	 */
	private void populateQueryDefn( IQueryDefinition queryDefn,
			Iterator paramBindingIt, Iterator filterIt, Iterator bindingIt, boolean disAllowAggregation ) throws AdapterException
	{
		try
		{
			while ( bindingIt != null && bindingIt.hasNext( ) )
			{
				Object computedBinding = bindingIt.next( );
				IBinding binding = null;
				if ( computedBinding instanceof ComputedColumnHandle )
				{
					binding = this.modelAdaptor.adaptBinding( (ComputedColumnHandle) computedBinding );
				}
				else if ( computedBinding instanceof org.eclipse.birt.report.model.api.elements.structures.ComputedColumn )
				{
					binding = adaptBinding( (org.eclipse.birt.report.model.api.elements.structures.ComputedColumn) computedBinding );
				}
				if ( binding == null
						|| ( disAllowAggregation && binding.getAggrFunction( ) != null ) )
					continue;
				queryDefn.addBinding( binding );
			}

			List parameterBindings = convertParamterBindings( paramBindingIt );

			// add parameter binding
			if ( parameterBindings != null )
				queryDefn.getInputParamBindings( ).addAll( parameterBindings );

			// add filter
			List filters = convertFilters( filterIt );
			if ( filters != null )
				queryDefn.getFilters( ).addAll( filters );
		}
		catch ( DataException e )
		{
			throw new AdapterException( e.getLocalizedMessage( ), e );
		}
	}

	/**
	 * NOTE: This binding is the temp binding, it would not be aggregation. The
	 * binding is used when select value list for filter expression.
	 * 
	 * @param structure
	 * @return
	 */
	private IBinding adaptBinding(
			org.eclipse.birt.report.model.api.elements.structures.ComputedColumn structure )
	{
		try
		{
			if ( structure == null )
				return null;
			Binding result = new Binding( structure.getName( ) );
			if ( structure.getExpression( ) != null )
			{
				ScriptExpression expr = this.modelAdaptor.adaptExpression( structure.getExpressionProperty( org.eclipse.birt.report.model.api.elements.structures.ComputedColumn.EXPRESSION_MEMBER ) );
				result.setExpression( expr );
			}
			result.setDisplayName( structure.getDisplayName( ) );
			result.setDataType( org.eclipse.birt.report.data.adapter.api.DataAdapterUtil.adaptModelDataType( structure.getDataType( ) ) );

			return result;
		}
		catch ( Exception e )
		{
			return null;
		}
	}
	
	/**
	 * 
	 * @param filterIt
	 * @return
	 * @throws AdapterException 
	 */
	private List convertFilters( Iterator filterIt ) throws AdapterException
	{
		if ( filterIt == null )
			return null;

		List filters = new ArrayList( );
		while ( filterIt.hasNext( ) )
		{
			while ( filterIt.hasNext( ) )
			{
				IFilterDefinition filter = this.modelAdaptor.adaptFilter( (FilterConditionHandle) filterIt.next( ) );
				filters.add( filter );
			}
		}
		return filters;
	}

	/**
	 * 
	 * @param paramBindingIt
	 * @return
	 */
	private List convertParamterBindings( Iterator paramBindingIt )
	{
		if ( paramBindingIt == null )
			return null;

		List parameterBindings = new ArrayList( );
		while ( paramBindingIt.hasNext( ) )
		{
			Object paramObj = paramBindingIt.next();
			if ( paramObj instanceof ParamBindingHandle )
			{
				ParamBindingHandle paramBinding = (ParamBindingHandle) paramObj;
				if ( paramBinding.getExpression( ) != null )
				{
					ScriptExpression paramValueExpr = new ScriptExpression( paramBinding.getExpression( ) );
					InputParameterBinding inputParamBinding = new InputParameterBinding( paramBinding.getParamName( ),
							paramValueExpr );
					parameterBindings.add( inputParamBinding );
				}
			}
			else if ( paramObj instanceof OdaDataSetParameterHandle )
			{
				OdaDataSetParameterHandle paramBinding = (OdaDataSetParameterHandle) paramObj;
				if ( paramBinding.getDefaultValue( ) != null )
				{
					ScriptExpression paramValueExpr = new ScriptExpression( paramBinding.getDefaultValue( ) );
					InputParameterBinding inputParamBinding = new InputParameterBinding( paramBinding.getName( ),
							paramValueExpr );
					parameterBindings.add( inputParamBinding );
				}
			}
		}
		return parameterBindings;
	}

	/**
	 * @param dataSetName
	 * @throws AdapterException
	 * @throws BirtException
	 */
	private void defineDataSet( DataSetHandle handle, boolean keepDataSetFilter, boolean disAllowAggregation ) throws AdapterException,
			BirtException
	{

		if ( handle == null )
		{
			return;
			//throw new AdapterException( ResourceConstants.DATASETHANDLE_NULL_ERROR );
		}
		
		DataSourceHandle dataSourceHandle = handle.getDataSource( );
		if ( dataSourceHandle != null )
		{
			IBaseDataSourceDesign dsourceDesign = this.modelAdaptor.adaptDataSource( dataSourceHandle );
			dataEngine.defineDataSource( dsourceDesign );
		}
		if ( handle instanceof JointDataSetHandle )
		{
			defineSourceDataSets( (JointDataSetHandle) handle );
		}
		if( handle instanceof DerivedDataSetHandle )
		{
			List inputDataSet = ( (DerivedDataSetHandle) handle ).getInputDataSets( );
			for ( int i = 0; i < inputDataSet.size( ); i++ )
			{
				defineDataSet( (DataSetHandle) inputDataSet.get( i ),
						keepDataSetFilter,
						disAllowAggregation );
			}
		}

		BaseDataSetDesign baseDS = this.modelAdaptor.adaptDataSet( handle );
		if ( useResultHints == false && handle.equals( major ))
		{
			baseDS.getResultSetHints( ).clear( );
		}
		
		if ( !keepDataSetFilter )
		{
			if ( baseDS.getFilters( ) != null )
				baseDS.getFilters( ).clear( );
		}
		
		if ( disAllowAggregation )
		{
			List computedColumns = baseDS.getComputedColumns();
			if ( computedColumns != null && computedColumns.size()!= 0)
			{
				for( int i = 0; i < computedColumns.size( ); i++ )
				{
					IComputedColumn computedColumn = (IComputedColumn)computedColumns.get( i );
					if( computedColumn.getAggregateFunction() != null )
					{
						computedColumns.set( i, new ComputedColumn( computedColumn.getName( ), "null") );
					}
				}
			}
		}
		
		if ( handle.getDataSourceName( ) != null )
		{
			TransientDataMartUtil.prepareDataSet( sessionContext.getAppContext( ),
					sessionContext.getDataEngineContext( ),
					( (DataEngineImpl) dataEngine ).getDataSourceDesign( baseDS.getDataSourceName( ) ),
					baseDS,
					session );
		}
		dataEngine.defineDataSet( baseDS );
	}

	/**
	 * @param dataSet
	 * @param dataSetDesign
	 * @throws BirtException
	 */
	private void defineSourceDataSets( JointDataSetHandle jointDataSetHandle )
			throws BirtException
	{
		Iterator iter = ( (JointDataSetHandle) jointDataSetHandle ).dataSetsIterator();
		while (iter.hasNext( ))
		{
			DataSetHandle dsHandle = (DataSetHandle) iter.next( ); 
			if ( dsHandle != null )
			{
				defineDataSet( dsHandle, true, false );
			}
		}
	}

}
