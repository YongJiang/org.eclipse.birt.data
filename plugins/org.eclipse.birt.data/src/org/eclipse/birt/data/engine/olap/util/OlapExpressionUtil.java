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

package org.eclipse.birt.data.engine.olap.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.birt.core.exception.BirtException;
import org.eclipse.birt.data.engine.api.IBaseExpression;
import org.eclipse.birt.data.engine.api.IBaseQueryResults;
import org.eclipse.birt.data.engine.api.IBinding;
import org.eclipse.birt.data.engine.api.IScriptExpression;
import org.eclipse.birt.data.engine.core.DataException;
import org.eclipse.birt.data.engine.i18n.ResourceConstants;
import org.eclipse.birt.data.engine.impl.IQueryService;
import org.eclipse.birt.data.engine.olap.api.ICubeQueryResults;
import org.eclipse.birt.data.engine.olap.data.api.DimLevel;
import org.eclipse.birt.data.engine.olap.script.JSCubeBindingObject;
import org.eclipse.birt.data.engine.script.ScriptConstants;
import org.mozilla.javascript.Scriptable;

/**
 * 
 */

public class OlapExpressionUtil
{
	/**
	 * get the attribute reference name.
	 * 
	 * @param dimName
	 * @param levelName
	 * @param attrName
	 * @return
	 */
	public static String getAttrReference( String dimName, String levelName,
			String attrName )
	{
		return dimName + '/' + levelName + '/' + attrName;
	}
	
	/**
	 * 
	 * @param originalMeasureName
	 * @return
	 */
	public static String createMeasureCalculateMemeberName(
			String originalMeasureName )
	{
		return "_&$" + originalMeasureName + "$&_";
	}
	/**
	 * 
	 * @param expr
	 * @return
	 */
	public static boolean isReferenceToDimLevel( String expr )
	{
		if( expr == null )
			return false;
		return expr.matches( "\\Qdimension[\"\\E.*\\Q\"][\"\\E.*\\Q\"]\\E" );
	}
	
	/**
	 * This method is used to get the level name that reference by a level
	 * reference expression of following format:
	 * dimension["dimensionName"]["levelName"].
	 * 
	 * String[0] dimensionName;
	 * String[1] levelName;
	 * @param expr
	 * @return String[]
	 */
	private static String[] getTargetLevel( String expr )
	{
		// TODO enhance me.
		if ( expr == null )
			return null;
		if ( !expr.matches( "\\Qdimension[\"\\E.*\\Q\"][\"\\E.*\\Q\"]\\E" ) )
			return null;

		expr = expr.replaceFirst( "\\Qdimension\\E", "" );
		String[] result = expr.split( "\\Q\"][\"\\E" );
		result[0] = result[0].replaceAll( "\\Q[\"\\E", "" );
		result[1] = result[1].replaceAll( "\\Q\"]\\E", "" );
		return result;
	}
	
	/**
	 * This method is used to get the dimension,level,attributes name that reference by a level&attribute
	 * reference expression of following format:
	 * dimension["dimensionName"]["levelName"]["attributeName"].
	 * 
	 * String[0] dimensionName;
	 * String[1] levelName;
	 * String[2] attributeName;
	 * @param expr
	 * @return String[]
	 * @throws DataException 
	 */
	private static String[] getTargetAttribute( String expr, List bindings )
			throws DataException
	{
		if ( expr == null )
			return null;
		if ( !( expr.matches( "\\Qdimension[\"\\E.*\\Q\"][\"\\E.*\\Q\"][\"\\E.*\\Q\"]\\E" ) || expr.matches( "\\Qdimension[\"\\E.*\\Q\"][\"\\E.*\\Q\"]\\E" ) ) )
		{
			String bindingName = getBindingName( expr );
			if ( bindingName != null )
			{
				for ( int i = 0; i < bindings.size( ); i++ )
				{
					IBinding binding = (IBinding) bindings.get( i );
					if ( bindingName.equals( binding.getBindingName( ) ) )
					{
						if ( binding.getExpression( ) instanceof IScriptExpression )
						{
							return getTargetAttribute( ( (IScriptExpression) binding.getExpression( ) ).getText( ),
									bindings );
						}
					}
				}
			}

			throw new DataException( ResourceConstants.INVALID_EXPRESSION, expr );
		}

		expr = expr.replaceFirst( "\\Qdimension\\E", "" );
		String[] result = new String[3];
		String[] candidateResult = expr.split( "\\Q\"][\"\\E" );
		if ( candidateResult.length == 2 )
		{
			result[0] = candidateResult[0].replaceAll( "\\Q[\"\\E", "" );
			result[1] = candidateResult[1].replaceAll( "\\Q\"]\\E", "" );
		}
		else
		{
			result[0] = candidateResult[0].replaceAll( "\\Q[\"\\E", "" );
			result[1] = candidateResult[1];
			result[2] = candidateResult[2].replaceAll( "\\Q\"]\\E", "" );
		}
		return result;
	}
	
	/**
	 * 
	 * @param expr
	 * @return
	 * @throws DataException
	 */
	public static DimLevel getTargetDimLevel( String expr ) throws DataException
	{
		final String[] target = getTargetLevel( expr );
		if ( target == null || target.length < 2 )
		{
			throw new DataException( ResourceConstants.LEVEL_NAME_NOT_FOUND,
					expr );
		}
		switch ( target.length )
		{
			case 2 :
				return new DimLevel( target[0], target[1] );
			case 3 :
				return new DimLevel( target[0], target[1], target[2] );
			default :
				throw new DataException( ResourceConstants.LEVEL_NAME_NOT_FOUND,
						expr );
		}
	}

	/**
	 * This method is to get the measure name that referenced by a measure
	 * reference expression.
	 * 
	 * @param expr
	 * @return
	 */
	public static String getMeasure( String expr ) throws DataException
	{
		if( expr == null )
			return null;
		
		String result = findMeasure(expr);
		if ( result == null  )
			throw new DataException( ResourceConstants.INVALID_MEASURE_REF,
					expr );

		return result;

	}
	
	/**
	 * 
	 * @param expr
	 * @return
	 */
	private static String findMeasure( String expr )
	{
		if( expr == null )
			return null;
		
		if ( !expr.matches( "\\Qmeasure[\"\\E.*\\Q\"]\\E" ) )
			return null;

		return expr.replaceFirst( "\\Qmeasure[\"\\E", "" )
				.replaceFirst( "\\Q\"]\\E", "" );
	}
	
	/**
	 * 
	 * @param expr
	 * @return
	 */
	public static String getMeasure( IBaseExpression expr )
	{
		if( expr instanceof IScriptExpression )
		{
			return findMeasure(((IScriptExpression) expr).getText());
		}
		
		return null;
	}
	
	/**
	 * to check whether the expression directly reference to a dimension or
	 * measure
	 * 
	 * @param expression
	 * @param bindings
	 * @return
	 * @throws DataException
	 */
	public static boolean isDirectRerenrence( IBaseExpression expression,
			List<IBinding> bindings ) throws DataException
	{
		if ( !( expression instanceof IScriptExpression ) )
			return false;
		String expr = ( (IScriptExpression) expression ).getText( );
		if ( expr == null )
			return false;
		if ( expr.matches( "\\Qdimension[\"\\E.*\\Q\"][\"\\E.*\\Q\"]\\E" ) )// dimension
			return true;
		else if ( expr.matches( "\\Qmeasure[\"\\E.*\\Q\"]\\E" ) )// measure
			return true;
		else if ( expr.matches( "\\Qdata[\"\\E.*\\Q\"]\\E" ) )// data binding
		{
			String bindingName = getBindingName( expr );
			for ( IBinding binding : bindings )
			{
				if ( binding.getBindingName( ).equals( bindingName ) )
				{
					return isDirectRerenrence( binding.getExpression( ),
							bindings );
				}
			}
		}
		return false;
	}
	
	/**
	 * Return the binding name of data["binding"]
	 * @param expr
	 * @return
	 */
	public static String getBindingName( String expr )
	{
		if ( expr == null )
			return null;
		if ( !expr.matches( "\\Qdata[\"\\E.*\\Q\"]\\E" ) )
			return null;
		return expr.replaceFirst( "\\Qdata[\"\\E", "" )
				.replaceFirst( "\\Q\"]\\E", "" );
	
	}

	/**
	 * 
	 * @param level
	 * @param attribute
	 * @return
	 */
	public static String getAttributeColumnName( String level, String attribute )
	{
		return level + "/" + attribute;
	}
	
	/**
	 * 
	 * @param dimentionName
	 * @param levelName
	 * @return
	 */
	public static String getQualifiedLevelName( String dimensionName,
			String levelName )
	{
		return dimensionName + "/" + levelName;
	}
	
	/**
	 * 
	 * @param level
	 * @return
	 */
	public static String getDisplayColumnName( String level )
	{
		return level + "/" + "DisplayName";
	}
	
	/**
	 * This method returns a list of ICubeAggrDefn instances which describes the
	 * aggregations that need to be calculated in cube query.
	 * 
	 * @param bindings
	 * @return
	 * @throws DataException 
	 */
	public static ICubeAggrDefn[] getAggrDefns( List bindings ) throws DataException
	{
		if ( bindings == null || bindings.size( ) == 0 )
			return new ICubeAggrDefn[0];

		List cubeAggrDefns = new ArrayList( );
		for ( Iterator it = bindings.iterator( ); it.hasNext( ); )
		{
			IBinding binding = ( (IBinding) it.next( ) );
			try
			{
				if ( binding.getAggrFunction( ) != null
						|| binding.getAggregatOns( ).size( ) != 0 )
					cubeAggrDefns.add( new CubeAggrDefn( binding.getBindingName( ),
							getMeasure( binding.getExpression( )==null?null:( (IScriptExpression) binding.getExpression( ) ).getText( ) ),
							convertToDimLevel( binding.getAggregatOns( ) ),
							binding.getAggrFunction( ),
							convertToDimLevelAttribute( binding.getArguments( ),
									bindings ),
							binding.getFilter( ) ) );
			}
			catch ( DataException ex )
			{
				throw new DataException( ResourceConstants.INVALID_AGGR_BINDING_EXPRESSION,
						ex,
						binding.getBindingName( ) );
			}
		}

		ICubeAggrDefn[] result = new ICubeAggrDefn[cubeAggrDefns.size( )];
		for ( int i = 0; i < result.length; i++ )
		{
			result[i] = (ICubeAggrDefn) cubeAggrDefns.get( i );
		}

		return result;
	}

	/**
	 * This method returns a list of ICubeAggrDefn instances which describes the
	 * aggregations that need to be calculated in cube query.
	 * 
	 * @param bindings
	 * @return
	 * @throws DataException 
	 */
	public static ICubeAggrDefn[] getAggrDefnsByNestBinding( List<IBinding> bindings ) throws DataException
	{
		if ( bindings == null || bindings.size( ) == 0 )
			return new ICubeAggrDefn[0];

		List<CubeAggrDefn> cubeAggrDefns = new ArrayList<CubeAggrDefn>( );
		for ( IBinding binding : bindings )
		{
			try
			{
				if ( binding.getAggrFunction( ) != null )
					cubeAggrDefns.add( new CubeAggrDefn( binding.getBindingName( ),
							getBindingName( ( (IScriptExpression) binding.getExpression( ) ).getText( ) ),
							convertToDimLevel( binding.getAggregatOns( ) ),
							binding.getAggrFunction( ),
							convertToDimLevelAttribute( binding.getArguments( ),
									bindings ),
							binding.getFilter( ) ) );
			}
			catch ( DataException ex )
			{
				throw new DataException( ResourceConstants.INVALID_AGGR_BINDING_EXPRESSION,
						ex,
						binding.getBindingName( ) );
			}
		}
		return cubeAggrDefns.toArray( new CubeAggrDefn[0] );
	}
	
	public static boolean isAggregationBinding(IBinding binding) throws DataException
	{
		if (binding == null)
		{
			return false;
		}
		return binding.getAggrFunction( ) != null;
	}
	
	/**
	 * 
	 * @param expr
	 * @param bindings
	 * @return
	 * @throws DataException
	 */
	public static String getReferencedDimensionName( IBaseExpression expr,
			List bindings ) throws DataException
	{
		String result = OlapExpressionCompiler.getReferencedScriptObject( expr,
				ScriptConstants.DIMENSION_SCRIPTABLE );
		if ( result == null )
		{
			String bindingName = OlapExpressionCompiler.getReferencedScriptObject( expr,
					ScriptConstants.DATA_BINDING_SCRIPTABLE );
			if ( bindingName == null )
				return null;
			else
			{
				for ( int i = 0; i < bindings.size( ); i++ )
				{
					IBinding binding = (IBinding) bindings.get( i );
					if ( binding.getBindingName( ).equals( bindingName ) )
					{
						return getReferencedDimensionName( binding.getExpression( ),
								bindings );
					}
				}
			}
		}
		return result;
	}
	
	private static List convertToDimLevel( List dimLevelExpressions ) throws DataException
	{
		List result = new ArrayList();
		for( int i = 0; i < dimLevelExpressions.size( ); i++ )
		{
			result.add( getTargetDimLevel( dimLevelExpressions.get( i ).toString( )) );
		}
		return result;
	}
	
	private static List convertToDimLevelAttribute( List dimLevelExpressions, List bindings )
			throws DataException
	{
		List result = new ArrayList( );
		for ( int i = 0; i < dimLevelExpressions.size( ); i++ )
		{
			result.add( getTargetAttribute( ( (IScriptExpression) dimLevelExpressions.get( i ) ).getText( ), bindings ) );
		}
		return result;
	}
	
	private static class CubeAggrDefn implements ICubeAggrDefn
	{

		//
		private String name;
		private String measure;
		private List aggrLevels, arguments;
		private String aggrName;
		private IBaseExpression filterExpression;

		/*
		 * 
		 */
		CubeAggrDefn( String name, String measure, List aggrLevels,
				String aggrName, List arguments, IBaseExpression filterExpression )
		{
			assert name != null;
			assert aggrLevels != null;

			this.name = name;
			this.measure = measure;
			this.aggrLevels = aggrLevels;
			this.aggrName = aggrName;
			this.arguments = arguments;
			this.filterExpression = filterExpression;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.eclipse.birt.data.engine.olap.util.ICubeAggrDefn#getAggrLevels()
		 */
		public List getAggrLevels( )
		{
			return this.aggrLevels;
		}
		
		/*
		 * (non-Javadoc)
		 * 
		 * @see org.eclipse.birt.data.engine.olap.util.ICubeAggrDefn#getArguments()
		 */
		public List getArguments( )
		{
			return this.arguments;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.eclipse.birt.data.engine.olap.util.ICubeAggrDefn#getMeasure()
		 */
		public String getMeasure( )
		{
			return this.measure;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.eclipse.birt.data.engine.olap.util.ICubeAggrDefn#getName()
		 */
		public String getName( )
		{
			return this.name;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.eclipse.birt.data.engine.olap.util.ICubeAggrDefn#aggrName()
		 */
		public String getAggrName( )
		{
			return this.aggrName;
		}

		/*
		 * (non-Javadoc)
		 * @see org.eclipse.birt.data.engine.olap.util.ICubeAggrDefn#getFilter()
		 */
		public IBaseExpression getFilter( )
		{
			return this.filterExpression;
		}

	}

	/**
	 * 
	 * @param expr
	 * @return
	 */
	public static boolean isComplexDimensionExpr( String expr )
	{
		if ( expr == null )
			return false;
		if ( expr.matches( "\\Qdimension[\"\\E.*\\Q\"][\"\\E.*\\Q\"]\\E\\S+?" )
				|| expr.matches( "\\S+?\\Qdimension[\"\\E.*\\Q\"][\"\\E.*\\Q\"]\\E" ) )
			return true;
		return false;
	}
	
	/**
	 * 
	 * @param expr
	 * @param bindings
	 * @return
	 * @throws DataException
	 */
	public static boolean isReferenceToAttribute( IBaseExpression expr,
			List bindings ) throws DataException
	{
		Set set = OlapExpressionCompiler.getReferencedDimLevel( expr, bindings );
		if ( set.size( ) != 1 )
		{
			return false;
		}
		for ( Iterator k = set.iterator( ); k.hasNext( ); )
		{
			Object obj = k.next( );
			if ( obj instanceof DimLevel )
			{
				DimLevel dimLevel = (DimLevel) obj;
				if ( dimLevel.getAttrName( ) != null )
				{
					return true;
				}
			}
		}
		return false;
	}
	
	/**
	 * 
	 * @param outResults
	 * @return
	 * @throws DataException
	 */
	public static Scriptable createQueryResultsScriptable(
			IBaseQueryResults outResults ) throws DataException
	{
		if ( outResults instanceof ICubeQueryResults )
		{
			return new JSCubeBindingObject( ( (ICubeQueryResults) outResults ).getCubeCursor( ) );
		}
		else if ( outResults instanceof IQueryService )
		{
			try
			{
				return ( (IQueryService) outResults ).getExecutorHelper( ).getScriptable( );
			}
			catch ( BirtException e )
			{
				throw DataException.wrap( e );
			}
		}
		return null;
	}

}
