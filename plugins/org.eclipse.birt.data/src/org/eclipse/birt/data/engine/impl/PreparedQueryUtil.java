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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Timestamp;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
import org.eclipse.birt.data.engine.api.IConditionalExpression;
import org.eclipse.birt.data.engine.api.IFilterDefinition;
import org.eclipse.birt.data.engine.api.IGroupDefinition;
import org.eclipse.birt.data.engine.api.IJointDataSetDesign;
import org.eclipse.birt.data.engine.api.IOdaDataSetDesign;
import org.eclipse.birt.data.engine.api.IPreparedQuery;
import org.eclipse.birt.data.engine.api.IQueryDefinition;
import org.eclipse.birt.data.engine.api.IQueryResults;
import org.eclipse.birt.data.engine.api.IScriptDataSetDesign;
import org.eclipse.birt.data.engine.api.IScriptExpression;
import org.eclipse.birt.data.engine.api.ISortDefinition;
import org.eclipse.birt.data.engine.api.ISubqueryDefinition;
import org.eclipse.birt.data.engine.api.aggregation.IBuildInAggregation;
import org.eclipse.birt.data.engine.api.script.IBaseDataSetEventHandler;
import org.eclipse.birt.data.engine.core.DataException;
import org.eclipse.birt.data.engine.expression.ExpressionCompilerUtil;
import org.eclipse.birt.data.engine.i18n.ResourceConstants;
import org.eclipse.birt.data.engine.impl.document.FilterDefnUtil;
import org.eclipse.birt.data.engine.impl.document.GroupDefnUtil;
import org.eclipse.birt.data.engine.impl.document.QueryCompUtil;
import org.eclipse.birt.data.engine.impl.document.QueryResultIDUtil;
import org.eclipse.birt.data.engine.impl.document.QueryResultInfo;
import org.eclipse.birt.data.engine.impl.document.QueryResults;
import org.eclipse.birt.data.engine.impl.document.RDLoad;
import org.eclipse.birt.data.engine.impl.document.RDUtil;
import org.eclipse.birt.data.engine.impl.document.stream.StreamManager;
import org.mozilla.javascript.Scriptable;

/**
 * Create concreate class of IPreparedQuery
 */
class PreparedQueryUtil
{
	private static final int BASED_ON_RESULTSET = 1;
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
	static IPreparedQuery newInstance( DataEngineImpl dataEngine,
			IQueryDefinition queryDefn, Map appContext ) throws DataException
	{
		assert dataEngine != null;
		assert queryDefn != null;
		if ( queryDefn.getQueryResultsID( ) != null )
		{
			if ( dataEngine.getContext( ).getMode( ) == DataEngineContext.MODE_GENERATION
				|| dataEngine.getContext( ).getMode( ) == DataEngineContext.DIRECT_PRESENTATION )
			{
				return new DummyPreparedQuery( queryDefn, dataEngine.getSession( ).getTempDir( ));
			}
			return newIVInstance( dataEngine, queryDefn );
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
				return new PreparedDummyQuery( dataEngine.getContext( ),
						queryDefn,
						dataEngine.getSession().getSharedScope( ) );
		}

		IPreparedQuery preparedQuery;

		if ( dset instanceof IScriptDataSetDesign )
		{
			preparedQuery = new PreparedScriptDSQuery( dataEngine,
					queryDefn,
					dset,
					appContext );
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
						appContext );
			}
		}
		else if ( dset instanceof IJointDataSetDesign )
		{
			preparedQuery = new PreparedJointDataSourceQuery( dataEngine,
					queryDefn,
					dset,
					appContext );
		}
		else
		{
			throw new DataException( ResourceConstants.UNSUPPORTED_DATASET_TYPE,
					dset.getName( ) );
		}

		return preparedQuery;
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
		else if ( dataSetDesign == null )
		{
			return null;
		}
		throw new DataException( ResourceConstants.UNSUPPORTED_DATASET_TYPE,
				dataSetDesign.getName( ) );
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
					String mode = parser.getModeByID( id );
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
						String message = MessageFormat.format( ResourceConstants.UNSUPPORTED_INCRE_CACHE_MODE,
								new Object[]{
									mode
								} );
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
			IQueryDefinition queryDefn ) throws DataException
	{
		switch ( runQueryOnRS( dataEngine, queryDefn ) )
		{
			case BASED_ON_RESULTSET:
				return new PreparedIVQuery( dataEngine, queryDefn );
			case BASED_ON_DATASET:	
				return new PreparedIVDataSourceQuery( dataEngine, queryDefn );
			default:
				return new DummyPreparedQuery( queryDefn, dataEngine.getSession( ).getTempDir( ), dataEngine.getContext( ));
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

		IBaseQueryDefinition rootQueryDefn = rdLoad.loadQueryDefn( StreamManager.ROOT_STREAM,
						StreamManager.BASE_SCOPE );
	
		if( QueryCompUtil.isIVQueryDefnEqual( rootQueryDefn, queryDefn ))
		{
			return BASED_ON_PRESENTATION;
		}
		
		if( !queryDefn.usesDetails( ) )
		{
			queryDefn.getSorts( ).clear( );
		}
		
		boolean runningOnRS = GroupDefnUtil.isEqualGroups( queryDefn.getGroups( ),
				rdLoad.loadGroupDefn( StreamManager.ROOT_STREAM,
						StreamManager.BASE_SCOPE ) );
		if ( runningOnRS == false )
			return BASED_ON_DATASET;

		runningOnRS = !hasAggregationInFilter( queryDefn.getFilters( ) );
		if ( runningOnRS == false )
			return BASED_ON_DATASET;

		runningOnRS = isCompatibleRSMap( rdLoad.loadQueryDefn( StreamManager.ROOT_STREAM,
				StreamManager.BASE_SCOPE ).getBindings( ),
				queryDefn.getBindings( ) );

		if ( runningOnRS == false )
			return BASED_ON_DATASET;
		
		runningOnRS = isCompatibleSubQuery( rootQueryDefn,
				queryDefn );

		if ( runningOnRS == false )
			return BASED_ON_DATASET;

		IBaseQueryDefinition qd = rdLoad.loadQueryDefn( StreamManager.ROOT_STREAM,
				StreamManager.BASE_SCOPE );
		List filters = qd.getFilters( );

		if ( FilterDefnUtil.isConflictFilter( filters, queryDefn.getFilters( ) ) )
		{
			runningOnRS = false;
			
			FilterDefnUtil.getRealFilterList( rdLoad.loadOriginalQueryDefn( StreamManager.ROOT_STREAM,
					StreamManager.BASE_SCOPE ).getFilters( ), queryDefn.getFilters( ) );
		}

		if ( runningOnRS == false )
			return BASED_ON_DATASET;

		if ( ! QueryCompUtil.isEqualSorts( queryDefn.getSorts( ),
					qd.getSorts( )))
		{
			Iterator bindings = queryDefn.getBindings( ).values( ).iterator( );
			while( bindings.hasNext( ) )
			{
				IBinding binding = (IBinding)bindings.next( );
				if( binding.getAggrFunction( ) != null )
				{
					if( IBuildInAggregation.TOTAL_FIRST_FUNC.equals( binding.getAggrFunction( ) )
						|| IBuildInAggregation.TOTAL_LAST_FUNC.equals( binding.getAggrFunction( ) )	)
					{
						return BASED_ON_DATASET;
					}	
				}
				//TODO:Remove me after iportal team switch to use new aggregation definition in binding.
				if( binding.getExpression( )!= null && binding.getExpression( ) instanceof IScriptExpression )
				{
					IScriptExpression expr = (IScriptExpression)binding.getExpression( );
					if ( ExpressionUtil.hasAggregation( expr.getText( ) ))
					{
						if( expr.getText( ).matches( ".*\\QTotal.first\\E.*" ) || expr.getText( ).matches( ".*\\QTotal.last\\E.*" ))
						{
							return BASED_ON_DATASET;
						}
					}
				}
			}
		}
		// TODO enhance me
		// If the following conditions hold, running on data set
		// 1.There are sorts that different from that of original design
		// 2.The query has subqueries.
		// 3.The sorts are not direct reference to binding

		if ( !isBindingReferenceSort( queryDefn.getSorts( )))
			return BASED_ON_DATASET;	
		
		if ( hasSubquery( queryDefn ) )
		{
			if ( hasSubQueryInDetail( queryDefn.getSubqueries( ) ) )
				return BASED_ON_DATASET;
			
			if ( !QueryCompUtil.isEqualSorts( queryDefn.getSorts( ),
					qd.getSorts( ) ) )
			{   
				runningOnRS = false;
			}

			Collection subqueries = queryDefn.getSubqueries( );
			List gps = queryDefn.getGroups( );
			if ( gps != null && gps.size( ) > 0 )
			{
				for ( int i = 0; i < gps.size( ); i++ )
				{
					subqueries.addAll( ( (IGroupDefinition) gps.get( i ) ).getSubqueries( ) );
				}
			}
			
			Iterator it = subqueries.iterator( );
			while ( it.hasNext( ) )
			{
				IBaseQueryDefinition query = (IBaseQueryDefinition) it.next( );
				if ( !query.usesDetails( ) )
					query.getSorts( ).clear( );
				if ( query.getFilters( ) != null
						&& query.getFilters( ).size( ) > 0 )
				{
					runningOnRS = false;
					break;
				}
				//If there is group definition in subquery, do the query based on data source
				List groups = query.getGroups( );
				if ( groups != null && !groups.isEmpty( ) )
					runningOnRS = false;
				if ( runningOnRS == false )
					break;
			}
		}

		if ( runningOnRS == false )
			return BASED_ON_DATASET;

		if ( queryDefn.getFilters( ) != null
				&& queryDefn.getFilters( ).size( ) > 0 )
		{
			if ( !isFiltersEquals( filters, queryDefn.getFilters( ) ) )
				runningOnRS = queryDefn.getBindings( ).values( ) == null
						|| !hasAggregationOnRowObjects( queryDefn.getBindings( )
								.values( )
								.iterator( ) );
		}
		return runningOnRS?BASED_ON_RESULTSET:BASED_ON_DATASET;
	}
	
	private static boolean isBindingReferenceSort( List sorts )
	{
		if( sorts == null || sorts.size() == 0 )
			return true;
		for( int i = 0; i < sorts.size( ); i++ )
		{
			ISortDefinition sort = (ISortDefinition) sorts.get( i );
			if( sort.getExpression( )!= null )
			{
				try
				{
					if(ExpressionUtil.getColumnBindingName( sort.getExpression().getText( )) == null )
						return false;
				}
				catch ( BirtException e )
				{
				}
			}
		}
		return true;
	}

	/**
	 * 
	 * @param queryDefn
	 * @return
	 */
	private static boolean hasSubQueryInDetail( Collection col )
	{
		if( col == null || col.size( ) == 0 )
			return false;
		Iterator it = col.iterator( );
		while( it.hasNext( ) )
		{
			ISubqueryDefinition sub = (ISubqueryDefinition)it.next( );
			if( !sub.applyOnGroup( ) )
				return true;
			if( hasSubQueryInDetail( sub.getSubqueries( )))
				return true;
		}
		return false;
	}

	/**
	 * 
	 * @param oldFilter
	 * @param newFilter
	 * @return
	 */
	private static boolean isFiltersEquals( List oldFilter, List newFilter )
	{
		if( oldFilter.size() != newFilter.size( ))
			return false;
		for( int i = 0; i < oldFilter.size( ); i++ )
		{
			if( !FilterDefnUtil.isEqualFilter( (IFilterDefinition)oldFilter.get(i), (IFilterDefinition)newFilter.get(i )))
					return false;
		}	
		return true;
	}

	/**
	 * @param filters
	 * @return
	 */
	private static boolean hasAggregationInFilter( List filters )
	{
		if ( filters == null || filters.size( ) == 0 )
			return false;

		for ( int i = 0; i < filters.size( ); i++ )
		{
			Object o = ( (IFilterDefinition) filters.get( i ) ).getExpression( );
			if ( o instanceof IConditionalExpression )
			{
				int type = ( (IConditionalExpression) o ).getOperator( );
				if ( type == IConditionalExpression.OP_TOP_N
						|| type == IConditionalExpression.OP_BOTTOM_N
						|| type == IConditionalExpression.OP_TOP_PERCENT
						|| type == IConditionalExpression.OP_BOTTOM_PERCENT )
					return true;
				if ( ExpressionCompilerUtil.hasAggregationInExpr( (IBaseExpression) o ) )
					return true;
			}
		}

		return false;
	}

	/**
	 * @return
	 * @throws DataException 
	 */
	private static boolean isCompatibleRSMap( Map oldMap, Map newMap ) throws DataException
	{
		if ( oldMap == null )
			return newMap.size( ) == 0;
		else if ( newMap == null )
			return oldMap.size( ) == 0;

		if ( newMap.size( ) > oldMap.size( ) )
			return false;
		
		Iterator it = newMap.keySet( ).iterator( );
		while( it.hasNext( ) )
		{
			Object key = it.next( );
			Object oldObj = oldMap.get( key );
			Object newObj = newMap.get( key );
			if ( oldObj != null )
			{
				if( !QueryCompUtil.isTwoBindingEqual((IBinding)newObj, (IBinding)oldObj ))
					return false;
			}else
			{
				return false;
			}
 		}
		return true;
	}

	
	/**
	 * @param oldSubQuery
	 * @param newSubQuery
	 * @return
	 */
	private static boolean isCompatibleSubQuery( IBaseQueryDefinition oldDefn,
			IBaseQueryDefinition newDefn )
	{
		boolean isComp = QueryCompUtil.isCompatibleSQs( oldDefn.getSubqueries( ),
				newDefn.getSubqueries( ) );

		if ( isComp == false )
			return false;

		Iterator oldIt = oldDefn.getGroups( ).iterator( );
		Iterator newIt = newDefn.getGroups( ).iterator( );
		while ( newIt.hasNext( ) )
		{
			IGroupDefinition oldGroupDefn = (IGroupDefinition) oldIt.next( );
			IGroupDefinition newGroupDefn = (IGroupDefinition) newIt.next( );
			isComp = QueryCompUtil.isCompatibleSQs( oldGroupDefn.getSubqueries( ),
					newGroupDefn.getSubqueries( ) );
			if ( isComp == false )
				return false;
		}

		return true;
	}

	/**
	 * 
	 * @param query
	 * @return
	 * @throws DataException 
	 */
	private static boolean hasAggregationOnRowObjects( Iterator it ) throws DataException
	{
		while ( it.hasNext( ) )
		{
			IBinding binding = (IBinding)it.next( );
			if ( ExpressionCompilerUtil.hasAggregationInExpr( binding.getExpression( ) ) )
			{
				return true;
			}
			if ( binding.getAggrFunction( ) != null )
				return true;
		}
		return false;
	}

	/**
	 * 
	 * @param qd
	 * @return
	 */
	private static boolean hasSubquery( IQueryDefinition qd )
	{
		assert qd != null;
		if ( qd.getSubqueries( ) != null && qd.getSubqueries( ).size( ) > 0 )
		{
			return true;
		}

		if ( qd.getGroups( ) != null )
		{
			for ( int i = 0; i < qd.getGroups( ).size( ); i++ )
			{
				IGroupDefinition gd = (IGroupDefinition) qd.getGroups( )
						.get( i );
				if ( gd.getSubqueries( ) != null
						&& gd.getSubqueries( ).size( ) > 0 )
				{
					return true;
				}
			}
		}
		return false;
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
		
		/**
		 * 
		 * @param queryDefn
		 * @param context
		 */
		public DummyPreparedQuery( IQueryDefinition queryDefn, String tempDir )
		{
			this.queryDefn = queryDefn;
			this.tempDir = tempDir;
		}
		
		public DummyPreparedQuery( IQueryDefinition queryDefn, String tempDir, DataEngineContext context )
		{
			this( queryDefn, tempDir );
			this.context = context;
		}
		
		/*
		 * (non-Javadoc)
		 * @see org.eclipse.birt.data.engine.api.IPreparedQuery#execute(org.mozilla.javascript.Scriptable)
		 */
		public IQueryResults execute( Scriptable queryScope )
				throws BirtException
		{
			if ( context == null )
				return new CachedQueryResults( tempDir,
						this.queryDefn.getQueryResultsID( ), this );
			else
				return new QueryResults( this.tempDir,
						this.context,
						this.queryDefn.getQueryResultsID( ), null );
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
					return new CachedQueryResults( tempDir,
							this.queryDefn.getQueryResultsID( ), this );

				else
					return new QueryResults( this.tempDir,
							this.context,
							this.queryDefn.getQueryResultsID( ), outerResults );
			}
			catch ( BirtException e )
			{
				throw DataException.wrap( e );
			}
		}
		
	}
}

abstract class DataSetAdapter implements IBaseDataSetDesign
{
	private List computedColumns;
	private IBaseDataSetDesign source;

	public DataSetAdapter( IBaseDataSetDesign source )
	{
	    this.source = source;
	    this.computedColumns = new ArrayList();
	    if( this.source.getComputedColumns( )!= null )
	    {
	    	this.computedColumns.addAll( this.source.getComputedColumns( ) );
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
		return this.source.getFilters( );
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
	 */
	public static URL getConfigFileURL( Map appContext )
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
					url = new URL( configPath );
				}
				catch ( MalformedURLException e )
				{
					try
					{// try to use file protocol to parse configPath
						url = new URL( "file", "/", configPath );
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
