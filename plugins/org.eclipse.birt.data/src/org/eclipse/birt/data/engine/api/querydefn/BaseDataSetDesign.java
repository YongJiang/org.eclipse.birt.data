/*
 *************************************************************************
 * Copyright (c) 2004, 2005 Actuate Corporation.
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

import org.eclipse.birt.data.engine.api.IBaseDataSetDesign;
import org.eclipse.birt.data.engine.api.IComputedColumn;
import org.eclipse.birt.data.engine.api.IFilterDefinition;
import org.eclipse.birt.data.engine.api.IParameterDefinition;
import org.eclipse.birt.data.engine.api.IInputParameterDefinition;
import org.eclipse.birt.data.engine.api.IOutputParameterDefinition;
import org.eclipse.birt.data.engine.api.IColumnDefinition;
import org.eclipse.birt.data.engine.api.IInputParameterBinding;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;


/**
 * Default implementation of IBaseDataSetDesign interface.<p>
 * Describes the static design of any data set to be used by 
 * the Data Engine.
 * Each subclass defines a specific type of data set. 
 */
public class BaseDataSetDesign implements IBaseDataSetDesign
{
    private String 	name;
    private String 	dataSourceName;
    private List	parameters;
    private List	resultSetHints;
    private List	computedColumns;
    private List	filters;
    private Collection inputParamBindings;
    private String 	beforeOpenScript;
    private String 	afterOpenScript;
    private String 	onFetchScript;
    private String 	beforeCloseScript;
    private String 	afterCloseScript;
	
	/**
	 * Instantiates a data set with given name.
	 * @param name Name of data set
	 */
	public BaseDataSetDesign( String name )
	{
		this.name = name;
	}
	
	/**
	 * Instantiates a data set with given name and data source name.
	 * @param name Name of data set
	 * @param dataSourceName Name of data source used by this data set. 
	 * 						Can be null or empty if this data
	 * 						set does not specify a data source.
	 */
	public BaseDataSetDesign( String name, String dataSourceName )
	{
		this.name = name;
		this.dataSourceName = dataSourceName;
	}
	
    /* (non-Javadoc)
     * @see org.eclipse.birt.data.engine.api.IBaseDataSetDesign#getName()
     */	
	public String getName( )
	{
		return name;
	}
	
    /* (non-Javadoc)
     * @see org.eclipse.birt.data.engine.api.IBaseDataSetDesign#getDataSourceName()
     */	
	public String getDataSourceName( )
	{
		return dataSourceName;
	}

	/**
	 * Specifies the data source (connection) name.
	 * @param dataSource The name of the dataSource to set.
	 */
	public void setDataSource( String dataSourceName ) 
	{
		this.dataSourceName = dataSourceName;
	}
	
    /* (non-Javadoc)
     * @see org.eclipse.birt.data.engine.api.IBaseDataSetDesign#getComputedColumns()()
     */	
	public List getComputedColumns( )
	{
	    if ( computedColumns == null )
	        computedColumns = new ArrayList();
		return computedColumns;
	}
	
	/**
	 * Adds a new computed column to the data set.
	 * Ignores given computed column if null.
	 * @param column	Could be null.
	 */
	public void addComputedColumn( IComputedColumn column )
	{
	    if ( column != null )
	        getComputedColumns().add( column );
	}
	
    /* (non-Javadoc)
     * @see org.eclipse.birt.data.engine.api.IBaseDataSetDesign#getFilters()
     */	
	public List getFilters( )
	{
	    if ( filters == null )
	        filters = new ArrayList();
	    return filters;
	}
	
	/**
	 * Adds a filter to the filter list.
	 * Ignores given filter if null.
	 * @param filter	Could be null.
	 */
	public void addFilter( IFilterDefinition filter )
	{
	    if ( filter != null )
	        getFilters().add( filter );
	}
    
    /* (non-Javadoc)
     * @see org.eclipse.birt.data.engine.api.IBaseDataSetDesign#getParameters()
     */	
    public List getParameters()
    {
	    if ( parameters == null )
	        parameters = new ArrayList();
		return parameters;
    }
	
	/**
	 * Adds a parameter definition to the data set.
	 */
	public void addParameter( IParameterDefinition param )
	{
	    if ( param != null )
	        getParameters().add( param );
	}
	
    /**
     * Returns the input parameter definitions as a list
     * of IInputParameterDefinition objects. 
     * @return the input parameter definitions. 
     * 			An empty list if none is defined.
     * @deprecated use getParameters()
     */
	public List getInputParameters( )
	{
	    List params = getParameters();
	    if ( params.isEmpty() )
	        return params;
	    
	    // iterate through the parameters list, and return only
	    // those that are of input mode
        List inputParams = new ArrayList();
		Iterator paramItr = params.iterator( );
		while ( paramItr.hasNext() )
		{
		    IParameterDefinition paramDefn = (IParameterDefinition) paramItr.next();
		    if ( paramDefn.isInputMode() )
		        inputParams.add( paramDefn );
		}
		return inputParams;
	}
	
	/**
	 * Adds an input paramter definition to the data set.
     * @deprecated use addParameter()
	 */
	public void addInputParameter( IInputParameterDefinition param )
	{
	    addParameter( param );
	}
	
    /**
     * Returns the output parameter definitions as a list
     * of IOutputParameterDefinition objects.
     * @return the output parameter definitions. 
     * 			An empty list if none is defined.
     * @deprecated use getParameters()
     */
	public List getOutputParameters( )
	{
	    List params = getParameters();
	    if ( params.isEmpty() )
	        return params;
	    
	    // iterate through the parameters list, and return only
	    // those that are of output mode
        List outputParams = new ArrayList();
		Iterator paramItr = params.iterator( );
		while ( paramItr.hasNext() )
		{
		    IParameterDefinition paramDefn = (IParameterDefinition) paramItr.next();
		    if ( paramDefn.isOutputMode() )
		        outputParams.add( paramDefn );
		}
		return outputParams;
	}
	
	/**
	 * Adds an output paramter definition to the list.
     * @deprecated use addParameter()
	 */
	public void addOutputParameter( IOutputParameterDefinition param )
	{
	    addParameter( param );
	}
		
    /* (non-Javadoc)
     * @see org.eclipse.birt.data.engine.api.IBaseDataSetDesign#getResultSetHints()
     */	
	public List getResultSetHints()
	{
	    if ( resultSetHints == null )
	        resultSetHints = new ArrayList();
		return resultSetHints;
	}
	
	/**
	 * Adds a column to the result set hints definition.
	 * @param col
	 */
	public void addResultSetHint( IColumnDefinition col )
	{
	    if ( col != null )
	        getResultSetHints().add( col );
	}

    /* (non-Javadoc)
     * @see org.eclipse.birt.data.engine.api.IBaseDataSetDesign#getInputParamBindings()
     */	
	public Collection getInputParamBindings()
	{
	    if ( inputParamBindings == null )
	        inputParamBindings = new ArrayList();
	    return inputParamBindings;
	}
	
	/**
	 * Adds an IInputParamBinding to the set of input parameter bindings.
	 * Ignores given binding if null.
	 * @param binding	Could be null.
	 */
	public void addInputParamBinding( IInputParameterBinding binding )
	{
	    if ( binding != null )
	        getInputParamBindings().add( binding );
	}
	
    /* (non-Javadoc)
     * @see org.eclipse.birt.data.engine.api.IBaseDataSetDesign#getBeforeOpenScript()
     */	
	public String getBeforeOpenScript( )
	{
		return beforeOpenScript;
	}

	/**
	 * Assigns the BeforeOpen script.
	 * @param beforeOpenScript The BeforeOpen script to set.
	 */
	public void setBeforeOpenScript( String beforeOpenScript ) 
	{
		this.beforeOpenScript = beforeOpenScript;
	}
	
    /* (non-Javadoc)
     * @see org.eclipse.birt.data.engine.api.IBaseDataSetDesign#getAfterOpenScript()
     */	
	public String getAfterOpenScript( )
	{
		return afterOpenScript;
	}

	/**
	 * Assigns the AfterOpen script.
	 * @param afterOpenScript The AfterOpen script to set.
	 */
	public void setAfterOpenScript( String afterOpenScript ) 
	{
		this.afterOpenScript = afterOpenScript;
	}
	
    /* (non-Javadoc)
     * @see org.eclipse.birt.data.engine.api.IBaseDataSetDesign#getOnFetchScript()
     */	
	public String getOnFetchScript( )
	{
		return onFetchScript;
	}
	
	/**
	 * Specifies the OnFetch script.
	 * @param onFetchScript The OnFetch script to set.
	 */
	public void setOnFetchScript( String onFetchScript ) 
	{
		this.onFetchScript = onFetchScript;
	}
	
    /* (non-Javadoc)
     * @see org.eclipse.birt.data.engine.api.IBaseDataSetDesign#getBeforeCloseScript()
     */	
	public String getBeforeCloseScript( )
	{
		return beforeCloseScript;
	}
	
	/**
	 * Specifies the BeforeClose script.
	 * @param beforeCloseScript The BeforeClose script to set.
	 */
	public void setBeforeCloseScript( String beforeCloseScript ) 
	{
		this.beforeCloseScript = beforeCloseScript;
	}

    /* (non-Javadoc)
     * @see org.eclipse.birt.data.engine.api.IBaseDataSetDesign#getAfterCloseScript()
     */	
	public String getAfterCloseScript( )
	{
		return afterCloseScript;
	}

	/**
	 * Specifies the AfterClose script.
	 * @param afterCloseScript The AfterClose script to set.
	 */
	public void setAfterCloseScript( String afterCloseScript ) 
	{
		this.afterCloseScript = afterCloseScript;
	}

}
