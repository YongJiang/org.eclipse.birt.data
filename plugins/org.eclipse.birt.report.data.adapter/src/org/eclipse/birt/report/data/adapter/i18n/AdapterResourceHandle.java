/*******************************************************************************
 * Copyright (c) 2004 Actuate Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Actuate Corporation  - initial API and implementation
 *******************************************************************************/

package org.eclipse.birt.report.data.adapter.i18n;

import org.eclipse.birt.core.i18n.ResourceHandle;

import com.ibm.icu.util.ULocale;

/**
 * Implementation of ResourceHandle in DtE project
 */
public class AdapterResourceHandle extends ResourceHandle
{
	private static AdapterResourceHandle resourceHandle;
	
	/**
	 * @param locale
	 */
	private AdapterResourceHandle( ULocale locale )
	{
		super( locale );
	}
	
	/**
	 * @return the DataResourceHandle with default ULocale
	 */
	public synchronized static AdapterResourceHandle getInstance( )
	{
		if ( resourceHandle == null )
			resourceHandle = new AdapterResourceHandle( ULocale.getDefault( ) );

		return resourceHandle;
	}
	
}
