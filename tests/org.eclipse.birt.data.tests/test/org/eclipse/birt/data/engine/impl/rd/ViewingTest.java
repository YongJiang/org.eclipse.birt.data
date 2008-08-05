/*******************************************************************************
* Copyright (c) 2004 Actuate Corporation.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*  Actuate Corporation  - initial API and implementation
*******************************************************************************/
package org.eclipse.birt.data.engine.impl.rd;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.birt.core.exception.BirtException;
import org.eclipse.birt.data.engine.api.DataEngine;
import org.eclipse.birt.data.engine.api.DataEngineContext;
import org.eclipse.birt.data.engine.api.IBaseExpression;
import org.eclipse.birt.data.engine.api.IBinding;
import org.eclipse.birt.data.engine.api.IQueryResults;
import org.eclipse.birt.data.engine.api.IResultIterator;
import org.eclipse.birt.data.engine.api.ISortDefinition;
import org.eclipse.birt.data.engine.api.querydefn.BaseQueryDefinition;
import org.eclipse.birt.data.engine.api.querydefn.Binding;
import org.eclipse.birt.data.engine.api.querydefn.ConditionalExpression;
import org.eclipse.birt.data.engine.api.querydefn.FilterDefinition;
import org.eclipse.birt.data.engine.api.querydefn.GroupDefinition;
import org.eclipse.birt.data.engine.api.querydefn.GroupInstanceInfo;
import org.eclipse.birt.data.engine.api.querydefn.QueryDefinition;
import org.eclipse.birt.data.engine.api.querydefn.QueryExecutionHints;
import org.eclipse.birt.data.engine.api.querydefn.ScriptExpression;
import org.eclipse.birt.data.engine.api.querydefn.SortDefinition;
import org.eclipse.birt.data.engine.api.querydefn.SubqueryDefinition;
import org.eclipse.birt.data.engine.api.querydefn.SubqueryLocator;
import org.eclipse.birt.data.engine.core.DataException;

import testutil.ConfigText;

/**
* Test case for interactive viewing
*/
public class ViewingTest extends RDTestCase
{
	private String queryResultID;
	private List expectedValue;

	private String[] rowExprName;
	private String[] totalExprName;

	private String[] subRowExprName;

	private IBaseExpression[] rowBeArray;
	private IBinding[] totalBeArray;
	
	private boolean GEN_add_filter;
	private boolean GEN_add_group;
	private boolean GEN_add_secondGroup;
	private boolean GEN_subquery_on_group;
	private boolean GEN_useDetail;
	private boolean GEN_make_empty;
	private boolean PRE_add_filter;
	private boolean PRE_add_sort;
	private boolean PRE_add_group;
	private boolean PRE_change_oldbinding;
	private boolean PRE_printGroupInfo;
	private boolean PRE_printExtraAggr;
	private FilterDefinition GEN_filterDefn;

	/*
	 * @see org.eclipse.birt.data.engine.api.APITestCase#getDataSourceInfo()
	 */
	protected DataSourceInfo getDataSourceInfo( )
	{
		return new DataSourceInfo( ConfigText.getString( "Api.TestData.TableName" ),
				ConfigText.getString( "Api.TestData.TableSQL" ),
				ConfigText.getString( "Api.TestData.TestDataFileName" ) );
	}

	/*
	 * @see org.eclipse.birt.data.engine.api.APITestCase#setUp()
	 */
	public void setUp( ) throws Exception
	{
		super.setUp( );

		expectedValue = new ArrayList( );
		
		this.GEN_add_filter = false;
		this.GEN_add_group = false;		
		this.GEN_add_secondGroup = false;
		this.GEN_subquery_on_group = false;
		this.GEN_useDetail = true;
		this.GEN_make_empty = false;
		this.PRE_add_filter = false;
		this.PRE_add_sort = false;
		this.PRE_change_oldbinding = false;
		this.PRE_add_group = false;
		this.PRE_printGroupInfo = false;
		this.PRE_printExtraAggr = false;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.birt.data.engine.impl.rd.RDTestCase#tearDown()
	 */
	public void tearDown() throws Exception
	{
		if( myPreDataEngine != null )
		{
			myPreDataEngine.shutdown( );
			myPreDataEngine.clearCache( dataSource, dataSet );
			myPreDataEngine = null;
		}
		if( myPreDataEngine2 != null )
		{
			myPreDataEngine2.shutdown( );
			myPreDataEngine2.clearCache( dataSource, dataSet );
			myPreDataEngine2 = null;
		}
		super.tearDown( );
	}
	
	/**
	 * @throws BirtException
	 */
	public void testBasicIV( ) throws Exception
	{
		this.genBasicIV( );
		this.closeArchiveWriter( );

		DataEngineContext deContext2 = newContext( DataEngineContext.MODE_PRESENTATION,
				fileName );
		deContext2.setTmpdir( this.getTempDir( ) );
		myPreDataEngine = DataEngine.newDataEngine( deContext2 );
		
		this.preBasicIV( );
		this.closeArchiveReader( );

		this.checkOutputFile( );
	}

	/**
	 * With filter
	 * @throws BirtException
	 */
	public void testBasicIV2( ) throws Exception
	{
		this.GEN_add_filter = true;
		this.genBasicIV( );
		this.closeArchiveWriter( );

		DataEngineContext deContext2 = newContext( DataEngineContext.MODE_UPDATE,
				fileName, fileName );
		myPreDataEngine = DataEngine.newDataEngine( deContext2 );

		this.PRE_add_sort = true;		
		this.preBasicIV( );
		this.closeArchiveReader( );

		this.checkOutputFile( );
	}

	/**
	 * With group
	 * @throws BirtException
	 */
	public void testBasicIV3( ) throws Exception
	{
		this.GEN_add_group = true;
		this.genBasicIV( );
		this.closeArchiveWriter( );

		DataEngineContext deContext2 = newContext( DataEngineContext.MODE_UPDATE,
				fileName, fileName );
		myPreDataEngine = DataEngine.newDataEngine( deContext2 );

		this.PRE_add_sort = true;
		this.PRE_add_group = true;
		this.preBasicIV( );
		this.closeArchiveReader( );

		this.checkOutputFile( );
	}

	/**
	 * With group and filter
	 * @throws BirtException
	 */
	public void testBasicIV4( ) throws Exception
	{
		this.GEN_add_filter = true;
		this.GEN_add_group = true;
		this.genBasicIV( );
		this.closeArchiveWriter( );

		DataEngineContext deContext2 = newContext( DataEngineContext.MODE_UPDATE,
				fileName, fileName );
		myPreDataEngine = DataEngine.newDataEngine( deContext2 );

		this.PRE_add_filter = true;
		this.PRE_add_sort = true;
		this.PRE_add_group = true;
		this.preBasicIV( );
		this.closeArchiveReader( );

		this.checkOutputFile( );
	}
	
	/**
	 * @throws Exception
	 */
	public void testBasicIV5( ) throws Exception
	{
		this.GEN_add_filter = true;
		this.GEN_add_group = true;
		this.genBasicIV( );
		this.closeArchiveWriter( );

		DataEngineContext deContext2 = newContext( DataEngineContext.MODE_UPDATE,
				fileName, fileName );
		myPreDataEngine = DataEngine.newDataEngine( deContext2 );

		this.PRE_add_filter = true;
		this.PRE_add_sort = true;
		this.PRE_add_group = true;
		this.preBasicIV( );
		this.closeArchiveReader( );

		this.checkOutputFile( );
	}
	
	/**
	 * @throws Exception
	 */
	public void testSourceQueryIV1( ) throws Exception
	{
		this.GEN_add_filter = true;
		this.GEN_add_group = true;
		this.genBasicIV( );
		this.closeArchiveWriter( );

		DataEngineContext deContext2 = newContext( DataEngineContext.MODE_PRESENTATION,
				fileName, fileName );
		myPreDataEngine = DataEngine.newDataEngine( deContext2 );

		QueryDefinition baseQuery = new QueryDefinition( );
		baseQuery.setQueryResultsID( this.queryResultID );
		QueryDefinition query = new QueryDefinition( );
		
		query.setSourceQuery( baseQuery );
		
		ScriptExpression filterExpr = new ScriptExpression( "row.AMOUNT_1>350" );
		query.addFilter( new FilterDefinition( filterExpr ) );
		
		SortDefinition sd = new SortDefinition( );
		sd.setExpression( "row.SALE_NAME_1" );
		sd.setSortDirection( ISortDefinition.SORT_ASC );
		query.addSort( sd );
		_preBasicIV1( query );
		this.closeArchiveReader( );

		this.checkOutputFile( );
	}
	
	public void testSourceQueryIVLikeFilter( ) throws Exception
	{
		this.GEN_add_filter = true;
		this.GEN_add_group = true;
		this.genBasicIV( );
		this.closeArchiveWriter( );

		DataEngineContext deContext2 = newContext( DataEngineContext.MODE_PRESENTATION,
				fileName, fileName );
		myPreDataEngine = DataEngine.newDataEngine( deContext2 );

		QueryDefinition baseQuery = new QueryDefinition( );
		baseQuery.setQueryResultsID( this.queryResultID );
		QueryDefinition query = new QueryDefinition( );
		
		query.setSourceQuery( baseQuery );
		
		ConditionalExpression filterExpr = new ConditionalExpression( "row.CITY_1", ConditionalExpression.OP_LIKE, "\"Beijin%\"" );
		query.addFilter( new FilterDefinition( filterExpr ) );
		
		SortDefinition sd = new SortDefinition( );
		sd.setExpression( "row.SALE_NAME_1" );
		sd.setSortDirection( ISortDefinition.SORT_ASC );
		query.addSort( sd );
		_preBasicIV1( query );
		this.closeArchiveReader( );

		this.checkOutputFile( );
	}
	/**
	 * @throws Exception
	 */
	public void testSourceQueryIV2( ) throws Exception
	{
		this.GEN_add_filter = true;
		this.GEN_add_group = true;
		this.genBasicIV( );
		this.closeArchiveWriter( );

		DataEngineContext deContext2 = newContext( DataEngineContext.MODE_PRESENTATION,
				fileName, fileName );
		myPreDataEngine = DataEngine.newDataEngine( deContext2 );

		QueryDefinition baseQuery = new QueryDefinition( );
		baseQuery.setQueryResultsID( this.queryResultID );
		QueryDefinition query = new QueryDefinition( );
		
		query.setSourceQuery( baseQuery );
		
		ScriptExpression filterExpr = new ScriptExpression( "row.AMOUNT_1 + 251 > 350" );
		query.addFilter( new FilterDefinition( filterExpr ) );
		
		SortDefinition sd = new SortDefinition( );
		sd.setExpression( "row.AMOUNT_1 + 251" );
		sd.setSortDirection( ISortDefinition.SORT_ASC );
		query.addSort( sd );
		_preBasicIV1( query );
		this.closeArchiveReader( );

		this.checkOutputFile( );
	}
	
	/**
	 * @throws Exception
	 */
	public void testSourceQueryIVWithGroup( ) throws Exception
	{
		this.GEN_add_filter = true;
		this.GEN_add_group = false;
		this.genBasicIV( );
		this.closeArchiveWriter( );
		DataEngineContext deContext2 = newContext( DataEngineContext.MODE_PRESENTATION,
				fileName, fileName );
		myPreDataEngine = DataEngine.newDataEngine( deContext2 );

		QueryDefinition baseQuery = new QueryDefinition( );
		baseQuery.setQueryResultsID( this.queryResultID );
		QueryDefinition query = new QueryDefinition( );
		
		query.setSourceQuery( baseQuery );
		
		ScriptExpression filterExpr = new ScriptExpression( "row.AMOUNT_1 + 251 > 350" );
		query.addFilter( new FilterDefinition( filterExpr ) );
		
		SortDefinition sd = new SortDefinition( );
		sd.setExpression( "row.AMOUNT_1 + 251" );
		sd.setSortDirection( ISortDefinition.SORT_ASC );
		query.addSort( sd );
		
		String columnBindingNameGroup = "COUNTRY1";
		IBaseExpression columnBindingExprGroup = new ScriptExpression( "row.COUNTRY_1" );
		GroupDefinition gd = new GroupDefinition( "COUNTRY");
		gd.setKeyColumn( "COUNTRY1" );
		query.addResultSetExpression( columnBindingNameGroup,
				columnBindingExprGroup );
		query.addGroup( gd );
		
		_preBasicIV1( query );
		this.closeArchiveReader( );

		this.checkOutputFile( );
	}
	
	public void testSourceSubQueryIV( ) throws Exception
	{
		this.GEN_add_filter = false;
		this.GEN_add_group = true;
		this.GEN_subquery_on_group = true;
		this._genBasicIVWithSubQuery( );
		this.closeArchiveWriter( );

		DataEngineContext deContext2 = newContext( DataEngineContext.MODE_PRESENTATION,
				fileName, fileName );
		myPreDataEngine = DataEngine.newDataEngine( deContext2 );

		QueryDefinition baseQuery = new QueryDefinition( );
		baseQuery.setQueryResultsID( this.queryResultID );
		SubqueryLocator subqueryLocator = new SubqueryLocator( 5, "IAMTEST", baseQuery );
		
		QueryDefinition query = new QueryDefinition( );
		
		query.setSourceQuery( subqueryLocator );
		
		ScriptExpression filterExpr = new ScriptExpression( "row.sub2>200" );
		query.addFilter( new FilterDefinition( filterExpr ) );
		
		SortDefinition sd = new SortDefinition( );
		sd.setExpression( "row.sub2" );
		sd.setSortDirection( ISortDefinition.SORT_ASC );
		query.addSort( sd );
		_preBasicSubIV1( query );
		this.closeArchiveReader( );

		this.checkOutputFile( );
	}
	
	/**
	 * @param GEN_add_filter
	 * @param GEN_add_group
	 * @param qd
	 * @throws BirtException
	 */
	private void _preBasicSubIV1( QueryDefinition qd ) throws BirtException
	{
		IQueryResults qr = myPreDataEngine.prepare( qd ).execute( null );
		
		IResultIterator ri = qr.getResultIterator( );

		ri.moveTo( 0 );
		String abc = "";
		subRowExprName = new String[3];
		subRowExprName[0] = "sub1";
		subRowExprName[1] = "sub2";
		subRowExprName[2] = "sub3";
		for ( int i = 0; i < subRowExprName.length; i++ )
			abc += subRowExprName[i] + "  ";
		this.testPrintln( abc );
		do
		{
			abc = "";
			for ( int i = 0; i < subRowExprName.length; i++ )
				abc += ri.getValue( subRowExprName[i] ) + "  ";
			this.testPrintln( abc + ri.getRowId( ) );
		} while ( ri.next( ) );

		ri.close( );
		myPreDataEngine.shutdown( );
	}
	
	/**
	 * Test filter by group instance 
	 * @throws BirtException
	 */
	public void testFilterByGroupInstanceIV( ) throws Exception
	{
		this.GEN_add_group = true;
		this.genBasicIV( );
		this.closeArchiveWriter( );

		DataEngineContext deContext2 = newContext( DataEngineContext.MODE_PRESENTATION,
				fileName, fileName );
		myPreDataEngine = DataEngine.newDataEngine( deContext2 );

		this.PRE_add_group = true;
		this.PRE_printGroupInfo = true;
		QueryDefinition query = newPreIVReportQuery( );

		QueryExecutionHints hints = new QueryExecutionHints();
		hints.addTargetGroupInstance( new GroupInstanceInfo( 1, 2) );
		query.setQueryExecutionHints( hints );
		this._preBasicIV( query );
		this.closeArchiveReader( );

		this.checkOutputFile( );
	}
	
	/**
	 * Test filter by group instance 
	 * @throws BirtException
	 */
	public void testFilterByGroupInstanceIV1( ) throws Exception
	{
		this.GEN_add_group = true;
		this.genBasicIV( );
		this.closeArchiveWriter( );

		DataEngineContext deContext2 = newContext( DataEngineContext.MODE_PRESENTATION,
				fileName, fileName );
		myPreDataEngine = DataEngine.newDataEngine( deContext2 );

		this.PRE_add_group = true;
		this.PRE_printGroupInfo = true;
		QueryDefinition query = newPreIVReportQuery( );

		QueryExecutionHints hints = new QueryExecutionHints();
		hints.addTargetGroupInstance( new GroupInstanceInfo( 1, 5) );
		query.setQueryExecutionHints( hints );
		this._preBasicIV( query );
		this.closeArchiveReader( );

		this.checkOutputFile( );
	}
	
	/**
	 * Test filter by group instance 
	 * @throws BirtException
	 */
	public void testFilterByGroupInstanceIV_testAggr1( ) throws Exception
	{
		this.GEN_add_group = true;
		this.GEN_add_secondGroup = true;
		QueryDefinition genquery = this.newGenIVReportQuery( );
		addExtraAggregation( genquery );
		this.genBasicIV( genquery );
		this.closeArchiveWriter( );

		DataEngineContext deContext2 = newContext( DataEngineContext.MODE_UPDATE,
				fileName, fileName );
		myPreDataEngine = DataEngine.newDataEngine( deContext2 );

		this.PRE_add_group = true;
		this.PRE_printGroupInfo = true;
		this.PRE_printExtraAggr = true;
		this.PRE_add_filter = true;
		QueryDefinition query = newPreIVReportQuery( );
		addExtraAggregation( query );
		QueryExecutionHints hints = new QueryExecutionHints();
		hints.addTargetGroupInstance( new GroupInstanceInfo( 1, 5) );
		query.setQueryExecutionHints( hints );
		this._preBasicIV( query );
		this.closeArchiveReader( );

		this.checkOutputFile( );
	}
	/**
	 * Test filter by group instance 
	 * @throws BirtException
	 */
	public void testFilterByGroupInstanceIV2( ) throws Exception
	{
		this.GEN_add_group = true;
		this.GEN_add_secondGroup = true;
		this.genBasicIV( );
		this.closeArchiveWriter( );

		DataEngineContext deContext2 = newContext( DataEngineContext.MODE_PRESENTATION,
				fileName, fileName );
		myPreDataEngine = DataEngine.newDataEngine( deContext2 );

		this.PRE_add_group = true;
		this.PRE_printGroupInfo = true;
		QueryDefinition query = newPreIVReportQuery( );

		QueryExecutionHints hints = new QueryExecutionHints();
		hints.addTargetGroupInstance( new GroupInstanceInfo( 2, 5) );
		query.setQueryExecutionHints( hints );
		this._preBasicIV( query );
		this.closeArchiveReader( );

		this.checkOutputFile( );
	}
	
	/**
	 * Test filter by group instance 
	 * @throws BirtException
	 */
	public void testFilterByGroupInstanceIV_testAggr2( ) throws Exception
	{
		this.GEN_add_group = true;
		this.GEN_add_secondGroup = true;
		QueryDefinition genquery = this.newGenIVReportQuery( );
		addExtraAggregation( genquery );
		this.genBasicIV( genquery );
		this.closeArchiveWriter( );

		DataEngineContext deContext2 = newContext( DataEngineContext.MODE_UPDATE,
				fileName, fileName );
		myPreDataEngine = DataEngine.newDataEngine( deContext2 );

		this.PRE_add_group = true;
		this.PRE_printGroupInfo = true;
		this.PRE_printExtraAggr = true;
		this.PRE_add_sort = true;
		QueryDefinition query = newPreIVReportQuery( );
		addExtraAggregation( query );
		QueryExecutionHints hints = new QueryExecutionHints();
		hints.addTargetGroupInstance( new GroupInstanceInfo( 2, 5) );
		query.setQueryExecutionHints( hints );
		this._preBasicIV( query );
		this.closeArchiveReader( );

		this.checkOutputFile( );
	}
	/**
	 * Test filter by group instance 
	 * @throws BirtException
	 */
	public void testFilterByGroupInstanceIV3( ) throws Exception
	{
		this.GEN_add_group = true;
		this.GEN_add_secondGroup = true;
		this.genBasicIV( );
		this.closeArchiveWriter( );

		DataEngineContext deContext2 = newContext( DataEngineContext.MODE_PRESENTATION,
				fileName, fileName );
		myPreDataEngine = DataEngine.newDataEngine( deContext2 );

		this.PRE_add_group = true;
		this.PRE_printGroupInfo = true;
		QueryDefinition query = newPreIVReportQuery( );

		QueryExecutionHints hints = new QueryExecutionHints();
		hints.addTargetGroupInstance( new GroupInstanceInfo( 2, 5) );
		hints.addTargetGroupInstance( new GroupInstanceInfo( 1, 4) );
		query.setQueryExecutionHints( hints );
		this._preBasicIV( query );
		this.closeArchiveReader( );

		this.checkOutputFile( );
	}
	
	/**
	 * Test filter by group instance 
	 * @throws BirtException
	 */
	public void testFilterByGroupInstanceIV_testAggr3( ) throws Exception
	{
		this.GEN_add_group = true;
		this.GEN_add_secondGroup = true;
		QueryDefinition genquery = this.newGenIVReportQuery( );
		addExtraAggregation( genquery );
		this.genBasicIV( genquery );
		this.closeArchiveWriter( );

		DataEngineContext deContext2 = newContext( DataEngineContext.MODE_UPDATE,
				fileName, fileName );
		myPreDataEngine = DataEngine.newDataEngine( deContext2 );

		this.PRE_add_group = true;
		this.PRE_printGroupInfo = true;
		this.PRE_printExtraAggr = true;
		this.PRE_add_sort = true;
		QueryDefinition query = newPreIVReportQuery( );
		addExtraAggregation( query );
		QueryExecutionHints hints = new QueryExecutionHints();
		hints.addTargetGroupInstance( new GroupInstanceInfo( 2, 5) );
		hints.addTargetGroupInstance( new GroupInstanceInfo( 1, 4) );
		query.setQueryExecutionHints( hints );
		this._preBasicIV( query );
		this.closeArchiveReader( );

		this.checkOutputFile( );
	}
	/**
	 * Test filter by group instance 
	 * @throws BirtException
	 */
	public void testFilterByGroupInstanceIV4( ) throws Exception
	{
		this.GEN_add_group = true;
		this.GEN_add_secondGroup = true;
		this.genBasicIV( );
		this.closeArchiveWriter( );

		DataEngineContext deContext2 = newContext( DataEngineContext.MODE_PRESENTATION,
				fileName, fileName );
		myPreDataEngine = DataEngine.newDataEngine( deContext2 );

		this.PRE_add_group = true;
		this.PRE_printGroupInfo = true;
		QueryDefinition query = newPreIVReportQuery( );

		QueryExecutionHints hints = new QueryExecutionHints();
		hints.addTargetGroupInstance( new GroupInstanceInfo( 2, 5) );
		hints.addTargetGroupInstance( new GroupInstanceInfo( 1, 2) );
		query.setQueryExecutionHints( hints );
		this._preBasicIV( query );
		this.closeArchiveReader( );

		this.checkOutputFile( );
	}
	
	public void testFilterByGroupInstanceIV_testAggr4( ) throws Exception
	{
		this.GEN_add_group = true;
		this.GEN_add_secondGroup = true;
		QueryDefinition genquery = this.newGenIVReportQuery( );
		addExtraAggregation( genquery );
		this.genBasicIV( genquery );
		this.closeArchiveWriter( );

		DataEngineContext deContext2 = newContext( DataEngineContext.MODE_UPDATE,
				fileName, fileName );
		myPreDataEngine = DataEngine.newDataEngine( deContext2 );

		this.PRE_add_group = true;
		this.PRE_printGroupInfo = true;
		this.PRE_printExtraAggr = true;
		this.PRE_add_sort = true;
		this.PRE_add_filter = true;
		QueryDefinition query = newPreIVReportQuery( );
		addExtraAggregation( query );
		QueryExecutionHints hints = new QueryExecutionHints();
		hints.addTargetGroupInstance( new GroupInstanceInfo( 2, 5) );
		hints.addTargetGroupInstance( new GroupInstanceInfo( 1, 2) );
		query.setQueryExecutionHints( hints );
		this._preBasicIV( query );
		this.closeArchiveReader( );

		this.checkOutputFile( );
	}
	/**
	 * Test filter by group instance 
	 * @throws BirtException
	 */
	public void testFilterByGroupInstanceIV5( ) throws Exception
	{
		this.GEN_add_group = true;
		this.GEN_add_secondGroup = true;
		this.genBasicIV( );
		this.closeArchiveWriter( );

		DataEngineContext deContext2 = newContext( DataEngineContext.MODE_PRESENTATION,
				fileName, fileName );
		myPreDataEngine = DataEngine.newDataEngine( deContext2 );

		this.PRE_add_group = true;
		this.PRE_printGroupInfo = true;
		QueryDefinition query = newPreIVReportQuery( );

		QueryExecutionHints hints = new QueryExecutionHints();
		hints.addTargetGroupInstance( new GroupInstanceInfo( 2, 5) );
		hints.addTargetGroupInstance( new GroupInstanceInfo( 2, 1) );
		query.setQueryExecutionHints( hints );
		this._preBasicIV( query );
		this.closeArchiveReader( );

		this.checkOutputFile( );
	}
	
	public void testFilterByGroupInstanceIV_testAggr5( ) throws Exception
	{
		this.GEN_add_group = true;
		this.GEN_add_secondGroup = true;
		QueryDefinition genquery = this.newGenIVReportQuery( );
		addExtraAggregation( genquery );
		this.genBasicIV( genquery );
		this.closeArchiveWriter( );

		DataEngineContext deContext2 = newContext( DataEngineContext.MODE_UPDATE,
				fileName, fileName );
		myPreDataEngine = DataEngine.newDataEngine( deContext2 );

		this.PRE_add_group = true;
		this.PRE_printGroupInfo = true;
		this.PRE_printExtraAggr = true;

		QueryDefinition query = newPreIVReportQuery( );
		addExtraAggregation( query );
		QueryExecutionHints hints = new QueryExecutionHints();
		hints.addTargetGroupInstance( new GroupInstanceInfo( 2, 5) );
		hints.addTargetGroupInstance( new GroupInstanceInfo( 2, 1) );
		query.setQueryExecutionHints( hints );
		this._preBasicIV( query );
		this.closeArchiveReader( );

		this.checkOutputFile( );
	}
	
	/**
	 * Test filter by group instance 
	 * @throws BirtException
	 */
	public void testFilterByGroupInstanceIV6( ) throws Exception
	{
		this.GEN_add_group = true;
		this.GEN_useDetail = false;
		this.genBasicIV( );
		this.closeArchiveWriter( );

		DataEngineContext deContext2 = newContext( DataEngineContext.MODE_PRESENTATION,
				fileName, fileName );
		myPreDataEngine = DataEngine.newDataEngine( deContext2 );

		this.PRE_add_group = true;
		this.PRE_printGroupInfo = true;
		QueryDefinition query = newPreIVReportQuery( );
		query.setUsesDetails(  false  );
		QueryExecutionHints hints = new QueryExecutionHints();
		hints.addTargetGroupInstance( new GroupInstanceInfo( 1, 1) );
		query.setQueryExecutionHints( hints );
		this._preBasicIV( query );
		this.closeArchiveReader( );

		this.checkOutputFile( );
	}
	
	public void testFilterByGroupInstanceIV_testAggr6( ) throws Exception
	{
		this.GEN_add_group = true;
		this.GEN_add_secondGroup = true;
		this.GEN_useDetail = false;
		QueryDefinition genquery = this.newGenIVReportQuery( );
		addExtraAggregation( genquery );
		this.genBasicIV( genquery );
		this.closeArchiveWriter( );

		DataEngineContext deContext2 = newContext( DataEngineContext.MODE_UPDATE,
				fileName, fileName );
		myPreDataEngine = DataEngine.newDataEngine( deContext2 );

		this.PRE_add_group = true;
		this.PRE_printGroupInfo = true;
		this.PRE_printExtraAggr = true;

		QueryDefinition query = newPreIVReportQuery( );
		addExtraAggregation( query );
		QueryExecutionHints hints = new QueryExecutionHints();
		hints.addTargetGroupInstance( new GroupInstanceInfo( 1, 1) );
		query.setQueryExecutionHints( hints );
		this._preBasicIV( query );
		this.closeArchiveReader( );

		this.checkOutputFile( );
	}
	/**
	 * Test filter by group instance 
	 * @throws BirtException
	 */
	public void testFilterByGroupInstanceIV7( ) throws Exception
	{
		this.GEN_add_group = true;
		this.GEN_add_secondGroup = true;
		this.GEN_useDetail = false;
		this.genBasicIV( );
		this.closeArchiveWriter( );

		DataEngineContext deContext2 = newContext( DataEngineContext.MODE_PRESENTATION,
				fileName, fileName );
		myPreDataEngine = DataEngine.newDataEngine( deContext2 );

		this.PRE_add_group = true;
		this.PRE_printGroupInfo = true;
		QueryDefinition query = newPreIVReportQuery( );
		query.setUsesDetails(  false  );
		QueryExecutionHints hints = new QueryExecutionHints();
		hints.addTargetGroupInstance( new GroupInstanceInfo( 1, 1) );
		hints.addTargetGroupInstance( new GroupInstanceInfo( 2, 4) );
		query.setQueryExecutionHints( hints );
		this._preBasicIV( query );
		this.closeArchiveReader( );

		this.checkOutputFile( );
	}
	
	
	public void testFilterByGroupInstanceIV_testAggr7( ) throws Exception
	{
		this.GEN_add_group = true;
		this.GEN_add_secondGroup = true;
		this.GEN_useDetail = false;
		QueryDefinition genquery = this.newGenIVReportQuery( );
		addExtraAggregation( genquery );
		this.genBasicIV( genquery );
		this.closeArchiveWriter( );

		DataEngineContext deContext2 = newContext( DataEngineContext.MODE_UPDATE,
				fileName, fileName );
		myPreDataEngine = DataEngine.newDataEngine( deContext2 );

		this.PRE_add_group = true;
		this.PRE_printGroupInfo = true;
		this.PRE_printExtraAggr = true;

		QueryDefinition query = newPreIVReportQuery( );
		addExtraAggregation( query );
		QueryExecutionHints hints = new QueryExecutionHints();
		hints.addTargetGroupInstance( new GroupInstanceInfo( 1, 1) );
		hints.addTargetGroupInstance( new GroupInstanceInfo( 2, 4) );
		query.setQueryExecutionHints( hints );
		this._preBasicIV( query );
		this.closeArchiveReader( );

		this.checkOutputFile( );
	}
	/**
	 * Test filter by group instance 
	 * @throws BirtException
	 */
	public void testFilterByGroupInstanceIV8( ) throws Exception
	{
		this.GEN_add_group = true;
		this.GEN_useDetail = false;
		this.GEN_make_empty = true;
		this.genBasicIV( );
		this.closeArchiveWriter( );

		DataEngineContext deContext2 = newContext( DataEngineContext.MODE_PRESENTATION,
				fileName, fileName );
		myPreDataEngine = DataEngine.newDataEngine( deContext2 );

		this.PRE_add_group = true;
		this.PRE_printGroupInfo = true;
		QueryDefinition query = newPreIVReportQuery( );
		query.setUsesDetails(  false  );
		QueryExecutionHints hints = new QueryExecutionHints();
		hints.addTargetGroupInstance( new GroupInstanceInfo( 1, 1) );
		query.setQueryExecutionHints( hints );
		this._preBasicIV( query );
		this.closeArchiveReader( );
	}
	
	/**
	 * Test filter by group instance 
	 * @throws BirtException
	 */
	public void testFilterByGroupInstanceIV9( ) throws Exception
	{
		this.GEN_add_group = true;
		this.GEN_useDetail = true;

		this.genBasicIV( );
		this.closeArchiveWriter( );

		DataEngineContext deContext2 = newContext( DataEngineContext.MODE_PRESENTATION,
				fileName, fileName );
		myPreDataEngine = DataEngine.newDataEngine( deContext2 );

		this.PRE_add_group = true;
		this.PRE_printGroupInfo = true;
		QueryDefinition query = newPreIVReportQuery( );
		query.setUsesDetails(  false  );
		QueryExecutionHints hints = new QueryExecutionHints();
		hints.addTargetGroupInstance( new GroupInstanceInfo( 1, 10) );
		query.setQueryExecutionHints( hints );
		this._preBasicIV( query );
		this.closeArchiveReader( );
	}
	
	public void testFilterByGroupInstanceIV_testAggr( ) throws Exception
	{
		this.GEN_add_group = true;
		this.GEN_add_secondGroup = true;
		QueryDefinition genquery = this.newGenIVReportQuery( );
		addExtraAggregation( genquery );
		this.genBasicIV( genquery );
		this.closeArchiveWriter( );

		DataEngineContext deContext2 = newContext( DataEngineContext.MODE_UPDATE,
				fileName, fileName );
		myPreDataEngine = DataEngine.newDataEngine( deContext2 );

		this.PRE_add_group = true;
		this.PRE_printGroupInfo = true;
		this.PRE_printExtraAggr = true;
		this.PRE_add_sort = true;
		QueryDefinition query = newPreIVReportQuery( );
		addExtraAggregation( query );
		QueryExecutionHints hints = new QueryExecutionHints();
		hints.addTargetGroupInstance( new GroupInstanceInfo( 2, 5) );
		query.setQueryExecutionHints( hints );
		this._preBasicIV( query );
		this.closeArchiveReader( );

		this.checkOutputFile( );
	}

	/**
	 * Test filter by group instance 
	 * @throws BirtException
	 */
	public void testFilterByGroupInstanceIV10( ) throws Exception
	{
		this.GEN_add_group = true;
		this.GEN_add_secondGroup = true;
		this.genBasicIV( );
		this.closeArchiveWriter( );

		DataEngineContext deContext2 = newContext( DataEngineContext.MODE_PRESENTATION,
				fileName, fileName );
		myPreDataEngine = DataEngine.newDataEngine( deContext2 );

		this.PRE_add_group = true;
		this.PRE_printGroupInfo = true;
		QueryDefinition query = newPreIVReportQuery( );

		QueryExecutionHints hints = new QueryExecutionHints();
		hints.addTargetGroupInstance( new GroupInstanceInfo( 2, 5) );
		hints.addTargetGroupInstance( new GroupInstanceInfo( 2, 3) );
		query.setQueryExecutionHints( hints );
		this._preBasicIV( query );
		this.closeArchiveReader( );

		this.checkOutputFile( );
	}
	private void addExtraAggregation( QueryDefinition genquery )
			throws DataException
	{
		IBinding binding1 = new Binding( "Count_on_1st_group");
		binding1.setAggrFunction( "COUNT" );
		binding1.addAggregateOn( "COUNTRY" );
		IBinding binding2 = new Binding( "Count_on_2nd_group");
		binding2.setAggrFunction( "COUNT" );
		binding2.addAggregateOn( "CITY" );
		IBinding binding3 = new Binding( "Count_on_2nd_group_1");
		binding3.setExpression( new ScriptExpression( "row.Count_on_2nd_group + 1") );
		IBinding binding4 = new Binding( "Count_on_2nd_group_1_2");
		binding4.setExpression( new ScriptExpression( "row.Count_on_2nd_group_1+2") );
		IBinding binding5 = new Binding( "Count_on_1st_group_10");
		binding5.setExpression( new ScriptExpression( "row.Count_on_1st_group + 10") );
		
		genquery.addBinding( binding1 );
		genquery.addBinding(  binding2 );
		genquery.addBinding( binding3);
		genquery.addBinding(  binding4 );
		genquery.addBinding( binding5);
		
	}
	/**
	 * @throws Exception
	 */
	public void testBasicIV6() throws Exception {
		this.GEN_add_filter = false;
		this.GEN_add_group = false;
		this.genBasicIV();
		this.closeArchiveWriter();

		DataEngineContext deContext2 = newContext( DataEngineContext.MODE_UPDATE,
				fileName, fileName );
		myPreDataEngine = DataEngine.newDataEngine(deContext2);

		this.PRE_add_filter = false;
		this.PRE_add_sort = false;
		this.PRE_add_group = false;
		this.preBasicIV();

		DataEngineContext deContext3 = newContext( DataEngineContext.MODE_UPDATE,
				fileName, fileName );
		myPreDataEngine = DataEngine.newDataEngine(deContext3);
		this.PRE_change_oldbinding = true;
		this.preBasicIV();
		this.closeArchiveReader();

		this.checkOutputFile();

	}

	private void genBasicIV( QueryDefinition qd ) throws BirtException
	{
		// prepare
		IBaseExpression[] rowBeArray = getRowExpr( );
		IBinding[] totalBeArray = getAggrExpr( );
		prepareExprNameAndQuery( rowBeArray, totalBeArray, qd );

		// generation
		IQueryResults qr = myGenDataEngine.prepare( qd ).execute( scope );

		// important step
		queryResultID = qr.getID( );

		IResultIterator ri = qr.getResultIterator( );
		while ( ri.next( ) )
		{
			for ( int i = 0; i < rowBeArray.length; i++ )
				expectedValue.add( ri.getValue( this.rowExprName[i] ) );

			for ( int i = 0; i < totalBeArray.length; i++ )
				expectedValue.add( ri.getValue( this.totalExprName[i] ) );
		}

		ri.close( );
		qr.close( );
		myGenDataEngine.shutdown( );
	}
	/**
	 * @throws BirtException
	 */
	private void genBasicIV( )
			throws BirtException
	{
		QueryDefinition qd = newGenIVReportQuery( );
		genBasicIV( qd );
		
	}
	
	/**
	 * @return
	 */
	private QueryDefinition newGenIVReportQuery( )
	{
		QueryDefinition qd = newReportQuery( );

		if ( GEN_add_filter == true )
		{
			// do filtering on column 4
			String columnBindingNameFilter = "AMOUNT_1";
			IBaseExpression columnBindingExprFilter = new ScriptExpression( "dataSetRow.AMOUNT" );
			ScriptExpression filterExpr = new ScriptExpression( "row.AMOUNT_1>50" );
			GEN_filterDefn = new FilterDefinition( filterExpr );
			qd.addResultSetExpression( columnBindingNameFilter,
					columnBindingExprFilter );
			qd.addFilter( GEN_filterDefn );
		}

		if ( GEN_make_empty == true )
		{
			// do filtering on column 4
			String columnBindingNameFilter = "AMOUNT_1";
			IBaseExpression columnBindingExprFilter = new ScriptExpression( "dataSetRow.AMOUNT" );
			ScriptExpression filterExpr = new ScriptExpression( "row.AMOUNT_1<0" );

			qd.addResultSetExpression( columnBindingNameFilter,
					columnBindingExprFilter );
			qd.addFilter( new FilterDefinition( filterExpr ) );
		}
		// do sorting on column 2
		String columnBindingNameSort = "CITY_1";
		IBaseExpression columnBindingExprSort = new ScriptExpression( "dataSetRow.CITY" );
		SortDefinition sortDefn = new SortDefinition( );
		sortDefn.setColumn( columnBindingNameSort );
		qd.addResultSetExpression( columnBindingNameSort, columnBindingExprSort );
		qd.addSort( sortDefn );

		if ( GEN_add_group == true )
		{
			// add grouping on column1
			String columnBindingNameGroup = "COUNTRY2";
			IBaseExpression columnBindingExprGroup = new ScriptExpression( "dataSetRow.COUNTRY" );
			GroupDefinition gd = new GroupDefinition( "COUNTRY");
			gd.setKeyColumn( "COUNTRY2" );
			qd.addResultSetExpression( columnBindingNameGroup,
					columnBindingExprGroup );
			qd.addGroup( gd );
		}

		if ( GEN_add_secondGroup == true )
		{
			// add grouping on column1
			String columnBindingNameGroup = "CITY2";
			IBaseExpression columnBindingExprGroup = new ScriptExpression( "dataSetRow.CITY" );
			GroupDefinition gd = new GroupDefinition( "CITY");
			gd.setKeyColumn( "CITY2" );
			qd.addResultSetExpression( columnBindingNameGroup,
					columnBindingExprGroup );
			qd.addGroup( gd );
		}
		
		qd.setUsesDetails( this.GEN_useDetail );
		return qd;
	}
	
	/**
	 * @throws BirtException
	 */
	private void preBasicIV( )
			throws BirtException
	{
		// here queryResultID needs to set as the data set
		QueryDefinition qd = newPreIVReportQuery( );
		_preBasicIV( qd );
	}
	
	/**
	 * @param PRE_add_filter
	 * @param PRE_add_sort
	 * @param PRE_use_oldbinding
	 * @return
	 * @throws DataException 
	 */
	private QueryDefinition newPreIVReportQuery( ) throws DataException
	{
		QueryDefinition qd = new QueryDefinition( );

		if ( GEN_add_filter == true )
		{
			qd.addFilter( GEN_filterDefn );
		}
		
		if ( PRE_add_filter == true )
		{
			// do filtering on column 4
			ScriptExpression filterExpr = new ScriptExpression( "row.AMOUNT_1>200" );
			FilterDefinition fd = new FilterDefinition( filterExpr );
			qd.addFilter( fd );
		}

		if ( PRE_add_sort )
		{
			// do sorting on column 4
			SortDefinition sd = new SortDefinition( );
			sd.setExpression( "row.AMOUNT_1" );
			sd.setSortDirection( ISortDefinition.SORT_ASC );
			qd.addSort( sd );
		}

		if ( PRE_add_group == true )
		{
			// add grouping on column1
			String columnBindingNameGroup = "COUNTRY2";
			IBaseExpression columnBindingExprGroup = new ScriptExpression( "dataSetRow.COUNTRY" );
			GroupDefinition gd = new GroupDefinition( "COUNTRY");
			gd.setKeyColumn( "COUNTRY2" );
			qd.addResultSetExpression( columnBindingNameGroup,
					columnBindingExprGroup );
			qd.addGroup( gd );
		}
		
		if ( GEN_add_secondGroup == true )
		{
			// add grouping on column1
			String columnBindingNameGroup = "CITY2";
			IBaseExpression columnBindingExprGroup = new ScriptExpression( "dataSetRow.CITY" );
			GroupDefinition gd = new GroupDefinition( "CITY");
			gd.setKeyColumn( "CITY2" );
			qd.addResultSetExpression( columnBindingNameGroup,
					columnBindingExprGroup );
			qd.addGroup( gd );
		}
		for ( int i = 0; i < rowExprName.length; i++ )
		{
			if ( PRE_change_oldbinding && i == rowExprName.length - 1 )
			{
				qd.addResultSetExpression( this.rowExprName[i],
						new ScriptExpression( "dataSetRow.AMOUNT+100" ) );
			}
			else
			{
				qd.addResultSetExpression( this.rowExprName[i],
						this.rowBeArray[i] );
			}
		}

		for ( int i = 0; i < totalExprName.length; i++ )
		{
			qd.addBinding( //binding )( this.totalExprName[i],
					this.totalBeArray[i] );
		}
		return qd;
	}
	
	/**
	 * @param GEN_add_filter
	 * @param GEN_add_group
	 * @param qd
	 * @throws BirtException
	 */
	private void _preBasicIV( QueryDefinition qd ) throws BirtException
	{
		qd.setQueryResultsID( this.queryResultID );

		IQueryResults qr = myPreDataEngine.prepare( qd ).execute( null );
		IResultIterator ri = qr.getResultIterator( );

		ri.moveTo( 0 );
		if ( !ri.isEmpty( ) )
		{
			do
			{
				String abc = "";
		
				for ( int i = 0; i < rowExprName.length; i++ )
					abc += ri.getValue( rowExprName[i] ) + "  ";
				for ( int i = 0; i < totalExprName.length; i++ )
					abc += ri.getValue( totalExprName[i] ) + "  ";
				if( this.PRE_printExtraAggr )
					abc += ri.getValue( "Count_on_1st_group" ) + "  " + ri.getValue( "Count_on_2nd_group" ) + "  "
					      +ri.getValue( "Count_on_2nd_group_1" ) + "  " + ri.getValue( "Count_on_2nd_group_1_2" ) + "  "
					      + ri.getValue( "Count_on_1st_group_10" ) + "  ";
				if ( this.PRE_printGroupInfo )
					this.testPrintln( abc
							+ ri.getRowId( ) + " " + ri.getRowIndex( ) + " "
							+ ri.getStartingGroupLevel( ) + " "
							+ ri.getEndingGroupLevel( ) );
				else
					this.testPrintln( abc + ri.getRowId( ) );
			} while ( ri.next( ) );
		}
		ri.close( );
		myPreDataEngine.shutdown( );
	}
	
	/**
	 * @param GEN_add_filter
	 * @param GEN_add_group
	 * @param qd
	 * @throws BirtException
	 */
	private void _preBasicIV1( QueryDefinition qd ) throws BirtException
	{
		IQueryResults qr = myPreDataEngine.prepare( qd ).execute( null );
		
		IResultIterator ri = qr.getResultIterator( );

		ri.moveTo( 0 );
		String abc = "";
		for ( int i = 0; i < rowExprName.length; i++ )
			abc += rowExprName[i] + "  ";
		for ( int i = 0; i < totalExprName.length; i++ )
			abc += totalExprName[i] + "  ";
		if( qd.getGroups( ).size( ) > 0 )
		{
			abc += "starting level  " + "ending level  ";
		}
		this.testPrintln( abc );
		do
		{
			abc = "";
			
			for ( int i = 0; i < rowExprName.length; i++ )
				abc += ri.getValue( rowExprName[i] ) + "  ";
			for ( int i = 0; i < totalExprName.length; i++ )
				abc += ri.getValue( totalExprName[i] ) + "  ";
			abc += ri.getRowId( );
			if( qd.getGroups( ).size( ) > 0 )
			{
				abc += "  " + ri.getStartingGroupLevel( ) + "  " + ri.getEndingGroupLevel( );
			}
			this.testPrintln( abc );
		} while ( ri.next( ) );

		ri.close( );
		myPreDataEngine.shutdown( );
	}
	
	/**
	 * With group and filter
	 * @throws BirtException
	 */
	public void atestBasicIVSubQuery1( ) throws Exception
	{
		this.GEN_add_filter = true;
		this.GEN_add_group = true;
		this.genBasicIV( );
		this.closeArchiveWriter( );

		DataEngineContext deContext2 = newContext( 
				DataEngineContext.MODE_PRESENTATION,
				fileName );
		deContext2.setTmpdir( this.getTempDir( ) );
		myPreDataEngine = DataEngine.newDataEngine( deContext2 );

		this.PRE_add_filter = true;
		this.PRE_add_sort = true;
		this._preBasicIVWithSubQuery( );
		this.closeArchiveReader( );
	}

	/**
	 * With group and filter
	 * @throws BirtException
	 */
	public void testBasicIVSubQuery( ) throws Exception
	{
		this.GEN_add_filter = false;
		this.GEN_add_group = true;
		this.GEN_subquery_on_group = true;
		this._genBasicIVWithSubQuery( );
		this.closeArchiveWriter( );

		DataEngineContext deContext2 = newContext( DataEngineContext.MODE_UPDATE,
				fileName, fileName );
		myPreDataEngine = DataEngine.newDataEngine( deContext2 );

		this.PRE_add_filter = true;
		this.PRE_add_sort = true;
		this.PRE_add_group = true;
		this._preBasicIVWithSubQuery( );
		this.closeArchiveReader( );
		
		this.checkOutputFile( );
	}

	/**
	 * @throws BirtException
	 */
	private void _genBasicIVWithSubQuery( ) throws BirtException
	{
		QueryDefinition qd = newGenIVReportQuery( );

		// prepare
		IBaseExpression[] rowBeArray = getRowExpr( );
		IBinding[] totalBeArray = getAggrExpr( );
		prepareExprNameAndQuery( rowBeArray, totalBeArray, qd );

		//		 ---------- begin sub query ----------
		SubqueryDefinition subqueryDefn = new SubqueryDefinition( "IAMTEST", qd );
		if( GEN_subquery_on_group )
		{
			((GroupDefinition)qd.getGroups().get(0)).addSubquery( subqueryDefn );
			subqueryDefn.setApplyOnGroupFlag(true);
		}
		else
		{
			qd.addSubquery( subqueryDefn );
			subqueryDefn.setApplyOnGroupFlag( false );
		}
		subRowExprName = new String[3];
		subRowExprName[0] = "sub1";
		subRowExprName[1] = "sub2";
		subRowExprName[2] = "sub3";
		ScriptExpression[] exprs = new ScriptExpression[3];
		exprs[0] = new ScriptExpression( "row.__rownum");
		exprs[1] = new ScriptExpression( "dataSetRow[\"AMOUNT\"]");
		exprs[2] = new ScriptExpression( "dataSetRow[\"CITY\"]");

		for( int i = 0; i < subRowExprName.length; i++ )
			subqueryDefn.addResultSetExpression(subRowExprName[i], exprs[i]);
		

		// generation
		IQueryResults qr = myGenDataEngine.prepare( qd ).execute( scope );

		// important step
		queryResultID = qr.getID( );

		IResultIterator ri = qr.getResultIterator( );
		while ( ri.next( ) )
		{
			for ( int i = 0; i < rowBeArray.length; i++ )
				expectedValue.add( ri.getValue( this.rowExprName[i] ) );

			for ( int i = 0; i < totalBeArray.length; i++ )
				expectedValue.add( ri.getValue( this.totalExprName[i] ) );

			IResultIterator subRi = ri.getSecondaryIterator("IAMTEST", scope);
			
			while (subRi.next())
			{
				for (int i = 0; i < subRowExprName.length; i++) 
				{
					subRi.getValue(subRowExprName[i]);
				}
			}
			subRi.close();
		}


		ri.close( );
		qr.close( );
		myGenDataEngine.shutdown( );
	}
	
	/**
	 * @throws BirtException
	 */
	private void _preBasicIVWithSubQuery( )
			throws BirtException
	{
		QueryDefinition qd = newPreIVReportQuery( );
		qd.setQueryResultsID( this.queryResultID );
		
		SubqueryDefinition subqueryDefn = new SubqueryDefinition( "IAMTEST", qd );
		((GroupDefinition)qd.getGroups( ).get(0)).addSubquery( subqueryDefn );
		subRowExprName = new String[3];
		subRowExprName[0] = "sub1";
		subRowExprName[1] = "sub2";
		subRowExprName[2] = "sub3";
		ScriptExpression[] exprs = new ScriptExpression[3];
		exprs[0] = new ScriptExpression( "row.__rownum");
		exprs[1] = new ScriptExpression( "dataSetRow.AMOUNT");
		exprs[2] = new ScriptExpression( "dataSetRow.CITY");

		for( int i = 0; i < subRowExprName.length; i++ )
			subqueryDefn.addResultSetExpression(subRowExprName[i], exprs[i]);
		subqueryDefn.setApplyOnGroupFlag(true);

		IQueryResults qr = myPreDataEngine.prepare( qd ).execute( null );
		IResultIterator ri = qr.getResultIterator( );

		ri.moveTo( 0 );
		do
		{
			String abc = "";
			for ( int i = 0; i < rowExprName.length; i++ )
				abc += ri.getValue( rowExprName[i] ) + "  ";
			for ( int i = 0; i < totalExprName.length; i++ )
				abc += ri.getValue( totalExprName[i] ) + "  ";
			
			this.testPrintln( abc + ri.getRowId( ) );
			
			IResultIterator subRi = ri.getSecondaryIterator("IAMTEST", scope);
			while( subRi.next() )
			{
				abc = "          [" + subRi.getValue("sub1") + "]" + "["
						+ subRi.getValue("sub2") + "]" + "["
						+ subRi.getValue("sub2") + "]" + "  " + subRi.getRowId( );
				this.testPrintln( abc );
			}
		} while ( ri.next( ) );

		ri.close( );
		myPreDataEngine.shutdown( );
	}
	
	/**
	 * @return row expression array
	 */
	private IBaseExpression[] getRowExpr( )
	{
		// row test
		int num = 4;
		rowBeArray = new IBaseExpression[num];
		rowBeArray[0] = new ScriptExpression( "dataSetRow.COUNTRY" );
		rowBeArray[1] = new ScriptExpression( "dataSetRow.CITY" );
		rowBeArray[2] = new ScriptExpression( "dataSetRow.SALE_DATE" );
		rowBeArray[3] = new ScriptExpression( "dataSetRow.AMOUNT" );

		this.rowExprName = new String[rowBeArray.length];
		this.rowExprName[0] = "COUNTRY_1";
		this.rowExprName[1] = "CITY_1";
		this.rowExprName[2] = "SALE_NAME_1";
		this.rowExprName[3] = "AMOUNT_1";

		return rowBeArray;
	}

	/**
	 * @return aggregation expression array
	 * @throws DataException 
	 */
	private IBinding[] getAggrExpr( ) throws DataException
	{
		int num2 = 2;
		totalExprName = new String[num2];
		this.totalExprName[0] = "TOTAL_COUNT_1";
		this.totalExprName[1] = "TOTAL_AMOUNT_1";
		
		totalBeArray = new IBinding[num2];
		totalBeArray[0] = new Binding( this.totalExprName[0]);
		totalBeArray[0].setAggrFunction( "COUNT" );
		
		totalBeArray[1] = new Binding( this.totalExprName[1], new ScriptExpression( "dataSetRow.AMOUNT" ));
		totalBeArray[1].setAggrFunction( "SUM" );

	

		return totalBeArray;
	}

	/**
	 * Add expression on the row of group
	 * @param rowBeArray
	 * @param totalBeArray
	 * @param qd
	 * @throws DataException 
	 */
	private void prepareExprNameAndQuery( IBaseExpression[] rowBeArray,
			IBinding[] totalBeArray, BaseQueryDefinition qd ) throws DataException
	{
		int num = rowBeArray.length;
		int num2 = totalBeArray.length;

		for ( int i = 0; i < num; i++ )
			qd.addResultSetExpression( this.rowExprName[i], rowBeArray[i] );

		for ( int i = 0; i < num2; i++ )
			qd.addBinding( totalBeArray[i] );
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.birt.data.engine.impl.rd.RDTestCase#useFolderArchive()
	 */
	protected boolean useFolderArchive()
	{
		return true;
	}
}

