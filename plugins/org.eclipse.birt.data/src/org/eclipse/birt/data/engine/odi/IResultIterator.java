/*
 *************************************************************************
 * Copyright (c) 2004 Actuate Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Actuate Corporation  - initial API and implementation
 *  
 *************************************************************************
 */

package org.eclipse.birt.data.engine.odi;

import org.eclipse.birt.data.engine.core.DataException;

/**
 * An iterator that an user can iterate to get 
 * query result objects returned by an executed query. <p>
 * When an iterator is first obtained, the current position is
 * placed at the first row. <p>
 * The iterator is capable of detecting breaks within the groups 
 * defined in the IQuery.
 * It can also be used to associate with an ICandidateQuery object 
 * based on a group of rows in this result set.
 * Independent set of transforms can be applied to the result candidates, 
 * to provide an alternative view of the data contained 
 * in this result set. 
 */
public interface IResultIterator
{
    /**
     * Gets the metadata of the associated result instances.
     * @return	The IResultClass instance that represents the
     * 			metadata of the query result instances.
     */
    public IResultClass getResultClass() throws DataException;
    
    /**
     * Moves down one element from its current position of the iterator. 
     * @return 	true if next element exists and 
     * 			has not reached the limit on the maximum number of rows 
     * 			that can be accessed. 
     * @throws 	DataException if error occurs in Data Engine
     */
    public boolean next() throws DataException;
    
    /**
     * Moves the current position to the first element in the current group
     * at the specified grouping level. A grouping level is specified as an integer
     * which is 1-based index of its associated group key defined in IQuery. Therefore
     * the outermost group has an grouping level of 1, its immediate inner group has a
     * grouping level of 2, etc. A grouping level of 0 denotes the entire result set,
     * i.e., first(0) returns the current position to the first row of the result set.
     * @param groupingLevel The 1-based index of group 
     * @throws DataException
     */
    public void first( int groupingLevel ) throws DataException;
    
    /**
     * Moves the current position to the last element in the current group at
     * the specified grouping level. 
     * @param groupingLevel The 1-based index of group. If 0, move current position to 
     *   last row of the result set 
     * @throws DataException
     */
    public void last( int groupingLevel ) throws DataException;

    /**
     * Gets the result object in the current position of the iterator. 
     * Null is returned if no such element exists 
     * (i.e., current position is beyond the end of the last row).
     * @return IResultObject at the current position of the iterator.
     */
    public IResultObject getCurrentResult() throws DataException;

    /**
     * Gets the index of current result object within the result set.
     * The index starts at 0.
     * @return Id of the current object as an integer.
     * @throws DataException if iterator has no current object
     */
    public int getCurrentResultIndex() throws DataException;
    
    /**
     * Gets the index of the current group at the specified group level.
     * The index starts at 0  
     */
    public int getCurrentGroupIndex( int groupLevel ) throws DataException;
    
    /**
     * Returns the 1-based index of the outermost group
     * in which the current row is the first row. 
     * For example, if a query contain N groups 
     * (group with index 1 being the outermost group, and group with 
     * index N being the innermost group),
     * and this function returns a value M, it indicates that the 
     * current row is the first row in groups with 
     * indexes (M, M+1, ..., N ).
     * @return	The 1-based index of the outermost group in which 
     * 			the current row is the first row;
     * 			(N+1) if the current row is not at the start of any group;
     */
    public int getStartingGroupLevel() throws DataException;

    /**
     * Returns the 1-based index of the outermost group
     * in which the current row is the last row. 
     * For example, if a query contain N groups 
     * (group with index 1 being the outermost group, and group with 
     * index N being the innermost group),
     * and this function returns a value M, it indicates that the 
     * current row is the last row in groups with 
     * indexes (M, M+1, ..., N ). 
     * @return	The 1-based index of the outermost group in which 
     * 			the current row is the last row;
     * 			(N+1) if the current row is not at the end of any group;
     */
    public int getEndingGroupLevel() throws DataException;
 
    /** 
     * Closes this result iterator and any associated resources,  
     * providing a hint that the consumer is done with this result,
     * whose resources can be safely released as appropriate.
     * This instance of IResultIterator can no
     * longer be used after it is closed.
     */
    public void close();
    
    /**
     * @param groupLevel
     * @return
     */
    public int[] getGroupStartAndEndIndex( int groupLevel ) throws DataException;

    /**
	 * @return the count of rows
	 */
	public int getRowCount( ) throws DataException;

}
