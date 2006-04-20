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

package org.eclipse.birt.data.engine.expression;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.birt.data.engine.aggregation.AggregationFactory;
import org.eclipse.birt.data.engine.api.aggregation.IAggregation;
import org.eclipse.birt.data.engine.core.DataException;
import org.eclipse.birt.data.engine.i18n.ResourceConstants;
import org.eclipse.birt.data.engine.impl.aggregation.AggregateRegistry;
import org.mozilla.javascript.CompilerEnvirons;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Interpreter;
import org.mozilla.javascript.Node;
import org.mozilla.javascript.Parser;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.ScriptOrFnNode;
import org.mozilla.javascript.Token;

/**
 * 
 * This class handles the compilation of ROM JavaScript expressions to be
 * evaluated by the DtE at report query preparation time. Each expression is
 * compiled to generate a handle, which is an instance of CompiledExpression or
 * its derived class. The expression handle is used by the factory to evaluate
 * the expression after the report query is executed. <br>
 * ExpressionCompiler compiles the expression into Rhino byte code for faster
 * evaluation at runtime.
 */
public final class ExpressionCompiler
{
    private CompilerEnvirons m_compilerEnv;
    
	private final static String STRING_ROW = "row";
	private final static String STRING_DATASETROW = "dataSetRow";
	private final static String TOTAL = "Total";
	public final static String AGGR_VALUE = "_aggr_value";

	protected static Logger logger = Logger.getLogger( ExpressionCompiler.class.getName() );
	
	private String rowIndicator = STRING_ROW;

	private boolean isDataSetMode = false;
	/**
	 * Compiles a Javascript expression to produce a subclass of CompileExpression, which
	 * contains the compiled form of the JS expression, after suitable replacement
	 * of aggregate functin occurrences in the expression. Aggregate function calls
	 * that appear in the expression are registered with the provided AggregateRegistry
	 * instance. <p>
	 * Aggregate registry can be null, in which case the provided expression is expected
	 * NOT to contain aggregates. If it does, a DteException is thrown.
	 * @param expression Text of expression to compile
	 * @param registry Registry for aggregate expressions. Can be null if expression is not expected to contain aggregates
	 * @param context Rhino context associated with current thread
	 * @return
	 * @throws DataException
	 */	
	public CompiledExpression compile( String expression, 
								AggregateRegistry registry, 
								Context context ) 
	{
		logger.entering( ExpressionCompiler.class.getName( ),
				"compile",
				expression );
		try
		{
			if(expression == null || expression.trim().length() == 0)
			{
				throw new DataException( ResourceConstants.EXPRESSION_CANNOT_BE_NULL_OR_BLANK);
			}
			
			logger.logp( Level.FINER,
					ExpressionCompiler.class.getName( ),
					"compile",
					"Start to compile expression to produce a CompileExpression",
					expression );
			ScriptOrFnNode tree = parse( expression, context );
			return getCompiledExprFromTree( expression, context, registry, tree );
		}
		catch (Exception e)
		{
			// Here exception will not be thrown, since invalid
			// expression is not such a fatal error that requires
			// stop generating report.
			DataException dataException = new DataException( ResourceConstants.INVALID_JS_EXPR,
					 e, expression );
			logger.logp( Level.FINE,
					ExpressionCompiler.class.getName( ),
					"compile",
					"Expression is not valid.",
					e );
			return new InvalidExpression( dataException );
		}
		finally
		{
			logger.logp( Level.FINER,
					ExpressionCompiler.class.getName( ),
					"compile",
					"Finish compiling an expression" );
			logger.exiting( ExpressionCompiler.class.getName( ), "compile" ); 			
		}
	}
	
	private ScriptOrFnNode parse( String expression, Context cx )
	{
		logger.entering( ExpressionCompiler.class.getName( ),
				"parse",
				expression );
		CompilerEnvirons compilerEnv = getCompilerEnv( cx ); 
		Parser p = new Parser(compilerEnv, cx.getErrorReporter());
		return p.parse( expression, null, 0 );
	}
	
	// gets a CompiledExpression instance based on the parse tree that we got back 
	// from the Rhino parser
	private CompiledExpression getCompiledExprFromTree( 
			String expression, 
			Context context, 
			AggregateRegistry registry, 
			ScriptOrFnNode tree )
		throws DataException
	{
		logger.entering( ExpressionCompiler.class.getName( ),
				"getCompiledExprFromTree" );
		CompiledExpression expr;
		if ( tree.getFirstChild() == tree.getLastChild() )
		{
			// A single expression
			if ( tree.getFirstChild().getType() != Token.EXPR_RESULT &&
				 tree.getFirstChild().getType() != Token.EXPR_VOID &&
				 tree.getFirstChild().getType() != Token.BLOCK
				 )
			{
				// This should never happen?
				throw new DataException( ResourceConstants.INVALID_JS_EXPR, expression ); 
			}
			Node exprNode = tree.getFirstChild();
			Node child = exprNode.getFirstChild();
			assert( child != null );
			expr = processChild( context, registry, exprNode, child );
		}
		else
		{
			// Multiple expressions exist; we should produce a ComplexExpression 
			// However, individual subexpressions still needs to be processed to identify 
			// the interesting subexpressions
			expr = getComplexExpr( context, registry, tree );
		}
		
		if( expr instanceof BytecodeExpression )
			compileForBytecodeExpr( context, tree, expr );
		
		logger.exiting( ExpressionCompiler.class.getName( ),
				"getCompiledExprFromTree",
				expr );
		return expr;
	}

	private void compileForBytecodeExpr( Context context, ScriptOrFnNode tree, 
										 CompiledExpression expr )
	{
		logger.entering( ExpressionCompiler.class.getName( ),
				"compileForBytecodeExpr",
				new Object[]{
						context, tree, expr
				} );
		assert ( expr instanceof BytecodeExpression );
		CompilerEnvirons compilerEnv = getCompilerEnv( context );
		Interpreter compiler = new Interpreter( );
		Object compiledOb = compiler.compile( compilerEnv, tree, null, false );
		Script script = (Script) compiler.createScriptObject( compiledOb, null );
		( (BytecodeExpression) expr ).setScript( script );
		logger.exiting( ExpressionCompiler.class.getName( ),
				"compileForBytecodeExpr" );
	}

	// returns the compiled expression from processing a child node
	private CompiledExpression processChild( Context context, 
											 AggregateRegistry registry, 
											 Node parent, 
											 Node child )
		throws DataException
	{
		CompiledExpression compiledExpr = null;
		switch( child.getType() )
		{
			case Token.NUMBER:
				compiledExpr = new ConstantExpression( child.getDouble() );
				break;
				
			case Token.STRING:
				compiledExpr = new ConstantExpression( child.getString() );
				break;
				
			case Token.NULL:
				compiledExpr = new ConstantExpression();
				break;
				
			case Token.TRUE:
				compiledExpr = new ConstantExpression( true );
				break;
				
			case Token.FALSE:
				compiledExpr = new ConstantExpression( false );
				break;
			
			case Token.GETPROP:
				ConstantExpression ce = AggregationConstantsUtil.getConstantExpression( child );
				if ( ce != null )
				{
					compiledExpr = ce;
					break;
				}
            case Token.GETELEM:
                 compiledExpr = getDirectColRefExpr( child );
                 break;
				
			case Token.CALL:
				compiledExpr = getAggregateExpr( context, registry, parent, child );
				break;
		}
		
		if( compiledExpr == null )
			compiledExpr = getComplexExpr( context, registry, child );
		return compiledExpr;
	}
	
	// Check if the expression is a direct column reference type. If so, returns
	// an instance of DirectColRefExpr that represents it; otherwise returns null.
	private ColumnReferenceExpression getDirectColRefExpr( Node refNode )
	{
		// if it's a GETPROP or GETELEM with row on the left side, 
		// and either a STRING or NUMBER on the right side, then it's 
		// a direct column reference
		assert( refNode.getType() == Token.GETPROP || 
				refNode.getType() == Token.GETELEM );
		
		Node rowName = refNode.getFirstChild( );
		assert( rowName != null );
		if ( rowName.getType( ) != Token.NAME )
			return null;

		String str = rowName.getString( );
		assert( str != null );
		if (!str.equals(rowIndicator))
			return null;

		Node rowColumn = rowName.getNext( );
		assert( rowColumn != null );
		
		if ( refNode.getType( ) == Token.GETPROP
				&& rowColumn.getType( ) == Token.STRING )
		{
			if ( "_outer".equals( rowColumn.getString( ) ) )
				return null;

			return new ColumnReferenceExpression( this.isDataSetMode
					? STRING_ROW : STRING_DATASETROW, rowColumn.getString( ) );
		}
		if( refNode.getType() == Token.GETELEM )
		{
			if( rowColumn.getType() == Token.NUMBER )
				return new ColumnReferenceExpression(
						this.isDataSetMode ? STRING_ROW : STRING_DATASETROW,
						(int) rowColumn.getDouble());
			else if( rowColumn.getType() == Token.STRING )
				return new ColumnReferenceExpression(
						this.isDataSetMode ? STRING_ROW : STRING_DATASETROW,
						rowColumn.getString());
		}		
		
		// right side is not a STRING or a NUMBER, which is what is needed for 
		// a direct column reference. so it could be something 
		// like row[getColumnIndex()] and that would be a complex expression
		return null;
	}
	
	private AggregateExpression getAggregateExpr( Context context, 
												  AggregateRegistry registry,
												  Node parent, 
												  Node callNode )
		throws DataException
	{
		logger.entering( ExpressionCompiler.class.getName( ),
				"getAggregateExpr" );
		
		assert( callNode.getType() == Token.CALL );
		
		IAggregation aggregation = getAggregationFunction( callNode );
		// not an aggregation function being called, then it's considered 
		// a complex expression
		if( aggregation == null )
		{
			logger.exiting( ExpressionCompiler.class.getName( ), "getAggregateExpr" );
			return null;
		}
		
		AggregateExpression aggregateExpression = 
			new AggregateExpression( aggregation );
		
		extractArguments( context, registry, aggregateExpression, callNode );
		replaceAggregateNode( registry, aggregateExpression, parent, callNode );
		
		logger.exiting( ExpressionCompiler.class.getName( ),
				"getAggregateExpr",
				aggregateExpression );
		return aggregateExpression;
	}
	
	private IAggregation getAggregationFunction( Node callNode ) throws DataException
	{
		// if this is an aggregation expression, then it'll be in the form of 
		// Total.sum( row.x )
		// This means the first child is a GETPROP node, and its left child is 
		// "Total" and its right child is "sum"
		Node firstChild = callNode.getFirstChild();
		if( firstChild.getType() != Token.GETPROP )
			return null;
		
		Node getPropLeftChild = firstChild.getFirstChild();
		if( getPropLeftChild.getType() != Token.NAME || 
			! getPropLeftChild.getString().equals( TOTAL ) )
			return null;
		
		Node getPropRightChild = firstChild.getLastChild();
		if( getPropRightChild.getType() != Token.STRING )
			return null;
		
		String aggrFuncName = getPropRightChild.getString();
		IAggregation agg = AggregationFactory.getInstance().getAggregation( aggrFuncName );
		if ( agg == null )
		{
			// Aggr function name after Total is invalid; this will eventuall cause
			// an error. Report error now
			throw new DataException( ResourceConstants.INVALID_TOTAL_NAME,
					aggrFuncName );
		}
		return agg;
	}
	
	private void extractArguments( Context context, 
								   AggregateRegistry registry,
								   AggregateExpression aggregateExpression, 
								   Node callNode )
		throws DataException
	{
		Node arg = callNode.getFirstChild().getNext();
		
		while( arg != null )
		{
			// need to hold on to the next argument because the tree extraction 
			// will cause us to lose the reference otherwise
			Node nextArg = arg.getNext();
			
			CompiledExpression expr = processChild( context, registry, 
													callNode, arg );
			if( ! ( expr instanceof BytecodeExpression ) )
			{
				aggregateExpression.addArgument( expr );
				arg = nextArg;
				continue;
			}
			
			ScriptOrFnNode tree = new ScriptOrFnNode( Token.SCRIPT );
			Node exprNode = new Node( Token.EXPR_RESULT);
			exprNode.addChildToFront( arg );
			tree.addChildrenToFront( exprNode );
			compileForBytecodeExpr( context, tree, expr );
			aggregateExpression.addArgument( expr );
			
			arg = nextArg;
		}
	}
	
	private void replaceAggregateNode( AggregateRegistry registry,
									   AggregateExpression aggregateExpression, 
									   Node parent, Node aggregateCallNode )
		throws DataException
	{
		// TODO externalize for i18n
		if( registry == null )
			throw new DataException( ResourceConstants.INVALID_CALL_AGGR );
		
		// replace the aggregate CALL node with _aggr_value[<aggregateId>]
		int aggregateId = registry.register( aggregateExpression );
		Node newFirstChild = Node.newString( Token.NAME, AGGR_VALUE );
		Node newSecondChild = Node.newNumber( aggregateId );
		Node aggregateNode = new Node( Token.GETELEM, newFirstChild, newSecondChild );
		parent.replaceChild( aggregateCallNode, aggregateNode );
	}
	
	private ComplexExpression getComplexExpr( Context context, 
											  AggregateRegistry registry, 
											  Node complexNode )
		throws DataException
	{
		logger.entering( ExpressionCompiler.class.getName( ), "getComplexExpr" );
		ComplexExpression complexExpr = new ComplexExpression( );
		Node child = complexNode.getFirstChild( );
		complexExpr.addTokenList( new Integer( complexNode.getType( ) ) );
		while ( child != null )
		{
			// keep reference to next child, since subsequent steps could lose 
			// the reference to it
			Node nextChild = child.getNext();
			
			// do not include constants into the sub-expression list
			if( child.getType() == Token.NUMBER || 
				child.getType() == Token.STRING ||
				child.getType() == Token.TRUE ||
				child.getType() == Token.FALSE ||
				child.getType() == Token.NULL )
			{
				CompiledExpression subExpr = processChild( context,
						registry,
						complexNode,
						child );
				if ( subExpr instanceof ConstantExpression )
					complexExpr.addContantsExpressions( subExpr );
				child = nextChild;
				continue;
			}
			
			CompiledExpression subExpr = 
				processChild( context, registry, complexNode, child );
			complexExpr.addSubExpression( subExpr );
			complexExpr.addTokenList( new Integer( child.getType( ) ) );
			child = nextChild;
		}
		
		flattenNestedComplexExprs( complexExpr );
		
		logger.exiting( ExpressionCompiler.class.getName( ),
				"getComplexExpr",
				complexExpr );
		return complexExpr;
	}
	
	private void flattenNestedComplexExprs( ComplexExpression complexExpr )
	{
		// need to flatten out the tree when there are nested ComplexExpressions 
		// ( i.e. ComplexExpression with ComplexExpressions as sub expressions )
		// the nesting provides no additional information for the calculation to use.
		// this nesting could occur depending on the Rhino parse tree that's returned 
		// by Rhino, and the limited way we can traverse down the parse tree.
		ArrayList interestingSubExpr = new ArrayList();
		ArrayList interestingConstantExpr = new ArrayList();
		Collection subExprs = complexExpr.getSubExpressions();
		Iterator iter = subExprs.iterator();
		while( iter.hasNext() )
		{
			CompiledExpression childExpr = (CompiledExpression) iter.next();
			if( childExpr instanceof ColumnReferenceExpression ||
				childExpr instanceof AggregateExpression )
				interestingSubExpr.add( childExpr );
			else if( childExpr instanceof ComplexExpression )
			{
				Collection childSubExprs = 
					( (ComplexExpression) childExpr ).getSubExpressions();
				
				childSubExprs.addAll( ( (ComplexExpression) childExpr ).getConstantExpressions( ) );
				
				Iterator childIter = childSubExprs.iterator();
				while( childIter.hasNext() )
				{
					CompiledExpression childChildExpr = 
						(CompiledExpression) childIter.next();
					if( childChildExpr instanceof ColumnReferenceExpression ||
						childChildExpr instanceof AggregateExpression )
						interestingSubExpr.add( childChildExpr );
					else if ( childChildExpr instanceof ConstantExpression )
						interestingConstantExpr.add( childChildExpr );
					else
						assert false;
				}
				
				
			}
			else
				assert false;
			
			iter.remove();
		}
		
		complexExpr.addSubExpressions( interestingSubExpr );
		complexExpr.addContantsExpressions( interestingConstantExpr );
	}

	private CompilerEnvirons getCompilerEnv( Context context )
	{
		if( m_compilerEnv == null )
		{
			m_compilerEnv = new CompilerEnvirons();
			m_compilerEnv.initFromContext( context );
		}
		return m_compilerEnv;
	}
	
	public void setDataSetMode( boolean isDataSetMode )
	{
		this.isDataSetMode  = isDataSetMode;
		if( isDataSetMode )
		{
			this.rowIndicator = STRING_ROW;
		}else
		{
			this.rowIndicator = STRING_DATASETROW;
		}
	}
}