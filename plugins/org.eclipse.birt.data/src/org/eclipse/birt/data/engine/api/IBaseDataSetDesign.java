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

package org.eclipse.birt.data.engine.api;

import java.util.Collection;
import java.util.List;

import org.eclipse.birt.data.engine.api.script.IBaseDataSetEventHandler;

/**
 * Describes the static design of any data set to be used by 
 * the Data Engine.
 * Each sub-interface defines a specific type of data set. 
 */
public interface IBaseDataSetDesign
{
    /**
     * Gets the name of the data set.
     * @return Name of data set.
     */
    public abstract String getName();
    
    /**
	 * When cache option is true, user needs to specify how many rows will be
	 * retrieved into cache for use.
	 * 
	 * @return cache row count
	 */
    public abstract int getCacheRowCount( );
    
    /**
	 * When user wants to retrieve the distinct row, this flag needs to be set
	 * as true. The distinct row means there is no two rows which will have the
	 * same value on all columns.
	 * 
	 * @return true,  distinct row is required
	 * 		   false, no distinct requirement on row
	 */
	public boolean needDistinctValue( );

    /**
     * Returns the data source (connection) name for this data set. 
     * 
     * @return Name of the data source (connection) for this data set.
     */
    public abstract String getDataSourceName();

    /**
     * Returns a list of computed columns. Contains
     * IComputedColumn objects. Computed columns must be computed before
     * applying filters.
     * @return the computed columns.  An empty list if none is defined.
     */
    public abstract List getComputedColumns();

    /**
     * Returns a list of filters. The List contains {@link org.eclipse.birt.data.engine.api.IFilterDefinition}
     *  objects. The data set should discard any
     * row that does not satisfy all the filters.
     * @return the filters. An empty list if none is defined.
     */
    public abstract List getFilters();

    /**
     * Returns the data set parameter definitions as a list
     * of {@link org.eclipse.birt.data.engine.api.IParameterDefinition} objects. 
     * @return the parameter definitions. 
     * 			An empty list if none is defined.
     */
    public abstract List getParameters();

    /**
     * Returns the primary result set hints as a list of 
     * {@link org.eclipse.birt.data.engine.api.IColumnDefinition}
     * objects. 
     * @return the result set hints as a list of <code>IColumnDefinition</code> objects.
     * 			An empty list if none is defined, which normally
     * 			means that the data set can provide the definition 
     * 			from the underlying data access provider.
     */
    public abstract List getResultSetHints();
	
	/**
	 * Returns the set of input parameter bindings as an unordered collection
	 * of {@link org.eclipse.birt.data.engine.api.IInputParameterBinding} objects.
	 * @return the input parameter bindings. 
	 * 			An empty collection if none is defined.
	 */
	public abstract Collection getInputParamBindings( );

    /**
     * Returns the <code>beforeOpen</code> script to be called just before opening the data
     * set.
     * @return the <code>beforeOpen</code> script. Null if none is defined.
     */
    public abstract String getBeforeOpenScript();

    /**
     * Returns the <code>afterOpen</code> script to be called just after the data set is
     * opened, but before fetching each row.
     * @return the <code>afterOpen</code> script.  Null if none is defined.
     */
    public abstract String getAfterOpenScript();

    /**
     * Returns the <code>onFetch</code> script to be called just after the a row is read
     * from the data set. Called after setting computed columns and only for
     * rows that pass the filters. (Not called for rows that are filtered out
     * of the data set.)
     * @return the <code>onFetch</code> script. Null if none is defined.
     */
    public abstract String getOnFetchScript();

    /**
     * Returns the <code>beforeClose</code> script to be called just before closing the
     * data set.
     * @return the <code>beforeClose</code> script.  Null if none is defined.
     */
    public abstract String getBeforeCloseScript();

    /**
     * Returns the <code>afterClose</code> script to be called just after the data set is
     * closed.
     * @return the <code>afterClose</code> script.  Null if none is defined.
     */
    public abstract String getAfterCloseScript();
    
	/**
	 * Returns the event handler for the data set
	 */ 
	public abstract IBaseDataSetEventHandler getEventHandler( );

}