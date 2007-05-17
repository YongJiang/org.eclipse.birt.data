
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
package org.eclipse.birt.data.engine.olap.data.api.cube;

import java.io.EOFException;
import java.io.IOException;

import org.eclipse.birt.core.archive.IDocArchiveWriter;
import org.eclipse.birt.core.archive.RAOutputStream;
import org.eclipse.birt.core.exception.BirtException;
import org.eclipse.birt.data.engine.core.DataException;
import org.eclipse.birt.data.engine.olap.data.api.ILevel;
import org.eclipse.birt.data.engine.olap.data.document.DocumentManagerFactory;
import org.eclipse.birt.data.engine.olap.data.document.IDocumentManager;
import org.eclipse.birt.data.engine.olap.data.document.IDocumentObject;
import org.eclipse.birt.data.engine.olap.data.impl.Cube;
import org.eclipse.birt.data.engine.olap.data.impl.NamingUtil;
import org.eclipse.birt.data.engine.olap.data.impl.dimension.Dimension;
import org.eclipse.birt.data.engine.olap.data.impl.dimension.Hierarchy;

/**
 * 
 */

public class CubeMaterializer
{
	private IDocumentManager documentManager;
	
	/**
	 * 
	 * @param pathName
	 * @param managerName
	 * @throws BirtOlapException
	 * @throws IOException
	 */
	public CubeMaterializer( String pathName, String managerName  ) throws DataException, IOException
	{
		documentManager = DocumentManagerFactory.createFileDocumentManager( pathName, managerName );
	}

	/**
	 * 
	 * @throws DataException
	 * @throws IOException
	 */
	public CubeMaterializer(  ) throws DataException, IOException
	{
		documentManager = DocumentManagerFactory.createFileDocumentManager( );
	}
	
	/**
	 * 
	 * @return
	 */
	public IDocumentManager getDocumentManager( )
	{
		return documentManager;
	}
	
	/**
	 * @param dimensionName
	 * @param hierarchyName
	 * @param iterator
	 * @param levelDefs
	 * @return
	 * @throws IOException
	 * @throws BirtException
	 */
	public IHierarchy createHierarchy( String dimensionName, String hierarchyName, IDatasetIterator iterator, ILevelDefn[] levelDefs ) throws IOException, BirtException
	{
		Hierarchy hierarchy = new Hierarchy( documentManager, dimensionName, hierarchyName ); 
		hierarchy.createAndSaveHierarchy( 
				iterator,
				levelDefs );
		return hierarchy;
	}
	
	/**
	 * 
	 * @param name
	 * @param hierarchy
	 * @return
	 * @throws BirtException
	 * @throws IOException
	 */
	public IDimension createDimension( String name, IHierarchy hierarchy ) throws BirtException, IOException
	{
		if (hierarchy instanceof Hierarchy) {
			return new Dimension(name, documentManager, (Hierarchy) hierarchy,
					false);
		} else {
			return null;
		}
	}
	
	/**
	 * 
	 * @param name
	 * @param dimensions
	 * @param factTable
	 * @param measureColumns
	 * @param stopSign
	 * @return
	 * @throws IOException
	 * @throws BirtException
	 */
	public void createCube( String name, IDimension[] dimensions, IDatasetIterator factTable, String[] measureColumns, StopSign stopSign ) throws IOException, BirtException
	{
		if ( stopSign == null )
		{
			stopSign = new StopSign( );
		}
		Cube cube = new Cube( name, documentManager );
		cube.create( dimensions, factTable, measureColumns, stopSign );
		cube.close( );
		documentManager.flush( );
	}
	
	/**
	 * 
	 * @param cubeName
	 * @param writer
	 * @throws IOException 
	 * @throws DataException 
	 */
	public void saveCubeToReportDocument( String cubeName, IDocArchiveWriter writer, StopSign stopSign ) throws IOException, DataException
	{
		if ( stopSign == null )
		{
			stopSign = new StopSign( );
		}
		Cube cube = new Cube( cubeName, documentManager );
		cube.load( stopSign );
		//save cube
		saveDocObjToReportDocument( NamingUtil.getCubeDocName( cubeName ), writer, stopSign );
		//save facttable
		String factTableName = cube.getFactTable( ).getName( );
		saveDocObjToReportDocument( NamingUtil.getFactTableName( factTableName ), writer, stopSign );
		saveDocObjToReportDocument( NamingUtil.getFTSUListName( factTableName ), writer, stopSign );
		//save FTSU
		IDocumentObject documentObject = documentManager.openDocumentObject( NamingUtil.getFTSUListName( factTableName ) );
		try
		{
			String FTSUName = documentObject.readString( );
			while ( FTSUName != null )
			{
				saveDocObjToReportDocument( FTSUName, writer, stopSign );
				FTSUName = documentObject.readString( );
			}
		}
		catch ( EOFException e )
		{

		}
		//save dimension
		IDimension[] dimensions = cube.getDimesions( );
		for ( int i = 0; i < dimensions.length; i++ )
		{
			saveDocObjToReportDocument( NamingUtil.getDimensionDocName( dimensions[i].getName( ) ), writer, stopSign );
			IHierarchy hierarchy = dimensions[i].getHierarchy( );
			saveDocObjToReportDocument( NamingUtil.getHierarchyDocName( dimensions[i].getName( ), hierarchy.getName( ) ), writer, stopSign );
			saveDocObjToReportDocument( NamingUtil.getHierarchyOffsetDocName( dimensions[i].getName( ), hierarchy.getName( ) ), writer, stopSign );
			ILevel[] levels = hierarchy.getLevels( );
			for ( int j = 0; j < levels.length; j++ )
			{
				saveDocObjToReportDocument( NamingUtil.getLevelIndexDocName( dimensions[i].getName( ),
						levels[j].getName( ) ),
						writer,
						stopSign );
				saveDocObjToReportDocument( NamingUtil.getLevelIndexOffsetDocName( dimensions[i].getName( ),
						levels[j].getName( ) ),
						writer,
						stopSign );
				
			}
		}
		
		writer.flush( );
	}
	
	/**
	 * 
	 * @param name
	 * @param writer
	 * @param stopSign
	 * @throws IOException
	 * @throws DataException
	 */
	private void saveDocObjToReportDocument( String name, IDocArchiveWriter writer, StopSign stopSign ) throws IOException, DataException
	{
		IDocumentObject documentObject = documentManager.openDocumentObject( name );
		RAOutputStream outputStreadm = writer.createRandomAccessStream( name );
		byte[] buffer = new byte[4096];
		if ( documentObject == null )
		{
			documentObject = null;
		}
		int readSize = documentObject.read( buffer, 0, buffer.length );
		
		while ( !stopSign.isStopped( ) && readSize >= 0 )
		{
			outputStreadm.write( buffer, 0, readSize );
			readSize = documentObject.read( buffer, 0, buffer.length );
		}
		outputStreadm.flush( );
		outputStreadm.close( );
		documentObject.close( );
	}
	
	/**
	 * @throws IOException 
	 * 
	 */
	public void close( ) throws IOException
	{
		documentManager.close( );
	}
}