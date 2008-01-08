
/*******************************************************************************
 * Copyright (c) 2004, 2007 Actuate Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Actuate Corporation  - initial API and implementation
 *******************************************************************************/
package org.eclipse.birt.data.engine.api;

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.eclipse.birt.core.data.DataType;
import org.eclipse.birt.core.exception.BirtException;
import org.eclipse.birt.data.engine.api.querydefn.Binding;
import org.eclipse.birt.data.engine.api.querydefn.ColumnDefinition;
import org.eclipse.birt.data.engine.api.querydefn.QueryDefinition;
import org.eclipse.birt.data.engine.api.querydefn.ScriptDataSetDesign;
import org.eclipse.birt.data.engine.api.querydefn.ScriptDataSourceDesign;
import org.eclipse.birt.data.engine.api.querydefn.ScriptExpression;
import org.eclipse.birt.data.engine.impl.DataEngineImpl;


/**
 * 
 */

public class DteLevelDataSetCacheTest extends TestCase
{
//	public void testDataSetWithDteLevelCache() throws BirtException
//	{
//		DataEngineContext context = DataEngineContext.newInstance( DataEngineContext.DIRECT_PRESENTATION, 
//				null,null,null );
//		DataEngine dataEngine = DataEngine.newDataEngine( context );
//	
//		ScriptDataSourceDesign dataSource = new ScriptDataSourceDesign( "ds" );
//		dataSource.setOpenScript( "i = 0;" );
//		ScriptDataSetDesign dataSet = new ScriptDataSetDesign( "test" );
//		dataSet.setDataSource( "ds" );
//
//		dataSet.addResultSetHint( new ColumnDefinition( "column1" ) );
//
//		dataSet.setFetchScript( " i++; if ( i % 10 == 0 ) return false; row.column1 = i;" +
//				"return true;" );
//
//		dataEngine.defineDataSource( dataSource );
//		dataEngine.defineDataSet( dataSet );
//		
//		QueryDefinition qd = new QueryDefinition();
//		qd.addBinding( new Binding( "column1",
//				new ScriptExpression( "dataSetRow[\"column1\"]",
//						DataType.INTEGER_TYPE ) ) );
//		qd.setDataSetName( "test" );
//		Map appContextMap = new HashMap( );
//		IResultIterator ri1 = dataEngine.prepare( qd, appContextMap ).execute( null ).getResultIterator( );
//		IResultIterator ri2 = dataEngine.prepare( qd, appContextMap ).execute( null ).getResultIterator( );
//		
//		assertTrue(((DataEngineImpl)dataEngine).getSession( ).getDataSetCacheManager( ).doesLoadFromCache( ) );
//		while ( ri1.next( ) )
//		{
//			assertTrue( ri2.next( ) );
//			assertEquals( ri1.getValue( "column1" ), ri2.getValue( "column1" ) );
//		}
//		
//	}
	
	public void testDataSetWithJVMCache() throws BirtException
	{
		DataEngineContext context = DataEngineContext.newInstance( DataEngineContext.DIRECT_PRESENTATION, 
				null,null,null );
		DataEngine dataEngine = DataEngine.newDataEngine( context );
	
		ScriptDataSourceDesign dataSource = new ScriptDataSourceDesign( "ds" );
		dataSource.setOpenScript( "i = 0;" );
		ScriptDataSetDesign dataSet = new ScriptDataSetDesign( "test" );
		dataSet.setCacheRowCount( 100 );
		dataSet.setDataSource( "ds" );

		dataSet.addResultSetHint( new ColumnDefinition( "column1" ) );

		dataSet.setFetchScript( " i++; if ( i % 10 == 0 ) return false; row.column1 = i;" +
				"return true;" );

		dataEngine.defineDataSource( dataSource );
		dataEngine.defineDataSet( dataSet );
		
		QueryDefinition qd = new QueryDefinition();
		qd.addBinding( new Binding( "column1",
				new ScriptExpression( "dataSetRow[\"column1\"]",
						DataType.INTEGER_TYPE ) ) );
		qd.setDataSetName( "test" );
		Map appContextMap = new HashMap( );
		appContextMap.put(DataEngine.DATA_SET_CACHE_ROW_LIMIT, "100");
		IResultIterator ri1 = dataEngine.prepare( qd, appContextMap ).execute( null ).getResultIterator( );
		IResultIterator ri2 = dataEngine.prepare( qd, appContextMap ).execute( null ).getResultIterator( );
		
		assertTrue(((DataEngineImpl)dataEngine).getSession( ).getDataSetCacheManager( ).doesLoadFromCache( ) );
		while ( ri1.next( ) )
		{
			assertTrue( ri2.next( ) );
			assertEquals( ri1.getValue( "column1" ), ri2.getValue( "column1" ) );
		}
		
	}
}