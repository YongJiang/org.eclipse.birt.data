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
package org.eclipse.birt.data.engine.api;

import java.math.BigDecimal;
import java.sql.Blob;
import java.util.Date;

import org.eclipse.birt.core.exception.BirtException;
import org.mozilla.javascript.Scriptable;

/**
 * An iterator on a result set from a prepared and executed query.
 * Multiple <code>IResultIterator</code> objects could be associated with
 * the same <code>IQueryResults</code> object, such as in the case of an ODA data set
 * capable of producing multiple result sets.
 */
public interface IResultIterator
{
    /**
     * Returns the {@link org.eclipse.birt.data.engine.api.IQueryResults} 
     * from which this result iterator is obtained. If this iterator
     * is that of a subquery, null is returned. 
     */
    public IQueryResults getQueryResults();

    /**
     * Returns the JavaScript scope associated with this result iterator.
     * All JavaScript result objects, e.g. rows, row, specific to this
     * result set are defined within this scope.    
     * The returned scope is the same as the one passed to <code>IPreparedQuery.excute()</code>
     * which produced this iterator's <code>IQueryResults</code>.
     * @return	The JavaScript scope associated to this result iterator.
     */
    public Scriptable getScope();

    /**
     * Returns the metadata of this result set's detail row.
     * @return	The result metadata of a detail row.
     */
    public IResultMetaData getResultMetaData() throws BirtException;
    
    /**
     * Moves down one element from its current position of the iterator.
     * This method applies to a result whose ReportQuery is defined to 
     * use detail or group rows. 
     * @return 	true if next element exists and 
     * 			has not reached the limit on the maximum number of rows 
     * 			that can be accessed. 
     * @throws 	BirtException if error occurs in Data Engine
     */
    public boolean next() throws BirtException;
    
    /**
	 * Each row has its an ID associated with it, and this id will never be
	 * changed no matter when the query is running against a data set or against
	 * a report document.
	 * 
	 * @since 2.1
	 * @return row id of current row
	 * @throws BirtException
	 *             if error occurs in Data Engine
	 */
	public int getRowId( ) throws BirtException;
	
	/**
	 * Each row has its own index, which indicates this row position in the
	 * result set. This method retrieves current row index. The row index is 0
	 * based, and -1 is returned when there is no current row.
	 * 
	 * @since 2.1
	 * @return row index of current row
	 * @throws BirtException
	 *             if error occurs in Data Engine
	 */
	public int getRowIndex( ) throws BirtException;

	/**
	 * Moves iterator to the row with given absolute index. Valid index must be
	 * both not less than current row index and not great than the maximum row
	 * index. Presently backward see is not supportted.
	 * 
	 * @since 2.1
	 * @param rowIndex,
	 *            which index needs to advance to
	 * @throws BirtException,
	 *             if rowIndex is invalid
	 */
	public void moveTo( int rowIndex ) throws BirtException;

    /**
     * Returns the value of a query result expression. 
     * A given data expression could be for one of the Selected Columns
     * (if detail rows are used), or of an Aggregation specified 
     * in the prepared ReportQueryDefn spec.
     * When requesting for the value of a Selected Column, its value
     * in the current row of the iterator will be returned.
     * <p>
     * Throws an exception if a result expression value is requested 
     * out of sequence from the prepared <code>IQueryDefinition</code> spec.  
     * E.g. A group aggregation is defined to be after_last_row. 
     * It would be out of sequence if requested before having
     * iterated/skipped to the last row of the group. 
     * In future release, this could have intelligence to auto recover 
     * and performs dependent operations to properly evaluate 
     * any out-of-sequence result values. 
     * @param dataExpr 	An <code>IBaseExpression</code> object provided in
     * 					the <code>IQueryDefinition</code> at the time of prepare.
     * @return			The value of the given expression.
     * 					It could be null.
     * @throws 			BirtException if error occurs in Data Engine
     * @deprecated
     */
    public Object getValue( IBaseExpression dataExpr ) throws BirtException;

    /**
     * Returns the value of a query result expression as a Boolean,
     * by type casting the Object returned by getValue.
     * <br>
     * A convenience method for the API consumer.
     * <br>
     * If the expression value has an incompatible type,  
     * a <code>DataException</code> is thrown at runtime.
     * @param dataExpr 	An <code>IBaseExpression</code> object provided in
     * 					the <code>IQueryDefinition</code> at the time of prepare.
     * @return			The value of the given expression as a Boolean.
     * 					It could be null.
     * @throws 			BirtException if error occurs in Data Engine
     * @deprecated
     */
    public Boolean getBoolean( IBaseExpression dataExpr ) throws BirtException;

    /**
     * Returns the value of a query result expression as an Integer,
     * by type casting the Object returned by getValue.
     * <br>
     * A convenience method for the API consumer.
     * <br>
     * If the expression value has an incompatible type,  
     * a <code>DataException</code> is thrown at runtime.
     * @param dataExpr 	An <code>IBaseExpression</code> object provided in
     * 					the <code>IQueryDefinition</code> at the time of prepare.
     * @return			The value of the given expression as an Integer.
     * 					It could be null.
     * @throws 			BirtException if error occurs in Data Engine
     * @deprecated
     */
    public Integer getInteger( IBaseExpression dataExpr ) throws BirtException;

    /**
     * Returns the value of a query result expression as a Double,
     * by type casting the Object returned by getValue.
     * <br>
     * A convenience method for the API consumer.
     * <br>
     * If the expression value has an incompatible type,  
     * a <code>DataException</code> is thrown at runtime.
     * @param dataExpr 	An <code>IBaseExpression</code> object provided in
     * 					the <code>IQueryDefinition</code> at the time of prepare.
     * @return			The value of the given expression as a Double.
     * 					It could be null.
     * @throws 			BirtException if error occurs in Data Engine
     * @deprecated
     */
    public Double getDouble( IBaseExpression dataExpr ) throws BirtException;

    /**
     * Returns the value of a query result expression as a String,
     * by type casting the Object returned by getValue.
     * <br>
     * A convenience method for the API consumer.
     * <br>
     * If the expression value has an incompatible type,  
     * a <code>DataException</code> is thrown at runtime.
     * @param dataExpr 	An <code>IBaseExpression</code> object provided in
     * 					the <code>IQueryDefinition</code> at the time of prepare.
     * @return			The value of the given expression as a String.
     * 					It could be null.
     * @throws 			BirtException if error occurs in Data Engine
     * @deprecated
     */
    public String getString( IBaseExpression dataExpr ) throws BirtException;

    /**
     * Returns the value of a query result expression as a BigDecimal,
     * by type casting the Object returned by getValue.
     * <br>
     * A convenience method for the API consumer.
     * <br>
     * If the expression value has an incompatible type,  
     * a <code>DataException</code> is thrown at runtime.
     * @param dataExpr 	An <code>IBaseExpression</code> object provided in
     * 					the <code>IQueryDefinition</code> at the time of prepare.
     * @return			The value of the given expression as a BigDecimal.
     * 					It could be null.
     * @throws 			BirtException if error occurs in Data Engine
     * @deprecated
     */
    public BigDecimal getBigDecimal( IBaseExpression dataExpr ) throws BirtException;

    /**
     * Returns the value of a query result expression as a Date,
     * by type casting the Object returned by getValue.
     * <br>
     * A convenience method for the API consumer.
     * <br>
     * If the expression value has an incompatible type,  
     * a <code>DataException</code> is thrown at runtime.
     * @param dataExpr 	An <code>IBaseExpression</code> object provided in
     * 					the <code>IQueryDefinition</code> at the time of prepare.
     * @return			The value of the given expression as a Date.
     * 					It could be null.
     * @throws 			BirtException if error occurs in Data Engine
     * @deprecated
     */
    public Date getDate( IBaseExpression dataExpr ) throws BirtException;

    /**
     * Returns the value of a query result expression 
     * representing Blob data.
     * <br>
     * If the expression value has an incompatible type,  
     * a <code>DataException</code> is thrown at runtime.
     * @param dataExpr 	An <code>IBaseExpression</code> object provided in
     * 					the <code>IQueryDefinition</code> at the time of prepare.
     * @return			The value of the given Blob expression.
     * 					It could be null.
     * @throws 			BirtException if error occurs in Data Engine
     * @deprecated
     */
    public Blob getBlob( IBaseExpression dataExpr ) throws BirtException;

    /**
     * Returns the value of a query result expression 
     * representing Binary data.
     * <br>
     * If the expression value has an incompatible type,  
     * a <code>DataException</code> is thrown at runtime.
     * @param dataExpr 	An <code>IBaseExpression</code> object provided in
     * 					the <code>IQueryDefinition</code> at the time of prepare.
     * @return			The value of the given Blob expression.
     * 					It could be null.
     * @throws 			BirtException if error occurs in Data Engine
     * @deprecated
     */
    public byte[] getBytes( IBaseExpression dataExpr ) throws BirtException;
    
    /**
	 * Returns the value of a bound column. Currently it is only a dummy
	 * implementation.
	 * 
	 * @param name of bound column
	 * @return value of bound column
	 * @throws BirtException
	 */
    public Object getValue( String name ) throws BirtException;
    
    /**
	 * Returns the value of a bound column as the Boolean data type.
	 * Currently it is only a dummy implementation.
	 * 
	 * @param name of bound column
	 * @return value of bound column
	 * @throws BirtException
	 */
    public Boolean getBoolean( String name ) throws BirtException;

    /**
	 * Returns the value of a bound column as the Integer data type.
	 * Currently it is only a dummy implementation.
	 * 
	 * @param name of bound column
	 * @return value of bound column
	 * @throws BirtException
	 */
    public Integer getInteger( String name ) throws BirtException;

    /**
	 * Returns the value of a bound column as the Double data type.
	 * Currently it is only a dummy implementation.
	 * 
	 * @param name of bound column
	 * @return value of bound column
	 * @throws BirtException
	 */
    public Double getDouble( String name ) throws BirtException;

    /**
	 * Returns the value of a bound column as the String data type.
	 * Currently it is only a dummy implementation.
	 * 
	 * @param name of bound column
	 * @return value of bound column
	 * @throws BirtException
	 */
    public String getString( String name ) throws BirtException;

    /**
	 * Returns the value of a bound column as the BigDecimal data type.
	 * Currently it is only a dummy implementation.
	 * 
	 * @param name of bound column
	 * @return value of bound column
	 * @throws BirtException
	 */
    public BigDecimal getBigDecimal( String name ) throws BirtException;

    /**
	 * Returns the value of a bound column as the Date data type. Currently
	 * it is only a dummy implementation.
	 * 
	 * @param name of bound column
	 * @return value of bound column
	 * @throws BirtException
	 */
    public Date getDate( String name ) throws BirtException;

    /**
	 * Returns the value of a bound column as the Blob data type. Currently
	 * it is only a dummy implementation.
	 * 
	 * @param name of bound column
	 * @return value of bound column
	 * @throws BirtException
	 */
    public Blob getBlob( String name ) throws BirtException;

    /**
	 * Returns the value of a bound column as the byte[] data type.
	 * Currently it is only a dummy implementation.
	 * 
	 * @param name of bound column
	 * @return value of bound column
	 * @throws BirtException
	 */
    public byte[] getBytes( String name ) throws BirtException;
    
    /**
     * Advances the iterator, skipping rows to the last row in the current group 
     * at the specified group level.
     * This is for result sets that do not use detail rows to advance
     * to next group.  Calling next() after skip() would position 
     * the current row to the first row of the next group.
     * @param groupLevel	An absolute value for group level. 
     * 						A value of 0 applies to the whole result set.
     * @throws 			BirtException if error occurs in Data Engine
     */
    public void skipToEnd( int groupLevel ) throws BirtException;

    /**
     * Returns the 1-based index of the outermost group
     * in which the current row is the first row. 
     * For example, if a query contain N groups 
     * (group with index 1 being the outermost group, and group with 
     * index N being the innermost group),
     * and this function returns a value M, it indicates that the 
     * current row is the first row in groups with 
     * indexes (M, M+1, ..., N ).
     * @return	1-based index of the outermost group in which 
     * 			the current row is the first row;
     * 			(N+1) if the current row is not at the start of any group;
     * 			0 if the result set has no groups.
     */
    public int getStartingGroupLevel() throws BirtException;

    /**
     * Returns the 1-based index of the outermost group
     * in which the current row is the last row. 
     * For example, if a query contain N groups 
     * (group with index 1 being the outermost group, and group with 
     * index N being the innermost group),
     * and this function returns a value M, it indicates that the 
     * current row is the last row in groups with 
     * indexes (M, M+1, ..., N ). 
     * @return	1-based index of the outermost group in which 
     * 			the current row is the last row;
     * 			(N+1) if the current row is not at the end of any group;
     * 			0 if the result set has no groups.
     */
    public int getEndingGroupLevel() throws BirtException;

    /**
     * Returns the secondary result specified by a sub query 
     * that was defined in the prepared <code>IQueryDefinition</code>.
     * @throws 			DataException if error occurs in Data Engine
     * @param subQueryName name of sub query which defines the secondary result set
     * @param scope Javascript scope to be associated with the secondary result set
     */
    public IResultIterator getSecondaryIterator( String subQueryName, Scriptable scope ) 
    		throws BirtException;

    /** 
     * Closes this result and any associated secondary result iterator(s),  
     * providing a hint that the consumer is done with this result,
     * whose resources can be safely released as appropriate.  
     * @throws BirtException 
     */
    public void close() throws BirtException;
    
    /**
    * Move the current position of the iterator to the first element of the group with matching group key values.
    * To locate the [n]th inner group, values for all outer groups�? keys need to be provided in the array
    * groupKeyValues. groupKeyValue[0] is the key value for group 1 (outermost group), groupKeyValue[1] is the key value for
    * group 2, etc. 
    * 
    * @param groupKeyValues Values of group keys 
    * @return true if group located successfully and cursor is re-positioned. False if no group is found to match
    * the group key values exactly, and iterator cursor is not moved.
     * @throws DataException 
    */
    public boolean findGroup( Object[] groupKeyValues ) throws BirtException;

}