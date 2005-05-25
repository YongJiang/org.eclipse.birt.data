/*
 *****************************************************************************
 * Copyright (c) 2004, 2005 Actuate Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Actuate Corporation  - initial API and implementation
 *
 ******************************************************************************
 */ 

package org.eclipse.birt.data.engine.odaconsumer;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.birt.data.engine.core.DataException;
import org.eclipse.birt.data.engine.executor.ResultFieldMetadata;
import org.eclipse.birt.data.engine.i18n.ResourceConstants;

class ProjectedColumns
{
	private ArrayList m_columns;
	private int[] m_projectedIndices;
	private int m_baseColumnMetadataCount;
	
	// hold these values in case we need them for re-creating another 
	// ProjectedColumns with a different set of runtime metadata
	private ArrayList m_customColumns;
	private ArrayList m_columnHints;
	private String[] m_projectedColumns;

	// trace logging variables
	private static String sm_className = ProjectedColumns.class.getName();
	private static String sm_loggerName = ConnectionManager.sm_packageName;
	private static Logger sm_logger = Logger.getLogger( sm_loggerName );
	
	ProjectedColumns( ResultSetMetaData runtimeMetaData ) throws DataException
	{
		String methodName = "ProjectedColumns";
		sm_logger.entering( sm_className, methodName, runtimeMetaData );

		assert( runtimeMetaData != null );
		m_columns = new ArrayList();
		m_baseColumnMetadataCount = runtimeMetaData.getColumnCount();
		
		for( int i = 1; i <= m_baseColumnMetadataCount; i++ )
		{
			String name = runtimeMetaData.getColumnName( i );
			String label = runtimeMetaData.getColumnLabel( i );
			Class driverDataType = runtimeMetaData.getColumnTypeAsJavaClass( i );
			String nativeTypeName = runtimeMetaData.getColumnNativeTypeName( i );
			ResultFieldMetadata column = 
					new ResultFieldMetadata( i, name, label, 
					        				 driverDataType, nativeTypeName, 
											 false /* isCustom */ );
			column.setDriverProvidedDataType( driverDataType );
			
			m_columns.add( column );
		}

		sm_logger.exiting( sm_className, methodName, this );
	}
	
	// it's possible that a hint may not match any of the runtime metadata, 
	// it's not an error when that happens
	void addHint( ColumnHint columnHint ) throws DataException
	{
		String methodName = "addHint";
		sm_logger.entering( sm_className, methodName, columnHint );

		assert( columnHint != null );
		
		int columnPosition = columnHint.getPosition();
		if( columnPosition > 0 && columnPosition <= m_columns.size() )
		{	
			// the 1-based position of the column hint is for mapping to the 
			// corresponding position in the runtime metadata, which is stored 
			// in a 0-based m_columns array.
			int driverIndex = columnPosition - 1;
			ResultFieldMetadata column = 
				(ResultFieldMetadata) m_columns.get( driverIndex );
			
			// make sure we validate everything before updating anything
			String columnHintAlias = columnHint.getAlias();
			if( columnHintAlias != null )
				validateNewNameOrAlias( columnHintAlias, driverIndex );
			
			String columnName = column.getName();
			if( columnName == null || columnName.length() == 0 )
			{
				String columnHintName = columnHint.getName();
				validateNewNameOrAlias( columnHintName, driverIndex );
				column.setName( columnHintName );
			}
			
			updateDataTypeAndAlias( column, columnHintAlias, 
			                        columnHint.getDataType() );
		}
		else
		{
			// no column position was specified, so match the column hint and 
			// runtime metadata columns by name if possible
			String columnHintName = columnHint.getName();
			for( int i = 0, n = m_columns.size(); i < n; i++ )
			{
				ResultFieldMetadata column = 
					(ResultFieldMetadata) m_columns.get( i );
				if( column.getName().equals( columnHintName ) )
				{
					String columnHintAlias = columnHint.getAlias();
					if( columnHintAlias != null )
						validateNewNameOrAlias( columnHintAlias, i );
					updateDataTypeAndAlias( column, columnHintAlias, 
					                        columnHint.getDataType() );
				}
			}
		}
		
		doGetColumnHints().add( columnHint );
		
		sm_logger.exiting( sm_className, methodName );
	}

	private void updateDataTypeAndAlias( ResultFieldMetadata column,
									  	 String columnHintAlias, 
									  	 Class columnHintType )
	{
		String methodName = "updateDataTypeAndAlias";

		// accepts column hint's data type only if the driver
		// cannot provide a data type
		if( column.getDriverProvidedDataType() == null && 
		    columnHintType != null )
			column.setDataType( columnHintType );
		
		column.setAlias( columnHintAlias );

		if( sm_logger.isLoggable( Level.FINER ) )
		    sm_logger.logp( Level.FINER, sm_className, methodName, 
				"Updated columns to data type: {0} , alias: {1}.", 
				new Object[] { columnHintType, columnHintAlias } );
	}

	private ArrayList doGetColumnHints()
	{
		if( m_columnHints == null )
			m_columnHints = new ArrayList();
		
		return m_columnHints;
	}
	
	void addCustomColumn( String columnName, Class columnType )
		throws DataException
	{
		String methodName = "addCustomColumn";
		sm_logger.entering( sm_className, methodName, columnName );

		assert( columnName != null && columnName.length() > 0 );
		
		// ensure new custom column name doesn't have the same column name 
		// or alias in any existing column
		validateNewNameOrAlias( columnName, -1 /* driverIndex */ );
		
		// Custom column has no driver position; -1 is used for this value
		ResultFieldMetadata column = 
			new ResultFieldMetadata( -1, columnName, columnName, 
			                         columnType, null /* nativeTypeName */, 
									 true /* isCustom */ );
		m_columns.add( column );
		
		doGetCustomColumns().add( new PreparedStatement.CustomColumn( columnName,
		                                                              columnType ) );
		
		sm_logger.exiting( sm_className, methodName, column );
	}
	
	private ArrayList doGetCustomColumns()
	{
		if( m_customColumns == null )
			m_customColumns = new ArrayList();
		
		return m_customColumns;
	}

	void setProjectedNames( String[] projectedColumns ) throws DataException
	{
		String methodName = "setProjectedNames";
		sm_logger.entering( sm_className, methodName, projectedColumns );

		// can project since declared custom columns don't need to be 
		// projected to be added to the IResultClass.  this allows us 
		// to validate the projection list right away.
		if( projectedColumns == null )
			projectAllBaseColumns();
		else
			projectSelectedBaseColumns( projectedColumns );
		
		m_projectedColumns = projectedColumns;

		sm_logger.exiting( sm_className, methodName );
	}
	
	// returns the projected columns based on the runtime 
	// metadata, column hints, and the projected column names.
	// returns an empty List if there are no projected columns.
	List getColumnsMetadata()
	{
		String methodName = "getColumnsMetadata";
		sm_logger.entering( sm_className, methodName );
		
		// if the projected indices array is null, then that 
		// means the caller didn't call setProjectedNames()
		// therefore we want to project all columns then.
		if( m_projectedIndices == null )
			projectAllBaseColumns();
		
		ArrayList projectedColumns = new ArrayList();
		
		// add the base columns that were projected based on 
		// the order in which they were projected
		for( int i = 0, n = m_projectedIndices.length; i < n; i++ )
		{
			int colIndex = m_projectedIndices[i];
			ResultFieldMetadata column = 
				(ResultFieldMetadata) m_columns.get( colIndex );
			projectedColumns.add( column );
		}
		
		// add the custom columns after the projected non-custom
		// columns have been added
		for( int i = m_baseColumnMetadataCount, n = m_columns.size(); i < n; i++ )
		{
			ResultFieldMetadata column = 
				(ResultFieldMetadata) m_columns.get( i );
			projectedColumns.add( column );
		}
		
		sm_logger.exiting( sm_className, methodName, projectedColumns );
		
		return projectedColumns;
	}
	
	private void projectSelectedBaseColumns( String[] projectedColumns ) 
		throws DataException
	{
		String methodName = "projectSelectedBaseColumns";

		// only project non-custom columns
		ArrayList projectedIndices = new ArrayList();
		
		for( int i = 0; i < projectedColumns.length; i++ )
		{
			String projectedName = projectedColumns[i];
			
			// this should have been validated already
			assert( projectedName != null || projectedName.length() == 0 );
			
			int colIndex = findColumnIndex( projectedName );
			
			// only project non-custom columns
			if( colIndex >= 0 && colIndex < m_baseColumnMetadataCount )
			{
				projectedIndices.add( new Integer( colIndex ) );
				continue;
			}
			
			// the caller tried to project a custom column, not an error.
			// but ignore it, since they'll be added to the end of the 
			// IResultClass metadata in the order in which they were declared
			if( colIndex >= m_baseColumnMetadataCount )
				continue;
	
			// couldn't find a match to the projected column name to 
			// the base metadata
			projectedIndices = null;
			sm_logger.logp( Level.SEVERE, sm_className, methodName, 
					"No match found for project column {0} ", projectedName );

			throw new DataException( ResourceConstants.UNRECOGNIZED_PROJECTED_COLUMN_NAME,
			                         new Object[] { projectedName } );
		}
		
		int size = projectedIndices.size();
		m_projectedIndices = new int[size];
		for( int i = 0; i < size; i++ )
			m_projectedIndices[i] = ( (Integer) projectedIndices.get( i ) ).intValue();
	}

	// finds the column index based on the name or alias, returns -1 to indicate 
	// that the name/alias didn't match the runtime metadata
	private int findColumnIndex( String projectedName )
	{
		String methodName = "findColumnIndex";

		int foundIndex = -1;		
		for( int colIndex = 0, n = m_columns.size(); colIndex < n; colIndex++ )
		{
			ResultFieldMetadata column = (ResultFieldMetadata) m_columns.get( colIndex );
			if( projectedName.equals( column.getName() ) ||
				projectedName.equals( column.getAlias() ) )
			{
			    foundIndex = colIndex;
			    break;
			}
		}

		if( sm_logger.isLoggable( Level.FINEST ) )
		    sm_logger.logp( Level.FINEST, sm_className, methodName, 
				"Found column {0} at index {1}.", 
				new Object[] { projectedName, new Integer( foundIndex ) } );
		
		return foundIndex;
	}

	private void projectAllBaseColumns()
	{
		String methodName = "projectAllBaseColumns";

		// only project non-custom columns
		m_projectedIndices = new int[ m_baseColumnMetadataCount ];
		
		for( int i = 0; i < m_baseColumnMetadataCount; i++ )
			m_projectedIndices[i] = i;

		if( sm_logger.isLoggable( Level.FINEST ) )
		    sm_logger.logp( Level.FINEST, sm_className, methodName, 
				"Projected all columns {0} .", m_projectedIndices );
	}

	// validates that the new name/alias from a column hint or a new custom 
	// column doesn't conflict with existing column names or aliases
	private void validateNewNameOrAlias( String newColumnNameOrAlias, 
										 int driverIndex )
		throws DataException
	{
		String methodName = "validateNewNameOrAlias";

		assert( newColumnNameOrAlias != null && 
		        newColumnNameOrAlias.length() > 0 );
		
		for( int i = 0, n = m_columns.size(); i < n; i++ )
		{
			// skip the specified driver index because it would be okay 
			// to find the same name at that index. (i.e. when the 
			// column name is the same as the column alias)
			if( i == driverIndex )
				continue;
			
			ResultFieldMetadata column = 
				(ResultFieldMetadata) m_columns.get( i );
			if( ( column.getName() != null && 
				  column.getName().equals( newColumnNameOrAlias ) ) ||
				( column.getAlias() != null &&
				  column.getAlias().equals( newColumnNameOrAlias ) ) )
			{
			    sm_logger.logp( Level.SEVERE, sm_className, methodName, 
			            "column name or alias {0} is aready used by column {1}", 
						new Object[] { newColumnNameOrAlias, column } );
				throw new DataException( ResourceConstants.COLUMN_NAME_OR_ALIAS_ALREADY_USED,
				        				new Object[] { newColumnNameOrAlias, new Integer( i + 1 ) } );
			}
		}
	}
	
	ArrayList getColumnHints()
	{
		return m_columnHints;
	}
	
	ArrayList getCustomColumns()
	{
		return m_customColumns;
	}
	
	String[] getProjections()
	{
		return m_projectedColumns;
	}
}
