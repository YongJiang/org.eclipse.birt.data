
/*******************************************************************************
 * Copyright (c) 2004, 2005 Actuate Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Actuate Corporation  - initial API and implementation
 *******************************************************************************/
package org.eclipse.birt.data.engine.impl;

import java.util.logging.Logger;

import org.eclipse.birt.core.exception.BirtException;
import org.eclipse.birt.data.engine.api.querydefn.ComputedColumn;
import org.eclipse.birt.data.engine.impl.group.ICalculator;


/**
 * This class is a subclass of ComputedColumn which contains a ICalculator
 * instance. To get the result value the method calculate() of ICalculator
 * should be called after evaluating the JavaScript expression.
 */

public class GroupComputedColumn extends ComputedColumn
{
	private ICalculator calcultor = null;
	
	private static Logger logger = Logger.getLogger( GroupComputedColumn.class.getName( ) );

	/**
	 * @param name
	 * @param expr
	 */
    public GroupComputedColumn( String name, String expr, ICalculator calcultor )
	{
		super(name, expr );
		Object[] params = {
				name, expr, calcultor
		};
		logger.entering( GroupComputedColumn.class.getName( ),
				"GroupComputedColumn",
				params );

		this.calcultor = calcultor;
		logger.exiting( GroupComputedColumn.class.getName( ),
				"GroupComputedColumn" );
    }
    
    /**
     * Constructs a new computed column with specified name and expression
     * @param name Name of computed column
     * @param expr Expression of computed column
     * @param dataType data Type of computed column
     * @param calcultor
     */
    public GroupComputedColumn( String name, String expr, int dataType, ICalculator calcultor )
	{
    	super(name, expr, dataType );
		Object[] params = { name, expr, new Integer(dataType), calcultor };
		logger.entering( GroupComputedColumn.class.getName( ),
				"GroupComputedColumn",
				params );

		this.calcultor = calcultor;
		logger.exiting( GroupComputedColumn.class.getName( ), "GroupComputedColumn" );
    }
    
    /**
     * 
     * @return
     * @throws BirtException 
     */
    public Object calculate( Object value ) throws BirtException
    {
    	return calcultor.calculate( value );
    }
}
