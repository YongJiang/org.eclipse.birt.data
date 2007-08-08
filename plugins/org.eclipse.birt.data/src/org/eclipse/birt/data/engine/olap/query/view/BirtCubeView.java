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

package org.eclipse.birt.data.engine.olap.query.view;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.olap.OLAPException;
import javax.olap.cursor.CubeCursor;

import org.eclipse.birt.core.exception.BirtException;
import org.eclipse.birt.data.engine.core.DataException;
import org.eclipse.birt.data.engine.olap.api.query.ICubeQueryDefinition;
import org.eclipse.birt.data.engine.olap.api.query.IEdgeDefinition;
import org.eclipse.birt.data.engine.olap.cursor.CubeCursorImpl;
import org.eclipse.birt.data.engine.olap.driver.IResultSet;
import org.eclipse.birt.data.engine.olap.impl.query.CubeQueryExecutor;

/**
 * A <code>BirtCubeView</code> represents a multi-dimensional selection of
 * values. A view contains a collection of <code>BirtEdgeView</code> that
 * group dimensions into a logical layout. This view has three types of
 * edges:rowEdgeView, coulumnEdgeView, measureEdgeView. A BirtCubeView has
 * association with a CubeCursor. This association will provide a user a way to
 * get data for the current intersection of the multi-dimensional selection.
 * 
 */
public class BirtCubeView
{

	private ICubeQueryDefinition queryDefn;
	private BirtEdgeView columnEdgeView, rowEdgeView;
	private BirtEdgeView calculatedMemberView[];
	private MeasureNameManager manager;
	private CubeQueryExecutor executor;

	/**
	 * Constructor: construct the row/column/measure EdgeView.
	 * 
	 * @param queryExecutor
	 * @throws DataException 
	 */
	public BirtCubeView( CubeQueryExecutor queryExecutor ) throws DataException
	{
		this.queryDefn = queryExecutor.getCubeQueryDefinition( );
		columnEdgeView = createBirtEdgeView( this.queryDefn.getEdge( ICubeQueryDefinition.COLUMN_EDGE ) );
		rowEdgeView = createBirtEdgeView( this.queryDefn.getEdge( ICubeQueryDefinition.ROW_EDGE ) );
		this.executor = queryExecutor;
		CalculatedMember[] members = CubeQueryDefinitionUtil.getCalculatedMembers( queryDefn,
				queryExecutor.getSession( ).getSharedScope( ) );
		if ( members != null && members.length > 0 )
		{
			Set rsIDSet = new HashSet( );
			calculatedMemberView = new BirtEdgeView[members.length];
			int index =0;
			for ( int i = 0; i < members.length; i++ )
			{
				if ( rsIDSet.contains( new Integer( members[i].getRsID( ) ) ) )
					continue;
				calculatedMemberView[index] = this.createBirtEdgeView( members[i] );
				rsIDSet.add( new Integer( members[i].getRsID( ) ) );
				index++;
			}
		}
		manager = new MeasureNameManager( members );
	}

	/**
	 * Get cubeCursor for current cubeView.
	 * @return CubeCursor
	 * @throws OLAPException
	 * @throws DataException 
	 */
	public CubeCursor getCubeCursor( ) throws OLAPException, DataException
	{
		Map relationMap = CubeQueryDefinitionUtil.getRelationWithMeasure( queryDefn );
		IResultSet result;
		try
		{
			result = new QueryExecutor( ).execute( this, executor, manager );
		}
		catch ( IOException e )
		{
			throw new OLAPException( e.getLocalizedMessage( ) );
		}
		catch ( BirtException e )
		{
			throw new OLAPException( e.getLocalizedMessage( ) );
		}

		CubeCursor cubeCursor = new CubeCursorImpl( this,
				result,
				relationMap,
				manager );
		return cubeCursor;
	}

	/**
	 * @return
	 */
	public BirtEdgeView getRowEdgeView( )
	{
		return this.rowEdgeView;
	}

	/**
	 * @return
	 */
	public BirtEdgeView getColumnEdgeView( )
	{
		return this.columnEdgeView;
	}

	/**
	 * 
	 * @return
	 */
	public BirtEdgeView[] getMeasureEdgeView( )
	{
		return this.calculatedMemberView;
	}

	/**
	 * 
	 * @param edgeDefn
	 * @return
	 */
	private BirtEdgeView createBirtEdgeView( IEdgeDefinition edgeDefn )
	{
		if ( edgeDefn == null )
			return null;
		return new BirtEdgeView( this, edgeDefn );
	}

	/**
	 * 
	 * @param calculatedMember
	 * @return
	 */
	private BirtEdgeView createBirtEdgeView( CalculatedMember calculatedMember )
	{
		return new BirtEdgeView( calculatedMember );
	}
}
