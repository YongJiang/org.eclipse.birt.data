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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Date;

import org.eclipse.birt.core.data.DataType;
import org.eclipse.birt.data.engine.core.DataException;
import org.eclipse.birt.data.engine.executor.ResultObject;
import org.eclipse.birt.data.engine.odi.IResultClass;
import org.eclipse.birt.data.engine.odi.IResultObject;

/**
 * Utility class for ResultObject for serialize and deserialize. Since available
 * memory is bound in a specific case, data needs to be export to file and
 * import from it. Therefore, the operation of serialization and deserialization
 * is necessary and important.
 */
class ResultObjectUtil
{
	// column count of current processed table
	private int columnCount;

	// data type array of result set
	private Class[] typeArray;

	// meta data of result set
	private IResultClass rsMetaData;

	/**
	 * In serializaing data to file and deserializing it from file, metadata
	 * information is necessary to know which data type a column is, and then
	 * proper read/write method will be called. This method must be called at
	 * first when any actual read/write action is taken. Since multi thread
	 * might call DtE at the same time, an instance needs to be new to
	 * correspond to different metadata.
	 * 
	 * @param rsMetaData
	 * @throws DataException
	 */
	static ResultObjectUtil newInstance( IResultClass rsMetaData )
	{
		ResultObjectUtil instance = new ResultObjectUtil( );
		int length = rsMetaData.getFieldCount( );
		instance.typeArray = new Class[length];
		for ( int i = 0; i < length; i++ )
		{
			try
			{
				instance.typeArray[i] = rsMetaData.getFieldValueClass( i + 1 );
			}
			catch ( DataException e )
			{
				// the index will be always valid
			}
		}

		instance.columnCount = rsMetaData.getFieldCount( );
		instance.rsMetaData = rsMetaData;
		
		return instance;
	}

	/**
	 * Contruction, private 
	 */
	private ResultObjectUtil( )
	{
	}
	
	/**
	 * New a instance of ResultObject according to the parameter of object array
	 * plus the metadata stored before.
	 * 
	 * @param ob
	 * @return RowData
	 */
	ResultObject newResultObject( Object[] rowData )
	{
		return new ResultObject( rsMetaData, rowData );
	}

	/**
	 * Deserialze result object array from input stream. The reading procedure
	 * is strictly sequential, that means there is no random access.
	 * 
	 * Datatype Corresponds to executor#setDataType
	 * 
	 * @param br
	 *            input stream
	 * @param length
	 *            how many objects needs to be read
	 * @return result object array
	 * @throws IOException
	 */
	IResultObject[] readData( InputStream bis, int length )
			throws IOException
	{
		ResultObject[] rowDatas = new ResultObject[length];

		int rowLen;
		byte[] rowDataBytes;
				
		ByteArrayInputStream bais;
		DataInputStream dis;

		for ( int i = 0; i < length; i++ )
		{
			rowLen = IOUtil.readInt( bis );
			rowDataBytes = new byte[rowLen];
			bis.read( rowDataBytes );
			
			bais = new ByteArrayInputStream( rowDataBytes );
			dis = new DataInputStream( bais );

			Object[] obs = new Object[columnCount];
			for ( int j = 0; j < columnCount; j++ )
			{
				Class fieldType = typeArray[j];
				if ( dis.readByte( ) == 0 )
				{
					obs[j] = null;
					continue;
				}
				
				if ( fieldType.equals( Integer.class ) )
					obs[j] = new Integer( dis.readInt( ) );
				else if ( fieldType.equals( Double.class ) )
					obs[j] = new Double( dis.readDouble( ) );
				else if ( fieldType.equals( BigDecimal.class ) )
					obs[j] = new BigDecimal( dis.readUTF( ) );
				else if ( fieldType.equals( Date.class ) )
					obs[j] = new Date( dis.readLong( ) );
				else if ( fieldType.equals( Time.class ) )
					obs[j] = new Time( dis.readLong( ) );
				else if ( fieldType.equals( Timestamp.class ) )
					obs[j] = new Timestamp( dis.readLong( ) );
				else if ( fieldType.equals( Boolean.class ) )
					obs[j] = new Boolean( dis.readBoolean( ) );
				else if ( fieldType.equals( String.class )
						|| fieldType.equals( DataType.getClass( DataType.ANY_TYPE ) ) )
					obs[j] = dis.readUTF( );
			}
			rowDatas[i] = newResultObject( obs );

			rowDataBytes = null;			
			dis = null;
			bais = null;
		}

		return rowDatas;
	}

	/**
	 * Serialze result object array to file. The serialize procedure is
	 * conversed with de-serialize(read) procedure.
	 * 
	 * @param bos output stream
	 * @param resultObjects result objects needs to be deserialized
	 * @param length how many objects to be deserialized
	 * @throws IOException
	 */
	void writeData( OutputStream bos,
			IResultObject[] resultObjects, int length ) throws IOException
	{
		byte[] rowsDataBytes;

		ByteArrayOutputStream baos;
		DataOutputStream dos;
		
		for ( int i = 0; i < length; i++ )
		{
			baos = new ByteArrayOutputStream( );
			dos = new DataOutputStream( baos );
			for ( int j = 0; j < columnCount; j++ )
			{
				Object fieldValue = null;
				try
				{
					fieldValue = resultObjects[i].getFieldValue( j + 1 );
				}
				catch ( DataException e )
				{
					// never get here since the index value is always value
				}
					
				// process null object
				if ( fieldValue == null )
				{					
					dos.writeByte( 0 );
					continue;
				}
				else
				{
					dos.writeByte( 1 );
				}
				
				Class fieldType = typeArray[j];
				if ( fieldType.equals( Integer.class ) )
					dos.writeInt( ( (Integer) fieldValue ).intValue( ) );
				else if ( fieldType.equals( Double.class ) )
					dos.writeDouble( ( (Double) fieldValue ).doubleValue( ) );
				else if ( fieldType.equals( BigDecimal.class ) )
					dos.writeUTF( ( (BigDecimal) fieldValue ).toString( ) );
				else if ( fieldType.equals( Date.class ) )
					dos.writeLong( ( (Date) fieldValue ).getTime( ) );
				else if ( fieldType.equals( Time.class ) )
					dos.writeLong( ( (Time) fieldValue ).getTime( ) );
				else if ( fieldType.equals( Timestamp.class ) )
					dos.writeLong( ( (Timestamp) fieldValue ).getTime( ) );
				else if ( fieldType.equals( Boolean.class ) )
					dos.writeBoolean( ( (Boolean) fieldValue ).booleanValue( ) );
				else if ( fieldType.equals( String.class )
						|| fieldType.equals( DataType.getClass( DataType.ANY_TYPE ) ) )
					dos.writeUTF( fieldValue.toString( ) );
			}
			dos.flush( );

			rowsDataBytes = baos.toByteArray( );
			IOUtil.writeInt( bos, rowsDataBytes.length );
			bos.write( rowsDataBytes );

			rowsDataBytes = null;
			dos = null;
			baos = null;
		}
	}
	
}