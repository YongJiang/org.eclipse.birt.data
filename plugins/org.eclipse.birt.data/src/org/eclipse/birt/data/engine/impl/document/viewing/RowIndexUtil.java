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

package org.eclipse.birt.data.engine.impl.document.viewing;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.eclipse.birt.core.util.IOUtil;

/**
 * 
 */
class RowIndexUtil
{
	private DataOutputStream rowDos;
	private DataInputStream rowDis;

	/**
	 * @param rowOs
	 */
	RowIndexUtil( OutputStream rowOs )
	{
		rowDos = new DataOutputStream( rowOs );
	}

	/**
	 * @param rowIs
	 */
	RowIndexUtil( InputStream rowIs )
	{
		rowDis = new DataInputStream( rowIs );
	}

	/**
	 * @param rowId
	 * @param filterHint
	 */
	void write( int rowId )
	{
		try
		{
			IOUtil.writeInt( rowDos, rowId );
		}
		catch ( IOException e )
		{
			e.printStackTrace( );
		}
	}

	/**
	 * @return
	 */
	int read( )
	{
		try
		{
			return IOUtil.readInt( rowDis );
		}
		catch ( IOException e )
		{
			return -1;
		}
	}

	/**
	 *
	 */
	void close( )
	{
		try
		{
			if ( rowDos != null )
			{
				rowDos.close( );
			}
			if ( rowDis != null )
			{
				rowDis.close( );
			}
		}
		catch ( IOException e )
		{
			// ignore
		}
	}
	
}
