
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
package org.eclipse.birt.data.engine.olap.data.impl.aggregation;

import org.eclipse.birt.data.engine.olap.data.impl.dimension.Member;
import org.eclipse.birt.data.engine.olap.data.util.IStructure;
import org.eclipse.birt.data.engine.olap.data.util.IStructureCreator;
import org.eclipse.birt.data.engine.olap.data.util.ObjectArrayUtil;

/**
 * 
 */

public class Row4Aggregation implements IStructure
{
	private Member[] levelMembers;
	private Object[] measures;
	
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.birt.data.olap.data.util.IStructure#getFieldValues()
	 */
	public Object[] getFieldValues( )
	{
		Object[][] objectArrays = new Object[getLevelMembers().length+1][];
		for ( int i = 0; i < getLevelMembers().length; i++ )
		{
			objectArrays[i] = getLevelMembers()[i].getFieldValues( );
		}
		objectArrays[objectArrays.length-1] = getMeasures();
		return ObjectArrayUtil.convert( objectArrays );
	}
	
	/*
	 * 
	 */
	public static IStructureCreator getCreator()
	{
		return new Row4AggregationCreator( );
	}

	void setLevelMembers( Member[] levelMembers )
	{
		this.levelMembers = levelMembers;
	}

	Member[] getLevelMembers( )
	{
		return levelMembers;
	}

	void setMeasures( Object[] measures )
	{
		this.measures = measures;
	}

	Object[] getMeasures( )
	{
		return measures;
	}
}

/**
 * 
 * @author Administrator
 *
 */
class Row4AggregationCreator implements IStructureCreator
{
	private static IStructureCreator levelMemberCreator = Member.getCreator( );
	
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.birt.data.olap.data.util.IStructureCreator#createInstance(java.lang.Object[])
	 */
	public IStructure createInstance( Object[] fields )
	{
		Object[][] objectArrays = ObjectArrayUtil.convert( fields );
		Row4Aggregation result = new Row4Aggregation( );
		
		result.setLevelMembers( new Member[objectArrays.length - 1] );
		for ( int i = 0; i < result.getLevelMembers().length; i++ )
		{
			result.getLevelMembers()[i] = (Member) levelMemberCreator.createInstance( objectArrays[i] );
		}
		result.setMeasures( objectArrays[objectArrays.length-1] );
		
		return result;
	}
}
