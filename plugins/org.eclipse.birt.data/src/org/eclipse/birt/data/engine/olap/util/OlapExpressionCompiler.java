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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.birt.data.engine.api.IBaseExpression;
import org.eclipse.birt.data.engine.api.IBinding;
import org.eclipse.birt.data.engine.api.IConditionalExpression;
import org.eclipse.birt.data.engine.api.IScriptExpression;
import org.eclipse.birt.data.engine.core.DataException;
import org.eclipse.birt.data.engine.olap.data.api.DimLevel;
import org.mozilla.javascript.CompilerEnvirons;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Node;
import org.mozilla.javascript.Parser;
import org.mozilla.javascript.ScriptOrFnNode;
import org.mozilla.javascript.Token;

/**
 * 
 */

public class OlapExpressionCompiler
{

	/**
	 * Get referenced Script Object (dimension, data, measure, etc) according to
	 * given object name.
	 * 
	 * @param expr
	 * @param objectName
	 * @return
	 */
	public static String getReferencedScriptObject( IBaseExpression expr,
			String objectName )
	{
		if ( expr instanceof IScriptExpression )
		{
			return getReferencedScriptObject( ( (IScriptExpression) expr ),
					objectName );
		}
		else if ( expr instanceof IConditionalExpression )
		{
			String dimName = null;
			IScriptExpression expr1 = ( (IConditionalExpression) expr ).getExpression( );
			dimName = getReferencedScriptObject( expr1, objectName );
			if ( dimName != null )
				return dimName;
			IScriptExpression op1 = ( (IConditionalExpression) expr ).getOperand1( );
			dimName = getReferencedScriptObject( op1, objectName );
			if ( dimName != null )
				return dimName;

			IScriptExpression op2 = ( (IConditionalExpression) expr ).getOperand2( );
			dimName = getReferencedScriptObject( op2, objectName );
			return dimName;
		}

		return null;
	}

	/**
	 * 
	 * @param expr
	 * @param objectName
	 * @return
	 */
	private static String getReferencedScriptObject( IScriptExpression expr,
			String objectName )
	{
		if ( expr == null )
			return null;
		else
			return getReferencedScriptObject( expr.getText( ), objectName );
	}

	/**
	 * 
	 * @param expr
	 * @param objectName
	 * @return
	 */
	public static String getReferencedScriptObject( String expr,
			String objectName )
	{
		if ( expr == null )
			return null;
		try
		{
			Context cx = Context.enter( );
			CompilerEnvirons ce = new CompilerEnvirons( );
			Parser p = new Parser( ce, cx.getErrorReporter( ) );
			ScriptOrFnNode tree = p.parse( expr, null, 0 );

			return getScriptObjectName( tree, objectName );
		}
		finally
		{
			Context.exit( );
		}
	}

	/**
	 * 
	 * @param expr
	 * @param bindings
	 * @return
	 * @throws DataException
	 */
	public static Set getReferencedDimLevel( IBaseExpression expr, List bindings )
			throws DataException
	{
		return getReferencedDimLevel( expr, bindings, false );
	}

	/**
	 * Get set of reference DimLevels.
	 * 
	 * @param expr
	 * @param bindings
	 * @param onlyFromDirectReferenceExpr
	 * @return
	 * @throws DataException
	 */
	public static Set getReferencedDimLevel( IBaseExpression expr,
			List bindings, boolean onlyFromDirectReferenceExpr )
			throws DataException
	{
		if ( expr instanceof IScriptExpression )
		{
			return getReferencedDimLevel( ( (IScriptExpression) expr ),
					bindings,
					onlyFromDirectReferenceExpr );
		}
		else if ( expr instanceof IConditionalExpression )
		{
			Set result = new HashSet( );
			IScriptExpression expr1 = ( (IConditionalExpression) expr ).getExpression( );
			result.addAll( getReferencedDimLevel( expr1,
					bindings,
					onlyFromDirectReferenceExpr ) );

			IScriptExpression op1 = ( (IConditionalExpression) expr ).getOperand1( );
			result.addAll( getReferencedDimLevel( op1,
					bindings,
					onlyFromDirectReferenceExpr ) );

			IScriptExpression op2 = ( (IConditionalExpression) expr ).getOperand2( );
			result.addAll( getReferencedDimLevel( op2,
					bindings,
					onlyFromDirectReferenceExpr ) );
			return result;
		}

		return null;
	}

	/**
	 * 
	 * @param expr
	 * @param bindings
	 * @param onlyFromDirectReferenceExpr
	 * @return
	 * @throws DataException
	 */
	private static Set getReferencedDimLevel( IScriptExpression expr,
			List bindings, boolean onlyFromDirectReferenceExpr )
			throws DataException
	{
		if ( expr == null )
			return new HashSet( );

		try
		{
			Set result = new HashSet( );
			Context cx = Context.enter( );
			CompilerEnvirons ce = new CompilerEnvirons( );
			Parser p = new Parser( ce, cx.getErrorReporter( ) );
			ScriptOrFnNode tree = p.parse( expr.getText( ), null, 0 );

			populateDimLevels( null,
					tree,
					result,
					bindings,
					onlyFromDirectReferenceExpr );
			return result;
		}
		finally
		{
			Context.exit( );
		}
	}

	/**
	 * 
	 * @param n
	 * @param result
	 * @param bindings
	 * @param onlyFromDirectReferenceExpr
	 * @throws DataException
	 */
	private static void populateDimLevels( Node grandpa, Node n, Set result,
			List bindings, boolean onlyFromDirectReferenceExpr )
			throws DataException
	{
		if ( n == null )
			return;
		if ( onlyFromDirectReferenceExpr )
		{
			if ( n.getType( ) == Token.SCRIPT )
			{
				if ( n.getFirstChild( ) == null
						|| n.getFirstChild( ).getType( ) != Token.EXPR_RESULT )
					return;
				if ( n.getFirstChild( ).getFirstChild( ) == null
						|| ( n.getFirstChild( ).getFirstChild( ).getType( ) != Token.GETPROP && n.getFirstChild( )
								.getFirstChild( )
								.getType( ) != Token.GETELEM ) )
					return;
			}
		}
		if ( n.getFirstChild( ) != null
				&& ( n.getType( ) == Token.GETPROP || n.getType( ) == Token.GETELEM ) )
		{
			if ( n.getFirstChild( ).getFirstChild( ) != null
					&& ( n.getFirstChild( ).getFirstChild( ).getType( ) == Token.GETPROP || n.getFirstChild( )
							.getFirstChild( )
							.getType( ) == Token.GETELEM ) )
			{
				Node dim = n.getFirstChild( ).getFirstChild( );
				if ( "dimension".equals( dim.getFirstChild( ).getString( ) ) )
				{
					String dimName = dim.getLastChild( ).getString( );
					String levelName = dim.getNext( ).getString( );
					String attr = n.getLastChild( ).getString( );

					DimLevel dimLevel = new DimLevel( dimName, levelName, attr );
					if ( !result.contains( dimLevel ) )
						result.add( dimLevel );
				}
			}
			else if ( n.getFirstChild( ) != null
					&& n.getFirstChild( ).getType( ) == Token.NAME )
			{
				if ( "dimension".equals( n.getFirstChild( ).getString( ) ) )
				{
					if ( n.getLastChild( ) != null && n.getNext( ) != null )
					{
						String dimName = n.getLastChild( ).getString( );
						String levelName = n.getNext( ).getString( );
						String attr = null;
						if ( grandpa != null
								&& grandpa.getNext( ) != null
								&& grandpa.getNext( ).getType( ) == Token.STRING )
						{
							attr = grandpa.getNext( ).getString( );
						}
						DimLevel dimLevel = new DimLevel( dimName,
								levelName,
								attr );
						if ( !result.contains( dimLevel ) )
							result.add( dimLevel );
					}
				}
				else if ( "data".equals( n.getFirstChild( ).getString( ) ) )
				{
					if ( n.getLastChild( ) != null )
					{
						String bindingName = n.getLastChild( ).getString( );
						IBinding binding = getBinding( bindings, bindingName );
						if ( binding != null )
						{
							result.addAll( getReferencedDimLevel( binding.getExpression( ),
									bindings,
									onlyFromDirectReferenceExpr ) );
						}
					}
				}
			}
		}
		populateDimLevels( grandpa,
				n.getFirstChild( ),
				result,
				bindings,
				onlyFromDirectReferenceExpr );
		populateDimLevels( grandpa,
				n.getLastChild( ),
				result,
				bindings,
				onlyFromDirectReferenceExpr );
	}

	/**
	 * Get binding
	 * 
	 * @param bindings
	 * @param bindingName
	 * @return
	 * @throws DataException
	 */
	private static IBinding getBinding( List bindings, String bindingName )
			throws DataException
	{
		for ( int i = 0; i < bindings.size( ); i++ )
		{
			if ( ( (IBinding) bindings.get( i ) ).getBindingName( )
					.equals( bindingName ) )
				return (IBinding) bindings.get( i );
		}
		return null;
	}

	/**
	 * 
	 * @param n
	 * @param objectName
	 * @return
	 */
	private static String getScriptObjectName( Node n, String objectName )
	{
		if ( n == null )
			return null;
		String result = null;
		if ( n.getType( ) == Token.NAME )
		{
			if ( objectName.equals( n.getString( ) ) )
			{
				Node dimNameNode = n.getNext( );
				if ( dimNameNode == null
						|| dimNameNode.getType( ) != Token.STRING )
					return null;

				return dimNameNode.getString( );
			}
		}

		result = getScriptObjectName( n.getFirstChild( ), objectName );
		if ( result == null )
			result = getScriptObjectName( n.getLastChild( ), objectName );

		return result;
	}
}
