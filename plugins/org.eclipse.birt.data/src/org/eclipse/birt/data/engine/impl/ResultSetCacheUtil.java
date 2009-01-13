
/*******************************************************************************
 * Copyright (c) 2004, 2007 Actuate Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Actuate Corporation  - initial API and implementation
 *******************************************************************************/
package org.eclipse.birt.data.engine.impl;

import java.io.File;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;

/**
 * The util class which is used to provide utilities for Result Set Cache feature.
 * 
 */

class ResultSetCacheUtil
{
	private static final String CACHED_FILE_PREFIX = "cachedResultIterator";
	
	/**
	 * Return the Meta file
	 *  
	 * @return
	 */
	static File getMetaFile( final String tempDir, final String id )
	{
		try
		{
			return (File) AccessController.doPrivileged( new PrivilegedExceptionAction<Object>( ) {

				public Object run( ) throws Exception
				{

					File tmpDir = new File( tempDir );
					if (!tmpDir.exists( ) || !tmpDir.isDirectory( ))
					{
						tmpDir.mkdirs( );
					}
					File file = new File( tempDir
							+ CACHED_FILE_PREFIX
							+ id+"meta");
					return file;
				}
			} );
		}
		catch ( Exception e )
		{
			return null;
		}
	}
	
	/**
	 *  Return the data file
	 *  
	 * @return
	 */
	static File getDataFile( String tempDir, String id )
	{
		File file = new File( tempDir
				+ CACHED_FILE_PREFIX
				+ id+"data");
		return file;
	}
}
