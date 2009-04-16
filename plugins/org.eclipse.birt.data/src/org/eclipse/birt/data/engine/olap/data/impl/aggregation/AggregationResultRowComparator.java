
/*******************************************************************************
 * Copyright (c) 2004, 2009 Actuate Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Actuate Corporation  - initial API and implementation
 *******************************************************************************/
package org.eclipse.birt.data.engine.olap.data.impl.aggregation;

import java.util.Comparator;

import org.eclipse.birt.data.engine.olap.data.api.IAggregationResultRow;
import org.eclipse.birt.data.engine.olap.data.impl.dimension.Member;

/**
 * 
 */

public class AggregationResultRowComparator implements Comparator<IAggregationResultRow>
{
	private int[] keyLevelIndexs;
	
	AggregationResultRowComparator( int[] keyLevelIndexs )
	{
		this.keyLevelIndexs = keyLevelIndexs;
	}

	/*
	 * (non-Javadoc)
	 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
	 */
	public int compare( IAggregationResultRow o1, IAggregationResultRow o2 )
	{
		Member[] member1 = ( (IAggregationResultRow) o1 ).getLevelMembers( );
		Member[] member2 = ( (IAggregationResultRow) o2 ).getLevelMembers( );

		for ( int i = 0; i < keyLevelIndexs.length; i++ )
		{
			int result = ( member1[keyLevelIndexs[i]] ).compareTo( member2[keyLevelIndexs[i]] );
			if ( result < 0 )
			{
				return result;
			}
			else if ( result > 0 )
			{
				return result;
			}
		}
		return 0;
	}
}
