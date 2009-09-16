
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
package org.eclipse.birt.data.engine.api.querydefn;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.birt.core.data.DataType;
import org.eclipse.birt.data.engine.api.IBaseExpression;
import org.eclipse.birt.data.engine.api.IBinding;
import org.eclipse.birt.data.engine.api.aggregation.AggregationManager;
import org.eclipse.birt.data.engine.api.aggregation.IAggrFunction;
import org.eclipse.birt.data.engine.api.aggregation.IParameterDefn;
import org.eclipse.birt.data.engine.core.DataException;

/**
 * 
 */

public class Binding implements IBinding
{
	private List aggregateOn;
	private Map argument;
	private List orderedArgument;
	private IBaseExpression expr;
	private IBaseExpression filter;
	private String aggrFunc;
	private String name;
	private String displayName;
	private int dataType;
	private boolean exportable;
	
	public Binding( String name )
	{
		this ( name, null );
	}

	public Binding( String name, IBaseExpression expr )
	{
		this.name = name;
		this.expr = expr;
		this.aggregateOn = new ArrayList();
		this.argument = new LinkedHashMap();
		this.orderedArgument = new ArrayList();
		if ( expr != null )
			this.dataType = expr.getDataType( );
		else
			this.dataType = DataType.ANY_TYPE;
		this.exportable = true;
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.birt.data.engine.api.IBinding#addAggregateOn(java.lang.String)
	 */
	public void addAggregateOn( String levelName ) throws DataException
	{
		if ( !this.aggregateOn.contains( levelName ) )
		{
			this.aggregateOn.add( levelName );
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.birt.data.engine.api.IBinding#addArgument(org.eclipse.birt.data.engine.api.IBaseExpression)
	 */
	public void addArgument( IBaseExpression expr )
	{
		if( expr!= null )
			this.orderedArgument.add( expr );
		
	}
	
	public void addArgument( String name, IBaseExpression expr )
	{
		this.argument.put( name, expr );
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.birt.data.engine.api.IBinding#getAggrFunction()
	 */
	public String getAggrFunction( )
	{
		return aggrFunc;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.birt.data.engine.api.IBinding#getAggregatOns()
	 */
	public List getAggregatOns( )
	{
		return this.aggregateOn;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.birt.data.engine.api.IBinding#getArguments()
	 */
	public List getArguments( ) throws DataException
	{
		if( this.aggrFunc == null )
			return this.orderedArgument;
		
		if( this.orderedArgument.size() > 0 )
			return this.orderedArgument;
		
		IAggrFunction info = AggregationManager.getInstance().getAggregation( this.aggrFunc );
		
		if( info == null )
			return this.orderedArgument;
		
		IParameterDefn[] parameters =  info.getParameterDefn();
		
		if( parameters!= null )
		{
			for( int i = 0; i < parameters.length; i++ )
			{
				IParameterDefn pInfo = parameters[i];
				if( this.argument.get( pInfo.getName()) != null )
				{
					orderedArgument.add( this.argument.get(pInfo.getName()));
				}
			}
		}
		return orderedArgument;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.birt.data.engine.api.IBinding#getDataType()
	 */
	public int getDataType( )
	{
		return this.dataType;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.birt.data.engine.api.IBinding#getFilter()
	 */
	public IBaseExpression getFilter( )
	{
		return this.filter;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.birt.data.engine.api.IBinding#setAggrFunction(java.lang.String)
	 */
	public void setAggrFunction( String functionName )
	{
		this.aggrFunc = functionName;
		
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.birt.data.engine.api.IBinding#setDataType(int)
	 */
	public void setDataType( int type )
	{
		this.dataType = type;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.birt.data.engine.api.IBinding#setExpression(java.lang.String)
	 */
	public void setExpression( IBaseExpression expr )
	{
		this.expr = expr;
		
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.birt.data.engine.api.IBinding#setFilter(org.eclipse.birt.data.engine.api.IBaseExpression)
	 */
	public void setFilter( IBaseExpression expr )
	{
		this.filter = expr;
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.birt.data.engine.api.IBinding#getBindingName()
	 */
	public String getBindingName( )
	{
		return this.name;
	}

	/**
	 * @see org.eclipse.birt.data.engine.api.IBinding#getExpression()
	 */
	public IBaseExpression getExpression( )
	{
		return this.expr;
	}

	/**
	 * @see org.eclipse.birt.data.engine.api.IBinding#getDisplayName()
	 */
	public String getDisplayName( ) throws DataException
	{
		return this.displayName == null ? this.name:this.displayName;
	}

	/**
	 * @see org.eclipse.birt.data.engine.api.IBinding#setDisplayName(String)
	 */
	public void setDisplayName( String displayName ) throws DataException
	{
		this.displayName = displayName;
	}

	@Override
	public int hashCode( )
	{
		return name.hashCode( );
	}

	@Override
	public boolean equals( Object obj )
	{
		if ( this == obj )
			return true;
		if ( obj == null )
			return false;
		if ( getClass( ) != obj.getClass( ) )
			return false;
		Binding other = (Binding) obj;
		return name.equals( other.getBindingName( ) );
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.birt.data.engine.api.IBinding#exportable()
	 */
	public boolean exportable( ) throws DataException
	{
		return this.exportable;
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.birt.data.engine.api.IBinding#setExportable(boolean)
	 */
	public void setExportable( boolean exportable ) throws DataException
	{
		this.exportable = exportable;
	}
}