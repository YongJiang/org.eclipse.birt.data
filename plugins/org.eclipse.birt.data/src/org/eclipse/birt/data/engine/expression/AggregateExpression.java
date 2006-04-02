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
package org.eclipse.birt.data.engine.expression;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.birt.data.engine.api.aggregation.IAggregation;

/**
 * The compiled form of an expression that contains only a single aggregate function call.
 * For example, "Total.Sum(row.x)", or "Total.movingAve( row.y + row.z, 30, row.z != 0 )".
 */
public final class AggregateExpression extends BytecodeExpression
{
	private IAggregation aggregation;
    private List arguments;
    private int m_id;		// Id of this expression in the aggregate registry

	AggregateExpression( IAggregation aggregation )
    {
		logger.entering( AggregateExpression.class.getName( ),
				"AggregateExpression" );
    	this.aggregation = aggregation;
    	this.arguments = new ArrayList();
		logger.exiting( AggregateExpression.class.getName( ),
				"AggregateExpression" );
    }
    
    public IAggregation getAggregation()
    {
    	return aggregation;
    }
    
    /*
     * @see java.lang.Object#equals(java.lang.Object)
     */
	public boolean equals( Object other )
	{
		if ( other == null || !( other instanceof AggregateExpression ) )
			return false;

		AggregateExpression expr2 = (AggregateExpression) other;

		if ( !aggregation.getName( ).equals( expr2.getAggregation( )
				.getName( ) ) )
			return false;
		if ( arguments.size( ) != expr2.getArguments( ).size( ) )
			return false;
		for ( int i = 0; i < arguments.size( ); i++ )
		{
			if ( !arguments.get( i ).equals( expr2.getArguments( ).get( i ) ) )
				return false;
		}
		return true;
	}
    
    /**
     * Returns a list of arguments for this aggregation expression.  Each element 
     * is an instance of <code>CompiledExpression</code>.
     * @return	a list of arguments for this aggregation expression.
     */
    public List getArguments()
    {
    	return arguments;
    }
    
    /**
     * Adds the specific <code>CompiledExpression</code> as an argument to this 
     * <code>AggregateExpression</code>.
     * @param expr	the <code>CompiledExpression</code> argument for this 
     * 				<code>AggregateExpression</code>.
     */
    void addArgument( CompiledExpression expr )
    {
    	assert( expr != null );
    	arguments.add( expr );
    }
    
    public int getType()
    {
        return TYPE_SINGLE_AGGREGATE;
    }
    
    // Sets the ID of this aggregate expression in the aggregate registry
    public void setRegId( int id)
    {
    	m_id = id;
    }
    
    // Gets the ID of this aggregate expression in the registry
    public int getRegId( )
    {
    	return m_id;
    }
    
}
