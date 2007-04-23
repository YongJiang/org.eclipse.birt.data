
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
package org.eclipse.birt.data.engine.olap.data.impl;


/**
 * 
 */

public class Range
{

	/**
	 * 
	 * @param start
	 * @param end
	 */
	public Range( Object start, Object end )
	{
		this.start = start;
		this.end = end;
	}
	private Object start;
	private Object end;
	
	public Object getStart( )
	{
		return start;
	}
	
	public void setStart( Object start )
	{
		this.start = start;
	}
	
	public Object getEnd( )
	{
		return end;
	}
	
	public void setEnd( Object end )
	{
		this.end = end;
	}
}
