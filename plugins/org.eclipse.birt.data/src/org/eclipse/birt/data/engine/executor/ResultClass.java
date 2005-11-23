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

package org.eclipse.birt.data.engine.executor;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eclipse.birt.data.engine.core.DataException;
import org.eclipse.birt.data.engine.executor.cache.IOUtil;
import org.eclipse.birt.data.engine.i18n.ResourceConstants;
import org.eclipse.birt.data.engine.odi.IResultClass;

/**
 * <code>ResultClass</code> contains the metadata about 
 * the projected columns in the result set.
 */
public class ResultClass implements IResultClass
{
	private List m_projectedColumns;
	private HashMap m_nameToIdMapping;
	private String[] m_fieldNames;
	private int[] m_fieldDriverPositions;
	
	public ResultClass( List projectedColumns )
	{	
		assert( projectedColumns != null );
		
		initColumnsInfo( projectedColumns );
	}
	
	/**
	 * @param projectedColumns
	 */
	private void initColumnsInfo( List projectedColumns )
	{
		m_projectedColumns = new ArrayList( );
		m_projectedColumns.addAll( projectedColumns );
		m_nameToIdMapping = new HashMap( );

		for ( int i = 0, n = projectedColumns.size( ); i < n; i++ )
		{
			ResultFieldMetadata column = (ResultFieldMetadata) projectedColumns.get( i );

			String upperCaseName = column.getName( );
			if ( upperCaseName != null )
				upperCaseName = upperCaseName.toUpperCase( );

			// need to add 1 to the 0-based array index, so we can put the
			// 1-based index into the name-to-id mapping that will be used
			// for the rest of the interfaces in this class
			Integer index = new Integer( i + 1 );

			// If the name is a duplicate of an existing column name or alias,
			// this entry is not put into the mapping table. This effectively
			// makes this entry inaccessible by name, which is the intended
			// behavior

			if ( !m_nameToIdMapping.containsKey( upperCaseName ) )
			{
				m_nameToIdMapping.put( upperCaseName, index );
			}

			String upperCaseAlias = column.getAlias( );
			if ( upperCaseAlias != null )
				upperCaseAlias = upperCaseAlias.toUpperCase( );
			if ( upperCaseAlias != null
					&& upperCaseAlias.length( ) > 0
					&& !m_nameToIdMapping.containsKey( upperCaseAlias ) )
			{
				m_nameToIdMapping.put( upperCaseAlias, index );
			}
		}
	}
	
	/**
	 * New an instance of ResultClass from input stream.
	 * 
	 * @param inputStream
	 * @throws DataException
	 */
	public ResultClass( InputStream inputStream ) throws DataException
	{
		assert inputStream != null;
		
		DataInputStream dis = new DataInputStream( inputStream );
		
		try
		{
			List newProjectedColumns = new ArrayList( );

			int size = IOUtil.readInt( dis );
			for ( int i = 0; i < size; i++ )
			{
				int driverPos = IOUtil.readInt( dis );
				String name = IOUtil.readStr( dis );
				String lable = IOUtil.readStr( dis );
				String alias = IOUtil.readStr( dis );
				String dtName = IOUtil.readStr( dis );
				String ntName = IOUtil.readStr( dis );
				boolean bool = IOUtil.readBool( dis );
				String dpdpName = IOUtil.readStr( dis );
				
				ResultFieldMetadata metaData = new ResultFieldMetadata( driverPos,
						name,
						lable,
						Class.forName( dtName ),
						ntName,
						bool );
				metaData.setAlias( alias );
				if ( dpdpName != null )
					metaData.setDriverProvidedDataType( Class.forName( dpdpName ) );
				newProjectedColumns.add( metaData );
			}
			dis.close( );
			
			initColumnsInfo( newProjectedColumns );
		}
		catch ( ClassNotFoundException e )
		{
			throw new DataException( ResourceConstants.RD_LOAD_ERROR,
					e,
					"Result Class" );
		}
		catch ( IOException e )
		{
			throw new DataException( ResourceConstants.RD_LOAD_ERROR,
					e,
					"Result Class" );
		}
	}

	/**
	 * Serialize instance status into output stream.
	 * 
	 * @param outputStream
	 * @throws DataException 
	 */
	public void doSave( OutputStream outputStream ) throws DataException
	{
		assert outputStream != null;
		
		DataOutputStream dos = new DataOutputStream( outputStream );
		
		int size = m_projectedColumns.size( );
		try
		{
			IOUtil.writeInt( outputStream, size );
			for ( int i = 0; i < size; i++ )
			{
				ResultFieldMetadata column = (ResultFieldMetadata) m_projectedColumns.get( i );

				IOUtil.writeInt( dos, column.getDriverPosition( ) );
				IOUtil.writeStr( dos, column.getName( ) );
				IOUtil.writeStr( dos, column.getLabel( ) );
				IOUtil.writeStr( dos, column.getAlias( ) );
				IOUtil.writeStr( dos, column.getDataType( ).getName( ) );
				IOUtil.writeStr( dos, column.getNativeTypeName( ) );
				IOUtil.writeBool( dos, column.isCustom( ) );
				if ( column.getDriverProvidedDataType( ) == null )
					IOUtil.writeStr( dos, null );
				else
					IOUtil.writeStr( dos, column.getDriverProvidedDataType( )
							.getName( ) );
			}
			
			dos.close( );
		}
		catch ( IOException e )
		{
			throw new DataException( ResourceConstants.RD_SAVE_ERROR,
					e,
					"Result Class" );
		}
	}
	
	public int getFieldCount()
	{
		return m_projectedColumns.size();
	}

	// returns the field names in the projected order
	// or an empty array if no fields were projected
	public String[] getFieldNames()
	{
		return doGetFieldNames();
	}

	private String[] doGetFieldNames()
	{
		if( m_fieldNames == null )
		{
			int size = m_projectedColumns.size();
			m_fieldNames = new String[ size ];
			for( int i = 0; i < size; i++ )
			{
				ResultFieldMetadata column = 
					(ResultFieldMetadata) m_projectedColumns.get( i );
				String name = column.getName();
				m_fieldNames[i] = name;
			}
		}
		
		return m_fieldNames;
	}

	public int[] getFieldDriverPositions()
	{
		if( m_fieldDriverPositions == null )
		{
			int size = m_projectedColumns.size();
			m_fieldDriverPositions = new int[ size ];
			for( int i = 0; i < size; i++ )
			{
				ResultFieldMetadata column = 
					(ResultFieldMetadata) m_projectedColumns.get( i );
				m_fieldDriverPositions[i] = column.getDriverPosition();
			}
		}
		
		return m_fieldDriverPositions;
	}
	
	public String getFieldName( int index ) throws DataException
	{
		validateFieldIndex( index );
		ResultFieldMetadata column = 
			(ResultFieldMetadata) m_projectedColumns.get( index - 1 );
		return column.getName();
	}

	public String getFieldAlias( int index ) throws DataException
	{
		ResultFieldMetadata column = findColumn( index );
		return column.getAlias();
	}
	
	public int getFieldIndex( String fieldName )
	{
		Integer i = 
			(Integer) m_nameToIdMapping.get( fieldName.toUpperCase( ) );
		return ( i == null ) ? -1 : i.intValue();
	}
	
	private int doGetFieldIndex( String fieldName ) throws DataException
	{
		int index = getFieldIndex( fieldName );
		
		if( index <= 0 )
			throw new DataException( ResourceConstants.INVALID_FIELD_NAME, fieldName );
		
		return index;
	}

	public Class getFieldValueClass( String fieldName ) throws DataException
	{
		int index = doGetFieldIndex( fieldName );
		return getFieldValueClass( index );
	}

	public Class getFieldValueClass( int index ) throws DataException
	{
		ResultFieldMetadata column = findColumn( index );
		return column.getDataType();
	}

	public boolean isCustomField( String fieldName ) throws DataException
	{
		int index = doGetFieldIndex( fieldName );
		return isCustomField( index );
	}

	public boolean isCustomField( int index ) throws DataException
	{
		ResultFieldMetadata column = findColumn( index );
		return column.isCustom();
	}

	public String getFieldLabel( int index ) throws DataException
	{
		ResultFieldMetadata column = findColumn( index );
		return column.getLabel();
	}
	
	private ResultFieldMetadata findColumn( int index ) throws DataException
	{
		validateFieldIndex( index );
		return (ResultFieldMetadata) m_projectedColumns.get( index - 1 );
	}
	
	// field indices are 1-based
    private void validateFieldIndex( int index ) throws DataException
    {
        if ( index < 1 || index > getFieldCount() )
            throw new DataException( ResourceConstants.INVALID_FIELD_INDEX, new Integer(index) );
    }

	public String getFieldNativeTypeName( int index ) throws DataException 
	{
		ResultFieldMetadata column = findColumn( index );
		return column.getNativeTypeName();
	}
	
	public ResultFieldMetadata getFieldMetaData( int index )
			throws DataException
	{
		validateFieldIndex( index );
		return (ResultFieldMetadata) m_projectedColumns.get( index - 1 );
	}
	
}
