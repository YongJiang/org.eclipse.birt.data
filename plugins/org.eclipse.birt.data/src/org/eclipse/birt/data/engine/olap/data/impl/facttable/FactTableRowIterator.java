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

package org.eclipse.birt.data.engine.olap.data.impl.facttable;

import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import org.eclipse.birt.data.engine.core.DataException;
import org.eclipse.birt.data.engine.i18n.ResourceConstants;
import org.eclipse.birt.data.engine.impl.StopSign;
import org.eclipse.birt.data.engine.olap.data.api.IComputedMeasureHelper;
import org.eclipse.birt.data.engine.olap.data.api.MeasureInfo;
import org.eclipse.birt.data.engine.olap.data.document.DocumentObjectUtil;
import org.eclipse.birt.data.engine.olap.data.document.IDocumentObject;
import org.eclipse.birt.data.engine.olap.data.impl.NamingUtil;
import org.eclipse.birt.data.engine.olap.data.impl.Traversalor;
import org.eclipse.birt.data.engine.olap.data.util.Bytes;
import org.eclipse.birt.data.engine.olap.data.util.IDiskArray;
import org.eclipse.birt.data.engine.olap.util.filter.IFacttableRow;

/**
 * An iterator on a result set from a executed fact table query.
 */

public class FactTableRowIterator implements IFactTableRowIterator
{
	private FactTable factTable;
	private MeasureInfo[] computedMeasureInfo;
	private MeasureInfo[] allMeasureInfo;	//include measures and computed measures
	
	private IDiskArray[] selectedPos;
	private int[] dimensionIndex;
	private int[] currentSubDim;
	private List[] selectedSubDim = null;

	private IDocumentObject currentSegment;
	private int[] currentPos;
	private Object[] currentMeasureValues;		//current values for measures
	private MeasureMap currentMeasureMap;	//<name, value> map for current measures
	private Object[] currentComputedMeasureValues;	//current values for computed measures

	private Traversalor traversalor;
	private StopSign stopSign;
	
	private int[][] selectedPosOfCurSegment;
	
	private IComputedMeasureHelper computedMeasureHelper;

	private static Logger logger = Logger.getLogger( FactTableRowIterator.class.getName( ) );

	/**
	 * 
	 * @param factTable
	 * @param dimensionName
	 * @param dimensionPos
	 * @param stopSign
	 * @throws IOException
	 */
	public FactTableRowIterator( FactTable factTable, String[] dimensionName,
			IDiskArray[] dimensionPos, StopSign stopSign ) throws IOException
	{
		this( factTable, dimensionName, dimensionPos, null, stopSign );
	}
	/**
	 * 
	 * @param factTable
	 * @param dimensionName
	 * @param dimensionPos
	 * @param stopSign
	 * @throws IOException
	 */
	public FactTableRowIterator( FactTable factTable, String[] dimensionName,
			IDiskArray[] dimensionPos, IComputedMeasureHelper computedMeasureHelper, StopSign stopSign ) throws IOException
	{
		Object[] params = {
				factTable, dimensionName, dimensionPos, stopSign
		};
		logger.entering( FactTableRowIterator.class.getName( ),
				"FactTableRowIterator",
				params );
		this.factTable = factTable;
		this.selectedPos = dimensionPos;
		this.selectedSubDim = new List[factTable.getDimensionInfo( ).length];
		this.selectedPosOfCurSegment = new int[factTable.getDimensionInfo( ).length][];
		this.stopSign = stopSign;
		this.computedMeasureHelper = computedMeasureHelper;
		assert dimensionName.length == dimensionPos.length;
		
		for ( int i = 0; i < selectedSubDim.length; i++ )
		{
			this.selectedSubDim[i] = new ArrayList( );
		}
		dimensionIndex = new int[factTable.getDimensionInfo( ).length];
		for ( int i = 0; i < dimensionIndex.length; i++ )
		{
			dimensionIndex[i] = -1;
		}
		for ( int i = 0; i < dimensionName.length; i++ )
		{
			dimensionIndex[factTable.getDimensionIndex( dimensionName[i] )] = i;

		}
		filterSubDimension( );
		this.currentPos = new int[factTable.getDimensionInfo( ).length];
		this.currentMeasureValues = new Object[factTable.getMeasureInfo( ).length];
		this.currentMeasureMap = new MeasureMap( this.factTable.getMeasureInfo( ) );
		if ( this.computedMeasureHelper != null )
		{
			computedMeasureInfo = this.computedMeasureHelper.getAllComputedMeasureInfos( );
		}
		computeAllMeasureInfo();

		nextSegment( );
		logger.exiting( FactTableRowIterator.class.getName( ),
				"FactTableRowIterator" );
	}

	/**
	 * Filter sub dimensions by dimension position array. The filter result is
	 * saved in the variable selectedSubDim.
	 * 
	 * @throws IOException
	 */
	private void filterSubDimension( ) throws IOException
	{
		DimensionDivision[] dimensionDivisions = factTable.getDimensionDivision( );
		SelectedSubDimension selectedSubDimension = null;
		int[] selectedSubDimensionCount = new int[selectedSubDim.length];

		for ( int i = 0; i < selectedSubDim.length; i++ )
		{
			int pointer = 0;
			for ( int j = 0; j < dimensionDivisions[i].getRanges().length; j++ )
			{
				if ( dimensionIndex[i] > -1 )
				{
					while ( pointer < selectedPos[dimensionIndex[i]].size( )
							&& ( (Integer) selectedPos[dimensionIndex[i]].get( pointer ) ).intValue( ) < dimensionDivisions[i].getRanges()[j].start )
					{
						pointer++;
					}
					if ( pointer >= selectedPos[dimensionIndex[i]].size( ) )
					{
						break;
					}
					if ( ( (Integer) selectedPos[dimensionIndex[i]].get( pointer ) ).intValue( ) > dimensionDivisions[i].getRanges()[j].end )
					{
						continue;
					}
					selectedSubDimension = new SelectedSubDimension( );
					selectedSubDimension.subDimensionIndex = j;
					selectedSubDimension.start = pointer;
					while ( pointer < selectedPos[dimensionIndex[i]].size( )
							&& ( (Integer) selectedPos[dimensionIndex[i]].get( pointer ) ).intValue( ) <= dimensionDivisions[i].getRanges()[j].end )
					{
						pointer++;
					}
					selectedSubDimension.end = pointer - 1;
					selectedSubDim[i].add( selectedSubDimension );
				}
				else
				{
					selectedSubDimension = new SelectedSubDimension( );
					selectedSubDimension.subDimensionIndex = j;
					selectedSubDimension.start = -1;
					selectedSubDimension.end = -1;
					selectedSubDim[i].add( selectedSubDimension );
				}
			}
			selectedSubDimensionCount[i] = selectedSubDim[i].size( );
		}
		traversalor = new Traversalor( selectedSubDimensionCount );
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.birt.data.engine.olap.data.impl.facttable.IFactTableRowIterator#next()
	 */
	public boolean next( ) throws IOException, DataException
	{
		while ( !stopSign.isStopped( ) )
		{
			try
			{
				if( currentSegment == null )
				{
					return false;
				}
				Bytes combinedDimensionPosition = currentSegment.readBytes( );
				
				currentPos = factTable.getCombinedPositionCalculator( )
						.calculateDimensionPosition( getSubDimensionIndex( ),
								combinedDimensionPosition.bytesValue( ) );
				 
				for ( int i = 0; i < this.currentMeasureValues.length; i++ )
				{
					currentMeasureValues[i] = DocumentObjectUtil.readValue( currentSegment,
							factTable.getMeasureInfo()[i].getDataType( ) );
				}
				currentMeasureMap.setMeasureValue( currentMeasureValues );
				if ( computedMeasureHelper != null )
				{
					try
					{
						currentComputedMeasureValues = computedMeasureHelper.computeMeasureValues( currentMeasureMap );
					}
					catch ( DataException e )
					{
						throw new DataException(ResourceConstants.FAIL_COMPUTE_COMPUTED_MEASURE_VALUE, e);
					}
				}
				if ( !isSelectedRow( ) )
				{
					continue;
				}
				else
				{
					return true;
				}
			}
			catch ( EOFException e )
			{
				break;
			}
		}
		if ( stopSign.isStopped( ) || !nextSegment( ) )
		{
			return false;
		}
		return next( );
	}

	/**
	 * 
	 * @throws DataException
	 */
	public void close() throws DataException
	{
		if ( this.computedMeasureHelper!= null )
			this.computedMeasureHelper.cleanUp( );
	}
	
	/**
	 * 
	 * @return
	 */
	private int[] getSubDimensionIndex( )
	{
		int[] result = new int[selectedSubDim.length];
		for ( int i = 0; i < result.length; i++ )
		{
			result[i] = ( (SelectedSubDimension) selectedSubDim[i].get( currentSubDim[i] ) ).subDimensionIndex;
		}
		return result;
	}

	/**
	 * 
	 * @return
	 * @throws IOException
	 */
	private boolean isSelectedRow( ) throws IOException
	{
		for ( int i = 0; i < currentPos.length; i++ )
		{
			if ( dimensionIndex[i] != -1 )
			{
				if( Arrays.binarySearch( selectedPosOfCurSegment[i], currentPos[i] ) < 0 )
					return false;
			}
		}
		return true;
	}

	/**
     * Moves down one segment from its current segment of the iterator.
	 * @return
	 * @throws IOException
	 */
	private boolean nextSegment( ) throws IOException
	{
		if ( stopSign.isStopped( ) )
		{
			return false;
		}
		if ( !traversalor.next( ) )
		{
			return false;
		}
		currentSubDim = traversalor.getIntArray( );
		currentSegment = factTable.getDocumentManager( )
				.openDocumentObject( FTSUDocumentObjectNamingUtil.getDocumentObjectName( 
						NamingUtil.getFactTableName(factTable.getName( )),
						getSubDimensionIndex( ) ) );
		for ( int i = 0; i < dimensionIndex.length; i++ )
		{
			if ( dimensionIndex[i] != -1 )
			{
				SelectedSubDimension selectedSubDimension = ( (SelectedSubDimension) selectedSubDim[i].get( currentSubDim[i] ) );
				selectedPosOfCurSegment[i] = new int[selectedSubDimension.end
						- selectedSubDimension.start + 1];
				for ( int j = 0; j < selectedSubDimension.end
						- selectedSubDimension.start + 1; j++ )
				{
					selectedPosOfCurSegment[i][j] = ( (Integer) selectedPos[dimensionIndex[i]].get( selectedSubDimension.start + j ) ).intValue( );
				}
			}
		}
		return true;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.birt.data.engine.olap.data.impl.facttable.IFactTableRowIterator#getDimensionCount()
	 */
	public int getDimensionCount( )
	{
		return factTable.getDimensionInfo( ).length;
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.birt.data.engine.olap.data.impl.facttable.IFactTableRowIterator#getDimensionIndex(java.lang.String)
	 */
	public int getDimensionIndex( String dimensionName )
	{
		return factTable.getDimensionIndex( dimensionName );
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.birt.data.engine.olap.data.impl.facttable.IFactTableRowIterator#getMeasureIndex(java.lang.String)
	 */
	public int getMeasureIndex( String measureName )
	{
		int reValue = factTable.getMeasureIndex( measureName );
		if( reValue < 0 && computedMeasureInfo != null )
		{
			for ( int i = 0; i < computedMeasureInfo.length; i++ )
			{
				if( measureName.equals( computedMeasureInfo[i].getMeasureName( ) ) )
				{
					reValue = i + factTable.getMeasureInfo( ).length;
					break;
				}
			}
		}
		return reValue;
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.birt.data.engine.olap.data.impl.facttable.IFactTableRowIterator#getDimensionPosition(int)
	 */
	public int getDimensionPosition( int dimensionIndex )
	{
		return currentPos[dimensionIndex];
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.birt.data.engine.olap.data.impl.facttable.IFactTableRowIterator#getMeasureCount()
	 */
	public int getMeasureCount( )
	{
		if( computedMeasureInfo != null )
		{
			return factTable.getMeasureInfo( ).length + computedMeasureInfo.length;
		}
		else
		{
			return factTable.getMeasureInfo( ).length;
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.birt.data.engine.olap.data.impl.facttable.IFactTableRowIterator#getMeasure(int)
	 */
	public Object getMeasure( int measureIndex )
	{
		if ( measureIndex < currentMeasureValues.length )
		{
			return currentMeasureValues[measureIndex];
		}
		else
		{
			if ( currentComputedMeasureValues != null 
					&& ( measureIndex - currentMeasureValues.length ) < currentComputedMeasureValues.length )
			{
				return currentComputedMeasureValues[measureIndex - currentMeasureValues.length];
			}
			else
			{
				return null;
			}
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.birt.data.engine.olap.data.impl.facttable.IFactTableRowIterator#getMeasureInfo()
	 */
	public MeasureInfo[] getMeasureInfo( )
	{
		return allMeasureInfo;
	}
	
	/**
	 * 
	 */
	private void computeAllMeasureInfo() 
	{
		int len = factTable.getMeasureInfo( ).length;
		if (computedMeasureInfo != null)
		{
			len = len + computedMeasureInfo.length;
		}
		allMeasureInfo = new MeasureInfo[len];
		System.arraycopy( factTable.getMeasureInfo( ), 0, allMeasureInfo, 0, factTable.getMeasureInfo( ).length );
		if (computedMeasureInfo != null)
		{
			System.arraycopy( computedMeasureInfo, 0, allMeasureInfo, factTable.getMeasureInfo( ).length, computedMeasureInfo.length );
		}
	}
	
}

class SelectedSubDimension
{
	int subDimensionIndex;
	int start;
	int end;
}

class MeasureMap implements IFacttableRow
{
	private MeasureInfo[] measureInfos = null;
	private Object[] measureValues = null;
	/**
	 * 
	 * @param measureInfo
	 */
	MeasureMap( MeasureInfo[] measureInfo )
	{
		this.measureInfos = measureInfo;
	}
	
	/**
	 * 
	 * @param measureValues
	 */
	void setMeasureValue( Object[] measureValues )
	{
		this.measureValues = measureValues;
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.birt.data.engine.olap.data.api.IMeasureList#getMeasureValue(java.lang.String)
	 */
	public Object getMeasureValue( String measureName )
	{
		for ( int i = 0; i < measureInfos.length; i++ )
		{
			if ( measureInfos[i].getMeasureName().equals( measureName ) )
			{
				return measureValues[i];
			}
		}
		return null;
	}
}