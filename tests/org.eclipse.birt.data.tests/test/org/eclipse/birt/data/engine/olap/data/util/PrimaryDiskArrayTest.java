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
import java.math.BigDecimal;
import java.util.Date;

import org.eclipse.birt.data.engine.olap.data.util.PrimitiveDiskArray;

import junit.framework.TestCase;

/**
 * 
 */

public class PrimaryDiskArrayTest extends TestCase
{

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

	public void testInteger( ) throws IOException
	{
		int objectNumber = 1001;
		PrimitiveDiskArray array = new PrimitiveDiskArray( );
		for ( int i = 0; i < objectNumber; i++ )
		{
			array.add( new Integer( i ) );
		}
		assertEquals( array.size( ), objectNumber );
		for ( int i = 0; i < objectNumber; i++ )
		{
			assertEquals( array.get( i ), new Integer( i ) );
		}
		array.close( );
	}

	public void testDouble( ) throws IOException
	{
		int objectNumber = 1001;
		PrimitiveDiskArray array = new PrimitiveDiskArray( );
		for ( int i = 0; i < objectNumber; i++ )
		{
			array.add( new Double( i ) );
		}
		assertEquals( array.size( ), objectNumber );
		for ( int i = 0; i < objectNumber; i++ )
		{
			assertEquals( array.get( i ), new Double( i ) );
		}
		array.close( );
	}

	public void testBoolean( ) throws IOException
	{
		int objectNumber = 1001;
		PrimitiveDiskArray array = new PrimitiveDiskArray( );
		for ( int i = 0; i < objectNumber; i++ )
		{
			if ( i % 2 == 0 )
			{
				array.add( new Boolean( false ) );
			}
			else
			{
				array.add( new Boolean( true ) );
			}
		}
		assertEquals( array.size( ), objectNumber );
		for ( int i = 0; i < objectNumber; i++ )
		{
			if ( i % 2 == 0 )
			{
				assertEquals( array.get( i ), new Boolean( false ) );
			}
			else
			{
				assertEquals( array.get( i ), new Boolean( true ) );
			}
		}
		array.close( );
	}

	public void testString( ) throws IOException
	{
		int objectNumber = 200;
		PrimitiveDiskArray array = new PrimitiveDiskArray( );
		for ( int i = 0; i < objectNumber; i++ )
		{
			array.add( "string" + i );
		}
		assertEquals( array.size( ), objectNumber );
		for ( int i = 0; i < objectNumber; i++ )
		{
			assertEquals( array.get( i ), "string" + i );
		}
		array.close( );
	}

	public void testBigDecimal( ) throws IOException
	{
		int objectNumber = 3000;
		PrimitiveDiskArray array = new PrimitiveDiskArray( );
		for ( int i = 0; i < objectNumber; i++ )
		{
			array.add( new BigDecimal( "1010101010101010101010" + i ) );
		}
		assertEquals( array.size( ), objectNumber );
		for ( int i = 0; i < objectNumber; i++ )
		{
			assertEquals( array.get( i ),
					new BigDecimal( "1010101010101010101010" + i ) );
		}
		array.close( );
	}

	public void testDate( ) throws IOException
	{
		int objectNumber = 4101;
		PrimitiveDiskArray array = new PrimitiveDiskArray( );
		for ( int i = 0; i < objectNumber; i++ )
		{
			array.add( new Date( 1900100000 + i * 1000 ) );
		}
		for ( int i = 0; i < objectNumber; i++ )
		{
			assertEquals( array.get( i ), new Date( 1900100000 + i * 1000 ) );
		}
		array.close( );
	}
}
