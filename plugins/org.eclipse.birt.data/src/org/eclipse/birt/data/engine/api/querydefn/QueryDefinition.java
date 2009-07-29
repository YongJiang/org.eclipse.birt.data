/*
 *************************************************************************
 * Copyright (c) 2004, 2008 Actuate Corporation.
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
import java.util.Collection;
import java.util.List;

import org.eclipse.birt.data.engine.api.IBaseQueryDefinition;
import org.eclipse.birt.data.engine.api.IQueryDefinition;


/**
 * Default implementation of the {@link org.eclipse.birt.data.engine.api.IQueryDefinition} interface
 */

public class QueryDefinition extends BaseQueryDefinition implements IQueryDefinition 
{
	protected String 			dataSetName;
	private   String            queryResultsID;
	protected List 				bindings = new ArrayList();
	protected String[]			projectedColumns;
	
	private   boolean           autoBinding = false;
	private   boolean           isSummaryQuery = false;
	private IBaseQueryDefinition 	sourceQuery;
	
	/** Constructs an empty query definition */
	public QueryDefinition( )
	{
	   super(null); 
	}
	
	/**
	 * Constructs a  query that is nested within another query. The outer query (parent)
	 * can be another query, or a sub query.
	 * @param parent The outer query or subquery
	 */
	public QueryDefinition( BaseQueryDefinition parent)
	{
		super(parent);
	}
	
	/**
	 * Gets the name of the data set used by this query
	 */
	public String getDataSetName( )
	{
		return dataSetName;
	}
	
	/**
	 * @param dataSetName Name of data set used by this query.
	 */
	public void setDataSetName(String dataSetName) 
	{
		this.dataSetName = dataSetName;
	}
	
	/*
	 * @see org.eclipse.birt.data.engine.api.IQueryDefinition#needAutoBinding()
	 */
	public boolean needAutoBinding( )
	{
		return this.autoBinding;
	}
	
	/**
	 * @param autoBinding
	 */
	public void setAutoBinding( boolean autoBinding )
	{
		this.autoBinding = autoBinding;
	}
	
	/*
	 * @see org.eclipse.birt.data.engine.api.IQueryDefinition#getQueryResultID()
	 */
	public String getQueryResultsID( )
	{
		return this.queryResultsID;
	}
	
	/**
	 * @param queryResultID
	 */
	public void setQueryResultsID( String queryResultsID )
	{
		this.queryResultsID = queryResultsID;
	}
	
	/**
	 * Returns the set of input parameter bindings as an unordered collection
	 * of <code>InputParameterBinding</code> objects.
	 * 
	 * @return the input parameter bindings. If no binding is defined, null is returned.
	 */
	public Collection getInputParamBindings( )
	{
		return bindings;
	}
	
	
	/**
	 * Adds an input parameter binding to this report query.
	 * @param binding The bindings to set.
	 */
	public void addInputParamBinding( InputParameterBinding binding) 
	{
		this.bindings.add(binding);
	}
	
	/**
	 * Provides a column projection hint to the data engine. The caller informs the data engine that only
	 * a selected list of columns defined by the data set are used by this report query. The names of 
	 * those columns (the "projected columns") are passed in as an array of string. <br>
	 * If a column projection is set, runtime error may occur if the report query uses columns 
	 * that are not defined in the projected column list. 
	 */
	public void setColumnProjection( String[] projectedColumns )
	{
	    this.projectedColumns = projectedColumns;
	}
	
	/**
	 * @see org.eclipse.birt.data.engine.api.IQueryDefinition#getColumnProjection()
	 */
	public String[] getColumnProjection()
	{
	    return projectedColumns;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.birt.data.engine.api.IQueryDefinition#getBaseQuery()
	 */
	public IBaseQueryDefinition getSourceQuery( )
	{
		return sourceQuery;
	}
	
	/**
	 * 
	 * @param sourceQuery
	 */
	public void setSourceQuery( IBaseQueryDefinition sourceQuery )
	{
		this.sourceQuery = sourceQuery;
	}

	public void setIsSummaryQuery( boolean isSummaryQuery )
	{
		this.isSummaryQuery = isSummaryQuery;
	}
	public boolean isSummaryQuery( )
	{
		return this.isSummaryQuery;
	}

}
