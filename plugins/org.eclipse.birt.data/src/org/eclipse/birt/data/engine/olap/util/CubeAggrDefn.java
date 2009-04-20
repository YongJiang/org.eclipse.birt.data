
/*******************************************************************************
 * Copyright (c) 2004, 2008 Actuate Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Actuate Corporation  - initial API and implementation
 *******************************************************************************/
package org.eclipse.birt.data.engine.olap.util;

import java.util.List;

import org.eclipse.birt.data.engine.api.IBaseExpression;

/**
 * This is a class used to describe a measure that need to be calculated during 
 * Olap query execution.
 */
public abstract class CubeAggrDefn
{
	//
	private String name;
	private List aggrLevels, arguments;
	private String aggrName;
	private IBaseExpression filterExpression;

	/*
	 * 
	 */
	CubeAggrDefn( String name, List aggrLevels,
			String aggrName, List arguments, IBaseExpression filterExpression )
	{
		assert name != null;
		assert aggrLevels != null;

		this.name = name;
		this.aggrLevels = aggrLevels;
		this.aggrName = aggrName;
		this.arguments = arguments;
		this.filterExpression = filterExpression;
	}
	

	/**
	 * Return a list of levels that the aggregations is based.
	 * @return
	 */
	public List getAggrLevelsInAggregationResult( )
	{
		return this.aggrLevels;
	}
	
	public List getAggrLevelsInDefinition( )
	{
		return this.aggrLevels;
	}
	
	/**
	 * Return a list of arguments that the aggregations is based.
	 * @return
	 */
	public List getArguments( )
	{
		return this.arguments;
	}


	/**
	 * Return the name of the cube aggregation definition. Usually it is a binding name.
	 * 
	 * @return
	 */
	public String getName( )
	{
		return this.name;
	}

	/**
	 * Return the name of the aggregation operation.
	 * @return
	 */
	public String getAggrName( )
	{
		return this.aggrName;
	}

	/**
	 * Return FilterDefinition in aggregation definition
	 * 
	 * @return
	 */
	public IBaseExpression getFilter( )
	{
		return this.filterExpression;
	}
	
	public String[] getFirstArgumentInfo( )
	{
		if ( this.arguments == null || this.arguments.isEmpty( ) )
		{
			return new String[0];
		}
		else
			return (String[]) this.arguments.get( 0 );
	}
	
	/**
	 * 
	 * @return the target measure of IDataSet4Aggregation where this aggregation operates
	 */
	public abstract String getMeasure( );
}
