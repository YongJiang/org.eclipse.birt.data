
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
package org.eclipse.birt.data.engine.olap.api.query;

import org.eclipse.birt.data.engine.api.IBaseExpression;



public interface ICubeElementFactory
{

	static final String CUBE_ELEMENT_FACTORY_CLASS_NAME = "org.eclipse.birt.data.engine.olap.impl.query.CubeElementFactory";

	/**
	 * 
	 * @param name
	 * @return
	 */
	public ICubeQueryDefinition createCubeQuery( String name );
	
	/**
	 * 
	 * @param filterExpr
	 * @param targetLevel
	 * @param axisQulifierLevel
	 * @param axisQulifierValue
	 * @return
	 */
	public ICubeFilterDefinition creatCubeFilterDefinition(
			IBaseExpression filterExpr, ILevelDefinition targetLevel,
			ILevelDefinition[] axisQulifierLevel, Object[] axisQulifierValue );

	/**
	 * 
	 * @param filterExpr
	 * @param targetLevel
	 * @param axisQulifierLevel
	 * @param axisQulifierValue
	 * @param sortDirection
	 * @return
	 */
	public ICubeSortDefinition createCubeSortDefinition( String filterExpr,
			ILevelDefinition targetLevel, ILevelDefinition[] axisQulifierLevel,
			Object[] axisQulifierValue, int sortDirection );

	/**
	 * 
	 * @param dimensionName
	 * @param hierarchyName
	 * @param levelName
	 * @return
	 */
	public ILevelDefinition createLevel( String dimensionName,
			String hierarchyName, String levelName );

}