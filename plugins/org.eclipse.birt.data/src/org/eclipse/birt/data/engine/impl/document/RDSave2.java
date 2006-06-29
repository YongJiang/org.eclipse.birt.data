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

package org.eclipse.birt.data.engine.impl.document;

import java.util.Map;

import org.eclipse.birt.data.engine.api.DataEngineContext;
import org.eclipse.birt.data.engine.api.IBaseQueryDefinition;
import org.eclipse.birt.data.engine.core.DataException;
import org.eclipse.birt.data.engine.odi.IResultIterator;

/**
 * Used to save the query which is running based on a report document and the
 * result is based on the result set of report document instead of data set. For
 * the latter case, RDSave class will be used.
 */
class RDSave2 implements IRDSave
{
	private DataEngineContext context;
	private StreamManager streamManager;	
	private RDSaveUtil saveUtilHelper;
	
	/**
	 * @param context
	 * @param queryDefn
	 * @param queryResultID
	 * @param rowCount
	 * @param subQueryName
	 * @param subQueryIndex
	 * @throws DataException
	 */
	RDSave2( DataEngineContext context, IBaseQueryDefinition queryDefn,
			QueryResultInfo queryResultInfo ) throws DataException
	{
		this.context = context;

		this.streamManager = new StreamManager( context, queryResultInfo );
		this.saveUtilHelper = new RDSaveUtil( this.context.getMode( ),
				queryDefn,
				this.streamManager );
	}

	/*
	 * @see org.eclipse.birt.data.engine.impl.document.RDSave#saveExprValue(int,
	 *      java.util.Map)
	 */
	public void saveExprValue( int currIndex, Map valueMap )
			throws DataException
	{
		// do nothing
	}

	/*
	 * @see org.eclipse.birt.data.engine.impl.document.RDSave#saveFinish(int)
	 */
	public void saveFinish( int currIndex ) throws DataException
	{
		this.saveUtilHelper.saveQueryDefn( );
		this.saveUtilHelper.saveChildQueryID( );
	}

	/*
	 * @see org.eclipse.birt.data.engine.impl.document.IRDSave#saveResultIterator(org.eclipse.birt.data.engine.odi.IResultIterator,
	 *      int, int[])
	 */
	public void saveResultIterator( IResultIterator odiResult, int groupLevel,
			int[] subQueryInfo ) throws DataException
	{
		saveUtilHelper.saveResultIterator( odiResult, groupLevel, subQueryInfo );
	}

}
