/*******************************************************************************
 * Copyright (c) 2004, 2008 Actuate Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Actuate Corporation  - initial API and implementation
 *******************************************************************************/

package org.eclipse.birt.data.engine.api.aggregation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.birt.core.framework.FrameworkException;
import org.eclipse.birt.core.framework.IConfigurationElement;
import org.eclipse.birt.core.framework.IExtension;
import org.eclipse.birt.core.framework.IExtensionPoint;
import org.eclipse.birt.core.framework.IExtensionRegistry;
import org.eclipse.birt.core.framework.Platform;
import org.eclipse.birt.data.engine.aggregation.AggrFunctionWrapper;
import org.eclipse.birt.data.engine.aggregation.AggrFunctionWrapper.ParameterDefn;
import org.eclipse.birt.data.engine.core.DataException;
import org.eclipse.birt.data.engine.i18n.DataResourceHandle;
import org.eclipse.birt.data.engine.i18n.ResourceConstants;

/**
 * 
 */

public class AggregationManager
{

	private static final String ELEMENT_AGGREGATION_FACTORY = "AggregationFactory";//$NON-NLS-1$
	private static final String ATTRIBUTE_AGGREGATION_CLASS = "aggregationClass";//$NON-NLS-1$
	private static final String EXTENSION_POINT = "org.eclipse.birt.data.aggregation";//$NON-NLS-1$
	private static final String ELEMENT_AGGREGATIONS = "Aggregations";//$NON-NLS-1$
	private static final String ELEMENT_AGGREGATION = "Aggregation";//$NON-NLS-1$

	private static final String ELEMENT_UIINFO = "UIInfo";//$NON-NLS-1$
	private static final String ATTRIBUTE_PARAMETER_META_INFO = "parameterMetaInfo";//$NON-NLS-1$
	private static final String ATTRIBUTE_TEXT_DATA = "textData";//$NON-NLS-1$

	private static AggregationManager instance;
	public static Map aggrMap;

	/**
	 * allowed aggregation function names in x-tab
	 */
	private static String[] xTabAggrNames = new String[]{
			"SUM",//$NON-NLS-1$
			"AVE",//$NON-NLS-1$
			"MAX",//$NON-NLS-1$
			"MIN",//$NON-NLS-1$
			"FIRST",//$NON-NLS-1$
			"LAST",//$NON-NLS-1$
			"COUNT",//$NON-NLS-1$
			"COUNTDISTINCT"//$NON-NLS-1$
	};

	public static final int AGGR_TABULAR = 0;
	public static final int AGGR_XTAB = 1;
	public static final int AGGR_MEASURE = 2;

	/**
	 * allowed aggregation function names in cube measure.
	 */
	private static String[] measureAggrNames = new String[]{
			"SUM",//$NON-NLS-1$
			"MAX", //$NON-NLS-1$
			"MIN", //$NON-NLS-1$
			"FIRST", //$NON-NLS-1$
			"LAST", //$NON-NLS-1$
			"COUNT", //$NON-NLS-1$
			"COUNTDISTINCT"//$NON-NLS-1$
	};

	private static List allAggrNames = new ArrayList( );

	/**
	 * Return a shared instance of AggregationManager.
	 * 
	 * @return
	 * @throws DataException
	 */
	public static AggregationManager getInstance( ) throws DataException
	{
		if ( instance == null )
		{
			synchronized ( AggregationManager.class )
			{
				if ( instance == null )
					instance = new AggregationManager( );
			}
		}

		return instance;
	}

	/**
	 * 
	 */
	private AggregationManager( ) throws DataException
	{
		aggrMap = new HashMap( );
		populateAggregations( );
	}

	/**
	 * 
	 * @throws DataException
	 */
	private void populateAggregations( ) throws DataException
	{
		IExtensionRegistry extReg = Platform.getExtensionRegistry( );
		IExtensionPoint extPoint = extReg.getExtensionPoint( EXTENSION_POINT );

		if ( extPoint == null )
			return;

		IExtension[] exts = extPoint.getExtensions( );
		if ( exts == null )
			return;

		for ( int e = 0; e < exts.length; e++ )
		{
			IConfigurationElement[] configElems = exts[e].getConfigurationElements( );
			if ( configElems == null )
				continue;

			for ( int i = 0; i < configElems.length; i++ )
			{
				if ( configElems[i].getName( ).equals( ELEMENT_AGGREGATIONS ) )
				{
					IConfigurationElement[] subElems = configElems[i].getChildren( ELEMENT_AGGREGATION_FACTORY );
					populateFactoryAggregations( subElems );
					subElems = configElems[i].getChildren( ELEMENT_AGGREGATION );
					populateDeprecatedAggregations( subElems );
				}
			}
		}

	}

	/**
	 * 
	 * @param subElems
	 * @throws DataException
	 */
	private void populateFactoryAggregations( IConfigurationElement[] subElems )
			throws DataException
	{
		if ( subElems == null )
			return;
		for ( int j = 0; j < subElems.length; j++ )
		{
			try
			{
				IAggregationFactory factory = (IAggregationFactory) subElems[j].createExecutableExtension( "class" );
				List functions = factory.getAggregations( );
				for ( Iterator itr = functions.iterator( ); itr.hasNext( ); )
				{
					IAggrFunction aggrFunc = (IAggrFunction) itr.next( );
					String name = aggrFunc.getName( ).toUpperCase( );
					if ( aggrMap.put( name, aggrFunc ) != null )
						throw new DataException( ResourceConstants.DUPLICATE_AGGREGATION_NAME,
								name );
					allAggrNames.add( name );
				}
			}
			catch ( FrameworkException exception )
			{
				// TODO: log this exception or provide public
				// interface for the user to get uninstantiated
				// function names
			}
		}

	}

	/**
	 * 
	 * @param subElems
	 * @throws DataException
	 */
	private void populateDeprecatedAggregations(
			IConfigurationElement[] subElems ) throws DataException
	{
		if ( subElems == null )
			return;
		for ( int j = 0; j < subElems.length; j++ )
		{
			try
			{
				IAggregation aggrFunc = (IAggregation) subElems[j].createExecutableExtension( ATTRIBUTE_AGGREGATION_CLASS );
				String name = aggrFunc.getName( ).toUpperCase( );
				
				AggrFunctionWrapper aggrWrapper = new AggrFunctionWrapper( aggrFunc );
				populateExtendedAggrInfo( name,
						aggrFunc,
						subElems[j],
						aggrWrapper );
				
				if ( aggrMap.put( name, aggrWrapper ) != null )
					throw new DataException( ResourceConstants.DUPLICATE_AGGREGATION_NAME,
							name );
				allAggrNames.add( name );
			}
			catch ( FrameworkException exception )
			{
				// TODO: log this exception or provide public
				// interface for the user to get uninstantiated
				// function names
			}
		}
	}

	/**
	 * populate the extended extensions information.
	 * 
	 * @param name
	 * @param aggrFunc
	 * @param elem
	 * @param aggrWrapper 
	 */
	private void populateExtendedAggrInfo( String name, IAggregation aggrFunc,
			IConfigurationElement elem, AggrFunctionWrapper aggrWrapper )
	{
		IConfigurationElement[] uiInfo = elem.getChildren( ELEMENT_UIINFO );
		assert ( uiInfo != null && uiInfo.length == 1 );
		String paramInfo = uiInfo[0].getAttribute( ATTRIBUTE_PARAMETER_META_INFO );
		String textInfo = uiInfo[0].getAttribute( ATTRIBUTE_TEXT_DATA );
		aggrWrapper.setDisplayName( textInfo );
		// populate parameters to the aggrWrapper
		List paramList = new ArrayList( );
		String[] paramInfos = paramInfo.split( "," );//$NON-NLS-1$
		boolean[] paramFlags = aggrFunc.getParameterDefn( );
		if ( paramInfos != null && paramInfos.length > 0 )
		{
			populateDataFiledParameterDefn( paramList );
			for ( int k = 0; k < paramInfos.length; k++ )
			{
				final String s = paramInfos[k].trim( );
				int index = s.indexOf( ' ' );
				String paramName = null;
				if ( index > 0 )
				{
					paramName = s.substring( index + 1 ).trim( );
				}
				else
				{
					paramName = paramInfos[k];
				}
				if ( k + 1 >= paramFlags.length )
				{
					break;
				}
				ParameterDefn paramDefn = new ParameterDefn( paramName,
						paramName,
						!paramFlags[k + 1],
						false );
				paramList.add( paramDefn );
			}
		}
		aggrWrapper.setParameterDefn( null );
	}

	/**
	 * populate the default data field parameter definition to the paramList.
	 * 
	 * @param paramList
	 */
	private void populateDataFiledParameterDefn( List paramList )
	{
		String dataFileld = DataResourceHandle.getInstance( )
				.getMessage( ResourceConstants.AGGREGATION_DATA_FIELD_DISPLAY_NAME );
		ParameterDefn dataFieldDefn = new ParameterDefn( "Data Field",//$NON-NLS-1$
				dataFileld,
				false,
				true );
		paramList.add( dataFieldDefn );
	}

	/**
	 * Destroy shared instance of AggregationManager.
	 * 
	 */
	public static void destroyInstance( )
	{
		instance = null;
		aggrMap = null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.birt.data.engine.api.aggregation.IAggregationManager#getAggrFunction(java.lang.String)
	 */
	public IAggrFunction getAggregation( String name )
	{
		return name != null ? (IAggrFunction) aggrMap.get( name.toUpperCase( ) )
				: null;
	}

	/**
	 * get a list of IAggrFunction instances for the specified type, which must
	 * be one of the values below:
	 * AggregationManager.AGGR_TABULAR,AggregationManager.AGGR_XTAB,AggregationManager.AGGR_MEASURE.
	 * 
	 * @param type
	 * @return
	 */
	public List getAggregations( int type )
	{
		switch ( type )
		{
			case AGGR_TABULAR :
				return getResult( allAggrNames.toArray( ) );
			case AGGR_XTAB :
				return getResult( xTabAggrNames );
			case AGGR_MEASURE :
				return getResult( measureAggrNames );
		}
		return new ArrayList( );
	}

	/**
	 * 
	 * @param names
	 * @return
	 */
	private List getResult( Object[] names )
	{
		List list = new ArrayList( );
		for ( int i = 0; i < names.length; i++ )
		{
			Object aggrFunc = aggrMap.get( names[i] );
			if ( aggrFunc != null )
			{
				list.add( aggrFunc );
			}
		}
		return list;
	}

	/**
	 * get a list of IAggrFunction instances which contains all the aggregations
	 * function.
	 * 
	 * @return
	 */
	public List getAggregations( )
	{
		return getResult( allAggrNames.toArray( ) );
	}
}