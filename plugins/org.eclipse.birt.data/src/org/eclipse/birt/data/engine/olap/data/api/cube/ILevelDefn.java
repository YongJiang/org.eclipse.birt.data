
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
package org.eclipse.birt.data.engine.olap.data.api.cube;

/**
 * 
 */

public interface ILevelDefn
{
	/**
	 * 
	 * @return
	 */
	public String getLevelName( );
	
	/**
	 * 
	 * @return
	 */
	public String[] getKeyColumns( );

	/**
	 * 
	 * @return
	 */
	public String[] getAttributeColumns( );
	
	/**
	 * 
	 * @param timeType
	 */
	public void setTimeType( String timeType );
	
	/**
	 * 
	 * @return
	 */
	public String getTimeType( );
}
