/*******************************************************************************
 * Copyright (c) 2004, 2010 Actuate Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Actuate Corporation  - initial API and implementation
 *******************************************************************************/

package org.eclipse.birt.data.engine.olap.impl.query;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.birt.core.script.ScriptExpression;
import org.eclipse.birt.data.engine.api.DataEngineContext;
import org.eclipse.birt.data.engine.api.IBaseExpression;
import org.eclipse.birt.data.engine.api.IBaseQueryResults;
import org.eclipse.birt.data.engine.api.IBinding;
import org.eclipse.birt.data.engine.api.ICollectionConditionalExpression;
import org.eclipse.birt.data.engine.api.IConditionalExpression;
import org.eclipse.birt.data.engine.api.IExpressionCollection;
import org.eclipse.birt.data.engine.api.IFilterDefinition;
import org.eclipse.birt.data.engine.api.IScriptExpression;
import org.eclipse.birt.data.engine.core.DataException;
import org.eclipse.birt.data.engine.expression.ExpressionCompilerUtil;
import org.eclipse.birt.data.engine.impl.DataEngineSession;
import org.eclipse.birt.data.engine.olap.api.query.CubeFilterDefinition;
import org.eclipse.birt.data.engine.olap.api.query.ICubeFilterDefinition;
import org.eclipse.birt.data.engine.olap.api.query.ICubeQueryDefinition;
import org.eclipse.birt.data.engine.olap.api.query.ICubeSortDefinition;
import org.eclipse.birt.data.engine.olap.api.query.IDimensionDefinition;
import org.eclipse.birt.data.engine.olap.api.query.IEdgeDefinition;
import org.eclipse.birt.data.engine.olap.api.query.IHierarchyDefinition;
import org.eclipse.birt.data.engine.olap.api.query.ILevelDefinition;
import org.eclipse.birt.data.engine.olap.data.api.DimLevel;
import org.eclipse.birt.data.engine.olap.data.api.IComputedMeasureHelper;
import org.eclipse.birt.data.engine.olap.data.api.ISelection;
import org.eclipse.birt.data.engine.olap.data.impl.SelectionFactory;
import org.eclipse.birt.data.engine.olap.data.impl.aggregation.filter.SimpleLevelFilter;
import org.eclipse.birt.data.engine.olap.data.util.TempPathManager;
import org.eclipse.birt.data.engine.olap.util.ComputedMeasureHelper;
import org.eclipse.birt.data.engine.olap.util.OlapExpressionCompiler;
import org.eclipse.birt.data.engine.olap.util.OlapExpressionUtil;
import org.eclipse.birt.data.engine.olap.util.filter.AggrMeasureFilterEvalHelper;
import org.eclipse.birt.data.engine.olap.util.filter.BaseDimensionFilterEvalHelper;
import org.eclipse.birt.data.engine.olap.util.filter.IAggrMeasureFilterEvalHelper;
import org.eclipse.birt.data.engine.olap.util.filter.IJSFacttableFilterEvalHelper;
import org.eclipse.birt.data.engine.olap.util.filter.IJSFilterHelper;
import org.eclipse.birt.data.engine.olap.util.filter.JSFacttableFilterEvalHelper;
import org.eclipse.birt.data.engine.script.ScriptConstants;
import org.eclipse.birt.data.engine.script.ScriptEvalUtil;
import org.mozilla.javascript.Scriptable;

/**
 * 
 */

public class CubeQueryExecutor
{

	private ICubeQueryDefinition defn;
	private Scriptable scope;
	private DataEngineSession session;
	private DataEngineContext context;
	private String queryResultsId;
	private IBaseQueryResults outResults;
	
	private List<IJSFilterHelper> dimensionFilterEvalHelpers;
	private List<SimpleLevelFilter> dimensionSimpleFilter;
	private List<IAggrMeasureFilterEvalHelper> aggrMeasureFilterEvalHelpers;
	private List<IJSFacttableFilterEvalHelper> advancedFacttableBasedFilterEvalHelper;
	
	private static final int DIMENSION_FILTER = 0;
	private static final int AGGR_MEASURE_FILTER = 1;
	private static final int FACTTABLE_FILTER = 2;
	/**
	 * 
	 * @param outResults
	 * @param defn
	 * @param session
	 * @param scope
	 * @param context
	 * @throws DataException 
	 */
	public CubeQueryExecutor( IBaseQueryResults outResults, ICubeQueryDefinition defn, DataEngineSession session, Scriptable scope,
			DataEngineContext context ) throws DataException
	{
		this.defn = defn;
		this.scope = scope;
		this.context = context;
		this.session = session;
		TempPathManager.setTempPath( session.getTempDir( ) );
		this.outResults = outResults;
		this.dimensionFilterEvalHelpers = new ArrayList<IJSFilterHelper> ();
		this.dimensionSimpleFilter = new ArrayList<SimpleLevelFilter> ();
		this.aggrMeasureFilterEvalHelpers = new ArrayList<IAggrMeasureFilterEvalHelper>();
		this.advancedFacttableBasedFilterEvalHelper = new ArrayList<IJSFacttableFilterEvalHelper>();
		if ( !(context.getMode( ) == DataEngineContext.MODE_PRESENTATION
				&& defn.getQueryResultsID( ) != null) )
		{
			//query execution result will be loaded directly from document
			//needless to populate filer helpers
			populateFilterHelpers();
		}
	}
	
	private SimpleLevelFilter createSimpleLevelFilter( IFilterDefinition filter, List bindings )
	{
		if( ! ( filter instanceof CubeFilterDefinition ) )
			return null;
			
		IBaseExpression expr = (( CubeFilterDefinition )filter).getExpression( );
		if( !( expr instanceof IConditionalExpression ) )
			return null;
		IConditionalExpression condExpr = (IConditionalExpression) (( CubeFilterDefinition )filter).getExpression( );
		
		Set refDimLevel;
		try
		{
			refDimLevel = OlapExpressionCompiler.getReferencedDimLevel( condExpr.getExpression(), bindings );
		
			if( refDimLevel.size( ) != 1 )
				return null;
			DimLevel dimlevel = (DimLevel) refDimLevel.iterator( ).next();
			if( dimlevel.getAttrName() != null )
				return null;
			if( !( condExpr.getOperand1( ) instanceof IExpressionCollection ))
			{
				Object Op1 = ScriptEvalUtil.evalExpr( (IScriptExpression) condExpr.getOperand1( ),
						session.getEngineContext( ).getScriptContext( ).newContext( scope ),
						ScriptExpression.defaultID,
						0 );
				if( Op1 == null )
					return null;
				ISelection[] selction = new ISelection[1];
				if( condExpr.getOperator( ) == IConditionalExpression.OP_EQ || condExpr.getOperator( ) == IConditionalExpression.OP_IN )
				{
					selction[0] = SelectionFactory.createOneKeySelection( new Object[]{Op1} );
				}
				else if( condExpr.getOperator( ) == IConditionalExpression.OP_GT )
				{
					selction[0] = SelectionFactory.createRangeSelection( new Object[]{Op1}, null, false, false );
				}
				else if( condExpr.getOperator( ) == IConditionalExpression.OP_GE )
				{
					selction[0] = SelectionFactory.createRangeSelection( new Object[]{Op1}, null, true, false );
				}
				else if( condExpr.getOperator( ) == IConditionalExpression.OP_LT )
				{
					selction[0] = SelectionFactory.createRangeSelection( null, new Object[]{Op1}, false, false );
				}
				else if( condExpr.getOperator( ) == IConditionalExpression.OP_LE )
				{
					selction[0] = SelectionFactory.createRangeSelection( null, new Object[]{Op1}, false, true );
				}
				else
					return null;
				
				return new SimpleLevelFilter( dimlevel, selction );
			}
			else if( condExpr.getOperator( ) == IConditionalExpression.OP_IN )
			{
				IExpressionCollection combinedExpr = (IExpressionCollection) ( (IConditionalExpression) expr ).getOperand1( );
				Object[] exprs = combinedExpr.getExpressions( )
						.toArray( );

				Object[] opValues = new Object[exprs.length];
				boolean existValue = false;
				for ( int i = 0; i < opValues.length; i++ )
				{
					opValues[i] = ScriptEvalUtil.evalExpr( (IBaseExpression) exprs[i],
							session.getEngineContext( ).getScriptContext( ).newContext( scope ),
							ScriptExpression.defaultID,
							0 );
					if( opValues[i] != null )
					{
						existValue = true;
					}
				}
				ISelection[] selction = new ISelection[1];
				Object[][] keyValues = new Object[opValues.length][1];
				for( int i = 0; i < opValues.length; i++ )
				{
					keyValues[i][0] = opValues[i];
				}
				selction[0] = SelectionFactory.createMutiKeySelection( keyValues );
				return new SimpleLevelFilter( dimlevel, selction );
			}
			return null;
		}
		catch (DataException e)
		{
			return null;
		}
	}
	
	
	
	private void populateFilterHelpers( ) throws DataException
	{
		List filters = defn.getFilters( );
		List results = new ArrayList( );
		Set<DimLevel> dimLevelInCubeQuery = this.getDimLevelsDefinedInCubeQuery( );
		for ( int i = 0; i < filters.size( ); i++ )
		{
			IFilterDefinition filter = (IFilterDefinition) filters.get( i );
			switch ( this.getFilterType( filter, dimLevelInCubeQuery ))
			{ 
				case CubeQueryExecutor.DIMENSION_FILTER:
				{
					SimpleLevelFilter simpleLevelfilter = createSimpleLevelFilter( filter, defn.getBindings( ) );
					if( simpleLevelfilter == null )
					{
						this.dimensionFilterEvalHelpers.add( BaseDimensionFilterEvalHelper.createFilterHelper( this.outResults, this.scope,
							defn,
							filter,
							this.session.getEngineContext( ).getScriptContext( )) );
					}
					else
					{
						boolean existLevelFilter = false;
						for( int j = 0; j < this.dimensionSimpleFilter.size( ); j++ )
						{
							if( dimensionSimpleFilter.get( j ).getDimensionName( ).equals( simpleLevelfilter.getDimensionName( ) )
									&& dimensionSimpleFilter.get( j ).getLevelName( ).equals( simpleLevelfilter.getLevelName( ) ))
							{
								this.dimensionFilterEvalHelpers.add( BaseDimensionFilterEvalHelper.createFilterHelper( this.outResults, this.scope,
										defn,
										filter,
										this.session.getEngineContext( ).getScriptContext( )) );
								existLevelFilter = true;
								break;
							}
						}
						if( !existLevelFilter )
							this.dimensionSimpleFilter.add( simpleLevelfilter );
					}
					break;
				}
				case CubeQueryExecutor.AGGR_MEASURE_FILTER:
				{
					this.aggrMeasureFilterEvalHelpers.add( new AggrMeasureFilterEvalHelper( this.outResults, scope, 
							defn,
							filter,
							session.getEngineContext( ).getScriptContext( )) );
					break;
				}
				case CubeQueryExecutor.FACTTABLE_FILTER:
				default:
				{
					this.advancedFacttableBasedFilterEvalHelper.add( new JSFacttableFilterEvalHelper( scope,
							this.session.getEngineContext( ).getScriptContext( ),
							filter, this.outResults, this.defn ) );
				}
			}
		}		
	}

	private int getFilterType( IFilterDefinition filter, Set<DimLevel> dimLevelInCubeQuery ) throws DataException
	{
		if(! (filter instanceof ICubeFilterDefinition))
		{
			if( filter.getExpression( ) instanceof ICollectionConditionalExpression )
			{
				Collection<IScriptExpression> exprs = ( ( ICollectionConditionalExpression )( filter.getExpression( ) ) ).getExpr( );
				Set dimensionSet = new HashSet( );
				Iterator<IScriptExpression> exprsIterator = exprs.iterator( );
				while ( exprsIterator.hasNext( ) )
				{
					Iterator dimLevels = OlapExpressionCompiler.getReferencedDimLevel( exprsIterator.next( ),
							this.defn.getBindings( ) )
							.iterator( );
					while ( dimLevels.hasNext( ) )
						dimensionSet.add( ( (DimLevel) dimLevels.next( ) ).getDimensionName( ) );
				}
				if ( dimensionSet.size( ) == 1 )
					return CubeQueryExecutor.DIMENSION_FILTER;
				else
					return CubeQueryExecutor.FACTTABLE_FILTER;
			}
			return CubeQueryExecutor.DIMENSION_FILTER;
		}
		ICubeFilterDefinition cubeFilter = (ICubeFilterDefinition) filter;
		if ( cubeFilter.getTargetLevel( ) != null)
		{
			return CubeQueryExecutor.DIMENSION_FILTER;
		}
		else
		{
			String measure = OlapExpressionCompiler.getReferencedScriptObject( filter.getExpression( ), ScriptConstants.MEASURE_SCRIPTABLE );
			if( measure != null )
				return CubeQueryExecutor.FACTTABLE_FILTER;
			
			List bindingName = ExpressionCompilerUtil.extractColumnExpression( filter.getExpression( ), ScriptConstants.DATA_BINDING_SCRIPTABLE );
			if( bindingName.size( ) > 0 )
			{
				if( existAggregationBinding( bindingName, this.defn.getBindings( ) ) )
					return CubeQueryExecutor.AGGR_MEASURE_FILTER;
		
				return CubeQueryExecutor.FACTTABLE_FILTER;
			}
			else
			{
				List dimensionName = ExpressionCompilerUtil.extractColumnExpression( filter.getExpression( ), ScriptConstants.DIMENSION_SCRIPTABLE );
				if( dimensionName.size( ) > 1 )
				{
					return FACTTABLE_FILTER;
				}
				return DIMENSION_FILTER;
			}
		}
	}
	
	
	
	/**
	 * 
	 * @param bindingName
	 * @param bindings
	 * @return
	 * @throws DataException
	 */
	private static boolean existAggregationBinding( List bindingName, List bindings ) throws DataException
	{
		for( int i = 0; i < bindingName.size( ); i++ )
		{
			for( int j = 0; j < bindings.size( ); j++ )
			{
				IBinding binding = (IBinding) bindings.get( j );
				if( bindingName.get( i ).equals( binding.getBindingName( ) ) && OlapExpressionUtil.isAggregationBinding( binding ) )
				{
					return true;
				}
			}
		}
		return false;
	}
	/**
	 * 
	 * @return
	 * @throws DataException
	 */
	public List<IJSFilterHelper> getDimensionFilterEvalHelpers( ) throws DataException
	{
		return this.dimensionFilterEvalHelpers;
	}

	public List<SimpleLevelFilter> getdimensionSimpleFilter( ) throws DataException
	{
		return this.dimensionSimpleFilter;
	} 
	
	/**
	 * 
	 * @return
	 * @throws DataException
	 */
	public List<IAggrMeasureFilterEvalHelper> getMeasureFilterEvalHelpers() throws DataException
	{
		return this.aggrMeasureFilterEvalHelpers;
	}
	
	/**
	 * 
	 * @return
	 * @throws DataException
	 */
	public List<IJSFacttableFilterEvalHelper> getFacttableBasedFilterHelpers() throws DataException
	{
		return this.advancedFacttableBasedFilterEvalHelper;
	}

	private Set<DimLevel> getDimLevelsDefinedInCubeQuery( )
	{
		Set<DimLevel> dimLevelDefinedInCube = new HashSet<DimLevel>();
		populateDimLevelInEdge( dimLevelDefinedInCube, ICubeQueryDefinition.COLUMN_EDGE );
		populateDimLevelInEdge( dimLevelDefinedInCube, ICubeQueryDefinition.ROW_EDGE );
		populateDimLevelInEdge( dimLevelDefinedInCube, ICubeQueryDefinition.PAGE_EDGE );
		return dimLevelDefinedInCube;
	}

	private void populateDimLevelInEdge( Set<DimLevel> dimLevelDefinedInCube,
			int i )
	{
		IEdgeDefinition edge = defn.getEdge( i );
		if( edge == null )
			return;
		List<IDimensionDefinition> dims = edge.getDimensions( );
		for( IDimensionDefinition dim: dims )
		{
			List<ILevelDefinition> levels = ((IHierarchyDefinition) dim.getHierarchy( ).get( 0 )).getLevels( );
			for( ILevelDefinition level: levels )
			{
				dimLevelDefinedInCube.add( new DimLevel( dim.getName( ), level.getName( ) ) );
			}
		}
	}

	/**
	 * 
	 * @return
	 * @throws DataException
	 */
	public IComputedMeasureHelper getComputedMeasureHelper( )
			throws DataException
	{
		if ( this.defn.getComputedMeasures( ) != null
				&& this.defn.getComputedMeasures( ).size( ) > 0 )
			return new ComputedMeasureHelper( this.scope,
					session.getEngineContext( ).getScriptContext( ),
					this.defn.getComputedMeasures( ));
		return null;
	}
	
	/**
	 * 
	 * @return
	 */
	public ICubeQueryDefinition getCubeQueryDefinition( )
	{
		return this.defn;
	}

	/**
	 * 
	 * @return
	 */
	public DataEngineSession getSession( )
	{
		return this.session;
	}
	
	/**
	 * 
	 * @return
	 */
	public DataEngineContext getContext( )
	{
		return this.context;
	}

	/**
	 * 
	 * @return
	 */
	public List getColumnEdgeSort( )
	{
		return getEdgeSort( ICubeQueryDefinition.COLUMN_EDGE );
	}

	/**
	 * 
	 * @return
	 */
	public List getRowEdgeSort( )
	{
		return getEdgeSort( ICubeQueryDefinition.ROW_EDGE );
	}

	/**
	 * 
	 * @return
	 */
	public List getPageEdgeSort( )
	{
		return getEdgeSort( ICubeQueryDefinition.PAGE_EDGE );
	}
	
	/**
	 * 
	 * @return
	 */
	public String getQueryResultsId()
	{
		return this.queryResultsId;
	}
	
	/**
	 * 
	 * @param id
	 */
	public void setQueryResultsId( String id )
	{
		this.queryResultsId = id;
	}
	
	/**
	 * 
	 * @return
	 */
	public IBaseQueryResults getOuterResults( )
	{
		return this.outResults;
	}
	
	public Scriptable getScope( )
	{
		return scope;
	}
	
	/**
	 * 
	 * @param edgeType
	 * @return
	 */
	private List getEdgeSort( int edgeType )
	{
		List l = this.defn.getSorts( );
		List result = new ArrayList( );
		for ( int i = 0; i < l.size( ); i++ )
		{
			ICubeSortDefinition sort = (ICubeSortDefinition) l.get( i );
			if ( this.defn.getEdge( edgeType ) != null && sort.getTargetLevel( ) != null &&
					this.defn.getEdge( edgeType )
							.getDimensions( )
							.contains( sort.getTargetLevel( )
									.getHierarchy( )
									.getDimension( ) ) )
			{
				result.add( sort );
			}
		}

		Collections.sort( result, new Comparator( ) {

			public int compare( Object arg0, Object arg1 )
			{
				int level1 = ( (ICubeSortDefinition) arg0 ).getTargetLevel( )
						.getHierarchy( )
						.getLevels( )
						.indexOf( ( (ICubeSortDefinition) arg0 ).getTargetLevel( ) );
				int level2 = ( (ICubeSortDefinition) arg1 ).getTargetLevel( )
						.getHierarchy( )
						.getLevels( )
						.indexOf( ( (ICubeSortDefinition) arg1 ).getTargetLevel( ) );

				if ( level1 == level2 )
					return 0;
				else if ( level1 < level2 )
					return -1;
				else
					return 1;

			}
		} );
		return result;
	}
}
