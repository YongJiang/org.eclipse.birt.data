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
package org.eclipse.birt.data.engine.olap.cursor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.olap.OLAPException;
import javax.olap.OLAPWarning;

import org.eclipse.birt.data.engine.core.DataException;
import org.eclipse.birt.data.engine.i18n.ResourceConstants;
import org.eclipse.birt.data.engine.olap.data.api.IAggregationResultSet;
import org.eclipse.birt.data.engine.olap.driver.EdgeAxis;

/**
 * 
 * An EdgeNavigator maintains a cursor pointing to the Edge object. It will
 * navigate along the edge.
 * 
 */
class EdgeNavigator implements INavigator
{

	private EdgeInfoGenerator edgeInfoGenerator;
	private IAggregationResultSet rs;
	private int fetchSize = -1;
	private List warnings;

	EdgeNavigator( EdgeAxis axis )
	{
		this.edgeInfoGenerator = axis.getEdgeInfoUtil( );
		this.rs = axis.getQueryResultSet( );
	}

	/*
	 * @see org.eclipse.birt.data.jolap.cursor.INavigator#afterLast()
	 */
	public void afterLast( ) throws OLAPException
	{
		edgeInfoGenerator.edge_afterLast( );
	}

	/*
	 * @see org.eclipse.birt.data.jolap.cursor.INavigator#beforeFirst()
	 */
	public void beforeFirst( ) throws OLAPException
	{
		edgeInfoGenerator.edge_beforeFirst( );
	}

	/*
	 * @see org.eclipse.birt.data.engine.olap.cursor.INavigator#close()
	 */
	public void close( ) throws OLAPException
	{
		try
		{
			this.rs.close( );
		}
		catch ( IOException e )
		{
			throw new OLAPException( e.getLocalizedMessage( ) );
		}
	}

	/*
	 * @see org.eclipse.birt.data.jolap.cursor.INavigator#first()
	 */
	public boolean first( ) throws OLAPException
	{
		return edgeInfoGenerator.edge_first( );
	}

	/*
	 * @see org.eclipse.birt.data.jolap.cursor.INavigator#getExtend()
	 */
	public long getExtend( )
	{
		return 0;
	}

	/*
	 * @see org.eclipse.birt.data.jolap.cursor.INavigator#getPosition()
	 */
	public long getPosition( ) throws OLAPException
	{
		return edgeInfoGenerator.getEdgePostion( );
	}

	/*
	 * @see org.eclipse.birt.data.jolap.cursor.INavigator#getType()
	 */
	public int getType( )
	{
		return 0;
	}

	/*
	 * @see org.eclipse.birt.data.jolap.cursor.INavigator#isAfterLast()
	 */
	public boolean isAfterLast( ) throws OLAPException
	{
		return this.edgeInfoGenerator.edge_isAfterLast( );
	}

	/*
	 * @see org.eclipse.birt.data.jolap.cursor.INavigator#isBeforeFirst()
	 */
	public boolean isBeforeFirst( )
	{
		return this.edgeInfoGenerator.edge_isBeforeFirst( );
	}

	/*
	 * @see org.eclipse.birt.data.jolap.cursor.INavigator#isFirst()
	 */
	public boolean isFirst( ) throws OLAPException
	{
		return this.edgeInfoGenerator.edge_isFirst( );
	}

	/*
	 * @see org.eclipse.birt.data.jolap.cursor.INavigator#isLast()
	 */
	public boolean isLast( ) throws OLAPException
	{
		return this.edgeInfoGenerator.edge_isLast( );
	}

	/*
	 * @see org.eclipse.birt.data.jolap.cursor.INavigator#last()
	 */
	public boolean last( ) throws OLAPException
	{
		return this.edgeInfoGenerator.edge_last( );
	}

	/*
	 * @see org.eclipse.birt.data.jolap.cursor.INavigator#next()
	 */
	public boolean next( ) throws OLAPException
	{
		return this.edgeInfoGenerator.edge_next( );
	}

	/*
	 * @see org.eclipse.birt.data.jolap.cursor.INavigator#previous()
	 */
	public boolean previous( ) throws OLAPException
	{
		return this.edgeInfoGenerator.edge_previous( );
	}

	/*
	 * @see org.eclipse.birt.data.jolap.cursor.INavigator#relative(int)
	 */
	public boolean relative( int arg0 ) throws OLAPException
	{
		return this.edgeInfoGenerator.edge_relative( arg0 );
	}

	/*
	 * @see org.eclipse.birt.data.jolap.cursor.INavigator#setPosition(long)
	 */
	public void setPosition( long position ) throws OLAPException
	{
		this.edgeInfoGenerator.edge_setPostion( position );
	}

	/*
	 * @see org.eclipse.birt.data.engine.olap.cursor.INavigator#clearWarnings()
	 */
	public void clearWarnings( ) throws OLAPException
	{
		if ( warnings != null )
			this.warnings.clear( );
	}

	/*
	 * @see org.eclipse.birt.data.engine.olap.cursor.INavigator#getWarnings()
	 */
	public Collection getWarnings( ) throws OLAPException
	{
		return warnings == null ? new ArrayList( ) : warnings;
	}

	/*
	 * @see org.eclipse.birt.data.engine.olap.cursor.INavigator#setFetchSize(int)
	 */
	public void setFetchSize( int arg0 ) throws OLAPException
	{
		this.fetchSize = arg0;
		this.edgeInfoGenerator.setFetchSize( arg0 );
		if ( this.fetchSize >= 0 && this.fetchSize != this.rs.length( ) )
		{
			if ( warnings == null )
				warnings = new ArrayList( );
			DataException ex = new DataException( ResourceConstants.CONFIG_EDGE_FETCH_LIMIT_WARNING,
					new Object[]{
							new Integer( arg0 ),
							new Integer( this.rs.length( ) )
					} );
			warnings.add( new OLAPWarning( ex.getLocalizedMessage( ) ) );
		}
	}
}
