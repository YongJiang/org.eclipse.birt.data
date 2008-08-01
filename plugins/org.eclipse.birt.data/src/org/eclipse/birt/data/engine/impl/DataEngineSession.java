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
package org.eclipse.birt.data.engine.impl;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.birt.core.archive.RAOutputStream;
import org.eclipse.birt.core.script.CoreJavaScriptInitializer;
import org.eclipse.birt.core.util.IOUtil;
import org.eclipse.birt.data.engine.api.DataEngine;
import org.eclipse.birt.data.engine.api.DataEngineContext;
import org.eclipse.birt.data.engine.api.IShutdownListener;
import org.eclipse.birt.data.engine.core.DataException;
import org.eclipse.birt.data.engine.executor.DataSetCacheManager;
import org.eclipse.birt.data.engine.impl.document.NamingRelation;
import org.eclipse.birt.data.engine.impl.document.QueryResultIDUtil;
import org.mozilla.javascript.ImporterTopLevel;
import org.mozilla.javascript.Scriptable;

/**
 * Data Engine Session contains DataEngineImpl specific information. Each DataEngineSession
 * only has one DataEngineImpl instance accompany with, and verse visa. 
 */

public class DataEngineSession
{
	private Map context;
	private Scriptable scope;
	private DataSetCacheManager dataSetCacheManager;
	private DataEngineImpl engine;
	private String tempDir;
	private QueryResultIDUtil queryResultIDUtil;
	
	private NamingRelation namingRelation;
	
	private static ThreadLocal<ClassLoader> classLoaderHolder = new ThreadLocal<ClassLoader>();
	
	private static Logger logger = Logger.getLogger( DataEngineSession.class.getName( ) );
	/**
	 * Constructor.
	 * @param engine
	 */
	public DataEngineSession( DataEngineImpl engine )
	{
		Object[] params = { engine };
		logger.entering( DataEngineSession.class.getName( ),
				"DataEngineSession",
				params );
		
		this.context = new HashMap();

		this.engine = engine;
		this.scope = engine.getContext( ).getJavaScriptScope( );
		
	
			if ( this.scope == null )
		{
			this.scope = new ImporterTopLevel( engine.getContext( )
					.getScriptContext( )
					.getContext( ) );
		}

		new CoreJavaScriptInitializer( ).initialize( engine.getContext( )
				.getScriptContext( )
				.getContext( ), scope );
		tempDir = engine.getContext( ).getTmpdir( ) +
				"DataEngine_" + engine.hashCode( ) + File.separator;

		this.dataSetCacheManager = new DataSetCacheManager( this );
		
		classLoaderHolder.set( engine.getContext( ).getClassLoader( ) );
		engine.addShutdownListener( new IShutdownListener(){

			public void dataEngineShutdown( )
			{
				classLoaderHolder.set( null );
				
			}} );
		
		engine.addShutdownListener( new ReportDocumentShutdownListener( this ) );
		this.queryResultIDUtil = new QueryResultIDUtil();
		logger.exiting( DataEngineSession.class.getName( ), "DataEngineSession" );
	}
	
	/**
	 * Get the data engine.
	 * @return
	 */
	public DataEngine getEngine()
	{
		return this.engine;
	}
	
	/**
	 * Get a context property according to given key.
	 * 
	 * @param key
	 * @return
	 */
	public Object get( String key )
	{
		if( key!= null )
			return this.context.get( key );
		return null;
	}
	
	/**
	 * Set a context property with given key.
	 * 
	 * @param key
	 * @param value
	 */
	public void set( String key, Object value )
	{
		this.context.put( key, value );
	}
	
	/**
	 * 
	 * @return
	 */
	public Scriptable getSharedScope( )
	{
		return this.scope;
	}
	
	/**
	 * 
	 * @return
	 */
	public DataSetCacheManager getDataSetCacheManager( )
	{
		return this.dataSetCacheManager;
	}
	
	public static ClassLoader getCurrentClassLoader( )
	{
		return classLoaderHolder.get( );
	}
	
	/**
	 * @return the temp dir path used by this session, ended with File.Separator
	 */
	public String getTempDir()
	{
		return tempDir;
	}
	
	/**
	 * @return the binding Data Engine Context
	 */
	public DataEngineContext getEngineContext() 
	{
		return this.engine.getContext( );
	}
	
	/**
     * @return the bound QueryResultIDUtil.
     */
	public QueryResultIDUtil getQueryResultIDUtil()
	{
		return this.queryResultIDUtil;
	}
	
	
	/**
	 * @return the namingRelation
	 */
	public NamingRelation getNamingRelation( )
	{
		return namingRelation;
	}

	
	/**
	 * @param namingRelation the namingRelation to set
	 */
	public void setNamingRelation( NamingRelation namingRelation )
	{
		this.namingRelation = namingRelation;
	}

	/**
	 *
	 */
	class ReportDocumentShutdownListener implements IShutdownListener
	{

		private DataEngineSession session;

		ReportDocumentShutdownListener( DataEngineSession session )
		{
			this.session = session;
		}

		public void dataEngineShutdown( )
		{
			if ( session.getNamingRelation( ) == null )
			{
				return;
			}
			final int mode = session.getEngineContext( ).getMode( );
			if ( mode == DataEngineContext.MODE_GENERATION
					|| mode == DataEngineContext.MODE_UPDATE )
			{
				try
				{
					saveNamingRelation( session.getNamingRelation( ) );
				}
				catch ( DataException e1 )
				{
					e1.printStackTrace( );
				}
			}
		}

		/**
		 * 
		 * @param relation
		 * @throws DataException
		 */
		private void saveNamingRelation( NamingRelation relation )
				throws DataException
		{
			Map bookmarkMap = relation.getBookmarkMap( );
			Map elementIdMap = relation.getElementIdMap( );
			RAOutputStream out = session.getEngineContext( )
					.getOutputStream( null,
							null,
							DataEngineContext.NAMING_RELATION_STREAM );
			try
			{
				DataOutputStream dos = new DataOutputStream( new BufferedOutputStream( out ) );
				IOUtil.writeMap( dos, bookmarkMap );
				IOUtil.writeMap( dos, elementIdMap );
				dos.flush( );
			}
			catch ( IOException e )
			{
				throw new DataException( "", e ); //$NON-NLS-1$
			}
			finally
			{
				if ( out != null )
				{
					try
					{
						out.close( );
					}
					catch ( IOException e )
					{
						logger.log( Level.SEVERE, "", e );
					}
				}
			}
		}
	}
}
