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

package org.eclipse.birt.data.engine.olap.data.util;

import java.io.IOException;

/**
 * A List class providing the service of reading/writing objects from one file
 * when cache is not enough . It makes the reading/writing objects transparent.
 */

public class StructureDiskArray extends BaseDiskArray
{

	private IStructureCreator creator;
	private ObjectWriter[] fieldWriters;
	private ObjectReader[] fieldReaders;

	/**
	 * @throws IOException 
	 * 
	 * 
	 */
	public StructureDiskArray( IStructureCreator creator ) throws IOException
	{
		super( );
		this.creator = creator;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.birt.data.engine.cache.BasicCachedList#writeObject(java.io.DataOutputStream,
	 *      java.lang.Object)
	 */
	protected void writeObject( Object object ) throws IOException
	{
		if ( object == null )
		{
			randomAccessFile.writeShort( NULL_VALUE );
			return;
		}
		IStructure cachedObject = (IStructure) object;
		Object[] objects = cachedObject.getFieldValues( );
		randomAccessFile.writeShort( (short) objects.length );
		if ( fieldWriters == null )
		{
			createReadersAndWriters( objects.length );
		}
		for ( int i = 0; i < objects.length; i++ )
		{
			assert objects[i] != null;
			fieldWriters[i].write( randomAccessFile, objects[i] );
		}
	}

	/**
	 * 
	 * @param size
	 */
	private void createReadersAndWriters( int size )
	{
		fieldWriters = new ObjectWriter[size];
		fieldReaders = new ObjectReader[size];
		for ( int i = 0; i < size; i++ )
		{
			fieldWriters[i] = new ObjectWriter( );
			fieldReaders[i] = new ObjectReader( );
		}
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.birt.data.engine.cache.BasicCachedList#readObject(java.io.DataInputStream)
	 */
	protected Object readObject( ) throws IOException
	{
		short fieldCount = randomAccessFile.readShort( );
		if ( fieldCount == NULL_VALUE )
		{
			return null;
		}
		Object[] objects = new Object[fieldCount];
		for ( int i = 0; i < objects.length; i++ )
		{
			if ( fieldReaders[i].getDataType( ) != fieldWriters[i].getDataType( ) )
				fieldReaders[i].setDataType( fieldWriters[i].getDataType( ) );
			objects[i] = fieldReaders[i].read( randomAccessFile );
		}
		return creator.createInstance( objects );
	}
	
	public void clear( ) throws IOException
	{
		fieldWriters = null;
		fieldReaders = null;
		super.clear();
	}

}