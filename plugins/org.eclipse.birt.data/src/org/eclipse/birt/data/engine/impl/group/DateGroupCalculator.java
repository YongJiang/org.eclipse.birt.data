
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
package org.eclipse.birt.data.engine.impl.group;

import java.util.Date;

import org.eclipse.birt.core.data.DataTypeUtil;
import org.eclipse.birt.core.exception.BirtException;

import com.ibm.icu.util.Calendar;

/**
 * This calculator is used to calculate a datetime group key basing group interval.
 */

abstract class DateGroupCalculator extends GroupCalculator
{
	
	static protected Date defaultStart;
	
	private int range;
	
	static
	{
		Calendar c = Calendar.getInstance( );
		c.clear( );
		c.set( 1970, 0, 1 );
		defaultStart = c.getTime( );
	}
	
	/**
	 * 
	 * @param intervalStart
	 * @param intervalRange
	 * @throws BirtException
	 */
	public DateGroupCalculator(Object intervalStart, double intervalRange) throws BirtException
	{
		super( intervalStart, intervalRange );
		range = (int)Math.round( intervalRange );
		range = (range == 0 ? 1 : range);
		if ( intervalStart != null )
			this.intervalStart = DataTypeUtil.toDate( intervalStart );
	}
	
	/**
	 * 
	 * @return
	 */
	protected int getDateIntervalRange()
	{
		return range;
	}
	
	protected Date getDate( Object value ) throws BirtException
	{
		return DataTypeUtil.toDate( value );
	}
}
