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
package org.eclipse.birt.data.engine.script;

import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.birt.core.data.DataTypeUtil;
import org.eclipse.birt.core.exception.BirtException;
import org.eclipse.birt.data.engine.core.DataException;
import org.eclipse.birt.data.engine.api.IBaseExpression;
import org.eclipse.birt.data.engine.api.IConditionalExpression;
import org.eclipse.birt.data.engine.api.IScriptExpression;
import org.eclipse.birt.data.engine.api.querydefn.ConditionalExpression;
import org.eclipse.birt.data.engine.i18n.ResourceConstants;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.EcmaError;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.JavaScriptException;
import org.mozilla.javascript.Scriptable;


/**
 * Static utility methods to help evaluating Javascript expressions 
 */
public class ScriptEvalUtil
{
	/**
	 * Evaluates a conditional expression. A conditional expression comprises of
	 * a Javascript expression, an operator, and up to 2 operands (which are 
	 * Javascript expressions themselves).
	 * @return A Boolean result
	 */
	public static Object evalConditionalExpr( Object resultObject,
			int operator, Object resultOp1, Object resultOp2 )
			throws DataException
	{
		boolean result = false;
		switch ( operator )
		{
			case IConditionalExpression.OP_EQ :
				validateExpression( resultObject, resultOp1 );
				result = compare( resultObject, resultOp1 ) == 0;
				break;
			case IConditionalExpression.OP_NE :
				validateExpression( resultObject, resultOp1 );
				result = compare( resultObject, resultOp1 ) != 0;
				break;
			case IConditionalExpression.OP_LT :
				validateExpression( resultObject, resultOp1 );
				result = compare( resultObject, resultOp1 ) < 0;
				break;
			case IConditionalExpression.OP_LE :
				validateExpression( resultObject, resultOp1 );
				result = compare( resultObject, resultOp1 ) <= 0;
				break;
			case IConditionalExpression.OP_GE :
				validateExpression( resultObject, resultOp1 );
				result = compare( resultObject, resultOp1 ) >= 0;
				break;
			case IConditionalExpression.OP_GT :
				validateExpression( resultObject, resultOp1 );
				result = compare( resultObject, resultOp1 ) > 0;
				break;
			case IConditionalExpression.OP_BETWEEN :
				validateExpression( resultObject, resultOp1, resultOp2 );
				result = between( resultObject, resultOp1, resultOp2 );
				break;
			case IConditionalExpression.OP_NOT_BETWEEN :
				validateExpression( resultObject, resultOp1, resultOp2 );
				result = !( between( resultObject, resultOp1, resultOp2 ) );
				break;
			case IConditionalExpression.OP_NULL :
				result = resultObject == null;
				break;
			case IConditionalExpression.OP_NOT_NULL :
				result = resultObject != null;
				break;
			case IConditionalExpression.OP_TRUE :
				validateExpression( resultObject );
				result = isTrue( resultObject );
				break;
			case IConditionalExpression.OP_FALSE :
				validateExpression( resultObject );
				result = !isTrue( resultObject );
				break;
			case IConditionalExpression.OP_LIKE :
				validateExpression( resultObject, resultOp1 );
				result = like( resultObject, resultOp1 );
				break;
			case IConditionalExpression.OP_TOP_N :
			//TODO
			case IConditionalExpression.OP_BOTTOM_N :
			//TODO
			case IConditionalExpression.OP_TOP_PERCENT :
			//TODO
			case IConditionalExpression.OP_BOTTOM_PERCENT :
			//TODO
			case IConditionalExpression.OP_ANY :
			//TODO
			default :
				throw new DataException(
						ResourceConstants.UNSUPPORTTED_OPERATOR );
		}
		return new Boolean( result );
	}

	private static void validateExpression( Object ob1 ) throws DataException
	{
		if ( ob1 == null )
		{
			throw new DataException( ResourceConstants.INVALID_EXPR );
		}
	}

	private static void validateExpression( Object ob1, Object ob2 )
			throws DataException
	{
		if ( ob1 == null || ob2 == null )
		{
			throw new DataException( ResourceConstants.INVALID_EXPR );
		}
	}


	private static void validateExpression( Object ob1, Object ob2, Object ob3 )
			throws DataException
	{
		if ( ob1 == null || ob2 == null || ob3 == null )
		{
			throw new DataException( ResourceConstants.INVALID_EXPR );
		}
	}
	
	private static boolean isSameType( Object resultExpr, Object resultOp1 )
	{
		return resultExpr.getClass( ).equals( resultOp1.getClass( ) );
	}

	private static boolean isNumericOrString( Object result )
	{
		return ( result instanceof Number ) || ( result instanceof String );
	}

	private static boolean isDateOrString( Object result )
	{
		return ( result instanceof Date ) || ( result instanceof String );
	}
	
	private static boolean isTrue( Object obj ) throws DataException
	{
		assert obj != null;
		try
		{
			return DataTypeUtil.toBoolean( obj ).equals( Boolean.TRUE );
		}
		catch ( BirtException e )
		{
			throw new DataException( ResourceConstants.DATATYPEUTIL_ERROR, e );
		}
	}
	
	private static int compare( Object obj1, Object obj2 ) throws DataException
	{
		assert obj1 != null && obj2 != null;
		
		try
		{
			if ( isSameType( obj1, obj2 ) )
			{
				return ( (Comparable) obj1 ).compareTo( obj2 );
			}
			else if ( isNumericOrString( obj1 ) && isNumericOrString( obj2 ) )
			{
				return DataTypeUtil.toDouble( obj1 ).compareTo(
						DataTypeUtil.toDouble( obj2 ) );
			}
			else if ( isDateOrString( obj1 ) && isDateOrString( obj2 ) )
			{
				return DataTypeUtil.toDate( obj1 ).compareTo(
						DataTypeUtil.toDate( obj2 ) );
			}
			else
				throw new DataException( ResourceConstants.INVALID_TYPE_IN_EXPR );
		}
		catch ( BirtException e )
		{
			throw new DataException( ResourceConstants.DATATYPEUTIL_ERROR, e );
		}
	}
	
	/**
	 * Evaluates a ROM script and converts the result type into one accepted by BIRT:
	 * Double (for all numeric types), java.util.Date, String, Boolean. Converts 
	 * Javascript exception and script runtime exceptions to DataException
	 * 
	 * @throws DataException 
	 */
	public static Object evaluateJSExpr(Context cx, Scriptable scope,
			String scriptText, String source, int lineNo)
			throws DataException 
	{
		Object result = null;
		try
		{
			result = cx.evaluateString(scope, scriptText, source, lineNo, null);
		}
		catch ( Exception e)
		{
			RethrowJSEvalException(e);
		}
		return convertNativeObjToJavaObj(result);
	}
	
	/**
	 * Converts the result type into one accepted by BIRT:
	 * Double (for all numeric types), java.util.Date, String, Boolean. 
	 */	
	public static Object convertNativeObjToJavaObj(Object inputObj){
		if ( inputObj instanceof Scriptable) 
		{
			// Return type is a Javascript native object
			// Convert to Java object with same value
			String jsClass = ((Scriptable) inputObj).getClassName();
			if (jsClass.equals("Date")) 
			{
				return new Date((long) Context.toNumber(inputObj));
			} 
			else if (jsClass.equals("Boolean")) 
			{
				return new Boolean(Context.toBoolean(inputObj));
			} 
			else if (jsClass.equals("Number")) 
			{
				return new Double(Context.toNumber(inputObj));
			} 
			else 
			{
				// For JS "String" type, toString gives the correct result
				// For all other types that we cannot handle, toString is the best we can do
				return inputObj.toString();
			}
		}
		return inputObj;
	}
	
	/**
	 * Evaluates a IJSExpression or IConditionalExpression
	 */  
	public static Object evalExpr( IBaseExpression expr, Context cx, Scriptable scope, 
				String source, int lineNo )
		throws DataException
	{
		if ( expr == null )
		{
			return null;
		}
		else if ( expr instanceof IConditionalExpression)
		{
			ConditionalExpression ConditionalExpr = (ConditionalExpression) expr;
			Object expression = evalExpr( ConditionalExpr.getExpression( ), cx, scope, source, lineNo );
			Object Op1 = evalExpr( ConditionalExpr.getOperand1( ), cx, scope, source, lineNo );
			Object Op2 = evalExpr( ConditionalExpr.getOperand2( ), cx, scope, source, lineNo );
			return evalConditionalExpr( expression, ConditionalExpr.getOperator( ), Op1, Op2 );
		}
		else
		{
			IScriptExpression jsExpr = (IScriptExpression) expr;
			return evaluateJSExpr( cx, scope, jsExpr.getText(), source, lineNo );
		}
	}
	
	
	/**
	 * Converts an exception which occurred in the evaluation of a ROM script to a DataException,
	 * and rethrows such exception. This method never returns.
	 */
	public static void RethrowJSEvalException( Exception e ) throws DataException
	{
		// Only convert exceptions known to be thrown by Rhino engine; other exceptions
		// are not handled
		
		// TODO: log the expression (lineSource) to log file
		String source = null;
		int lineNo = 0;
		if ( e instanceof JavaScriptException )
		{
			JavaScriptException err = (JavaScriptException )e;
			source = err.getSourceName();
			lineNo = err.getLineNumber();
		}
		else if ( e instanceof EcmaError )
		{
			EcmaError err = (EcmaError) e;
			source = err.getSourceName();
			lineNo = err.getLineNumber();
		}
		else if ( e instanceof EvaluatorException)
		{
			EvaluatorException err = (EvaluatorException )e;
			source = err.getSourceName();
			lineNo = err.getLineNumber();
		}
		else if ( e instanceof RuntimeException)
		{
			// Not an expected error
			throw (RuntimeException) e;
		}
		
		throw new DataException( ResourceConstants.SCRIPT_EVAL_ERROR, e,
				new Object[] { source, new Integer(lineNo), e.getMessage() });
		
	}

	private static boolean like( Object obj1, Object pattern ) throws DataException
	{
		boolean b = false;
		try
		{
			Pattern p = Pattern.compile( pattern.toString( ) );
			Matcher m = p.matcher( obj1.toString( ) );
			b = m.matches( );
		}
		catch ( RuntimeException e )
		{
			throw new DataException( ResourceConstants.INVALID_TYPE, e );
		}
		return b;
	}
	
	private static boolean between( Object resultObject, Object resultOp1,
			Object resultOp2 ) throws DataException
	{
		Object min, max;
		if ( compare( resultOp1, resultOp2 ) <= 0 )
		{
			min = resultOp1;
			max = resultOp2;
		}
		else
		{
			min = resultOp2;
			max = resultOp1;
		}
		return compare( resultObject, min ) >= 0
				&& compare( resultObject, max ) <= 0;
	}
}
