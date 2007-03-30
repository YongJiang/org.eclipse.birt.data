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

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Date;

import org.eclipse.birt.data.engine.olap.data.util.BufferedRandomAccessFile;

import junit.framework.TestCase;

/**
 * 
 */

public class BufferedRandomAccessFileTest extends TestCase
{
	private static final String tmpPath = System.getProperty( "java.io.tmpdir" );
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see junit.framework.TestCase#setUp()
	 */
	protected void setUp( ) throws Exception
	{
		super.setUp( );
	}

	/*
	 * @see TestCase#tearDown()
	 */
	protected void tearDown( ) throws Exception
	{
		super.tearDown( );
	}
	
	public void testBytes( ) throws IOException
	{
		BufferedRandomAccessFile file = new BufferedRandomAccessFile(new File(
				tmpPath + File.separatorChar + "BufferedRandomAccessFile"),
				"rw", 1000);
		byte[] bytes = new byte[1024];
		bytes[0] = 1;
		bytes[1] = 2;
		file.seek( 0 );
		file.write( bytes, 0, bytes.length );
		file.write( bytes, 0, bytes.length );
		file.write( bytes, 0, bytes.length );
		bytes = new byte[932];
		file.write( bytes, 0, bytes.length );
		
		bytes = new byte[1024];
		file.seek( 0 );
		assertEquals(file.read( bytes, 0, bytes.length ), 1024);
		assertEquals(bytes[0], 1);
		assertEquals(bytes[1], 2);
		file.close( );
	}

	public void testInteger( ) throws IOException
	{
		int objectNumber = 1001;
		BufferedRandomAccessFile file = new BufferedRandomAccessFile(new File(
				tmpPath + File.separatorChar + "BufferedRandomAccessFile"),
				"rw", 1000);
		for ( int i = 0; i < objectNumber; i++ )
		{
			file.writeInt( i );
		}
		file.seek( 0 );
		for ( int i = 0; i < objectNumber; i++ )
		{
			assertEquals( file.readInt( ), i );
		}
		file.seek( 400 );
		assertEquals( file.readInt( ), 100 );
		file.seek( 804 );
		assertEquals( file.readInt( ), 201 );
		assertEquals( file.readInt( ), 202 );
		file.seek( 2804 );
		file.writeInt( 1000001 );
		assertEquals( file.readInt( ), 702 );
		file.seek( 2804 );
		assertEquals( file.readInt( ), 1000001 );
		file.close( );
	}

	public void testString( ) throws IOException
	{
		int objectNumber = 3000;
		BufferedRandomAccessFile file = new BufferedRandomAccessFile(new File(
				tmpPath + File.separatorChar + "BufferedRandomAccessFile"),
				"rw", 1000);
		for ( int i = 0; i < objectNumber; i++ )
		{
			file.writeUTF( "string" + i );
		}
		file.seek( 0 );
		for ( int i = 0; i < objectNumber; i++ )
		{
			assertEquals( file.readUTF( ), "string" + i );
		}
		file.close( );
	}

	public void testBigDecimal( ) throws IOException
	{
		int objectNumber = 3000;
		BufferedRandomAccessFile file = new BufferedRandomAccessFile(new File(
				tmpPath + File.separatorChar + "BufferedRandomAccessFile"),
				"rw", 1000);
		for ( int i = 0; i < objectNumber; i++ )
		{
			file.writeBigDecimal( new BigDecimal( "1010101010101010101010" + i ) );
		}
		file.seek( 0 );
		for ( int i = 0; i < objectNumber; i++ )
		{
			assertEquals( file.readBigDecimal( ),
					new BigDecimal( "1010101010101010101010" + i ) );
		}
		file.close( );
	}

	public void testDate( ) throws IOException
	{
		int objectNumber = 4101;
		BufferedRandomAccessFile file = new BufferedRandomAccessFile(new File(
				tmpPath + File.separatorChar + "BufferedRandomAccessFile"),
				"rw", 1000);
		for ( int i = 0; i < objectNumber; i++ )
		{
			file.writeDate( new Date( 1900100000 + i * 1000 ) );
		}
		file.seek( 0 );
		for ( int i = 0; i < objectNumber; i++ )
		{
			assertEquals( file.readDate( ), new Date( 1900100000 + i * 1000 ) );
		}
		file.close( );
	}

	public void testMixed( ) throws IOException
	{
		int objectNumber = 1001;
		BufferedRandomAccessFile file = new BufferedRandomAccessFile(new File(
				tmpPath + File.separatorChar + "BufferedRandomAccessFile"),
				"rw", 1000);
		for ( int i = 0; i < objectNumber; i++ )
		{
			file.writeInt( i );
		}
		file.seek( 0 );
		for ( int i = 0; i < objectNumber; i++ )
		{
			assertEquals( file.readInt( ), i );
		}
		file.writeBigDecimal( new BigDecimal( "1010101010101" ) );
		file.writeDate( new Date( 12202000 ) );
		file.writeUTF( "testString" );
		file.writeShort( 1300 );
		file.writeLong( 300000111l );
		file.seek( 0 );
		file.skipBytes( objectNumber * 4 );
		assertEquals( file.readBigDecimal( ), new BigDecimal( "1010101010101" ) );
		assertEquals( file.readDate( ), new Date( 12202000 ) );
		assertEquals( file.readUTF( ), "testString" );
		assertEquals( file.readShort( ), 1300 );
		assertEquals( file.readLong( ), 300000111l );
	}
}
