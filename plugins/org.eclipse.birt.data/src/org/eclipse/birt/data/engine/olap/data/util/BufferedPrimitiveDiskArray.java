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

import org.eclipse.birt.data.engine.olap.data.impl.Constants;

/**
 * 
 */

public class BufferedPrimitiveDiskArray implements IDiskArray
{
	final private static int DEFAULT_BUFFER_SIZE = 100;
	
	private PrimitiveDiskArray delegate = null;
	private Object[] buffer = null;
	private int bufferUsedSize = 0;

	public BufferedPrimitiveDiskArray( )
	{
		this( Constants.LIST_BUFFER_SIZE );
	}
	
	public BufferedPrimitiveDiskArray( int bufferSize )
	{
		if ( bufferSize <= 0 )
		{
			buffer = new Object[DEFAULT_BUFFER_SIZE];
		}
		else
		{
			buffer = new Object[bufferSize];
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.birt.data.olap.data.util.IDiskArray#add(java.lang.Object)
	 */
	public boolean add( Object o ) throws IOException
	{
		if ( bufferUsedSize < buffer.length )
		{
			buffer[bufferUsedSize] = o;
			bufferUsedSize++;
			return true;
		}
		if ( delegate == null )
		{
			delegate = new PrimitiveDiskArray( );
		}
		delegate.add( o );
		return true;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.birt.data.olap.data.util.IDiskArray#close()
	 */
	public void close( ) throws IOException
	{
		clearDiskFile( );
	}

	/**
	 * 
	 * @throws IOException
	 */
	private void clearDiskFile( ) throws IOException
	{
		if ( delegate != null )
		{
			delegate.close( );
			delegate = null;
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
			clearDiskFile( );
		}
		catch ( IOException e )
		{
			// TODO Auto-generated catch block
			e.printStackTrace( );
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.birt.data.olap.data.util.IDiskArray#get(int)
	 */
	public Object get( int index ) throws IOException
	{
		if ( index < bufferUsedSize )
		{
			return buffer[index];
		}
		if ( delegate == null )
		{
			throw new IndexOutOfBoundsException( "Index: "
					+ index + ", Size: " + size( ) );
		}
		return delegate.get( index - buffer.length );
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.birt.data.olap.data.util.IDiskArray#size()
	 */
	public int size( )
	{
		if ( delegate == null )
		{
			return bufferUsedSize;
		}
		else
		{
			return buffer.length + delegate.size( );
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.birt.data.olap.data.util.IDiskArray#clear()
	 */
	public void clear( ) throws IOException
	{
		bufferUsedSize = 0;
		if ( delegate != null )
			delegate.clear( );
	}
}