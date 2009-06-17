
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
package org.eclipse.birt.data.engine.olap.data.document;

import java.io.IOException;

import org.eclipse.birt.core.archive.IDocArchiveReader;
import org.eclipse.birt.core.archive.RAInputStream;

/**
 * 
 */

public class RADocumentManager implements IDocumentManager
{
	private IDocArchiveReader archiveReader;
	
	/**
	 * 
	 * @param archiveFile
	 * @throws IOException 
	 */
	RADocumentManager( IDocArchiveReader reader ) throws IOException
	{
		this.archiveReader = reader;
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.birt.data.engine.olap.data.document.IDocumentManager#close()
	 */
	public void close( ) throws IOException
	{
		//archiveReader.close( );
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.birt.data.engine.olap.data.document.IDocumentManager#createDocumentObject(java.lang.String)
	 */
	public IDocumentObject createDocumentObject( String documentObjectName ) throws IOException
	{
		throw new UnsupportedOperationException( );
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.birt.data.engine.olap.data.document.IDocumentManager#exist(java.lang.String)
	 */
	public boolean exist( String documentObjectName )
	{
		return archiveReader.exists( documentObjectName );
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.birt.data.engine.olap.data.document.IDocumentManager#openDocumentObject(java.lang.String)
	 */
	public IDocumentObject openDocumentObject( String documentObjectName ) throws IOException
	{
		RAInputStream inputStream = archiveReader.getStream( documentObjectName );
		if ( inputStream == null )
			return null;
		return new DocumentObject( new RandomDataAccessObject( new RAReader( inputStream ) ) );
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.birt.data.engine.olap.data.document.IDocumentManager#flush()
	 */
	public void flush( ) throws IOException
	{
		
	}

}