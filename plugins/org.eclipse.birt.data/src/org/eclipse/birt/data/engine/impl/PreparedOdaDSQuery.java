/*
 ******************************************************************************
 * Copyright (c) 2004, 2005 Actuate Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Actuate Corporation  - initial API and implementation
 * 
 ******************************************************************************
 */

package org.eclipse.birt.data.engine.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.eclipse.birt.data.engine.api.DataEngine;
import org.eclipse.birt.data.engine.api.IBaseDataSetDesign;
import org.eclipse.birt.data.engine.api.IBaseQueryResults;
import org.eclipse.birt.data.engine.api.IColumnDefinition;
import org.eclipse.birt.data.engine.api.IComputedColumn;
import org.eclipse.birt.data.engine.api.IOdaDataSetDesign;
import org.eclipse.birt.data.engine.api.IPreparedQuery;
import org.eclipse.birt.data.engine.api.IQueryDefinition;
import org.eclipse.birt.data.engine.api.IQueryResults;
import org.eclipse.birt.data.engine.core.DataException;
import org.eclipse.birt.data.engine.executor.DataSourceFactory;
import org.eclipse.birt.data.engine.i18n.ResourceConstants;
import org.eclipse.birt.data.engine.odaconsumer.QuerySpecHelper;
import org.eclipse.birt.data.engine.odi.IDataSource;
import org.eclipse.birt.data.engine.odi.IDataSourceQuery;
import org.eclipse.birt.data.engine.odi.IEventHandler;
import org.eclipse.birt.data.engine.odi.IParameterMetaData;
import org.eclipse.birt.data.engine.odi.IPreparedDSQuery;
import org.eclipse.birt.data.engine.odi.IQuery;
import org.eclipse.birt.data.engine.odi.IResultIterator;
import org.eclipse.datatools.connectivity.oda.spec.QuerySpecification;
import org.eclipse.datatools.connectivity.oda.spec.ValidationContext;
import org.mozilla.javascript.Scriptable;

/**
 * A prepared query which access an ODA data source.
 */
public class PreparedOdaDSQuery extends PreparedDataSourceQuery
		implements	IPreparedQuery
{
	/**
	 * @param dataEngine
	 * @param queryDefn
	 * @param dataSetDesign
	 * @throws DataException
	 */
	PreparedOdaDSQuery( DataEngineImpl dataEngine, IQueryDefinition queryDefn,
			IBaseDataSetDesign dataSetDesign, Map appContext, IQueryContextVisitor visitor )
			throws DataException
	{
		super( dataEngine, queryDefn, dataSetDesign, appContext != null
				? appContext: new HashMap( ), visitor );
		Object[] params = {
				dataEngine, queryDefn, dataSetDesign, appContext
		};
		logger.exiting( PreparedOdaDSQuery.class.getName( ),
				"PreparedOdaDSQuery", params );
	}
	
	/*
	 * @see org.eclipse.birt.data.engine.impl.PreparedQuery#newExecutor()
	 */
	protected QueryExecutor newExecutor() throws DataException
	{
		return new OdaDSQueryExecutor();
	}	

	/**
	 * @param publicProps
	 * @param privateProps
	 * @return
	 */
	private static Map copyProperties( Map publicProps, Map privateProps )
	{
	    if ( publicProps.isEmpty() && privateProps.isEmpty() )
	        return null;		// nothing to copy
	    
	    Map driverProps = new HashMap();
    	if ( ! publicProps.isEmpty() )
    	    driverProps.putAll( publicProps );
    	if ( ! privateProps.isEmpty() )
    	    driverProps.putAll( privateProps );

    	return driverProps;
	}
	
	/**
	 * @param odiDSQuery
	 * @param props
	 * @throws DataException
	 */
	private static void addProperty( IDataSourceQuery odiDSQuery, Map props )
			throws DataException
	{
	    if ( props == null  )
	        return;		// nothing to add
	    
	    Set entries = props.entrySet();
	    Iterator it = entries.iterator();
	    while ( it.hasNext() )
	    {
	    	Map.Entry entry = (Map.Entry)it.next();
	        String propName = (String) entry.getKey();
	        String value = (String) entry.getValue();
	        if ( propName == null || propName.length() == 0 )
	            continue;	// skip empty property name
	        
            odiDSQuery.addProperty( propName, value );
	    }
	}
	
	/*
	 * @see org.eclipse.birt.data.engine.api.IPreparedQuery#getParameterMetaData()
	 */
    public Collection getParameterMetaData() throws DataException
	{
    	OdaDSQueryExecutor exec = new OdaDSQueryExecutor();
    	return exec.getParameterMetaData();
	}
    
    /*
	 * @see org.eclipse.birt.data.engine.impl.PreparedDataSourceQuery#execute(org.eclipse.birt.data.engine.api.IQueryResults,
	 *      org.mozilla.javascript.Scriptable)
	 */
	public IQueryResults execute( IBaseQueryResults outerResults, Scriptable scope )
			throws DataException
	{
		this.initializeExecution( outerResults, scope );
		
		return super.execute( outerResults, scope );
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.birt.data.engine.impl.PreparedDataSourceQuery#initializeExecution(org.eclipse.birt.data.engine.api.IBaseQueryResults, org.mozilla.javascript.Scriptable)
	 */
	protected void initializeExecution( IBaseQueryResults outerResults, Scriptable scope ) throws DataException
	{
		this.configureParameterHints( queryDefn, appContext, scope );
	}
	
	/**
	 * A work-around to set the user defined parameter metadata
	 * 
	 * @param querySpec
	 * @param appContext
	 * @param scope
	 * @throws DataException
	 */
	private void configureParameterHints( IQueryDefinition querySpec,
			Map appContext, Scriptable scope ) throws DataException
	{
		if ( querySpec == null )
			return;
		
		if ( querySpec.getQueryResultsID( ) != null )
			return;
		
		List paramList = dataSetDesign.getParameters( );
		if ( paramList == null || paramList.size( ) == 0 )
			return;

		if ( ( (IOdaDataSetDesign) dataSetDesign ).getExtensionID( )
				.equals( "org.eclipse.birt.report.data.oda.jdbc.SPSelectDataSet" ) )
		{
			appContext.put( "org.eclipse.birt.report.data.oda.jdbc.ParameterHints",
					new UserDefinedParamMetaData( paramList ) );
		}
	}
	
    /**
     * 
	 * Concrete class of DSQueryExecutor used in PreparedExtendedDSQuery
	 * 
	 */
	public class OdaDSQueryExecutor extends DSQueryExecutor
	{
		// prepared query
		private IPreparedDSQuery odiPreparedQuery;

		private QuerySpecification querySpec;

		
		/**
		 * @return prepared query
		 */
		public IPreparedDSQuery getPreparedOdiQuery( )
		{
			return odiPreparedQuery;
		}
		
		/*
		 * @see org.eclipse.birt.data.engine.impl.PreparedQuery.Executor#createOdiDataSource()
		 */
		protected IDataSource createOdiDataSource( ) throws DataException
		{
			OdaDataSourceRuntime extDS = (OdaDataSourceRuntime) dataSource;
			assert extDS != null;

			// Obtains an odi data source matching the dynamic definition
			// of the extended data source
		    String driverName = extDS.getExtensionID();
		    if ( driverName == null || driverName.length() == 0 )
		        throw new DataException( ResourceConstants.MISSING_DATASOURCE_EXT_ID,
		        		extDS.getName( ) );

			ValidationContext validationContext = null;
			if ( queryDefn.getQueryExecutionHints( ).enablePushDown( ) )
			{
				int cacheSize = 0;
				if ( appContext.containsKey( DataEngine.MEMORY_DATA_SET_CACHE ) )
				{
					cacheSize = Integer.valueOf( appContext.get( DataEngine.MEMORY_DATA_SET_CACHE )
							.toString( ) )
							.intValue( );
				}
				if ( cacheSize <= 0
						&& appContext.containsKey( DataEngine.DATA_SET_CACHE_ROW_LIMIT ) )
				{
					cacheSize = Integer.valueOf( appContext.get( DataEngine.DATA_SET_CACHE_ROW_LIMIT )
							.toString( ) )
							.intValue( );
				}
				if ( cacheSize <= 0 )
				{
					validationContext = ( (OdaDataSetRuntime) dataSet ).getValidationContext( );
					if ( validationContext != null )
					{
						Properties connProperties = new Properties( );
						// merge public and private driver properties into a
						// single
						// Map
						Map driverProps = copyProperties( ( (OdaDataSourceRuntime) dataSource ).getPublicProperties( ),
								( (OdaDataSourceRuntime) dataSource ).getPrivateProperties( ) );

						if ( driverProps != null )
							connProperties.putAll( driverProps );

						QuerySpecHelper.setValidationConnectionContext( validationContext,
								connProperties,
								appContext );
					}
				}
			}
		    
			Map driverProps;
			// merge public and private driver properties into a single Map
			if ( validationContext == null
					|| validationContext.getConnection( ) == null )
				driverProps = copyProperties( extDS.getPublicProperties( ),
						extDS.getPrivateProperties( ) );
			else
				driverProps = validationContext.getConnection( )
						.getProperties( );

		    // calls ODI Data Source Factory to provide an ODI data source
			// object that matches the given properties
			return getDataSource( driverName, driverProps );
		}
		
		/**
		 * @param driverName
		 * @param driverProps
		 * @return
		 * @throws DataException
		 */
		private IDataSource getDataSource( String driverName, Map driverProps )
				throws DataException
		{
			PreparedOdaDSQuery self = PreparedOdaDSQuery.this;

			return DataSourceFactory.getFactory( ).getDataSource( driverName,
					driverProps,
					self.dataEngine.getSession( ) );
		}
		
		/*
		 * @see org.eclipse.birt.data.engine.impl.PreparedQuery.Executor#createOdiQuery()
		 */
		protected IQuery createOdiQuery( ) throws DataException
		{
			OdaDataSetRuntime extDataSet = (OdaDataSetRuntime) dataSet;
			assert extDataSet != null;
			assert odiDataSource != null;
			
			IDataSourceQuery odiQuery = null;
			String dataSetType = extDataSet.getExtensionID( );
			String dataText = extDataSet.getQueryText( );
			
			if ( queryDefn.getQueryExecutionHints( ).enablePushDown( ) )
			{
				int cacheSize = 0;
				if ( appContext.containsKey( DataEngine.MEMORY_DATA_SET_CACHE ) )
				{
					cacheSize = Integer.valueOf( appContext.get( DataEngine.MEMORY_DATA_SET_CACHE )
							.toString( ) )
							.intValue( );
				}
				if ( cacheSize <= 0
						&& appContext.containsKey( DataEngine.DATA_SET_CACHE_ROW_LIMIT ) )
				{
					cacheSize = Integer.valueOf( appContext.get( DataEngine.DATA_SET_CACHE_ROW_LIMIT )
							.toString( ) )
							.intValue( );
				}
				if ( cacheSize <= 0 )
				{
					ValidationContext validationContext = ( (OdaDataSetRuntime) dataSet ).getValidationContext( );

					if ( validationContext != null )
					{
						validationContext.setQueryText( ( (IOdaDataSetDesign) dataSetDesign ).getQueryText( ) );
						OptimizationRollbackHelper rollbackHelper = new OptimizationRollbackHelper(
								queryDefn, (IOdaDataSetDesign) dataSetDesign );
						rollbackHelper.collectOriginalInfo( );
						querySpec = OdaQueryOptimizationUtil.optimizeExecution( ( (OdaDataSourceRuntime) dataEngine.getDataSourceRuntime( dataSetDesign.getDataSourceName( ) ) ).getExtensionID( ),
								validationContext,
								(IOdaDataSetDesign) dataSetDesign,
								queryDefn,
								dataEngine.getSession( ),
								appContext,
								contextVisitor );
						if ( querySpec == null )
						{
							//roll back changes made in <code>dataSetDesign</code> and <code>queryDefn</code>
						    rollbackHelper.rollback( );
						}
					}					
				}
			}
			
			//Do not use cached DataSourceQuery when there is push-down operation
			if ( querySpec != null )
			{
				odiQuery = odiDataSource.newQuery( dataSetType, dataText, false, this.contextVisitor );
			}
			else
			{
				odiQuery = odiDataSource.newQuery( dataSetType,
						dataText,
						this.fromCache( ), this.contextVisitor );
			}

			if ( odiQuery instanceof IPreparedDSQuery )
			{
				( (IPreparedDSQuery) odiQuery ).setQuerySpecification( querySpec );
			}
			return odiQuery;
	 	}
		
		
		/*
		 * @see org.eclipse.birt.data.engine.impl.PreparedQuery.Executor#populateOdiQuery()
		 */
		protected void populateOdiQuery( ) throws DataException
		{
			super.populateOdiQuery( );
			
			OdaDataSetRuntime extDataSet = (OdaDataSetRuntime) dataSet;
			assert extDataSet != null;
			
			IDataSourceQuery odiDSQuery = (IDataSourceQuery) odiQuery;
			assert odiDSQuery != null;
			
			// assign driver properties
		    addProperty( odiDSQuery, extDataSet.getPublicProperties() );
		    addProperty( odiDSQuery, extDataSet.getPrivateProperties() );
		   
     		// assign parameter hints and result column hints
		    odiDSQuery.setParameterHints( 
		    		resolveDataSetParameters( true) );
		  
		    if ( extDataSet.getResultSetHints() != null )
		    {
				List source = extDataSet.getResultSetHints( );
				ArrayList odiHints = new ArrayList( );
				for ( int i = 0; i < source.size( ); i++ )
				{
					IColumnDefinition def = (IColumnDefinition) source.get( i );
					if ( def.getColumnName( )
							.equals( def.getColumnNativeName( ) )
							&& def.getAlias( ) == null )
					{
						//not a hint info
						continue;
					}
					IDataSourceQuery.ResultFieldHint odiHint = new IDataSourceQuery.ResultFieldHint( def.getColumnName( ) );
					odiHint.setPosition( def.getColumnPosition( ) );
					odiHint.setAlias( def.getAlias( ) );
					odiHint.setDataType( def.getDataType( ) );
					odiHint.setNativeDataType( def.getNativeDataType( ) );
					odiHints.add( odiHint );
		    	}
			    odiDSQuery.setResultHints( odiHints );
		    }	

		    // assign computed columns and projected columns
			// declare computed columns as custom fields
		    List ccList = extDataSet.getComputedColumns( );
			if ( ccList != null )
			{
				for ( int i = 0; i < ccList.size( ); i++ )
				{
					IComputedColumn cc = (IComputedColumn) ccList.get( i );
					odiDSQuery.declareCustomField( cc.getName( ),
							cc.getDataType( ) );
				}
			}
				
			// specify column projection, if any
	        odiDSQuery.setResultProjection( getReportQueryDefn().getColumnProjection() );
			
		}

		/*
		 * @see org.eclipse.birt.data.engine.impl.PreparedQuery.Executor#executeOdiQuery()
		 */
		protected IResultIterator executeOdiQuery( IEventHandler eventHandler )
				 throws DataException
		{
			dataSetAfterOpen( );
			OdaDataSetRuntime odaDataSet = (OdaDataSetRuntime) dataSet;
			assert odaDataSet != null;
			
			assert odiPreparedQuery != null;
			return odiPreparedQuery.execute( eventHandler );			
		}
		
		/*
		 * @see org.eclipse.birt.data.engine.impl.PreparedQuery.Executor#prepareOdiQuery()
		 */
		protected void prepareOdiQuery( ) throws DataException
		{
			IDataSourceQuery odiDSQuery = (IDataSourceQuery) odiQuery;
			assert odiDSQuery != null;

		    // prepare data set's odi query
			assert odiPreparedQuery == null;	// should not prepare more than once
			
			odiPreparedQuery = odiDSQuery.prepare();
			odiPreparedQuery.setQuerySpecification( querySpec );
		}
		
		
		/**
		 * Implements IPreparedQuery.getParameterMetadata. This method prepares
		 * the odi data source and query, and returns the query's parameter
		 * metadata as a Collection of ParameterMetadata objects.
		 * 
		 * @return
		 */
	    private Collection getParameterMetaData( ) throws DataException
		{
			// Create the data set runtime
			dataSet = newDataSetRuntime();
			assert dataSet != null;
			OdaDataSetRuntime odaDataSet = (OdaDataSetRuntime) dataSet;
			dataSource = findDataSource( );
			openDataSource( );
			
			// Run beforeOpen script now so the script can modify the DataSetRuntime properties
			dataSet.beforeOpen();

			// Create and populate odi query
			odiQuery = createOdiQuery( );
			IDataSourceQuery odiDSQuery = (IDataSourceQuery) odiQuery;
			assert odiDSQuery != null;
			
			// assign driver properties and parameter hints; these are the only
			// information we need from the data set to get param metadata
		    addProperty( odiDSQuery, odaDataSet.getPublicProperties() );
		    addProperty( odiDSQuery, odaDataSet.getPrivateProperties() );

		    odiDSQuery.setParameterHints(
		    		resolveDataSetParameters( false ) );
		    
		    // Prepare odi query; parameter metadata is available after the prepare call
			prepareOdiQuery( );
			
			assert odiPreparedQuery != null;

	        Collection odiParamsInfo = odiPreparedQuery.getParameterMetaData();
	        if ( odiParamsInfo == null || odiParamsInfo.isEmpty() )
	            return null;
	        
	        // iterates thru the most up-to-date collection, and
	        // wraps each of the ODI parameter metadata object
	        ArrayList paramMetaDataList = new ArrayList( odiParamsInfo.size() );
	        Iterator odiParamMDIter = odiParamsInfo.iterator();
	        while ( odiParamMDIter.hasNext() )
	        {
	        	IParameterMetaData odiMetaData = 
	                (IParameterMetaData) odiParamMDIter.next();
	        	ParameterMetaData apiMetaData = new ParameterMetaData( odiMetaData );
	            paramMetaDataList.add( apiMetaData );
	        }
	        return paramMetaDataList;
		}
	}
	
}
