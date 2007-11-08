
/*******************************************************************************
 * Copyright (c) 2004, 2007 Actuate Corporation.
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
import org.eclipse.birt.data.engine.core.DataException;

/**
 * This interface defines the API for computed measure.
 */

public interface IComputedMeasureDefinition extends IMeasureDefinition
{
	/**
	 * Return the expression of computed measure.
	 * 
	 * @return
	 * @throws DataException
	 */
	public IBaseExpression getExpression( ) throws DataException;
	
	/**
	 * Return the type of computed measure.
	 * 
	 * @return
	 * @throws DataException
	 */
	public int getType() throws DataException;
}
