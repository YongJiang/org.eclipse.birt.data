/*******************************************************************************
 * Copyright (c) 2004 Actuate Corporation. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Eclipse
 * Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Actuate Corporation - initial API and implementation
 ******************************************************************************/

package org.eclipse.birt.report.data.oda.jdbc.ui.util;

import java.sql.Connection;
import java.sql.SQLException;
import java.text.StringCharacterIterator;


import org.eclipse.birt.data.oda.OdaException;
import org.eclipse.birt.report.data.oda.jdbc.JDBCDriverManager;
import org.eclipse.birt.report.designer.internal.ui.util.ExceptionHandler;

public final class DriverLoader
{
	private DriverLoader( )
	{
	}

	public static Connection getConnection( String driverClassName,
			String connectionString, String userId,
			String password ) throws SQLException
	{
		try
		{
			return JDBCDriverManager.getInstance().getConnection( 
				driverClassName, connectionString, userId, password);
		}
		catch ( Exception e )
		{
			ExceptionHandler.handle(e);
			return null;
		}
	}

	static String escapeCharacters( String value )
	{
		final StringCharacterIterator iterator = new StringCharacterIterator( value );
		char character = iterator.current( );
		final StringBuffer result = new StringBuffer( );

		while ( character != StringCharacterIterator.DONE )
		{
			if ( character == '\\' )
			{
				result.append( "\\" ); //$NON-NLS-1$
			}
			else
			{
				//the char is not a special one
				//add it to the result as is
				result.append( character );
			}
			character = iterator.next( );
		}
		return result.toString( );

	}
	
	/**
	 * The method which test whether the give connection properties can be used to create a connection
	 * @param driverClassName the name of driver class
	 * @param connectionString the connection URL
	 * @param userId the user id
	 * @param password the pass word
	 * @return boolean whether could the connection being created
	 * @throws OdaException 
	 */
	public static boolean testConnection( String driverClassName,
			String connectionString, String userId,
			String password ) throws OdaException 
	{
		return JDBCDriverManager.getInstance().testConnection( driverClassName, connectionString, userId,password);
	}
}

