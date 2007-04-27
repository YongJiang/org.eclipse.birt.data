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

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.birt.core.exception.BirtException;
import org.eclipse.birt.data.engine.api.DataEngineContext;
import org.eclipse.birt.data.engine.api.IBaseDataSetDesign;
import org.eclipse.birt.data.engine.api.IBaseExpression;
import org.eclipse.birt.data.engine.api.IBaseQueryDefinition;
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
import org.eclipse.birt.data.engine.core.DataException;
import org.eclipse.birt.data.engine.expression.ExpressionCompilerUtil;
import org.eclipse.birt.data.engine.i18n.ResourceConstants;
import org.eclipse.birt.data.engine.impl.document.FilterDefnUtil;
import org.eclipse.birt.data.engine.impl.document.GroupDefnUtil;
import org.eclipse.birt.data.engine.impl.document.QueryDefnUtil;
import org.eclipse.birt.data.engine.impl.document.QueryResultIDUtil;
import org.eclipse.birt.data.engine.impl.document.QueryResultInfo;
import org.eclipse.birt.data.engine.impl.document.RDLoad;
import org.eclipse.birt.data.engine.impl.document.RDUtil;
import org.eclipse.birt.data.engine.impl.document.stream.StreamManager;
import org.mozilla.javascript.Scriptable;

/**
 * Create concreate class of IPreparedQuery
 */
class PreparedQueryUtil
{

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
				return new DummyPreparedQuery( queryDefn, dataEngine.getContext( ));
			}
			return newIVInstance( dataEngine, queryDefn );
		}

		IBaseDataSetDesign dset = dataEngine.getDataSetDesign( queryDefn.getDataSetName( ) );
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
			preparedQuery = new PreparedOdaDSQuery( dataEngine,
					queryDefn,
					dset,
					appContext );
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
	 * @param dataEngine
	 * @param queryDefn
	 * @return
	 * @throws DataException
	 */
	private static IPreparedQuery newIVInstance( DataEngineImpl dataEngine,
			IQueryDefinition queryDefn ) throws DataException
	{
		if ( runQueryOnRS( dataEngine, queryDefn ) )
			return new PreparedIVQuery( dataEngine, queryDefn );
		else
			return new PreparedIVDataSourceQuery( dataEngine, queryDefn );
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
	private static boolean runQueryOnRS( DataEngineImpl dataEngine,
			IQueryDefinition queryDefn ) throws DataException
	{
		if( !queryDefn.usesDetails( ) )
		{
			queryDefn.getSorts( ).clear( );
		}
		
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
		RDLoad rdLoad = RDUtil.newLoad( dataEngine.getContext( ),
				queryResultInfo );

		boolean runningOnRS = GroupDefnUtil.isEqualGroups( queryDefn.getGroups( ),
				rdLoad.loadGroupDefn( StreamManager.ROOT_STREAM,
						StreamManager.BASE_SCOPE ) );
		if ( runningOnRS == false )
			return false;

		runningOnRS = !hasAggregationInFilter( queryDefn.getFilters( ) );
		if ( runningOnRS == false )
			return false;

		runningOnRS = isCompatibleRSMap( rdLoad.loadQueryDefn( StreamManager.ROOT_STREAM,
				StreamManager.BASE_SCOPE )
				.getResultSetExpressions( ),
				queryDefn.getResultSetExpressions( ) );

		if ( runningOnRS == false )
			return false;
		
		runningOnRS = isCompatibleSubQuery( rdLoad.loadQueryDefn( StreamManager.ROOT_STREAM,
				StreamManager.BASE_SCOPE ),
				queryDefn );

		if ( runningOnRS == false )
			return false;

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
			return false;

		// TODO enhance me
		// If the following conditions hold, running on data set
		// 1.There are sorts that different from that of original design
		// 2.The query has subqueries.

		if ( hasSubquery( queryDefn ) )
		{
			if ( !QueryDefnUtil.isEqualSorts( queryDefn.getSorts( ),
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
				List groups = query.getGroups( );
				for ( int i = 0; i < groups.size( ); i++ )
				{
					List groupFilters = ( (IGroupDefinition) groups.get( i ) ).getFilters( );
					if ( groupFilters != null && groupFilters.size( ) > 0 )
					{
						runningOnRS = false;
						break;
					}
				}
				if ( runningOnRS == false )
					break;
			}
			
			

		}

		if ( runningOnRS == false )
			return false;

		if ( queryDefn.getFilters( ) != null
				&& queryDefn.getFilters( ).size( ) > 0 )
		{	
			if( !isFiltersEquals(filters, queryDefn.getFilters( )))
			runningOnRS = queryDefn.getResultSetExpressions( ).values( ) == null
					|| !hasAggregationOnRowObjects( queryDefn.getResultSetExpressions( )
							.values( )
							.iterator( ) );
		}
		return runningOnRS;
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
	 */
	private static boolean isCompatibleRSMap( Map oldMap, Map newMap )
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
				if( !isTwoExpressionEqual((IBaseExpression)newObj, (IBaseExpression)oldObj) )
					return false;
			}else
			{
				return false;
			}
 		}
		return true;
	}

	/**
	 * 
	 * @param obj1
	 * @param obj2
	 * @return
	 */
	private static boolean isTwoExpressionEqual( IBaseExpression obj1, IBaseExpression obj2 )
	{
		if( obj1 == null && obj2!= null )
			return false;
		if( obj1 != null && obj2 == null )
			return false;
		if ( obj1 == null && obj2 == null )
			return true;
		if ( !obj1.getClass( ).equals( obj2.getClass( ) ) )
			return false;
		
		if( obj1 instanceof IScriptExpression )
		{
			return isTwoExpressionEqual( (IScriptExpression)obj1, (IScriptExpression)obj2 );
		}else if ( obj1 instanceof IConditionalExpression )
		{
			return isTwoExpressionEqual( (IConditionalExpression)obj1, (IConditionalExpression)obj2 );
		}
		return false;
	}
	
	/**
	 * Return whether two IScriptExpression instance equals.
	 * @param obj1
	 * @param obj2
	 * @return
	 */
	private static boolean isTwoExpressionEqual( IScriptExpression obj1, IScriptExpression obj2 )
	{
		return isTwoStringEqual( obj1.getText( ), obj2.getText( ) )
				&& isTwoStringEqual( obj1.getGroupName( ), obj2.getGroupName( ))
				&& isTwoStringEqual( obj1.getText( ), obj2.getText( ))
				&& obj1.getDataType( ) == obj2.getDataType( );
	}
	
	/**
	 * 
	 * @param obj1
	 * @param obj2
	 * @return
	 */
	private static boolean isTwoExpressionEqual( IConditionalExpression obj1, IConditionalExpression obj2 )
	{
		if( obj1.getOperator( ) != obj2.getOperator( ) )
			return false;
		
		return isTwoStringEqual( obj1.getGroupName( ), obj2.getGroupName( ))
			 	&& isTwoExpressionEqual( obj1.getExpression( ), obj2.getExpression( ))
			 	&& isTwoExpressionEqual( obj1.getOperand1( ), obj2.getOperand1( ))
			 	&& isTwoExpressionEqual( obj1.getOperand2( ), obj2.getOperand2( ));
	}
	
	/**
	 * 
	 * @param s1
	 * @param s2
	 * @return
	 */
	private static boolean isTwoStringEqual( String s1, String s2 )
	{
		if( s1 == null && s2 != null )
			return false;
		
		if( s1 != null && s2 == null )
			return false;
		
		return s1.equals( s2 );
	}
	
	/**
	 * @param oldSubQuery
	 * @param newSubQuery
	 * @return
	 */
	private static boolean isCompatibleSubQuery( IBaseQueryDefinition oldDefn,
			IBaseQueryDefinition newDefn )
	{
		boolean isComp = QueryDefnUtil.isCompatibleSQs( oldDefn.getSubqueries( ),
				newDefn.getSubqueries( ) );

		if ( isComp == false )
			return false;

		Iterator oldIt = oldDefn.getGroups( ).iterator( );
		Iterator newIt = newDefn.getGroups( ).iterator( );
		while ( newIt.hasNext( ) )
		{
			IGroupDefinition oldGroupDefn = (IGroupDefinition) oldIt.next( );
			IGroupDefinition newGroupDefn = (IGroupDefinition) newIt.next( );
			isComp = QueryDefnUtil.isCompatibleSQs( oldGroupDefn.getSubqueries( ),
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
	 */
	private static boolean hasAggregationOnRowObjects( Iterator it )
	{
		while ( it.hasNext( ) )
		{
			Object o = it.next( );
			if ( ExpressionCompilerUtil.hasAggregationInExpr( (IBaseExpression) o ) )
			{
				return true;
			}
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
		private DataEngineContext context;
		
		/**
		 * 
		 * @param queryDefn
		 * @param context
		 */
		public DummyPreparedQuery( IQueryDefinition queryDefn, DataEngineContext context )
		{
			this.queryDefn = queryDefn;
			this.context = context;
		}
		
		/*
		 * (non-Javadoc)
		 * @see org.eclipse.birt.data.engine.api.IPreparedQuery#execute(org.mozilla.javascript.Scriptable)
		 */
		public IQueryResults execute( Scriptable queryScope )
				throws BirtException
		{
			return new CachedQueryResults( this.context,
					this.queryDefn.getQueryResultsID( ) );
		}

		/*
		 * (non-Javadoc)
		 * @see org.eclipse.birt.data.engine.api.IPreparedQuery#execute(org.eclipse.birt.data.engine.api.IQueryResults, org.mozilla.javascript.Scriptable)
		 */
		public IQueryResults execute( IQueryResults outerResults,
				Scriptable queryScope ) throws BirtException
		{
			throw new UnsupportedOperationException();
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
		
	}
}
