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

package org.eclipse.birt.data.engine.executor.cache;

import org.eclipse.birt.data.engine.core.DataException;
import org.eclipse.birt.data.engine.odi.IResultObject;

/**
 * A class caches the data of result set, in which filter and sorting on row
 * will be done. This class enables that external caller can do further data
 * process such as data grouping without caring how data is cached and retrieved
 * from memory, disk file or other data source.
 */
public interface ResultSetCache
{

	/**
	 * @return current result index, 0-based
	 * @throws DataException
	 */
	public int getCurrentIndex( ) throws DataException;

	/**
	 * @return current result object
	 * @throws DataException
	 */
	public IResultObject getCurrentResult( ) throws DataException;

	/**
	 * Follows the convention of java.sql. The currRowIndex is initialized to
	 * -1, and only after next is called once, the pointer will move to the real
	 * data.
	 * 
	 * @return true, if the new current row is valid
	 * @throws DataException
	 */
	public boolean next( ) throws DataException;

	/**
	 * Move the cursor to the next result object, and then fetch its data
	 * 
	 * @return next result object, null indicates beyond the end of result set
	 * @throws DataException
	 */
	public IResultObject fetch( ) throws DataException;

	/**
	 * Move row index to specified position. this function should be called with
	 * care, since it might need to consume a lot of time when disk-based data
	 * manuipulation is used.
	 * 
	 * @param destIndex
	 * @throws DataException
	 */
	public void moveTo( int destIndex ) throws DataException;

	/**
	 * @return count of result objects
	 */
	public int getCount( );

	/**
	 * Reset the current index to -1
	 */
	public void reset( ) throws DataException;

	/**
	 * Close result cache, and do clean up work here. As for DiskCache, the
	 * temporary file will be deleted. So it is important to call this method
	 * when this cache will not be used any more.
	 */
	public void close( );

}