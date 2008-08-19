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

package org.eclipse.birt.data.engine.impl;

import org.eclipse.birt.core.data.ExpressionUtil;
import org.eclipse.birt.core.exception.BirtException;
import org.eclipse.birt.data.engine.api.IBaseQueryDefinition;
import org.eclipse.birt.data.engine.api.IBinding;
import org.eclipse.birt.data.engine.api.IQueryDefinition;
import org.eclipse.birt.data.engine.api.querydefn.Binding;
import org.eclipse.birt.data.engine.api.querydefn.QueryDefinition;
import org.eclipse.birt.data.engine.api.querydefn.ScriptExpression;
import org.eclipse.birt.data.engine.api.querydefn.SubqueryLocator;
import org.eclipse.birt.data.engine.core.DataException;

/**
 * 
 */

public class PreparedIVDataExtractionQuery extends PreparedIVQuerySourceQuery
{

	PreparedIVDataExtractionQuery( DataEngineImpl dataEngine,
			IQueryDefinition queryDefn ) throws DataException
	{
		super( dataEngine, queryDefn );
		// TODO Auto-generated constructor stub
	}

	protected void prepareQuery( ) throws DataException
	{
		try
		{
			IBinding[] bindings = null;
			if ( this.queryDefn.getSourceQuery( ) instanceof SubqueryLocator )
			{
				this.queryResults = engine.getQueryResults( getParentQueryResultsID( (SubqueryLocator) ( queryDefn.getSourceQuery( ) ) ) );
				IQueryDefinition queryDefinition = queryResults.getPreparedQuery( )
						.getReportQueryDefn( );
				bindings = getSubQueryBindings( queryDefinition,
						( (SubqueryLocator) this.queryDefn.getSourceQuery( ) ).getName( ) );
			}
			else
			{
				this.queryResults = PreparedQueryUtil.newInstance( dataEngine,
						(IQueryDefinition) queryDefn.getSourceQuery( ),
						null ).execute( null );

				if ( queryResults != null
						&& queryResults.getPreparedQuery( ) != null )
				{
					IQueryDefinition queryDefinition = queryResults.getPreparedQuery( )
							.getReportQueryDefn( );
					bindings = (IBinding[]) queryDefinition.getBindings( )
							.values( )
							.toArray( new IBinding[0] );
				}
				else
				{
					bindings = new IBinding[0];
				}
			}
			for ( int i = 0; i < bindings.length; i++ )
			{
				IBinding binding = bindings[i];
				this.queryDefn.addBinding( new Binding( binding.getBindingName( ),
						new ScriptExpression( ExpressionUtil.createJSDataSetRowExpression( binding.getBindingName( ) ),
								binding.getDataType( ) ) ) );
			}
		}
		catch ( BirtException e )
		{
			throw DataException.wrap( e );
		}
	}

	/**
	 * 
	 * @param subqueryLocator
	 * @return
	 */
	private String getParentQueryResultsID( SubqueryLocator subqueryLocator )
	{
		IBaseQueryDefinition baseQueryDefinition = subqueryLocator.getParentQuery( );
		while ( !( baseQueryDefinition instanceof QueryDefinition ) )
		{
			baseQueryDefinition = baseQueryDefinition.getParentQuery( );
		}
		return ( (QueryDefinition) baseQueryDefinition ).getQueryResultsID( );
	}
}