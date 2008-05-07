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

package org.eclipse.birt.data.engine.binding;

import java.util.ArrayList;

import org.eclipse.birt.core.data.DataType;
import org.eclipse.birt.data.engine.api.IBaseExpression;
import org.eclipse.birt.data.engine.api.IConditionalExpression;
import org.eclipse.birt.data.engine.api.IGroupDefinition;
import org.eclipse.birt.data.engine.api.IQueryDefinition;
import org.eclipse.birt.data.engine.api.IResultIterator;
import org.eclipse.birt.data.engine.api.ISortDefinition;
import org.eclipse.birt.data.engine.api.querydefn.BaseDataSetDesign;
import org.eclipse.birt.data.engine.api.querydefn.ComputedColumn;
import org.eclipse.birt.data.engine.api.querydefn.ConditionalExpression;
import org.eclipse.birt.data.engine.api.querydefn.FilterDefinition;
import org.eclipse.birt.data.engine.api.querydefn.GroupDefinition;
import org.eclipse.birt.data.engine.api.querydefn.ScriptExpression;
import org.eclipse.birt.data.engine.api.querydefn.SortDefinition;
import org.eclipse.birt.data.engine.core.DataException;

import testutil.ConfigText;

/**
 * Test case for Computed Column feature
 */

public class ComputedColumnTest extends APITestCase
{

	private IQueryDefinition queryDefinition;

	/** computed column name */
	private String[] ccName;

	/** computed column expression */
	private String[] ccExpr;

	/*
	 * @see org.eclipse.birt.data.engine.api.APITestCase#getDataSourceInfo()
	 */
	protected DataSourceInfo getDataSourceInfo( )
	{
		return new DataSourceInfo( ConfigText.getString( "Api.TestData1.TableName" ),
				ConfigText.getString( "Api.TestData1.TableSQL" ),
				ConfigText.getString( "Api.TestData1.TestDataFileName" ) );
	}

	/**
	 * Test whether duplicated name exception can be found
	 */
	public void testNameDuplicate( ) throws Exception
	{	
		ccName = new String[] { "col0Addcol1", "col0Addcol1" };
		ccExpr = new String[] { "row.COL0+row.COL1",
				"row.COL0*row.COL1" };
		for (int i = 0; i < ccName.length; i++) {
			ComputedColumn computedColumn = new ComputedColumn(ccName[i],
					ccExpr[i], DataType.INTEGER_TYPE);
			((BaseDataSetDesign) this.dataSet)
					.addComputedColumn(computedColumn);
		}

		try
		{
			doTest( );
			fail( "Expecting exception due to duplicate field names." );
		}
		catch ( DataException e )
		{
		}
	}

	/**
	 * Test whether empty column name exception can be found
	 */
	public void testNameEmpty1( ) throws Exception
	{
		ccName = new String[] { null, "col0Addcol2" };
		ccExpr = new String[] { "row.COL0+row.COL1",
				"row.COL0*row.COL1" };
		for (int i = 0; i < ccName.length; i++) {
			ComputedColumn computedColumn = new ComputedColumn(ccName[i],
					ccExpr[i], DataType.INTEGER_TYPE);
			((BaseDataSetDesign) this.dataSet)
					.addComputedColumn(computedColumn);
		}

		try
		{
			doTest( );
			fail( "Custom field name must not be empty." );
		}
		catch ( DataException e )
		{
		}
	}

	/**
	 * Test whether empty column name exception can be found
	 */
	public void testNameEmpty2( ) throws Exception
	{
		ccName = new String[] { "col0Addcol", "" };
		ccExpr = new String[] { "row.COL0+row.COL1",
				"row.COL0*row.COL1" };
		for (int i = 0; i < ccName.length; i++) {
			ComputedColumn computedColumn = new ComputedColumn(ccName[i],
					ccExpr[i], DataType.INTEGER_TYPE);
			((BaseDataSetDesign) this.dataSet)
					.addComputedColumn(computedColumn);
		}
		try {
			doTest();
			fail("Custom field name must not be empty.");
		} catch (DataException e) {
		}
	}

	/**
	 * Test whether invalid expression exception can be found
	 */
	public void testInvalidExpr1( ) throws Exception
	{
		ccName = new String[] { "col0Addcol1", "col0Addcol2" };
		ccExpr = new String[] { "row.COL00row.COL1",
				"row.COL0*row.COL1" };
		for (int i = 0; i < ccName.length; i++) {
			ComputedColumn computedColumn = new ComputedColumn(ccName[i],
					ccExpr[i], DataType.INTEGER_TYPE);
			((BaseDataSetDesign) this.dataSet)
					.addComputedColumn(computedColumn);
		}

		try
		{
			doTest( );
			fail( "Invalid expr: dataSetRow.COL00dataSetRow.COL1" );
		}
		catch ( DataException e )
		{
		}
	}

	/**
	 * Same as testInvalidExpr1
	 * @throws Exception
	 */
	public void testInvalidExpr2( ) throws Exception
	{
		ccName = new String[] { "col0Addcol1", "col0Addcol2" };
		ccExpr = new String[] { "row.COL++row.COL1",
				"row.COL0*row.COL1" };
		for (int i = 0; i < ccName.length; i++) {
			ComputedColumn computedColumn = new ComputedColumn(ccName[i],
					ccExpr[i], DataType.INTEGER_TYPE);
			((BaseDataSetDesign) this.dataSet)
					.addComputedColumn(computedColumn);
		}

		try {
			doTest();
			fail("Invalid expr: dataSetRow.COL++dataSetRow.COL1");
		} catch (DataException e) {
		}		
	}
	
	/**
	 * Expression of computed column can not be null or trimmed to length of
	 * zero.
	 * 
	 * @throws Exception
	 */
	public void testInvalidExpr3( ) throws Exception
	{
		ccName = new String[]{
				"col"
		};
		ccExpr = new String[]{
				""
		};
		for (int i = 0; i < ccName.length; i++) {
			ComputedColumn computedColumn = new ComputedColumn(ccName[i],
					ccExpr[i], DataType.INTEGER_TYPE);
			((BaseDataSetDesign) this.dataSet)
					.addComputedColumn(computedColumn);
		}

		try
		{
			doTest( );
			fail( "Invalid expr: " );
		}
		catch ( DataException e )
		{
		}		
	}
	
	/**
	 * Expression of computed column can not be null or
	 * trimmed to length of zero.
	 * @throws Exception
	 */
	public void testA() throws Exception {
		ccName = new String[] { "col" };
		ccExpr = new String[] { "new String(\"abc\")" };
		for (int i = 0; i < ccName.length; i++) {
			ComputedColumn computedColumn = new ComputedColumn(ccName[i],
					ccExpr[i], DataType.INTEGER_TYPE);
			((BaseDataSetDesign) this.dataSet)
					.addComputedColumn(computedColumn);
		}

		try {
			doTest();
			fail("Invalid expr: ");
		} catch (DataException e) {
			e.printStackTrace();
		}
	}

	/**
	 * test sort on computed column
	 * @throws Exception
	 */
	public void testSortOnComputedColumn() throws Exception {
		ccName = new String[] { "ccc", "ccc2" };
		ccExpr = new String[] { "row.COL0+row.COL1",
				"row.COL1+10" };
		for (int i = 0; i < ccName.length; i++) {
			ComputedColumn computedColumn = new ComputedColumn(ccName[i],
					ccExpr[i], DataType.INTEGER_TYPE);
			((BaseDataSetDesign) this.dataSet)
					.addComputedColumn(computedColumn);
		}

		String[] bindingNameSort = new String[1];
		bindingNameSort[0] = "SORT_DEFINITION";
		IBaseExpression[] bindingExprSort = new IBaseExpression[1];
		bindingExprSort[0] = new ScriptExpression(
				"dataSetRow.ccc/Total.ave(dataSetRow.ccc)");
		SortDefinition[] sortDefn = new SortDefinition[] { new SortDefinition() };
		sortDefn[0].setExpression("row.SORT_DEFINITION");
		sortDefn[0].setSortDirection(ISortDefinition.SORT_DESC);

		String[] bindingNameRow = new String[6];
		bindingNameRow[0] = "ROW_COL0";
		bindingNameRow[1] = "ROW_COL1";
		bindingNameRow[2] = "ROW_COL2";
		bindingNameRow[3] = "ROW_COL3";
		bindingNameRow[4] = "ROW_ccc";
		bindingNameRow[5] = "ROW_ccc2";
		ScriptExpression[] bindingExprRow = new ScriptExpression[] {
				new ScriptExpression("dataSetRow." + "COL0", 0),
				new ScriptExpression("dataSetRow." + "COL1", 0),
				new ScriptExpression("dataSetRow." + "COL2", 0),
				new ScriptExpression("dataSetRow." + "COL3", 0),
				new ScriptExpression("dataSetRow." + ccName[0], 0),
				new ScriptExpression("dataSetRow." + ccName[1], 0), };

		IResultIterator resultIt = this.executeQuery(this.createQuery(null,
				null, null, bindingNameSort, bindingExprSort, sortDefn, null,
				null, null, bindingNameRow, bindingExprRow));

		printResult(resultIt, bindingNameRow, bindingExprRow);
		// assert
		checkOutputFile();

	}
	
	/**
	 * test computed column with script sort expression
	 * @throws Exception
	 */
	public void testSortOnComputedColumn1() throws Exception {
		ccName = new String[] { "cc" };
		ccExpr = new String[] { "(row.COL0%2==0?1:2)" };

		for (int i = 0; i < ccName.length; i++) {
			ComputedColumn computedColumn = new ComputedColumn(ccName[i],
					ccExpr[i], DataType.ANY_TYPE);
			((BaseDataSetDesign) this.dataSet)
					.addComputedColumn(computedColumn);
		}

		String[] bindingNameSort = new String[1];
		bindingNameSort[0] = "SORT_DEFINITION";
		IBaseExpression[] bindingExprSort = new IBaseExpression[1];
		bindingExprSort[0] = new ScriptExpression("dataSetRow.cc");

		SortDefinition[] sortDefn = new SortDefinition[] { new SortDefinition() };
		sortDefn[0].setColumn("SORT_DEFINITION");
		sortDefn[0].setSortDirection(ISortDefinition.SORT_DESC);

		String[] bindingNameRow = new String[5];
		bindingNameRow[0] = "ROW_COL0";
		bindingNameRow[1] = "ROW_COL1";
		bindingNameRow[2] = "ROW_COL2";
		bindingNameRow[3] = "ROW_COL3";
		bindingNameRow[4] = "ROW_cc";
		ScriptExpression[] bindingExprRow = new ScriptExpression[] {
				new ScriptExpression("dataSetRow." + "COL0", 0),
				new ScriptExpression("dataSetRow." + "COL1", 0),
				new ScriptExpression("dataSetRow." + "COL2", 0),
				new ScriptExpression("dataSetRow." + "COL3", 0),
				new ScriptExpression("dataSetRow." + ccName[0], 0), };

		IResultIterator resultIt = this.executeQuery(this.createQuery(null,
				null, null, bindingNameSort, bindingExprSort, sortDefn, null,
				null, null, bindingNameRow, bindingExprRow));

		printResult(resultIt, bindingNameRow, bindingExprRow);
		// assert
		checkOutputFile();
	}
	
	/**
	 * 
	 * @throws Exception
	 */
	public void testFilterOnComputedColumn( ) throws Exception
	{
		ccName = new String[]{
				"ccc", "ccc2"
		};
		ccExpr = new String[]{
				"row.COL0+row.COL1", "row.COL1+10"
		};
		
		for (int i = 0; i < ccName.length; i++) {
			ComputedColumn computedColumn = new ComputedColumn(ccName[i],
					ccExpr[i], DataType.ANY_TYPE);
			((BaseDataSetDesign) this.dataSet)
					.addComputedColumn(computedColumn);
		}
		
		String[] bindingNameFilter = new String[1];
		bindingNameFilter[0] = "FILTER_ccc";
		IBaseExpression[] bindingExprFilter = new IBaseExpression[1];
		bindingExprFilter[0] = new ScriptExpression("dataSetRow.ccc");
		FilterDefinition[] filterDefn = new FilterDefinition[] { new FilterDefinition(
				new ConditionalExpression("row.FILTER_ccc",
						IConditionalExpression.OP_BOTTOM_N, "6")) };
		
		String[] bindingNameRow = new String[6];
		bindingNameRow[0] = "ROW_COL0";
		bindingNameRow[1] = "ROW_COL1";
		bindingNameRow[2] = "ROW_COL2";
		bindingNameRow[3] = "ROW_COL3";
		bindingNameRow[4] = "ROW_ccc";
		bindingNameRow[5] = "ROW_ccc2";
		ScriptExpression[] bindingExprRow = new ScriptExpression[]{
				new ScriptExpression( "dataSetRow." + "COL0", 0 ),
				new ScriptExpression( "dataSetRow." + "COL1", 0 ),
				new ScriptExpression( "dataSetRow." + "COL2", 0 ),
				new ScriptExpression( "dataSetRow." + "COL3", 0 ),
				new ScriptExpression( "dataSetRow." + ccName[0], 0 ),
				new ScriptExpression( "dataSetRow." + ccName[1], 0 ),
		};
		
		IResultIterator resultIt = this.executeQuery(this.createQuery(null,
				null, null, null, null, null, bindingNameFilter,
				bindingExprFilter, filterDefn, bindingNameRow, bindingExprRow));

		printResult(resultIt, bindingNameRow, bindingExprRow);
		// assert
		checkOutputFile();
		
	}
	
	/**
	 * 
	 * @throws Exception
	 */
	public void testGroupOnComputedColumn( ) throws Exception
	{
		ccName = new String[] { "ccc", "ccc2" };
		ccExpr = new String[] { "row.COL0+row.COL1",
				"Total.sum(row.ccc,null,0)*(row.COL3+1)" };

		for (int i = 0; i < ccName.length; i++) {
			ComputedColumn computedColumn = new ComputedColumn(ccName[i],
					ccExpr[i], DataType.ANY_TYPE);
			((BaseDataSetDesign) this.dataSet)
					.addComputedColumn(computedColumn);
		}

		String[] bindingNameGroup = new String[2];
		bindingNameGroup[0] = "GROUP_GROUP1";
		bindingNameGroup[1] = "GROUP_GROUP2";
		IBaseExpression[] bindingExprGroup = new IBaseExpression[2];
		bindingExprGroup[0] = new ScriptExpression("dataSetRow.ccc");
		bindingExprGroup[1] = new ScriptExpression(
				"Total.sum(dataSetRow.ccc,null,1)*(dataSetRow.COL3+1)");
		GroupDefinition[] groupDefn = new GroupDefinition[] {
				new GroupDefinition("group1"), new GroupDefinition("group2") };
		groupDefn[0].setInterval(IGroupDefinition.NUMERIC_INTERVAL);
		groupDefn[0].setKeyExpression("row.GROUP_GROUP1");
		groupDefn[0].setIntervalRange(2);
		groupDefn[1].setKeyExpression("row.GROUP_GROUP2");

		String[] bindingNameRow = new String[6];
		bindingNameRow[0] = "ROW_COL0";
		bindingNameRow[1] = "ROW_COL1";
		bindingNameRow[2] = "ROW_COL2";
		bindingNameRow[3] = "ROW_COL3";
		bindingNameRow[4] = "ROW_ccc";
		bindingNameRow[5] = "ROW_ccc2";
		ScriptExpression[] bindingExprRow = new ScriptExpression[] {
				new ScriptExpression("dataSetRow." + "COL0", 0),
				new ScriptExpression("dataSetRow." + "COL1", 0),
				new ScriptExpression("dataSetRow." + "COL2", 0),
				new ScriptExpression("dataSetRow." + "COL3", 0),
				new ScriptExpression("dataSetRow." + ccName[0], 0),
				new ScriptExpression("dataSetRow." + ccName[1], 0) };

		IResultIterator resultIt = this.executeQuery(this.createQuery(
				bindingNameGroup, bindingExprGroup, groupDefn, null, null,
				null, null, null, null, bindingNameRow, bindingExprRow));

		printResult(resultIt, bindingNameRow, bindingExprRow);
		// assert
		checkOutputFile();
		
	}

	/**
	 * 
	 * @throws Exception
	 */
	public void testAggregationOnComputedColumn( ) throws Exception
	{
		ccName = new String[] { "cc1", "cc2", "cc3", "cc4" };
		ccExpr = new String[] {
				"row.COL0+row.COL1",
				"Total.sum(row.COL1+row.cc1)",
				"Total.ave(row.cc1+row.COL2+row.COL3, null, 0)*81",
				"Total.sum(row.COL1+row.COL2+row.COL3+row.COL0)" };
		
		for (int i = 0; i < ccName.length; i++) {
			ComputedColumn computedColumn = new ComputedColumn(ccName[i],
					ccExpr[i], DataType.ANY_TYPE);
			((BaseDataSetDesign) this.dataSet)
					.addComputedColumn(computedColumn);
		}
		
		String[] bindingNameRow = new String[8];
		bindingNameRow[0] = "ROW_COL0";
		bindingNameRow[1] = "ROW_COL1";
		bindingNameRow[2] = "ROW_COL2";
		bindingNameRow[3] = "ROW_COL3";
		bindingNameRow[4] = "ROW_cc1";
		bindingNameRow[5] = "ROW_cc2";
		bindingNameRow[6] = "ROW_cc3";
		bindingNameRow[7] = "ROW_cc4";

		ScriptExpression[] bindingExprRow = new ScriptExpression[] {
				new ScriptExpression("dataSetRow." + "COL0", 0),
				new ScriptExpression("dataSetRow." + "COL1", 0),
				new ScriptExpression("dataSetRow." + "COL2", 0),
				new ScriptExpression("dataSetRow." + "COL3", 0),
				new ScriptExpression("dataSetRow." + ccName[0], 0),
				new ScriptExpression("dataSetRow." + ccName[1], 0),
				new ScriptExpression("dataSetRow." + ccName[2], 0),
				new ScriptExpression("dataSetRow." + ccName[3], 0) };

		IResultIterator resultIt = this.executeQuery(this.createQuery(
				null, null, null, null, null,
				null, null, null, null, bindingNameRow, bindingExprRow));
		
		printResult(resultIt, bindingNameRow, bindingExprRow);
		// assert
		checkOutputFile();
		
	}
	
	/**
	 * 
	 * @throws Exception
	 */
	public void testNewAggregationOnComputedColumn( ) throws Exception
	{
		ccName = new String[] { "cc1", "cc2", "cc3", "cc4" };
		ccExpr = new String[] {
				"row.COL0+row.COL1",
				"Total.sum(row.COL1+row.cc1)",
				"Total.ave(row.cc1+row.COL2+row.COL3, null, 0)*81",
				"Total.sum(row.COL1+row.COL2+row.COL3+row.COL0)" };
		
		ComputedColumn cc1 = new ComputedColumn( "cc1", "row.COL0+row.COL1" );
		ComputedColumn cc2 = new ComputedColumn( "cc2",
				"row.COL1+row.cc1",
				DataType.ANY_TYPE,
				"SUM",
				null,
				new ArrayList( ) );
		ComputedColumn cc31 = new ComputedColumn( "cc31",
				"row.cc1+row.COL2+row.COL3",
				DataType.ANY_TYPE,
				"AVE",
				null,
				new ArrayList( ) );
		ComputedColumn cc3 = new ComputedColumn( "cc3", "row.cc31*81");
		ComputedColumn cc4 = new ComputedColumn( "cc4",
				"row.COL1+row.COL2+row.COL3+row.COL0",
				DataType.ANY_TYPE,
				"SUM",
				null,
				new ArrayList( ) );
		
		((BaseDataSetDesign) this.dataSet).addComputedColumn( cc1 );
		((BaseDataSetDesign) this.dataSet).addComputedColumn( cc2 );
		((BaseDataSetDesign) this.dataSet).addComputedColumn( cc3 );
		((BaseDataSetDesign) this.dataSet).addComputedColumn( cc31 );
		((BaseDataSetDesign) this.dataSet).addComputedColumn( cc4 );

		String[] bindingNameRow = new String[8];
		bindingNameRow[0] = "ROW_COL0";
		bindingNameRow[1] = "ROW_COL1";
		bindingNameRow[2] = "ROW_COL2";
		bindingNameRow[3] = "ROW_COL3";
		bindingNameRow[4] = "ROW_cc1";
		bindingNameRow[5] = "ROW_cc2";
		bindingNameRow[6] = "ROW_cc3";
		bindingNameRow[7] = "ROW_cc4";

		ScriptExpression[] bindingExprRow = new ScriptExpression[] {
				new ScriptExpression("dataSetRow." + "COL0", 0),
				new ScriptExpression("dataSetRow." + "COL1", 0),
				new ScriptExpression("dataSetRow." + "COL2", 0),
				new ScriptExpression("dataSetRow." + "COL3", 0),
				new ScriptExpression("dataSetRow." + ccName[0], 0),
				new ScriptExpression("dataSetRow." + ccName[1], 0),
				new ScriptExpression("dataSetRow." + ccName[2], 0),
				new ScriptExpression("dataSetRow." + ccName[3], 0) };

		IResultIterator resultIt = this.executeQuery(this.createQuery(
				null, null, null, null, null,
				null, null, null, null, bindingNameRow, bindingExprRow));
		
		printResult(resultIt, bindingNameRow, bindingExprRow);
		// assert
		checkOutputFile();
		
	}
	
	/**
	 * test nested aggregation computed column
	 * @throws Exception
	 */
	public void testNestedAggregationOnComputedColumn( ) throws Exception
	{
		ccName = new String[] { "cc1", "cc2", "cc3" };
		ccExpr = new String[] {
				"row.COL0+row.COL1",
				"Total.runningSum(row.cc1/Total.sum(row.cc1))*100",
				"Total.runningSum(Total.sum(row.cc1/Total.sum(row.cc1)))" };

		for (int i = 0; i < ccName.length; i++) {
			ComputedColumn computedColumn = new ComputedColumn(ccName[i],
					ccExpr[i], DataType.ANY_TYPE);
			((BaseDataSetDesign) this.dataSet)
					.addComputedColumn(computedColumn);
		}
		
		String[] bindingNameRow = new String[7];
		bindingNameRow[0] = "ROW_COL0";
		bindingNameRow[1] = "ROW_COL1";
		bindingNameRow[2] = "ROW_COL2";
		bindingNameRow[3] = "ROW_COL3";
		bindingNameRow[4] = "ROW_cc1";
		bindingNameRow[5] = "ROW_cc2";
		bindingNameRow[6] = "ROW_cc3";
		ScriptExpression[] bindingExprRow = new ScriptExpression[] {
				new ScriptExpression("dataSetRow." + "COL0", 0),
				new ScriptExpression("dataSetRow." + "COL1", 0),
				new ScriptExpression("dataSetRow." + "COL2", 0),
				new ScriptExpression("dataSetRow." + "COL3", 0),
				new ScriptExpression("dataSetRow." + ccName[0], 0),
				new ScriptExpression("dataSetRow." + ccName[1], 0),
				new ScriptExpression("dataSetRow." + ccName[2], 0) };

		IResultIterator resultIt = this.executeQuery(this.createQuery(null,
				null, null, null, null, null, null, null, null, bindingNameRow,
				bindingExprRow));

		printResult(resultIt, bindingNameRow, bindingExprRow);
		// assert
		checkOutputFile();
		
	}
	
	/**
	 * test nested computed column
	 * @throws Exception
	 */
	public void testNestedComputedColumn( ) throws Exception
	{
		ccName = new String[] { "cc1", "cc2", "cc3" };
		ccExpr = new String[] {
				"row.COL0+row.COL1",
				"row.cc1*100",
				"Total.runningSum(row.cc1/Total.sum(row.cc1))*100" };

		for (int i = 0; i < ccName.length; i++) {
			ComputedColumn computedColumn = new ComputedColumn(ccName[i],
					ccExpr[i], DataType.ANY_TYPE);
			((BaseDataSetDesign) this.dataSet)
					.addComputedColumn(computedColumn);
		}
		
		String[] bindingNameRow = new String[7];
		bindingNameRow[0] = "ROW_COL0";
		bindingNameRow[1] = "ROW_COL1";
		bindingNameRow[2] = "ROW_COL2";
		bindingNameRow[3] = "ROW_COL3";
		bindingNameRow[4] = "ROW_cc1";
		bindingNameRow[5] = "ROW_cc2";
		bindingNameRow[6] = "ROW_cc3";
		ScriptExpression[] bindingExprRow = new ScriptExpression[] {
				new ScriptExpression("dataSetRow." + "COL0", 0),
				new ScriptExpression("dataSetRow." + "COL1", 0),
				new ScriptExpression("dataSetRow." + "COL2", 0),
				new ScriptExpression("dataSetRow." + "COL3", 0),
				new ScriptExpression("dataSetRow." + ccName[0], 0),
				new ScriptExpression("dataSetRow." + ccName[1], 0),
				new ScriptExpression("dataSetRow." + ccName[2], 0) };

		IResultIterator resultIt = this.executeQuery(this.createQuery(null,
				null, null, null, null, null, null, null, null, bindingNameRow,
				bindingExprRow));

		printResult(resultIt, bindingNameRow, bindingExprRow);
		// assert
		checkOutputFile();
		
	}	
	
	/**
	 * test aggregation contains filter computed column
	 * 
	 * @throws Exception
	 */
	public void testFilterOnAggregationColumn() throws Exception {
		ccName = new String[] { "cc1", "cc2", "cc3" };
		ccExpr = new String[] { "row.COL0+row.COL1",
				"Total.runningSum(row.cc1,row.COL0==0,0)",
				"Total.runningSum(row.cc1,row.COL0>0,0)" };

		for (int i = 0; i < ccName.length; i++) {
			ComputedColumn computedColumn = new ComputedColumn(ccName[i],
					ccExpr[i], DataType.ANY_TYPE);
			((BaseDataSetDesign) this.dataSet)
					.addComputedColumn(computedColumn);
		}

		String[] bindingNameRow = new String[7];
		bindingNameRow[0] = "ROW_COL0";
		bindingNameRow[1] = "ROW_COL1";
		bindingNameRow[2] = "ROW_COL2";
		bindingNameRow[3] = "ROW_COL3";
		bindingNameRow[4] = "ROW_cc1";
		bindingNameRow[5] = "ROW_cc2";
		bindingNameRow[6] = "ROW_cc3";
		ScriptExpression[] bindingExprRow = new ScriptExpression[] {
				new ScriptExpression("dataSetRow." + "COL0", 0),
				new ScriptExpression("dataSetRow." + "COL1", 0),
				new ScriptExpression("dataSetRow." + "COL2", 0),
				new ScriptExpression("dataSetRow." + "COL3", 0),
				new ScriptExpression("dataSetRow." + ccName[0], 0),
				new ScriptExpression("dataSetRow." + ccName[1], 0),
				new ScriptExpression("dataSetRow." + ccName[2], 0) };

		IResultIterator resultIt = this.executeQuery(this.createQuery(null,
				null, null, null, null, null, null, null, null, bindingNameRow,
				bindingExprRow));

		printResult(resultIt, bindingNameRow, bindingExprRow);
		// assert
		checkOutputFile();

	}
	
	/**
	 * Test group filters when computed columns include aggregations.
	 * 
	 * @throws Exception
	 */
	public void testGroupFilterOnComputedColumnsWithAggregations( ) throws Exception
	{
		ccName = new String[] { "cc1", "cc2", "cc3", "cc4" };
		ccExpr = new String[] {
				"row.COL0+row.COL1",
				"Total.sum(row.COL1+row.cc1)",
				"Total.ave(row.cc1+row.COL2+row.COL3, null, 0)*81",
				"Total.sum(row.COL1+row.COL2+row.COL3+row.COL0)" };
		
		for (int i = 0; i < ccName.length; i++) {
			ComputedColumn computedColumn = new ComputedColumn(ccName[i],
					ccExpr[i], DataType.ANY_TYPE);
			((BaseDataSetDesign) this.dataSet)
					.addComputedColumn(computedColumn);
		}
		
		String[] bindingNameGroup = new String[1];
		bindingNameGroup[0] = "GROUP_GROUP1";
		IBaseExpression[] bindingExprGroup = new IBaseExpression[1];
		bindingExprGroup[0] = new ScriptExpression("dataSetRow.cc1");

		GroupDefinition[] groupDefn = new GroupDefinition[] {
				new GroupDefinition("group1") };	
		groupDefn[0].setKeyExpression("row.GROUP_GROUP1");
		
		FilterDefinition filter = new FilterDefinition(new ScriptExpression(
				"Total.sum(dataSetRow.COL0)>400"));
		groupDefn[0].addFilter(filter);

		String[] bindingNameRow = new String[8];
		bindingNameRow[0] = "ROW_COL0";
		bindingNameRow[1] = "ROW_COL1";
		bindingNameRow[2] = "ROW_COL2";
		bindingNameRow[3] = "ROW_COL3";
		bindingNameRow[4] = "ROW_cc1";
		bindingNameRow[5] = "ROW_cc2";
		bindingNameRow[6] = "ROW_cc3";
		bindingNameRow[7] = "ROW_cc4";
		ScriptExpression[] bindingExprRow = new ScriptExpression[] {
				new ScriptExpression("dataSetRow." + "COL0", 0),
				new ScriptExpression("dataSetRow." + "COL1", 0),
				new ScriptExpression("dataSetRow." + "COL2", 0),
				new ScriptExpression("dataSetRow." + "COL3", 0),
				new ScriptExpression("dataSetRow." + ccName[0], 0),
				new ScriptExpression("dataSetRow." + ccName[1], 0),
				new ScriptExpression("dataSetRow." + ccName[2], 0),
				new ScriptExpression("dataSetRow." + ccName[3], 0) };

		try {
			this.executeQuery(this.createQuery(null, null, null, null, null,
					null, null, null, null, bindingNameRow, bindingExprRow));
			// fail( "Should not arrive here" );
		} catch (DataException e) {

		}
	}
	
	/**
	 * 
	 * @throws Exception
	 */
	public void testWrongDataType( ) throws Exception
	{
		ccName = new String[] { "ccc", "ccc2" };
		ccExpr = new String[] { "'abc'+row.COL0+row.COL1+'abc'",
				"'a'+row.COL1+'abc'" };

		for ( int i = 0; i < ccName.length; i++ )
		{
			ComputedColumn computedColumn = new ComputedColumn( ccName[i],
					ccExpr[i],
					DataType.INTEGER_TYPE );
			( (BaseDataSetDesign) this.dataSet ).addComputedColumn( computedColumn );
		}
		
		String[] bindingNameRow = new String[6];
		bindingNameRow[0] = "ROW_COL0";
		bindingNameRow[1] = "ROW_COL1";
		bindingNameRow[2] = "ROW_COL2";
		bindingNameRow[3] = "ROW_COL3";
		bindingNameRow[4] = "ROW_ccc";
		bindingNameRow[5] = "ROW_ccc2";
		ScriptExpression[] bindingExprRow = new ScriptExpression[]{
				new ScriptExpression( "dataSetRow." + "COL0", 0 ),
				new ScriptExpression( "dataSetRow." + "COL1", 0 ),
				new ScriptExpression( "dataSetRow." + "COL2", 0 ),
				new ScriptExpression( "dataSetRow." + "COL3", 0 ),
				new ScriptExpression( "dataSetRow." + ccName[0], 0 ),
				new ScriptExpression( "dataSetRow." + ccName[1], 0 ),
		};

		try {
			Object o = this.executeQuery(this.createQuery(null, null, null, null, null,
					null, null, null, null, bindingNameRow, bindingExprRow));
			System.out.println(o);
		} catch (DataException e) {
			// expect a DataException
			return;
		}
		fail("Should throw a DataException.");
	}
	
	/**
	 * 
	 * @throws Exception
	 */
	public void testRowIndex( ) throws Exception
	{
		ccName = new String[] { "ccc" };
		ccExpr = new String[] { "row[0]" };
		for (int i = 0; i < ccName.length; i++) {
			ComputedColumn computedColumn = new ComputedColumn(ccName[i],
					ccExpr[i], DataType.ANY_TYPE);
			((BaseDataSetDesign) this.dataSet)
					.addComputedColumn(computedColumn);
		}
		
		String[] bindingNameRow = new String[5];
		bindingNameRow[0] = "ROW_COL0";
		bindingNameRow[1] = "ROW_COL1";
		bindingNameRow[2] = "ROW_COL2";
		bindingNameRow[3] = "ROW_COL3";
		bindingNameRow[4] = "ROW_ccc";
		ScriptExpression[] bindingExprRow = new ScriptExpression[]{
				new ScriptExpression( "dataSetRow." + "COL0", 0 ),
				new ScriptExpression( "dataSetRow." + "COL1", 0 ),
				new ScriptExpression( "dataSetRow." + "COL2", 0 ),
				new ScriptExpression( "dataSetRow." + "COL3", 0 ),
				new ScriptExpression( "dataSetRow." + ccName[0], 0 )
		};
		

		IResultIterator resultIt = this.executeQuery(this.createQuery(null,
				null, null, null, null, null, null, null, null, bindingNameRow,
				bindingExprRow));

		printResult(resultIt, bindingNameRow, bindingExprRow);
		// assert
		checkOutputFile();
	}
	
	/**
	 * Test multi-pass on sort. Currently only indirect aggregation
	 * function nestings are supported.
	 * @throws Exception
	 */
	public void testMultipass_Sort() throws Exception
	{
		ccName = new String[]{
				"cc1", "cc2"
		};
		ccExpr = new String[]{
				"Total.sum(row.COL1)", "Total.sum(row.cc1)"
		};

		for ( int i = 0; i < ccName.length; i++ )
		{
			ComputedColumn computedColumn = new ComputedColumn( ccName[i],
					ccExpr[i],
					DataType.INTEGER_TYPE );
			( (BaseDataSetDesign) this.dataSet ).addComputedColumn( computedColumn );
		}
		
		String[] bindingNameSort = new String[2];
		bindingNameSort[0] = "SORT_DEFINITION_1";
		bindingNameSort[1] = "SORT_DEFINITION_2";
		IBaseExpression[] bindingExprSort = new IBaseExpression[2];
		bindingExprSort[0] = new ScriptExpression(
				"dataSetRow.COL1/(Total.ave(dataSetRow.cc1)+dataSetRow.cc1)");
		bindingExprSort[1] = new ScriptExpression(
				"dataSetRow.cc2-dataSetRow.COL2");
		SortDefinition[] sortDefn = new SortDefinition[] {
				new SortDefinition(), new SortDefinition() };
		sortDefn[0].setColumn("SORT_DEFINITION_1");
		sortDefn[0].setSortDirection(ISortDefinition.SORT_DESC);
		sortDefn[1].setColumn("SORT_DEFINITION_2");
		sortDefn[1].setSortDirection(ISortDefinition.SORT_DESC);

		String[] bindingNameRow = new String[6];
		bindingNameRow[0] = "ROW_COL0";
		bindingNameRow[1] = "ROW_COL1";
		bindingNameRow[2] = "ROW_COL2";
		bindingNameRow[3] = "ROW_COL3";
		bindingNameRow[4] = "ROW_cc1";
		bindingNameRow[5] = "ROW_cc2";
		ScriptExpression[] bindingExprRow = new ScriptExpression[]{
				new ScriptExpression( "dataSetRow." + "COL0", 0 ),
				new ScriptExpression( "dataSetRow." + "COL1", 0 ),
				new ScriptExpression( "dataSetRow." + "COL2", 0 ),
				new ScriptExpression( "dataSetRow." + "COL3", 0 ),
				new ScriptExpression( "dataSetRow." + ccName[0], 0 ),
				new ScriptExpression( "dataSetRow." + ccName[1], 0 ),
		};
		IResultIterator resultIt = this.executeQuery(this.createQuery(null,
				null, null, bindingNameSort, bindingExprSort, sortDefn, null,
				null, null, bindingNameRow, bindingExprRow));

		printResult(resultIt, bindingNameRow, bindingExprRow);	
		// assert
		checkOutputFile();
	}
	
	/**
	 * 
	 * @throws Exception
	 */
	public void testMultiPass_Group() throws Exception {
		ccName = new String[] { "cc1", "cc2" };
		ccExpr = new String[] {
				"row.COL0+row.COL1+row.COL2+row.COL3",
				"row.cc1+10" };
		
		for (int i = 0; i < ccName.length; i++) {
			ComputedColumn computedColumn = new ComputedColumn(ccName[i],
					ccExpr[i], DataType.ANY_TYPE);
			((BaseDataSetDesign) this.dataSet)
					.addComputedColumn(computedColumn);
		}
		
		String[] bindingNameGroup = new String[2];
		bindingNameGroup[0] = "GROUP_GROUP1";
		bindingNameGroup[1] = "GROUP_GROUP2";
		IBaseExpression[] bindingExprGroup = new IBaseExpression[2];
		bindingExprGroup[0] = new ScriptExpression("dataSetRow.COL1");
		bindingExprGroup[1] = new ScriptExpression(
				"dataSetRow.cc2+Total.sum(dataSetRow.COL1,null,1)");
		GroupDefinition[] groupDefn = new GroupDefinition[] {
				new GroupDefinition("group1"), new GroupDefinition("group2") };
		groupDefn[0].setKeyExpression("row.GROUP_GROUP1");
		groupDefn[1].setKeyExpression("row.GROUP_GROUP2");

		String[] bindingNameRow = new String[6];
		bindingNameRow[0] = "ROW_COL0";
		bindingNameRow[1] = "ROW_COL1";
		bindingNameRow[2] = "ROW_COL2";
		bindingNameRow[3] = "ROW_COL3";
		bindingNameRow[4] = "ROW_cc1";
		bindingNameRow[5] = "ROW_cc2";
		ScriptExpression[] bindingExprRow = new ScriptExpression[] {
				new ScriptExpression("dataSetRow." + "COL0", 0),
				new ScriptExpression("dataSetRow." + "COL1", 0),
				new ScriptExpression("dataSetRow." + "COL2", 0),
				new ScriptExpression("dataSetRow." + "COL3", 0),
				new ScriptExpression("dataSetRow." + ccName[0], 0),
				new ScriptExpression("dataSetRow." + ccName[1], 0) };

		IResultIterator resultIt = this.executeQuery(this.createQuery(
				bindingNameGroup, bindingExprGroup, groupDefn, null, null,
				null, null, null, null, bindingNameRow, bindingExprRow));

		printResult(resultIt, bindingNameRow, bindingExprRow);
		// assert
		checkOutputFile();

	}
	
	/**
	 * 
	 * @throws Exception
	 */
	public void testWrongColumnName( ) throws Exception
	{
		ccName = new String[] { "ccc", "ccc2" };
		ccExpr = new String[] { "row.COL0+row.COL1",
				"row.COL1+10" };
		for (int i = 0; i < ccName.length; i++) {
			ComputedColumn computedColumn = new ComputedColumn(ccName[i],
					ccExpr[i], DataType.ANY_TYPE);
			((BaseDataSetDesign) this.dataSet)
					.addComputedColumn(computedColumn);
		}
		
		String[] bindingNameSort = new String[1];
		bindingNameSort[0] = "SORT_DEFINITION";
		IBaseExpression[] bindingExprSort = new IBaseExpression[1];
		bindingExprSort[0] = new ScriptExpression(
				"dataSetRow.ccc/Total.ave(dataSetRow.ccc)");
		SortDefinition[] sortDefn = new SortDefinition[] { new SortDefinition() };
		sortDefn[0].setColumn("SORT_DEFINITION");
		sortDefn[0].setSortDirection(ISortDefinition.SORT_DESC);

		String[] bindingNameRow = new String[6];
		bindingNameRow[0] = "ROW_COL0";
		bindingNameRow[1] = "ROW_COL1";
		bindingNameRow[2] = "ROW_COL2";
		bindingNameRow[3] = "ROW_COL3";
		bindingNameRow[4] = "ROW_cc1";
		bindingNameRow[5] = "ROW_cc2";
		ScriptExpression[] bindingExprRow = new ScriptExpression[] {
				new ScriptExpression("dataSetRow." + "COL0", 0),
				new ScriptExpression("dataSetRow." + "COL1", 0),
				new ScriptExpression("dataSetRow." + "COL2", 0),
				new ScriptExpression("dataSetRow." + "COL3", 0),
				new ScriptExpression("dataSetRow." + "cc2", 0),
				new ScriptExpression("dataSetRow." + ccName[1], 0), };

		try
		{
			IResultIterator resultIt = this.executeQuery( this.createQuery( null,
					null,
					null,
					bindingNameSort,
					bindingExprSort,
					sortDefn,
					null,
					null,
					null,
					bindingNameRow,
					bindingExprRow ) );
			
			printResult( resultIt, bindingNameRow, bindingExprRow );
			fail("should throw exception");

		} catch (DataException e) {

		}
	}
	
	/**
	 * print resultset iterator
	 * 
	 * @param resultIt
	 * @param bindingNameRow
	 * @param bindingExprRow
	 * @throws Exception
	 */
	private void printResult(IResultIterator resultIt, String[] bindingNameRow,
			ScriptExpression[] bindingExprRow) throws Exception {
		// output column name
		String str = "";
		for (int i = 0; i < bindingNameRow.length; i++) {
			str += bindingExprRow[i].getText().replaceAll("dataSetRow\\Q.\\E",
					"");
			str += "  ";
		}
		testPrintln(str);

		// output row data
		while ( resultIt.next( ) )
		{
			str = "";
			for ( int i = 0; i < bindingNameRow.length; i++ )
			{
				str += " ";
				if ( resultIt.getValue( bindingNameRow[i] ) == null )
					str += "<null>";
				else
					str += (int) Double.parseDouble( resultIt.getValue( bindingNameRow[i] )
							.toString( ) );
				str += "    ";
			}
			testPrintln(str);
		}
	}
	
	private IResultIterator doTest() throws Exception {
		queryDefinition = getDefaultQueryDefn(dataSet.getName());
		return executeQuery(queryDefinition);
	}	
}