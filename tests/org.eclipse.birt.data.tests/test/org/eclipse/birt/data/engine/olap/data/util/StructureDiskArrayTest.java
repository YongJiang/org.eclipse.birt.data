
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

import org.eclipse.birt.data.engine.olap.data.util.StructureDiskArray;

import junit.framework.TestCase;


/**
 * 
 */

public class StructureDiskArrayTest extends TestCase
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
	
	public void testMemberForTest( ) throws IOException
	{
		int objectNumber = 1001;
		StructureDiskArray array = new StructureDiskArray( MemberForTest.getMemberCreator( ) );
		for ( int i = 0; i < objectNumber; i++ )
		{
			array.add( createMember( i ) );
		}
		assertEquals( array.size( ), objectNumber );
		for ( int i = 0; i < objectNumber; i++ )
		{
			assertEquals( array.get( i ), createMember( i ) );
		}
		array.close( );
	}
	
	public void testMemberForTest2( ) throws IOException
	{
		int objectNumber = 1001;
		StructureDiskArray array = new StructureDiskArray( MemberForTest.getMemberCreator( ) );
		for ( int i = 0; i < objectNumber; i++ )
		{
			array.add( createMember( i ) );
		}
		assertEquals( array.size( ), objectNumber );
		array.add( createMember( 200 ) );
		array.add( createMember( 200 ) );
		array.add( createMember( 205 ) );
		for ( int i = 0; i < objectNumber; i++ )
		{
			assertEquals( array.get( i ), createMember( i ) );
		}
		assertEquals( array.get( objectNumber ), createMember( 200 ) );
		assertEquals( array.get( objectNumber+1 ), createMember( 200 ) );
		assertEquals( array.get( objectNumber+2 ), createMember( 205 ) );
		array.close( );
	}
	
	static private MemberForTest createMember( int i )
	{
		int iField = i;
		Date dateField = new Date( 190001000 + i * 1000 );
		String stringField = "string" + i;
		double doubleField = i + 10.0;
		BigDecimal bigDecimalField = new BigDecimal( "1010101010100101010110"
				+ i );
		boolean booleanField = ( i % 2 == 0 ? true : false );
		return new MemberForTest( iField,
				dateField,
				stringField,
				doubleField,
				bigDecimalField,
				booleanField );
	}
}
