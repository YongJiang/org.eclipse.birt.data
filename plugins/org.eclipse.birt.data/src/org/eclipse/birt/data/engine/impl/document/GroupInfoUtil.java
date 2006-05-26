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
package org.eclipse.birt.data.engine.impl.document;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.birt.data.engine.core.DataException;
import org.eclipse.birt.data.engine.executor.transform.group.GroupInfo;

/**
 * Utility class to manipulate group information.
 * 
 * @author lzhu
 *
 */
public class GroupInfoUtil {

	/*
	 * groups[level] is an ArrayList of GroupInfo objects at the specified level.
	 * Level is a 0-based group index, with 0 denoting the outermost group, etc.
	 * Example: 
	 * Row GroupKey1 	GroupKey2 	GroupKey3 	Column4 	Column5 
	 * 0: 	CHINA 		BEIJING 	2003 		Cola 		$100 
	 * 1: 	CHINA		BEIJING		2003		Pizza 		$320 
	 * 2: 	CHINA		BEIJING		2004 		Cola 		$402 
	 * 3: 	CHINA		SHANGHAI	2003		Cola		$553 
	 * 4:	CHINA		SHANGHAI	2003		Pizza		$223
	 * 5: 	CHINA 		SHANGHAI	2004		Cola		$226
	 * 6: 	USA 		CHICAGO		2004		Pizza		$133
	 * 7: 	USA			NEW YORK	2004		Cola		$339
	 * 8: 	USA 		NEW YORK	2004		Cola		$297
	 * 
	 * groups: (parent, child) 
	 * 		LEVEL 0 		LEVEL 1 		LEVEL 2
	 * ============================================ 
	 * 0: 	-,0 			0,0 			0,0 
	 * 1: 	-,2 			0,2 			0,2 
	 * 2:	 				1,4 			1,3 
	 * 3: 					1,5 			1,5 
	 * 4: 									2,6 
	 * 5: 									3,7 
	 */
	
	/**
	 * 
	 * @param groups
	 * @param indexArray
	 * @return
	 * @throws DataException 
	 */
	public static List[] getGroupInfo( List[] groups, int[] indexArray )
			throws DataException
	{
		Map deleteNumMap = new HashMap( );
		if ( groups == null || groups.length == 0 || indexArray == null )
			return groups;
		List endLevelList = groups[groups.length - 1];
		if ( validateGroupLevel( endLevelList, indexArray ) )
		{
			int firstChild = -1, count = 0, startCount = 0;

			GroupInfo baseInfo = (GroupInfo) endLevelList.get( 0 );
			for ( int i = 1; i < endLevelList.size( ); i++ )
			{
				count = 0;
				GroupInfo info = (GroupInfo) endLevelList.get( i );
				firstChild = info.firstChild;

				while ( startCount < indexArray.length )
				{
					if ( indexArray[startCount] < firstChild
							&& indexArray[startCount] >= baseInfo.firstChild )
					{
						startCount++;
						count++;
						continue;
					}
					else
						break;
				}
				if ( count < firstChild - baseInfo.firstChild )
				{
					deleteNumMap.put( info, new Integer( firstChild
							- baseInfo.firstChild - count ) );
				}
				baseInfo = info;
			}
		}
		else
		{
			throw new DataException( "The index array is invalid" );
		}
		return cleanUnUsedGroupInstance(doRefactorOnGroupInfo( groups, deleteNumMap ));
	}
	
	/**
	 * 
	 * @param groups
	 * @param deletedNumMap
	 * @return
	 * @throws DataException 
	 */
	private static List[] doRefactorOnGroupInfo( List[] groups, Map deletedNumMap ) throws DataException
	{
		List endLevelList = groups[groups.length - 1];

		int deleteNum;

		for ( int i = 1; i < endLevelList.size( ); i++ )
		{
			GroupInfo info = (GroupInfo) endLevelList.get( i );
			if ( deletedNumMap.get( info ) == null )
				continue;
			deleteNum = ( (Integer) deletedNumMap.get( info ) ).intValue( );
			if ( deleteNum > 0 )
			{
				if ( refactorOnGroup( groups, i, deleteNum, groups.length - 1 ) )
					deletedNumMap.remove( info );
			}
		}
		for ( int i = 1; i < endLevelList.size( ); i++ )
		{
			GroupInfo info = (GroupInfo) endLevelList.get( i );
			if ( deletedNumMap.get( info ) == null )
				continue;
			deleteNum = ( (Integer) deletedNumMap.get( info ) ).intValue( );
			
			if ( deleteNum > 0 )
			{
				resetStatus( groups, i, deleteNum, groups.length - 1 );
			}
		}
		return groups;
	}

	/**
	 * 
	 * @param groups
	 * @param groupIndex
	 * @param deletedNum
	 * @param level
	 * @return
	 * @throws DataException
	 */
	private static boolean refactorOnGroup( List[] groups, int groupIndex,
			int deletedNum, int level ) throws DataException
	{
		List levelList = groups[level];
		int index = groupIndex;

		GroupInfo groupInfo = (GroupInfo) levelList.get( groupIndex );
		GroupInfo baseInfo = (GroupInfo) levelList.get( groupIndex - 1 );

		if ( groupInfo.firstChild > 0
				&& baseInfo.firstChild >= 0
				&& groupInfo.firstChild - baseInfo.firstChild > deletedNum )
		{
			while ( index < levelList.size( ) && groupInfo.firstChild > 0 )
			{
				groupInfo = (GroupInfo) levelList.get( index );
				groupInfo.firstChild = groupInfo.firstChild - deletedNum;
				index++;
			}
			return true;
		}
		else if ( groupInfo.firstChild > 0
				&& baseInfo.firstChild >= 0
				&& groupInfo.firstChild - baseInfo.firstChild == deletedNum )
			return false;
		else
			throw new DataException( "The removed group item is not correct" );
	}
	
	/**
	 * 
	 * @param groups
	 * @param groupIndex
	 * @param deletedNum
	 * @param level
	 */
	private static void resetStatus( List[] groups, int groupIndex,
			int deletedNum, int level )
	{
		List levelList = groups[level];
		GroupInfo groupInfo = (GroupInfo) levelList.get( groupIndex );
		groupInfo.firstChild = -2;
	}
	
	/**
	 * 
	 * @param endLevelList
	 * @param indexArray
	 * @return
	 */
	private static boolean validateGroupLevel( List endLevelList, int[] indexArray )
	{
		assert endLevelList != null;
		int firstChild = -1, startCount = 0;

		GroupInfo baseInfo = (GroupInfo) endLevelList.get( 0 );

		for ( int i = 1; i < endLevelList.size( ); i++ )
		{
			GroupInfo info = (GroupInfo) endLevelList.get( i );
			firstChild = info.firstChild;

			while ( startCount < indexArray.length )
			{
				if ( indexArray[startCount] < firstChild
						&& indexArray[startCount] >= baseInfo.firstChild )
				{
					startCount++;
				}
				else
				{
					break;
				}
			}
			baseInfo = info;
		}
		if ( startCount + 1 == indexArray.length )
			return true;
		else
			return false;
	}

	/**
	 * 
	 */
	static List[] cleanUnUsedGroupInstance( List[] groups )
	{
		int last = groups.length - 1;
		List lastGroup = groups[last];
		for( int i = 0; i < lastGroup.size(); i++ )
		{
			if( ((GroupInfo)lastGroup.get(i)).firstChild == -2 )
			{
				return cleanUnUsedGroupInstance( removeWholeGroup( groups, last, i));
			}
		}
		return groups;
	}
	
	/**
	 * 
	 * @param groups
	 * @param groupLevelIndex
	 * @param groupInstanceIndex
	 * @return
	 */
	private static List[] removeWholeGroup( List[] groups, int groupLevelIndex, int groupInstanceIndex )
	{
		List lastGroup = groups[groupLevelIndex];
		
		int parent = ((GroupInfo)lastGroup.get(groupInstanceIndex)).parent;
		
		boolean shouldRemoveParent = manipulateParentGroup(groups, groupLevelIndex, groupInstanceIndex );
		
		if( groupLevelIndex < groups.length - 1 )
		{
			//If there are other group instances in current group level.
			if( groupInstanceIndex < lastGroup.size()-1)
			{
				int start = ((GroupInfo)groups[groupLevelIndex].get(groupInstanceIndex+1)).firstChild;
				//Here start is not equals to -2, for the adjacent group instance with child equals
				//to -2 only can be find in outermost group level.
				List childGroup = groups[groupLevelIndex+1];
				for( int j = start; j < childGroup.size(); j++ )
				{
					((GroupInfo) childGroup.get(j)).parent = ((GroupInfo) childGroup
							.get(j)).parent - 1; 
				}
			}
		}
		//Remove the GroupInfo from list.
		lastGroup.remove( groupInstanceIndex );
		
		if( shouldRemoveParent )
		{
			return removeWholeGroup( groups, groupLevelIndex-1, parent );
		}
		
		return groups;
	}

	private static boolean manipulateParentGroup(List[] groups,
			int groupLevelIndex, int groupInstanceIndex) 
	{
		boolean shouldRemoveParent = false;
		int parent = ((GroupInfo)groups[groupLevelIndex].get(groupInstanceIndex)).parent;
		if( groupLevelIndex > 0 )
		{
			List parentGroup = groups[groupLevelIndex - 1];
			for (int j = parent + 1; j < parentGroup.size(); j++)
			{
				((GroupInfo) parentGroup.get(j)).firstChild = ((GroupInfo) parentGroup
						.get(j)).firstChild - 1;
			}
			if (isWholeGroupEliminate(groupInstanceIndex, parent, groups[groupLevelIndex]))
			{
				shouldRemoveParent = true;
			}
		}
		return shouldRemoveParent;
	}
	
	/**
	 * Detemine whether the whole parent group instance is empty after remove current group 
	 * instance.
	 * 
	 * @param currentIndex
	 * @param parent
	 * @param groups
	 * @return
	 */
	private static boolean isWholeGroupEliminate( int currentIndex, int parent, List groups )
	{	
		if( currentIndex - 1 >= 0 )
		{
			if (((GroupInfo)groups.get(currentIndex-1)).parent == parent)
				return false;
		}
		if( currentIndex + 1 < groups.size() )
		{
			if(((GroupInfo)groups.get(currentIndex+1)).parent == parent)
				return false;
		}
		return true;
	}
}
