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
import java.util.Map;
import java.util.Set;

import org.eclipse.birt.core.util.IOUtil;
import org.eclipse.birt.data.engine.core.DataException;
import org.eclipse.birt.data.engine.executor.cache.ResultSetUtil;
import org.eclipse.birt.data.engine.i18n.ResourceConstants;
import org.eclipse.birt.data.engine.odi.IResultClass;

/**
 * <code>ResultClass</code> contains the metadata about 
 * the projected columns in the result set.
 */
public class ResultClass implements IResultClass
{
	private ResultFieldMetadata[] projectedCols;
	private int m_fieldCount;
	private HashMap nameToIdMapping;
	private String[] fieldNames;
	private int[] fieldDriverPositions;
	private ResultClassHelper resultClassHelper;
	
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
		m_fieldCount = projectedColumns.size( );
		projectedCols = new ResultFieldMetadata[m_fieldCount];
		nameToIdMapping = new HashMap( );

		for ( int i = 0, n = projectedColumns.size( ); i < n; i++ )
		{
			projectedCols[i] = (ResultFieldMetadata) projectedColumns.get( i );
			ResultFieldMetadata column = projectedCols[i];

			String upperCaseName = column.getName( );
			//if ( upperCaseName != null )
			//	upperCaseName = upperCaseName.toUpperCase( );

			// need to add 1 to the 0-based array index, so we can put the
			// 1-based index into the name-to-id mapping that will be used
			// for the rest of the interfaces in this class
			Integer index = new Integer( i + 1 );

			// If the name is a duplicate of an existing column name or alias,
			// this entry is not put into the mapping table. This effectively
			// makes this entry inaccessible by name, which is the intended
			// behavior

			if ( !nameToIdMapping.containsKey( upperCaseName ) )
			{
				nameToIdMapping.put( upperCaseName, index );
			}

			String upperCaseAlias = column.getAlias( );
			//if ( upperCaseAlias != null )
			//	upperCaseAlias = upperCaseAlias.toUpperCase( );
			if ( upperCaseAlias != null
					&& upperCaseAlias.length( ) > 0
					&& !nameToIdMapping.containsKey( upperCaseAlias ) )
			{
				nameToIdMapping.put( upperCaseAlias, index );
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
				String name = IOUtil.readString( dis );
				String lable = IOUtil.readString( dis );
				String alias = IOUtil.readString( dis );
				String dtName = IOUtil.readString( dis );
				String ntName = IOUtil.readString( dis );
				boolean bool = IOUtil.readBool( dis );
				String dpdpName = IOUtil.readString( dis );
				
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
	public void doSave( OutputStream outputStream, Map requestColumnMap )
			throws DataException
	{
		assert outputStream != null;
		
		DataOutputStream dos = new DataOutputStream( outputStream );
		Set resultSetNameSet = ResultSetUtil.getRsColumnRequestMap( requestColumnMap );

		int size = resultSetNameSet.size( );
		try
		{
			IOUtil.writeInt( outputStream, size );
			for ( int i = 0; i < m_fieldCount; i++ )
			{
				ResultFieldMetadata column = projectedCols[i];

				if ( resultSetNameSet.contains( column.getName( ) ) )
				{
					IOUtil.writeInt( dos, column.getDriverPosition( ) );
					IOUtil.writeString( dos, column.getName( ) );
					IOUtil.writeString( dos, column.getLabel( ) );
					IOUtil.writeString( dos, column.getAlias( ) );
					IOUtil.writeString( dos, column.getDataType( ).getName( ) );
					IOUtil.writeString( dos, column.getNativeTypeName( ) );
					IOUtil.writeBool( dos, column.isCustom( ) );
					if ( column.getDriverProvidedDataType( ) == null )
						IOUtil.writeString( dos, null );
					else
						IOUtil.writeString( dos,
								column.getDriverProvidedDataType( ).getName( ) );
				}
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
		return m_fieldCount;
	}

	// returns the field names in the projected order
	// or an empty array if no fields were projected
	public String[] getFieldNames()
	{
		return doGetFieldNames();
	}

	private String[] doGetFieldNames()
	{
		if( fieldNames == null )
		{
			int size = m_fieldCount;
			fieldNames = new String[ size ];
			for( int i = 0; i < size; i++ )
			{
				fieldNames[i] = projectedCols[i].getName();
			}
		}
		
		return fieldNames;
	}

	public int[] getFieldDriverPositions()
	{
		if( fieldDriverPositions == null )
		{
			int size = m_fieldCount;
			fieldDriverPositions = new int[ size ];
			for( int i = 0; i < size; i++ )
			{
				ResultFieldMetadata column = projectedCols[i];
				fieldDriverPositions[i] = column.getDriverPosition();
			}
		}
		
		return fieldDriverPositions;
	}
	
	public String getFieldName( int index ) throws DataException
	{
		return projectedCols[index - 1].getName();
	}

	public String getFieldAlias( int index ) throws DataException
	{
		return projectedCols[index - 1].getAlias();
	}
	
	public int getFieldIndex( String fieldName )
	{
		Integer i = 
			(Integer) nameToIdMapping.get( fieldName );//.toUpperCase( ) );
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
		return projectedCols[index - 1].getDataType();
	}

	public boolean isCustomField( String fieldName ) throws DataException
	{
		int index = doGetFieldIndex( fieldName );
		return isCustomField( index );
	}

	public boolean isCustomField( int index ) throws DataException
	{
		return projectedCols[index - 1].isCustom();
	}

	public String getFieldLabel( int index ) throws DataException
	{
		return projectedCols[index - 1].getLabel();
	}
	
	public String getFieldNativeTypeName( int index ) throws DataException 
	{
		return projectedCols[index - 1].getNativeTypeName();
	}
	
	public ResultFieldMetadata getFieldMetaData( int index )
			throws DataException
	{
		return projectedCols[index - 1];
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.birt.data.engine.odi.IResultClass#existCloborBlob()
	 */
	public boolean hasClobOrBlob( ) throws DataException
	{
		return getResultClasstHelper( ).hasClobOrBlob( );
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.birt.data.engine.odi.IResultClass#getClobIndexArray()
	 */
	public int[] getClobFieldIndexes( ) throws DataException
	{
		return getResultClasstHelper( ).getClobIndexArray( );
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.birt.data.engine.odi.IResultClass#getBlobIndexArray()
	 */
	public int[] getBlobFieldIndexes( ) throws DataException
	{
		return getResultClasstHelper( ).getBlobIndexArray( );
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.birt.data.engine.odi.IResultClass#getResultObjectHelper()
	 */
	public ResultClassHelper getResultClasstHelper( ) throws DataException
	{
		if ( resultClassHelper == null )
			resultClassHelper = new ResultClassHelper( this );
		return resultClassHelper;
	}
	
}
