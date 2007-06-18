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

import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Logger;

import org.eclipse.birt.core.exception.BirtException;
import org.eclipse.birt.data.engine.core.DataException;
import org.eclipse.birt.data.engine.i18n.ResourceConstants;
import org.eclipse.birt.data.engine.olap.data.api.ILevel;
import org.eclipse.birt.data.engine.olap.data.api.cube.IDatasetIterator;
import org.eclipse.birt.data.engine.olap.data.api.cube.StopSign;
import org.eclipse.birt.data.engine.olap.data.document.DocumentObjectCache;
import org.eclipse.birt.data.engine.olap.data.document.DocumentObjectUtil;
import org.eclipse.birt.data.engine.olap.data.document.IDocumentManager;
import org.eclipse.birt.data.engine.olap.data.document.IDocumentObject;
import org.eclipse.birt.data.engine.olap.data.impl.Constants;
import org.eclipse.birt.data.engine.olap.data.impl.NamingUtil;
import org.eclipse.birt.data.engine.olap.data.impl.dimension.Dimension;
import org.eclipse.birt.data.engine.olap.data.impl.dimension.DimensionKey;
import org.eclipse.birt.data.engine.olap.data.impl.dimension.DimensionRow;
import org.eclipse.birt.data.engine.olap.data.util.BufferedStructureArray;
import org.eclipse.birt.data.engine.olap.data.util.Bytes;
import org.eclipse.birt.data.engine.olap.data.util.DiskSortedStack;
import org.eclipse.birt.data.engine.olap.data.util.IDiskArray;

/**
 * This a accessor class for fact table which can be used to save or load a FactTable.
 */

public class FactTableAccessor
{
	private IDocumentManager documentManager =null;
	private static Logger logger = Logger.getLogger( FactTableAccessor.class.getName( ) );
	
	public FactTableAccessor( IDocumentManager documentManager )
	{
		logger.entering( FactTableAccessor.class.getName( ),
				"FactTableAccessor",
				documentManager );
		this.documentManager = documentManager;
		logger.exiting( FactTableAccessor.class.getName( ), "FactTableAccessor" );
	}
	
	/**
	 * 
	 * @param factTableName
	 * @param iterator
	 * @param dimensions
	 * @param measureColumnName
	 * @param stopSign
	 * @return
	 * @throws BirtException
	 * @throws IOException
	 */
	public FactTable saveFactTable( String factTableName,
			String[][] factTableJointColumnNames, String[][] DimJointColumnNames,
			IDatasetIterator iterator, Dimension[] dimensions,
			String[] measureColumnName, StopSign stopSign )
			throws BirtException, IOException
	{
		DiskSortedStack sortedFactTableRows = getSortedFactTableRows( iterator,
				factTableJointColumnNames,
				measureColumnName,
				stopSign );

		int segmentCount = getSegmentCount( sortedFactTableRows.size( ) );

		DimensionInfo[] dimensionInfo = getDimensionInfo( dimensions );
		MeasureInfo[] measureInfo = getMeasureInfo( iterator, measureColumnName );
		
		saveFactTableMetadata( factTableName,
				dimensionInfo,
				measureInfo,
				segmentCount );

		DimensionDivision[] subDimensions = calculateDimensionDivision( getDimensionMemberCount( dimensions ),
				segmentCount );
		
		int[][][] columnIndex = getColumnIndex( DimJointColumnNames, dimensions );
		DimensionPositionSeeker[] dimensionSeekers = new DimensionPositionSeeker[dimensions.length];
		for ( int i = 0; i < dimensionSeekers.length; i++ )
		{
			dimensionSeekers[i] = new DimensionPositionSeeker( getDimCombinatedKey( columnIndex[i],
					dimensions[i].getAllRows( ) ) );
		}

		FactTableRow currentRow = null;
		FactTableRow lastRow = null;
		int[] dimensionPosition = new int[dimensions.length];
		DocumentObjectCache documentObjectManager = new DocumentObjectCache( documentManager );
		CombinedPositionContructor combinedPositionCalculator = new CombinedPositionContructor( subDimensions );
		
		FTSUNameSaveHelper helper = new FTSUNameSaveHelper( documentManager, factTableName );
		Object popObject = sortedFactTableRows.pop( );
		while ( popObject != null && !stopSign.isStopped( ) )
		{
			currentRow = (FactTableRow) popObject;
			if ( lastRow != null && currentRow.equals( lastRow ) )
			{
				throw new DataException( ResourceConstants.FACTTABLE_ROW_NOT_DISTINCT,
						currentRow.toString( ) );
			}
			for ( int i = 0; i < dimensionPosition.length; i++ )
			{
				dimensionPosition[i] = dimensionSeekers[i].find( currentRow.getDimensionKeys()[i] );
				if ( dimensionPosition[i] < 0 )
				{
					String[] args = new String[2];
					args[0] = currentRow.toString( );
					args[1] = dimensions[i].getName( );
					throw new DataException( ResourceConstants.INVALID_DIMENSIONPOSITION_OF_FACTTABLEROW,
							args );
				}
			}
			int[] subDimensionIndex = getSubDimensionIndex( dimensionPosition,
					subDimensions );
			String FTSUDocName = FTSUDocumentObjectNamingUtil.getDocumentObjectName( 
					NamingUtil.getFactTableName( factTableName ),
					subDimensionIndex );
			helper.add( FTSUDocName );
			
			IDocumentObject documentObject = documentObjectManager.getIDocumentObject( FTSUDocName );
			documentObject.writeBytes( new Bytes( combinedPositionCalculator.
					calculateCombinedPosition( subDimensionIndex, dimensionPosition ).toByteArray( ) ) );
			for( int i=0;i<measureInfo.length;i++)
			{
				DocumentObjectUtil.writeValue( documentObject,
						measureInfo[i].dataType,
						currentRow.getMeasures()[i] );
			}
			popObject = sortedFactTableRows.pop( );
			lastRow = currentRow;
		}
		helper.save( );
		documentObjectManager.closeAll( );
		documentManager.flush( );
		return new FactTable( factTableName,
				documentManager,
				dimensionInfo,
				measureInfo,
				segmentCount,
				subDimensions);
		
	}

	private int[][][] getColumnIndex( String[][] keyColumnNames,
			Dimension[] dimensions ) throws DataException
	{
		int[][][] columnIndex = new int[keyColumnNames.length][][];
		for ( int i = 0; i < keyColumnNames.length; i++ )
		{
			columnIndex[i] = new int[keyColumnNames[i].length][];
			ILevel[] levels = dimensions[i].getHierarchy( ).getLevels( );
			for ( int j = 0; j < keyColumnNames[i].length; j++ )
			{
				columnIndex[i][j] = new int[3];
				columnIndex[i][j][0] = -1;
				for( int k = 0; k < levels.length; k++ )
				{
					String[] columns = levels[k].getKeyNames( );
					int index = find( columns, keyColumnNames[i][j] );
					if( index >= 0 )
					{
						//is key column
						columnIndex[i][j][0] = 0;
						columnIndex[i][j][1] = k;
						columnIndex[i][j][2] = index;
						break;
					}
					columns = levels[k].getAttributeNames( );
					index = find( columns, keyColumnNames[i][j] );
					if( index >= 0 )
					{
						//is key column
						columnIndex[i][j][0] = 1;
						columnIndex[i][j][1] = k;
						columnIndex[i][j][2] = index;
						break;
					}
				}
				if ( columnIndex[i][j][0] == -1 )
				{
					throw new DataException( ResourceConstants.FACTTABLE_JOINT_COL_NOT_EXIST,
							keyColumnNames[i][j] );
				}
			}
		}
		return columnIndex;
	}
	
	/**
	 * 
	 * @param strArray
	 * @param str
	 * @return
	 */
	private int find( String[] strArray, String str )
	{
		if( strArray == null )
		{
			return -1;
		}
		for( int i = 0; i < strArray.length; i++ )
		{
			if( strArray[i].equals( str ) )
			{
				return i;
			}
		}
		return -1;
	}
	
	/**
	 * 
	 * @param dimRowArray
	 * @return
	 * @throws IOException
	 */
	private static IDiskArray getDimCombinatedKey( int[][] columnIndex , IDiskArray dimRowArray ) throws IOException
	{
		BufferedStructureArray resultArray = new BufferedStructureArray( DimensionKey.getCreator( ),
				Constants.LIST_BUFFER_SIZE );
		for ( int i = 0; i < dimRowArray.size( ); i++ )
		{
			DimensionRow dimRow = (DimensionRow)dimRowArray.get( i );
			DimensionKey key = new DimensionKey( columnIndex.length );
			Object[] values = new Object[columnIndex.length];
			for( int j = 0; j < columnIndex.length; j++)
			{
				if( columnIndex[j][0] == 0 )
				{
					// this is a key column
					values[j] = dimRow.getMembers()[columnIndex[j][1]].getKeyValues( )[columnIndex[j][2]];
				}
				else
				{
					values[j] = dimRow.getMembers()[columnIndex[j][1]].getAttributes( )[columnIndex[j][2]];
				}
			}
			key.setKeyValues( values );
			key.setDimensionPos( i );
			resultArray.add( key );
		}
		return resultArray;
	}
	
	/**
	 * 
	 * @param factTableRowCount
	 * @return
	 */
	private static int getSegmentCount( int factTableRowCount )
	{
		int segmentCount = factTableRowCount / Constants.FACT_TABLE_BLOCK_SIZE;
		if ( segmentCount * Constants.FACT_TABLE_BLOCK_SIZE < factTableRowCount )
		{
			segmentCount++;
		}
		return segmentCount;
	}
	
	/**
	 * 
	 * @param dimension
	 * @return
	 */
	private static DimensionInfo[] getDimensionInfo( Dimension[] dimension )
	{
		DimensionInfo[] dimensionInfo = new DimensionInfo[dimension.length];
		for ( int i = 0; i < dimension.length; i++ )
		{
			dimensionInfo[i] = new DimensionInfo( );
			dimensionInfo[i].dimensionName = dimension[i].getName( );
			dimensionInfo[i].dimensionLength = dimension[i].length( );
		}
		return dimensionInfo;
	}

	/**
	 * 
	 * @param iterator
	 * @param measureColumnName
	 * @return
	 * @throws BirtException
	 */
	private static MeasureInfo[] getMeasureInfo( IDatasetIterator iterator,
			String[] measureColumnName ) throws BirtException
	{
		MeasureInfo[] measureInfo = new MeasureInfo[measureColumnName.length];
		for ( int i = 0; i < measureColumnName.length; i++ )
		{
			measureInfo[i] = new MeasureInfo( );
			measureInfo[i].measureName = measureColumnName[i];
			measureInfo[i].dataType = iterator.getFieldType( measureColumnName[i] );
		}
		return measureInfo;
	}
	
	/**
	 * 
	 * @param factTableName
	 * @param dimensionInfo
	 * @param measureInfo
	 * @param segmentNumber
	 * @throws IOException
	 * @throws BirtException
	 */
	private void saveFactTableMetadata( String factTableName,
			DimensionInfo[] dimensionInfo, MeasureInfo[] measureInfo,
			int segmentNumber ) throws IOException, BirtException
	{
		IDocumentObject documentObject = 
			documentManager.createDocumentObject( NamingUtil.getFactTableName( factTableName ) );
		// write dimension name and dimension member count
		documentObject.writeInt( dimensionInfo.length );
		for ( int i = 0; i < dimensionInfo.length; i++ )
		{
			documentObject.writeString( dimensionInfo[i].dimensionName );
			documentObject.writeInt( dimensionInfo[i].dimensionLength );
		}
		// write measure name and measure data type
		documentObject.writeInt( measureInfo.length );
		for ( int i = 0; i < measureInfo.length; i++ )
		{
			documentObject.writeString( measureInfo[i].measureName );
			documentObject.writeInt( measureInfo[i].dataType );
		}
		// write segment count
		documentObject.writeInt( segmentNumber );
		documentObject.close( );
	}

	/**
	 * 
	 * @param iterator
	 * @param keyColumnNames
	 * @param measureColumnNames
	 * @param stopSign
	 * @return
	 * @throws BirtException
	 * @throws IOException
	 */
	private static DiskSortedStack getSortedFactTableRows( IDatasetIterator iterator,
			String[][] keyColumnNames, String[] measureColumnNames, StopSign stopSign )
			throws BirtException, IOException
	{
		DiskSortedStack result = new DiskSortedStack( Constants.FACT_TABLE_BUFFER_SIZE,
				true,
				false,
				FactTableRow.getCreator( ) );

		int[][] levelKeyColumnIndex = new int[keyColumnNames.length][];
		int[] measureColumnIndex = new int[measureColumnNames.length];

		for ( int i = 0; i < keyColumnNames.length; i++ )
		{
			levelKeyColumnIndex[i] = new int[keyColumnNames[i].length];
			for ( int j = 0; j < keyColumnNames[i].length; j++ )
			{
				levelKeyColumnIndex[i][j] = iterator.getFieldIndex( keyColumnNames[i][j] );
			}
		}
		for ( int i = 0; i < measureColumnIndex.length; i++ )
		{
			measureColumnIndex[i] = iterator.getFieldIndex( measureColumnNames[i] );
		}

		while ( iterator.next( ) && !stopSign.isStopped( ) )
		{
			FactTableRow factTableRow = new FactTableRow( );
			factTableRow.setDimensionKeys( new DimensionKey[levelKeyColumnIndex.length] );
			for ( int i = 0; i < levelKeyColumnIndex.length; i++ )
			{
				factTableRow.getDimensionKeys()[i] = 
					new DimensionKey( levelKeyColumnIndex[i].length );
				for( int j=0;j<levelKeyColumnIndex[i].length;j++)
				{
					if ( levelKeyColumnIndex[i][j] >= 0 )
						factTableRow.getDimensionKeys()[i].getKeyValues()[j] =
								iterator.getValue( levelKeyColumnIndex[i][j] );
				}
			}
			factTableRow.setMeasures( new Object[measureColumnIndex.length] );
			for ( int i = 0; i < measureColumnIndex.length; i++ )
			{
				factTableRow.getMeasures()[i] = iterator.getValue( measureColumnIndex[i] );
				if(factTableRow.getMeasures()[i]==null)
				{
					throw new DataException( ResourceConstants.FACTTABLE_NULL_MEASURE_VALUE,
							factTableRow.toString( ) );
				}
			}
			result.push( factTableRow );
		}
		return result;
	}

	/**
	 * 
	 * @param dimensionNumbers
	 * @param multiple
	 * @return
	 */
	private static int[] getDimensionMemberCount( Dimension[] dimension )
	{
		int[] dimensionMemberCount = new int[dimension.length];
		for ( int i = 0; i < dimension.length; i++ )
		{
			dimensionMemberCount[i] = dimension[i].length( );
		}
		return dimensionMemberCount;
	}

	/**
	 * 
	 * @param dimensionMemberCount
	 * @param multiple
	 * @return
	 */
	static DimensionDivision[] calculateDimensionDivision(
			int[] dimensionMemberCount, int blockNumber )
	{
		int[] subDimensionCount = DimensionDivider.divideDimension( dimensionMemberCount,
				blockNumber );
		DimensionDivision[] result = new DimensionDivision[dimensionMemberCount.length];
		for ( int i = 0; i < result.length; i++ )
		{
			result[i] = new DimensionDivision( dimensionMemberCount[i],
					subDimensionCount[i] );
		}

		return result;
	}

	/**
	 * 
	 * @param dimensionPosition
	 * @param dimensionDivision
	 * @return
	 */
	private static int[] getSubDimensionIndex( int[] dimensionPosition,
			DimensionDivision[] dimensionDivision )
	{
		assert dimensionPosition.length == dimensionDivision.length;
		int[] result = new int[dimensionPosition.length];
		for ( int i = 0; i < result.length; i++ )
		{
			result[i] = dimensionDivision[i].getSubDimensionIndex( dimensionPosition[i] );
		}
		return result;
	}
	
	/**
	 * 
	 * @param factTableName
	 * @param stopSign
	 * @return
	 * @throws IOException
	 */
	public FactTable load( String factTableName, StopSign stopSign )
			throws IOException
	{
		int segmentNumber = 0;
		IDocumentObject documentObject = 
			documentManager.openDocumentObject( NamingUtil.getFactTableName( factTableName ) );
		DimensionInfo[] dimensionInfo = new DimensionInfo[documentObject.readInt( )];
		for ( int i = 0; i < dimensionInfo.length; i++ )
		{
			dimensionInfo[i] = new DimensionInfo( );
			dimensionInfo[i].dimensionName = documentObject.readString( );
			dimensionInfo[i].dimensionLength = documentObject.readInt( );
		}
		MeasureInfo[] measureInfo = new MeasureInfo[documentObject.readInt( )];
		for ( int i = 0; i < measureInfo.length; i++ )
		{
			measureInfo[i] = new MeasureInfo( );
			measureInfo[i].measureName = documentObject.readString( );
			measureInfo[i].dataType = documentObject.readInt( );
		}
		segmentNumber = documentObject.readInt( );
		
		int[] dimensionMemberCount = new int[dimensionInfo.length];
		for( int i = 0;i<dimensionInfo.length;i++)
		{
			dimensionMemberCount[i] = dimensionInfo[i].dimensionLength;
		}
		DimensionDivision[] subDimensions = calculateDimensionDivision( dimensionMemberCount,
				segmentNumber );
		return new FactTable( factTableName,
				documentManager,
				dimensionInfo,
				measureInfo,
				segmentNumber,
				subDimensions ); 
	}
	
	

}


/**
 * 
 * @author Administrator
 *
 */
class FTSUNameSaveHelper
{
	private HashMap map;
	private IDocumentManager documentManager; 
	private String factTableName;
	
	/**
	 * 
	 * @param documentManager
	 * @param factTableName
	 */
	FTSUNameSaveHelper( IDocumentManager documentManager, String factTableName )
	{
		this.documentManager = documentManager;
		this.factTableName = factTableName;
		this.map = new HashMap( );
	}
	
	/**
	 * 
	 * @param name
	 */
	void add( String name )
	{
		if ( !map.containsKey( name ) )
		{
			map.put( name, null );
		}
	}
	
	/**
	 * 
	 * @throws IOException
	 */
	void save( ) throws IOException
	{
		IDocumentObject FTSUNameSave = documentManager.createDocumentObject( NamingUtil.getFTSUListName( factTableName ) );
		
		Iterator nameIterator = map.keySet( ).iterator( );
		while ( nameIterator.hasNext( ) )
		{
			FTSUNameSave.writeString( (String)nameIterator.next( ) );
		}
		FTSUNameSave.close( );
	}
}

/**
 * 
 * @author Administrator
 *
 */
class FTSUDocumentObjectNamingUtil
{

	/**
	 * All possible chars for representing a number as a String
	 */
	final static char[] digits = {
			'0', '1', '2', '3', '4', '5', '6', '7', '8', '9'
	};

	private static char[] buffer = new char[100];

	static String getDocumentObjectName( String factTableName, int[] subDimensionNumber )
	{
		int radix = 10;
		
		int position = 0;
		int i;
		
		for ( int k = subDimensionNumber.length - 1; k >= 0 ; k-- )
		{
			i = subDimensionNumber[k];
			while ( i > radix )
			{
				buffer[position++] = digits[i % radix];
				i = i / radix;
			}
			buffer[position++] = digits[i % radix];
			
			if ( k != 0 )
			{
				buffer[position++] = 'X';
			}
		}
		for ( int k = 0; k < position / 2; k++ )
		{
			char c = buffer[position - 1 - k];
			buffer[position - 1 - k] = buffer[k];
			buffer[k] = c;
		}
		return factTableName + new String( buffer, 0, position );
	}
}

class DimensionDivider
{

	static int[] divideDimension( int[] dimensionLength,
			int blockNumber )
	{
		Set indexSet = new HashSet( );

		int[] subDimensionCount = new int[dimensionLength.length];
		for ( int i = 0; i < subDimensionCount.length; i++ )
		{
			subDimensionCount[i] = 1;
		}

		if ( blockNumber > 1 )
			calculateSubDimensionCount( dimensionLength,
					blockNumber,
					subDimensionCount,
					indexSet );

		return subDimensionCount;
	}

	private static void calculateSubDimensionCount( int[] dimensionLength,
			int maxSubDimensionCount, int[] subDimensionCount,
			Set indexSet )
	{
		if ( indexSet.size( ) == subDimensionCount.length )
			return;

		for ( int i = 0; i < subDimensionCount.length; i++ )
		{
			if ( indexSet.contains( new Integer( i ) ) )
				continue;

			if ( subDimensionCount[i] + 1 > dimensionLength[i] )
			{
				indexSet.add( new Integer( i ) );
				continue;
			}

			subDimensionCount[i]++;
			if ( isOver( subDimensionCount, maxSubDimensionCount ) )
			{
				subDimensionCount[i]--;
				return;
			}
		}

		calculateSubDimensionCount( dimensionLength,
				maxSubDimensionCount,
				subDimensionCount,
				indexSet );
	}

	private static boolean isOver( int[] candidateArray, int target )
	{
		int candidate = 1;
		for ( int i = 0; i < candidateArray.length; i++ )
		{
			candidate *= candidateArray[i];
			if ( candidate > target )
				return true;
		}

		return false;
	}
}

/**
 * This class is used to find dimension position by dimension key quickly.
 * @author Administrator
 *
 */
class DimensionPositionSeeker
{
	private IDiskArray diskMemberArray;
	private DimensionKey[] memberArray;
	private int diskPostion;
	private int position;

	/**
	 * 
	 * @param members
	 * @throws IOException
	 */
	DimensionPositionSeeker( IDiskArray member ) throws IOException
	{
		IDiskArray members = getSortedDimensionKeys( member );
		this.memberArray = new DimensionKey[Math.min( Constants.LIST_BUFFER_SIZE,
				members.size( ) )];
		for ( int i = 0; i < memberArray.length; i++ )
		{
			memberArray[i] = (DimensionKey) members.get( i );
		}
		if ( members.size( ) > Constants.LIST_BUFFER_SIZE )
		{
			this.diskMemberArray = members;
			this.diskPostion = memberArray.length;
			this.position = this.diskPostion;
		}
	}
	
	private IDiskArray getSortedDimensionKeys( IDiskArray members )
			throws IOException
	{
		DiskSortedStack sortedStack = new DiskSortedStack( Constants.FACT_TABLE_BUFFER_SIZE,
				true,
				false,
				DimensionKey.getCreator( ) );
		for ( int i = 0; i < members.size( ); i++ )
		{
			sortedStack.push( members.get( i ) );
		}
		IDiskArray resultArray = new BufferedStructureArray( DimensionKey.getCreator( ),
				Constants.LIST_BUFFER_SIZE );
		Object key = sortedStack.pop( );
		while( key != null )
		{
			resultArray.add( key );
			key = sortedStack.pop( );
		}
		return resultArray;
	}

	/**
	 * Find dimension position by dimension key.
	 * 
	 * @param key
	 * @return
	 * @throws IOException
	 */
	int find( DimensionKey key ) throws IOException
	{
		int result = binarySearch( key );
		if ( result >= 0 )
		{
			return result;
		}
		if ( diskMemberArray != null )
		{
			return traverseFind( key );
		}
		return result;
	}
	
	/**
	 * 
	 * @param key
	 * @return
	 */
	private int binarySearch( DimensionKey key )
	{
		int result = Arrays.binarySearch( memberArray, key );
		if( result >= 0 )
		{
			return memberArray[result].getDimensionPos();
		}
		return -1;
//		for (int i = 0; i < memberArray.length; i++) 
//		{
//			if( memberArray[i].compareTo(key) == 0 )
//			{
//				return i;
//			}
//		}
//		return -1;
	}
	
	/**
	 * 
	 * @param key
	 * @return
	 * @throws IOException
	 */
	private int traverseFind( DimensionKey key ) throws IOException
	{
		for ( int i = position; i < diskMemberArray.size( ); i++ )
		{
			if ( ( (DimensionKey) diskMemberArray.get( i ) ).compareTo( key ) == 0 )
			{
				position = i;
				return ((DimensionKey) diskMemberArray.get( i )).getDimensionPos();
			}
		}
		for ( int i = diskPostion; i < position; i++ )
		{
			if ( ( (DimensionKey) diskMemberArray.get( i ) ).compareTo( key ) == 0 )
			{
				position = i;
				return ((DimensionKey) diskMemberArray.get( i )).getDimensionPos();
			}
		}
		return -1;
	}
}

class DimensionInfo
{
	String dimensionName;
	int dimensionLength;
}

class MeasureInfo
{
	String measureName;
	int dataType;
}

class CombinedPositionContructor
{
	private DimensionDivision[] subDimensions;
	private int[] dimensionBitLength;
	private int totalBitLength;

	CombinedPositionContructor( DimensionDivision[] subDimensions )
	{
		this.subDimensions = subDimensions;
		calculateBitLength( subDimensions );
	}

	private void calculateBitLength( DimensionDivision[] dimensionDivision )
	{
		int maxRange;
		dimensionBitLength = new int[dimensionDivision.length];
		for ( int i = 0; i < dimensionDivision.length; i++ )
		{
			IntRange[] ranges = dimensionDivision[i].getRanges();
			maxRange = 0;
			for ( int j = 0; j < ranges.length; j++ )
			{
				if ( ranges[j].end - ranges[j].start > maxRange )
				{
					maxRange = ranges[j].end - ranges[j].start + 1;
				}
			}
			dimensionBitLength[i] = getBitLength( maxRange );
			totalBitLength += dimensionBitLength[i];
		}
	}

	/**
	 * 
	 * @param maxInt
	 * @return
	 */
	private int getBitLength( int maxInt )
	{
		int bitLength = 1;
		int powerValue = 2;

		while ( powerValue < maxInt )
		{
			bitLength++;
			powerValue *= 2;
		}
		return bitLength;
	}

	/**
	 * 
	 * @param subdimensionIndex
	 * @param dimensionPosition
	 * @return
	 */
	public BigInteger calculateCombinedPosition( int[] subdimensionIndex, int[] dimensionPosition )
	{
		long l = dimensionPosition[0]
				- subDimensions[0].getRanges()[subdimensionIndex[0]].start;
		int bitLength = dimensionBitLength[0];
		int i;
		for ( i = 1; i < dimensionPosition.length; i++ )
		{
			if ( bitLength + dimensionBitLength[i] >= 63 )
			{
				break;
			}
			l <<= dimensionBitLength[i];
			l |= dimensionPosition[i]
					- subDimensions[i].getRanges()[subdimensionIndex[i]].start;
			bitLength += dimensionBitLength[i];
		}

		BigInteger bigInteger = BigInteger.valueOf( l );
		for ( ; i < dimensionPosition.length; i++ )
		{
			bigInteger = bigInteger.shiftLeft( dimensionBitLength[i] );
			bigInteger = bigInteger.or( BigInteger.valueOf( dimensionPosition[i]
					- subDimensions[i].getRanges()[subdimensionIndex[i]].start ) );
		}

		return bigInteger;
	}

	/**
	 * 
	 * @param subdimensionIndex
	 * @param combinedPosition
	 * @return
	 */
	public int[] calculateDimensionPosition( int[] subdimensionIndex, byte[] combinedPosition )
	{
		BigInteger bigInteger = new BigInteger( combinedPosition );
		int[] dimensionPosition = new int[dimensionBitLength.length];
		if ( totalBitLength <= 63 )
		{
			long l = bigInteger.longValue( );
			for ( int i = dimensionBitLength.length - 1; i >= 0; i-- )
			{
				dimensionPosition[i] = subDimensions[i].getRanges()[subdimensionIndex[i]].start
						+ (int) ( l & ( 0x7fffffff >> ( 31 - dimensionBitLength[i] ) ) );
				l >>= dimensionBitLength[i];
			}
			return dimensionPosition;
		}
		for ( int i = dimensionBitLength.length - 1; i >= 0; i-- )
		{
			dimensionPosition[i] = subDimensions[i].getRanges()[subdimensionIndex[i]].start
					+ (int) ( bigInteger.and( BigInteger.valueOf( 0x7fffffff >> ( 31 - dimensionBitLength[i] ) ) ).longValue( ) );
			bigInteger = bigInteger.shiftRight( dimensionBitLength[i] );
		}

		return dimensionPosition;
	}

}