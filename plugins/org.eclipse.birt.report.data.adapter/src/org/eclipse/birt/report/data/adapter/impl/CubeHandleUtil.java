
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
package org.eclipse.birt.report.data.adapter.impl;

import java.util.Date;

import org.eclipse.birt.report.model.api.olap.DimensionHandle;

/**
 * 
 */

public class CubeHandleUtil
{	
	public static boolean isTimeDimension( DimensionHandle dimension )
	{
		return false;
	}
	
	public static Date getStartTime( DimensionHandle dimension )
	{
		return null;
	}
	
	public static Date getEndTime( DimensionHandle dimension )
	{
		return null;
	}
	
}
