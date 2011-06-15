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

import java.util.List;

import org.eclipse.birt.data.engine.api.DataEngineContext;
import org.eclipse.birt.data.engine.api.IQueryDefinition;
import org.eclipse.birt.data.engine.core.DataException;
import org.eclipse.birt.data.engine.executor.QueryExecutionStrategyUtil.Strategy;
import org.eclipse.birt.data.engine.executor.dscache.DataSetToCache;
import org.eclipse.birt.data.engine.executor.transform.CachedResultSet;
import org.eclipse.birt.data.engine.executor.transform.ResultSetWrapper;
import org.eclipse.birt.data.engine.executor.transform.SimpleResultSet;
import org.eclipse.birt.data.engine.executor.transform.TransformationConstants;
import org.eclipse.birt.data.engine.impl.ComputedColumnHelper;
import org.eclipse.birt.data.engine.impl.DataEngineImpl;
import org.eclipse.birt.data.engine.impl.DataEngineSession;
import org.eclipse.birt.data.engine.odi.ICandidateQuery;
import org.eclipse.birt.data.engine.odi.ICustomDataSet;
import org.eclipse.birt.data.engine.odi.IEventHandler;
import org.eclipse.birt.data.engine.odi.IResultClass;
import org.eclipse.birt.data.engine.odi.IResultIterator;
import org.eclipse.birt.data.engine.odi.IResultObjectEvent;

/**
 * Implementation of ICandidateQuery
 */
public class CandidateQuery extends BaseQuery implements ICandidateQuery
{

	private ICustomDataSet customDataSet;

	private IResultIterator resultObjsIterator;
	private int groupingLevel;

	private IResultClass resultMetadata;
	private DataEngineSession session;

	public CandidateQuery( DataEngineSession session )
	{
		this.session = session;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.birt.data.engine.odi.ICandidateQuery#setCandidates(org.eclipse.birt.data.engine.odi.IResultIterator,
	 *      int)
	 */
	public void setCandidates( IResultIterator resultObjsIterator,
			int groupingLevel ) throws DataException
	{
		assert resultObjsIterator != null;
		this.resultObjsIterator = resultObjsIterator;
		this.groupingLevel = groupingLevel;

		resultMetadata = resultObjsIterator.getResultClass( );
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.birt.data.engine.odi.ICandidateQuery#setCandidates(org.eclipse.birt.data.engine.odi.ICustomDataSet)
	 */
	public void setCandidates( ICustomDataSet customDataSet )
			throws DataException
	{
		assert customDataSet != null;
		this.customDataSet = customDataSet;

		resultMetadata = customDataSet.getResultClass( );
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.birt.data.engine.odi.ICandidateQuery#getResultClass()
	 */
	public IResultClass getResultClass( )
	{
		return resultMetadata;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.birt.data.engine.odi.ICandidateQuery#execute()
	 */
	public IResultIterator execute( IEventHandler eventHandler )
			throws DataException
	{
		if ( customDataSet == null ) // sub query
		{
			// resultObjsIterator
			// for sub query, the event handler is no use
			return new CachedResultSet( this,
					resultMetadata,
					resultObjsIterator,
					groupingLevel,
					eventHandler,
					session );
		}
		else
		// scripted query
		{
			if ( this.session.getDataSetCacheManager( ).doesSaveToCache( ) == false )
			{	
				if ( ( ( session.getEngineContext( ).getMode( ) == DataEngineContext.DIRECT_PRESENTATION || session.getEngineContext( )
						.getMode( ) == DataEngineContext.MODE_GENERATION ) )
						&& this.getQueryDefinition( ) instanceof IQueryDefinition )
				{
					IQueryDefinition queryDefn = (IQueryDefinition) this.getQueryDefinition( );
					
					Strategy strategy = QueryExecutionStrategyUtil.getQueryExecutionStrategy( this.session, queryDefn,
							queryDefn.getDataSetName( ) == null
							? null
							: ( (DataEngineImpl) this.session.getEngine( ) ).getDataSetDesign( queryDefn.getDataSetName( ) ) );
					if ( strategy  != Strategy.Complex )
					{
						for( IResultObjectEvent event: (List<IResultObjectEvent>)this.getFetchEvents( ) )
						{
							if( event instanceof ComputedColumnHelper )
							{
								((ComputedColumnHelper)event).setModel( TransformationConstants.ALL_MODEL );
							}
						}
						
						SimpleResultSet simpleResult = new SimpleResultSet( this,
								customDataSet,
								resultMetadata,
								eventHandler,
								this.getGrouping( ),
								this.session,
								strategy == Strategy.SimpleLookingFoward);
						
						IResultIterator it = strategy == Strategy.SimpleLookingFoward
								? new ResultSetWrapper( this.session, simpleResult )
								: simpleResult;
						eventHandler.handleEndOfDataSetProcess( it );
						return it;
					}
				}
							
				return new CachedResultSet( this,
						customDataSet,
						eventHandler,
						session );
			}
			else
				return new CachedResultSet( this,
						resultMetadata,
						new DataSetToCache( customDataSet,
								resultMetadata,
								session ),
						eventHandler,
						session );

		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.birt.data.engine.odi.ICandidateQuery#close()
	 */
	public void close( )
	{
		// nothing

	}

}
