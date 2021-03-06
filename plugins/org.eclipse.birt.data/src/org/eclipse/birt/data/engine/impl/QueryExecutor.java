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
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.birt.core.data.DataType;
import org.eclipse.birt.core.data.ExpressionUtil;
import org.eclipse.birt.core.exception.BirtException;
import org.eclipse.birt.core.script.ScriptContext;
import org.eclipse.birt.data.engine.api.DataEngineContext;
import org.eclipse.birt.data.engine.api.IBaseExpression;
import org.eclipse.birt.data.engine.api.IBaseQueryDefinition;
import org.eclipse.birt.data.engine.api.IBaseQueryResults;
import org.eclipse.birt.data.engine.api.IBinding;
import org.eclipse.birt.data.engine.api.IColumnDefinition;
import org.eclipse.birt.data.engine.api.IComputedColumn;
import org.eclipse.birt.data.engine.api.IConditionalExpression;
import org.eclipse.birt.data.engine.api.IDataScriptEngine;
import org.eclipse.birt.data.engine.api.IFilterDefinition;
import org.eclipse.birt.data.engine.api.IGroupDefinition;
import org.eclipse.birt.data.engine.api.IQueryDefinition;
import org.eclipse.birt.data.engine.api.IQueryExecutionHints;
import org.eclipse.birt.data.engine.api.IResultMetaData;
import org.eclipse.birt.data.engine.api.IScriptExpression;
import org.eclipse.birt.data.engine.api.ISortDefinition;
import org.eclipse.birt.data.engine.api.querydefn.Binding;
import org.eclipse.birt.data.engine.api.querydefn.ComputedColumn;
import org.eclipse.birt.data.engine.api.querydefn.ConditionalExpression;
import org.eclipse.birt.data.engine.api.querydefn.FilterDefinition;
import org.eclipse.birt.data.engine.api.querydefn.ScriptExpression;
import org.eclipse.birt.data.engine.api.script.IDataSourceInstanceHandle;
import org.eclipse.birt.data.engine.core.DataException;
import org.eclipse.birt.data.engine.executor.BaseQuery;
import org.eclipse.birt.data.engine.executor.JointDataSetQuery;
import org.eclipse.birt.data.engine.expression.ExpressionCompilerUtil;
import org.eclipse.birt.data.engine.expression.ExpressionProcessor;
import org.eclipse.birt.data.engine.i18n.ResourceConstants;
import org.eclipse.birt.data.engine.impl.aggregation.AggregateTable;
import org.eclipse.birt.data.engine.impl.group.GroupCalculatorFactory;
import org.eclipse.birt.data.engine.odi.ICandidateQuery;
import org.eclipse.birt.data.engine.odi.IDataSource;
import org.eclipse.birt.data.engine.odi.IEventHandler;
import org.eclipse.birt.data.engine.odi.IPreparedDSQuery;
import org.eclipse.birt.data.engine.odi.IQuery;
import org.eclipse.birt.data.engine.odi.IResultClass;
import org.eclipse.birt.data.engine.odi.IResultIterator;
import org.eclipse.birt.data.engine.odi.IResultObjectEvent;
import org.eclipse.birt.data.engine.olap.api.ICubeQueryResults;
import org.eclipse.birt.data.engine.olap.script.JSCubeBindingObject;
import org.eclipse.birt.data.engine.script.OnFetchScriptHelper;
import org.eclipse.birt.data.engine.script.ScriptConstants;
import org.mozilla.javascript.Scriptable;

import com.ibm.icu.text.Collator;

/**
 * 
 */
public abstract class QueryExecutor implements IQueryExecutor
{

	private IBaseQueryDefinition baseQueryDefn;
	private AggregateTable aggrTable;
	
	// from PreparedQuery->PreparedDataSourceQuery->DataEngineImpl
	private Scriptable sharedScope;
	/** Externally provided query scope; can be null */
	// from PreparedQuery->PreparedDataSourceQuery
	private Scriptable parentScope;

	// for query execution
	private Scriptable queryScope;

	private boolean isPrepared = false;
	private boolean isExecuted = false;
	
	private boolean loadFromCache;
	
	private Map queryAppContext;

	/** Query nesting level, 1 - outermost query */
	private int nestedLevel = 1;

	/** Runtime data source and data set used by this instance of executor */
	protected DataSourceRuntime dataSource;
	protected DataSetRuntime dataSet;

	protected IDataSource odiDataSource;
	protected IQuery odiQuery;

	/** Outer query's results; null if this query is not nested */
	protected IQueryService tabularOuterResults;
	private IResultIterator odiResult;
	private IExecutorHelper parentHelper;
	private DataEngineSession session;
	protected List temporaryComputedColumns = new ArrayList( );
	private static Logger logger = Logger.getLogger( QueryExecutor.class.getName( ) );
	protected IQueryContextVisitor contextVisitor;
	
	/**
	 * @param sharedScope
	 * @param baseQueryDefn
	 * @param aggrTable
	 */
	QueryExecutor( Scriptable sharedScope, IBaseQueryDefinition baseQueryDefn,
			AggregateTable aggrTable, DataEngineSession session, IQueryContextVisitor contextVisitor )
	{
		Object[] params = {
				sharedScope, baseQueryDefn, aggrTable, session
		};
		logger.entering( QueryExecutor.class.getName( ),
				"QueryExecutor",
				params );
		this.sharedScope = sharedScope;
		this.baseQueryDefn = baseQueryDefn;
		this.aggrTable = aggrTable;
		this.session = session;
		this.contextVisitor = contextVisitor;
		logger.exiting( QueryExecutor.class.getName( ), "QueryExecutor" );
	}

	public IQueryContextVisitor getQueryContextVisitor()
	{
		return this.contextVisitor;
	}
	/**
	 * Provide the actual DataSourceRuntime used for the query.
	 * 
	 * @return
	 */
	abstract protected DataSourceRuntime findDataSource( ) throws DataException;

	/**
	 * Create a new instance of data set runtime
	 * 
	 * @return
	 */
	abstract protected DataSetRuntime newDataSetRuntime( ) throws DataException;

	/**
	 * Create a new unopened odiDataSource given the data source runtime
	 * definition
	 * 
	 * @return
	 */
	abstract protected IDataSource createOdiDataSource( ) throws DataException;

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
	 * 
	 * @throws DataException
	 */
	protected void dataSourceBeforeOpen( ) throws DataException
	{
		if ( !this.loadFromCache )
		{
			this.dataSource.beforeOpen( );
		}
	}
	
	/**
	 * 
	 * @throws DataException
	 */
	protected void dataSourceAfterOpen( ) throws DataException
	{
		if ( !this.loadFromCache )
		{
			this.dataSource.afterOpen( );
		}
	}

	/**
	 * 
	 * @throws DataException
	 */
	protected void dataSetBeforeOpen( ) throws DataException
	{
		if ( !this.loadFromCache )
		{
			this.dataSet.beforeOpen( );
		}
	}
	
	/**
	 * 
	 * @throws DataException
	 */
	protected void dataSetAfterOpen( ) throws DataException
	{
		if ( !this.loadFromCache )
		{
			this.dataSet.afterOpen( );
		}
	}
	
	/**
	 * 
	 * @throws DataException
	 */
	protected void dataSetBeforeClose( ) throws DataException
	{
		if ( !this.loadFromCache )
		{
			dataSet.beforeClose( );
		}
	}
	
	/**
	 * 
	 * @throws DataException
	 */
	protected void dataSetAfterClose( ) throws DataException
	{
		if ( !this.loadFromCache )
		{
			this.dataSet.afterClose( );
		}
	}
	
	/**
	 * Executes the ODI query to reproduce a ODI result set
	 * @param eventHandler 
	 * @param stopSign
	 * @return
	 */
	abstract protected IResultIterator executeOdiQuery(
			IEventHandler eventHandler ) throws DataException;

	/**
	 * @param context
	 */
	void setAppContext( Map context )
	{
		queryAppContext = context;
	}

	/**
	 * Prepare Executor so that it is ready to execute the query
	 * 
	 * @param outerRts
	 * @param targetScope
	 * @throws DataException
	 */
	void prepareExecution( IBaseQueryResults outerRts, Scriptable targetScope )
			throws DataException
	{
		if ( isPrepared )
			return;

		this.parentScope = targetScope;
		dataSource = findDataSource( );

		if ( outerRts != null && ( outerRts instanceof IQueryService || outerRts instanceof ICubeQueryResults ))
		{
			if ( outerRts instanceof IQueryService )
			{
				tabularOuterResults = ( (IQueryService) outerRts );
				if ( tabularOuterResults.isClosed( ) )
				{
					// Outer result is closed; invalid
					throw new DataException( ResourceConstants.RESULT_CLOSED );
				}
				this.nestedLevel = tabularOuterResults.getNestedLevel( );
				// TODO: check helper is null
				IExecutorHelper helper = tabularOuterResults.getExecutorHelper( );
				this.setParentExecutorHelper( helper );
			}
			else if( outerRts instanceof ICubeQueryResults )
			{
				ExecutorHelper helper = new ExecutorHelper( null );
				helper.setScriptable( new JSCubeBindingObject( ( (ICubeQueryResults) outerRts ).getCubeCursor( ) ) );
				this.setParentExecutorHelper( helper );
			}
		}

		// Create the data set runtime
		// Since data set runtime contains the execution result, a new data set
		// runtime is needed for each execute
		dataSet = newDataSetRuntime( );
		assert dataSet != null;	
		
		//For cached data set, we need not execute any scripts.
		loadFromCache = loadFromCache( );
		
		dataSet.setFromCache( loadFromCache );
		
		openDataSource( );
		
		// Run beforeOpen script now so the script can modify the
		// DataSetRuntime properties
		dataSetBeforeOpen( );

		// Let subclass create a new and empty intance of the appropriate
		// odi IQuery
		odiQuery = createOdiQuery( );
		odiQuery.setDistinctValueFlag( dataSet.needDistinctValue( ) );
		odiQuery.setQueryDefinition( this.baseQueryDefn );
		odiQuery.setExprProcessor( new ExpressionProcessor( dataSet ) );
		
		//Set the row fetch limit for the IQuery instance.The row fetch limit
		//is the number of rows that a data set can fetch from data source.
		if( dataSet.getDesign( ) != null )
		{
			//When it is not a subquery, the property "row fetch limit" should be applied
			//to the query.
			odiQuery.setRowFetchLimit( dataSet.getDesign( ).getRowFetchLimit( ) );
		}
		populateOdiQuery( );
		try
		{
			prepareOdiQuery( );
		}
		catch ( DataException e )
		{
			throw new DataException( ResourceConstants.FAIL_PREPARE_EXECUTION,
					e,
					dataSet.getName( ) );
		}
		isPrepared = true;
	}
	
	/**
	 * 
	 * @return
	 * @throws DataException
	 */
	private boolean loadFromCache( ) throws DataException
	{
		if( this.dataSource == null )
			return false;
		if ( !( this.baseQueryDefn instanceof IQueryDefinition ) )
			return false;
		return this.session.getDataSetCacheManager( )
			.doesLoadFromCache( ((DataEngineImpl)session.getEngine( )).getDataSourceDesign( this.dataSet.getDesign( ).getDataSourceName( ) ),
				this.dataSet.getDesign( ),
				new ParameterUtil( this.tabularOuterResults == null
						? null
						: this.tabularOuterResults.getQueryScope( ),
						this.dataSet,
						( IQueryDefinition )this.baseQueryDefn,
						this.getQueryScope( ),
						session.getEngineContext( ).getScriptContext( )).resolveDataSetParameters( true ),
				this.queryAppContext );
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
		if ( dataSource != null )
		{
			// TODO: potential bug
			if ( !dataSource.isOpen( )
					|| session.getDataSetCacheManager( ).needsToCache( ))
			{
				// Data source is not open; create an Odi Data Source and open it
				// We should run the beforeOpen script now to give it a chance to modify
				// runtime data source properties
				dataSourceBeforeOpen( );

				// Let subclass create a new unopened odi data source
				odiDataSource = createOdiDataSource( );

				// Passes thru the prepared query executor's 
				// context to the new odi data source
				odiDataSource.setAppContext( queryAppContext );

				// Open the odi data source
				dataSource.openOdiDataSource( odiDataSource );

				dataSourceAfterOpen( );
			}
			else
			{
				// Use existing odiDataSource created for the data source runtime
				odiDataSource = dataSource.getOdiDataSource( );

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
		
		SortingOptimizer opt = new SortingOptimizer();

		// Set grouping
		populateGrouping( session.getEngineContext( ).getScriptContext( ), opt );

		// Set sorting
		populateSorting( opt );

		// set fetch event
		populateFetchEvent( session.getEngineContext( ).getScriptContext( ) );

		// specify max rows the query should fetch
		odiQuery.setMaxRows( this.baseQueryDefn.getMaxRows( ) );

		prepareCacheQuery( this.odiQuery );
		
	}

	/**
	 * TODO: enhance me, this is only a temp logic
	 * Set temporary computed columns to DataSourceQuery where cache is used
	 */
	protected void prepareCacheQuery( IQuery odiQuery )
	{
		if ( temporaryComputedColumns != null
				&& temporaryComputedColumns.size( ) > 0 )
		{
			if ( odiQuery instanceof org.eclipse.birt.data.engine.executor.dscache.DataSourceQuery )
			{
				( (org.eclipse.birt.data.engine.executor.dscache.DataSourceQuery) odiQuery ).setTempComputedColumn( this.temporaryComputedColumns );
			}
			else if ( odiQuery instanceof org.eclipse.birt.data.engine.executor.dscache.CandidateQuery )
			{
				( (org.eclipse.birt.data.engine.executor.dscache.CandidateQuery) odiQuery ).setTempComputedColumn( this.temporaryComputedColumns );
			}
		}
	}
	
	/**
	 * Populate grouping to the query.
	 * 
	 * @param cx
	 * @throws DataException
	 */
	private void populateGrouping( ScriptContext cx, SortingOptimizer opt ) throws DataException
	{
		List groups = this.baseQueryDefn.getGroups( );
		if ( groups != null && !groups.isEmpty( ) )
		{
			IQuery.GroupSpec[] groupSpecs = new IQuery.GroupSpec[groups.size( )];
			Iterator it = groups.iterator( );
			for ( int i = 0; it.hasNext( ); i++ )
			{
				IGroupDefinition src = (IGroupDefinition) it.next( );

				validateGroupExpression( src );

				String expr = getDataSetGroupKeyExpression( src );
				
				String groupName = populateGroupName( i, expr );
				int dataType = getColumnDataType( cx, getGroupKeyExpression( src ) );
				
				IQuery.GroupSpec dest = QueryExecutorUtil.groupDefnToSpec( cx,
						src,
						expr,
						groupName,
						-1,
						dataType,
						this.session.getEngineContext( ).getMode( ) == DataEngineContext.MODE_UPDATE? true:(
						src.getSortDirection( ) == IGroupDefinition.NO_SORT ? false:(
						this.baseQueryDefn.getQueryExecutionHints( ) == null 
								? true
								: this.baseQueryDefn.getQueryExecutionHints( )
										.doSortBeforeGrouping( ) )));
				
				groupSpecs[i] = dest;
				this.temporaryComputedColumns.add( getComputedColumnInstance( cx,
						groupSpecs[i].getInterval( ),
						src,
						expr,
						groupName,
						dest,
						dataType) );

			}

			if ( opt.acceptGroupSorting( ) )
			{
				for ( int i = 0; i < groupSpecs.length; i++ )
				{
					IQuery.GroupSpec spec = groupSpecs[i];
					spec.setSortDirection( IGroupDefinition.NO_SORT );
				}
			}
			odiQuery.setGrouping( Arrays.asList( groupSpecs ) );
		}
	}

	/**
	 * Validating the group expression.
	 * 
	 * @param src
	 * @throws DataException
	 */
	private void validateGroupExpression( IGroupDefinition src ) throws DataException
	{
		if ( ( src.getKeyColumn( ) == null || src.getKeyColumn( )
				.trim( )
				.length( ) == 0 )
				&& ( src.getKeyExpression( ) == null || src.getKeyExpression( )
						.trim( )
						.length( ) == 0 ) )
			throw new DataException( ResourceConstants.BAD_GROUP_EXPRESSION );
	}
	

	/**
	 * Populate the group name according to the given expression.
	 * 
	 * @param i
	 * @param expr
	 * @return
	 */
	private String populateGroupName( int i, String expr )
	{
		String groupName;
		if ( expr.trim( ).equalsIgnoreCase( "row[0]" )
				|| expr.trim( ).equalsIgnoreCase( "row._rowPosition" )
				|| expr.trim( ).equalsIgnoreCase( "dataSetRow[0]" )
				|| expr.trim( )
						.equalsIgnoreCase( "dataSetRow._rowPosition" ) )
		{
			groupName = "_{$TEMP_GROUP_" + i + "ROWID$}_";
		}
		else
		{
			groupName = "_{$TEMP_GROUP_" + i + "$}_";
		}
		return groupName;
	}

	/**
	 * Get the computed column instance according to the group type.If group has
	 * interval, return GroupComputedColumn, otherwise return normal computed
	 * column.
	 * 
	 * @param cx
	 * @param groupSpecs
	 * @param i
	 * @param src
	 * @param expr
	 * @param groupName
	 * @param dest
	 * @return
	 * @throws DataException
	 */
	private IComputedColumn getComputedColumnInstance( ScriptContext cx,
			int interval, IGroupDefinition src,
			String expr, String groupName, IQuery.GroupSpec dest,
			int dataType)
			throws DataException
	{
		if ( dest.getInterval( ) != IGroupDefinition.NO_INTERVAL )
		{
			return new GroupComputedColumn( groupName,
					expr,
					dataType == DataType.DECIMAL_TYPE ? dataType : QueryExecutorUtil.getTempComputedColumnType( interval ),
					GroupCalculatorFactory.getGroupCalculator( src.getInterval( ),
							src.getIntervalStart( ),
							src.getIntervalRange( ),
							dataType,
							session.getEngineContext( ).getLocale( ),
							session.getEngineContext( ).getTimeZone( )) );

		}
		else
		{
			return new ComputedColumn( groupName,
					expr,
					dataType );
		}
	}

	/**
	 * Populate the sortings in a query.
	 * 
	 * @throws DataException
	 */
	private void populateSorting( SortingOptimizer opt ) throws DataException
	{
		if ( opt.acceptQuerySorting( ) )
			return;

		populateQuerySorting( );
	}
	
	class SortingOptimizer{
		private List<ISortDefinition> sortHints;
		private List<ISortDefinition> sortings;
		private List<IGroupDefinition> groups;
		private IQueryExecutionHints queryExeHint;
		private SortMatcher hintsMatcher;
		private boolean optimizeGroupSorting = false;
		private boolean optimizeQuerySorting = false;
		
		public SortingOptimizer ( )
		{
			this.sortHints =  dataSet.getSortHints( );
			this.groups = baseQueryDefn.getGroups( );
			this.sortings = baseQueryDefn.getSorts( );
			this.queryExeHint = baseQueryDefn.getQueryExecutionHints( );
			hintsMatcher = new SortMatcher( sortHints );
		}
		
		public boolean acceptGroupSorting( )
		{
			// No sort hint and no group, no optimize
			if ( sortHints == null || groups == null)
				return false;
			
			// No group sorting, no optimize
			if ( queryExeHint != null && !queryExeHint.doSortBeforeGrouping( ) )
				return false;
			
			// Contains group interval, no optimize
			for ( Object o : groups )
			{
				IGroupDefinition g = (IGroupDefinition) o;
				if ( g.getInterval( ) != IGroupDefinition.NO_INTERVAL )
					return false;
			}
			
			if ( sortings != null )
			{
				// Merge group key sorting and query sorting into one sorting sequence 
				GroupSortingCaculator calc = new GroupSortingCaculator( groups );
				List<?> sorts = calc.getSortingSequence( sortings,
						new SortDefnMatchInfo( ) );
				if ( hintsMatcher.match( sorts,
						new GroupDefnSortDefnMatchInfo( ) ) )
				{
					optimizeGroupSorting = true;
					optimizeQuerySorting = true;
				}
			}
			else
			{
				if ( hintsMatcher.match( groups,
						new GroupDefnMatchInfo( ) ) )
					optimizeGroupSorting = true;
			}
			
			return optimizeGroupSorting;			
		}
		
		public boolean acceptQuerySorting() throws DataException
		{
			if ( sortHints == null || sortings == null )
			{
				return false;
			}
			
			if ( optimizeQuerySorting )
				return true;

			if ( hintsMatcher.match( baseQueryDefn.getSorts( ),
					new SortDefnMatchInfo( ) ) )
				return true;
			
			return false;
		}
	}
	
	class SortMatcher
	{
		private List<?> hints = null;
		private MatchInfo hInfo = null;

		public SortMatcher( List<?> sortHints, MatchInfo info )
		{
			this.hints = sortHints;
			hInfo = info;
		}
		
		public SortMatcher( List<?> sortHints )
		{
			this (sortHints, new SortHintMatchInfo());
		}

		public boolean match( List<?> sorts, MatchInfo util )
		{
			if ( hints == null || hints.size( ) == 0 || hints.size( ) < sorts.size( ))
				return false;

			int pos = 0;
			for ( ; pos < hints.size( ) && pos < sorts.size( ); )
			{
				String hKey = hInfo.getKey( hints.get( pos ) );
				int hDirection = hInfo.getDirection( hints.get( pos ) );
				String mKey = util.getKey( sorts.get( pos ) );
				int mDirection = util.getDirection( sorts.get( pos ) );
				if ( hKey != null
						&& mKey != null && hKey.equals( mKey )
						&& hDirection == mDirection )
				{
					pos++;
				}
				else
				{
					break;
				}
			}

			// For sortHints and sorting definitions:
			// 1. SortHints contains all sorts conditions.
			// 2. Sorts sequence match sortHints start from the first sort hint.
			if ( pos == sorts.size( ) )
				return true;

			return false;
		}
	}
	
	class GroupSortingCaculator
	{
		private List<?> base = null;
		private MatchInfo hInfo = null;
		private List<?> compareSorts = null;

		public GroupSortingCaculator( List<?> sortHints, MatchInfo info )
		{
			this.base = sortHints;
			hInfo = info;
		}

		public GroupSortingCaculator( List<?> sortHints )
		{
			this( sortHints, new GroupDefnMatchInfo( ) );
		}

		private void caculate( List<?> sorts, MatchInfo util )
		{
			if ( base == null || base.size( ) == 0 )
				return;

			int pos = 0;
			int j = 0;
			for ( ; pos < base.size( ) && j < sorts.size( ); )
			{
				String hKey = hInfo.getKey( base.get( pos ) );
				int hDirection = hInfo.getDirection( base.get( pos ) );
				String mKey = util.getKey( sorts.get( j ) );
				int mDirection = util.getDirection( sorts.get( j ) );

				if ( hKey != null
						&& mKey != null && hKey.equals( mKey )
						&& hDirection == mDirection )
				{
					pos++;
					j++;
				}
				else
				{
					pos++;
				}
			}

			compareSorts = sorts.subList( j, sorts.size( ) );
		}

		@SuppressWarnings({
				"rawtypes", "unchecked"
		})
		public List getSortingSequence( List<?> querySorts, MatchInfo util )
		{
			caculate( querySorts, util );

			ArrayList sorts = new ArrayList( );
			Iterator it = base.iterator( );
			while ( it.hasNext( ) )
			{
				sorts.add( it.next( ) );
			}

			if ( compareSorts != null )
			{
				it = compareSorts.iterator( );
				while ( it.hasNext( ) )
				{
					sorts.add( it.next( ) );
				}
			}
			return sorts;
		}
	}
	
	interface MatchInfo
	{
		public String getKey( Object o );

		public int getDirection( Object o );
	}

	class SortHintMatchInfo implements MatchInfo
	{
		public String getKey( Object o )
		{
			ISortDefinition sort = (ISortDefinition) o;
			String key = sort.getColumn( );
			if ( key == null )
				key = sort.getExpression( ).getText( );
			return key;
		}

		public int getDirection( Object o )
		{
			ISortDefinition sort = (ISortDefinition) o;
			return sort.getSortDirection( );
		}
	}

	class SortDefnMatchInfo implements MatchInfo
	{
		public String getKey( Object o )
		{
			ISortDefinition sort = (ISortDefinition) o;
			
			// No matching while sorting with local and strength.
			if ( sort.getSortLocale( ) != null
					|| sort.getSortStrength( ) != ISortDefinition.ASCII_SORT_STRENGTH )
				return null;
			
			String sortKey = sort.getColumn( );
			if ( sortKey == null )
				sortKey = sort.getExpression( ).getText( );
			else
				sortKey = getColumnRefExpression( sortKey );

			return getResolvedExpression( sortKey );
		}

		public int getDirection( Object o )
		{
			ISortDefinition sort = (ISortDefinition) o;
			return sort.getSortDirection( );
		}
	}
	
	class GroupDefnMatchInfo implements MatchInfo
	{
		public String getKey( Object o )
		{
			IGroupDefinition grp = (IGroupDefinition) o;
			String rowExpr = getGroupKeyExpression( grp );
			return getResolvedExpression( rowExpr );
		}

		public int getDirection( Object o )
		{
			IGroupDefinition grp = (IGroupDefinition) o;
			return grp.getSortDirection( );
		}
	}
	
	class GroupDefnSortDefnMatchInfo implements MatchInfo
	{
		private MatchInfo grpInfo = new GroupDefnMatchInfo();
		private MatchInfo sortInfo = new SortDefnMatchInfo();
		
		public String getKey( Object o )
		{
			if ( o instanceof IGroupDefinition)
				return grpInfo.getKey( o );
			if ( o instanceof ISortDefinition )
				return sortInfo.getKey( o );
			return null;
		}

		public int getDirection( Object o )
		{
			if ( o instanceof IGroupDefinition)
				return grpInfo.getDirection( o );
			if ( o instanceof ISortDefinition )
				return sortInfo.getDirection( o );
			return IGroupDefinition.NO_SORT;
		}
	}
	
	
	private String resolveDataSetExpr( String rowExpr ) throws DataException
	{
		if ( rowExpr == null)
			return null;
		
		String dataSetExpr = null ;
		try
		{
			String bindingName = ExpressionUtil.getColumnBindingName( rowExpr );
			Object binding = this.baseQueryDefn.getBindings( ).get( bindingName );
			if( binding != null )
			{
				IBaseExpression expr = ( (IBinding) binding ).getExpression( );
				if( expr != null && expr instanceof IScriptExpression )
				{
					dataSetExpr = ( (IScriptExpression) expr ).getText( );
					if ( dataSetExpr != null )
					{
						return resolveDataSetExpr( dataSetExpr );
					}
				}
				return dataSetExpr;
			}
			else
				return rowExpr; // Already resolved.
		}
		catch ( BirtException e )
		{
			throw DataException.wrap( e );
		}
	}
	
	@SuppressWarnings("rawtypes")
	private String resolveColumnAlias( String columnAlias )
	{
		List rsHints = this.getDataSet( ).getResultSetHints( );
		if ( rsHints == null )
			return null;

		String resolved = null;
		IColumnDefinition col = null;
		Iterator itr = rsHints.iterator( );
		while ( itr.hasNext( ) )
		{
			col = (IColumnDefinition) itr.next( );
			if ( col.getAlias( ) != null
					&& col.getAlias( ).equals( columnAlias ) )
			{
				resolved = col.getColumnName( );
				break;
			}
		}
		return resolved;
	}
	
	private String getResolvedExpression( String rowExpr )
	{
		String expr = null;
		try
		{
			expr = resolveDataSetExpr( rowExpr );
			if ( expr != null )
			{
				String bindingName = ExpressionUtil.getColumnName( expr );
				String column = resolveColumnAlias( bindingName );
				if ( column != null ) // Binding name is a column alias
					expr = ExpressionUtil.createDataSetRowExpression( column );
			}
		}
		catch ( BirtException ignore )
		{
			expr = null;
		}
		return expr;
	}
	
	private void populateQuerySorting( ) throws DataException
	{
		List<?> sorts = this.baseQueryDefn.getSorts( );
		if ( sorts != null && !sorts.isEmpty( ) )
		{
			IQuery.SortSpec[] sortSpecs = new IQuery.SortSpec[sorts.size( )];
			Iterator<?> it = sorts.iterator( );
			for ( int i = 0; it.hasNext( ); i++ )
			{
				ISortDefinition src = (ISortDefinition) it.next( );
				int sortIndex = -1;
				String sortKey = src.getColumn( );
				if ( sortKey == null )
				{
					sortKey = src.getExpression( ).getText( );
				}
				else
				{
					sortKey = getColumnRefExpression( sortKey );
				}
				String dataSetExpr = getDataSetExpr(sortKey);
				temporaryComputedColumns.add( new ComputedColumn( "_{$TEMP_SORT_"
						+ i + "$}_",
						dataSetExpr == null ? sortKey : dataSetExpr,
						getExpressionDataType( sortKey ) ) );
				sortIndex = -1;
				sortKey = String.valueOf( "_{$TEMP_SORT_" + i + "$}_" );

				IQuery.SortSpec dest = new IQuery.SortSpec( sortIndex,
						sortKey,
						src.getSortDirection( ) == ISortDefinition.SORT_ASC,
						createCollator( src )
					 );
				sortSpecs[i] = dest;
			}
			odiQuery.setOrdering( Arrays.asList( sortSpecs ) );
		}
	}
	
	private Collator createCollator( ISortDefinition sd )
	{
		if ( sd.getSortStrength( ) != -1 )
		{
			Collator c = Collator.getInstance( sd.getSortLocale( ) == null
							? session.getEngineContext( ).getLocale( )
									: sd.getSortLocale( ) );
			c.setStrength( sd.getSortStrength( ) );
			return c;
		}
		return null;
	}

	private String getDataSetExpr( String rowExpr ) throws DataException
	{
		String dataSetExpr = null ;
		try
		{
			String bindingName = ExpressionUtil.getColumnBindingName( rowExpr );
			Object binding = this.baseQueryDefn.getBindings( ).get( bindingName );
			if( binding != null )
			{
				IBaseExpression expr = ( (IBinding) binding ).getExpression( );
				if( expr != null && expr instanceof IScriptExpression )
				{
					dataSetExpr = ( ( IScriptExpression )expr ).getText( );
				}
			}
			return dataSetExpr;
		}
		catch ( BirtException e )
		{
			throw DataException.wrap( e );
		}
	}

	/**
	 * 
	 * @param expression
	 * @return
	 * @throws DataException
	 */
	private int getExpressionDataType( String expression ) throws DataException
	{
		try
		{
			if( expression == null )
				return DataType.ANY_TYPE;
			String bindingName = ExpressionUtil.getColumnBindingName( expression );
			if( bindingName == null )
				return DataType.ANY_TYPE;
			if ( bindingName.equals( ScriptConstants.ROW_NUM_KEYWORD ) )
				return DataType.INTEGER_TYPE;
			Object binding = this.baseQueryDefn.getBindings( ).get( bindingName );
			if( binding == null )
				return DataType.ANY_TYPE;
			int dataType = ( (IBinding) binding ).getDataType( );
			if( dataType != DataType.UNKNOWN_TYPE )
				return dataType;
			else
				return DataType.ANY_TYPE;
		}
		catch ( BirtException e )
		{
			throw DataException.wrap( e );
		}
	}
	
	/**
	 * 
	 * @param cx
	 * @throws DataException
	 */
	private void populateFetchEvent( ScriptContext cx ) throws DataException
	{
		List dataSetFilters = new ArrayList( );
		List queryFilters = new ArrayList( );
		List aggrFilters = new ArrayList( );
		List dataSetAggrFilters = new ArrayList( );
		if ( dataSet.getFilters( ) != null )
		{
			Map bindings = createBindingFromComputedColumn( dataSet.getComputedColumns( ));
			for ( int i = 0; i < dataSet.getFilters( ).size( ); i++ )
			{
				if ( QueryExecutorUtil.isAggrFilter( (IFilterDefinition) dataSet.getFilters( )
						.get( i ),
						bindings ) )
				{
					dataSetAggrFilters.add( dataSet.getFilters( ).get( i ) );
				}
				else
				{
					dataSetFilters.add( dataSet.getFilters( ).get( i ) );
				}
			}
		}

		if ( this.baseQueryDefn.getFilters( ) != null )
		{
			for ( int i = 0; i < this.baseQueryDefn.getFilters( ).size( ); i++ )
			{
				if ( QueryExecutorUtil.isAggrFilter( (IFilterDefinition) this.baseQueryDefn.getFilters( )
						.get( i ),
						this.baseQueryDefn.getBindings( ) ) )
				{
					aggrFilters.add( this.baseQueryDefn.getFilters( ).get( i ) );
				}
				else
				{
					queryFilters.add( this.baseQueryDefn.getFilters( ).get( i ) );
				}
			}
		}

		//When prepare filters, the temporaryComputedColumns would also be effect.
		List multipassFilters = prepareFilters( cx,
				dataSetFilters,
				queryFilters,
				temporaryComputedColumns );

		//******************populate the onFetchEvent below**********************/		    
		List computedColumns = null;
		// set computed column event
		computedColumns = this.dataSet.getComputedColumns( );
		if ( computedColumns == null )
			computedColumns = new ArrayList( );
		if ( computedColumns.size( ) > 0
				|| temporaryComputedColumns.size( ) > 0 )
		{
			IResultObjectEvent objectEvent = new ComputedColumnHelper( this.dataSet,
					computedColumns,
					temporaryComputedColumns, cx );
			odiQuery.addOnFetchEvent( objectEvent );
			this.dataSet.getComputedColumns( )
					.addAll( temporaryComputedColumns );
		}
		if ( dataSet.getEventHandler( ) != null )
		{
			OnFetchScriptHelper event = new OnFetchScriptHelper( dataSet );
			odiQuery.addOnFetchEvent( event );
		}

		if ( dataSetFilters.size( )
				+ queryFilters.size( ) + multipassFilters.size( ) + aggrFilters.size( ) + dataSetAggrFilters.size( ) > 0 )
		{
			IResultObjectEvent objectEvent = new FilterByRow( dataSetFilters,
					queryFilters,
					multipassFilters,
					aggrFilters,
					dataSetAggrFilters,
					dataSet );
			odiQuery.addOnFetchEvent( objectEvent );
		}
	}
	
	/**
	 * 
	 * @param computedColumns
	 * @return
	 * @throws DataException
	 */
	private Map<String, IBinding> createBindingFromComputedColumn( List computedColumns ) throws DataException
	{
		Map<String, IBinding> result = new HashMap<String, IBinding>();
		if( computedColumns == null || computedColumns.size( ) == 0 )
			return result;
		for( Object computedColumn: computedColumns )
		{
			IComputedColumn cc = (IComputedColumn)computedColumn;
			IBinding binding = new Binding( cc.getName( ) );
			binding.setExpression( cc.getExpression( ) );
			binding.setAggrFunction( cc.getAggregateFunction( ) );
			result.put( cc.getName( ), binding );
		}
		return result;
	}
	
	/**
	 * get the data type of a expression
	 * @param cx
	 * @param expr
	 * @return
	 * @throws DataException 
	 */
	private int getColumnDataType( ScriptContext cx, String expr ) throws DataException
	{
		String columnName = QueryExecutorUtil.getColInfoFromJSExpr( cx, expr )
				.getColumnName( );
		if ( columnName == null )
		{
			return DataType.UNKNOWN_TYPE;
		}
		if ( columnName.equals( ScriptConstants.ROW_NUM_KEYWORD ) )
		{
			return DataType.INTEGER_TYPE;
		}
		Object baseExpr = ( this.baseQueryDefn.getBindings( ).get( columnName ) );

		if ( baseExpr == null )
		{
			return DataType.UNKNOWN_TYPE;
		}
		int dataType = ( (IBinding) baseExpr ).getDataType( );
		if( dataType == DataType.UNKNOWN_TYPE )
			return DataType.ANY_TYPE;
		return dataType;
	}
	
	/**
	 * @param src
	 * @return
	 * @throws DataException 
	 */
	private String getDataSetGroupKeyExpression( IGroupDefinition src )
	{
		String expr = getGroupKeyExpression(src);
		String dataSetExpr;
		try
		{
			dataSetExpr = getDataSetExpr( expr );
		}
		catch (DataException e)
		{
			dataSetExpr = null;
		}
		try
		{
			if( "dataSetRow._rowPosition".equals( dataSetExpr ) )
				return expr;
			if( dataSetExpr != null && ExpressionUtil.getColumnName( dataSetExpr ) != null )
				return dataSetExpr;
		}
		catch (BirtException e)
		{
		}
		return expr;
	}

	private String getGroupKeyExpression(IGroupDefinition src) {
		String expr = src.getKeyColumn( );
		if ( expr == null )
		{
			expr = src.getKeyExpression( );
		}
		else
		{
			expr = getColumnRefExpression( expr );
		}
		return expr;
	}

	/**
	 * 
	 * @param expr
	 * @return
	 */
	private String getColumnRefExpression( String expr )
	{
		return ExpressionUtil.createJSRowExpression( expr );
	}

	void setParentExecutorHelper( IExecutorHelper helper )
	{
		this.parentHelper = helper;
	}

	/**
	 * 
	 * @param cx
	 * @param dataSetFilters
	 * @param queryFilters
	 * @param temporaryComputedColumns
	 * @return
	 * @throws DataException 
	 */
	private List prepareFilters( ScriptContext cx, List dataSetFilters,
			List queryFilters, List temporaryComputedColumns ) throws DataException
	{
		List result = new ArrayList( );
		/*List allFilter = new ArrayList();
		allFilter.addAll( dataSetFilters );
		allFilter.addAll( queryFilters );
		prepareFilter( cx, allFilter, temporaryComputedColumns, result );
		*/
		
		prepareFilter( cx, dataSetFilters,temporaryComputedColumns, result );
		prepareFilter( cx, queryFilters,temporaryComputedColumns, result );
		return result;
	}

	/**
	 * 
	 * @param cx
	 * @param dataSetFilters
	 * @param temporaryComputedColumns
	 * @param result
	 * @throws DataException 
	 */
	private void prepareFilter( ScriptContext cx, List dataSetFilters,
			List temporaryComputedColumns, List result ) throws DataException
	{
		if ( dataSetFilters != null && !dataSetFilters.isEmpty( ) )
		{
			Iterator it = dataSetFilters.iterator( );
			for ( int i = 0; it.hasNext( ); i++ )
			{
				IFilterDefinition src = (IFilterDefinition) it.next( );
				IBaseExpression expr = src.getExpression( );

				if ( isGroupFilter( src ) )
				{
					ConditionalExpression ce = ( (ConditionalExpression) expr );
					String exprText = ce.getExpression( ).getText( );
					ColumnInfo columnInfo = QueryExecutorUtil.getColInfoFromJSExpr( cx,
							exprText );

					int index = columnInfo.getColumnIndex( );
					String name = columnInfo.getColumnName( );

					if ( name == null && index < 0 )
					{
						int currentIndex = result.size( );
						// If failed to treate filter key as a column reference
						// expression
						// then treat it as a computed column expression
						temporaryComputedColumns.add( new ComputedColumn( "_{$TEMP_FILTER_"
								+ currentIndex + "$}_",
								exprText,
								DataType.ANY_TYPE ) );
						it.remove( );
						result.add( new FilterDefinition( new ConditionalExpression( new ScriptExpression( String.valueOf( "dataSetRow[\"_{$TEMP_FILTER_"
								+ currentIndex + "$}_\"]" ) ),
								ce.getOperator( ),
								ce.getOperand1( ),
								ce.getOperand2( ) ) ) );
					}
				}

			}
		}
	}

	/**
	 * 
	 * @param filter
	 * @return
	 * @throws DataException 
	 */
	private boolean isGroupFilter( IFilterDefinition filter ) throws DataException
	{
		IBaseExpression expr = filter.getExpression( );

		if ( expr instanceof IConditionalExpression )
		{
			if ( !ExpressionCompilerUtil.isValidExpressionInQueryFilter( expr,
					session.getEngineContext( ).getScriptContext( )) )
				throw new DataException( ResourceConstants.INVALID_DEFINITION_IN_FILTER,
						new Object[]{
							( (IConditionalExpression) expr ).getExpression( )
									.getText( )
						} );
			try
			{
				if ( odiQuery instanceof BaseQuery )
				{
					return ( (BaseQuery) odiQuery ).getExprProcessor( )
								.hasAggregation( expr );
				}
			}
			catch ( DataException e )
			{
				return true;
			}
		}
		return false;
	}

	/*
	 * @see org.eclipse.birt.data.engine.impl.IQueryExecutor#getResultMetaData()
	 */
	public IResultMetaData getResultMetaData( ) throws DataException
	{
		assert odiQuery instanceof IPreparedDSQuery
				|| odiQuery instanceof ICandidateQuery
				|| odiQuery instanceof JointDataSetQuery;

		if ( odiQuery instanceof IPreparedDSQuery )
		{
			if ( ( (IPreparedDSQuery) odiQuery ).getResultClass( ) != null )
				return new ColumnBindingMetaData( baseQueryDefn,
						( (IPreparedDSQuery) odiQuery ).getResultClass( ) );
			else
				return null;
		}
		else if ( odiQuery instanceof JointDataSetQuery )
		{
			return new ColumnBindingMetaData( baseQueryDefn,
					( (JointDataSetQuery) odiQuery ).getResultClass( ) );
		}
		else
		{
			IResultMetaData meta = DataSetDesignHelper.getResultMetaData( baseQueryDefn,
					odiQuery );
			if ( meta == null )
				return new ColumnBindingMetaData( baseQueryDefn,
						( (ICandidateQuery) odiQuery ).getResultClass( ) );
			else
				return meta;
		}
	}
	
	/*
	 * @see org.eclipse.birt.data.engine.impl.IQueryExecutor#getOdiResultClass()
	 */
	public IResultClass getOdiResultClass( ) throws DataException
	{
		assert odiQuery instanceof IPreparedDSQuery
				|| odiQuery instanceof ICandidateQuery
				|| odiQuery instanceof JointDataSetQuery;

		if ( odiQuery instanceof IPreparedDSQuery )
		{
			return ( (IPreparedDSQuery) odiQuery ).getResultClass( );
		}
		else if ( odiQuery instanceof JointDataSetQuery )
		{
			return ( (JointDataSetQuery) odiQuery ).getResultClass( );
		}
		else
		{
			IResultClass resultClass = DataSetDesignHelper.getResultClass( odiQuery );
			if ( resultClass != null )
				return resultClass;
			else
				return ( (ICandidateQuery) odiQuery ).getResultClass( );
		}
	}

	/*
	 * @see org.eclipse.birt.data.engine.impl.IQueryExecutor#execute()
	 */
	public void execute( IEventHandler eventHandler ) throws DataException
	{
		logger.logp( Level.FINER,
				QueryExecutor.class.getName( ),
				"execute",
				"Start to execute" );

		if ( this.isExecuted )
			return;

		ExecutorHelper helper = new ExecutorHelper( this.parentHelper );
		
		eventHandler.setExecutorHelper( helper );

		// Execute the query
		odiResult = executeOdiQuery( eventHandler );

		helper.setScriptable( this.dataSet.getJSResultRowObject( ) );
		
		resetComputedColumns( );
		// Bind the row object to the odi result set
		this.dataSet.setResultSet( odiResult, false );

		// Calculate aggregate values
		//this.aggrTable.calculate( odiResult, getQueryScope( ) );
		
		this.isExecuted = true;

		logger.logp( Level.FINER,
				QueryExecutor.class.getName( ),
				"execute",
				"Finish executing" );
	}

	/**
	 * reset computed columns
	 */
	private void resetComputedColumns( )
	{
		List l = this.getDataSet( ).getComputedColumns( );
		if ( l != null )
			l.removeAll( this.temporaryComputedColumns );
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
			dataSetBeforeClose( );
		}
		catch ( DataException e )
		{
			logger.logp( Level.FINE,
					QueryExecutor.class.getName( ),
					"close",
					e.getMessage( ),
					e );
		}

		if ( odiResult != null )
		{	
			try
			{
				odiResult.close( );
			}
			catch ( DataException e1 )
			{
				// TODO Auto-generated catch block
				e1.printStackTrace( );
			}
		}
		odiQuery.close( );

		try
		{
			dataSet.close( );
		}
		catch ( DataException e )
		{
			logger.logp( Level.FINE,
					QueryExecutor.class.getName( ),
					"close",
					e.getMessage( ),
					e );
		}

		odiQuery = null;
		odiDataSource = null;
		odiResult = null;
		queryScope = null;
		isPrepared = false;
		isExecuted = false;

		// Note: reset dataSet and dataSource only after afterClose() is executed, since
		// the script may access these two objects
		try
		{
			dataSetAfterClose( );
		}
		catch ( DataException e )
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

	/**
	 * Gets the Javascript scope for evaluating expressions for this query
	 * 
	 * @return
	 * @throws DataException 
	 */
	public Scriptable getQueryScope( ) throws DataException
	{
		try
		{
			if ( queryScope == null )
			{
				// Set up a query scope. All expressions are evaluated against
				// the
				// Data set JS object as the prototype (so that it has access to
				// all
				// data set properties). It uses a subscope of the externally
				// provided
				// parent scope, or the global shared scope
				queryScope = newSubScope( parentScope );
				queryScope.setPrototype( dataSet.getJSDataSetObject( ) );
			}
			return queryScope;
		}
		catch ( BirtException e )
		{
			throw DataException.wrap( e );
		}
	}

	/**
	 * Creates a subscope within parent scope
	 * @param parentAndProtoScope parent scope. If null, the shared top-level scope is used as parent
	 * @throws BirtException 
	 */
	private Scriptable newSubScope( Scriptable parentAndProtoScope ) throws BirtException
	{
		if ( parentAndProtoScope == null )
			parentAndProtoScope = sharedScope;

		Scriptable scope = ((IDataScriptEngine)session.getEngineContext( ).getScriptContext( ).getScriptEngine( IDataScriptEngine.ENGINE_NAME )).getJSContext( session.getEngineContext( ).getScriptContext( ) )
				.newObject( parentAndProtoScope );
		scope.setParentScope( parentAndProtoScope );
		scope.setPrototype( parentAndProtoScope );

		
		return scope;
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
		return this.aggrTable.getJSAggrValueObject( );
	}

	/*
	 * @see org.eclipse.birt.data.engine.impl.IQueryExecutor#getNestedDataSets(int)
	 */
	public DataSetRuntime[] getNestedDataSets( int nestedCount )
	{
		return tabularOuterResults == null ? null
				: tabularOuterResults.getDataSetRuntime( nestedCount );
	}

	/*
	 * @see org.eclipse.birt.data.engine.impl.IQueryExecutor#getOdiResultSet()
	 */
	public IResultIterator getOdiResultSet( )
	{
		return this.odiResult;
	}
	
	/**
	 * @param evaluateValue
	 * @return
	 * @throws DataException
	 */
	protected Collection resolveDataSetParameters( boolean evaluateValue )
			throws DataException
	{
		return new ParameterUtil( this.tabularOuterResults == null ? null:this.tabularOuterResults.getQueryScope( ),
				this.getDataSet( ),
				(IQueryDefinition) this.baseQueryDefn,
				this.getQueryScope( ),
				session.getEngineContext( ).getScriptContext( )).resolveDataSetParameters( evaluateValue );
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.birt.data.engine.impl.IQueryExecutor#getAppContext()
	 */
	public Map getAppContext()
	{
		return this.queryAppContext;
	}
	
	public DataEngineSession getSession()
	{
		return this.session;
	}
}