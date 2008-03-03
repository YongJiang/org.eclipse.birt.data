
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
package org.eclipse.birt.data.engine.olap.data.impl.aggregation;

import java.io.IOException;
import java.util.logging.Logger;

import org.eclipse.birt.data.engine.aggregation.AggregationUtil;
import org.eclipse.birt.data.engine.api.aggregation.Accumulator;
import org.eclipse.birt.data.engine.api.aggregation.AggregationManager;
import org.eclipse.birt.data.engine.api.aggregation.IAggrFunction;
import org.eclipse.birt.data.engine.core.DataException;
import org.eclipse.birt.data.engine.i18n.ResourceConstants;
import org.eclipse.birt.data.engine.olap.data.api.IAggregationResultRow;
import org.eclipse.birt.data.engine.olap.data.api.MeasureInfo;
import org.eclipse.birt.data.engine.olap.data.impl.AggregationDefinition;
import org.eclipse.birt.data.engine.olap.data.impl.AggregationFunctionDefinition;
import org.eclipse.birt.data.engine.olap.data.impl.Constants;
import org.eclipse.birt.data.engine.olap.data.impl.DimColumn;
import org.eclipse.birt.data.engine.olap.data.impl.dimension.Member;
import org.eclipse.birt.data.engine.olap.data.impl.facttable.IFactTableRowIterator;
import org.eclipse.birt.data.engine.olap.data.util.BufferedStructureArray;
import org.eclipse.birt.data.engine.olap.data.util.IDiskArray;
import org.eclipse.birt.data.engine.olap.util.filter.IFacttableRow;
import org.eclipse.birt.data.engine.olap.util.filter.IJSMeasureFilterEvalHelper;

/**
 * The AggregationCalculator class calculates values for its associated
 * Aggregation.
 */

public class AggregationCalculator
{
	AggregationDefinition aggregation;
	private Accumulator[] accumulators;
	private int levelCount;
	private int[] measureIndex;
	private MeasureInfo[] measureInfo;
	private IDiskArray result = null;
	private IAggregationResultRow currentResultObj = null;
	private int[] parameterColIndex;
	private FacttableRow facttableRow;
	
	private static Logger logger = Logger.getLogger( AggregationCalculator.class.getName( ) );

	/**
	 * 
	 * @param aggregationDef
	 * @param facttableRowIterator
	 * @throws DataException 
	 */
	AggregationCalculator( AggregationDefinition aggregationDef, DimColumn[] paramterColNames, IFactTableRowIterator facttableRowIterator ) throws DataException
	{
		Object[] params = {
				aggregationDef, facttableRowIterator
		};
		logger.entering( AggregationCalculator.class.getName( ),
				"AggregationCalculator",
				params );
		this.aggregation = aggregationDef;
		AggregationFunctionDefinition[] aggregationFunction = aggregationDef.getAggregationFunctions( );
		if(aggregationDef.getLevels( )==null)
			this.levelCount = 0;
		else
			this.levelCount = aggregationDef.getLevels( ).length;
		if ( aggregationFunction != null )
		{
			this.accumulators = new Accumulator[aggregationFunction.length];
			this.measureIndex = new int[aggregationFunction.length];
			this.parameterColIndex = new int[aggregationFunction.length];
				
			for ( int i = 0; i < aggregationFunction.length; i++ )
			{
				IAggrFunction aggregation = AggregationManager.getInstance( )
						.getAggregation( aggregationFunction[i].getFunctionName( ) );
				if ( AggregationUtil.needDataField( aggregation ) )
				{
					this.parameterColIndex[i] = find( paramterColNames,
							aggregationFunction[i].getParaCol( ) );
				}
				else
				{
					this.parameterColIndex[i] = -1;
				}
				this.accumulators[i] = aggregation.newAccumulator( );
				this.accumulators[i].start( );
				this.measureIndex[i] = facttableRowIterator.getMeasureIndex( aggregationFunction[i].getMeasureName( ) );
				final IAggrFunction aggrFunc = AggregationManager.getInstance( )
						.getAggregation( aggregation.getName( ) );
				if ( aggrFunc == null
						|| ( this.measureIndex[i] == -1 && AggregationUtil.needDataField( aggrFunc ) ) )
				{
					throw new DataException( ResourceConstants.MEASURE_NAME_NOT_FOUND,
							aggregationFunction[i].getMeasureName( ) );
				}
			}
		}
		result = new BufferedStructureArray( AggregationResultRow.getCreator( ), Constants.LIST_BUFFER_SIZE );
		measureInfo = facttableRowIterator.getMeasureInfo( );
		facttableRow = new FacttableRow( measureInfo );
		logger.exiting( AggregationCalculator.class.getName( ),
				"AggregationCalculator" );
	}
	
	/**
	 * 
	 * @param colArray
	 * @param col
	 * @return
	 */
	private static int find( DimColumn[] colArray, DimColumn col )
	{
		if( colArray == null || col == null )
		{
			return -1;
		}
		for ( int i = 0; i < colArray.length; i++ )
		{
			if ( col.equals( colArray[i] ) )
			{
				return i;
			}
		}
		return -1;
	}
	
	/**
	 * 
	 * @param row
	 * @throws IOException
	 * @throws DataException
	 */
	void onRow( Row4Aggregation row ) throws IOException, DataException
	{
		if ( currentResultObj == null )
		{
			newAggregationResultRow( row );
		}
		else
		{
			if ( currentResultObj.getLevelMembers() == null
					|| compare( row.getLevelMembers(), currentResultObj.getLevelMembers() ) == 0 )
			{
				if ( accumulators != null )
				{
					for ( int i = 0; i < accumulators.length; i++ )
					{
						if ( !getFilterResult( row, i ) )
						{
							continue;
						}
						accumulators[i].onRow( getAccumulatorParameter( row, i ) );
					}
				}
			}
			else
			{
				if ( accumulators != null )
				{
					currentResultObj.setAggregationValues( new Object[accumulators.length] );
					for ( int i = 0; i < accumulators.length; i++ )
					{
						accumulators[i].finish( );
						currentResultObj.getAggregationValues()[i] = accumulators[i].getValue( );
						accumulators[i].start( );
					}
				}
				result.add( currentResultObj );
				newAggregationResultRow( row );
			}
		}
	}
	
	/**
	 * 
	 * @param row
	 * @param functionNo
	 * @return
	 * @throws DataException 
	 */
	private boolean getFilterResult( Row4Aggregation row, int functionNo )
			throws DataException
	{
		facttableRow.setMeasure( row.getMeasures( ) );
		IJSMeasureFilterEvalHelper filterEvalHelper = ( aggregation.getAggregationFunctions( )[functionNo] ).getFilterEvalHelper( );
		if ( filterEvalHelper == null )
		{
			return true;
		}
		else
		{
			return filterEvalHelper.evaluateFilter( facttableRow );
		}
	}
	
	/**
	 * 
	 * @return
	 * @throws IOException
	 * @throws DataException
	 */
	IDiskArray getResult( ) throws IOException, DataException
	{
		if ( currentResultObj != null && accumulators != null )
		{
			currentResultObj.setAggregationValues( new Object[accumulators.length] );
			for ( int i = 0; i < accumulators.length; i++ )
			{
				accumulators[i].finish( );
				currentResultObj.getAggregationValues()[i] = accumulators[i].getValue( );
				accumulators[i].start( );
			}
		}
		if ( currentResultObj != null )
			result.add( currentResultObj );
		/*else
			result.add( new AggregationResultRow( ) );*/
		return this.result;
	}
	
	/**
	 * 
	 * @param row
	 * @throws DataException
	 */
	private void newAggregationResultRow( Row4Aggregation row ) throws DataException
	{
		currentResultObj = new AggregationResultRow( );
		if ( levelCount > 0 )
		{
			currentResultObj.setLevelMembers( new Member[levelCount] );
			System.arraycopy( row.getLevelMembers(),
					0,
					currentResultObj.getLevelMembers(),
					0,
					currentResultObj.getLevelMembers().length );
		}
		if ( accumulators != null )
		{
			for ( int i = 0; i < accumulators.length; i++ )
			{
				if ( !getFilterResult( row, i ) )
				{
					continue;
				}
				accumulators[i].onRow( getAccumulatorParameter( row, i ) );
			}
		}
	}
	
	private Object[] getAccumulatorParameter( Row4Aggregation row, int funcIndex )
	{
		Object[] parameters = null;
		if( parameterColIndex[funcIndex] == -1 )
		{
			parameters = new Object[1];
			if( measureIndex[funcIndex] < 0 )
			{
				parameters[0] = null;
			}
			else
			{
				parameters[0] = row.getMeasures()[measureIndex[funcIndex]];
			}
		}
		else
		{
			parameters = new Object[2];
			if( measureIndex[funcIndex] < 0 )
			{
				parameters[0] = null;
			}
			else
			{
				parameters[0] = row.getMeasures()[measureIndex[funcIndex]];
			}
			parameters[1] = row.getParameterValues( )[parameterColIndex[funcIndex]];
		}
		return parameters;
	}
	
	/**
	 * 
	 * @param key1
	 * @param key2
	 * @return
	 */
	private int compare( Object[] key1, Object[] key2 )
	{
		for ( int i = 0; i < aggregation.getLevels( ).length; i++ )
		{
			int result = ( (Comparable) key1[i] ).compareTo( key2[i] );
			if ( result < 0 )
			{
				return result;
			}
			else if ( result > 0 )
			{
				return result;
			}
		}
		return 0;
	}
}

class FacttableRow implements IFacttableRow
{
	private MeasureInfo[] measureInfo;
	private Object[] measureValues;
	
	/**
	 * 
	 * @param measureInfo
	 */
	FacttableRow( MeasureInfo[] measureInfo )
	{
		this.measureInfo = measureInfo;
	}
	
	/**
	 * 
	 * @param measureValues
	 */
	void setMeasure( Object[] measureValues )
	{
		this.measureValues = measureValues;
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.birt.data.engine.olap.util.filter.IFacttableRow#getMeasureValue(java.lang.String)
	 */
	public Object getMeasureValue( String measureName ) throws DataException
	{
		for ( int i = 0; i < measureInfo.length; i++ )
		{
			if(measureInfo[i].getMeasureName().equals( measureName ))
			{
				return measureValues[i];
			}
		}
		return null;
	}
}