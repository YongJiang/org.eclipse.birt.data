/*
 *************************************************************************
 * Copyright (c) 2006 Actuate Corporation.
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
package org.eclipse.birt.report.data.adapter.api.script;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.birt.core.data.DataTypeUtil;
import org.eclipse.birt.core.exception.BirtException;
import org.eclipse.birt.core.script.CoreJavaScriptInitializer;
import org.eclipse.birt.report.data.adapter.api.DataAdapterUtil;
import org.eclipse.birt.report.model.api.ConfigVariableHandle;
import org.eclipse.birt.report.model.api.DesignEngine;
import org.eclipse.birt.report.model.api.DesignFileException;
import org.eclipse.birt.report.model.api.ModuleHandle;
import org.eclipse.birt.report.model.api.ReportDesignHandle;
import org.eclipse.birt.report.model.api.ScalarParameterHandle;
import org.eclipse.birt.report.model.api.SessionHandle;
import org.eclipse.birt.report.model.api.elements.DesignChoiceConstants;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ImporterTopLevel;
import org.mozilla.javascript.Scriptable;

import com.ibm.icu.util.ULocale;

/**
 * Top-level scope created by Data Adaptor. This scope provides implementation of some
 * report level script object (e.g., "params")
 */
public class DataAdapterTopLevelScope extends ImporterTopLevel
{
	private static final long serialVersionUID = 4230948829384l;
	
	private static final String PROP_PARAMS = "params";
	private static final String PROP_REPORTCONTEXT = "reportContext";
	private ModuleHandle designModule;
	private Scriptable paramsProp;
	private Object reportContextProp;

	
	/**
	 * Constructor; initializes standard objects in scope
	 * @param cx Context used to initialze scope
	 * @param module Module associated with this scope; can be null
	 */
	public DataAdapterTopLevelScope( Context cx, ModuleHandle module )
	{
		super(cx);
		// init BIRT native objects
		new CoreJavaScriptInitializer( ).initialize( cx, this );
		designModule = module;
	}
	
    public boolean has(String name, Scriptable start) 
    {
    	if ( super.has(name, start) )
    		return true;
    	// "params" is available only if we have a module
    	if ( designModule != null && PROP_PARAMS.equals(name) )
    		return true;
    	
    	if ( PROP_REPORTCONTEXT.equals( name ) )
			return true;
    	return false;
    }

    public Object get(String name, Scriptable start) 
    {
        Object result = super.get(name, start);
        if (result != NOT_FOUND)
            return result;
        
    	if ( designModule != null && PROP_PARAMS.equals(name) )
    	{
    		return getParamsScriptable();
    	}
    	
    	if ( PROP_REPORTCONTEXT.equals( name ) )
			return getReportContext( );
    	
    	return NOT_FOUND;
    }
    
    /**
     * Gets a object which implements the "reportContext" property
     * @throws InvocationTargetException 
     * @throws InstantiationException 
     * @throws IllegalAccessException 
     */
    private Object getReportContext()
    {
		if ( reportContextProp != null )
			return reportContextProp;

		assert designModule != null;
		
		reportContextProp = new ReportContextObject( designModule );
		
		return reportContextProp;
    }
    
    /**
	 * Gets a scriptable object which implements the "params" property
	 */
	private Scriptable getParamsScriptable( )
	{
		if ( paramsProp != null )
			return paramsProp;

		assert designModule != null;

		Map parameters = new HashMap( );
		List paramsList = designModule.getAllParameters( );
		for ( int i = 0; i < paramsList.size( ); i++ )
		{
			Object parameterObject = paramsList.get( i );
			if ( parameterObject instanceof ScalarParameterHandle )
			{
				ScalarParameterHandle parameterHandle = (ScalarParameterHandle) parameterObject;
				String str = parameterHandle.getDefaultValue( );
				Object value = getParamValueFromConfigFile( parameterHandle );
				if ( value == null )
				{
					value = getParamDefaultValue( parameterHandle );
				}
				parameters.put( ( (ScalarParameterHandle) parameterObject ).getQualifiedName( ),
						new DummyParameterAttribute( value, "" ) );
			}
		}
		paramsProp = new ReportParameters( parameters, this );
		return paramsProp;
	}
	
	/**
	 * Get the parameter value from the static report parameter
	 * 
	 * @return Object[] the static parameter values
	 */
/*	private Object[] getStaticParamValue( ScalarParameterHandle handle )
	{
		Iterator it = handle.choiceIterator( );
		if ( it == null )
		{
			return null;
		}
		
		List values = new ArrayList( );
		
		while ( it.hasNext( ) )
		{
			SelectionChoiceHandle choice = (SelectionChoiceHandle) it.next( );
			values.add( choice.getValue( ) );
		}
		
		return values.toArray( );
	}*/
    
	/**
	 * Gets the default value of a parameter. If a usable default value is
	 * defined, use it; otherwise use a default value appropriate for the data
	 * type
	 */
	private Object getParamDefaultValue( Object params )
	{
		if ( !( params instanceof ScalarParameterHandle ) )
			return null;

		ScalarParameterHandle sp = (ScalarParameterHandle) params;
		String defaultValue = sp.getDefaultValue( );
		String type = sp.getDataType( );
		if ( defaultValue == null )
		{
			// No default value; if param allows null value, null is used
			if ( sp.allowNull( ) )
				return null;

			// Return a fixed default value appropriate for the data type
			if ( DesignChoiceConstants.PARAM_TYPE_STRING.equals( type ) )
			{
				return "";
			}
			if ( DesignChoiceConstants.PARAM_TYPE_FLOAT.equals( type ) )
				return new Double( 0 );
			if ( DesignChoiceConstants.PARAM_TYPE_DECIMAL.equals( type ) )
				return new BigDecimal( (double) 0 );
			if ( DesignChoiceConstants.PARAM_TYPE_DATETIME.equals( type ) )
				return new Date( 0 );
			if ( DesignChoiceConstants.PARAM_TYPE_DATE.equals( type ) )
				return new java.sql.Date( 0 );
			if ( DesignChoiceConstants.PARAM_TYPE_TIME.equals( type ) )
				return new java.sql.Time( 0 );
			if ( DesignChoiceConstants.PARAM_TYPE_BOOLEAN.equals( type ) )
				return Boolean.FALSE;
			if ( DesignChoiceConstants.PARAM_TYPE_INTEGER.equals( type ) )
				return new Integer( 0 );

			// unknown parameter type; unexpected
			assert false;
			return null;
		}

		try
		{
			return DataTypeUtil.convert( defaultValue, DataAdapterUtil.modelDataTypeToCoreDataType( type ) );
		}
		catch ( BirtException e )
		{
			return null;
		}
	}
	
	/**
	 * Get the parameter value from .rptconfig file if it does exist
	 * 
	 * @return Object[] the parameter value
	 */
	private Object getParamValueFromConfigFile( ScalarParameterHandle paramHandle )
	{
		String designFileName = designModule.getFileName( );
		// replace the file extension
		// maybe the report is provided as a stream
		// then the config file cannot be found
		int index = designFileName.lastIndexOf( '.' );
		if ( index < 0 )
		{
			return null;
		}
		String reportConfigName = designFileName.substring( 0, index + 1 )
				+ "rptconfig";
		File file = new File( reportConfigName );
		if ( file.exists( ) )
		{
			String paraName = paramHandle.getName( );
			ScalarParameterHandle parameterHandle = (ScalarParameterHandle) designModule.findParameter( paraName );
			paraName = paraName + "_" + parameterHandle.getID( );
			SessionHandle sessionHandle = new DesignEngine( null ).newSessionHandle( ULocale.US );
			ReportDesignHandle rdHandle = null;
			// Open report config file
			try
			{
				rdHandle = sessionHandle.openDesign( reportConfigName );
			}
			catch ( DesignFileException e )
			{
				return null;
			}
			// handle config vars
			if ( rdHandle != null )
			{
				List values = new ArrayList( );
				Iterator configVars = rdHandle.configVariablesIterator( );
				while ( configVars != null && configVars.hasNext( ) )
				{
					ConfigVariableHandle configVar = (ConfigVariableHandle) configVars.next( );
					if ( configVar != null )
					{
						String varName = prepareConfigVarName( configVar.getName( ) );
						Object varValue = configVar.getValue( );
						if ( varName == null || varValue == null )
						{
							continue;
						}
						if ( varName.equals( paraName ) )
						{
							String value = (String) varValue;
							// if the value actually is in String type, convert
							// it by adding quotation marks
							values.add( value );
							// return value;
						}
						if ( isNullValue( varName, (String) varValue, paraName ) )
						{
							if ( !parameterHandle.getParamType( )
									.equals( DesignChoiceConstants.SCALAR_PARAM_TYPE_MULTI_VALUE ) )
							{
								return null;
							}
							return new Object[0];
						}
					}
				}
				if( values.size( ) > 0 )
				{
					if ( parameterHandle.getParamType( )
							.equals( DesignChoiceConstants.SCALAR_PARAM_TYPE_SIMPLE ) )
					{
						try
						{
							return DataTypeUtil.convert( 
									values.get( 0 ), DataAdapterUtil.modelDataTypeToCoreDataType( parameterHandle.getDataType() ) );
						}
						catch ( BirtException e )
						{
							return null;
						}
					}
					
					try {
						Object[] reValues = new Object[values.size()];
						for (int i = 0; i < reValues.length; i++)
						{
							reValues[i] = DataTypeUtil.convert( 
									values.get(i), DataAdapterUtil.modelDataTypeToCoreDataType(parameterHandle.getDataType()));
						}
						return reValues;
					}
					catch (BirtException e)
					{
						return null;
					}
				}
			}
		}
		return null;
	}

	/**
	 * To check whether the object with the specific type should be converted
	 * 
	 * @param type
	 * @return true if should be converted
	 */
	private static boolean isToBeConverted( String type )
	{
		return type.equals( DesignChoiceConstants.PARAM_TYPE_STRING )
				|| type.equals( DesignChoiceConstants.PARAM_TYPE_DATETIME )
				|| type.equals( DesignChoiceConstants.PARAM_TYPE_TIME )
				|| type.equals( DesignChoiceConstants.PARAM_TYPE_DATE );
	}

	/**
	 * Delete the last "_" part
	 * 
	 * @param name
	 * @return String
	 */
	private static String prepareConfigVarName( String name )
	{
		int index = name.lastIndexOf( "_" ); //$NON-NLS-1$
		return name.substring( 0, index );
	}

	private static boolean isNullValue( String varName, String varValue,
			String newParaName )
	{
		return varName.toLowerCase( ).startsWith( "__isnull" )
				&& varValue.equals( newParaName );
	}
    	
}
