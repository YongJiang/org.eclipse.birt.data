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
import java.util.Arrays;

/**
 * 
 */

public class BufferedStructureArray implements IDiskArray
{

	private StructureDiskArray diskList = null;
	private IStructureCreator creator = null;

	private Object[] buffer = null;
	private int bufferPos = 0;

	public BufferedStructureArray( IStructureCreator creator, int bufferSize )
	{
		if ( bufferSize <= 0 )
		{
			buffer = new Object[100];
		}
		else
		{
			buffer = new Object[bufferSize];
		}
		this.creator = creator;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.birt.data.olap.data.util.IDiskArray#add(java.lang.Object)
	 */
	public boolean add( Object o ) throws IOException
	{
		if ( bufferPos < buffer.length )
		{
			buffer[bufferPos] = o;
			bufferPos++;
			return true;
		}
		if ( diskList == null )
		{
			diskList = new StructureDiskArray( creator );
		}
		diskList.add( o );
		return false;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.birt.data.olap.data.util.IDiskArray#close()
	 */
	public void close( ) throws IOException
	{
		clearTempDir( );
	}

	/*
	 * 
	 */
	private void clearTempDir( ) throws IOException
	{
		if ( diskList != null )
		{
			diskList.close( );
		}
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#finalize()
	 */
	public void finalize( )
	{
		try
		{
			clearTempDir( );
		}
		catch ( IOException e )
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.birt.data.olap.data.util.IDiskArray#get(int)
	 */
	public Object get( int index ) throws IOException
	{
		if ( index < bufferPos )
		{
			return buffer[index];
		}
		if ( diskList == null )
		{
			throw new IndexOutOfBoundsException( "Index: "
					+ index + ", Size: " + size( ) );
		}
		return diskList.get( index - buffer.length );
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.birt.data.olap.data.util.IDiskArray#size()
	 */
	public int size( )
	{
		if ( diskList == null )
		{
			return bufferPos;
		}
		else
		{
			return buffer.length + diskList.size( );
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.birt.data.olap.data.util.IDiskArray#clear()
	 */
	public void clear( ) throws IOException
	{
		bufferPos = 0;
		Arrays.fill( buffer, null );
		if ( diskList != null )
			diskList.clear( );
	}
}