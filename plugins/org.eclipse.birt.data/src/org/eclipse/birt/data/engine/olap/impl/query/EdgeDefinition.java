
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
package org.eclipse.birt.data.engine.olap.impl.query;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.birt.data.engine.olap.api.query.DimensionDefinition;
import org.eclipse.birt.data.engine.olap.api.query.IDimensionDefinition;
import org.eclipse.birt.data.engine.olap.api.query.IEdgeDefinition;
import org.eclipse.birt.data.engine.olap.api.query.IEdgeDrillFilter;
import org.eclipse.birt.data.engine.olap.api.query.ILevelDefinition;
import org.eclipse.birt.data.engine.olap.api.query.IMirroredDefinition;
import org.eclipse.birt.data.engine.olap.api.query.NamedObject;

/**
 * 
 */

public class EdgeDefinition extends NamedObject implements IEdgeDefinition
{
	private List<IDimensionDefinition> dimensions;
	private List<IEdgeDrillFilter> drillOperation;
	
	private ILevelDefinition mirrorStartingLevel;
	private IMirroredDefinition mirror;

	public EdgeDefinition( String name )
	{
		super( name );
		this.dimensions = new ArrayList<IDimensionDefinition>( );
		this.drillOperation = new ArrayList<IEdgeDrillFilter>( );
	}
	
	/*
	 * @see org.eclipse.birt.data.engine.olap.api.query.IEdgeDefinition#createDimension(java.lang.String)
	 */
	public IDimensionDefinition createDimension( String name )
	{
		IDimensionDefinition dim = new DimensionDefinition( name );
		this.dimensions.add( dim );
		return dim;
	}

	/*
	 * @see org.eclipse.birt.data.engine.olap.api.query.IEdgeDefinition#getDimensions()
	 */
	public List<IDimensionDefinition> getDimensions( )
	{
		return this.dimensions;
	}

	/*
	 * @see org.eclipse.birt.data.engine.olap.api.query.IEdgeDefinition#creatMirrorDefinition(org.eclipse.birt.data.engine.olap.api.query.ILevelDefinition, boolean)
	 */
	public void creatMirrorDefinition( ILevelDefinition level,
			boolean breakHierarchy )
	{
		this.mirror = new MirroredDefinition( level, breakHierarchy );
	}
	
	/*
	 * @see org.eclipse.birt.data.engine.olap.api.query.IEdgeDefinition#getMirroredDefinition()
	 */
	public IMirroredDefinition getMirroredDefinition( )
	{
		return this.mirror;
	}
	
	/*
	 * @see org.eclipse.birt.data.engine.olap.api.query.IEdgeDefinition#setMirrorStartingLevel(org.eclipse.birt.data.engine.olap.api.query.ILevelDefinition)
	 */
	public void setMirrorStartingLevel( ILevelDefinition level )
	{
		this.mirror = new MirroredDefinition( level, true );
	}

	/*
	 * @see org.eclipse.birt.data.engine.olap.api.query.IEdgeDefinition#getMirrorStartingLevel()
	 */
	public ILevelDefinition getMirrorStartingLevel( )
	{
		return this.mirrorStartingLevel;
	}

	/*
	 * @see org.eclipse.birt.data.engine.olap.api.query.IEdgeDefinition#getDrillFilter()
	 */
	public List<IEdgeDrillFilter> getDrillFilter( )
	{
		return this.drillOperation;
	}
	
	/*
	 * @see org.eclipse.birt.data.engine.olap.api.query.IEdgeDefinition#getDrillFilter()
	 */
	public IEdgeDrillFilter[] getDrillFilter( IDimensionDefinition dim )
	{
		List<IEdgeDrillFilter> drillList = new ArrayList<IEdgeDrillFilter>( );
		for ( int i = 0; i < this.drillOperation.size( ); i++ )
		{
			IEdgeDrillFilter filter = this.drillOperation.get( i );
			if ( filter.getTargetHierarchy( )
					.getDimension( )
					.getName( )
					.equals( dim.getName( ) ) )
			{
				drillList.add( filter );
			}
		}
		IEdgeDrillFilter[] drillFilters = new IEdgeDrillFilter[drillList.size( )];

		for ( int i = 0; i < drillList.size( ); i++ )
			drillFilters[i] = drillList.get( i );
		return drillFilters;
	}

	/*
	 * @see org.eclipse.birt.data.engine.olap.api.query.IEdgeDefinition#createDrillFilter(java.lang.String, org.eclipse.birt.data.engine.olap.api.query.IEdgeDrillFilter.DrillType)
	 */
	public IEdgeDrillFilter createDrillFilter( String name )
	{
		IEdgeDrillFilter drill = new EdgeDrillingFilterDefinition( name );
		drillOperation.add( drill );
		return drill;
	}
}
