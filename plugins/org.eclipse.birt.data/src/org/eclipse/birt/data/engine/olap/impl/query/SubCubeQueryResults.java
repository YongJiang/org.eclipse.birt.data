/*******************************************************************************
 * Copyright (c) 2004, 2008 Actuate Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Actuate Corporation  - initial API and implementation
 *******************************************************************************/
package org.eclipse.birt.data.engine.olap.impl.query;

import javax.olap.OLAPException;
import javax.olap.cursor.CubeCursor;

import org.eclipse.birt.core.exception.BirtException;
import org.eclipse.birt.data.engine.core.DataException;
import org.eclipse.birt.data.engine.i18n.ResourceConstants;
import org.eclipse.birt.data.engine.olap.api.ICubeCursor;
import org.eclipse.birt.data.engine.olap.api.ICubeQueryResults;
import org.eclipse.birt.data.engine.olap.api.query.ISubCubeQueryDefinition;
import org.eclipse.birt.data.engine.olap.query.view.BirtCubeView;
import org.eclipse.birt.data.engine.olap.script.JSLevelAccessor;
import org.eclipse.birt.data.engine.olap.script.JSMeasureAccessor;
import org.eclipse.birt.data.engine.script.ScriptConstants;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

/**
 * SubCubeQueryResults represents the result set from subQuery
 *
 */
public class SubCubeQueryResults implements ICubeQueryResults
{
	private ICubeCursor cubeCursor;
	private BirtCubeView cubeView;
	private Scriptable subScope;
	
	/**
	 * 
	 * @param cubeCursor
	 * @throws DataException 
	 */
	public SubCubeQueryResults( ISubCubeQueryDefinition query,
			ICubeQueryResults parent, Scriptable scope ) throws DataException
	{
		this.cubeView = ( (CubeCursorImpl) parent.getCubeCursor( ) ).getCubeView( );
		this.subScope = scope;
		
		this.cubeCursor = getSubCubeCursor( query.getStartingLevelOnColumn( ),
				query.getStartingLevelOnRow( ) );
	}

	/*
	 * @see org.eclipse.birt.data.engine.olap.api.ICubeCursor#getSubCubeCursor(java.lang.String, java.lang.String, java.lang.String, org.mozilla.javascript.Scriptable)
	 */
	private ICubeCursor getSubCubeCursor( String startingColumnLevel,
			String startingRowLevel ) throws DataException
	{
		ICubeCursor cubeCursorImpl;
		if ( this.cubeView != null )
		{
			BirtCubeView subCV = new BirtCubeView( this.cubeView.getCubeQueryExecutor( ),
					null );
			CubeCursor subCubeCursor = null;
			if ( subScope == null )
			{
				Context cx = Context.enter( );
				try
				{
					Scriptable scope = cubeView.getCubeQueryExecutor( ).getSession( ).getSharedScope( );
					subScope = cx.newObject( scope );
					subScope.setParentScope( scope );
					subScope.setPrototype( scope );
				}
				finally
				{
					Context.exit( );
				}
			}
			try
			{
				subCubeCursor = subCV.getCubeCursor( null,
						startingColumnLevel,
						startingRowLevel,
						this.cubeView );
				subScope.put( ScriptConstants.MEASURE_SCRIPTABLE,
						subScope,
						new JSMeasureAccessor( subCubeCursor,
								subCV.getMeasureMapping( ) ) );
				subScope.put( ScriptConstants.DIMENSION_SCRIPTABLE,
						subScope,
						new JSLevelAccessor( this.cubeView.getCubeQueryExecutor( )
								.getCubeQueryDefinition( ),
								subCV ) );
			}
			catch ( OLAPException e )
			{
				throw new DataException( e.getLocalizedMessage( ) );
			}
			cubeCursorImpl = new CubeCursorImpl( null,
					subCubeCursor,
					subScope,
					this.cubeView.getCubeQueryExecutor( )
							.getCubeQueryDefinition( ),
					subCV );
		}
		else
		{
			throw new DataException( ResourceConstants.NO_PARENT_RESULT_CURSOR );
		}
		return cubeCursorImpl;
	}
	
	/*
	 * @see org.eclipse.birt.data.engine.olap.api.ICubeQueryResults#cancel()
	 */
	public void cancel( )
	{
	}

	/*
	 * @see org.eclipse.birt.data.engine.olap.api.ICubeQueryResults#getCubeCursor()
	 */
	public ICubeCursor getCubeCursor( ) throws DataException
	{
		return cubeCursor;
	}

	/*
	 * @see org.eclipse.birt.data.engine.api.IBaseQueryResults#close()
	 */
	public void close( ) throws BirtException
	{
	}

	/*
	 * @see org.eclipse.birt.data.engine.api.IBaseQueryResults#getID()
	 */
	public String getID( )
	{
		return null;
	}

}