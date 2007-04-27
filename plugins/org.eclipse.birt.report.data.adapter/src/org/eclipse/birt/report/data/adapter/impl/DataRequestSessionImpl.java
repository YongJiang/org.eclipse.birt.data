/*
 *************************************************************************
 * Copyright (c) 2006 Actuate Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Actuate Corporation  - initial API and implementation
 *  
 *************************************************************************
 */

package org.eclipse.birt.report.data.adapter.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.birt.core.data.ExpressionUtil;
import org.eclipse.birt.core.data.IColumnBinding;
import org.eclipse.birt.core.exception.BirtException;
import org.eclipse.birt.data.engine.api.DataEngine;
import org.eclipse.birt.data.engine.api.DataEngineContext;
import org.eclipse.birt.data.engine.api.IBaseDataSetDesign;
import org.eclipse.birt.data.engine.api.IBaseDataSourceDesign;
import org.eclipse.birt.data.engine.api.IBasePreparedQuery;
import org.eclipse.birt.data.engine.api.IBaseQueryResults;
import org.eclipse.birt.data.engine.api.IComputedColumn;
import org.eclipse.birt.data.engine.api.IDataQueryDefinition;
import org.eclipse.birt.data.engine.api.IPreparedQuery;
import org.eclipse.birt.data.engine.api.IQueryDefinition;
import org.eclipse.birt.data.engine.api.IQueryResults;
import org.eclipse.birt.data.engine.api.IResultIterator;
import org.eclipse.birt.data.engine.api.IResultMetaData;
import org.eclipse.birt.data.engine.api.querydefn.GroupDefinition;
import org.eclipse.birt.data.engine.api.querydefn.QueryDefinition;
import org.eclipse.birt.data.engine.api.querydefn.ScriptExpression;
import org.eclipse.birt.data.engine.core.DataException;
import org.eclipse.birt.data.engine.olap.api.IPreparedCubeQuery;
import org.eclipse.birt.data.engine.olap.api.cube.CubeElementFactory;
import org.eclipse.birt.data.engine.olap.api.cube.CubeMaterializer;
import org.eclipse.birt.data.engine.olap.api.cube.IDimension;
import org.eclipse.birt.data.engine.olap.api.cube.IHierarchy;
import org.eclipse.birt.data.engine.olap.api.cube.ILevelDefn;
import org.eclipse.birt.data.engine.olap.api.query.ICubeQueryDefinition;
import org.eclipse.birt.report.data.adapter.api.AdapterException;
import org.eclipse.birt.report.data.adapter.api.DataRequestSession;
import org.eclipse.birt.report.data.adapter.api.DataSessionContext;
import org.eclipse.birt.report.data.adapter.api.IModelAdapter;
import org.eclipse.birt.report.data.adapter.api.IRequestInfo;
import org.eclipse.birt.report.data.adapter.i18n.ResourceConstants;
import org.eclipse.birt.report.model.api.ComputedColumnHandle;
import org.eclipse.birt.report.model.api.DataSetHandle;
import org.eclipse.birt.report.model.api.DimensionConditionHandle;
import org.eclipse.birt.report.model.api.DimensionJoinConditionHandle;
import org.eclipse.birt.report.model.api.LevelAttributeHandle;
import org.eclipse.birt.report.model.api.ModuleHandle;
import org.eclipse.birt.report.model.api.olap.CubeHandle;
import org.eclipse.birt.report.model.api.olap.DimensionHandle;
import org.eclipse.birt.report.model.api.olap.MeasureGroupHandle;
import org.eclipse.birt.report.model.api.olap.MeasureHandle;
import org.eclipse.birt.report.model.api.olap.TabularCubeHandle;
import org.eclipse.birt.report.model.api.olap.TabularHierarchyHandle;
import org.eclipse.birt.report.model.api.olap.TabularLevelHandle;
import org.mozilla.javascript.Scriptable;

/**
 * Implementation of DataRequestSession
 */
public class DataRequestSessionImpl extends DataRequestSession
{

	//
	private DataEngine dataEngine;
	private IModelAdapter modelAdaptor;
	private DataSessionContext sessionContext;

	/**
	 * Constructs the data request session with the provided session context
	 * information.
	 * 
	 * @param context
	 * @throws BirtException
	 */
	public DataRequestSessionImpl( DataSessionContext context )
			throws BirtException
	{
		if ( context == null )
			throw new AdapterException( ResourceConstants.CONEXT_NULL_ERROR );

		dataEngine = DataEngine.newDataEngine( context.getDataEngineContext( ) );
		modelAdaptor = new ModelAdapter( context );
		sessionContext = context;

		// Comments out the following code. Now the definition of all data elements
		// will be defered until necessary.
		// If a report design handle provided, adapt all data sets and data
		// sources
		//adaptAllDataElements( );
	}

	/*
	 * @see org.eclipse.birt.report.data.adaptor.impl.IDataRequestSession#defineDataSource(org.eclipse.birt.data.engine.api.IBaseDataSourceDesign)
	 */
	public void defineDataSource( IBaseDataSourceDesign design )
			throws BirtException
	{
		dataEngine.defineDataSource( design );
	}

	/*
	 * @see org.eclipse.birt.report.data.adaptor.impl.IDataRequestSession#defineDataSet(org.eclipse.birt.data.engine.api.IBaseDataSetDesign)
	 */
	public void defineDataSet( IBaseDataSetDesign design ) throws BirtException
	{
		dataEngine.defineDataSet( design );
	}

	/*
	 * @see org.eclipse.birt.report.data.adaptor.api.DataRequestSession#getDataSetMetaData(java.lang.String,
	 *      boolean)
	 */
	public IResultMetaData getDataSetMetaData( String dataSetName,
			boolean useCache ) throws BirtException
	{
		return getDataSetMetaData( this.sessionContext.getModuleHandle( )
				.findDataSet( dataSetName ), useCache );
	}

	/*
	 * @see org.eclipse.birt.report.data.adaptor.api.DataRequestSession#getDataSetMetaData(org.eclipse.birt.report.model.api.DataSetHandle,
	 *      boolean)
	 */
	public IResultMetaData getDataSetMetaData( DataSetHandle dataSetHandle,
			boolean useCache ) throws BirtException
	{
		return new DataSetMetaDataHelper( this.dataEngine,
				this.modelAdaptor,
				this.sessionContext ).getDataSetMetaData( dataSetHandle,
				useCache );
	}

	/*
	 * @see org.eclipse.birt.report.data.adapter.api.DataRequestSession#getColumnValueSet(org.eclipse.birt.report.model.api.DataSetHandle,
	 *      java.util.Iterator, java.util.Iterator, java.lang.String)
	 */
	public Collection getColumnValueSet( DataSetHandle dataSet,
			Iterator inputParamBindings, Iterator columnBindings,
			String boundColumnName ) throws BirtException
	{
		return getColumnValueSet( dataSet,
				inputParamBindings,
				columnBindings,
				boundColumnName,
				null );
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.birt.report.data.adapter.api.DataRequestSession#getColumnValueSet(org.eclipse.birt.report.model.api.DataSetHandle, java.util.Iterator, java.util.Iterator, java.lang.String, int, int)
	 */
	public Collection getColumnValueSet( DataSetHandle dataSet,
			Iterator inputParamBindings, Iterator columnBindings,
			String boundColumnName, IRequestInfo requestInfo )
			throws BirtException
	{
		ArrayList temp = new ArrayList();
		
		while ( columnBindings.hasNext( ) )
		{
			temp.add( columnBindings.next( ) );
		}
		if ( referToAggregation( temp, boundColumnName ) )
			return new ArrayList( );
		
		IQueryResults queryResults = getGroupingQueryResults( dataSet,
				inputParamBindings,
				temp.iterator( ),
				boundColumnName );
		IResultIterator resultIt = queryResults.getResultIterator( );

		int maxRowCount = -1;
		if ( requestInfo != null )
		{
			resultIt.moveTo( requestInfo.getStartRow( ) );
			maxRowCount = requestInfo.getMaxRow( );
		}
		// Iterate through result, getting one column value per group, skipping
		// group detail rows
		ArrayList values = new ArrayList( );

		while ( resultIt.next( ) && maxRowCount != 0 )
		{
			Object value = resultIt.getValue( boundColumnName );
			values.add( value );
			resultIt.skipToEnd( 1 );
			maxRowCount--;
		}
		resultIt.close( );
		queryResults.close( );

		return values;
	}

	/*
	 * @see org.eclipse.birt.report.data.adaptor.api.DataRequestSession#refreshMetaData(org.eclipse.birt.report.model.api.DataSetHandle)
	 */
	public IResultMetaData refreshMetaData( DataSetHandle dataSetHandle )
			throws BirtException
	{
		return new DataSetMetaDataHelper( this.dataEngine,
				this.modelAdaptor,
				this.sessionContext ).refreshMetaData( dataSetHandle );
	}

	/*
	 * 
	 * @see org.eclipse.birt.report.data.adapter.api.DataRequestSession#refreshMetaData(org.eclipse.birt.report.model.api.DataSetHandle,
	 *      boolean)
	 */
	public IResultMetaData refreshMetaData( DataSetHandle dataSetHandle,
			boolean holdEvent ) throws BirtException
	{
		return new DataSetMetaDataHelper( this.dataEngine,
				this.modelAdaptor,
				this.sessionContext ).refreshMetaData( dataSetHandle, holdEvent );
	}

	/*
	 * @see org.eclipse.birt.report.data.adaptor.api.DataRequestSession#executeQuery(org.eclipse.birt.data.engine.api.IQueryDefinition,
	 *      java.util.Iterator, java.util.Iterator, java.util.Iterator)
	 */
	public IQueryResults executeQuery( QueryDefinition queryDefn,
			Iterator paramBindingIt, Iterator filterIt, Iterator bindingIt )
			throws BirtException
	{
		return new QueryExecutionHelper( this.dataEngine,
				this.modelAdaptor,
				this.sessionContext ).executeQuery( queryDefn,
				paramBindingIt,
				filterIt,
				bindingIt );
	}

	/*
	 * @see org.eclipse.birt.report.data.adaptor.impl.IDataRequestSession#getQueryResults(java.lang.String)
	 */
	public IQueryResults getQueryResults( String queryResultID )
			throws BirtException
	{
		return dataEngine.getQueryResults( queryResultID );
	}

	/*
	 * @see org.eclipse.birt.report.data.adaptor.impl.IDataRequestSession#clearCache(org.eclipse.birt.data.engine.api.IBaseDataSourceDesign,
	 *      org.eclipse.birt.data.engine.api.IBaseDataSetDesign)
	 */
	public void clearCache( IBaseDataSourceDesign dataSource,
			IBaseDataSetDesign dataSet ) throws BirtException
	{
		dataEngine.clearCache( dataSource, dataSet );
	}

	/*
	 * @see org.eclipse.birt.report.data.adaptor.impl.IDataRequestSession#prepare(org.eclipse.birt.data.engine.api.IQueryDefinition,
	 *      java.util.Map)
	 */
	public IPreparedQuery prepare( IQueryDefinition query, Map appContext )
			throws BirtException
	{
		if ( appContext == null )
			// Use session app context
			appContext = sessionContext.getAppContext( );
		return dataEngine.prepare( query, appContext );
	}

	/**
	 * @see org.eclipse.birt.report.data.adapter.api.DataRequestSession#prepare(org.eclipse.birt.data.engine.api.IQueryDefinition)
	 */
	public IPreparedQuery prepare(IQueryDefinition query) throws BirtException
	{
		// Use session app context
		return dataEngine.prepare( query, this.sessionContext.getAppContext() );
	}

	/*
	 * @see org.eclipse.birt.report.data.adaptor.impl.IDataRequestSession#closeDataSource(java.lang.String)
	 */
	public void closeDataSource( String dataSourceName ) throws BirtException
	{
		dataEngine.closeDataSource( dataSourceName );
	}

	/*
	 * @see org.eclipse.birt.report.data.adaptor.impl.IDataRequestSession#getModelAdaptor()
	 */
	public IModelAdapter getModelAdaptor( )
	{
		return modelAdaptor;
	}

	/*
	 * @see org.eclipse.birt.report.data.adaptor.impl.IDataRequestSession#shutdown()
	 */
	public void shutdown( )
	{
		dataEngine.shutdown( );
		dataEngine = null;
	}

	/**
	 * get the distinct value of query
	 * @param dataSet
	 * @param inputParamBindings
	 * @param columnBindings
	 * @param boundColumnName
	 * @return
	 * @throws BirtException
	 */
	private IQueryResults getGroupingQueryResults( DataSetHandle dataSet,
			Iterator inputParamBindings, Iterator columnBindings,
			String boundColumnName ) throws BirtException
	{
		assert dataSet != null;
		// TODO: this is the inefficient implementation
		// Need to enhance the implementation to verify that the column is bound
		// to a data set column

		// Run a query with the provided binding information. Group by bound
		// column so we can
		// retrieve distinct values using the grouping feature
		QueryDefinition query = new QueryDefinition( );
		query.setDataSetName( dataSet.getQualifiedName( ) );
		GroupDefinition group = new GroupDefinition( );
		group.setKeyColumn( boundColumnName );
		query.addGroup( group );
		query.setUsesDetails( false );

		ModuleHandle moduleHandle = sessionContext.getModuleHandle( );
		if ( moduleHandle == null )
			moduleHandle = dataSet.getModuleHandle( );

		QueryExecutionHelper execHelper = new QueryExecutionHelper( this.dataEngine,
				this.modelAdaptor,
				this.sessionContext );
		IQueryResults results = execHelper.executeQuery( query,
				inputParamBindings,
				null,
				columnBindings );
		return results;
	}

	/**
	 * This method is used to validate the column binding to see if it contains aggregations.
	 * If so then return true, else return false;
	 * 
	 * @param columnBindings
	 * @param boundColumnName
	 * @throws BirtException
	 */
	private boolean referToAggregation( List bindings,
			String boundColumnName ) throws BirtException
	{
		if ( boundColumnName == null )
			return true;
		Iterator columnBindings = bindings.iterator( ); 
		while ( columnBindings != null && columnBindings.hasNext( ) )
		{
			IComputedColumn column = this.modelAdaptor.adaptComputedColumn( (ComputedColumnHandle) columnBindings.next( ) );
			if ( column.getName( ).equals( boundColumnName ) )
			{
				ScriptExpression sxp = (ScriptExpression) column.getExpression( );
				if ( ExpressionUtil.hasAggregation( sxp.getText( ) ) )
				{
					return true;
				}
				else
				{
					Iterator columnBindingNameIt = ExpressionUtil.extractColumnExpressions( sxp.getText( ) )
							.iterator( );
					while ( columnBindingNameIt.hasNext( ) )
					{
						IColumnBinding columnBinding = (IColumnBinding)columnBindingNameIt.next( );
						
						if ( referToAggregation( bindings,
								columnBinding.getResultSetColumnName( ) ) )
							return true;
					}
				}
			}
		}
		return false;
	}
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.birt.report.data.adapter.api.DataRequestSession#execute(org.eclipse.birt.data.engine.api.IBasePreparedQuery, org.eclipse.birt.data.engine.api.IBaseQueryResults, org.mozilla.javascript.Scriptable)
	 */
	public IBaseQueryResults execute( IBasePreparedQuery query,
			IBaseQueryResults outerResults, Scriptable scope )
			throws AdapterException
	{
		try
		{
			if ( query instanceof IPreparedQuery )
			{
				return ( (IPreparedQuery) query ).execute( ( outerResults instanceof IQueryResults )
						? ( (IQueryResults) outerResults ) : null,
						scope );
			}
			else if ( query instanceof IPreparedCubeQuery )
			{
				return ( (IPreparedCubeQuery) query ).execute( scope );
			}
			return null;
		}
		catch ( BirtException e )
		{
			throw new AdapterException( e.getLocalizedMessage( ) );
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.birt.report.data.adapter.api.DataRequestSession#prepare(org.eclipse.birt.data.engine.api.IDataQueryDefinition)
	 */
	public IBasePreparedQuery prepare( IDataQueryDefinition query,
			Map appContext ) throws AdapterException
	{

		try
		{
			if ( query instanceof IQueryDefinition )
				return prepare( (IQueryDefinition) query, appContext == null
						? this.sessionContext.getAppContext( ) : appContext );
			else if ( query instanceof ICubeQueryDefinition )
				return prepare( (ICubeQueryDefinition) query );
			else
				return null;
		}
		catch ( BirtException e )
		{
			throw new AdapterException( e.getLocalizedMessage( ) );
		}
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.birt.data.engine.api.DataEngine#defineCube(org.eclipse.birt.report.model.api.olap.CubeHandle)
	 */
	public void defineCube( CubeHandle cubeHandle ) throws BirtException
	{
		int mode = this.sessionContext.getDataEngineContext( ).getMode( );
		try
		{
			CubeMaterializer cubeMaterializer = null;

			if ( mode == DataEngineContext.DIRECT_PRESENTATION )
			{
				cubeMaterializer = new org.eclipse.birt.data.engine.olap.api.cube.CubeMaterializer( this.sessionContext.getDataEngineContext( )
						.getTmpdir( ),
						cubeHandle.getQualifiedName( ) );
				createCube( (TabularCubeHandle)cubeHandle, cubeMaterializer );
			}
			else if ( mode == DataEngineContext.MODE_GENERATION )
			{
				cubeMaterializer = new org.eclipse.birt.data.engine.olap.api.cube.CubeMaterializer( this.sessionContext.getDataEngineContext( )
						.getTmpdir( ),
						cubeHandle.getQualifiedName( ) );
				createCube(  (TabularCubeHandle)cubeHandle, cubeMaterializer );
				cubeMaterializer.saveCubeToReportDocument( cubeHandle.getQualifiedName( ),
						this.sessionContext.getDocumentWriter( ),
						null );
			}
		} 
		catch ( Exception e )
		{
			throw new DataException( e.getLocalizedMessage( ) );
		}
	}

	/**
	 * 
	 * @param cubeHandle
	 * @param cubeMaterializer
	 * @throws IOException
	 * @throws BirtException
	 * @throws DataException
	 */
	private void createCube( TabularCubeHandle cubeHandle,
			CubeMaterializer cubeMaterializer ) throws IOException,
			BirtException, DataException
	{
		List measureNames = new ArrayList( );
		List measureGroups = cubeHandle.getContents( CubeHandle.MEASURE_GROUPS_PROP );
		for ( int i = 0; i < measureGroups.size( ); i++ )
		{
			MeasureGroupHandle mgh = (MeasureGroupHandle) measureGroups.get( i );
			List measures = mgh.getContents( MeasureGroupHandle.MEASURES_PROP );
			for ( int j = 0; j < measures.size( ); j++ )
			{
				MeasureHandle measure = (MeasureHandle) measures.get( j );
				measureNames.add( measure.getName( ) );
			}
		}

		Map extraTimeLevelNameHolder = new HashMap();
		IDimension[] dimensions = populateDimensions( cubeMaterializer, cubeHandle, extraTimeLevelNameHolder );
		cubeMaterializer.createCube( cubeHandle.getQualifiedName( ),
				dimensions,
				new DataSetIterator( this, cubeHandle, extraTimeLevelNameHolder ),
				this.toStringArray( measureNames ),
				null );
	}

	/**
	 * Populate all dimensions.
	 * @param cubeMaterializer
	 * @param dimHandles
	 * @return
	 * @throws IOException
	 * @throws BirtException
	 * @throws DataException
	 */
	private IDimension[] populateDimensions( CubeMaterializer cubeMaterializer,
			TabularCubeHandle cubeHandle, Map extraTimeLevelHolder ) throws IOException, BirtException, DataException
	{
		List dimHandles = cubeHandle.getContents( CubeHandle.DIMENSIONS_PROP );
		List result = new ArrayList( );
		for ( int i = 0; i < dimHandles.size( ); i++ )
		{
			result.add( populateDimension( cubeMaterializer,
					(DimensionHandle) dimHandles.get( i ), cubeHandle, extraTimeLevelHolder ) );
		}
		
		IDimension[] dimArray = new IDimension[dimHandles.size( )];
		for ( int i = 0; i < result.size( ); i++ )
		{
			dimArray[i] = (IDimension) result.get( i );
		}
		return dimArray;
	}

	/**
	 * Populate the dimension.
	 * 
	 * @param cubeMaterializer
	 * @param dim
	 * @return
	 * @throws IOException
	 * @throws BirtException
	 * @throws DataException
	 */
	private IDimension populateDimension( CubeMaterializer cubeMaterializer,
			DimensionHandle dim, TabularCubeHandle cubeHandle, Map extraTimeLevelHolder ) throws IOException,
			BirtException, DataException
	{
		List hiers = dim.getContents( DimensionHandle.HIERARCHIES_PROP );
		List iHiers = new ArrayList();
		for ( int j = 0; j < hiers.size( ); j++ )
		{
			TabularHierarchyHandle hierhandle = (TabularHierarchyHandle) hiers.get( 0 );
			List levels = hierhandle.getContents( TabularHierarchyHandle.LEVELS_PROP );
			boolean shareFactTable = cubeHandle.getDataSet( ).equals( hierhandle.getDataSet( ) );
			ILevelDefn[] levelInHier = null;
			
			String timeLevelName = null;
			int timeLevelOffset = 0;
			if ( dim.isTimeType( ) )
			{
				TabularLevelHandle level = (TabularLevelHandle)hierhandle.getContents( TabularHierarchyHandle.LEVELS_PROP ).get( 0 );
				timeLevelName = level.getColumnName( );
				timeLevelOffset = 1;
				extraTimeLevelHolder.put( dim, timeLevelName );
			}
			
			String leafLevelName = null;
			if ( !shareFactTable )
			{
				Iterator it = cubeHandle.joinConditionsIterator( );
				while ( it.hasNext( ) )
				{
					DimensionConditionHandle dimCondHandle = (DimensionConditionHandle) it.next( );

					if ( dimCondHandle.getHierarchy( ).equals( hierhandle ) )
					{
						//Currently only support 1 join condition for each facttable--dimensiontable
						DimensionJoinConditionHandle joinCondition = (DimensionJoinConditionHandle) dimCondHandle.getJoinConditions( )
								.iterator( )
								.next( );
					
						leafLevelName = joinCondition.getHierarchyKey( );
						if( hierhandle.getLevel( leafLevelName ) != null && !leafLevelName.equals( timeLevelName ))
						{
							if( ! (hierhandle.getLevel( hierhandle.getLevelCount( )-1 ).equals( hierhandle.getLevel( leafLevelName ) )))
							{
								throw new AdapterException("The join key should always be last level.");
							}
							levelInHier = new ILevelDefn[hierhandle.getLevelCount( )+timeLevelOffset];
							leafLevelName = null;
						}
						else
						{
							levelInHier = new ILevelDefn[hierhandle.getLevelCount( ) + 1 + timeLevelOffset];
						}
					}	
				}
			}else
			{
				levelInHier = new ILevelDefn[hierhandle.getLevelCount( )+timeLevelOffset];
			}
			
			for ( int k = 0; k < levels.size( ); k++ )
			{
				TabularLevelHandle level = (TabularLevelHandle) levels.get( k );
				List levelKeys = new ArrayList();
				Iterator it = level.attributesIterator( );
				while( it.hasNext( ) )
				{
					LevelAttributeHandle levelAttr = (LevelAttributeHandle)it.next( );
					levelKeys.add( level.getName( ) + "/" + levelAttr.getName( ));
				}
				levelInHier[k] = CubeElementFactory.createLevelDefinition( level.getName( ),
						new String[]{
							level.getName( )
						},
						this.toStringArray( levelKeys ) );
				
			}
			
			if ( timeLevelName!= null )
			{
				int index = 0;
				if( leafLevelName!= null )
					index = levelInHier.length-2;
				else
					index = levelInHier.length-1;
				levelInHier[index] = CubeElementFactory.createLevelDefinition( timeLevelName,
						new String[]{
						timeLevelName
				},
				new String[0] );
			}
			if ( leafLevelName!= null )
			{
				levelInHier[levelInHier.length-1] = CubeElementFactory.createLevelDefinition( leafLevelName,
						new String[]{
						leafLevelName
				},
				new String[0] );
			} 
			
			iHiers.add( cubeMaterializer.createHierarchy( hierhandle.getName( ),
					new DataSetIterator( this, hierhandle, timeLevelName, leafLevelName ),
					levelInHier ) );
		}
		return cubeMaterializer.createDimension( dim.getName( ),
				(IHierarchy) iHiers.get( 0 ) ) ;
	}
	
	/**
	 * 
	 * @param object
	 * @return
	 */
	private String[] toStringArray( List object )
	{
		String[] result = new String[object.size( )];
		for( int i = 0; i < object.size( ); i ++ )
		{
			result[i] = object.get( i ).toString();
		}
		return result;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.birt.report.data.adapter.api.DataRequestSession#prepare(org.eclipse.birt.data.engine.olap.api.query.ICubeQueryDefinition)
	 */
	public IPreparedCubeQuery prepare( ICubeQueryDefinition query )
			throws BirtException
	{
		return this.dataEngine.prepare( query, null );
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.birt.report.data.adapter.api.DataRequestSession#getCachedDataSetMetaData(org.eclipse.birt.data.engine.api.IBaseDataSourceDesign, org.eclipse.birt.data.engine.api.IBaseDataSetDesign)
	 */
	public IResultMetaData getCachedDataSetMetaData(
			IBaseDataSourceDesign dataSource, IBaseDataSetDesign dataSet )
			throws BirtException
	{
		return this.dataEngine.getCachedDataSetMetaData( dataSource, dataSet );
	}
	
}
