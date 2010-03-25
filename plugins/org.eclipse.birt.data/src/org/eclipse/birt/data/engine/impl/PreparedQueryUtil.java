/*******************************************************************************
 * Copyright (c) 2004, 2008 Actuate Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Actuate Corporation  - initial API and implementation
 *******************************************************************************/

package org.eclipse.birt.data.engine.impl;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.sql.Timestamp;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.eclipse.birt.core.data.ExpressionUtil;
import org.eclipse.birt.core.exception.BirtException;
import org.eclipse.birt.data.engine.api.DataEngine;
import org.eclipse.birt.data.engine.api.DataEngineContext;
import org.eclipse.birt.data.engine.api.IBaseDataSetDesign;
import org.eclipse.birt.data.engine.api.IBaseExpression;
import org.eclipse.birt.data.engine.api.IBaseQueryDefinition;
import org.eclipse.birt.data.engine.api.IBaseQueryResults;
import org.eclipse.birt.data.engine.api.IBinding;
import org.eclipse.birt.data.engine.api.IComputedColumn;
import org.eclipse.birt.data.engine.api.IGroupDefinition;
import org.eclipse.birt.data.engine.api.IGroupInstanceInfo;
import org.eclipse.birt.data.engine.api.IJointDataSetDesign;
import org.eclipse.birt.data.engine.api.IOdaDataSetDesign;
import org.eclipse.birt.data.engine.api.IPreparedQuery;
import org.eclipse.birt.data.engine.api.IQueryDefinition;
import org.eclipse.birt.data.engine.api.IQueryResults;
import org.eclipse.birt.data.engine.api.IScriptDataSetDesign;
import org.eclipse.birt.data.engine.api.IScriptExpression;
import org.eclipse.birt.data.engine.api.ISortDefinition;
import org.eclipse.birt.data.engine.api.ISubqueryDefinition;
import org.eclipse.birt.data.engine.api.querydefn.BaseQueryDefinition;
import org.eclipse.birt.data.engine.api.querydefn.Binding;
import org.eclipse.birt.data.engine.api.querydefn.ScriptExpression;
import org.eclipse.birt.data.engine.api.querydefn.SortDefinition;
import org.eclipse.birt.data.engine.api.script.IBaseDataSetEventHandler;
import org.eclipse.birt.data.engine.core.DataException;
import org.eclipse.birt.data.engine.core.security.URLSecurity;
import org.eclipse.birt.data.engine.expression.ExpressionCompilerUtil;
import org.eclipse.birt.data.engine.expression.NamedExpression;
import org.eclipse.birt.data.engine.i18n.ResourceConstants;
import org.eclipse.birt.data.engine.impl.document.QueryResultIDUtil;
import org.eclipse.birt.data.engine.impl.document.QueryResultInfo;
import org.eclipse.birt.data.engine.impl.document.QueryResults;
import org.eclipse.birt.data.engine.impl.document.RDLoad;
import org.eclipse.birt.data.engine.impl.document.RDUtil;
import org.eclipse.birt.data.engine.impl.document.stream.StreamManager;
import org.eclipse.birt.data.engine.odi.IResultClass;
import org.mozilla.javascript.Scriptable;

/**
 * Create concreate class of IPreparedQuery
 */
public class PreparedQueryUtil
{
	private static final int BASED_ON_DATASET = 2;
	private static final int BASED_ON_PRESENTATION = 3;
	
	/**
	 * Creates a new instance of the proper subclass based on the type of the
	 * query passed in.
	 * 
	 * @param dataEngine
	 * @param queryDefn
	 * @param appContext
	 *            Application context map; could be null.
	 * @return PreparedReportQuery
	 * @throws DataException
	 */
	public static IPreparedQuery newInstance( DataEngineImpl dataEngine,
			IQueryDefinition queryDefn, Map appContext ) throws DataException
	{
		assert dataEngine != null;
		assert queryDefn != null;
		
		if( queryDefn.getDistinctValue( ) )
		{
			addAllBindingAsSortKey( queryDefn );
		}
		
		validateQuery(dataEngine, queryDefn);
		FilterPrepareUtil.prepareFilters( queryDefn, dataEngine.getContext( ).getScriptContext( ) );
		IQueryContextVisitor contextVisitor = QueryContextVisitorUtil.createQueryContextVisitor( queryDefn,
				appContext );
		if ( queryDefn.getSourceQuery( ) != null )
		{
			return new PreparedIVDataExtractionQuery( dataEngine,
					queryDefn,
					appContext,
					contextVisitor );   
		}
		
		IPreparedQuery preparedQuery = QueryPrepareUtil.prepareQuery( dataEngine,
				queryDefn,
				dataEngine.getDataSetDesign( queryDefn.getDataSetName( ) ),
				appContext,
				contextVisitor );
		
		if ( preparedQuery != null ) 
			return preparedQuery;
		
		if ( queryDefn.getQueryResultsID( ) != null )
		{
			if ( dataEngine.getContext( ).getMode( ) == DataEngineContext.MODE_GENERATION
				|| dataEngine.getContext( ).getMode( ) == DataEngineContext.DIRECT_PRESENTATION )
			{
				return new DummyPreparedQuery( queryDefn, dataEngine.getSession( ));
			}
			
			if ( dataEngine.getContext( ).getMode( ) == DataEngineContext.MODE_PRESENTATION )
			{
				return new DummyPreparedQuery( queryDefn,
						dataEngine.getSession( ),
						dataEngine.getContext( ),
						queryDefn.getQueryExecutionHints( ) != null
								? queryDefn.getQueryExecutionHints( )
										.getTargetGroupInstances( ) : null );
			}
			
			return newIVInstance( dataEngine, queryDefn, appContext );
		}
	
		
		IBaseDataSetDesign dset = cloneDataSetDesign( dataEngine.getDataSetDesign( queryDefn.getDataSetName( ) ) , appContext);
		if ( dset == null )
		{
			// In new column binding feature, when there is no data set,
			// it is indicated that a dummy data set needs to be created
			// internally. But using the dummy one, the binding expression only
			// can refer to row object and no other object can be refered such
			// as rows.
			if ( queryDefn.getQueryResultsID( ) == null )
				return new PreparedDummyQuery( queryDefn, dataEngine.getSession( ) );
		}

		
		FilterPrepareUtil.prepareFilters( dset.getFilters( ), dataEngine.getContext( ).getScriptContext( ) );
		QueryContextVisitorUtil.populateDataSet( contextVisitor, dset );
		

		if ( dset instanceof IScriptDataSetDesign )
		{
			preparedQuery = new PreparedScriptDSQuery( dataEngine,
					queryDefn,
					dset,
					appContext,
					QueryContextVisitorUtil.createQueryContextVisitor( queryDefn,
							appContext ));
		}
		else if ( dset instanceof IOdaDataSetDesign )
		{
			if ( dset instanceof IIncreCacheDataSetDesign )
			{
				preparedQuery = new PreparedIncreCacheDSQuery( dataEngine,
						queryDefn,
						dset,
						appContext );
			}
			else
			{
				preparedQuery = new PreparedOdaDSQuery( dataEngine,
						queryDefn,
						dset,
						appContext,
						contextVisitor );
			}
		}
		else if ( dset instanceof IJointDataSetDesign )
		{
			preparedQuery = new PreparedJointDataSourceQuery( dataEngine,
					queryDefn,
					dset,
					appContext,contextVisitor );
		}
		else
		{
			preparedQuery = DataSetDesignHelper.createPreparedQueryInstance( dset,
					dataEngine,
					queryDefn,
					appContext );
			if ( preparedQuery == null )
				throw new DataException( ResourceConstants.UNSUPPORTED_DATASET_TYPE,
						dset.getName( ) );
		}

		return preparedQuery;
	}
	
	private static void addAllBindingAsSortKey( IQueryDefinition queryDefn ) throws DataException
	{
		if( ! ( queryDefn instanceof BaseQueryDefinition ) )
		{
			return;
		}
		Set<String> sortedBinding = new HashSet<String>( );
		List<ISortDefinition> sorts = queryDefn.getSorts( );
		if ( sorts != null )
		{
			for ( ISortDefinition sd : sorts )
			{
				List<String> bindingNames = ExpressionCompilerUtil.extractColumnExpression( sd.getExpression( ), ExpressionUtil.ROW_INDICATOR );
				if ( bindingNames != null && bindingNames.size( ) > 0 )
				{
					for ( String bindingName : bindingNames )
					{
						sortedBinding.add( bindingName );
					}
				}
				else
				{
					if( sd.getColumn( ) != null )
						sortedBinding.add( sd.getColumn( ) );
				}
			}
		}
		Iterator bindings = queryDefn.getBindings( ).values( ).iterator( );
		BaseQueryDefinition queryDefinition = ( ( BaseQueryDefinition ) queryDefn );
		while( bindings.hasNext( ) )
		{
			IBinding binding = (IBinding) bindings.next( );
			if( !sortedBinding.contains( binding.getBindingName( ) ) )
			{
				SortDefinition sd = new SortDefinition( );
				sd.setExpression( ExpressionUtil.createJSRowExpression( binding.getBindingName( ) ) );
				queryDefinition.addSort( sd );
			}
		}
	}
	
	/**
	 * validate query
	 * @param dataEngine
	 * @param queryDefn
	 * @throws DataException
	 */
	private static void validateQuery(DataEngineImpl dataEngine,
			IQueryDefinition queryDefn) throws DataException
	{
		String dataSetName = queryDefn.getDataSetName( );
		IBaseDataSetDesign dataSet = dataEngine.getDataSetDesign( dataSetName );
		if (dataSet != null)
		{
			validateComputedColumns(dataSet);
		}
		validateSorts( queryDefn );
		//validateSummaryQuery( queryDefn );
	}
	
	private static void validateSummaryQuery( IQueryDefinition queryDefn ) throws DataException
	{
		if ( queryDefn.isSummaryQuery( ) )
		{
			String lastGroupName = null;
			if ( queryDefn.getGroups( ).size( ) > 0 )
			{
				IGroupDefinition group = (IGroupDefinition) queryDefn.getGroups( )
						.get( queryDefn.getGroups( ).size( ) - 1 );
				lastGroupName = group.getName( );
			}
			Map<String, IBinding> bindings = queryDefn.getBindings( );
			for ( IBinding binding : bindings.values( ) )
			{
				if ( binding.getAggrFunction( ) != null )
				{
					if ( binding.getAggregatOns( ).size( ) == 0
							&& lastGroupName == null )
						continue;
					if ( binding.getAggregatOns( ).size( ) == 1
							&& binding.getAggregatOns( )
									.get( 0 )
									.toString( )
									.equals( lastGroupName ) )
						continue;
					throw new DataException( ResourceConstants.INVALID_AGGR_LEVEL_IN_SUMMARY_QUERY,
							binding.getBindingName( ) );
				}
			}
		}
	}

	@SuppressWarnings("unchecked")
	private static void validateSorts( IQueryDefinition queryDefn ) throws DataException
	{
		List<ISortDefinition> sorts = queryDefn.getSorts( );
		if ( sorts != null )
		{
			for ( ISortDefinition sd : sorts )
			{
				List<String> bindingNames = ExpressionCompilerUtil.extractColumnExpression( sd.getExpression( ), ExpressionUtil.ROW_INDICATOR );
				if ( bindingNames != null )
				{
					for ( String bindingName : bindingNames )
					{
						IBinding binding = (IBinding)queryDefn.getBindings( ).get( bindingName );
						if ( binding != null && binding.getAggrFunction( ) != null )
						{
							//sort key expression can't base on Aggregation
							throw new DataException( ResourceConstants.SORT_ON_AGGR, bindingName );
						}
					}
				}
			}
		}
	}
	
	/**
	 * Check whether computed columns defined in data set are valid 
	 * @param bdsd
	 * @throws DataException
	 */
	@SuppressWarnings("unchecked")
	private static void validateComputedColumns(IBaseDataSetDesign bdsd) throws DataException
	{
		//check whether dependency cycle exist in computed columns
		List<IComputedColumn> ccs = bdsd.getComputedColumns( );
		if (ccs != null)
		{
			//used check whether reference cycle exists
			Set<NamedExpression> namedExpressions = new HashSet<NamedExpression>( ); 
			for (IComputedColumn cc : ccs)
			{
				String name = cc.getName( );
				if (name == null || name.equals( "" ))
				{
					throw new DataException( ResourceConstants.CUSTOM_FIELD_EMPTY );
				}
				IBaseExpression expr = cc.getExpression( );
				namedExpressions.add( new NamedExpression(name, expr) );
			}
			String nameInvolvedInCycle
				= ExpressionCompilerUtil.getFirstFoundNameInCycle( namedExpressions, ExpressionUtil.ROW_INDICATOR );
			if (nameInvolvedInCycle != null)
			{
				throw new DataException( ResourceConstants.COMPUTED_COLUMN_CYCLE, nameInvolvedInCycle);
			}
		}
		
	}
	
	/**
	 * 
	 * @param dataSetDesign
	 * @param appContext 
	 * @return
	 * @throws DataException
	 */
	private static IBaseDataSetDesign cloneDataSetDesign(
			IBaseDataSetDesign dataSetDesign, Map appContext ) throws DataException
	{
		if ( dataSetDesign instanceof IScriptDataSetDesign )
		{
			return new ScriptDataSetAdapter( dataSetDesign );
		}
		else if ( dataSetDesign instanceof IOdaDataSetDesign )
		{
			return adaptOdaDataSetDesign( dataSetDesign, appContext );
		}
		else if ( dataSetDesign instanceof IJointDataSetDesign )
		{
			return new JointDataSetAdapter( dataSetDesign );
		}
		else
		{
			IBaseDataSetDesign design = DataSetDesignHelper.createAdapter( dataSetDesign );
			return design;
		}
	}

	/**
	 * @param dataSetDesign
	 * @param appContext
	 * @return
	 * @throws DataException
	 */
	private static IBaseDataSetDesign adaptOdaDataSetDesign(
			IBaseDataSetDesign dataSetDesign, Map appContext )
			throws DataException
	{
		IBaseDataSetDesign adaptedDesign = null;
		URL configFileUrl = IncreCacheDataSetAdapter.getConfigFileURL( appContext );
		if ( configFileUrl != null )
		{
			try
			{
				InputStream is = configFileUrl.openStream( );
				ConfigFileParser parser = new ConfigFileParser( is );
				String id = dataSetDesign.getName( );
				if ( parser.containDataSet( id ) )
				{
					final String mode = parser.getModeByID( id );
					if ( "incremental".equalsIgnoreCase( mode ) )
					{
						String queryTemplate = parser.getQueryTextByID( id );
						String timestampColumn = parser.getTimeStampColumnByID( id );
						String formatPattern = parser.getTSFormatByID( id );
						IncreCacheDataSetAdapter pscDataSet = new IncreCacheDataSetAdapter( dataSetDesign );
						pscDataSet.setCacheMode( IIncreCacheDataSetDesign.MODE_PERSISTENT );
						pscDataSet.setConfigFileUrl( configFileUrl );
						pscDataSet.setQueryTemplate( queryTemplate );
						pscDataSet.setTimestampColumn( timestampColumn );
						pscDataSet.setFormatPattern( formatPattern );
						adaptedDesign = pscDataSet;
					}
					else
					{
						String message = (String)AccessController.doPrivileged( new PrivilegedAction<Object>()
						{
						  public Object run()
						  {
						    return MessageFormat.format(ResourceConstants.UNSUPPORTED_INCRE_CACHE_MODE,new Object[]{mode});
						  } 
						});
						
						throw new UnsupportedOperationException( message );
					}
				}
				is.close( );
			}
			catch ( FileNotFoundException e )
			{
				e.printStackTrace( );
			}
			catch ( IOException e )
			{
				e.printStackTrace( );
			}
		}
		if ( adaptedDesign == null )
		{
			adaptedDesign = new OdaDataSetAdapter( dataSetDesign );
		}
		return adaptedDesign;
	}

	/**
	 * @param dataEngine
	 * @param queryDefn
	 * @return
	 * @throws DataException
	 */
	private static IPreparedQuery newIVInstance( DataEngineImpl dataEngine,
			IQueryDefinition queryDefn, Map appContext ) throws DataException
	{
		switch ( runQueryOnRS( dataEngine, queryDefn ) )
		{
			case BASED_ON_DATASET:	
				return new PreparedIVDataSourceQuery( dataEngine, queryDefn, QueryContextVisitorUtil.createQueryContextVisitor( queryDefn,
						appContext ) );
			default:
				return new DummyPreparedQuery( queryDefn,
						dataEngine.getSession( ),
						dataEngine.getContext( ),
						PLSUtil.isPLSEnabled( queryDefn )? queryDefn.getQueryExecutionHints( )
										.getTargetGroupInstances( ) : null );
		}
	}

	/**
	 * Whether query is running based on the result set of report document or
	 * the data set.
	 * 
	 * @param dataEngine
	 * @param queryDefn
	 * @return true, running on result set
	 * @throws DataException
	 */
	private static int runQueryOnRS( DataEngineImpl dataEngine,
			IQueryDefinition queryDefn ) throws DataException
	{
		String queryResultID = queryDefn.getQueryResultsID( );

		String rootQueryResultID = QueryResultIDUtil.get1PartID( queryResultID );
		String parentQueryResultID = null;
		if ( rootQueryResultID != null )
			parentQueryResultID = QueryResultIDUtil.get2PartID( queryResultID );
		else
			rootQueryResultID = queryResultID;

		QueryResultInfo queryResultInfo = new QueryResultInfo( rootQueryResultID,
				parentQueryResultID,
				null,
				null,
				-1 );
		RDLoad rdLoad = RDUtil.newLoad( dataEngine.getSession( ).getTempDir( ), dataEngine.getContext( ),
				queryResultInfo );

		//Please Note We should use parent scope here, for the new query should be compared to the query being executed 
		//immediately behind it rather than the root query.
		IBaseQueryDefinition previousQueryDefn = rdLoad.loadQueryDefn( StreamManager.ROOT_STREAM,
						StreamManager.PARENT_SCOPE );
	
		if( QueryCompUtil.isIVQueryDefnEqual( dataEngine.getContext( ).getMode( ), previousQueryDefn, queryDefn ))
		{
			return BASED_ON_PRESENTATION;
		}
		else
		{
			if ( queryDefn.isSummaryQuery( ) )
			{
				IResultClass rsMeta = rdLoad.loadExprDataResultSet( true )
						.getResultClass( );
				populateSummaryBinding( queryDefn, rsMeta );
			}
			return BASED_ON_DATASET;
		}		
	}
	
	private static void populateSummaryBinding( IQueryDefinition queryDefn, IResultClass rsMeta ) throws DataException
	{
		Set<String> nameSet = new HashSet<String>( );

		for ( int i = 1; i < rsMeta.getFieldCount( ); i++ )
		{
			nameSet.add( rsMeta.getFieldName( i ) );
		}
		Iterator<IBinding> bindingIt = queryDefn.getBindings( )
				.values( )
				.iterator( );
		while ( bindingIt.hasNext( ) )
		{
			IBinding binding = bindingIt.next( );
			if ( nameSet.contains( binding.getBindingName( ) ) )
			{
				binding.setAggrFunction( null );
				binding.getAggregatOns( ).clear( );
				binding.getArguments( ).clear( );
				binding.setExpression( new ScriptExpression( ExpressionUtil.createDataSetRowExpression( binding.getBindingName( ) ) ) );
			}
		}
	}
	
	/**
	 * Used for Result Set Sharing.
	 *
	 */
	private static class DummyPreparedQuery implements IPreparedQuery
	{
		private IQueryDefinition queryDefn;
		private String  tempDir;
		private DataEngineContext context;
		private List<IGroupInstanceInfo> targetGroups;
		private DataEngineSession session;
		
		/**
		 * 
		 * @param queryDefn
		 * @param session
		 */
		public DummyPreparedQuery( IQueryDefinition queryDefn,
				DataEngineSession session )
		{
			this.queryDefn = queryDefn;
			this.session = session;
			this.tempDir = session.getTempDir( );
		}
		
		/**
		 * 
		 * @param queryDefn
		 * @param session
		 * @param context
		 * @param targetGroups
		 */
		public DummyPreparedQuery( IQueryDefinition queryDefn,
				DataEngineSession session, DataEngineContext context,
				List<IGroupInstanceInfo> targetGroups )
		{
			this( queryDefn, session );
			this.context = context;
			this.targetGroups = targetGroups;
		}
		
		/*
		 * (non-Javadoc)
		 * @see org.eclipse.birt.data.engine.api.IPreparedQuery#execute(org.mozilla.javascript.Scriptable)
		 */
		public IQueryResults execute( Scriptable queryScope )
				throws BirtException
		{
			return this.execute( null, queryScope );
		}

		/*
		 * (non-Javadoc)
		 * @see org.eclipse.birt.data.engine.api.IPreparedQuery#execute(org.eclipse.birt.data.engine.api.IQueryResults, org.mozilla.javascript.Scriptable)
		 */
		public IQueryResults execute( IQueryResults outerResults,
				Scriptable queryScope ) throws BirtException
		{
			try
			{
				return this.execute( (IBaseQueryResults)outerResults, queryScope );
			}
			catch ( BirtException e )
			{
				throw DataException.wrap( e );
			}
		}

		/*
		 * (non-Javadoc)
		 * @see org.eclipse.birt.data.engine.api.IPreparedQuery#getParameterMetaData()
		 */
		public Collection getParameterMetaData( ) throws BirtException
		{
			return null;
		}

		/*
		 * (non-Javadoc)
		 * @see org.eclipse.birt.data.engine.api.IPreparedQuery#getReportQueryDefn()
		 */
		public IQueryDefinition getReportQueryDefn( )
		{
			return this.queryDefn;
		}

		public IQueryResults execute( IBaseQueryResults outerResults,
				Scriptable scope ) throws DataException
		{
			try
			{
				if ( context == null )
					return new CachedQueryResults( session,
							this.queryDefn.getQueryResultsID( ), this );

				else
					return new QueryResults( this.tempDir,
							this.context,
							this.queryDefn.getQueryResultsID( ), outerResults, this.targetGroups );
			}
			catch ( BirtException e )
			{
				throw DataException.wrap( e );
			}
		}
		
	}

	/**
	 * @throws DataException 
	 * 
	 */
	static void mappingParentColumnBinding( IBaseQueryDefinition baseQueryDefn ) throws DataException
	{
		IBaseQueryDefinition queryDef =  baseQueryDefn;
		while( queryDef instanceof ISubqueryDefinition )
		{
			queryDef = queryDef.getParentQuery();
			Map parentBindings = queryDef.getBindings( );
			addParentBindings( baseQueryDefn, parentBindings);
		}
	}
	
	/**
	 * 
	 * @param parentBindings
	 * @throws DataException 
	 */
	static void addParentBindings( IBaseQueryDefinition baseQueryDefn, Map parentBindings ) throws DataException {
		Map<String, Boolean> aggrInfo = QueryDefinitionUtil.parseAggregations( parentBindings );
		Iterator it = parentBindings.keySet( ).iterator( );
		while ( it.hasNext( ) )
		{
			String name = (String)it.next( );
			
			if ( !aggrInfo.get( name ))
			{
				//not an aggregation
				IBinding b = (IBinding)parentBindings.get( name );
				
				if ( baseQueryDefn.getBindings( ).get( name ) == null )
				{
					IBinding binding = new Binding( name );
					binding.setDataType( b.getDataType( ) );
					binding.setExpression( copyScriptExpr( b.getExpression( ) ) );
					baseQueryDefn.addBinding( binding );
				}
			}
		}
	}
	

	/**
	 * Colon a script expression, however do not populate the "AggregateOn" field. All the column binding that inherit
	 * from parent query by sub query should have no "AggregateOn" field, for they could not be aggregations. However, 
	 * if an aggregateOn field is set to an expression without aggregation, we should also make it inheritable by sub query
	 * for the expression actually involves no aggregations.
	 * 
	 * @param expr
	 * @return
	 */
	private static ScriptExpression copyScriptExpr( IBaseExpression expr )
	{
		if( expr == null )
			return null;
		ScriptExpression se = new ScriptExpression( ( (IScriptExpression) expr ).getText( ),
				( (IScriptExpression) expr ).getDataType( ) );
		return se;
	}
	
}

abstract class DataSetAdapter implements IBaseDataSetDesign
{
	private List computedColumns;
	private List filters;
	private IBaseDataSetDesign source;

	public DataSetAdapter( IBaseDataSetDesign source )
	{
	    this.source = source;
	    this.computedColumns = new ArrayList();
	    if( this.source.getComputedColumns( )!= null )
	    {
	    	this.computedColumns.addAll( this.source.getComputedColumns( ) );
	    }
		this.filters = new ArrayList( );
		if ( this.source.getFilters( ) != null )
		{
			this.filters.addAll( this.source.getFilters( ) );
		}
	}
	public String getAfterCloseScript( )
	{
		return this.source.getAfterCloseScript( );
	}
	public String getAfterOpenScript( )
	{
		return this.source.getAfterOpenScript( );
	}
	
	public String getBeforeCloseScript( )
	{
		return this.source.getBeforeCloseScript( );
	}
	public String getBeforeOpenScript( )
	{
		return this.source.getBeforeOpenScript( );
	}
	/**
	 * @deprecated
	 */
	public int getCacheRowCount( )
	{
		return this.source.getCacheRowCount( );
	}
	public List getComputedColumns( )
	{
		return this.computedColumns;
	}
	public String getDataSourceName( )
	{
		return this.source.getDataSourceName( );
	}
	public IBaseDataSetEventHandler getEventHandler( )
	{
		return this.source.getEventHandler( );
	}
	public List getFilters( )
	{
		return this.filters;
	}
	public Collection getInputParamBindings( )
	{
		return this.source.getInputParamBindings( );
	}
	public String getName( )
	{
		return this.source.getName( );
	}
	public String getOnFetchScript( )
	{
		return this.source.getOnFetchScript( );
	}
	public List getParameters( )
	{
		return this.source.getParameters( );
	}
	public List getResultSetHints( )
	{
		return this.source.getResultSetHints( );
	}
	public int getRowFetchLimit( )
	{
		return this.source.getRowFetchLimit( );
	}
	public boolean needDistinctValue( )
	{
		return this.source.needDistinctValue( );
	}
	public void setRowFetchLimit( int max )
	{
		this.source.setRowFetchLimit( max );
	}
}

class OdaDataSetAdapter extends DataSetAdapter implements IOdaDataSetDesign
{
	private IOdaDataSetDesign source;
	
	public OdaDataSetAdapter( IBaseDataSetDesign source )
	{
		super( source );
		this.source = ( IOdaDataSetDesign )source;
	}

	public String getExtensionID( )
	{
		return this.source.getExtensionID( );
	}

	public String getPrimaryResultSetName( )
	{
		return this.source.getPrimaryResultSetName( );
	}

	public Map getPrivateProperties( )
	{
		return this.source.getPrivateProperties( );
	}

	public Map getPublicProperties( )
	{
		return this.source.getPublicProperties( );
	}

	public String getQueryText( )
	{
		return this.source.getQueryText( );
	}

	public int getPrimaryResultSetNumber( )
	{
		return this.source.getPrimaryResultSetNumber( );
	}
	
}

class JointDataSetAdapter extends DataSetAdapter implements IJointDataSetDesign
{
	private IJointDataSetDesign source;
	
	public JointDataSetAdapter( IBaseDataSetDesign source )
	{
		super( source );
		this.source = ( IJointDataSetDesign )source;
	}

	public List getJoinConditions( )
	{
		return this.source.getJoinConditions( );
	}

	public int getJoinType( )
	{
		return this.source.getJoinType( );
	}

	public String getLeftDataSetDesignName( )
	{
		return this.source.getLeftDataSetDesignName( );
	}

	public String getRightDataSetDesignName( )
	{
		return this.source.getRightDataSetDesignName( );
	}
	
}

class ScriptDataSetAdapter extends DataSetAdapter implements IScriptDataSetDesign
{
	private IScriptDataSetDesign source;
	public ScriptDataSetAdapter( IBaseDataSetDesign source )
	{
		super( source );
		this.source = (IScriptDataSetDesign)source;
	}

	
	public String getCloseScript( )
	{
		return this.source.getCloseScript( );
	}

	public String getDescribeScript( )
	{
		return this.source.getDescribeScript( );
	}

	public String getFetchScript( )
	{
		return this.source.getFetchScript( );
	}

	public String getOpenScript( )
	{
		return this.source.getOpenScript( );
	}
}


/**
 * 
 */
class IncreCacheDataSetAdapter extends OdaDataSetAdapter
		implements
			IIncreCacheDataSetDesign
{

	/**
	 * string patterns for parsing the query.
	 */
	private final String DATE = "\\Q${DATE}$\\E";
	private final String TS_COLUMN = "\\Q${TIMESTAMP-COLUMN}$\\E";
	private final String TS_FORMAT = "\\Q${TIMESTAMP-FORMAT}$\\E";
	
	protected URL configFileUrl;
	protected String queryTemplate;
	protected String timestampColumn;
	protected String formatPattern;
	protected int cacheMode;

	private String queryForUpdate;

	public IncreCacheDataSetAdapter( IBaseDataSetDesign source )
	{
		super( source );
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.birt.data.engine.api.IIncreDataSetDesign#getCacheMode()
	 */
	public int getCacheMode( )
	{
		return cacheMode;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.birt.data.engine.api.IIncreDataSetDesign#getConfigFilePath()
	 */
	public URL getConfigFileUrl( )
	{
		return configFileUrl;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.birt.data.engine.api.IIncreDataSetDesign#getQueryForUpdate(long)
	 */
	public String getQueryForUpdate( long timestamp )
	{
		return parseQuery( timestamp );
	}

	/**
	 * 
	 * @param time
	 * @return
	 */
	private String parseQuery( long time )
	{
		SimpleDateFormat formater = new SimpleDateFormat( formatPattern );
		String timestamp = formater.format( new Timestamp( time ) );
		if ( queryForUpdate == null )
		{
			queryForUpdate = replaceIgnoreCase( queryTemplate, TS_COLUMN, timestampColumn );
			queryForUpdate = replaceIgnoreCase( queryForUpdate, TS_FORMAT, formatPattern );
		}
		return replaceIgnoreCase( queryForUpdate, DATE, timestamp );
	}

	/**
	 * replace the target substring <code>target</code> in <code>source</code>
	 * with <code>replacement</code> case insensively.
	 * 
	 * @param source
	 * @param target
	 * @param replacement
	 * @return
	 */
	private String replaceIgnoreCase( String source, CharSequence target,
			CharSequence replacement )
	{
		return Pattern.compile( target.toString( ), Pattern.CASE_INSENSITIVE )
				.matcher( source )
				.replaceAll( quote( replacement.toString( ) ) );
	}
	
	/**
	 * Returns a literal replacement <code>String</code> for the specified
     * <code>String</code>.
	 * @param s
	 * @return
	 */
	private static String quote( String s )
	{
		if ( ( s.indexOf( '\\' ) == -1 ) && ( s.indexOf( '$' ) == -1 ) )
			return s;
		StringBuffer sb = new StringBuffer( );
		for ( int i = 0; i < s.length( ); i++ )
		{
			char c = s.charAt( i );
			if ( c == '\\' )
			{
				sb.append( '\\' );
				sb.append( '\\' );
			}
			else if ( c == '$' )
			{
				sb.append( '\\' );
				sb.append( '$' );
			}
			else
			{
				sb.append( c );
			}
		}
		return sb.toString( );
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.birt.data.engine.api.IIncreDataSetDesign#getTimestampColumn()
	 */
	public String getTimestampColumn( )
	{
		return timestampColumn;
	}

	/**
	 * @param configFilePath
	 *            the configFilePath to set
	 */
	public void setConfigFileUrl( URL configFileUrl )
	{
		this.configFileUrl = configFileUrl;
	}

	/**
	 * @param queryTemplate
	 *            the queryTemplate to set
	 */
	public void setQueryTemplate( String queryTemplate )
	{
		this.queryTemplate = queryTemplate;
	}

	/**
	 * @param timestampColumn
	 *            the timestampColumn to set
	 */
	public void setTimestampColumn( String timestampColumn )
	{
		this.timestampColumn = timestampColumn;
	}

	/**
	 * @param formatPattern
	 *            the formatPattern to set
	 */
	public void setFormatPattern( String formatPattern )
	{
		this.formatPattern = formatPattern;
	}

	
	/**
	 * @param cacheMode
	 *            the cacheMode to set
	 */
	public void setCacheMode( int cacheMode )
	{
		this.cacheMode = cacheMode;
	}
	
	/**
	 * the specified configure value can be a path or an URL object represents
	 * the location of the configure file, but the final returned value must be
	 * an URL object or null if fails to parse it.
	 * 
	 * @param appContext
	 * @return
	 * @throws DataException 
	 */
	public static URL getConfigFileURL( Map appContext ) throws DataException
	{
		if ( appContext != null )
		{
			Object configValue = appContext.get( DataEngine.INCREMENTAL_CACHE_CONFIG );
			URL url = null;
			if ( configValue instanceof URL )
			{
				url = (URL) configValue;
			}
			else if ( configValue instanceof String )
			{
				String configPath = configValue.toString( );
				try
				{
					url = URLSecurity.getURL( configPath );
				}
				catch ( MalformedURLException e )
				{
					try
					{// try to use file protocol to parse configPath
						url = URLSecurity.getURL( "file", "/", configPath );
					}
					catch ( MalformedURLException e1 )
					{
						return null;
					}
				}
			}
			return url;
		}
		return null;
	}
}
