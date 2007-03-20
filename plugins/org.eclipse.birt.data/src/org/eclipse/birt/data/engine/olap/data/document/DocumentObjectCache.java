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
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * A cache that keeps a collection of at most maximumCapacity document object.
 * When the number of entries exceeds that limit, least recently used entries
 * are removed so the current size is the same as the maximum capacity.
 */

public class DocumentObjectCache
{

	private IDocumentManager documentManager = null;
	private int cachedSize;
	private LinkedList linkedList = null;
	private HashMap map = null;

	public DocumentObjectCache( IDocumentManager documentManager )
	{
		this( documentManager, 200 );
	}

	public DocumentObjectCache( IDocumentManager documentManager,
			int cachedSize )
	{
		this.documentManager = documentManager;
		this.cachedSize = cachedSize;

		linkedList = new LinkedList( );
		map = new HashMap( );
	}

	/**
	 * Get the instance of IDocumentObject by name.
	 * @param name
	 * @return
	 * @throws IOException
	 */
	public IDocumentObject getIDocumentObject( String name )
			throws IOException
	{
		Object cachedObject = map.get( name );
		if ( cachedObject != null )
		{
			return (IDocumentObject) cachedObject;
		}

		if ( linkedList.size( ) >= cachedSize )
		{
			String lastName = (String) linkedList.getLast( );
			linkedList.removeLast( );
			( (IDocumentObject) map.get( lastName ) ).close( );
			map.remove( lastName );
		}

		IDocumentObject newDocumentObject = documentManager.openDocumentObject( name );
		if ( newDocumentObject == null )
		{
			newDocumentObject = documentManager.createDocumentObject( name );
		}
		newDocumentObject.seek( newDocumentObject.length( ) );
		map.put( name, newDocumentObject );
		linkedList.addFirst( name );
		return newDocumentObject;
	}

	/**
	 * Close all cached document objects.
	 * 
	 * @throws IOException
	 */
	public void closeAll( ) throws IOException
	{
		Iterator allOjbects = map.values( ).iterator( );

		while ( allOjbects.hasNext( ) )
		{
			( (IDocumentObject) allOjbects.next( ) ).close( );
		}
	}

}
