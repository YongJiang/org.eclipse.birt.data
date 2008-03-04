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

package org.eclipse.birt.data.aggregation.impl.rank;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.birt.data.aggregation.i18n.ResourceConstants;
import org.eclipse.birt.data.aggregation.impl.AggrException;
import org.eclipse.birt.data.engine.aggregation.SummaryAccumulator;
import org.eclipse.birt.data.engine.core.DataException;

/**
 * Accumulator that is used by Percentile and Quartile.
 * The formula to calculate the Percentile is not of standard one. It follows
 * Microsoft excel convention.
 * 
 * Say, if you want pct-th percentile from acading array a[], 
 * the pseudocodes of calculation looks like follows:
 *  
 * 			k=Math.floor((pct/4)*(n-1))+1)
 * 			f=(pct/4)*(n-1))+1 - k; // We also need to calculate fraction:
 * 			ad = a[k]+(f*(a[k+1]-a[k])) //Then we can calculate out the adjustment:
 * 			result = a[k] + ad;
 * 
 */
abstract class PercentileAccumulator extends SummaryAccumulator
{

	//
	private double pct;
	private List cachedValues;

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.birt.data.engine.aggregation.SummaryAccumulator#start()
	 */
	public void start( )
	{
		super.start( );

		pct = -1;
		cachedValues = new ArrayList( );
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.birt.data.engine.aggregation.Accumulator#onRow(java.lang.Object[])
	 */
	public void onRow( Object[] args ) throws DataException
	{
		assert ( args.length == 2 );
		if ( args[0] != null )
		{
			if ( args[0] != null )
			{
				Double d = RankAggregationUtil.getNumericValue( args[0] );
				if ( d != null )
					cachedValues.add( d );
			}
		}
		if ( pct == -1 )
		{
			Double pctValue = RankAggregationUtil.getNumericValue( args[1] );
			pct = getPctValue( pctValue );
		}
	}

	protected abstract double getPctValue( Double d ) throws DataException;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.birt.data.engine.aggregation.SummaryAccumulator#getSummaryValue()
	 */
	public Object getSummaryValue( ) throws DataException
	{
		Object[] sortedObjs = this.cachedValues.toArray( );
		if ( sortedObjs.length == 0 )
		{
			return DataException.wrap( new AggrException( ResourceConstants.INVALID_PERCENTILE_COLUMN ) );
		}
		RankAggregationUtil.sortArray( sortedObjs );
		double n = pct * ( sortedObjs.length - 1 ) + 1;
		int k = (int) Math.floor( n );
		double fraction = n - k;
		double value = ( (Double) sortedObjs[k - 1] ).doubleValue( );

		double adjustment = 0;
		if ( fraction != 0 )
		{
			double nextValue = ( (Double) sortedObjs[k] ).doubleValue( );
			adjustment = fraction * ( nextValue - value );
		}

		return new Double( value + adjustment );
	}

}
