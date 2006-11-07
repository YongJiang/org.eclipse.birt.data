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
package org.eclipse.birt.data.engine.regre.db;

import testutil.ConfigText;

/**
 *  
 */
public class ConnectOracleTest extends ConnectionTest
{

	/*
	 *  (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	protected void setUp( ) throws Exception
	{
		DriverClass = ConfigText.getString( "Regre.Oracle.DriverClass" );
		URL = ConfigText.getString( "Regre.Oracle.URL" );
		User = ConfigText.getString( "Regre.Oracle.User" );
		Password = ConfigText.getString( "Regre.Oracle.Password" );
		
		super.setUp();
	}
	
	/* (non-Javadoc)
	 * @see testutil.BaseTestCase#getTestTableName()
	 */
	protected String getTestTableName( )
	{
		return "\"ROOT\".\""+ConfigText.getString( "Regre.ConnectTest.TableName" )+"\"";
	}
}
