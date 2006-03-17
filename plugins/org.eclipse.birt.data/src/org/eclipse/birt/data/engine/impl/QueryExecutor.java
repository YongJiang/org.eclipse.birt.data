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
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.birt.core.data.DataType;
import org.eclipse.birt.data.engine.api.IBaseQueryDefinition;
import org.eclipse.birt.data.engine.api.IGroupDefinition;
import org.eclipse.birt.data.engine.api.IQueryResults;
import org.eclipse.birt.data.engine.api.IResultMetaData;
import org.eclipse.birt.data.engine.api.ISortDefinition;
import org.eclipse.birt.data.engine.api.querydefn.ComputedColumn;
import org.eclipse.birt.data.engine.api.script.IDataSourceInstanceHandle;
import org.eclipse.birt.data.engine.core.DataException;
import org.eclipse.birt.data.engine.executor.DataSetCacheManager;
import org.eclipse.birt.data.engine.executor.transform.IExpressionProcessor;
import org.eclipse.birt.data.engine.expression.ExpressionProcessor;
import org.eclipse.birt.data.engine.i18n.ResourceConstants;
import org.eclipse.birt.data.engine.impl.QueryExeutorUtil.ColumnInfo;
import org.eclipse.birt.data.engine.impl.aggregation.AggregateCalculator;
import org.eclipse.birt.data.engine.impl.aggregation.AggregateTable;
import org.eclipse.birt.data.engine.odi.ICandidateQuery;
import org.eclipse.birt.data.engine.odi.IDataSource;
import org.eclipse.birt.data.engine.odi.IPreparedDSQuery;
import org.eclipse.birt.data.engine.odi.IQuery;
import org.eclipse.birt.data.engine.odi.IResultIterator;
import org.eclipse.birt.data.engine.odi.IResultObjectEvent;
import org.eclipse.birt.data.engine.script.OnFetchScriptHelper;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

/**
 * 
 */
abstract class QueryExecutor implements IQueryExecutor
{	
	private 	Scriptable 				sharedScope;	
	private 	IBaseQueryDefinition 	baseQueryDefn;
	private 	AggregateTable 			aggrTable;
	private 	AggregateCalculator		aggregates;
	private		Scriptable				queryScope;
	
	/** Externally provided query scope; can be null */
	private		Scriptable				parentScope;

	private 	boolean 				isPrepared = false;
	private 	boolean					isExecuted = false;
	private		Map						queryAppContext;

	/** Query nesting level, 1 - outermost query */
	private		int						nestedLevel = 1;

	/** Runtime data source and data set used by this instance of executor */
	protected 	DataSourceRuntime		dataSource;	
	protected	DataSetRuntime			dataSet;
	
	protected 	IDataSource				odiDataSource;
	protected 	IQuery					odiQuery;
	
	/** Outer query's results; null if this query is not nested */
	protected	QueryResults			outerResults;	
	private 	IResultIterator			odiResult;
	
	private static Logger logger = Logger.getLogger( DataEngineImpl.class.getName( ) );
	
	/**
	 * @param sharedScope
	 * @param baseQueryDefn
	 * @param aggrTable
	 */
	QueryExecutor( Scriptable sharedScope, IBaseQueryDefinition baseQueryDefn,
			AggregateTable aggrTable )
	{
		this.sharedScope = sharedScope;
		this.baseQueryDefn = baseQueryDefn;
		this.aggrTable = aggrTable;
	}
	
	/**
	 * Provide the actual DataSourceRuntime used for the query.
	 * 
	 * @return
	 */
	abstract protected DataSourceRuntime findDataSource( )
			throws DataException;

	/**
	 * Create a new instance of data set runtime
	 * 
	 * @return
	 */
	abstract protected DataSetRuntime newDataSetRuntime( )
			throws DataException;

	
	/**
	 * Create a new unopened odiDataSource given the data source runtime
	 * definition
	 * 
	 * @return
	 */
	abstract protected IDataSource createOdiDataSource( )
			throws DataException;
	
	/**
	 * Create an empty instance of odi query
	 * 
	 * @return
	 */
	abstract protected IQuery createOdiQuery( ) throws DataException;
	
	/**
	 * Prepares the ODI query
	 */
	protected void prepareOdiQuery( ) throws DataException
	{
	}
	
	/**
	 * Executes the ODI query to reproduce a ODI result set
	 * 
	 * @return
	 */
	abstract protected IResultIterator executeOdiQuery( )
			throws DataException;
		
	/**
	 * @param context
	 */
	void setAppContext( Map context )
	{
	    queryAppContext = context;
	}
	
	/**
	 * Gets the Javascript scope for evaluating expressions for this query
	 * 
	 * @return
	 */
	Scriptable getQueryScope()
	{
		if ( queryScope == null )
		{
			// Set up a query scope. All expressions are evaluated against the 
			// Data set JS object as the prototype (so that it has access to all
			// data set properties). It uses a subscope of the externally provided
			// parent scope, or the global shared scope
			queryScope = newSubScope( parentScope );
			queryScope.setPrototype( dataSet.getJSDataSetObject() );
		}
		return queryScope;
	}
	
	/**
	 * Creates a subscope within parent scope
	 * @param parentScope parent scope. If null, the shared top-level scope is used as parent
	 */
	Scriptable newSubScope( Scriptable parentScope )
	{
		if ( parentScope == null )
			parentScope = sharedScope;
		
		Context cx = Context.enter( );
		try
		{
			Scriptable scope = cx.newObject( parentScope );
			scope.setParentScope( parentScope );
			scope.setPrototype( parentScope );
			return scope;
		}
		finally
		{
			Context.exit( );
		}
	}
	
	/**
	 * Prepare Executor so that it is ready to execute the query
	 * 
	 * @param outerRts
	 * @param targetScope
	 * @throws DataException
	 */
	void prepareExecution( IQueryResults outerRts, Scriptable targetScope ) throws DataException
	{
		if(isPrepared)return;
		
		this.parentScope = targetScope;
		dataSource = findDataSource( );

		if ( outerRts != null )
		{
			outerResults = ((QueryResults) outerRts );
			if ( outerResults.isClosed( ) )
			{
				// Outer result is closed; invalid
				throw new DataException( ResourceConstants.RESULT_CLOSED );
			}
			this.nestedLevel = outerResults.getNestedLevel( );
		}
		
		// Create the data set runtime
		// Since data set runtime contains the execution result, a new data set
		// runtime is needed for each execute
		dataSet = newDataSetRuntime();
		assert dataSet != null;
		
		openDataSource( );
		
		// Run beforeOpen script now so the script can modify the DataSetRuntime properties
		dataSet.beforeOpen();
					
		IExpressionProcessor exprProcessor = new ExpressionProcessor( null,
				null,
				dataSet,
				null );

		// Let subclass create a new and empty intance of the appropriate
		// odi IQuery
		odiQuery = createOdiQuery( );
		odiQuery.setExprProcessor( exprProcessor );
		populateOdiQuery( );
		prepareOdiQuery( );
		isPrepared = true;
	}
	
	/**
	 * Open the required DataSource. This method should be called after
	 * "dataSource" is initialized by findDataSource() method.
	 * 
	 * @throws DataException
	 */
	protected void openDataSource( ) throws DataException
	{
		assert odiDataSource == null;
		
		// Open the underlying data source
	    // dataSource = findDataSource( );
		if ( dataSource != null  )
		{
			// TODO: potential bug
			if ( !dataSource.isOpen( )
					|| DataSetCacheManager.getInstance( ).doesLoadFromCache( ) == true )
			{
				// Data source is not open; create an Odi Data Source and open it
				// We should run the beforeOpen script now to give it a chance to modify
				// runtime data source properties
				dataSource.beforeOpen();
				
				// Let subclass create a new unopened odi data source
				odiDataSource = createOdiDataSource( ); 
				
				// Passes thru the prepared query executor's 
				// context to the new odi data source
			    odiDataSource.setAppContext( queryAppContext );

				// Open the odi data source
				dataSource.openOdiDataSource( odiDataSource );
				
				dataSource.afterOpen();
			}
			else
			{
				// Use existing odiDataSource created for the data source runtime
				odiDataSource = dataSource.getOdiDataSource();
				
				// Passes thru the prepared query executor's 
				// current context to existing data source
			    odiDataSource.setAppContext( queryAppContext );
			}
		}
	}
	
	/**
	 * Populates odiQuery with this query's definitions
	 * 
	 * @throws DataException
	 */
	protected void populateOdiQuery( ) throws DataException
	{
		assert odiQuery != null;
		assert this.baseQueryDefn != null;
		
		Context cx = Context.enter();
		try
		{
			List temporaryComputedColumns = new ArrayList();
			
			// Set grouping
			List groups = this.baseQueryDefn.getGroups();
			if ( groups != null && ! groups.isEmpty() )
			{
				IQuery.GroupSpec[] groupSpecs = new IQuery.GroupSpec[ groups.size() ];
				Iterator it = groups.iterator();
				for ( int i = 0; it.hasNext(); i++ )
				{
					IGroupDefinition src = (IGroupDefinition) it.next();
					//TODO does the index of column significant?
					IQuery.GroupSpec dest = QueryExeutorUtil.groupDefnToSpec( cx,
							src,
							"_{$TEMP_GROUP_" + i + "$}_",
							-1 );
					groupSpecs[i] = dest;
					
					if( groupSpecs[i].isCompleteExpression() )
					{
						temporaryComputedColumns.add( new ComputedColumn( "_{$TEMP_GROUP_"
								+ i + "$}_",
								src.getKeyExpression( ),
								QueryExeutorUtil.getTempComputedColumnType( groupSpecs[i].getInterval( ) ) ) );
					}
				}
				odiQuery.setGrouping( Arrays.asList( groupSpecs));
			}		
			// Set sorting
			List sorts = this.baseQueryDefn.getSorts();
			if ( sorts != null && !sorts.isEmpty( ) )
			{
				IQuery.SortSpec[] sortSpecs = new IQuery.SortSpec[ sorts.size() ];
				Iterator it = sorts.iterator();
				for ( int i = 0; it.hasNext(); i++ )
				{
					ISortDefinition src = (ISortDefinition) it.next();
					int sortIndex = -1;
					String sortKey = src.getColumn();
					if ( sortKey == null || sortKey.length() == 0 )
					{ 
						//Firstly try to treat sort key as a column reference expression
						ColumnInfo columnInfo = QueryExeutorUtil.getColInfoFromJSExpr( cx,
								src.getExpression( ).getText( ) );
													
						sortIndex = columnInfo.getColumnIndex(); 
						sortKey = columnInfo.getColumnName( );
					}
					if ( sortKey == null && sortIndex < 0 )
					{
						//If failed to treate sort key as a column reference expression
						//then treat it as a computed column expression
						temporaryComputedColumns.add(new ComputedColumn( "_{$TEMP_SORT_"+i+"$}_", src.getExpression().getText(), DataType.ANY_TYPE));
						sortIndex = -1; 
						sortKey = String.valueOf("_{$TEMP_SORT_"+i+"$}_");
					}
					
					IQuery.SortSpec dest = new IQuery.SortSpec( sortIndex,
							sortKey,
							src.getSortDirection( ) == ISortDefinition.SORT_ASC );
					sortSpecs[i] = dest;
				}
				odiQuery.setOrdering( Arrays.asList( sortSpecs));
			}

			
			List computedColumns = null;
		    // set computed column event
			computedColumns = this.dataSet.getComputedColumns( );
			if ( computedColumns != null )
			{
				computedColumns.addAll( temporaryComputedColumns );
			}
			if ( (computedColumns != null && computedColumns.size() > 0)|| temporaryComputedColumns.size( ) > 0 )
			{
				IResultObjectEvent objectEvent = new ComputedColumnHelper( this.dataSet,
						(computedColumns == null&&computedColumns.size()>0) ? temporaryComputedColumns : computedColumns );
				odiQuery.addOnFetchEvent( objectEvent );
			}
	    	if ( dataSet.getEventHandler() != null )
	    	{
	    		OnFetchScriptHelper event = new OnFetchScriptHelper( dataSet ); 
	    		odiQuery.addOnFetchEvent( event );
		    }
		    
		    // set filter event
		    List dataSetFilters = new ArrayList( );
		    List queryFilters = new ArrayList( );
		    if ( dataSet.getFilters( ) != null )
			{
				dataSetFilters = dataSet.getFilters( );
			}
		    
		    if ( this.baseQueryDefn.getFilters( ) != null )
			{
		    	queryFilters = this.baseQueryDefn.getFilters( );
			}
		   		   			    
		    if ( dataSetFilters.size( ) + queryFilters.size( ) > 0 )
		    {
		    	IResultObjectEvent objectEvent = new FilterByRow( dataSetFilters, queryFilters,
		    			dataSet );
		    	odiQuery.addOnFetchEvent( objectEvent );
		    }
		    
			// specify max rows the query should fetch
		    odiQuery.setMaxRows( this.baseQueryDefn.getMaxRows() );
		}
		finally
		{
			Context.exit();
		}
	}
	
	/*
	 * @see org.eclipse.birt.data.engine.impl.IQueryExecutor#getResultMetaData()
	 */
	public IResultMetaData getResultMetaData( ) throws DataException
	{
		assert odiQuery instanceof IPreparedDSQuery
				|| odiQuery instanceof ICandidateQuery;
		if ( odiQuery instanceof IPreparedDSQuery )
		{
			if ( ( (IPreparedDSQuery) odiQuery ).getResultClass( ) != null )
				return new ResultMetaData( ( (IPreparedDSQuery) odiQuery ).getResultClass( ) );
			else
			    return null;
		}
		else
		{
			return new ResultMetaData( ( (ICandidateQuery) odiQuery ).getResultClass( ) );
		}
	}
	
	/*
	 * @see org.eclipse.birt.data.engine.impl.IQueryExecutor#execute()
	 */
	public void execute( ) throws DataException
	{
		logger.logp( Level.FINER,
				QueryExecutor.class.getName( ),
				"execute",
				"Start to execute" );

		if(this.isExecuted)
			return;

		// Execute the query
		odiResult = executeOdiQuery( );

		// Bind the row object to the odi result set
		this.dataSet.setResultSet( odiResult, false );
			
	    // Calculate aggregate values
	    aggregates = new AggregateCalculator( this.aggrTable, odiResult );
		    
	    // Calculate aggregate values
	    aggregates.calculate( getQueryScope() );
		
		this.isExecuted = true;
		
		logger.logp( Level.FINER,
				QueryExecutor.class.getName( ),
				"execute",
				"Finish executing" );
	}
	
	/*
	 * Closes the executor; release all odi resources
	 * 
	 * @see org.eclipse.birt.data.engine.impl.IQueryExecutor#close()
	 */
	public void close( )
	{
		if ( odiQuery == null )
		{
			// already closed
			logger.logp( Level.FINER,
					QueryExecutor.class.getName( ),
					"close",
					"executor closed " );
			return;
		}
		
	    // Close the data set and associated odi query
	    try
		{
    		dataSet.beforeClose();
		}
	    catch (DataException e )
		{
			logger.logp( Level.FINE,
					QueryExecutor.class.getName( ),
					"close",
					e.getMessage( ),
					e );
		}
	    
	    if ( odiResult != null )
	    	odiResult.close();
	    odiQuery.close();
	    
	    try
		{
    		dataSet.close();
		}
	    catch (DataException e )
		{
			logger.logp( Level.FINE,
					QueryExecutor.class.getName( ),
					"close",
					e.getMessage( ),
					e );
		}
	    
		odiQuery = null;
		odiDataSource = null;
		aggregates = null;
		odiResult = null;
		queryScope = null;
		isPrepared = false;
		isExecuted = false;
		
		// Note: reset dataSet and dataSource only after afterClose() is executed, since
		// the script may access these two objects
		try
		{
    		dataSet.afterClose();
		}
	    catch (DataException e )
		{
			logger.logp( Level.FINE,
						QueryExecutor.class.getName( ),
						"close",
						e.getMessage( ),
						e );
		}
	    dataSet = null;
		dataSource = null;
	    
		logger.logp( Level.FINER,
				QueryExecutor.class.getName( ),
				"close",
				"executor closed " );
	}

	/*
	 * @see org.eclipse.birt.data.engine.impl.IQueryExecutor#getDataSet()
	 */
	public DataSetRuntime getDataSet( )
	{
		return dataSet;
	}
	
	/*
	 * @see org.eclipse.birt.data.engine.impl.IQueryExecutor#getSharedScope()
	 */
	public Scriptable getSharedScope( )
	{
		return this.sharedScope;
	}
	
	/*
	 * @see org.eclipse.birt.data.engine.impl.IQueryExecutor#getNestedLevel()
	 */
	public int getNestedLevel( )
	{
		return this.nestedLevel;
	}

	/*
	 * @see org.eclipse.birt.data.engine.impl.IQueryExecutor#getDataSourceInstanceHandle()
	 */
	public IDataSourceInstanceHandle getDataSourceInstanceHandle( )
	{
		return this.dataSource;
	}

	/*
	 * @see org.eclipse.birt.data.engine.impl.IQueryExecutor#getJSAggrValueObject()
	 */
	public Scriptable getJSAggrValueObject( )
	{
		if ( aggregates != null )
			return aggregates.getJSAggrValueObject( );
		else
			return null;
	}
	
	/*
	 * @see org.eclipse.birt.data.engine.impl.IQueryExecutor#getNestedDataSets(int)
	 */
	public DataSetRuntime[] getNestedDataSets( int nestedCount )
	{
		return outerResults.getDataSetRuntime( nestedCount );
	}
	
	/*
	 * @see org.eclipse.birt.data.engine.impl.IQueryExecutor#getOdiResultSet()
	 */
	public IResultIterator getOdiResultSet( )
	{
		return this.odiResult;
	}
	
}
