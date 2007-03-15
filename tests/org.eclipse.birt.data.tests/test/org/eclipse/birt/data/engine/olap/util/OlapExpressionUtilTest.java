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

package org.eclipse.birt.data.engine.olap.util;

import junit.framework.TestCase;

/**
 * 
 */

public class OlapExpressionUtilTest extends TestCase
{
	/**
	 * 
	 */
	public void testGetTargetLevel( )
	{
		assertEquals( "level1",
				OlapExpressionUtil.getTargetLevel( "dimension[\"dim1\"][\"level1\"]" ) );
		
		assertEquals( null,
				OlapExpressionUtil.getTargetLevel( "dimension[\"dim1\"]" ) );
	}

}
