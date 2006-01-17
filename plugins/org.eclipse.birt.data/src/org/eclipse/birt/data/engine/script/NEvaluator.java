package org.eclipse.birt.data.engine.script;

import org.eclipse.birt.core.data.DataTypeUtil;
import org.eclipse.birt.core.exception.BirtException;
import org.eclipse.birt.data.engine.api.IConditionalExpression;
import org.eclipse.birt.data.engine.api.IScriptExpression;
import org.eclipse.birt.data.engine.core.DataException;
import org.eclipse.birt.data.engine.i18n.ResourceConstants;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

/**
 * The implementation of this class is used to evaluate TopN/BottomN expressions 
 * @author lzhu
 *
 */
public abstract class NEvaluator
{
	private Object[] valueList; 
	private int[] rowIdList;
	private int firstPassRowNumberCounter = 0;
	private int secondPassRowNumberCounter = 0;
	private int qualifiedRowCounter = 0;
	
	//The "N" of topN/bottomN.
	private int N = -1;
	
	// whether we are doing N percent
	private boolean n_percent = false;
	
	// expression for operand (to be compared)
	private IScriptExpression op_expr;
	// expression for N 
	private IScriptExpression n_expr;
	
	private FilterPassController filterPassController;

	/**
	 * Create a new instance to evaluate the top/bottom expression
	 * @param operator 
	 * @param op_expr operand expression
	 * @param n_expr expression to yield N 
	 * @return
	 */
	public static NEvaluator newInstance( int operator, IScriptExpression op_expr, 
			IScriptExpression n_expr, FilterPassController filterPassController  )
	 	throws DataException
	{
		NEvaluator instance = null;
		switch ( operator )
		{
			case IConditionalExpression.OP_TOP_N :
				instance = new TopNEvaluator();
				instance.n_percent = false;
				break;
			case IConditionalExpression.OP_TOP_PERCENT:
				instance = new TopNEvaluator( );
				instance.n_percent = true;
				break;
			case IConditionalExpression.OP_BOTTOM_N:
				instance = new BottomNEvaluator( );
				instance.n_percent = false;
				break;
			case IConditionalExpression.OP_BOTTOM_PERCENT:
				instance = new BottomNEvaluator( );
				instance.n_percent = true;
				break;
			default:
				assert false;		// shouldn't get here
				return null;
		}
		
		instance.op_expr = op_expr;
		instance.n_expr = n_expr;
		instance.filterPassController = filterPassController;
		return instance;
	}
	
	
	/**
	 * Evaluate the given value
	 * @param value
	 * @param n
	 * @return
	 * @throws DataException
	 */
	public boolean evaluate( Context cx, Scriptable scope ) throws DataException
	{
		if( filterPassController.getForceReset() )
		{
			doReset();
			filterPassController.setForceReset( false );
		}
		
		if ( N == -1 )
		{
			// Create a new evaluator
			// Evaluate N (which is operand1) at this time
			Object n_object = ScriptEvalUtil.evalExpr( n_expr, cx, scope, "Filter", 0 );
			double n_value = -1;
			try
			{
				n_value = DataTypeUtil.toDouble( n_object ).doubleValue();
			}
			catch ( BirtException e )
			{
				// conversion error
				throw new DataException(ResourceConstants.INVALID_TOP_BOTTOM_PERCENT_ARGUMENT, e);
			}
			
			// First time; calculate N based on updated row count
			if( n_percent )
			{
				if( n_value < 0 || n_value > 100)
					throw new DataException(ResourceConstants.INVALID_TOP_BOTTOM_PERCENT_ARGUMENT);
				N = (int)Math.round( n_value / 100 * filterPassController.getRowCount() );
			}
			else
			{
				if( n_value < 0 )
					throw new DataException(ResourceConstants.INVALID_TOP_BOTTOM_PERCENT_ARGUMENT);
				N = (int)n_value;
			}
		}
		
		// Evaluate operand expression
		Object value = ScriptEvalUtil.evalExpr( op_expr, cx, scope, "Filter", 0 );
		
		if ( filterPassController.getPassLevel( ) == FilterPassController.FIRST_PASS )
		{
			return doFirstPass( value );
		}
		else if ( filterPassController.getPassLevel( ) == FilterPassController.SECOND_PASS )
		{
			return doSecondPass( );
		}
		return false;
	}

	/**
	 * Do the first pass. In the first pass we maintain a value list and a row id list that will
	 * host all top/bottom N values/rowIds so that in pass 2 we can use them to filter rows out.
	 * @param value
	 * @return
	 * @throws DataException
	 */
	private boolean doFirstPass( Object value ) throws DataException
	{
		firstPassRowNumberCounter++;
		if ( valueList == null )
		{
			valueList = new Object[N];
			rowIdList = new int[N];
		}
		populateValueListAndRowIdList( value, N );
		return false;
	}

	/**
	 * @param value
	 * @param N
	 * @throws DataException
	 */
	private void populateValueListAndRowIdList( Object value, int N ) throws DataException
	{
		assert N>=0;
		for( int i = 0; i < N; i++ )
		{
			if( value == null )
				value = "{NULL_VALUE_!@#$%^&}";
			if( valueList[i] == null )
			{
				valueList[i] = value;
				rowIdList[i] = firstPassRowNumberCounter;
				break;
			}else
			{
				Object result = this.doCompare( value, valueList[i] );
				try
				{
					// filter in
					if ( DataTypeUtil.toBoolean( result ).booleanValue( ) == true )
					{
						for( int j = N - 1; j > i; j--)
						{
							valueList[j] = valueList[j-1];
							rowIdList[j] = rowIdList[j-1];
						}
						valueList[i] = value;
						rowIdList[i] = firstPassRowNumberCounter;
						break;
					}
				}
				catch ( BirtException e )
				{
					throw DataException.wrap(e);
				}
			}
		}
	}
	
	/**
	 * Do the second pass
	 * @param N
	 * @return
	 */
	private boolean doSecondPass( )
	{
		secondPassRowNumberCounter++;
		if ( qualifiedRowCounter < N )
		{
			for ( int i = 0; i < N; i++ )
			{
				if ( rowIdList[i] == secondPassRowNumberCounter )
				{
					qualifiedRowCounter++;
					reset( );
					return true;

				}
			}
			return false;
		}
		else
		{
			reset( );
			return false;
		}
	}

	/**
	 * Reset all the member data to their default value.
	 */
	private void reset( )
	{
		if ( firstPassRowNumberCounter  == secondPassRowNumberCounter )
		{
			doReset();
		}
	}

	/**
	 * 
	 *
	 */
	private void doReset()
	{
		firstPassRowNumberCounter = 0;
		secondPassRowNumberCounter = 0;
		qualifiedRowCounter = 0;
		rowIdList = null;
		valueList = null;
		N = -1;
	}
	protected abstract Object doCompare( Object value1, Object value2 ) throws DataException;
}

/**
 * The class that provides "Top N" calculation service
 *
 */
class TopNEvaluator extends NEvaluator
{
	protected Object doCompare( Object value1, Object value2 ) throws DataException
	{
		return ScriptEvalUtil.evalConditionalExpr2( value1, IConditionalExpression.OP_GT, value2, null);
	}
}

/**
 * The class that provides "Bottom N" calculation service
 *
 */
class BottomNEvaluator extends NEvaluator
{
	protected Object doCompare( Object value1, Object value2 ) throws DataException
	{
		return ScriptEvalUtil.evalConditionalExpr2( value1, IConditionalExpression.OP_LT, value2, null);
	}
}
