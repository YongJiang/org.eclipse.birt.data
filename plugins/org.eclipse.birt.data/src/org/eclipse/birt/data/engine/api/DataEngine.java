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

import java.io.File;
import java.util.Map;

import org.mozilla.javascript.Scriptable;
import org.eclipse.birt.core.exception.BirtException;
import org.eclipse.birt.data.engine.core.DataException;
import org.eclipse.birt.data.engine.impl.DataEngineImpl;

/**
 * Data Engine API class.
 * <br>
 * Provides methods to define data sources and data sets, and to prepare a
 * {@link org.eclipse.birt.data.engine.api.IQueryDefinition}. An application
 * typically needs only one instance of this class, which can be used to 
 * prepare and execute multiple data queries.
 * <p>
 * User of this class must always call the <code>shutdown</code> method when it is
 * done with an instance of this class to ensure release of all data source 
 * connections and related resources.
 */
abstract public class DataEngine
{

    /**
	 * Creates a new instance of DataEngine, using the specified
	 * DataEngineContext as its running environment
	 * 
	 * @param context,
	 *            When this value is null, a default context will be used. The
	 *            default context is DataEngineContext.MODE_DIRECTPRESENT.
	 * @return an instance of DataEngine under specified context
	 */
    public static DataEngine newDataEngine( DataEngineContext context )
    {    	
    	if ( context == null )
		{
			try
			{
				context = DataEngineContext.newInstance( DataEngineContext.MODE_DIRECTPRESENT,
						null,
						null, 
						null);
			}
			catch ( BirtException e )
			{
				// impossible get here
			}
		}
    	
		return new DataEngineImpl( context );
    }

    /**
	 * Creates a new instance of DataEngine, using the specified Javascript
	 * scope and home directory setting.
	 * 
	 * @param sharedScope
	 *            a Javascript scope to be used as the "shared" scope to
	 *            evaluate Javascript expressions by the data engine.
	 * @deprecated use newDataEngine( DataEngineContext context ) instead
	 */
    public static DataEngine newDataEngine( Scriptable sharedScope )
	{
		try
		{
			return newDataEngine( DataEngineContext.newInstance( DataEngineContext.MODE_DIRECTPRESENT,
					sharedScope,
					null, 
					null) );
		}
		catch ( BirtException e )
		{
			// impossible get here
			return null;
		}
	}
	    
    /**
     * @deprecated Use newDataEngine(Scriptable) instead. Home Dir is no longer used.
     */
    public static DataEngine newDataEngine( Scriptable sharedScope, File homeDir )
    {
        return newDataEngine( sharedScope );
    }
        
    /**
	 * If and only if current mode is DataEngineContext.MODE_PRESENTATION, query
	 * result can be retrieved from report document. Otherwise a BirtException
	 * will be thrown immediatelly.
	 * 
	 * @param queryResultID
	 * @return an instanceof IQueryResults
	 * @throws BirtException
	 */
	public abstract IQueryResults getQueryResults( String queryResultID )
			throws BirtException;
    
	/**
	 * Provides the definition of a data source to Data Engine. A data source
	 * must be defined using this method prior to preparing any report query
	 * that uses such data source. <br>
	 * Data sources are uniquely identified name. If specified data source has
	 * already been defined, its definition will be updated with the content of
	 * the provided definition object.
	 */
	abstract public void defineDataSource( IBaseDataSourceDesign dataSource ) 
			throws BirtException;

	/**
	 * Provides the definition of a data set to Data Engine. A data set must be
	 * defined using this method prior to preparing any report query that uses such data set.
	 * <br>
	 * Data sets are uniquely identified name. If specified data set has already
	 * been defined, its definition will be updated with the content of the provided definition object.
	 */
	abstract public void defineDataSet( IBaseDataSetDesign dataSet ) 
			throws BirtException;
	
	/**
	 * Verifies the elements of a report query spec
	 * and provides a hint to the query to prepare and optimize 
	 * an execution plan.
	 * The given querySpec could be a <code>IQueryDefinition</code> 
	 * (raw data transform) spec  
	 * based on static definition found in a report design.
	 * <p> 
	 * This report query spec could be further refined 
	 * during engine execution after having resolved any related
	 * runtime condition.
	 * For example, a nested report item might not be rendered based
	 * on a runtime condition.  Thus its associated data expression
	 * could be removed from the report query defn given to 
	 * DtE to prepare.
	 * <p>
	 * During prepare, the DTE does not open a data set. 
	 * In other words, any <code>beforeOpen</code> script on a data set will not be
	 * evaluated at this stage. 
	 * @param	querySpec	Specifies
	 * 				the data access and data transforms services
	 * 				needed from DtE to produce a set of query results.
	 * @return		The <code>IPreparedQuery</code> object that contains a prepared 
	 * 				query ready for execution.
	 * @throws 		DataException if error occurs during the preparation of querySpec
	 */
	abstract public IPreparedQuery prepare( IQueryDefinition querySpec )
			throws BirtException;

	/**
	 * Verifies the elements of a report query spec,
	 * and provides a hint and application context object(s) to the 
	 * query to prepare and optimize an execution plan.
	 * <br>This has the same behavior as the 
	 * prepare( IQueryDefinition querySpec ) method,
	 * with an additional argument for an application to pass in 
	 * a context map to the underlying data provider, 
	 * e.g. an ODA run-time driver.
	 * @param	querySpec	Specifies
	 * 				the data access and data transforms services
	 * 				needed from DtE to produce a set of query results.
	 * @param appContext	The application context map for 
	 * 				preparation and execution of the querySpec; 
	 * 				could be null.
	 * @return		The <code>IPreparedQuery</code> object that contains a prepared 
	 * 				query ready for execution.
	 * @throws 		BirtException if error occurs during the preparation of querySpec
	 * @since		2.0
	 */
	abstract public IPreparedQuery prepare( IQueryDefinition querySpec, 
	        								Map appContext )
			throws BirtException;

	/**
	 * Provides a hint to DtE that the consumer is done with the given 
	 * data source, and 
	 * that its resources can be safely released as appropriate.
	 * This tells DtE that there is to be no more query
	 * that uses such data source.
	 * @param	dataSourceName	The name of a data source. The named data source
	 *          must have been previously defined.
	 */
	abstract public void closeDataSource( String dataSourceName )
			throws BirtException;
	
	/**
	 * Shuts down this instance of data engine, and releases all associated resources.
	 * This method should be called when the caller is done with an instance of the data engine.
	 */
	abstract public void shutdown();
	
}

