/*
 *************************************************************************
 * Copyright (c) 2004 Actuate Corporation.
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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.birt.data.engine.core.DataException;
import org.eclipse.birt.data.engine.executor.transform.IExpressionProcessor;
import org.eclipse.birt.data.engine.odi.IQuery;
import org.eclipse.birt.data.engine.odi.IResultObjectEvent;

/**
 * Implementation of the ODI IQuery interface. Common base class for DataSourceQuery and CandidateQuery
 */
public abstract class BaseQuery implements IQuery
{
    private SortSpec[] 	sorts;
    private GroupSpec[] 	groups;
    private int			maxRows = 0;
    private List 			fetchEventList;
    
    private IExpressionProcessor exprProcessor;
    
    /**
     * @see org.eclipse.birt.data.engine.odi.IQuery#setOrdering(java.util.List)
     */
    public void setOrdering(List sortSpecs) throws DataException
    {
    	if( sortSpecs == null )
    		sorts = null;
    	else
    		sorts = (SortSpec[]) sortSpecs.toArray( new SortSpec[0] );
    }

    /**
     * @see org.eclipse.birt.data.engine.odi.IQuery#setGrouping(java.util.List)
     */
    public void setGrouping(List groupSpecs) throws DataException
    {
        if( groupSpecs == null )
        	groups = null;
        else
        	groups = ( GroupSpec[] ) groupSpecs.toArray( new GroupSpec[0]);
    }
    
    /**
     * @see org.eclipse.birt.data.engine.odi.IQuery#setMaxRows(int)
     */
    public void setMaxRows(int maxRows)
    {
        this.maxRows = maxRows;
    }
    
    /**
     * Gets the query's sort specification. Returns null if no sort specs are defined.
     */
    public SortSpec[] getOrdering()
    {
        return sorts;
    }
    
    /**
     * Gets the query's grouping specification. Returns null if no groups are defined.
     */
    public GroupSpec[] getGrouping()
    {
        return groups;
    }
    
    public int getMaxRows()
    {
        return maxRows;
    }
    
    /**
     * Add event to fetch event list
     */
	public void addOnFetchEvent( IResultObjectEvent event )
	{
		assert event != null;
		
		if ( fetchEventList == null )
			fetchEventList = new ArrayList( );
		
		fetchEventList.add( event );
	}
	
	public List getFetchEvents( )
	{
		return fetchEventList;
	}
	
	/*
	 * @see org.eclipse.birt.data.engine.odi.IQuery#setExprProcessor(org.eclipse.birt.data.engine.executor.transformation.IExpressionProcessor)
	 */
	public void setExprProcessor( IExpressionProcessor exprProcessor )
	{
		this.exprProcessor = exprProcessor;
	}
    
    /**
	 * @return
	 */
	public IExpressionProcessor getExprProcessor( )
	{
		return exprProcessor;
	}
	
}
