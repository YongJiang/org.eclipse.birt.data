
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
package org.eclipse.birt.data.engine.impl.document.stream;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.HashMap;

import org.eclipse.birt.core.archive.RAInputStream;
import org.eclipse.birt.core.util.IOUtil;
import org.eclipse.birt.data.engine.api.DataEngineContext;
import org.eclipse.birt.data.engine.core.DataException;
import org.eclipse.birt.data.engine.i18n.ResourceConstants;

/**
 * 
 */

public class MetaStreamReader extends StreamReader
{

	/**
	 * 
	 * @param context
	 * @param id
	 * @throws DataException
	 */
	public MetaStreamReader( DataEngineContext context, StreamID id ) throws DataException
	{
		try
		{
			this.streamMap = new HashMap();
			this.id = id;
			this.context = context;
			RAInputStream is = context.getInputStream( id.getStartStream( ),
					id.getSubQueryStream( ),
					DataEngineContext.META_INDEX_STREAM );
			
			DataInputStream metaIndexStream = new DataInputStream( is );
			
			
			while( is.getOffset( ) != is.length( ) )
			{
				int type = IOUtil.readInt( metaIndexStream );
				long offset = IOUtil.readLong( metaIndexStream );
				int size = IOUtil.readInt( metaIndexStream );
									
				this.streamMap.put( Integer.valueOf( type ), new OffsetInfo( offset, size ) );
			}
			
			metaIndexStream.close( );
		}
		catch ( IOException e )
		{
			throw new DataException( e.getLocalizedMessage( ) );
		}
	}
	
	/**
	 * 
	 * @param streamType
	 * @return
	 * @throws DataException
	 */
	public RAInputStream getRAInputStream( int streamType ) throws DataException
	{
		Object temp = this.streamMap.get( Integer.valueOf( streamType ) );
		if( temp == null )
		{
			throw new DataException( ResourceConstants.DOCUMENT_ERROR_CANNOT_LOAD_STREAM,
					DataEngineContext.getPath( id.getStartStream( ),
							id.getSubQueryStream( ),
							streamType ) );
		}
		else
		{
			OffsetInfo oi = (OffsetInfo)temp;
			
			long offset = oi.offset;
			int size = oi.size;
			RAInputStream metaStream = new WrapperedRAInputStream((RAInputStream)context.getInputStream( id.getStartStream( ),
					id.getSubQueryStream( ),
					getCollectionStreamType() ), offset, size);
			return metaStream;

		}
		
	}

	/**
	 * 
	 */
	protected int getCollectionStreamType( )
	{
		return DataEngineContext.META_STREAM;
	}
	
	private static class OffsetInfo
	{
		private long offset;
		private int size;
		
		public OffsetInfo( long offset, int size )
		{
			this.offset = offset;
			this.size = size;
		}
	}
}
