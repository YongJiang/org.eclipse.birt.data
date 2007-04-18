/*
 *************************************************************************
 * Copyright (c) 2006 Actuate Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Actuate Corporation  - initial API and implementation
 *  
 *************************************************************************
 */

package org.eclipse.birt.report.data.adapter.impl;

import java.sql.Time;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.eclipse.birt.core.data.DataType;
import org.eclipse.birt.core.data.ExpressionUtil;
import org.eclipse.birt.core.exception.BirtException;
import org.eclipse.birt.data.engine.api.IResultIterator;
import org.eclipse.birt.data.engine.api.querydefn.GroupDefinition;
import org.eclipse.birt.data.engine.api.querydefn.QueryDefinition;
import org.eclipse.birt.data.engine.api.querydefn.ScriptExpression;
import org.eclipse.birt.data.engine.olap.api.cube.IDatasetIterator;
import org.eclipse.birt.data.engine.olap.util.OlapExpressionUtil;
import org.eclipse.birt.report.data.adapter.api.DataRequestSession;
import org.eclipse.birt.report.model.api.LevelAttributeHandle;
import org.eclipse.birt.report.model.api.olap.CubeHandle;
import org.eclipse.birt.report.model.api.olap.DimensionHandle;
import org.eclipse.birt.report.model.api.olap.MeasureGroupHandle;
import org.eclipse.birt.report.model.api.olap.MeasureHandle;
import org.eclipse.birt.report.model.api.olap.TabularCubeHandle;
import org.eclipse.birt.report.model.api.olap.TabularDimensionHandle;
import org.eclipse.birt.report.model.api.olap.TabularHierarchyHandle;
import org.eclipse.birt.report.model.api.olap.TabularLevelHandle;

/**
 * This is an implementation of IDatasetIterator interface.
 *
 */
public class DataSetIterator implements IDatasetIterator
{

	//
	private boolean started = false;
	private IResultIterator it;
	private ResultMeta metadata;

	private static long nullTime;
	
	static
	{
		Calendar c = Calendar.getInstance( );
		c.clear( );
		c.set( 0, 0, 1, 0, 0, 1 );
		nullTime = c.getTimeInMillis( );
		
	}
	
	/**
	 * Create DataSetIterator for a hierarchy.
	 * 
	 * @param session
	 * @param hierHandle
	 * @throws BirtException
	 */
	public DataSetIterator( DataRequestSession session,
			TabularHierarchyHandle hierHandle ) throws BirtException
	{
		QueryDefinition query = new QueryDefinition( );
		query.setUsesDetails( true );

		query.setDataSetName( hierHandle.getDataSet( ).getQualifiedName( ) );

		List metaList = new ArrayList();
		this.prepareLevels( query,
				hierHandle, metaList );

		this.it = session.prepare( query ).execute( null ).getResultIterator( );
		this.metadata = new ResultMeta( metaList );
	}

	/**
	 * Create DataSetIterator for fact table.
	 * 
	 * @param session
	 * @param cubeHandle
	 * @throws BirtException
	 */
	public DataSetIterator( DataRequestSession session,
			TabularCubeHandle cubeHandle ) throws BirtException
	{
		QueryDefinition query = new QueryDefinition( );

		query.setUsesDetails( true );
		query.setDataSetName( cubeHandle.getDataSet( ).getQualifiedName( ) );

		List dimensions = cubeHandle.getContents( CubeHandle.DIMENSIONS_PROP );

		List metaList = new ArrayList();
		if ( dimensions != null )
		{
			for ( int i = 0; i < dimensions.size( ); i++ )
			{
				TabularDimensionHandle dimension = (TabularDimensionHandle) dimensions.get( i );
				List hiers = dimension.getContents( DimensionHandle.HIERARCHIES_PROP );

				//By now we only support one hierarchy per dimension.
				assert hiers.size( ) == 1;

				TabularHierarchyHandle hierHandle = (TabularHierarchyHandle) hiers.get( 0 );

				if ( hierHandle.getDataSet( ) == null
						|| hierHandle.getDataSet( )
								.getQualifiedName( )
								.equals( cubeHandle.getDataSet( ).getQualifiedName( ) ) )
				{
					prepareLevels( query,
							hierHandle, metaList );
				}
				else
				{
					//TODO need model support.
					/*//Use different data set
					 Iterator it = cubeHandle.joinConditionsIterator( );
					 DimensionConditionHandle dimCondHandle = (DimensionConditionHandle) it.next( );
					 
					 if ( dimCondHandle.getHierarchy( ).equals( hierHandle ))
					 {
					 dimCondHandle.getPrimaryKeys( )
					 }*/
				}

			}
		}

		prepareMeasure( cubeHandle, query, metaList );

		this.it = session.prepare( query ).execute( null ).getResultIterator( );
		this.metadata = new ResultMeta( metaList );

	}

	/**
	 * 
	 * @param cubeHandle
	 * @param query
	 * @param resultMetaList
	 */
	private void prepareMeasure( TabularCubeHandle cubeHandle,
			QueryDefinition query, List metaList )
	{
		List measureGroups = cubeHandle.getContents( CubeHandle.MEASURE_GROUPS_PROP );
		for ( int i = 0; i < measureGroups.size( ); i++ )
		{
			MeasureGroupHandle mgh = (MeasureGroupHandle) measureGroups.get( i );
			List measures = mgh.getContents( MeasureGroupHandle.MEASURES_PROP );
			for ( int j = 0; j < measures.size( ); j++ )
			{
				MeasureHandle measure = (MeasureHandle) measures.get( j );
				String function = measure.getFunction( );
				if ( query.getGroups( ).size( ) > 0 )
				{
					ScriptExpression se = populateExpression( query,
							measure,
							function );

					query.addResultSetExpression( measure.getName( ), se );
				}
				else
				{
					query.addResultSetExpression( measure.getName( ),
							new ScriptExpression( measure.getMeasureExpression( ) ) );
				}

				ColumnMeta meta = new ColumnMeta( measure.getName( ), false );
				//TODO after model finish support measure type, use data type defined in
				//measure handle.
				meta.setDataType( ModelAdapter.adaptModelDataType( measure.getDataType( )) );
				metaList.add( meta );
			}
		}
	}

	/**
	 * 
	 * @param query
	 * @param measure
	 * @param function
	 * @return
	 */
	private ScriptExpression populateExpression( QueryDefinition query,
			MeasureHandle measure, String function )
	{
		ScriptExpression se = null;
		
		if ( function == null || function.equals( "sum" ) )
		{
			se = new ScriptExpression( "Total.sum("
					+ measure.getMeasureExpression( ) + ",null,"
					+ query.getGroups( ).size( ) + ")" );

		}
		else if ( function.equals( "count" ) )
		{
			se = new ScriptExpression( "Total.count("
					+ "null," + query.getGroups( ).size( ) + ")" );
		}
		else if ( function.equals( "min" ) )
		{
			se = new ScriptExpression( "Total.min("
					+ measure.getMeasureExpression( ) + ",null,"
					+ query.getGroups( ).size( ) + ")" );
		}
		else if ( function.equals( "max" ) )
		{
			se = new ScriptExpression( "Total.max("
					+ measure.getMeasureExpression( ) + ",null,"
					+ query.getGroups( ).size( ) + ")" );
		}
		se.setDataType( ModelAdapter.adaptModelDataType( measure.getDataType( ) ) );
		return se;
	}

	/**
	 * 
	 * @param query
	 * @param resultMetaList
	 * @param levelNameColumnNamePair
	 * @param hierHandle
	 */
	private void prepareLevels( QueryDefinition query, TabularHierarchyHandle hierHandle, List metaList )
	{
		//Use same data set as cube fact table
		List levels = hierHandle.getContents( TabularHierarchyHandle.LEVELS_PROP );

		for ( int j = 0; j < levels.size( ); j++ )
		{

			TabularLevelHandle level = (TabularLevelHandle) levels.get( j );
			ColumnMeta temp = new ColumnMeta( level.getName( ), true );
			int type = ModelAdapter.adaptModelDataType(level.getDataType( ));
			if ( isTimeType( type ))
				temp.setDataType( DataType.INTEGER_TYPE );
			temp.setDataType( type );
			metaList.add( temp );
			Iterator it = level.attributesIterator( );
			while ( it.hasNext( ) )
			{
				LevelAttributeHandle levelAttr = (LevelAttributeHandle) it.next( );
				ColumnMeta meta = new ColumnMeta( OlapExpressionUtil.getAttributeColumnName( level.getName( ),
						levelAttr.getName( )), false );

				meta.setDataType( ModelAdapter.adaptModelDataType( levelAttr.getDataType( ) ) );
				metaList.add( meta );
				query.addResultSetExpression( meta.getName( ),
						new ScriptExpression( ExpressionUtil.createJSDataSetRowExpression( levelAttr.getName( ) ) ) );
			}
			
			String exprString = populateLevelKeyExpression( level, type );
			
			query.addResultSetExpression( level.getName( ),
					new ScriptExpression( exprString ));
			
			GroupDefinition gd = new GroupDefinition( );
			gd.setKeyExpression( ExpressionUtil.createJSRowExpression( level.getName( ) ) );
			query.addGroup( gd );
		}
	}

	private String populateLevelKeyExpression( TabularLevelHandle level,
			int type )
	{
		String exprString = ExpressionUtil.createJSDataSetRowExpression( level.getColumnName( ) );
	/*	if ( isTimeType( type ))
		{
			String dateName = level.getDateTimeLevelType( );
			if ( DesignChoiceConstants.DATE_TIME_LEVEL_TYPE_DAY.equals( dateName ))
			{
				exprString = "BirtDateTime.day(" + exprString + ")";
			}
			else if ( DesignChoiceConstants.DATE_TIME_LEVEL_TYPE_WEEK.equals( dateName ))
			{
				exprString = "BirtDateTime.week(" + exprString + ")";
			}
			if ( DesignChoiceConstants.DATE_TIME_LEVEL_TYPE_MONTH.equals( dateName ))
			{
				exprString = "BirtDateTime.month(" + exprString + ")";
			}
			if ( DesignChoiceConstants.DATE_TIME_LEVEL_TYPE_QUARTER.equals( dateName ))
			{
				exprString = "BirtDateTime.quarter(" + exprString + ")";
			}
			if ( DesignChoiceConstants.DATE_TIME_LEVEL_TYPE_YEAR.equals( dateName ))
			{
				exprString = "BirtDateTime.year(" + exprString + ")";
			}
		}*/
		return exprString;
	}

	/**
	 * 
	 * @param type
	 * @return
	 */
	private boolean isTimeType( int type )
	{
		if ( type == DataType.DATE_TYPE 
			 || type == DataType.SQL_DATE_TYPE	)
		{
			return true;
		}	
		return false;
	}
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.birt.data.engine.olap.api.cube.IDatasetIterator#close()
	 */
	public void close( ) throws BirtException
	{
		it.close( );

	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.birt.data.engine.olap.api.cube.IDatasetIterator#getFieldIndex(java.lang.String)
	 */
	public int getFieldIndex( String name ) throws BirtException
	{
		return this.metadata.getFieldIndex( name );
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.birt.data.engine.olap.api.cube.IDatasetIterator#getFieldType(java.lang.String)
	 */
	public int getFieldType( String name ) throws BirtException
	{
		return this.metadata.getFieldType( name );
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.birt.data.engine.olap.api.cube.IDatasetIterator#getValue(int)
	 */
	public Object getValue( int fieldIndex ) throws BirtException
	{
		Object value = it.getValue( this.metadata.getFieldName( fieldIndex ) );
		if ( value == null )
		{
			return this.metadata.getNullValueReplacer( fieldIndex );
		}
		return value;
	}



	/*
	 * (non-Javadoc)
	 * @see org.eclipse.birt.data.engine.olap.api.cube.IDatasetIterator#next()
	 */
	public boolean next( ) throws BirtException
	{
		if ( it.getQueryResults( )
				.getPreparedQuery( )
				.getReportQueryDefn( )
				.getGroups( )
				.size( ) == 0 )
			return it.next( );
		if ( !started )
		{
			started = true;
			return it.next( );
		}
		else
		{
			it.skipToEnd( it.getQueryResults( )
					.getPreparedQuery( )
					.getReportQueryDefn( )
					.getGroups( )
					.size( ) );
			return it.next( );
		}
	}

	/**
	 * 
	 *
	 */
	private class ResultMeta
	{
		//
		private HashMap columnMetaMap;
		private HashMap indexMap;
		private Object[] nullValueReplacer;
		
		
		/**
		 * Constructor.
		 * @param columnMetas
		 */
		ResultMeta( List columnMetas )
		{
			this.columnMetaMap = new HashMap( );
			this.indexMap = new HashMap( );
			this.nullValueReplacer = new Object[columnMetas.size( )];
			for ( int i = 0; i < columnMetas.size( ); i++ )
			{
				ColumnMeta columnMeta = (ColumnMeta) columnMetas.get( i );
				columnMeta.setIndex( i + 1 );
				this.columnMetaMap.put( columnMeta.getName( ), columnMeta );
				this.indexMap.put( new Integer( i + 1 ), columnMeta );
				if ( columnMeta.isLevelKey( ))
				{
					this.nullValueReplacer[i] = createNullValueReplacer( columnMeta.getType( ));
				}
			}
		}

		/**
		 * 
		 * @param fieldName
		 * @return
		 */
		public int getFieldIndex( String fieldName )
		{
			return ( (ColumnMeta) this.columnMetaMap.get( fieldName ) ).getIndex( );
		}

		/**
		 * 
		 * @param fieldName
		 * @return
		 */
		public int getFieldType( String fieldName )
		{
			return ( (ColumnMeta) this.columnMetaMap.get( fieldName ) ).getType( );
		}

		/**
		 * 
		 * @param index
		 * @return
		 */
		public String getFieldName( int index )
		{
			return ( (ColumnMeta) this.indexMap.get( new Integer( index ) ) ).getName( );
		}
		
		/**
		 * 
		 * @param index
		 * @return
		 */
		public Object getNullValueReplacer( int index )
		{
			return this.nullValueReplacer[index - 1];
		}
		
		/**
		 * 
		 * @param fieldType
		 * @return
		 */
		private Object createNullValueReplacer( int fieldType )
		{
			
			switch ( fieldType )
			{
				case DataType.DATE_TYPE :
					return new java.util.Date( nullTime );
				case DataType.SQL_DATE_TYPE :
					return new java.sql.Date( nullTime );
				case DataType.SQL_TIME_TYPE :
					return new Time( nullTime );
				case DataType.BOOLEAN_TYPE :
					return new Boolean( false );
				case DataType.DECIMAL_TYPE :
					return new Double( 0 );
				case DataType.DOUBLE_TYPE :
					return new Double( 0 );
				case DataType.INTEGER_TYPE :
					return new Integer( 0 );
				case DataType.STRING_TYPE :
					return "";
				default :
					return "";
			}
		}
	}

	/**
	 * 
	 *
	 */
	private class ColumnMeta
	{
		//
		private String name;
		private int type;
		private int index;
		private boolean isLevelKey;
		/**
		 * 
		 * @param name
		 */
		ColumnMeta( String name, boolean isLevelKey )
		{
			this.name = name;
			this.isLevelKey = isLevelKey;
		}

		/**
		 * 
		 * @return
		 */
		public int getIndex( )
		{
			return this.index;
		}

		/**
		 * 
		 * @return
		 */
		public int getType( )
		{
			return this.type;
		}

		/**
		 * 
		 * @return
		 */
		public String getName( )
		{
			return this.name;
		}

		/**
		 * 
		 * @param index
		 */
		public void setIndex( int index )
		{
			this.index = index;
		}

		/**
		 * 
		 * @param type
		 */
		public void setDataType( int type )
		{
			this.type = type;
		}
		
		/**
		 * 
		 * @return
		 */
		public boolean isLevelKey( )
		{
			return this.isLevelKey;
		}
	}
}
