/*******************************************************************************
 * Copyright (c) 2008 Actuate Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Actuate Corporation  - initial API and implementation
 *******************************************************************************/

package org.eclipse.birt.report.data.oda.jdbc.ui.model;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.birt.report.data.oda.jdbc.ui.JdbcPlugin;
import org.eclipse.birt.report.data.oda.jdbc.ui.provider.JdbcMetaDataProvider;
import org.eclipse.birt.report.data.oda.jdbc.ui.util.Utility;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.graphics.Image;

public class SchemaNode extends ChildrenAllowedNode
		implements
			Comparable<SchemaNode>
{

	private static Logger logger = Logger.getLogger( SchemaNode.class.getName( ) );
	private static String SCHEMA_ICON = SchemaNode.class.getName( )
			+ ".SchemaIcon";
	static
	{
		ImageRegistry reg = JFaceResources.getImageRegistry( );
		reg.put( SCHEMA_ICON, ImageDescriptor.createFromFile( JdbcPlugin.class,
				"icons/schema.gif" ) );//$NON-NLS-1$
	}

	private String schemaName;

	public SchemaNode( String schemaName )
	{
		this.schemaName = schemaName;
	}

	@Override
	protected IDBNode[] refetchChildren( FilterConfig fc )
	{
		String[] tableTypes = fc.getTableTypesForJDBC( );
		List<IDBNode> children = new ArrayList<IDBNode>( );
		if ( tableTypes != null )
		{
			ResultSet rs = JdbcMetaDataProvider.getInstance( )
					.getAlltables( schemaName, fc.getNamePattern( ), tableTypes );

			if ( rs != null )
			{
				int maxTableCountPerSchema = fc.getMaxTableCountPerSchema( );
				int count = 0;
				try
				{
					while ( rs.next( ) && count < maxTableCountPerSchema )
					{
						String tableName = rs.getString( "TABLE_NAME" );
						String type = rs.getString( "TABLE_TYPE" );//$NON-NLS-1$
						TableNode table = new TableNode( schemaName,
								tableName,
								"VIEW".equalsIgnoreCase( type ) );
						children.add( table );
						count++;
					}
				}
				catch ( SQLException e )
				{
					logger.log( Level.WARNING, e.getLocalizedMessage( ), e );
				}
			}
		}
		if ( JdbcMetaDataProvider.getInstance( ).isSupportProcedure( )
				&& ( fc.getType( ) == FilterConfig.Type.ALL || fc.getType( ) == FilterConfig.Type.PROCEDURE ) )
		{
			children.add( new ProcedureFlagNode( schemaName ) );
		}
		return children.toArray( new IDBNode[0] );
	}

	public int compareTo( SchemaNode o )
	{
		return schemaName.compareTo( o.schemaName );
	}

	public String getDisplayName( )
	{
		return schemaName;
	}

	public Image getImage( )
	{
		return JFaceResources.getImageRegistry( ).get( SCHEMA_ICON );
	}

	public String getQualifiedNameInSQL( boolean useIdentifierQuoteString,
			boolean includeSchema )
	{
		String quoteFlag = "";
		if ( useIdentifierQuoteString )
		{
			quoteFlag = JdbcMetaDataProvider.getInstance( )
					.getIdentifierQuoteString( );
		}
		return Utility.quoteString( schemaName, quoteFlag );
	}

}
