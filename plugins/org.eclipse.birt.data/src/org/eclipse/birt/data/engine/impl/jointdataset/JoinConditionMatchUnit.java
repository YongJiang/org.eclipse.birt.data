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
package org.eclipse.birt.data.engine.impl.jointdataset;

import org.eclipse.birt.data.engine.api.IScriptExpression;
import org.eclipse.birt.data.engine.core.DataException;
import org.eclipse.birt.data.engine.script.ScriptEvalUtil;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

/**
 * Utility class which is used by IJoinConditionMatcher.
 */
public class JoinConditionMatchUnit
{
	private IScriptExpression expr = null;
	private Scriptable scope = null;

	
	/**
	 * Constructor
	 * 
	 * @param expr
	 * @param scope
	 */
	public JoinConditionMatchUnit( IScriptExpression expr, Scriptable scope )
	{
		this.expr = expr;
		this.scope = scope;
	}
	
	/**
	 * Get the value of current column.
	 * 
	 * @return
	 * @throws DataException
	 */
	public Object getColumnValue() throws DataException
	{
		Context cx = Context.enter( );
		try
		{
			Object leftValue = ScriptEvalUtil.evalExpr( this.expr,
					cx,
					this.scope,
					"JOINDATASET",
					0 );
				
			return leftValue;
		}	
		finally
		{
			Context.exit( );
		}
	}
}
