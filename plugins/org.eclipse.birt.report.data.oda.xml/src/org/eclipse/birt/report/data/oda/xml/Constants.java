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
package org.eclipse.birt.report.data.oda.xml;

/**
 * This class hosts all constants used in xml driver.
 */
public final class Constants
{
	public static final int DATA_SOURCE_MAJOR_VERSION = 1;
	public static final int DATA_SOURCE_MINOR_VERSION = 0;
	public static final String DATA_SOURCE_PRODUCT_NAME = "XML-ODA Driver";
	public static final int CACHED_RESULT_SET_LENGTH = 10000;
	
	//The connection proporty that is used to give the relation information string
	//to the driver.
	public static final String CONST_PROP_RELATIONINFORMATION = "RELATIONINFORMATION";
	
	//The connection property that gives the file name(s).Currently we only
	//support single file.
	public static final String CONST_PROP_FILELIST = "FILELIST";
}
