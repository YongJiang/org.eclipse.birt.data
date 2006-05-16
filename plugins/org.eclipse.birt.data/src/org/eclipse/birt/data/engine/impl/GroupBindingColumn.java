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
package org.eclipse.birt.data.engine.impl;

import java.util.Map;
import java.util.Set;

import org.eclipse.birt.data.engine.api.IBaseExpression;

/**
 * 
 */
class GroupBindingColumn
{
	//
	private int groupLevel;
	private String groupKey;
	private Map bindings;
		
	/**
	 * 
	 * @param bindings
	 * @param groupLevel
	 */
	GroupBindingColumn( String groupKey, int groupLevel, Map bindings )
	{
		this.groupKey = groupKey;
		this.groupLevel = groupLevel;
		this.bindings = bindings;
	}
		
	/**
	 * 
	 * @return
	 */
	String getGroupKey()
	{
		return this.groupKey;
	}
	
	/**
	 * 
	 * @return
	 */
	int getGroupLevel()
	{
		return this.groupLevel;
	}
	
	/**
	 * 
	 * @return
	 */
	Set getColumnNames()
	{
		return this.bindings.keySet( );
	}
	
	/**
	 * 
	 * @param name
	 * @return
	 */
	IBaseExpression getExpression( String name )
	{
		return (IBaseExpression) this.bindings.get( name );
	}
	
}
