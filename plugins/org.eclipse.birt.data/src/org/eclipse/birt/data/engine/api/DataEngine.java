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

package org.eclipse.birt.data.engine.api;

import java.io.File;

import org.mozilla.javascript.Scriptable;
import org.eclipse.birt.data.engine.core.DataException;
import org.eclipse.birt.data.engine.impl.DataEngineImpl;

/**
 * BIRT-specific Data Engine API class.
 * <br>
 * The same DataEngine object can be used to prepare and execute
 * multiple ReportQuery definition.
 * <p>
 * Currently only covers services for runtime report generation.
 * <br>
 * In Release 1, the scope of a DataEngine object is one
 * per report generation session.		
 */
abstract public class DataEngine
{

    /**
     * Creates a new instance of DataEngine, using the specified Javascript scope and
     * home directory setting. 
     * @param sharedScope a Javascript scope to be used as the "shared" scope to evaluate
     *    Javascript expressions by the data engine. 
     * @param homeDir The data engine's home directory. The home directory is where the
     *    data engine will look for its configuration resources.
     * 
     */
    public static DataEngine newDataEngine( Scriptable sharedScope, File homeDir )
    {
        return new DataEngineImpl( sharedScope, homeDir );
    }
	
	/**
	 * Provides the definition of a data source to Data Engine. A data source must be
	 * defined using this method prior to preparing any report query that uses such data source.
	 * <br>
	 * Data sources are uniquely identified name. If specified data source has already
	 * been defined, its definition will be updated with the content of the provided DataSourceDesign
	 */
	abstract public void defineDataSource( IBaseDataSourceDesign dataSource ) 
			throws DataException;

	/**
	 * Provides the definition of a data set to Data Engine. A data set must be
	 * defined using this method prior to preparing any report query that uses such data set.
	 * <br>
	 * Data sets are uniquely identified name. If specified data set has already
	 * been defined, its definition will be updated with the content of the provided DataSetDesign
	 */
	abstract public void defineDataSet( IBaseDataSetDesign dataSet ) 
			throws DataException;
	
	/**
	 * Verifies the elements of a report query spec
	 * and provides a hint to the query to prepare and optimize 
	 * an execution plan.
	 * The given querySpec could be a ReportQueryDefn 
	 * (raw data transform) spec generated by the factory 
	 * based on static definition found in a report design.
	 * <p> 
	 * This report query spec could be further refined by FPE 
	 * during engine execution after having resolved any related
	 * runtime condition.  This is probably not in BIRT Release 1.
	 * For example, a nested report item might not be rendered based
	 * on a runtime condition.  Thus its associated data expression
	 * could be removed from the report query defn given to 
	 * DtE to prepare.
	 * <p>
	 * During prepare, the DTE does not open a data set. 
	 * In other words, any before-open script on a data set will not be
	 * evaluated at this stage.  That could mean that certain query 
	 * plan generation must be deferred 
	 * to execution time since necessary result set metadata 
	 * might not be available at Prepare time.
	 * @param	querySpec	An IReportQueryDefn object that specifies
	 * 				the data access and data transforms services
	 * 				needed from DtE to produce a set of query results.
	 * @return		The PreparedQuery object that contains a prepared 
	 * 				ReportQuery ready for execution.
	 * @throws 		DataException if error occurs in Data Engine
	 */
	abstract public IPreparedQuery prepare( IReportQueryDefn querySpec )
			throws DataException;
	
	/**
	 * Provides a hint to DtE that the consumer is done with the given 
	 * data source connection, and 
	 * that its resources can be safely released as appropriate.
	 * This tells DtE that there is no more ReportQuery
	 * on a data set that uses such data source connection.
	 * The data source identified by name, should be one referenced 
	 * in one or more of the previously prepared ReportQuery.  
	 * Otherwise, it would simply return with no-op.
	 * <br>
	 * In BIRT Release 1, this method will likely be called by FPE 
	 * at the end of a report generation.
	 * @param	dataSourceName	The name of a data source connection.
	 */
	abstract public void closeDataSource( String dataSourceName )
			throws DataException;
	
	/**
	 * Shuts down this instance of data engine, and releases all associated resources.
	 * This method should be called when the caller is done with an instance of the data engine.
	 */
	abstract public void shutdown();
	
}

