/*
 *************************************************************************
 * Copyright (c) 2004, 2005 Actuate Corporation.
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
package org.eclipse.birt.data.engine.executor;

import java.sql.Blob;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.eclipse.birt.core.data.DataType;
import org.eclipse.birt.core.data.DataTypeUtil;
import org.eclipse.birt.data.engine.api.IParameterDefinition;
import org.eclipse.birt.data.engine.core.DataException;
import org.eclipse.birt.data.engine.executor.dscache.DataSetResultCache;
import org.eclipse.birt.data.engine.executor.transform.CachedResultSet;
import org.eclipse.birt.data.engine.i18n.ResourceConstants;
import org.eclipse.birt.data.engine.impl.ParamDefnAndValBinding;
import org.eclipse.birt.data.engine.odaconsumer.ColumnHint;
import org.eclipse.birt.data.engine.odaconsumer.ParameterHint;
import org.eclipse.birt.data.engine.odaconsumer.PreparedStatement;
import org.eclipse.birt.data.engine.odaconsumer.ResultSet;
import org.eclipse.birt.data.engine.odi.IDataSourceQuery;
import org.eclipse.birt.data.engine.odi.IEventHandler;
import org.eclipse.birt.data.engine.odi.IParameterMetaData;
import org.eclipse.birt.data.engine.odi.IPreparedDSQuery;
import org.eclipse.birt.data.engine.odi.IResultClass;
import org.eclipse.birt.data.engine.odi.IResultIterator;

/**
 *	Structure to hold info of a custom field. 
 */
final class CustomField
{
    String 	name;
    int dataType = -1;
    
    CustomField( String name, int dataType)
    {
        this.name = name;
        this.dataType = dataType;
    }
    
    CustomField()
    {}
    
    public int getDataType()
    {
        return dataType;
    }
    
    public void setDataType(int dataType)
    {
        this.dataType = dataType;
    }
    
    public String getName()
    {
        return name;
    }
    
    public void setName(String name)
    {
        this.name = name;
    }
}

/**
 * Structure to hold Parameter binding info
 * @author lzhu
 *
 */
class ParameterBinding
{
	private String name;
	private int position = -1;
	private Object value;
	
	ParameterBinding( String name, Object value )
	{
		this.name = name;
		this.value = value;
	}
	
	ParameterBinding( int position, Object value )
	{
		this.position = position;
		this.value = value;
	}
	
	public int getPosition()
	{
		return position;
	}
	
	public String getName()
	{
		return name;
	}

	public Object getValue()
	{
		return value;
	}
}

/**
 * Implementation of ODI's IDataSourceQuery interface
 */
class DataSourceQuery extends BaseQuery implements IDataSourceQuery, IPreparedDSQuery
{
    protected DataSource 		dataSource;
    protected String			queryText;
    protected String			queryType;
    protected PreparedStatement	odaStatement;
    
    // Collection of ColumnHint objects
    protected Collection		resultHints;
    
    // Collection of CustomField objects
    protected Collection		customFields;
    
    protected IResultClass		resultMetadata;
    
    // Names (or aliases) of columns in the projected result set
    protected String[]			projectedFields;
	
	// input/output parameter hints
	private Collection paramDefnAndValBindings;
    
	// input parameter values
	private Collection inputParamValues;
	
	// Properties added by addProperty()
	private ArrayList propNames;
	private ArrayList propValues;
	
	/**
	 * Constructor. 
	 * 
	 * @param dataSource
	 * @param queryType
	 * @param queryText
	 */
    DataSourceQuery( DataSource dataSource, String queryType, String queryText )
    {
        this.dataSource = dataSource;
        this.queryText = queryText;
        this.queryType = queryType;
    }

    /*
     * @see org.eclipse.birt.data.engine.odi.IDataSourceQuery#setResultHints(java.util.Collection)
     */
    public void setResultHints(Collection columnDefns)
    {
        resultHints = columnDefns;
    }

    /*
     * @see org.eclipse.birt.data.engine.odi.IDataSourceQuery#setResultProjection(java.lang.String[])
     */
    public void setResultProjection(String[] fieldNames) throws DataException
    {
        if ( fieldNames == null || fieldNames.length == 0 )
            return;		// nothing to set
        this.projectedFields = fieldNames;
    }
    
    /*
     * @see org.eclipse.birt.data.engine.odi.IDataSourceQuery#setParameterDefnAndDeftValBindings(java.util.Collection)
     */
	public void setParameterDefnAndValBindings( Collection collection )
	{
        if ( collection == null || collection.isEmpty() )
			return; 	// nothing to set
        
        // assign to placeholder, for use later during prepare()
		this.paramDefnAndValBindings = collection;
	}

    /*
     * @see org.eclipse.birt.data.engine.odi.IDataSourceQuery#addProperty(java.lang.String, java.lang.String)
     */
    public void addProperty(String name, String value ) throws DataException
    {
    	if ( name == null )
    		throw new NullPointerException("Property name is null");
    	
    	// Must be called before prepare() per interface spec
        if ( odaStatement != null )
            throw new DataException( ResourceConstants.QUERY_HAS_PREPARED );
    	
   		if ( propNames == null )
   		{
   			assert propValues == null;
   			propNames = new ArrayList();
   			propValues = new ArrayList();
   		}
   		assert propValues != null;
   		propNames.add( name );
   		propValues.add( value );
    }

    /*
     * @see org.eclipse.birt.data.engine.odi.IDataSourceQuery#declareCustomField(java.lang.String, int)
     */
    public void declareCustomField( String fieldName, int dataType ) throws DataException
    {
        if ( fieldName == null || fieldName.length() == 0 )
            throw new DataException( ResourceConstants.CUSTOM_FIELD_EMPTY );
        
        if ( customFields == null )
        {
            customFields = new ArrayList();
        }
        else
        {
        	Iterator cfIt = customFields.iterator( );
			while ( cfIt.hasNext( ) )
			{
				CustomField cf = (CustomField) cfIt.next();
				if ( cf.name.equals( fieldName ) )
				{
					throw new DataException( ResourceConstants.DUP_CUSTOM_FIELD_NAME, fieldName );
				}
			}
        }
        
        customFields.add( new CustomField( fieldName, dataType ) );
    }

    /* (non-Javadoc)
     * @see org.eclipse.birt.data.engine.odi.IDataSourceQuery#prepare()
     */
    public IPreparedDSQuery prepare() throws DataException
    {
        if ( odaStatement != null )
            throw new DataException( ResourceConstants.QUERY_HAS_PREPARED );

        odaStatement = dataSource.prepareStatement( queryText, queryType );
        
        // Add custom properties
        addProperties();
        
        // Add parameter defns. This step must be done before odaStatement.setColumnsProjection()
        // for some jdbc driver need to carry out a query execution before the metadata can be achieved
        // and only when the Parameters are successfully set the query execution can succeed.
        addParameterDefns();
        // Ordering is important for the following operations. Column hints should be defined
        // after custom fields are declared (since hints may be given to those custom fields).
        // Column projection comes last because it needs hints and custom column information
        addCustomFields( odaStatement );
        addColumnHints( odaStatement );
        
		odaStatement.setColumnsProjection( this.projectedFields );

        // If ODA can provide result metadata, get it now
        try
        {
            resultMetadata = odaStatement.getMetaData();
        }
        catch ( DataException e )
        {
            // Assume metadata not available at prepare time; ignore the exception
        	resultMetadata = null;
        }
        
        return this;
    }

    /** 
     * Adds custom properties to oda statement being prepared 
     */
    private void addProperties() throws DataException
	{
    	assert odaStatement != null;
    	if ( propNames != null )
    	{
    		assert propValues != null;
    		
    		Iterator it_name = propNames.iterator();
    		Iterator it_val = propValues.iterator();
    		while ( it_name.hasNext())
    		{
    			assert it_val.hasNext();
    			String name = (String) it_name.next();
    			String val = (String) it_val.next();
    			odaStatement.setProperty( name, val );
    		}
    	}
	}
      
	/** 
	 * Adds input and output parameter hints to odaStatement
	 */
	private void addParameterDefns() throws DataException
	{
		if ( this.paramDefnAndValBindings == null )
		    return;	// nothing to add

		// iterate thru the collection to add parameter hints
		Iterator list = this.paramDefnAndValBindings.iterator( );
		while ( list.hasNext( ) )
		{
			ParamDefnAndValBinding paramDefnAndValBinding = (ParamDefnAndValBinding) list.next( );
		    IParameterDefinition parameterDefn = paramDefnAndValBinding.getParamDefn();
			ParameterHint parameterHint = new ParameterHint( parameterDefn.getName(), 
															 parameterDefn.isInputMode(),
															 parameterDefn.isOutputMode() );
			if( isParameterPositionValid(parameterDefn.getPosition()))
				parameterHint.setPosition( parameterDefn.getPosition( ) );

			// following data types is not supported by odaconsumer currently
			Class dataTypeClass = DataType.getClass( parameterDefn.getType( ) );
			if ( dataTypeClass == DataType.AnyType.class
					|| dataTypeClass == Boolean.class || dataTypeClass == Blob.class )
			{
				dataTypeClass = null;
			}
			parameterHint.setDataType( dataTypeClass );
			parameterHint.setIsInputOptional( parameterDefn.isInputOptional( ) );
			parameterHint.setDefaultInputValue( paramDefnAndValBinding.getValue() );
			parameterHint.setIsNullable( parameterDefn.isNullable() );
			odaStatement.addParameterHint( parameterHint );
			
			//If the parameter is input parameter then add it to input value list.
			if ( parameterHint.isInputMode( )
					&& parameterHint.getDefaultInputValue( ) != null )
			{
				Object inputValue = convertToValue( parameterHint.getDefaultInputValue( ),
						parameterDefn.getType( ) );
				if ( isParameterPositionValid(parameterHint.getPosition( )) )
					this.setInputParamValue( parameterHint.getPosition( ),
							inputValue );
				else
					this.setInputParamValue( parameterHint.getName( ),
							inputValue );
			}			
		}
		this.setInputParameterBinding();
	}
	
	/**
	 * Check whether the given parameter position is valid.
	 * 
	 * @param parameterPosition
	 * @return
	 */
	private boolean isParameterPositionValid(int parameterPosition)
	{
		return parameterPosition > 0;
	}
	
	/**
	 * @param inputParamName
	 * @param paramValue
	 * @throws DataException
	 */
	private void setInputParamValue( String inputParamName, Object paramValue )
			throws DataException
	{

		ParameterBinding pb = new ParameterBinding( inputParamName, paramValue );
		getInputParamValues().add( pb );
	}

	/**
	 * @param inputParamPos
	 * @param paramValue
	 * @throws DataException
	 */
	private void setInputParamValue( int inputParamPos, Object paramValue )
			throws DataException
	{
		ParameterBinding pb = new ParameterBinding( inputParamPos, paramValue );
		getInputParamValues().add( pb );
	}
	
	/**
	 * Declares custom fields on Oda statement
	 * 
	 * @param stmt
	 * @throws DataException
	 */
    private void addCustomFields( PreparedStatement stmt ) throws DataException
	{
    	if ( this.customFields != null )
    	{
    		Iterator it = this.customFields.iterator( );
    		while ( it.hasNext( ) )
    		{
    			CustomField customField = (CustomField) it.next( );
    			stmt.declareCustomColumn( customField.getName( ),
    				DataType.getClass( customField.getDataType() ) );
    		}
    	}
	}
    
    /**
     * Adds Odi column hints to ODA statement
     *  
     * @param stmt
     * @throws DataException
     */
    private void addColumnHints( PreparedStatement stmt ) throws DataException
	{
    	assert stmt != null;
    	if ( resultHints == null || resultHints.size() == 0 )
    		return;
    	Iterator it = resultHints.iterator();
    	while ( it.hasNext())
    	{
    		IDataSourceQuery.ResultFieldHint hint = 
    				(IDataSourceQuery.ResultFieldHint) it.next();
    		ColumnHint colHint = new ColumnHint( hint.getName() );
    		colHint.setAlias( hint.getAlias() );
    		if ( hint.getDataType( ) == DataType.ANY_TYPE )
				colHint.setDataType( null );
			else
				colHint.setDataType( DataType.getClass( hint.getDataType( ) ) );
			if ( hint.getPosition() > 0 )
    			colHint.setPosition( hint.getPosition());

   			stmt.addColumnHint( colHint );
    	}
	}
       
	/*
	 * @see org.eclipse.birt.data.engine.odi.IPreparedDSQuery#getResultClass()
	 */
    public IResultClass getResultClass() 
    {
        // Note the return value can be null if resultMetadata was 
        // not available during prepare() time
        return resultMetadata;
    }

    /*
     * @see org.eclipse.birt.data.engine.odi.IPreparedDSQuery#getParameterMetaData()
     */
    public Collection getParameterMetaData()
			throws DataException
	{
        if ( odaStatement == null )
			throw new DataException( ResourceConstants.QUERY_HAS_NOT_PREPARED );
        
        Collection odaParamsInfo = odaStatement.getParameterMetaData();
        if ( odaParamsInfo == null || odaParamsInfo.isEmpty() )
            return null;
        
        // iterates thru the most up-to-date collection, and
        // wraps each of the odaconsumer parameter metadata object
        ArrayList paramMetaDataList = new ArrayList( odaParamsInfo.size() );
        Iterator odaParamMDIter = odaParamsInfo.iterator();
        while ( odaParamMDIter.hasNext() )
        {
            org.eclipse.birt.data.engine.odaconsumer.ParameterMetaData odaMetaData = 
                (org.eclipse.birt.data.engine.odaconsumer.ParameterMetaData) odaParamMDIter.next();
            paramMetaDataList.add( new ParameterMetaData( odaMetaData ) );
        }
        return paramMetaDataList;
	}
    
    /**
     * Return the input parameter value list
     * 
     * @return
     */
	private Collection getInputParamValues()
	{
	    if ( inputParamValues == null )
	        inputParamValues = new ArrayList();
	    return inputParamValues;
	}

	/*
	 * @see org.eclipse.birt.data.engine.odi.IPreparedDSQuery#execute()
	 */
    public IResultIterator execute( IEventHandler eventHandler )
			throws DataException
	{
    	assert odaStatement != null;

    	this.setInputParameterBinding();
		// Execute the prepared statement
		if ( !odaStatement.execute( ) )
			throw new DataException( ResourceConstants.NO_RESULT_SET );
		ResultSet rs = odaStatement.getResultSet();
		
		// If we did not get a result set metadata at prepare() time, get it now
		if ( resultMetadata == null )
		{
			resultMetadata = rs.getMetaData();
            if ( resultMetadata == null )
    			throw new DataException(ResourceConstants.METADATA_NOT_AVAILABLE);
		}
		
		// Initialize CachedResultSet using the ODA result set
		if ( DataSetCacheManager.getInstance( ).doesSaveToCache( ) == false )
			return new CachedResultSet( this, resultMetadata, rs, eventHandler );
		else
			return new CachedResultSet( this,
					resultMetadata,
					new DataSetResultCache( rs, resultMetadata ),
					eventHandler );
    }
    
    /**
     *  set input parameter bindings
     */
    private void setInputParameterBinding() throws DataException{
		//		 set input parameter bindings
		Iterator inputParamValueslist = getInputParamValues().iterator( );
		while ( inputParamValueslist.hasNext( ) )
		{
			ParameterBinding paramBind = (ParameterBinding) inputParamValueslist.next( );
			if ( isParameterPositionValid(paramBind.getPosition( )) )
				odaStatement.setParameterValue( paramBind.getPosition( ),
						paramBind.getValue() );
			else
				odaStatement.setParameterValue( paramBind.getName( ),
						paramBind.getValue() );
		}
    }
    
	/*
	 * @see org.eclipse.birt.data.engine.odi.IPreparedDSQuery#getParameterValue(int)
	 */
	public Object getOutputParameterValue( int index ) throws DataException
	{
		assert odaStatement != null;
		
		int newIndex = getCorrectParamIndex( index );
		return odaStatement.getParameterValue( newIndex );
	}

	/*
	 * @see org.eclipse.birt.data.engine.odi.IPreparedDSQuery#getParameterValue(java.lang.String)
	 */
	public Object getOutputParameterValue( String name ) throws DataException
	{
		assert odaStatement != null;
				
		checkOutputParamNameValid( name );
		return odaStatement.getParameterValue( name );
	}
    
	/**
	 * In oda layer, it does not differentiate the value retrievation of input
	 * parameter value and ouput parameter value. They will be put in a same
	 * sequence list. However, in odi layer, we need to clearly distinguish them
	 * since only retrieving output parameter is suppored and it should be based
	 * on its own output parameter index. Therefore, this method will do such a
	 * conversion from the output parameter index to the parameter index.
	 * 
	 * @param index based on output parameter order
	 * @return index based on the whole parameters order
	 * @throws DataException
	 */
	private int getCorrectParamIndex( int index ) throws DataException
	{
		if ( index <= 0 )
			throw new DataException( "Invalid output parameter index: " + index );
		
		int newIndex = 0; // 1-based
		int curOutputIndex = 0; // 1-based
		
		Collection collection = getParameterMetaData( );
		if ( collection != null )
		{
			Iterator it = collection.iterator( );
			while ( it.hasNext( ) )
			{
				newIndex++;
				
				IParameterMetaData metaData = (IParameterMetaData) it.next( );
				if ( metaData.isOutputMode( ).booleanValue( ) == true )
				{
					curOutputIndex++;
					
					if ( curOutputIndex == index )
						break;
				}
			}
		}

		if ( curOutputIndex < index )
			throw new DataException( "Output parameter index is out of bound"
					+ index );

		return newIndex;
	}
	
	/**
	 * Validate the name of output parameter
	 * 
	 * @param name
	 * @throws DataException
	 */
	private void checkOutputParamNameValid( String name ) throws DataException
	{
		assert name != null;

		boolean isValid = false;

		Collection collection = getParameterMetaData( );
		if ( collection != null )
		{
			Iterator it = collection.iterator( );
			while ( it.hasNext( ) )
			{
				IParameterMetaData metaData = (IParameterMetaData) it.next( );

				String paramName = metaData.getName( );
				if ( paramName.equals( name ) )
				{
					isValid = metaData.isOutputMode( ).booleanValue( );
					break;
				}
			}
		}

		if ( isValid == false )
			throw new DataException( "Invalid output parameter name" + name );
	}
	
	/*
	 * @see org.eclipse.birt.data.engine.odi.IQuery#close()
	 */
    public void close()
    {
        if ( odaStatement != null )
        {
        	this.dataSource.closeStatement( odaStatement );
	        odaStatement = null;
        }
        
        this.dataSource = null;
        // TODO: close all CachedResultSets created by us
    }
    
    /**
     * convert the String value to Object according to it's datatype.
     *  
     * @param inputValue
     * @param type
     * @return
     * @throws DataException
     */
    private static Object convertToValue( String inputValue, int type )
			throws DataException
	{
		//dataType can be refered from DataType's class type.
		int dataType = type;
		Object convertedResult = null;
		try
		{
			if ( dataType == DataType.INTEGER_TYPE )
			{
				convertedResult = DataTypeUtil.toInteger( inputValue );
			}
			else if ( dataType == DataType.DOUBLE_TYPE )
			{
				convertedResult = DataTypeUtil.toDouble( inputValue );
			}
			else if ( dataType == DataType.STRING_TYPE )
			{
				convertedResult = inputValue;
			}
			else if ( dataType == DataType.DECIMAL_TYPE )
			{
				convertedResult = DataTypeUtil.toBigDecimal( inputValue );
			}
			else if ( dataType == DataType.DATE_TYPE )
			{
				convertedResult = DataTypeUtil.toDate( inputValue );
			}
			else if ( dataType == DataType.ANY_TYPE
					|| dataType == DataType.UNKNOWN_TYPE )
			{
				convertedResult = DataTypeUtil.toAutoValue( inputValue );
			}
			else
			{
				convertedResult = inputValue;
			}
			return convertedResult;
		}
		catch ( Exception ex )
		{
			throw new DataException( ResourceConstants.CANNOT_CONVERT_PARAMETER_TYPE,
					ex,
					new Object[]{
							inputValue, DataType.getClass( dataType )
					} );
		}
	}
    
}
