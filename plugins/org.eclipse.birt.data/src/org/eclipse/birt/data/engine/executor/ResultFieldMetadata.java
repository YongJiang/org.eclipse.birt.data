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

import java.math.BigDecimal;
import java.sql.Time;
import java.sql.Timestamp;

import org.eclipse.datatools.connectivity.oda.IBlob;
import org.eclipse.datatools.connectivity.oda.IClob;

/**
 * <code>ResultFieldMetadata</code> contains metadata about a 
 * column that is needed by <code>ResultClass</code>.
 */
public class ResultFieldMetadata
{
	private int m_driverPosition;
	private String m_name;
	private String m_label;
	private String m_alias;
	private Class m_dataType;	// can be overwritten by column hints
	private String m_nativeTypeName;
	private boolean m_isCustom;
	private Class m_driverProvidedDataType;
	private int m_analysisType = -1;
	private boolean m_indexColumn;
	private boolean m_compressedColumn;

	public ResultFieldMetadata( int driverPosition, String name, 
						 		String label, Class dataType,
								String nativeTypeName, boolean isCustom )
	{
		m_driverPosition = driverPosition;
		m_name = name;
		m_label = label;
		m_alias = label;
		m_dataType = dataType;
		m_nativeTypeName = nativeTypeName;
		m_isCustom = isCustom;
		m_driverProvidedDataType = null;
		// initialize to unknown
	}
	
	public ResultFieldMetadata( int driverPosition, String name, 
						 		String label, Class dataType,
								String nativeTypeName, boolean isCustom, int analysisType )
	{
		this( driverPosition, name, label, dataType, nativeTypeName, isCustom );
		this.m_analysisType = analysisType;
	}
	
	public ResultFieldMetadata( int driverPosition, String name, 
	 		String label, Class dataType,
			String nativeTypeName, boolean isCustom, int analysisType, boolean indexColumn, boolean compressedColumn )
	{
		this( driverPosition, name, label, dataType, nativeTypeName, isCustom );
		this.m_analysisType = analysisType;
		this.m_indexColumn = indexColumn;
		this.m_compressedColumn = compressedColumn;
	}
	
	public int getAnalysisType( )
	{
		return this.m_analysisType;
	}
	
	public void setAnalysisType( int type )
	{
		this.m_analysisType = type;
	}
	
	public boolean isIndexColumn( )
	{
		return this.m_indexColumn;
	}
	
	public void setIndexColumn( boolean indexColumn )
	{
		this.m_indexColumn = indexColumn;
	}
	
	public boolean isCompressedColumn()
	{
		return this.m_compressedColumn;
	}
	
	public void setCompressedColumn( boolean compressedColumn )
	{
		this.m_compressedColumn = compressedColumn;
	}
	
	// returns the driver position from the runtime metadata
	public int getDriverPosition()
	{
		return m_driverPosition;
	}
	
	public String getName()
	{
		return m_name;
	}
	
	public void setName( String name )
	{
		m_name = name;
	}
	
	public String getAlias()
	{
		return m_alias;
	}
	
	public void setAlias( String alias )
	{
		m_alias = alias;
	}
	
	public Class getDataType()
	{
	    if ( m_dataType != null )
	        return m_dataType;
	    
	    Class driverDataType = getDriverProvidedDataType();
	    if ( driverDataType != null )
	        return driverDataType;
	    
	    // default to a String if data type is unknown
	    return String.class;
	}
	
	public void setDataType( Class dataType )
	{
/*		assert( dataType == Integer.class ||
		        dataType == Double.class ||
		        dataType == String.class ||
		        dataType == BigDecimal.class ||
		        dataType == java.util.Date.class ||   // backward compatibilty
                dataType == java.sql.Date.class ||
		        dataType == Time.class ||
		        dataType == Timestamp.class ||
		        dataType == IBlob.class ||
		        dataType == IClob.class ||
                dataType == Boolean.class ); */
		
		m_dataType = dataType;
	}
	
	public String getNativeTypeName()
	{
		return m_nativeTypeName;
	}
	
	public void setNativeTypeName( String nativeTypeName )
	{
		m_nativeTypeName = nativeTypeName;
	}
	
	public String getLabel()
	{
		return m_label;
	}
	
	public boolean isCustom()
	{
		return m_isCustom;
	}

    public Class getDriverProvidedDataType()
    {
        return m_driverProvidedDataType;
    }
 
    public void setDriverProvidedDataType( Class odaDataTypeAsClass )
    {
        m_driverProvidedDataType = odaDataTypeAsClass;
    }
}
