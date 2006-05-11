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

package org.eclipse.birt.data.engine.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.eclipse.birt.data.engine.api.IBaseExpression;
import org.eclipse.birt.data.engine.api.IConditionalExpression;
import org.eclipse.birt.data.engine.api.IScriptExpression;
import org.eclipse.birt.data.engine.core.DataException;
import org.eclipse.birt.data.engine.expression.ExpressionCompilerUtil;
import org.eclipse.birt.data.engine.expression.ExpressionParserUtil;
import org.eclipse.birt.data.engine.i18n.ResourceConstants;

/**
 * This is a utility class which is used to validate the colum bindings defined 
 * in ExprManager instance.
 */
public class ExprManagerUtil
{
	private ExprManagerUtil(){}
	
	/**
	 *	This method tests whether column bindings in ExprManager is valid or not.
	 * 
	 * @param exprManager
	 * @return
	 * @throws DataException
	 */
	public static void validateColumnBinding( ExprManager exprManager )
			throws DataException
	{
		testDependencyCycle( exprManager ); 
		testGroupNameValidation( exprManager);
	}

	/**
	 *	Test whether high level group keys are depended on low level group keys.
	 * 
	 * @param exprManager
	 * @return
	 * @throws DataException 
	 */
	private static void testGroupNameValidation( ExprManager exprManager ) throws DataException
	{
		HashMap map = exprManager.getGroupKeys( );
		Iterator it = map.keySet( ).iterator( );
		while ( it.hasNext( ) )
		{
			Integer level = (Integer)it.next( );
			exprManager.setEntryGroupLevel( level.intValue( ) );
			
			if(!ExpressionCompilerUtil.hasColumnRow( map.get( level ).toString( ), exprManager ))
			{
				exprManager.setEntryGroupLevel( ExprManager.OVERALL_GROUP );
				throw new DataException( ResourceConstants.INVALID_GROUP_KEY, new Object[]{ map.get( level ).toString( ), level});
			}
		}
		exprManager.setEntryGroupLevel( ExprManager.OVERALL_GROUP );
	}
	
	/**
	 * Test whether there are dependency cycles in exprManager.
	 * 
	 * @param exprManager
	 * @return
	 * @throws DataException
	 */
	private static void testDependencyCycle( ExprManager exprManager ) throws DataException
	{
		List result = new ArrayList( );
		Iterator it = exprManager.getColumnNames( ).iterator( );

		while ( it.hasNext( ) )
		{
			String name = it.next( ).toString( );
			Node n = new Node( name );
			IBaseExpression expr = exprManager.getExpr( name );
			if ( expr != null )
			{
				if ( !( expr instanceof IScriptExpression || expr instanceof IConditionalExpression ) )
				{
					throw new DataException( ResourceConstants.BAD_DATA_EXPRESSION );
				}

				List l = null;
				try
				{
					if ( expr instanceof IScriptExpression )
						l = ExpressionParserUtil.extractColumnExpression( (IScriptExpression) expr );
					else if ( expr instanceof IConditionalExpression )
						l = ExpressionParserUtil.extractColumnExpression( (IConditionalExpression) expr );
				}
				catch ( DataException e )
				{
					// Do nothing.The mal-formatted expression should not prevent
					//other correct expression from being evaluated and displayed.
				}
				
				if ( l != null )
				{
					for ( int j = 0; j < l.size( ); j++ )
					{
						n.addChild( new Node( l.get( j ) == null ? null
								: l.get( j ).toString( ) ) );
					}
				}
			}
			result.add( n );
		}
		Node[] source = new Node[result.size( )];
		for ( int i = 0; i < source.length; i++ )
		{
			source[i] = (Node) result.get( i );
		}

		validateNodes( source );
	}

	/**
	 * 
	 * @param source
	 * @return
	 * @throws DataException 
	 */
	private static void validateNodes( Node[] source ) throws DataException
	{
		Node[] preparedNodes = populateNodeList( source );
		for ( int i = 0; i < preparedNodes.length; i++ )
		{
			isValidNode( preparedNodes[i], preparedNodes[i] );
		}
	}

	/**
	 * 
	 * @param startNode
	 * @param candidateNode
	 * @return
	 * @throws DataException 
	 */
	private static void isValidNode( Node startNode, Node candidateNode ) throws DataException
	{
		//boolean result = true;
		Object[] nodes = startNode.getChildren( ).toArray( );
		for ( int i = 0; i < nodes.length; i++ )
		{
			if ( candidateNode.equals( nodes[i] )
					|| startNode.equals( nodes[i] ) )
			{
				throw new DataException( ResourceConstants.COLUMN_BINDING_CYCLE,((Node)nodes[i]).getValue( ));
			}
			else
			{
				isValidNode( (Node) nodes[i], candidateNode );
			}
		}
	}

	/**
	 * 
	 * @param source
	 * @return
	 */
	private static Node[] populateNodeList( Node[] source )
	{
		Node[] result = new Node[source.length];
		for ( int i = 0; i < result.length; i++ )
		{
			result[i] = new Node( source[i].getValue( ) );
		}

		for ( int i = 0; i < result.length; i++ )
		{
			List l = source[i].getChildren( );
			for ( int j = 0; j < l.size( ); j++ )
			{
				Node n = getMatchedNode( (Node) l.get( j ), result );
				//If matched node found.
				if( n!= null )
					result[i].addChild( n );
			}
		}
		return result;
	}

	/**
	 * 
	 * @param node
	 * @param nodes
	 * @return
	 */
	private static Node getMatchedNode( Node node, Node[] nodes )
	{
		for ( int i = 0; i < nodes.length; i++ )
		{
			if ( nodes[i].equals( node ) )
				return nodes[i];
		}

		return null;
	}
}

/**
 * 
 *
 */
class Node
{

	/**
	 * 
	 */
	private List children;
	private String value;

	/**
	 * 
	 * @param value
	 */
	Node( String value )
	{
		this.value = value;
		this.children = new ArrayList( );
	}

	/**
	 * 
	 * @return
	 */
	public String getValue( )
	{
		return this.value;
	}

	/**
	 * 
	 * @param n
	 */
	public void addChild( Node n )
	{
		this.children.add( n );
	}

	/**
	 * 
	 * @return
	 */
	public List getChildren( )
	{
		return this.children;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals( Object o )
	{
		if ( ( o instanceof Node ) && ( (Node) o ).value.equals( this.value ) )
			return true;
		return false;
	}
}
