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
 *	A generic result class that defines the metadata 
 *	of an IResultObject instance returned by a query's
 *  underlying data source.
 *	<p> 
 *  The metadata is defined based on a query's runtime metadata
 *  (as described by its data source driver),
 *  merging with static result hints specified in a IDataSourceQuery.
 *  It includes projected columns only, which are all columns
 *  returned by a query if no explicit projection is specified. <br>
 *  It also describes any computed columns and
 *  custom columns declared in an IDataSourceQuery. <br> 
 *  A field index is 1-based for the data fields.
 */
public interface IResultClass
{
	/**
	 * Return the number of fields declared in this result class definition.
	 * @return	The number of data fields declared.
	 */
	public int getFieldCount();
	
	/**
	 * Return a list of field names declared in this result class definition.	
	 * @return	An array of the names of fields.  The sequence of fields in 
	 * 			array has no determined meaning, and could change when its
	 * 			associated result class definition changes.
	 * 			An empty array is returned if no field is known.
	 */
	public String[] getFieldNames();

	/**
	 * Gets the name of the field at the specified index position. 
	 * @param index		The 1-based index of a field.
	 * @return			The name of the specified field.
	 * @throws DataException	if given index is invalid.
	 */
	public String getFieldName( int index ) throws DataException;

	/**
	 * Gets the alias of the field at the specified index position.
	 * An alias is given to a field as a programmatic convenience.
	 * A field can be referred using a name or an alias interchangeably. 
	 * @param index		The 1-based index of a field.
	 * @return			The alias of the specified field.
	 * 					Null if none is defined.
	 * @throws DataException	if given index is invalid.
	 */
	public String getFieldAlias( int index ) throws DataException;

	/**
	 * Gets the index of a field with given name or alias. 
	 * -1 is returned if given name or alias is not found.
	 * @param fieldName The name or alias of a field.
	 * @return The 1-based index of the field if found, -1 otherwise.
	 */
	public int getFieldIndex( String fieldName );
	
	/**
	 * Returns the class of the value of the given field, 
	 * specified by name.
	 * @param fieldName		The name or alias of a field.
	 * @return				The class of the field value.
	 * @throws DataException	if given fieldName is invalid.
	 */
	public Class getFieldValueClass( String fieldName ) throws DataException;
	
	/**
	 * Returns the class of the value of the given field, 
	 * specified by its index position.
	 * @param index		The 1-based index of a field.
	 * @return			The class of the field value.
	 * @throws DataException	if given index is invalid.
	 */
	public Class getFieldValueClass( int index ) throws DataException;
			
	/**
	 * Indicates whether the specified field is a custom user-defined field.
	 * Only those fields declared dynamically via 
	 * IDataSourceQuery.declareCustomField() are considered 
	 * "custom" fields.
	 * @param fieldName		The name or alias of a field.
	 * @return				true if the given field is a custom field;
	 * 						false otherwise
	 * @throws DataException	if given fieldName is invalid.
	 */
	public boolean isCustomField( String fieldName ) throws DataException;
	
	/**
	 * Indicates whether the specified field is a custom user-defined field.
	 * Only those fields declared dynamically via 
	 * IDataSourceQuery.declareCustomField() are considered 
	 * "custom" fields.
	 * @param index		The 1-based index of a field.
	 * @return			true if the given field is a custom field;
	 * 					false otherwise
	 * @throws DataException	if given index is invalid.
	 */
	public boolean isCustomField( int index ) throws DataException;

	/**
	 * Gets the label or display name of the field at
	 * the specified index position.  
	 * The label is display-friendly, and can be localized.
	 * Its value is not used directly by the Data Engine.
	 * @param index	The 1-based index of a field.
	 * @return		The label of the specified field;
	 * 				null if none is defined.
	 * @throws DataException	if given index is invalid.
	 */
	public String getFieldLabel( int index ) throws DataException;

	/**
	 * Gets the native type name of the field at the specified index 
	 * position.
	 * @param index	The 1-based index of a field.
	 * @return		The native type name of the specified field; 
	 * 				null if none is defined.
	 * @throws DataException	if given index is invalid.
	 */
	public String getFieldNativeTypeName( int index ) throws DataException;
}
