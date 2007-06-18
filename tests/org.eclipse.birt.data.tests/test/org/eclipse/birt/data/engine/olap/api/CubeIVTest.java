
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
package org.eclipse.birt.data.engine.olap.api;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.olap.OLAPException;
import javax.olap.cursor.CubeCursor;
import javax.olap.cursor.DimensionCursor;
import javax.olap.cursor.EdgeCursor;

import org.eclipse.birt.core.archive.FileArchiveReader;
import org.eclipse.birt.core.archive.FileArchiveWriter;
import org.eclipse.birt.core.archive.IDocArchiveWriter;
import org.eclipse.birt.core.exception.BirtException;
import org.eclipse.birt.data.engine.api.DataEngine;
import org.eclipse.birt.data.engine.api.DataEngineContext;
import org.eclipse.birt.data.engine.api.IBinding;
import org.eclipse.birt.data.engine.api.IConditionalExpression;
import org.eclipse.birt.data.engine.api.IFilterDefinition;
import org.eclipse.birt.data.engine.api.ISortDefinition;
import org.eclipse.birt.data.engine.api.aggregation.IBuildInAggregation;
import org.eclipse.birt.data.engine.api.querydefn.Binding;
import org.eclipse.birt.data.engine.api.querydefn.ConditionalExpression;
import org.eclipse.birt.data.engine.api.querydefn.FilterDefinition;
import org.eclipse.birt.data.engine.api.querydefn.ScriptExpression;
import org.eclipse.birt.data.engine.core.DataException;
import org.eclipse.birt.data.engine.olap.api.query.ICubeQueryDefinition;
import org.eclipse.birt.data.engine.olap.api.query.IDimensionDefinition;
import org.eclipse.birt.data.engine.olap.api.query.IEdgeDefinition;
import org.eclipse.birt.data.engine.olap.api.query.IHierarchyDefinition;
import org.eclipse.birt.data.engine.olap.api.query.ILevelDefinition;
import org.eclipse.birt.data.engine.olap.cursor.CubeUtility;
import org.eclipse.birt.data.engine.olap.data.api.cube.CubeMaterializer;
import org.eclipse.birt.data.engine.olap.data.api.cube.IHierarchy;
import org.eclipse.birt.data.engine.olap.data.api.cube.ILevelDefn;
import org.eclipse.birt.data.engine.olap.data.api.cube.StopSign;
import org.eclipse.birt.data.engine.olap.data.document.IDocumentManager;
import org.eclipse.birt.data.engine.olap.data.impl.Cube;
import org.eclipse.birt.data.engine.olap.data.impl.dimension.Dimension;
import org.eclipse.birt.data.engine.olap.data.impl.dimension.DimensionFactory;
import org.eclipse.birt.data.engine.olap.data.impl.dimension.DimensionForTest;
import org.eclipse.birt.data.engine.olap.data.impl.dimension.LevelDefinition;
import org.eclipse.birt.data.engine.olap.impl.query.CubeQueryDefinition;
import org.eclipse.birt.data.engine.olap.impl.query.CubeSortDefinition;

import testutil.BaseTestCase;


/**
 * 
 */

public class CubeIVTest extends BaseTestCase
{
	private static String cubeName = "cube";
	
	private static String documentPath = System.getProperty( "java.io.tmpdir" );
	
	static
	{
		if( !documentPath.endsWith( File.separator ))
		{
			documentPath += File.separator;
		}
	}
	
	/**
	 * Test use all dimension levels.
	 * @throws Exception
	 */
	public void testBasic( ) throws Exception
	{
		ICubeQueryDefinition cqd = new CubeQueryDefinition( cubeName);
		IEdgeDefinition columnEdge = cqd.createEdge( ICubeQueryDefinition.COLUMN_EDGE );
		IEdgeDefinition rowEdge = cqd.createEdge( ICubeQueryDefinition.ROW_EDGE );
		IDimensionDefinition dim1 = columnEdge.createDimension( "dimension1" );
		IHierarchyDefinition hier1 = dim1.createHierarchy( "dimension1" );
		hier1.createLevel( "level11" );
		hier1.createLevel( "level12" );
		hier1.createLevel( "level13" );
		
		IDimensionDefinition dim2 = rowEdge.createDimension( "dimension2" );
		IHierarchyDefinition hier2 = dim2.createHierarchy( "dimension2" );
		hier2.createLevel( "level21" );
		
		cqd.createMeasure( "measure1" );
		
		IBinding binding1 = new Binding( "edge1level1");
		
		binding1.setExpression( new ScriptExpression("dimension[\"dimension1\"][\"level11\"]") );
		cqd.addBinding( binding1 );
		
		IBinding binding2 = new Binding( "edge1level2");
		
		binding2.setExpression( new ScriptExpression("dimension[\"dimension1\"][\"level12\"]") );
		cqd.addBinding( binding2 );
		
		IBinding binding3 = new Binding( "edge1level3");
		
		binding3.setExpression( new ScriptExpression("dimension[\"dimension1\"][\"level13\"]") );
		cqd.addBinding( binding3 );
		
		IBinding binding4 = new Binding( "edge2level1");
		
		binding4.setExpression( new ScriptExpression("dimension[\"dimension2\"][\"level21\"]") );
		cqd.addBinding( binding4 );
		
		IBinding binding5 = new Binding( "measure1" );
		binding5.setExpression( new ScriptExpression("measure[\"measure1\"]") );
		cqd.addBinding( binding5 );
		
		cqd.setCacheQueryResults( true );
		FileArchiveWriter writter = new FileArchiveWriter( documentPath + "testTemp" );
		DataEngine engine = DataEngine.newDataEngine( DataEngineContext.newInstance( DataEngineContext.MODE_GENERATION,
				null,
				null,
				writter ) );
		this.createCube( writter, engine );
		
		IPreparedCubeQuery pcq = engine.prepare( cqd, null );
		ICubeQueryResults queryResults = pcq.execute( null );
		CubeCursor cursor = queryResults.getCubeCursor( );
		
		
		//Load from cache.
		cqd.setQueryResultsID( queryResults.getID( ) );
		pcq = engine.prepare( cqd, null );
		queryResults = pcq.execute( null );
		cursor = queryResults.getCubeCursor( );
		
		writter.finish( );
		engine.shutdown( );
		
		//Load from RD
		FileArchiveReader reader = new FileArchiveReader( documentPath + "testTemp" );
		engine = DataEngine.newDataEngine( DataEngineContext.newInstance( DataEngineContext.MODE_PRESENTATION,
				null,
				reader,
				null ) );
		cqd.setQueryResultsID( queryResults.getID( ) );
		pcq = engine.prepare( cqd, null );
		queryResults = pcq.execute( null );
		cursor = queryResults.getCubeCursor( );
		
		
		List columnEdgeBindingNames = new ArrayList();
		columnEdgeBindingNames.add( "edge1level1" );
		columnEdgeBindingNames.add( "edge1level2" );
		columnEdgeBindingNames.add( "edge1level3" );
		this.printCube( cursor, columnEdgeBindingNames, "edge2level1", "measure1" );
		
		engine.shutdown( );
	}
	
	/**
	 * Test use part of dimension levels.
	 * @throws Exception
	 */
	public void testBasic1( ) throws Exception
	{
		ICubeQueryDefinition cqd = new CubeQueryDefinition( cubeName);
		IEdgeDefinition columnEdge = cqd.createEdge( ICubeQueryDefinition.COLUMN_EDGE );
		IEdgeDefinition rowEdge = cqd.createEdge( ICubeQueryDefinition.ROW_EDGE );
		IDimensionDefinition dim1 = columnEdge.createDimension( "dimension1" );
		IHierarchyDefinition hier1 = dim1.createHierarchy( "dimension1" );
		hier1.createLevel( "level11" );
		hier1.createLevel( "level12" );
				
		IDimensionDefinition dim2 = rowEdge.createDimension( "dimension2" );
		IHierarchyDefinition hier2 = dim2.createHierarchy( "dimension2" );
		hier2.createLevel( "level21" );
		
		cqd.createMeasure( "measure1" );
		
		IBinding binding1 = new Binding( "edge1level1");
		
		binding1.setExpression( new ScriptExpression("dimension[\"dimension1\"][\"level11\"]") );
		cqd.addBinding( binding1 );
		
		IBinding binding2 = new Binding( "edge1level2");
		
		binding2.setExpression( new ScriptExpression("dimension[\"dimension1\"][\"level12\"]") );
		cqd.addBinding( binding2 );
		
		IBinding binding4 = new Binding( "edge2level1");
		
		binding4.setExpression( new ScriptExpression("dimension[\"dimension2\"][\"level21\"]") );
		cqd.addBinding( binding4 );
		
		IBinding binding5 = new Binding( "measure1" );
		binding5.setExpression( new ScriptExpression("measure[\"measure1\"]") );
		cqd.addBinding( binding5 );
		
		List columnEdgeBindingNames = new ArrayList();
		columnEdgeBindingNames.add( "edge1level1" );
		columnEdgeBindingNames.add( "edge1level2" );
		
		cqd.setCacheQueryResults( true );
		FileArchiveWriter writter = new FileArchiveWriter( documentPath + "testTemp" );
		DataEngine engine = DataEngine.newDataEngine( DataEngineContext.newInstance( DataEngineContext.MODE_GENERATION,
				null,
				null,
				writter ) );
		this.createCube( writter, engine );
		
		IPreparedCubeQuery pcq = engine.prepare( cqd, null );
		ICubeQueryResults queryResults = pcq.execute( null );
		CubeCursor cursor = queryResults.getCubeCursor( );
		
		
		//Load from cache.
		cqd.setQueryResultsID( queryResults.getID( ) );
		pcq = engine.prepare( cqd, null );
		queryResults = pcq.execute( null );
		cursor = queryResults.getCubeCursor( );
		
		writter.finish( );
		engine.shutdown( );
		
		//Load from RD
		FileArchiveReader reader = new FileArchiveReader(documentPath + "testTemp" );
		engine = DataEngine.newDataEngine( DataEngineContext.newInstance( DataEngineContext.MODE_PRESENTATION,
				null,
				reader,
				null ) );
		cqd.setQueryResultsID( queryResults.getID( ) );
		pcq = engine.prepare( cqd, null );
		queryResults = pcq.execute( null );
		cursor = queryResults.getCubeCursor( );

		this.printCube( cursor, columnEdgeBindingNames, "edge2level1", "measure1" );
	
	}
	
	/**
	 * Filter1, filter out all level11 == CN.
	 * @throws Exception
	 */
	public void testFilter1( ) throws Exception
	{
		ICubeQueryDefinition cqd = new CubeQueryDefinition( cubeName);
		IEdgeDefinition columnEdge = cqd.createEdge( ICubeQueryDefinition.COLUMN_EDGE );
		IEdgeDefinition rowEdge = cqd.createEdge( ICubeQueryDefinition.ROW_EDGE );
		IDimensionDefinition dim1 = columnEdge.createDimension( "dimension1" );
		IHierarchyDefinition hier1 = dim1.createHierarchy( "dimension1" );
		hier1.createLevel( "level11" );
		hier1.createLevel( "level12" );
				
		IDimensionDefinition dim2 = rowEdge.createDimension( "dimension2" );
		IHierarchyDefinition hier2 = dim2.createHierarchy( "dimension2" );
		hier2.createLevel( "level21" );
		
		cqd.createMeasure( "measure1" );
		
		IBinding binding1 = new Binding( "edge1level1");
		
		binding1.setExpression( new ScriptExpression("dimension[\"dimension1\"][\"level11\"]") );
		cqd.addBinding( binding1 );
		
		IBinding binding2 = new Binding( "edge1level2");
		
		binding2.setExpression( new ScriptExpression("dimension[\"dimension1\"][\"level12\"]") );
		cqd.addBinding( binding2 );
		
		IBinding binding4 = new Binding( "edge2level1");
		
		binding4.setExpression( new ScriptExpression("dimension[\"dimension2\"][\"level21\"]") );
		cqd.addBinding( binding4 );
		
		IBinding binding5 = new Binding( "measure1" );
		binding5.setExpression( new ScriptExpression("measure[\"measure1\"]") );
		cqd.addBinding( binding5 );
		
		IFilterDefinition filter = new FilterDefinition( new ConditionalExpression( "dimension[\"dimension1\"][\"level11\"]",
				IConditionalExpression.OP_EQ,
				"\"CN\"" ) );
		cqd.addFilter( filter );
	
		List columnEdgeBindingNames = new ArrayList();
		columnEdgeBindingNames.add( "edge1level1" );
		columnEdgeBindingNames.add( "edge1level2" );
	
		cqd.setCacheQueryResults( true );
		FileArchiveWriter writter = new FileArchiveWriter(documentPath + "testTemp" );
		DataEngine engine = DataEngine.newDataEngine( DataEngineContext.newInstance( DataEngineContext.MODE_GENERATION,
				null,
				null,
				writter ) );
		this.createCube( writter, engine );
		
		IPreparedCubeQuery pcq = engine.prepare( cqd, null );
		ICubeQueryResults queryResults = pcq.execute( null );
		CubeCursor cursor = queryResults.getCubeCursor( );
		
		
		//Load from cache.
		cqd.setQueryResultsID( queryResults.getID( ) );
		pcq = engine.prepare( cqd, null );
		queryResults = pcq.execute( null );
		cursor = queryResults.getCubeCursor( );
		
		writter.finish( );
		engine.shutdown( );
		
		//Load from RD
		FileArchiveReader reader = new FileArchiveReader(documentPath + "testTemp" );
		engine = DataEngine.newDataEngine( DataEngineContext.newInstance( DataEngineContext.MODE_PRESENTATION,
				null,
				reader,
				null ) );
		cqd.setQueryResultsID( queryResults.getID( ) );
		pcq = engine.prepare( cqd, null );
		queryResults = pcq.execute( null );
		cursor = queryResults.getCubeCursor( );

		this.printCube( cursor, columnEdgeBindingNames, "edge2level1", "measure1" );
	
	}
	
	/**
	 * Filter2, filter out all level11 = CN and level21 > 2000.
	 * @throws Exception
	 *//*
	public void testFilter2( ) throws Exception
	{
		this.createCube( );
		ICubeQueryDefinition cqd = new CubeQueryDefinition( cubeName);
		IEdgeDefinition columnEdge = cqd.createEdge( ICubeQueryDefinition.COLUMN_EDGE );
		IEdgeDefinition rowEdge = cqd.createEdge( ICubeQueryDefinition.ROW_EDGE );
		IDimensionDefinition dim1 = columnEdge.createDimension( "dimension1" );
		IHierarchyDefinition hier1 = dim1.createHierarchy( "dimension1" );
		hier1.createLevel( "level11" );
		hier1.createLevel( "level12" );
				
		IDimensionDefinition dim2 = rowEdge.createDimension( "dimension2" );
		IHierarchyDefinition hier2 = dim2.createHierarchy( "dimension2" );
		hier2.createLevel( "level21" );
		
		cqd.createMeasure( "measure1" );
		
		IBinding binding1 = new Binding( "edge1level1");
		
		binding1.setExpression( new ScriptExpression("dimension[\"dimension1\"][\"level11\"]") );
		cqd.addBinding( binding1 );
		
		IBinding binding2 = new Binding( "edge1level2");
		
		binding2.setExpression( new ScriptExpression("dimension[\"dimension1\"][\"level12\"]") );
		cqd.addBinding( binding2 );
		
		IBinding binding4 = new Binding( "edge2level1");
		
		binding4.setExpression( new ScriptExpression("dimension[\"dimension2\"][\"level21\"]") );
		cqd.addBinding( binding4 );
		
		IBinding binding5 = new Binding( "measure1" );
		binding5.setExpression( new ScriptExpression("measure[\"measure1\"]") );
		cqd.addBinding( binding5 );
		
		IFilterDefinition filter1 = new FilterDefinition( new ConditionalExpression( "dimension[\"dimension1\"][\"level11\"]",
				IConditionalExpression.OP_EQ,
				"\"CN\"" ) );
		IFilterDefinition filter2 = new FilterDefinition( new ConditionalExpression( "dimension[\"dimension2\"][\"level21\"]",
				IConditionalExpression.OP_GE,
				"2000" ) );
		
		cqd.addFilter( filter1 );
		cqd.addFilter( filter2 );
		DataEngine engine = DataEngine.newDataEngine( DataEngineContext.newInstance( DataEngineContext.DIRECT_PRESENTATION,
				null,
				null,
				null ) );
		
		IPreparedCubeQuery pcq = engine.prepare( cqd, null );
		ICubeQueryResults queryResults = pcq.execute( null );
		CubeCursor cursor = queryResults.getCubeCursor( );
		List columnEdgeBindingNames = new ArrayList();
		columnEdgeBindingNames.add( "edge1level1" );
		columnEdgeBindingNames.add( "edge1level2" );
		
		this.printCube( cursor, columnEdgeBindingNames, "edge2level1", "measure1" );
	
	}
	*//**
	 * Simple sort on 1 level
	 * @throws Exception
	 *//*
	public void testSort1( ) throws Exception
	{
		this.createCube( );
		ICubeQueryDefinition cqd = new CubeQueryDefinition( cubeName);
		IEdgeDefinition columnEdge = cqd.createEdge( ICubeQueryDefinition.COLUMN_EDGE );
		IEdgeDefinition rowEdge = cqd.createEdge( ICubeQueryDefinition.ROW_EDGE );
		IDimensionDefinition dim1 = columnEdge.createDimension( "dimension1" );
		IHierarchyDefinition hier1 = dim1.createHierarchy( "dimension1" );
		hier1.createLevel( "level11" );
		hier1.createLevel( "level12" );
				
		IDimensionDefinition dim2 = rowEdge.createDimension( "dimension2" );
		IHierarchyDefinition hier2 = dim2.createHierarchy( "dimension2" );
		hier2.createLevel( "level21" );
		
		cqd.createMeasure( "measure1" );
		
		IBinding binding1 = new Binding( "edge1level1");
		
		binding1.setExpression( new ScriptExpression("dimension[\"dimension1\"][\"level11\"]") );
		cqd.addBinding( binding1 );
		
		IBinding binding2 = new Binding( "edge1level2");
		
		binding2.setExpression( new ScriptExpression("dimension[\"dimension1\"][\"level12\"]") );
		cqd.addBinding( binding2 );
		
		IBinding binding4 = new Binding( "edge2level1");
		
		binding4.setExpression( new ScriptExpression("dimension[\"dimension2\"][\"level21\"]") );
		cqd.addBinding( binding4 );
		
		IBinding binding5 = new Binding( "measure1" );
		binding5.setExpression( new ScriptExpression("measure[\"measure1\"]") );
		cqd.addBinding( binding5 );
		
		SortDefinition sorter = new SortDefinition();
		sorter.setExpression( "dimension[\"dimension2\"][\"level21\"]" );
		sorter.setSortDirection( ISortDefinition.SORT_DESC );
		cqd.addSort( sorter );
		DataEngine engine = DataEngine.newDataEngine( DataEngineContext.newInstance( DataEngineContext.DIRECT_PRESENTATION,
				null,
				null,
				null ) );
		
		IPreparedCubeQuery pcq = engine.prepare( cqd, null );
		ICubeQueryResults queryResults = pcq.execute( null );
		CubeCursor cursor = queryResults.getCubeCursor( );
		List columnEdgeBindingNames = new ArrayList();
		columnEdgeBindingNames.add( "edge1level1" );
		columnEdgeBindingNames.add( "edge1level2" );
		
		this.printCube( cursor, columnEdgeBindingNames, "edge2level1", "measure1" );
	
	}
	
	*//**
	 * Complex sort on multiple levels
	 * @throws Exception
	 *//*
	public void testSort2( ) throws Exception
	{
		this.createCube( );
		ICubeQueryDefinition cqd = new CubeQueryDefinition( cubeName);
		IEdgeDefinition columnEdge = cqd.createEdge( ICubeQueryDefinition.COLUMN_EDGE );
		IEdgeDefinition rowEdge = cqd.createEdge( ICubeQueryDefinition.ROW_EDGE );
		IDimensionDefinition dim1 = columnEdge.createDimension( "dimension1" );
		IHierarchyDefinition hier1 = dim1.createHierarchy( "dimension1" );
		hier1.createLevel( "level11" );
		hier1.createLevel( "level12" );
				
		IDimensionDefinition dim2 = rowEdge.createDimension( "dimension2" );
		IHierarchyDefinition hier2 = dim2.createHierarchy( "dimension2" );
		hier2.createLevel( "level21" );
		
		cqd.createMeasure( "measure1" );
		
		IBinding binding1 = new Binding( "edge1level1");
		
		binding1.setExpression( new ScriptExpression("dimension[\"dimension1\"][\"level11\"]") );
		cqd.addBinding( binding1 );
		
		IBinding binding2 = new Binding( "edge1level2");
		
		binding2.setExpression( new ScriptExpression("dimension[\"dimension1\"][\"level12\"]") );
		cqd.addBinding( binding2 );
		
		IBinding binding4 = new Binding( "edge2level1");
		
		binding4.setExpression( new ScriptExpression("dimension[\"dimension2\"][\"level21\"]") );
		cqd.addBinding( binding4 );
		
		IBinding binding5 = new Binding( "measure1" );
		binding5.setExpression( new ScriptExpression("measure[\"measure1\"]") );
		cqd.addBinding( binding5 );
		
		//sort on year
		SortDefinition sorter1 = new SortDefinition();
		sorter1.setExpression( "dimension[\"dimension2\"][\"level21\"]" );
		sorter1.setSortDirection( ISortDefinition.SORT_DESC );
		
		//sort on country
		SortDefinition sorter2 = new SortDefinition();
		sorter2.setExpression( "dimension[\"dimension1\"][\"level11\"]" );
		sorter2.setSortDirection( ISortDefinition.SORT_DESC );
		
		//sort on city.
		SortDefinition sorter3 = new SortDefinition();
		sorter3.setExpression( "dimension[\"dimension1\"][\"level12\"]" );
		sorter3.setSortDirection( ISortDefinition.SORT_DESC );
		
		cqd.addSort( sorter1 );
		cqd.addSort( sorter2 );
		cqd.addSort( sorter3);
		DataEngine engine = DataEngine.newDataEngine( DataEngineContext.newInstance( DataEngineContext.DIRECT_PRESENTATION,
				null,
				null,
				null ) );
		
		IPreparedCubeQuery pcq = engine.prepare( cqd, null );
		ICubeQueryResults queryResults = pcq.execute( null );
		CubeCursor cursor = queryResults.getCubeCursor( );
		List columnEdgeBindingNames = new ArrayList();
		columnEdgeBindingNames.add( "edge1level1" );
		columnEdgeBindingNames.add( "edge1level2" );
		
		this.printCube( cursor, columnEdgeBindingNames, "edge2level1", "measure1" );
	
	}
	
	*//**
	 * Test grand total
	 * @throws Exception
	 *//*
	public void testGrandTotal( ) throws Exception
	{
		this.createCube( );
		ICubeQueryDefinition cqd = new CubeQueryDefinition( cubeName);
		IEdgeDefinition columnEdge = cqd.createEdge( ICubeQueryDefinition.COLUMN_EDGE );
		IEdgeDefinition rowEdge = cqd.createEdge( ICubeQueryDefinition.ROW_EDGE );
		IDimensionDefinition dim1 = columnEdge.createDimension( "dimension1" );
		IHierarchyDefinition hier1 = dim1.createHierarchy( "dimension1" );
		hier1.createLevel( "level11" );
		hier1.createLevel( "level12" );
				
		IDimensionDefinition dim2 = rowEdge.createDimension( "dimension2" );
		IHierarchyDefinition hier2 = dim2.createHierarchy( "dimension2" );
		hier2.createLevel( "level21" );
		
		cqd.createMeasure( "measure1" );
		
		IBinding binding1 = new Binding( "edge1level1");
		
		binding1.setExpression( new ScriptExpression("dimension[\"dimension1\"][\"level11\"]") );
		cqd.addBinding( binding1 );
		
		IBinding binding2 = new Binding( "edge1level2");
		
		binding2.setExpression( new ScriptExpression("dimension[\"dimension1\"][\"level12\"]") );
		cqd.addBinding( binding2 );
		
		IBinding binding4 = new Binding( "edge2level1");
		
		binding4.setExpression( new ScriptExpression("dimension[\"dimension2\"][\"level21\"]") );
		cqd.addBinding( binding4 );
		
		IBinding binding5 = new Binding( "measure1" );
		binding5.setExpression( new ScriptExpression("measure[\"measure1\"]") );
		cqd.addBinding( binding5 );
		
		IBinding binding6 = new Binding( "rowGrandTotal");
		binding6.setExpression( new ScriptExpression("measure[\"measure1\"]") );
		binding6.setAggrFunction( BuiltInAggregationFactory.TOTAL_SUM_FUNC );
		binding6.addAggregateOn( "level21" );
		cqd.addBinding( binding6 );
		
		IBinding binding7 = new Binding( "columnGrandTotal");
		binding7.setExpression( new ScriptExpression("measure[\"measure1\"]") );
		binding7.setAggrFunction( BuiltInAggregationFactory.TOTAL_SUM_FUNC );
		binding7.addAggregateOn( "level11" );
		binding7.addAggregateOn( "level12" );
		cqd.addBinding( binding7 );
		
		IBinding binding8 = new Binding( "grandTotal");
		binding8.setExpression( new ScriptExpression("measure[\"measure1\"]") );
		binding8.setAggrFunction( BuiltInAggregationFactory.TOTAL_SUM_FUNC );
		cqd.addBinding( binding8 );
		
		//sort on year
		SortDefinition sorter1 = new SortDefinition();
		sorter1.setExpression( "dimension[\"dimension2\"][\"level21\"]" );
		sorter1.setSortDirection( ISortDefinition.SORT_DESC );
		
		//sort on country
		SortDefinition sorter2 = new SortDefinition();
		sorter2.setExpression( "dimension[\"dimension1\"][\"level11\"]" );
		sorter2.setSortDirection( ISortDefinition.SORT_DESC );
		
		//sort on city.
		SortDefinition sorter3 = new SortDefinition();
		sorter3.setExpression( "dimension[\"dimension1\"][\"level12\"]" );
		sorter3.setSortDirection( ISortDefinition.SORT_DESC );
		
		cqd.addSort( sorter1 );
		cqd.addSort( sorter2 );
		cqd.addSort( sorter3);
		DataEngine engine = DataEngine.newDataEngine( DataEngineContext.newInstance( DataEngineContext.DIRECT_PRESENTATION,
				null,
				null,
				null ) );
		
		IPreparedCubeQuery pcq = engine.prepare( cqd, null );
		ICubeQueryResults queryResults = pcq.execute( null );
		CubeCursor cursor = queryResults.getCubeCursor( );
		List columnEdgeBindingNames = new ArrayList();
		columnEdgeBindingNames.add( "edge1level1" );
		columnEdgeBindingNames.add( "edge1level2" );
		
		this.printCube( cursor, columnEdgeBindingNames, "edge2level1", "measure1", "columnGrandTotal", "rowGrandTotal", "grandTotal" );
	
	}
	
	*//**
	 * Test grand total
	 * @throws Exception
	 */
	public void testAggrSort( ) throws Exception
	{
		ICubeQueryDefinition cqd = new CubeQueryDefinition( cubeName);
		IEdgeDefinition columnEdge = cqd.createEdge( ICubeQueryDefinition.COLUMN_EDGE );
		IEdgeDefinition rowEdge = cqd.createEdge( ICubeQueryDefinition.ROW_EDGE );
		IDimensionDefinition dim1 = columnEdge.createDimension( "dimension1" );
		IHierarchyDefinition hier1 = dim1.createHierarchy( "dimension1" );
		ILevelDefinition level11 = hier1.createLevel( "level11" );
		ILevelDefinition level12 = hier1.createLevel( "level12" );
		ILevelDefinition level13 = hier1.createLevel( "level13" );
		IDimensionDefinition dim2 = rowEdge.createDimension( "dimension2" );
		IHierarchyDefinition hier2 = dim2.createHierarchy( "dimension2" );
		ILevelDefinition level21 = hier2.createLevel( "level21" );
		
		createSortTestBindings( cqd );
		
		//sort on year
		CubeSortDefinition sorter1 = new CubeSortDefinition();
		sorter1.setExpression( "data[\"rowGrandTotal\"]" );
		sorter1.setAxisQualifierLevels( null );
		sorter1.setAxisQualifierValues( null );
		sorter1.setTargetLevel( level21 );
		sorter1.setSortDirection( ISortDefinition.SORT_DESC );
		
		//sort on country
		CubeSortDefinition sorter2 = new CubeSortDefinition();
		sorter2.setExpression( "data[\"city_year_total\"]" );
		sorter2.setAxisQualifierLevels( new ILevelDefinition[]{level21} );
		sorter2.setAxisQualifierValues( new Object[]{"2002"} );
		sorter2.setTargetLevel( level12 );
		sorter2.setSortDirection( ISortDefinition.SORT_DESC );
		
		CubeSortDefinition sorter3 = new CubeSortDefinition();
		sorter3.setExpression( "data[\"country_year_total\"]" );
		sorter3.setAxisQualifierLevels( new ILevelDefinition[]{level21} );
		sorter3.setAxisQualifierValues( new Object[]{"2002"} );
		sorter3.setTargetLevel( level11 );
		sorter3.setSortDirection( ISortDefinition.SORT_DESC );
		
		CubeSortDefinition sorter4 = new CubeSortDefinition();
		sorter4.setExpression( "dimension[\"dimension1\"][\"level13\"]" );
		sorter4.setAxisQualifierLevels( null );
		sorter4.setAxisQualifierValues( null );
		sorter4.setTargetLevel( level13 );
		sorter4.setSortDirection( ISortDefinition.SORT_DESC );
		
		//Make UN before China.
		CubeSortDefinition sorter5 = new CubeSortDefinition();
		sorter5.setExpression( "dimension[\"dimension1\"][\"level11\"]" );
		sorter5.setAxisQualifierLevels( null );
		sorter5.setAxisQualifierValues( null );
		sorter5.setTargetLevel( level11 );
		sorter5.setSortDirection( ISortDefinition.SORT_DESC );
		
		cqd.addSort( sorter1 );
		cqd.addSort( sorter2 );
		cqd.addSort( sorter3 );
		cqd.addSort( sorter4 );
		cqd.addSort( sorter5 );

		cqd.setCacheQueryResults( true );
		FileArchiveWriter writter = new FileArchiveWriter(documentPath + "testTemp" );
		DataEngine engine = DataEngine.newDataEngine( DataEngineContext.newInstance( DataEngineContext.MODE_GENERATION,
				null,
				null,
				writter ) );
		this.createCube( writter, engine );
		
		IPreparedCubeQuery pcq = engine.prepare( cqd, null );
		ICubeQueryResults queryResults = pcq.execute( null );
		CubeCursor cursor = queryResults.getCubeCursor( );
		
		
		//Load from cache.
		cqd.setQueryResultsID( queryResults.getID( ) );
		pcq = engine.prepare( cqd, null );
		queryResults = pcq.execute( null );
		cursor = queryResults.getCubeCursor( );
		
		writter.finish( );
		engine.shutdown( );
		
		//Load from RD
		FileArchiveReader reader = new FileArchiveReader(documentPath + "testTemp" );
		engine = DataEngine.newDataEngine( DataEngineContext.newInstance( DataEngineContext.MODE_PRESENTATION,
				null,
				reader,
				null ) );
		cqd.setQueryResultsID( queryResults.getID( ) );
		pcq = engine.prepare( cqd, null );
		queryResults = pcq.execute( null );
		cursor = queryResults.getCubeCursor( );
		
		List columnEdgeBindingNames = new ArrayList();
		columnEdgeBindingNames.add( "edge1level1" );
		columnEdgeBindingNames.add( "edge1level2" );
		columnEdgeBindingNames.add( "edge1level3" );
		
		printCube( cursor, "country_year_total",
				"city_year_total", "dist_total", "city_total", "country_total","rowGrandTotal", "grandTotal",
				new String[] {"edge1level1","edge1level2","edge1level3"}, "edge2level1", "measure1" );
		
	
	}

	/**
	 * Test grand total
	 * @throws Exception
	 *//*
	public void testAggrSort1( ) throws Exception
	{
		this.createCube( );
		ICubeQueryDefinition cqd = new CubeQueryDefinition( cubeName);
		IEdgeDefinition columnEdge = cqd.createEdge( ICubeQueryDefinition.COLUMN_EDGE );
		IEdgeDefinition rowEdge = cqd.createEdge( ICubeQueryDefinition.ROW_EDGE );
		IDimensionDefinition dim1 = columnEdge.createDimension( "dimension1" );
		IHierarchyDefinition hier1 = dim1.createHierarchy( "dimension1" );
		hier1.createLevel( "level11" );
		hier1.createLevel( "level12" );
		hier1.createLevel( "level13" );
		IDimensionDefinition dim2 = rowEdge.createDimension( "dimension2" );
		IHierarchyDefinition hier2 = dim2.createHierarchy( "dimension2" );
		ILevelDefinition level21 = hier2.createLevel( "level21" );
		
		createSortTestBindings( cqd );
		
		//sort on year
		CubeSortDefinition sorter1 = new CubeSortDefinition();
		sorter1.setExpression( "data[\"rowGrandTotal\"]" );
		sorter1.setAxisQualifierLevel( null );
		sorter1.setAxisQualifierValue( null );
		sorter1.setTargetLevel( level21 );
		sorter1.setSortDirection( ISortDefinition.SORT_DESC );
	
		cqd.addSort( sorter1 );
		DataEngine engine = DataEngine.newDataEngine( DataEngineContext.newInstance( DataEngineContext.DIRECT_PRESENTATION,
				null,
				null,
				null ) );
		
		IPreparedCubeQuery pcq = engine.prepare( cqd, null );
		ICubeQueryResults queryResults = pcq.execute( null );
		CubeCursor cursor = queryResults.getCubeCursor( );
		List columnEdgeBindingNames = new ArrayList();
		columnEdgeBindingNames.add( "edge1level1" );
		columnEdgeBindingNames.add( "edge1level2" );
		columnEdgeBindingNames.add( "edge1level3" );
		
		printCube( cursor, "country_year_total",
				"city_year_total", "dist_total", "city_total", "country_total","rowGrandTotal", "grandTotal",
				new String[] {"edge1level1","edge1level2","edge1level3"}, "edge2level1", "measure1" );
		
	
	}
	
	*//**
	 * Test grand total
	 * @throws Exception
	 *//*
	public void testAggrSort2( ) throws Exception
	{
		this.createCube( );
		ICubeQueryDefinition cqd = new CubeQueryDefinition( cubeName);
		IEdgeDefinition columnEdge = cqd.createEdge( ICubeQueryDefinition.COLUMN_EDGE );
		IEdgeDefinition rowEdge = cqd.createEdge( ICubeQueryDefinition.ROW_EDGE );
		IDimensionDefinition dim1 = columnEdge.createDimension( "dimension1" );
		IHierarchyDefinition hier1 = dim1.createHierarchy( "dimension1" );
		hier1.createLevel( "level11" );
		ILevelDefinition level12 = hier1.createLevel( "level12" );
		hier1.createLevel( "level13" );
		IDimensionDefinition dim2 = rowEdge.createDimension( "dimension2" );
		IHierarchyDefinition hier2 = dim2.createHierarchy( "dimension2" );
		ILevelDefinition level21 = hier2.createLevel( "level21" );
		
		createSortTestBindings( cqd );
		
		//sort on year
		CubeSortDefinition sorter1 = new CubeSortDefinition();
		sorter1.setExpression( "data[\"rowGrandTotal\"]" );
		sorter1.setAxisQualifierLevel( null );
		sorter1.setAxisQualifierValue( null );
		sorter1.setTargetLevel( level21 );
		sorter1.setSortDirection( ISortDefinition.SORT_DESC );
		
		//sort on country
		CubeSortDefinition sorter2 = new CubeSortDefinition();
		sorter2.setExpression( "data[\"city_year_total\"]" );
		sorter2.setAxisQualifierLevel( new ILevelDefinition[]{level21} );
		sorter2.setAxisQualifierValue( new Object[]{"2002"} );
		sorter2.setTargetLevel( level12 );
		sorter2.setSortDirection( ISortDefinition.SORT_DESC );
		
		cqd.addSort( sorter1 );
		cqd.addSort( sorter2 );
		DataEngine engine = DataEngine.newDataEngine( DataEngineContext.newInstance( DataEngineContext.DIRECT_PRESENTATION,
				null,
				null,
				null ) );
		
		IPreparedCubeQuery pcq = engine.prepare( cqd, null );
		ICubeQueryResults queryResults = pcq.execute( null );
		CubeCursor cursor = queryResults.getCubeCursor( );
		List columnEdgeBindingNames = new ArrayList();
		columnEdgeBindingNames.add( "edge1level1" );
		columnEdgeBindingNames.add( "edge1level2" );
		columnEdgeBindingNames.add( "edge1level3" );
		
		printCube( cursor, "country_year_total",
				"city_year_total", "dist_total", "city_total", "country_total","rowGrandTotal", "grandTotal",
				new String[] {"edge1level1","edge1level2","edge1level3"}, "edge2level1", "measure1" );
		
	
	}
	
	*//**
	 * Test grand total
	 * @throws Exception
	 *//*
	public void testAggrSort3( ) throws Exception
	{
		this.createCube( );
		ICubeQueryDefinition cqd = new CubeQueryDefinition( cubeName);
		IEdgeDefinition columnEdge = cqd.createEdge( ICubeQueryDefinition.COLUMN_EDGE );
		IEdgeDefinition rowEdge = cqd.createEdge( ICubeQueryDefinition.ROW_EDGE );
		IDimensionDefinition dim1 = columnEdge.createDimension( "dimension1" );
		IHierarchyDefinition hier1 = dim1.createHierarchy( "dimension1" );
		ILevelDefinition level11 = hier1.createLevel( "level11" );
		ILevelDefinition level12 = hier1.createLevel( "level12" );
		hier1.createLevel( "level13" );
		IDimensionDefinition dim2 = rowEdge.createDimension( "dimension2" );
		IHierarchyDefinition hier2 = dim2.createHierarchy( "dimension2" );
		ILevelDefinition level21 = hier2.createLevel( "level21" );
		
		createSortTestBindings( cqd );
		
		//sort on year
		CubeSortDefinition sorter1 = new CubeSortDefinition();
		sorter1.setExpression( "data[\"rowGrandTotal\"]" );
		sorter1.setAxisQualifierLevel( null );
		sorter1.setAxisQualifierValue( null );
		sorter1.setTargetLevel( level21 );
		sorter1.setSortDirection( ISortDefinition.SORT_DESC );
		
		//sort on country
		CubeSortDefinition sorter2 = new CubeSortDefinition();
		sorter2.setExpression( "data[\"city_year_total\"]" );
		sorter2.setAxisQualifierLevel( new ILevelDefinition[]{level21} );
		sorter2.setAxisQualifierValue( new Object[]{"2002"} );
		sorter2.setTargetLevel( level12 );
		sorter2.setSortDirection( ISortDefinition.SORT_DESC );
		
		CubeSortDefinition sorter3 = new CubeSortDefinition();
		sorter3.setExpression( "data[\"country_year_total\"]" );
		sorter3.setAxisQualifierLevel( new ILevelDefinition[]{level21} );
		sorter3.setAxisQualifierValue( new Object[]{"2002"} );
		sorter3.setTargetLevel( level11 );
		sorter3.setSortDirection( ISortDefinition.SORT_DESC );
	
		cqd.addSort( sorter1 );
		cqd.addSort( sorter2 );
		cqd.addSort( sorter3 );
		DataEngine engine = DataEngine.newDataEngine( DataEngineContext.newInstance( DataEngineContext.DIRECT_PRESENTATION,
				null,
				null,
				null ) );
		
		IPreparedCubeQuery pcq = engine.prepare( cqd, null );
		ICubeQueryResults queryResults = pcq.execute( null );
		CubeCursor cursor = queryResults.getCubeCursor( );
		List columnEdgeBindingNames = new ArrayList();
		columnEdgeBindingNames.add( "edge1level1" );
		columnEdgeBindingNames.add( "edge1level2" );
		columnEdgeBindingNames.add( "edge1level3" );
		
		printCube( cursor, "country_year_total",
				"city_year_total", "dist_total", "city_total", "country_total","rowGrandTotal", "grandTotal",
				new String[] {"edge1level1","edge1level2","edge1level3"}, "edge2level1", "measure1" );
		
	
	}
	
	*//**
	 * Test grand total
	 * @throws Exception
	 *//*
	public void testAggrSort5( ) throws Exception
	{
		this.createCube( );
		ICubeQueryDefinition cqd = new CubeQueryDefinition( cubeName);
		IEdgeDefinition columnEdge = cqd.createEdge( ICubeQueryDefinition.COLUMN_EDGE );
		IEdgeDefinition rowEdge = cqd.createEdge( ICubeQueryDefinition.ROW_EDGE );
		IDimensionDefinition dim1 = columnEdge.createDimension( "dimension1" );
		IHierarchyDefinition hier1 = dim1.createHierarchy( "dimension1" );
		hier1.createLevel( "level11" );
		hier1.createLevel( "level12" );
		ILevelDefinition level13 = hier1.createLevel( "level13" );
		IDimensionDefinition dim2 = rowEdge.createDimension( "dimension2" );
		IHierarchyDefinition hier2 = dim2.createHierarchy( "dimension2" );
		ILevelDefinition level21 = hier2.createLevel( "level21" );
		
		createSortTestBindings( cqd );
		
		//sort on year
		CubeSortDefinition sorter1 = new CubeSortDefinition();
		sorter1.setExpression( "data[\"measure1\"]" );
		sorter1.setAxisQualifierLevel( new ILevelDefinition[]{level21} );
		sorter1.setAxisQualifierValue( new Object[]{"1998"} );
		sorter1.setTargetLevel( level13 );
		sorter1.setSortDirection( ISortDefinition.SORT_DESC );
	
		cqd.addSort( sorter1 );
		DataEngine engine = DataEngine.newDataEngine( DataEngineContext.newInstance( DataEngineContext.DIRECT_PRESENTATION,
				null,
				null,
				null ) );
		
		IPreparedCubeQuery pcq = engine.prepare( cqd, null );
		ICubeQueryResults queryResults = pcq.execute( null );
		CubeCursor cursor = queryResults.getCubeCursor( );
		List columnEdgeBindingNames = new ArrayList();
		columnEdgeBindingNames.add( "edge1level1" );
		columnEdgeBindingNames.add( "edge1level2" );
		columnEdgeBindingNames.add( "edge1level3" );
		
		printCube( cursor, "country_year_total",
				"city_year_total", "dist_total", "city_total", "country_total","rowGrandTotal", "grandTotal",
				new String[] {"edge1level1","edge1level2","edge1level3"}, "edge2level1", "measure1" );
		
	
	} */
	private void createSortTestBindings( ICubeQueryDefinition cqd )
			throws DataException
	{
		cqd.createMeasure( "measure1" );
		
		IBinding binding1 = new Binding( "edge1level1");
		
		binding1.setExpression( new ScriptExpression("dimension[\"dimension1\"][\"level11\"]") );
		cqd.addBinding( binding1 );
		
		IBinding binding2 = new Binding( "edge1level2");
		
		binding2.setExpression( new ScriptExpression("dimension[\"dimension1\"][\"level12\"]") );
		cqd.addBinding( binding2 );
		
		IBinding binding3 = new Binding( "edge1level3");
		
		binding3.setExpression( new ScriptExpression("dimension[\"dimension1\"][\"level13\"]") );
		cqd.addBinding( binding3 );
		
		IBinding binding4 = new Binding( "edge2level1");
		
		binding4.setExpression( new ScriptExpression("dimension[\"dimension2\"][\"level21\"]") );
		cqd.addBinding( binding4 );
		
		IBinding binding5 = new Binding( "measure1" );
		binding5.setExpression( new ScriptExpression("measure[\"measure1\"]") );
		/*binding5.setAggrFunction( IBuildInAggregation.TOTAL_SUM_FUNC );
		binding5.addAggregateOn( "dimension[\"dimension2\"][\"level21\"]" );
		binding5.addAggregateOn( "dimension[\"dimension1\"][\"level11\"]" );
		binding5.addAggregateOn( "dimension[\"dimension1\"][\"level12\"]" );
		binding5.addAggregateOn( "dimension[\"dimension1\"][\"level13\"]" );*/
		
		cqd.addBinding( binding5 );
		
		IBinding binding6 = new Binding( "rowGrandTotal");
		binding6.setExpression( new ScriptExpression("measure[\"measure1\"]") );
		binding6.setAggrFunction( IBuildInAggregation.TOTAL_SUM_FUNC );
		binding6.addAggregateOn( "dimension[\"dimension2\"][\"level21\"]" );
		cqd.addBinding( binding6 );
		
		IBinding binding7 = new Binding( "columnGrandTotal");
		binding7.setExpression( new ScriptExpression("measure[\"measure1\"]") );
		binding7.setAggrFunction( IBuildInAggregation.TOTAL_SUM_FUNC );
		binding7.addAggregateOn( "dimension[\"dimension1\"][\"level11\"]" );
		binding7.addAggregateOn( "dimension[\"dimension1\"][\"level12\"]" );
		cqd.addBinding( binding7 );
		
		IBinding binding8 = new Binding( "grandTotal");
		binding8.setExpression( new ScriptExpression("measure[\"measure1\"]") );
		binding8.setAggrFunction( IBuildInAggregation.TOTAL_SUM_FUNC );
		cqd.addBinding( binding8 );
		
		IBinding binding9 = new Binding( "country_year_total");
		binding9.setExpression( new ScriptExpression("measure[\"measure1\"]") );
		binding9.setAggrFunction( IBuildInAggregation.TOTAL_SUM_FUNC );
		binding9.addAggregateOn( "dimension[\"dimension1\"][\"level11\"]" );
		binding9.addAggregateOn( "dimension[\"dimension2\"][\"level21\"]" );
		cqd.addBinding( binding9 );
		
		IBinding binding10= new Binding( "city_year_total");
		binding10.setExpression( new ScriptExpression("measure[\"measure1\"]") );
		binding10.setAggrFunction( IBuildInAggregation.TOTAL_SUM_FUNC );
		binding10.addAggregateOn( "dimension[\"dimension1\"][\"level11\"]" );
		binding10.addAggregateOn( "dimension[\"dimension1\"][\"level12\"]" );
		binding10.addAggregateOn( "dimension[\"dimension2\"][\"level21\"]" );
		cqd.addBinding( binding10 );
		
		IBinding binding11= new Binding( "dist_total");
		binding11.setExpression( new ScriptExpression("measure[\"measure1\"]") );
		binding11.setAggrFunction( IBuildInAggregation.TOTAL_SUM_FUNC );
		binding11.addAggregateOn( "dimension[\"dimension1\"][\"level11\"]" );
		binding11.addAggregateOn( "dimension[\"dimension1\"][\"level12\"]" );
		binding11.addAggregateOn( "dimension[\"dimension1\"][\"level13\"]" );
		
		cqd.addBinding( binding11 );
		
		IBinding binding12= new Binding( "city_total");
		binding12.setExpression( new ScriptExpression("measure[\"measure1\"]") );
		binding12.setAggrFunction( IBuildInAggregation.TOTAL_SUM_FUNC );
		binding12.addAggregateOn( "dimension[\"dimension1\"][\"level11\"]" );
		binding12.addAggregateOn( "dimension[\"dimension1\"][\"level12\"]" );
		
		
		cqd.addBinding( binding12 );
		
		IBinding binding13= new Binding( "country_total");
		binding13.setExpression( new ScriptExpression("measure[\"measure1\"]") );
		binding13.setAggrFunction( IBuildInAggregation.TOTAL_SUM_FUNC );
		binding13.addAggregateOn( "dimension[\"dimension1\"][\"level11\"]" );
		
		cqd.addBinding( binding13 );
	}
	
	
	/**
	 * Test binding "row" reference
	 * @throws Exception
	 *//*
	public void testBindingRowReference( ) throws Exception
	{
		this.createCube( );
		ICubeQueryDefinition cqd = new CubeQueryDefinition( cubeName);
		IEdgeDefinition columnEdge = cqd.createEdge( ICubeQueryDefinition.COLUMN_EDGE );
		IEdgeDefinition rowEdge = cqd.createEdge( ICubeQueryDefinition.ROW_EDGE );
		IDimensionDefinition dim1 = columnEdge.createDimension( "dimension1" );
		IHierarchyDefinition hier1 = dim1.createHierarchy( "dimension1" );
		hier1.createLevel( "level11" );
		hier1.createLevel( "level12" );
				
		IDimensionDefinition dim2 = rowEdge.createDimension( "dimension2" );
		IHierarchyDefinition hier2 = dim2.createHierarchy( "dimension2" );
		hier2.createLevel( "level21" );
		
		cqd.createMeasure( "measure1" );
		
		IBinding binding1 = new Binding( "edge1level1");
		
		binding1.setExpression( new ScriptExpression("dimension[\"dimension1\"][\"level11\"]") );
		cqd.addBinding( binding1 );
		
		IBinding binding2 = new Binding( "edge1level2");
		
		binding2.setExpression( new ScriptExpression("dimension[\"dimension1\"][\"level12\"]") );
		cqd.addBinding( binding2 );
		
		IBinding binding4 = new Binding( "edge2level1");
		
		binding4.setExpression( new ScriptExpression("dimension[\"dimension2\"][\"level21\"]") );
		cqd.addBinding( binding4 );
		
		IBinding binding5 = new Binding( "measure1" );
		binding5.setExpression( new ScriptExpression("measure[\"measure1\"]") );
		cqd.addBinding( binding5 );
		
		IBinding binding6 = new Binding( "rowGrandTotal");
		binding6.setExpression( new ScriptExpression("measure[\"measure1\"]") );
		binding6.setAggrFunction( BuiltInAggregationFactory.TOTAL_SUM_FUNC );
		binding6.addAggregateOn( "level21" );
		cqd.addBinding( binding6 );
		
		IBinding binding7 = new Binding( "columnGrandTotal");
		binding7.setExpression( new ScriptExpression("measure[\"measure1\"]") );
		binding7.setAggrFunction( BuiltInAggregationFactory.TOTAL_SUM_FUNC );
		binding7.addAggregateOn( "level11" );
		binding7.addAggregateOn( "level12" );
		cqd.addBinding( binding7 );
		
		IBinding binding8 = new Binding( "grandTotal");
		binding8.setExpression( new ScriptExpression("measure[\"measure1\"]") );
		binding8.setAggrFunction( BuiltInAggregationFactory.TOTAL_SUM_FUNC );
		cqd.addBinding( binding8 );
		
		IBinding binding9 = new Binding( "row_rowGrandTotal");
		binding9.setExpression( new ScriptExpression("data[\"rowGrandTotal\"]*10") );
		cqd.addBinding( binding9 );
		
		IBinding binding10 = new Binding( "row_columnGrandTotal");
		binding10.setExpression( new ScriptExpression("data[\"columnGrandTotal\"]*10") );
		cqd.addBinding( binding10 );
		
		IBinding binding11 = new Binding( "row_grandTotal");
		binding11.setExpression( new ScriptExpression("data[\"grandTotal\"]*10") );
		cqd.addBinding( binding11 );
	
		IBinding binding12 = new Binding( "row_measure1" );
		binding12.setExpression( new ScriptExpression("data[\"measure1\"]*10") );
		cqd.addBinding( binding12 );
				
		//sort on year
		SortDefinition sorter1 = new SortDefinition();
		sorter1.setExpression( "dimension[\"dimension2\"][\"level21\"]" );
		sorter1.setSortDirection( ISortDefinition.SORT_DESC );
		
		//sort on country
		SortDefinition sorter2 = new SortDefinition();
		sorter2.setExpression( "dimension[\"dimension1\"][\"level11\"]" );
		sorter2.setSortDirection( ISortDefinition.SORT_DESC );
		
		//sort on city.
		SortDefinition sorter3 = new SortDefinition();
		sorter3.setExpression( "dimension[\"dimension1\"][\"level12\"]" );
		sorter3.setSortDirection( ISortDefinition.SORT_DESC );
		
		cqd.addSort( sorter1 );
		cqd.addSort( sorter2 );
		cqd.addSort( sorter3);
		DataEngine engine = DataEngine.newDataEngine( DataEngineContext.newInstance( DataEngineContext.DIRECT_PRESENTATION,
				null,
				null,
				null ) );
		
		IPreparedCubeQuery pcq = engine.prepare( cqd, null );
		ICubeQueryResults queryResults = pcq.execute( null );
		CubeCursor cursor = queryResults.getCubeCursor( );
		List columnEdgeBindingNames = new ArrayList();
		columnEdgeBindingNames.add( "edge1level1" );
		columnEdgeBindingNames.add( "edge1level2" );
		
		this.printCube( cursor,
				columnEdgeBindingNames,
				"edge2level1",
				"row_measure1",
				"row_columnGrandTotal",
				"row_rowGrandTotal",
				"row_grandTotal" );
	
	}
*/
	private void printCube( CubeCursor cursor, String country_year_total,
			String city_year_total, String dist_total, String city_total,
			String country_total, String year_total, String grand_total,
			String[] columns, String row, String measure ) throws OLAPException, IOException
	{
		EdgeCursor edge1 = (EdgeCursor) (cursor.getOrdinateEdge( ).get( 0 ));
		EdgeCursor edge2 = (EdgeCursor) (cursor.getOrdinateEdge( ).get( 1 ));
		
		String[] lines = new String[ edge1.getDimensionCursor( ).size( ) ];
		for ( int i = 0; i < lines.length; i++ )
		{
			lines[i] = "		";
		}
		
		
		while( edge1.next( ) )
		{
			long countryEnd = ((DimensionCursor)edge1.getDimensionCursor( ).get( 0 )).getEdgeEnd( );
			long cityEnd = ((DimensionCursor)edge1.getDimensionCursor( ).get( 1 )).getEdgeEnd( );
			
			lines[0] += cursor.getObject( columns[0] ) + "		";
			lines[1] += cursor.getObject( columns[1] ) + "		";
			lines[2] += cursor.getObject( columns[2] ) + "		";

			if ( cityEnd == edge1.getPosition( ) )
			{
				lines[0] += cursor.getObject( columns[0] ) + "		";
				lines[1] += cursor.getObject( columns[1] ) + "		";
				lines[2] += "[Total]" + "		";
			}
			
			if ( countryEnd == edge1.getPosition( ))
			{
				lines[0] += cursor.getObject( columns[0] ) + "		";
				lines[1] += "[Total]" + "		";
				lines[2] += "  " + "  		";
			}	
		}	
		lines[0] += "[Total]";
		String output = "";
		for ( int i = 0; i < lines.length; i++ )
		{
			output += "\n" + lines[i];
		}

		while ( edge2.next( ) )
		{
			String line = cursor.getObject( row ).toString( ) + "		";
			edge1.beforeFirst( );
			while ( edge1.next( ) )
			{
				long countryEnd = ( (DimensionCursor) edge1.getDimensionCursor( )
						.get( 0 ) ).getEdgeEnd( );
				long cityEnd = ( (DimensionCursor) edge1.getDimensionCursor( )
						.get( 1 ) ).getEdgeEnd( );

				line += cursor.getObject( measure ) + "		";

				if ( cityEnd == edge1.getPosition( ) )
					line += "["+cursor.getObject( city_year_total )+"]" + "		";
				
				if ( countryEnd == edge1.getPosition( ) )
					line += "["+cursor.getObject( country_year_total )+"]" + "		";
				
			}
			
			line += "["+cursor.getObject( year_total )+"]";
			output += "\n" + line;
		}
		
		String line = "[Total]" + "		";
		edge1.beforeFirst( );
		while ( edge1.next( ) )
		{
			long countryEnd = ( (DimensionCursor) edge1.getDimensionCursor( )
					.get( 0 ) ).getEdgeEnd( );
			long cityEnd = ( (DimensionCursor) edge1.getDimensionCursor( )
					.get( 1 ) ).getEdgeEnd( );

			line += cursor.getObject( dist_total ) + "		";

			if ( cityEnd == edge1.getPosition( ) )
				line += "["+cursor.getObject( city_total )+"]" + "		";
			
			if ( countryEnd == edge1.getPosition( ) )
				line += "["+cursor.getObject( country_total )+"]" + "		";
			
		}
		line += "["+cursor.getObject( grand_total )+"]" + "		";
		output += "\n" + line;
		this.testPrint( output );

		this.checkOutputFile( );
	}
	private void printCube( CubeCursor cursor, List columnEdgeBindingNames, String rowEdgeBindingNames, String measureBindingNames ) throws Exception
	{
		this.printCube( cursor, columnEdgeBindingNames, rowEdgeBindingNames, measureBindingNames, null, null, null );
	}
	
	private void printCube( CubeCursor cursor, List columnEdgeBindingNames, String rowEdgeBindingNames, String measureBindingNames, String columnAggr, String rowAggr, String overallAggr ) throws Exception
	{
		EdgeCursor edge1 = (EdgeCursor) (cursor.getOrdinateEdge( ).get( 0 ));
		EdgeCursor edge2 = (EdgeCursor) (cursor.getOrdinateEdge( ).get( 1 ));

		String[] lines = new String[ edge1.getDimensionCursor( ).size( ) ];
		for ( int i = 0; i < lines.length; i++ )
		{
			lines[i] = "		";
		}
		
		while( edge1.next( ) )
		{
			for ( int i = 0; i < lines.length; i++ )
			{
				lines[i] += cursor.getObject( columnEdgeBindingNames.get( i ).toString( ) ) + "		";
			}
		}
		
		if ( rowAggr != null )
			lines[lines.length-1] += "Total";
		
		String output = "";
		for ( int i = 0; i < lines.length; i++ )
		{
			output+="\n" + lines[i] ;
		}
		
		while ( edge2.next( ) )
		{
			String line = cursor.getObject( rowEdgeBindingNames ).toString( )  + "		";
			edge1.beforeFirst( );
			while ( edge1.next( ) )
			{ 
				line+= cursor.getObject( measureBindingNames ) + "		";
			}
			
			if ( rowAggr!= null )
				line+= cursor.getObject( rowAggr );
			output+="\n" + line;
		}
		
		if ( columnAggr!= null )
		{
			String line = "Total" + "		";
			edge1.beforeFirst( );
			while ( edge1.next( ) )
			{ 
				line+= cursor.getObject( columnAggr ) + "		";
			}
			if ( overallAggr != null )
				line+= cursor.getObject( overallAggr );
			
			output+="\n" + line;
		}
		this.testPrint( output );

		this.checkOutputFile( );
	}
	
	private void createCube( IDocArchiveWriter writter, DataEngine engine ) throws BirtException, IOException
	{
		CubeMaterializer cubeMaterializer = new CubeMaterializer( documentPath+engine.hashCode( ),
				"cube" );
		
		IDocumentManager documentManager = cubeMaterializer.getDocumentManager( );

		Dimension[] dimensions = new Dimension[2];

		// dimension0
		String[] levelNames = new String[3];
		levelNames[0] = "level11";
		levelNames[1] = "level12";
		levelNames[2] = "level13";
		DimensionForTest iterator = new DimensionForTest( levelNames );
		iterator.setLevelMember( 0, TestFactTable.DIM0_L1Col );
		iterator.setLevelMember( 1, TestFactTable.DIM0_L2Col );
		iterator.setLevelMember( 2, TestFactTable.DIM0_L3Col );

		ILevelDefn[] levelDefs = new ILevelDefn[3];
		levelDefs[0] = new LevelDefinition( "level11", new String[]{
			"level11"
		}, null );
		levelDefs[1] = new LevelDefinition( "level12", new String[]{
			"level12"
		}, null );
		levelDefs[2] = new LevelDefinition( "level13", new String[]{
			"level13"
		}, null );
		dimensions[0] = (Dimension) DimensionFactory.createDimension( "dimension1",
				documentManager,
				iterator,
				levelDefs,
				false );
		IHierarchy hierarchy = dimensions[0].getHierarchy( );
		assertEquals( hierarchy.getName( ), "dimension1" );
		assertEquals( dimensions[0].length( ), 13 );

		// dimension1
		levelNames = new String[1];
		levelNames[0] = "level21";
		iterator = new DimensionForTest( levelNames );
		iterator.setLevelMember( 0, distinct( TestFactTable.DIM1_L1Col ) );

		levelDefs = new ILevelDefn[1];
		levelDefs[0] = new LevelDefinition( "level21", new String[]{
			"level21"
		}, null );
		dimensions[1] = (Dimension) DimensionFactory.createDimension( "dimension2",
				documentManager,
				iterator,
				levelDefs,
				false );
		hierarchy = dimensions[1].getHierarchy( );
		assertEquals( hierarchy.getName( ), "dimension2" );
		assertEquals( dimensions[1].length( ), 5 );

		TestFactTable factTable2 = new TestFactTable( );
		String[] measureColumnName = new String[1];
		measureColumnName[0] = "measure1";
		Cube cube = new Cube( cubeName, documentManager );

		cube.create( CubeUtility.getKeyColNames(dimensions), dimensions, factTable2, measureColumnName, new StopSign( ) );
		
		cube.close( );
		documentManager.flush( );
		cubeMaterializer.saveCubeToReportDocument( "cube",
				writter,
				null );
		cubeMaterializer.close( );
	}
	
	private String[] distinct( String[] values )
	{
		String[] lValues = new String[values.length];
		System.arraycopy( values, 0, lValues, 0, values.length );
		Arrays.sort( lValues );
		List tempList = new ArrayList( );
		tempList.add( lValues[0] );
		for ( int i = 1; i < lValues.length; i++ )
		{
			if ( !lValues[i].equals( lValues[i - 1] ) )
			{
				tempList.add( lValues[i] );
			}
		}
		String[] result = new String[tempList.size( )];
		for ( int i = 0; i < result.length; i++ )
		{
			result[i] = ((String)tempList.get( i ));
		}
		return result;
	}
	
}
