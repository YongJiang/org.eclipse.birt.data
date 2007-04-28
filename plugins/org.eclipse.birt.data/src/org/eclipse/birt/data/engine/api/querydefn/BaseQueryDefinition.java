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
package org.eclipse.birt.data.engine.api.querydefn;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.birt.data.engine.api.IBaseExpression;
import org.eclipse.birt.data.engine.api.IBaseQueryDefinition;
import org.eclipse.birt.data.engine.api.IBinding;


/**
 * Default implementation of the {@link org.eclipse.birt.data.engine.api.IBaseQueryDefinition} 
 * interface.
 *
 */

abstract public class BaseQueryDefinition extends BaseTransform implements IBaseQueryDefinition
{
	protected List 		        groups = new ArrayList();
	protected boolean 			hasDetail = true;
	protected IBaseQueryDefinition 	parentQuery;
	protected int				maxRowCount = 0;
	
	private   boolean           cacheQueryResults = false;
	
	//	 order might be sensitive, use LinkedHashMap instead of HashMap
	private 	Map resultExprsMap = new LinkedHashMap( );
	private 	Map bindingMap = new HashMap();
	/**
	 * Constructs an instance with parent set to the specified <code>BaseQueryDefinition</code>
	 */
	BaseQueryDefinition( IBaseQueryDefinition parent )
	{
		parentQuery = parent;
	}
	
	/**
	 * Returns the group definitions as an ordered collection of <code>GroupDefinition</code>
	 * objects. Groups are organizations within the data that support
	 * aggregation, filtering and sorting. Reports use groups to trigger
	 * level breaks.
	 * 
	 * @return the list of groups. If no group is defined, null is returned.
	 */
	
	public List getGroups( )
	{
		return groups;
	}

	/**
	 * Appends a group definition to the group list.
	 * @param group Group definition to add
	 */
	public void addGroup( GroupDefinition group )
	{
	    groups.add( group );
	}
	
	/**
	 * Indicates if the report will use the detail rows. Allows the data
	 * transform engine to optimize the query if the details are not used.
	 * 
	 * @return true if the detail rows are used, false if not used
	 */
	public boolean usesDetails( )
	{
		return hasDetail;
	}
	
	
	/**
	 * @param usesDetails Whether detail rows are used in this query
	 */
	public void setUsesDetails(boolean usesDetails) 
	{
		this.hasDetail = usesDetails;
	}

	/**
	 * Returns the parent query. The parent query is the outer query which encloses
	 * this query
	 */
	public IBaseQueryDefinition getParentQuery() 
	{
		return parentQuery;
	}
	
	/**
	 * Gets the maximum number of detail rows that can be retrieved by this report query
	 * @return Maximum number of rows. If 0, there is no limit on how many rows this query can retrieve.
	 */
	public int getMaxRows( ) 
	{
		return maxRowCount;
	}
	
	/**
	 * Sets the maximum number of detail rows that can be retrieved by this report query
	 * 
	 */
	public void setMaxRows( int maxRows ) 
	{
	    maxRowCount = maxRows;
	}
	
	
	/**
	 * @param name
	 * @param expression
	 * @deprecated
	 */
	public void addResultSetExpression( String name, IBaseExpression expression )
	{
		Binding binding = new Binding( name );
		binding.setExpression(expression );
		this.bindingMap.put( name, binding );
		
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.birt.data.engine.api.IBaseQueryDefinition#addBinding(java.lang.String, org.eclipse.birt.data.engine.api.IBinding)
	 */
	public void addBinding( String name, IBinding binding )
	{
		this.bindingMap.put( name, binding );
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.birt.data.engine.api.IBaseQueryDefinition#getBindings()
	 */
	public Map getBindings( ) 
	{
		for( Iterator it = this.resultExprsMap.keySet( ).iterator( ); it.hasNext( );)
		{
			String key = it.next( ).toString( );
			IBaseExpression expr = (IBaseExpression)this.resultExprsMap.get( key );
			if ( this.bindingMap.get( key ) == null )
			{
				Binding binding = new Binding( key );
				binding.setExpression( expr );
				this.bindingMap.put( key, binding );
			}
		}
		return this.bindingMap;
	}
	
	/* 
	 * @see org.eclipse.birt.data.engine.api.IBaseTransform#getResultSetExpressions()
	 */
	public Map getResultSetExpressions( )
	{
		return this.resultExprsMap;
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.birt.data.engine.api.IBaseQueryDefinition#needCache()
	 */
	public boolean cacheQueryResults() 
	{
		return cacheQueryResults;
	}
	
	/*
	 * 
	 */
	public void setCacheQueryResults( boolean cacheQueryResults )
	{
		this.cacheQueryResults = cacheQueryResults ;
	}
}
