/**************************************************************************
 * Copyright (c) 2004 Actuate Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Actuate Corporation  - initial API and implementation
 *  
 **************************************************************************/ 

package org.eclipse.birt.data.engine.expression;

import org.eclipse.birt.data.engine.core.DataException;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

/** 
 * This class encapulates an DtE expression that has been analyzed, rewritten,
 * and compiled during report query preparation. An instance of its derived class
 * is given to the factory as a handle. The factory uses this handle to evaluate
 * the compiled expression at query execution time.
 */
public abstract class CompiledExpression
{
	/*
	 * Constants returned by getType()
	 */
	
	/**
	 * The expression is a direct column reference ( "row.column_x", "row[2]" etc. )
	 */
	final static int TYPE_DIRECT_COL_REF 	= 1;
	
	/**
	 * The expression is a single aggregate function call
	 * e.g., "Total.sum( row.column_x )"
	 */
	final static int TYPE_SINGLE_AGGREGATE 	= 2;

	/**
	 * A complex expression 
	 */
	final static int TYPE_COMPLEX_EXPR 		= 3;
	
	/**
	 * A constant expression
	 */
	final static int TYPE_CONSTANT_EXPR		= 4;	
	
	/**
	 * An invalid expression
	 */
	final static int TYPE_INVALID_EXPR		= 5;

	/**
	 * gets the type of the compiled expression
	 */
	abstract int getType();
	
	/**
	 * Evaluates this expression
	 */
	public abstract Object evaluate( Context context, Scriptable scope )
		throws DataException;	
	
}
