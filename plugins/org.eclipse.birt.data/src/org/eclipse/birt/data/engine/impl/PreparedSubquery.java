/*
 *************************************************************************
 * Copyright (c) 2004 Actuate Corporation.
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
package org.eclipse.birt.data.engine.impl;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.birt.data.engine.api.DataEngineContext;
import org.eclipse.birt.data.engine.api.ISubqueryDefinition;
import org.eclipse.birt.data.engine.core.DataException;
import org.eclipse.birt.data.engine.executor.DataSourceFactory;
import org.eclipse.birt.data.engine.expression.ExpressionCompiler;
import org.eclipse.birt.data.engine.odi.ICandidateQuery;
import org.eclipse.birt.data.engine.odi.IDataSource;
import org.eclipse.birt.data.engine.odi.IEventHandler;
import org.eclipse.birt.data.engine.odi.IQuery;
import org.eclipse.birt.data.engine.odi.IResultIterator;
import org.mozilla.javascript.Scriptable;

/**
 * A prepared Sub query, which does not have its own data set, but rather
 * queries a subset of data produced by its a parent query.
 */
class PreparedSubquery implements IPreparedQueryService
{
	private int groupLevel;
	private PreparedQuery preparedQuery;
	private IPreparedQueryService queryService;
	
	private static Logger logger = Logger.getLogger( DataEngineImpl.class.getName( ) );
	
	/**
	 * @param subquery
	 *            Subquery definition
	 * @param parentQuery
	 *            Parent query (which can be a subquery itself, or a
	 *            PreparedReportQuery)
	 * @param groupLevel
	 *            Index of group in which this subquery is defined within the
	 *            parent query. If 0, subquery is defined outside of any groups.
	 * @throws DataException
	 */
	PreparedSubquery( DataEngineContext context, ExpressionCompiler exCompiler,
			Scriptable scope, ISubqueryDefinition subquery,
			IPreparedQueryService queryService, int groupLevel ) throws DataException
	{
		this.groupLevel = groupLevel;
		this.queryService = queryService;
		logger.logp( Level.FINER,
				PreparedSubquery.class.getName( ),
				"PreparedSubquery",
				"PreparedSubquery starts up." );
		
		this.preparedQuery = new PreparedQuery( context,
				exCompiler,
				scope,
				subquery,
				this,
				null );
	}
	
	/*
	 * @see org.eclipse.birt.data.engine.impl.IPreparedQueryService#getDataSourceQuery()
	 */
	public PreparedDataSourceQuery getDataSourceQuery( )
	{
		// Gets the parent's report query
		return queryService.getDataSourceQuery( );
	}
	
	/*
	 * @see org.eclipse.birt.data.engine.impl.IPreparedQueryService#execSubquery(org.eclipse.birt.data.engine.odi.IResultIterator,
	 *      java.lang.String, org.mozilla.javascript.Scriptable)
	 */
	public QueryResults execSubquery( IResultIterator iterator, String subQueryName, Scriptable subScope ) throws DataException
	{
		return this.preparedQuery.execSubquery( iterator, subQueryName, subScope );
	}
	
	/**
	 * @return group level of current sub query
	 */
	int getGroupLevel( )
	{
		return this.groupLevel;
	}
	
	/**
	 * Executes this subquery
	 * 
	 * @param parentIterator
	 * @param scope
	 * @return
	 * @throws DataException
	 */
	QueryResults execute( IResultIterator parentIterator, Scriptable scope ) 
		throws DataException
	{
		logger.logp( Level.FINER,
				PreparedSubquery.class.getName( ),
				"execute",
				"start to execute a PreparedSubquery." );
		try
		{
			return preparedQuery.doPrepare( null,
					scope,
					new SubQueryExecutor( parentIterator ),
					getDataSourceQuery( ) );
		}
		finally
		{
			logger.logp( Level.FINER,
					PreparedSubquery.class.getName( ),
					"execute",
					"finish executing a PreparedSubquery." );
		}
	}
	
	/**
	 * Concrete class of PreparedQuery.Executor used in PreparedSubquery
	 */
	private class SubQueryExecutor extends QueryExecutor
	{
		private IResultIterator parentIterator;
		
		/**
		 * @param parentIterator
		 */
		public SubQueryExecutor( IResultIterator parentIterator )
		{	
			super( preparedQuery.getSharedScope( ),
					preparedQuery.getBaseQueryDefn( ),
					preparedQuery.getAggrTable( ) );
			
			this.parentIterator = parentIterator;
		}
		
		/*
		 * @see org.eclipse.birt.data.engine.impl.PreparedQuery.Executor#createOdiDataSource()
		 */
		protected IDataSource createOdiDataSource( )
		{
			// Subqueries don't have its own data source
			return null;
		}
		
		/*
		 * @see org.eclipse.birt.data.engine.impl.PreparedQuery.Executor#findDataSource()
		 */
		protected DataSourceRuntime findDataSource( )
		{
			// Subqueries don't have its own data source
			return null;
		}
		
		/*
		 * @see org.eclipse.birt.data.engine.impl.PreparedQuery.Executor#newDataSetRuntime()
		 */
		protected DataSetRuntime newDataSetRuntime()
		{
			return new SubqueryDataSetRuntime( this );
		}
		
		/*
		 * @see org.eclipse.birt.data.engine.impl.PreparedQuery.Executor#createOdiQuery()
		 */
		protected IQuery createOdiQuery( ) throws DataException
		{
			// An empty odi data source is used for sub query data set
			return DataSourceFactory.getFactory( ).getDataSource( null,
					null,
					null,
					null ).newCandidateQuery( );
		}
		
		/*
		 * @see org.eclipse.birt.data.engine.impl.PreparedQuery.Executor#executeOdiQuery()
		 */
		protected IResultIterator executeOdiQuery( IEventHandler eventHandler ) 
				throws DataException
		{
			assert parentIterator != null;
	
			ICandidateQuery cdQuery = (ICandidateQuery) odiQuery; 
			cdQuery.setCandidates( parentIterator, groupLevel );
			IResultIterator ret = cdQuery.execute( eventHandler );
			parentIterator = null;
			
			return ret;
		}
	}
	
}
