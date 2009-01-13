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

package org.eclipse.birt.data.engine.executor.cache.disk;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

import org.eclipse.birt.data.engine.executor.cache.ResultObjectUtil;
import org.eclipse.birt.data.engine.impl.StopSign;
import org.eclipse.birt.data.engine.odi.IResultObject;

/**
 * A utility file writer handler, which keeps the file stream and output stream
 * for reading data.
 */
class DataFileWriter
{
	private File file;
	private boolean isOpen;

	private FileOutputStream fos;
	private BufferedOutputStream bos;

	private ResultObjectUtil resultObjectUtil;
	
	/**
	 * A util method to new instance of DataFileWriter
	 * 
	 * @param file
	 * @return DataFileWriter instance
	 */
	static DataFileWriter newInstance( File file,
			ResultObjectUtil resultObjectUtil )
	{
		return new DataFileWriter( file, resultObjectUtil );
	}

	/**
	 * Construction
	 * 
	 * @param file
	 */
	private DataFileWriter( File file, ResultObjectUtil resultObjectUtil )
	{
		this.resultObjectUtil = resultObjectUtil;
		setWriteFile( file );
	}
	
	/**
	 * Set which file to be written. This method is mainly used to new less
	 * instance.
	 * 
	 * @param file
	 */
	void setWriteFile( File file )
	{
		if ( isOpen )
			close( );

		this.file = file;
		this.isOpen = false;
	}

	/**
	 * Write the specified length of objects from file. Notice to improve the
	 * efficienly of reading, the order of writing only can be sequencial. The
	 * caller has responsibility to design a good algorithm to achive this goal.
	 * 
	 * @param resultObjects
	 * @param count
	 * @param stopSign
	 * @throws IOException,
	 *             exception of writing file
	 */
	void write( IResultObject[] resultObjects, int count, StopSign stopSign ) throws IOException
	{
		if ( isOpen == false )
		{
			try
			{
				AccessController.doPrivileged( new PrivilegedExceptionAction<Object>( ) {

						public Object run( ) throws FileNotFoundException
						{
							fos = new FileOutputStream( file );
							bos = new BufferedOutputStream( fos );
							isOpen = true;
							return null;
						}
					} );
			}
			catch ( Exception e )
			{
				// normally this exception will never be thrown
				// since file will always exist
			}
			
		}

		resultObjectUtil.writeData( bos, resultObjects, count, stopSign );
	}

	/**
	 * Close current output file 
	 * 
	 * @throws IOException, file close exception
	 */
	void close( )
	{
		if ( isOpen )
		{
			try
			{
				bos.close( );
				fos.close( );
				isOpen = false;
			}
			catch ( IOException e )
			{
				// normally this exception will never be thrown
			}
		}
	}

}