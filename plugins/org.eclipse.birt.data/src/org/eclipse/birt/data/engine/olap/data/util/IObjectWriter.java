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

package org.eclipse.birt.data.engine.olap.data.util;

import java.io.IOException;

/**
 * 
 */

public interface IObjectWriter
{

	/**
	 * Write a java object to a file.
	 * 
	 * @param file
	 * @param obj
	 * @throws IOException
	 */
	public void write( BufferedRandomAccessFile file, Object obj )
			throws IOException;
}
