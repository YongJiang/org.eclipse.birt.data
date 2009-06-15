
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
package org.eclipse.birt.data.engine.impl.document.stream;

import java.io.IOException;

import org.eclipse.birt.core.archive.RAInputStream;
import org.eclipse.birt.data.engine.core.DataException;

/**
 * 
 */

public class WrapperedRAInputStream extends RAInputStream
{
	private RAInputStream raIn;
	private long startOffset;
	private long size;
	
	public WrapperedRAInputStream( RAInputStream input, long startOffset, long size ) throws DataException
	{
		this.raIn = input;
		this.startOffset = startOffset;
		this.size = size;
		try
		{
			this.raIn.seek( this.startOffset );
		}
		catch ( IOException e )
		{
			throw new DataException( e.getLocalizedMessage( ));
		}
	}
	
	public int available( ) throws IOException
	{
		return this.raIn.available( );
	}

	public long getOffset( ) throws IOException
	{
		return this.raIn.getOffset( ) - this.startOffset;
	}

	public long length( ) throws IOException
	{
		return this.size;
	}

	public void readFully( byte[] b, int off, int len ) throws IOException
	{
		this.raIn.readFully( b, off, len );
	}

	public int readInt( ) throws IOException
	{
		return this.raIn.readInt( );
	}

	public long readLong( ) throws IOException
	{
		return this.raIn.readLong( );
	}

	public void refresh( ) throws IOException
	{
		this.raIn.refresh( );
		this.raIn.seek( this.startOffset );
	}

	public void seek( long localPos ) throws IOException
	{
		this.raIn.seek( this.startOffset + localPos );
		
	}

	public int read( ) throws IOException
	{
		return this.raIn.read( );
	}
	
	public void close() throws IOException
	{
		this.raIn.close( );
	}
}
