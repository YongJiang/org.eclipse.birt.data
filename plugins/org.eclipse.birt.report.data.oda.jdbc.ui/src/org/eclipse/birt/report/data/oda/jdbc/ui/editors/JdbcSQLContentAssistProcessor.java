/*******************************************************************************
 * Copyright (c) 2005 Actuate Corporation. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Eclipse
 * Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Actuate Corporation - initial API and implementation
 ******************************************************************************/

package org.eclipse.birt.report.data.oda.jdbc.ui.editors;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;

import org.eclipse.birt.report.data.oda.jdbc.ui.util.Column;
import org.eclipse.birt.report.data.oda.jdbc.ui.util.ConnectionMetaData;
import org.eclipse.birt.report.data.oda.jdbc.ui.util.ConnectionMetaDataManager;
import org.eclipse.birt.report.data.oda.jdbc.ui.util.JdbcToolKit;
import org.eclipse.birt.report.data.oda.jdbc.ui.util.Schema;
import org.eclipse.birt.report.data.oda.jdbc.ui.util.Table;
import org.eclipse.birt.report.designer.ui.editors.sql.ISQLSyntax;
import org.eclipse.birt.report.model.api.DataSetHandle;
import org.eclipse.birt.report.model.api.InputParameterHandle;
import org.eclipse.birt.report.model.api.OdaDataSetHandle;
import org.eclipse.birt.report.model.api.OdaDataSourceHandle;
import org.eclipse.birt.report.model.api.PropertyHandle;
import org.eclipse.birt.report.model.elements.DataSet;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;

/**
 * This is the content assistant for the sql editor. It provides the list of
 * parameters when the user types the ? keyword. It also shows a list of
 * available columns or tables depending on whether the user has entered a table
 * or a schema before the (.) dot keyword.
 * 
 * If both a schema and a table have the same name the results are
 * unpredictable.
 * 
 * @version $Revision: 1.2 $ $Date: 2005/02/06 06:33:32 $
 */

public class JdbcSQLContentAssistProcessor implements
		IContentAssistProcessor,
		ISQLSyntax
{

	private transient OdaDataSetHandle handle = null;
	private transient ConnectionMetaData metaData = null;
	private transient ICompletionProposal[] lastProposals = null;

	/**
	 *  
	 */
	public JdbcSQLContentAssistProcessor( DataSetHandle ds )
	{
		super( );
		handle = (OdaDataSetHandle) ds;
		OdaDataSourceHandle dataSourceHandle = (OdaDataSourceHandle) handle.getDataSource( );
		metaData = ConnectionMetaDataManager.getInstance( )
				.getMetaData( dataSourceHandle.getPublicDriverProperty( "ODA:driver-class" ),//$NON-NLS-1$
						dataSourceHandle.getPublicDriverProperty( "ODA:url" ),//$NON-NLS-1$
						dataSourceHandle.getPublicDriverProperty( "ODA:user" ),//$NON-NLS-1$
						dataSourceHandle.getPublicDriverProperty( "ODA:password" ), //$NON-NLS-1$
						JdbcToolKit.getJdbcDriverClassPath( dataSourceHandle.getDriverName( ) ),
						null );
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#computeCompletionProposals(org.eclipse.jface.text.ITextViewer,
	 *      int)
	 */
	public ICompletionProposal[] computeCompletionProposals(
			ITextViewer viewer, int offset )
	{
		try
		{
			if ( offset > viewer.getTopIndexStartOffset( ) )
			{
				//Check the character before the offset
				char ch = viewer.getDocument( ).getChar( offset - 1 );

				//If this is a ? then get the list of parameters
				if ( ch == '?' ) //$NON-NLS-1$
				{
					lastProposals = getParameterCompletionProposals( viewer,
							offset );
					return lastProposals;
				}
				else if ( ch == '.' ) //$NON-NLS-1$
				{
					lastProposals = getTableOrColumnCompletionProposals( viewer,
							offset );
					return lastProposals;
				}
				else
				{
					return getRelevantProposals( viewer, offset );
				}
			}
		}
		catch ( BadLocationException e )
		{
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#computeContextInformation(org.eclipse.jface.text.ITextViewer,
	 *      int)
	 */
	public IContextInformation[] computeContextInformation( ITextViewer viewer,
			int offset )
	{
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#getCompletionProposalAutoActivationCharacters()
	 */
	public char[] getCompletionProposalAutoActivationCharacters( )
	{
		return new char[]{
				'?', '.'
		}; //$NON-NLS-1$
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#getContextInformationAutoActivationCharacters()
	 */
	public char[] getContextInformationAutoActivationCharacters( )
	{
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#getErrorMessage()
	 */
	public String getErrorMessage( )
	{
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#getContextInformationValidator()
	 */
	public IContextInformationValidator getContextInformationValidator( )
	{
		return null;
	}

	/**
	 * @param viewer
	 * @param offset
	 * @return
	 */
	private ICompletionProposal[] getTableOrColumnCompletionProposals(
			ITextViewer viewer, int offset )
	{
		if ( offset > viewer.getTopIndexStartOffset( ) + 2 )
		{
			try
			{
				//Get the word before the dot
				//This can either be the table name or the schema name
				String tableName = stripQuotes( findWord( viewer, offset - 2 ) );
				String schemaName = null;

				//Check the character before this word
				int startOffset = offset - tableName.length( ) - 2;
				if ( startOffset > viewer.getTopIndexStartOffset( ) )
				{
					//If this is a dot then find the schama name
					if ( viewer.getDocument( ).getChar( startOffset ) == '.' )//$NON-NLS-1$
					{
						schemaName = findWord( viewer, startOffset - 1 );
					}
				}

				if ( schemaName == null )
				{
					//If the schema name is null
					//then the table name can either be a schema or a table
					//First check whether it is a schema
					Schema schema = metaData.getSchema( tableName );
					//If this is not null then just return all the tables from
					// it.
					if ( schema != null )
					{
						return convertTablesToCompletionProposals( schema.getTables( ),
								offset );
					}
					else
					{
						//Find the first table match in all the schemas and
						// return the columns
						ArrayList schemas = metaData.getSchemas( );
						Iterator iter = schemas.iterator( );
						while ( iter.hasNext( ) )
						{
							schema = (Schema) iter.next( );
							Table table = schema.getTable( tableName );
							if ( table != null )
							{
								return convertColumnsToCompletionProposals( table.getColumns( ),
										offset );
							}
						}
					}
				}
				else
				{
					schemaName = stripQuotes( schemaName );
					//We have both the schema and table name
					//return the column names
					Schema schema = metaData.getSchema( schemaName );
					if ( schema != null )
					{
						Table table = schema.getTable( tableName );
						if ( table != null )
						{
							return convertColumnsToCompletionProposals( table.getColumns( ),
									offset );
						}
					}
				}
			}
			catch ( BadLocationException e )
			{
			}
			catch ( SQLException e )
			{
			}
		}
		return null;
	}

	private ICompletionProposal[] getRelevantProposals( ITextViewer viewer,
			int offset ) throws BadLocationException
	{
		if ( lastProposals != null )
		{
			ArrayList relevantProposals = new ArrayList( 10 );

			String word = ( findWord( viewer, offset - 1 ) ).toLowerCase( );
			//Search for this word in the list

			for ( int n = 0; n < lastProposals.length; n++ )
			{
				if ( stripQuotes( lastProposals[n].getDisplayString( )
						.toLowerCase( ) ).startsWith( word ) )
				{
					CompletionProposal proposal = new CompletionProposal( lastProposals[n].getDisplayString( ),
							offset - word.length( ),
							word.length( ),
							lastProposals[n].getDisplayString( ).length( ) );
					relevantProposals.add( proposal );
				}
			}

			if ( relevantProposals.size( ) > 0 )
			{
				return (ICompletionProposal[]) relevantProposals.toArray( new ICompletionProposal[]{} );
			}
		}

		return null;
	}

	/**
	 * @param columns
	 * @return
	 */
	private ICompletionProposal[] convertColumnsToCompletionProposals(
			ArrayList columns, int offset )
	{
		if ( columns.size( ) > 0 )
		{
			ICompletionProposal[] proposals = new ICompletionProposal[columns.size( )];
			Iterator iter = columns.iterator( );
			int n = 0;
			while ( iter.hasNext( ) )
			{
				Column column = (Column) iter.next( );
				proposals[n++] = new CompletionProposal( addQuotes( column.getName( ) ),
						offset,
						0,
						column.getName( ).length( ) );
			}
			return proposals;
		}
		return null;
	}

	/**
	 * @param tables
	 * @return
	 */
	private ICompletionProposal[] convertTablesToCompletionProposals(
			ArrayList tables, int offset )
	{
		if ( tables.size( ) > 0 )
		{
			ICompletionProposal[] proposals = new ICompletionProposal[tables.size( )];
			Iterator iter = tables.iterator( );
			int n = 0;
			while ( iter.hasNext( ) )
			{
				Table table = (Table) iter.next( );
				proposals[n++] = new CompletionProposal( addQuotes( table.getName( ) ),
						offset,
						0,
						table.getName( ).length( ) );
			}
			return proposals;
		}
		return null;
	}

	private String findWord( ITextViewer viewer, int offset )
			throws BadLocationException
	{
		//Check the character at the current position
		char ch = viewer.getDocument( ).getChar( offset );
		int startOffset = offset;
		if ( ch == '\'' || ch == '"' )//$NON-NLS-1$
		{
			startOffset--;
			char quoteChar = ch;
			//if the current character is a quote then we have to look till
			//the previous quote
			for ( ; startOffset > viewer.getTopIndexStartOffset( ); startOffset-- )
			{
				ch = viewer.getDocument( ).getChar( startOffset );
				if ( ch == quoteChar )
				{
					break;
				}
			}
		}
		else
		{
			//just raad until we encounter something that is not a character
			while ( startOffset > viewer.getTopIndexStartOffset( )
					&& Character.isLetterOrDigit( viewer.getDocument( )
							.getChar( startOffset ) ) )
			{
				startOffset--;
			}
			startOffset++;
		}

		return viewer.getDocument( )
				.get( startOffset, offset - startOffset + 1 );
	}

	/**
	 * @param viewer
	 * @param offset
	 * @return
	 */
	private ICompletionProposal[] getParameterCompletionProposals(
			ITextViewer viewer, int offset )
	{
		//Retrieve the list of parameters from the nandle
		PropertyHandle parameters = handle.getPropertyHandle( DataSet.INPUT_PARAMETERS_PROP );
		Iterator iter = parameters.iterator( );
		ArrayList items = new ArrayList( );
		while ( iter.hasNext( ) )
		{
			InputParameterHandle parameter = (InputParameterHandle) iter.next( );
			String name = parameter.getName( );

			CompletionProposal proposal = new CompletionProposal( name,
					offset,
					0,
					name.length( ) );
			items.add( proposal );
		}
		return (ICompletionProposal[]) items.toArray( new ICompletionProposal[]{} );
	}

	private String stripQuotes( String string )
	{
		if ( string.length( ) > 0 )
		{
			if ( ( string.charAt( 0 ) == '\'' || string.charAt( 0 ) == '"' )
					&& //$NON-NLS-1$
					( string.charAt( string.length( ) - 1 ) == '\'' || string.charAt( string.length( ) - 1 ) == '"' )//$NON-NLS-1$
			)
			{
				return string.substring( 1, string.length( ) - 1 );
			}

		}
		return string;
	}

	private String addQuotes( String string )
	{
		if ( string.indexOf( ' ' ) != -1 )
		{
			return "\"" + string + "\"";//$NON-NLS-1$
		}

		return string;
	}
}