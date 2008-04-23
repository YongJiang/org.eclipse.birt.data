/*******************************************************************************
 * Copyright (c) 2004-2005 Actuate Corporation. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Eclipse
 * Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Actuate Corporation - initial API and implementation
 ******************************************************************************/

package org.eclipse.birt.report.data.oda.jdbc.ui.util;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Driver;
import java.util.Hashtable;
import java.util.List;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.eclipse.birt.report.data.oda.jdbc.ui.JdbcPlugin;
import org.eclipse.birt.report.data.oda.jdbc.ui.dialogs.JdbcDriverManagerDialog;
import org.eclipse.ui.PlatformUI;

public class JdbcToolKit
{
	// A list of JDBCDriverInformation objects
	private static List jdbcDriverInfos = null;
	// A list kept failure loaded driver files
	private static List failLoadFileList = null;

	// A map from driverClass (String) to JDBCDriverInformation
	private static HashMap driverNameMap = null;
	private static Hashtable file2Drivers = null;
	
	private static final Class DriverClass = Driver.class;

	/**
	 * Resets cached jdbc driver list to null, force reget the information when
	 * required next time.
	 */
	public static void resetJdbcDriverNames( )
	{
		jdbcDriverInfos = null;
		failLoadFileList = null;
		driverNameMap = null;
		file2Drivers = null;
	}
	 
	 /**
	  * Found drivers in the Jar file List
	  * @param file
	  * @return a List of JDBCDriverInformation
	  */
	public static void getJdbcDriverFromFile( List fileList )
	{
		if ( failLoadFileList != null )
		{
			boolean duplicated;
			for ( int i = 0; i < failLoadFileList.size( ); i++ )
			{
				duplicated = false;
				File failToLoadFile = (File) failLoadFileList.get( i );
				for ( int j = 0; j < fileList.size( ); j++ )
				{
					if ( failToLoadFile.getName( )
							.equals( ( (File) fileList.get( j ) ).getName( ) ) )
					{
						duplicated = true;
						break;
					}
				}
				if ( !duplicated )
					fileList.add( failToLoadFile );
			}
		}
		jdbcDriverInfos.addAll( getJDBCDriverInfoList( fileList ) );
	}

	/**
	 * Returns a List jdbc Drivers. The Drivers are searched from predefined
	 * directories in the DTE plug-in. Currently it is expected that the jdbc
	 * drivers are in the "drivers" directory of the DTE oda.jdbc plug-in.
	 * @param driverName
	 * @return
	 */
	public static List getJdbcDriversFromODADir( String driverName )
	{
		if ( jdbcDriverInfos != null )
		{
			// remove the forged driver if exists
			if ( !driverNameMap.containsValue( jdbcDriverInfos.get( 0 ) ) )
				jdbcDriverInfos.remove( 0 );

			if ( JdbcDriverManagerDialog.needResetPreferences( ) )
			{
				resetPreferences( );
				JdbcDriverManagerDialog.resetDriverChangedStatus( );
			}

			return jdbcDriverInfos;
		}
		
		jdbcDriverInfos = new ArrayList( );
		failLoadFileList = new ArrayList( );
		driverNameMap = new HashMap( );
		file2Drivers = new Hashtable( );

		// Get drivers from drivers subdirectory
		addDriversFromFiles( );

		final String ODBCJDBCDriverName = "sun.jdbc.odbc.JdbcOdbcDriver";
		JDBCDriverInformation ODBCJDBCInfo = null; 
		
		// Merge drivers from the driverInfo extension point
		JDBCDriverInformation driverInfos[] = JDBCDriverInfoManager.getDrivers();
		for (int i = 0; i < driverInfos.length; i++)
		{
			JDBCDriverInformation newInfo = driverInfos[i];
			// If driver already found in last step, update it; otherwise add new
			JDBCDriverInformation existing = 
					(JDBCDriverInformation)	driverNameMap.get( newInfo.getDriverClassName());
			if ( existing == null )
			{
				if ( newInfo.getDriverClassName( )
						.equalsIgnoreCase( ODBCJDBCDriverName ) )
				{
					ODBCJDBCInfo = newInfo;
					continue;
				}
				jdbcDriverInfos.add( newInfo );
				driverNameMap.put( newInfo.getDriverClassName(), newInfo);
			}
			else
			{
				existing.setDisplayName( newInfo.getDisplayName());
				existing.setUrlFormat( newInfo.getUrlFormat());
			}
		}
		
		// Put ODBC-JDBC driver to the last position of list
		if ( ODBCJDBCInfo != null )
		{
			jdbcDriverInfos.add( ODBCJDBCInfo );
			driverNameMap.put( ODBCJDBCInfo.getDriverClassName( ), ODBCJDBCInfo );
		}
		
		resetPreferences( );
		
		return jdbcDriverInfos;
	}

	/**
	 * Read user setting from the preference store and update
	 * 
	 */
	private static void resetPreferences( )
	{
		Map preferenceMap = Utility.getPreferenceStoredMap( JdbcPlugin.DRIVER_MAP_PREFERENCE_KEY );

		for ( Iterator itr = jdbcDriverInfos.iterator( ); itr.hasNext( ); )
		{
			JDBCDriverInformation info = (JDBCDriverInformation) itr.next( );
				
			Object ob = preferenceMap.get( info.toString( ) );
			if ( ob != null )
			{
				DriverInfo driverInfo = (DriverInfo) ob;
				if ( driverInfo.getDisplayName( ) != null
						&& driverInfo.getDisplayName( ).length( ) >= 0 )
				{
					info.setDisplayName( driverInfo.getDisplayName( ) );
				}
				if ( driverInfo.getUrlTemplate( ) != null
						&& driverInfo.getUrlTemplate( ).length( ) >= 0 )
				{
					info.setUrlFormat( driverInfo.getUrlTemplate( ) );
				}
			}
		}
	}

	/**
	 * Get a List of JDBCDriverInformations loaded from the given fileList
	 * @param fileList the File List
	 * @param urlClassLoader
	 * @return List of JDBCDriverInformation
	 */
	private static List getJDBCDriverInfoList( List fileList )
	{
		List driverList = new ArrayList( );
		for ( int i = 0; i < fileList.size( ); i++ )
		{
			URLClassLoader urlClassLoader = createClassLoader( (File) fileList.get( i ) );
			String[] resourceNames = getAllResouceNames( (File) fileList.get( i ) );
			List subDriverList = new ArrayList( );
			for ( int j = 0; j < resourceNames.length; j++ )
			{
				String resourceName = resourceNames[j];
				if ( resourceName.endsWith( ".class" ) ) //$NON-NLS-1$
				{
					resourceName = modifyResourceName( resourceName );

					Class aClass = loadClass( urlClassLoader, resourceName );

					// Do not add it, if it is a Abstract class
					if ( isImplementedDriver( aClass ) )
					{
						JDBCDriverInformation info = JDBCDriverInformation.newInstance( aClass );
						if ( info != null )
						{
							driverList.add( info );
							subDriverList.add( info );
						}
					}
				}
			}
			if ( subDriverList.isEmpty( ) )
			{
				if ( !failLoadFileList.contains( fileList.get( i ) ) )
					failLoadFileList.add( fileList.get( i ) );
			}
			else
			{
				if ( failLoadFileList.contains( fileList.get( i ) ) )
					failLoadFileList.remove( fileList.get( i ) );
			}
			file2Drivers.put( ( (File) fileList.get( i ) ).getName( ),
					subDriverList );
		}
		return driverList;
	}

	/**
	 * add new found driver(s) to runtime driver list
	 * @param fileList
	 */
	public static void addToDriverList( List fileList )
	{
		if ( fileList != null && fileList.size( ) != 0 )
			getJdbcDriverFromFile( fileList );
	}

	/**
	 * remove driver(s) from runtime driver list
	 * @param fileList
	 */
	public static void removeFromDriverList( List fileList )
	{
		for ( int i = 0; i < fileList.size( ); i++ )
		{
			String fileName = ( (File) fileList.get( i ) ).getName( );

			List driverNames = (List) file2Drivers.get( fileName );
			for ( int j = 0; j < jdbcDriverInfos.size( ); j++ )
			{
				for ( int k = 0; k < driverNames.size( ); k++ )
					if ( ( (JDBCDriverInformation) jdbcDriverInfos.get( j ) ).getDriverClassName( )
							.equals( ( (JDBCDriverInformation) driverNames.get( k ) ).getDriverClassName( ) ) )
					{
						jdbcDriverInfos.remove( j );
					}
			}
			if ( failLoadFileList.contains( fileList.get( i ) ) )
				failLoadFileList.remove( fileList.get( i ) );
		}
	}

	/**
	 * 
	 * @return
	 */
	public static List getDriverList( )
	{
		return jdbcDriverInfos;
	}
	
	/**
	 * modify resourceName,prepare for loadClass()
	 * @param resourceName
	 * @return
	 */
	private static String modifyResourceName( String resourceName )
	{
		resourceName = ( resourceName.replaceAll( "/", "." ) ).substring( 0,resourceName.length( ) - 6 );
		return resourceName;
	}
	
	/**
	 * Get the corresponding drivers list by the given JarFile
	 * 
	 * @param JarFile jar
	 * @return List that contains the corresponding drivers
	 */
	public static List getDriverByJar( JarFile jar )
	{
		List drivers = null;
		if ( jar == null )
			return drivers;

		List jarList = new ArrayList( 1 );
		jarList.add( new File( jar.getFilePath( ) ) );
		drivers = getJDBCDriverInfoList( jarList );
		return drivers;
	}
	
	/**
	 * Search files under "drivers" directory for JDBC drivers. Found drivers are added
	 * to jdbdDriverInfos as JDBCDriverInformation instances
	 */
	private static void addDriversFromFiles( )
	{
		List jdbcDriverFiles = JdbcDriverConfigUtil.getDriverFiles( );
		if ( jdbcDriverFiles == null || jdbcDriverFiles.size( ) == 0 )
			return;

		List driverList = getJDBCDriverInfoList( jdbcDriverFiles );
		jdbcDriverInfos.addAll( driverList );
		for ( int i = 0; i < driverList.size( ); i++ )
		{
			JDBCDriverInformation info = (JDBCDriverInformation) driverList.get( i );
			driverNameMap.put( info.getDriverClassName( ), info );
		}
	}

	/**
	 * Create a URLClassLoader based on the given file list
	 * @param jdbcDriverFiles a File List
	 * @return URLClassLoader
	 */
	private static URLClassLoader createClassLoader( File jdbcDriverFile )
	{
		// Create a URL Array for the class loader to use
		URL[] urlList = new URL[1];
			try
			{
				urlList[0] = new URL( "file", null, jdbcDriverFile.getAbsolutePath( ) );
			}
			catch ( MalformedURLException e )
			{
				ExceptionHandler.showException( PlatformUI.getWorkbench( )
						.getDisplay( )
						.getActiveShell( ),
						JdbcPlugin.getResourceString( "exceptionHandler.title.error" ),
						e.getLocalizedMessage( ),
						e );

			}
		URLClassLoader urlClassLoader = new URLClassLoader( urlList,
					ClassLoader.getSystemClassLoader( ) );
		return urlClassLoader;
	}

	/**
	 * Load a Class using the given ClassLoader
	 * @param urlClassLoader
	 * @param resourceName
	 * @return
	 */
	private static Class loadClass( URLClassLoader urlClassLoader, String resourceName )
	{
		Class aClass = null;
		try
		{
			aClass = urlClassLoader.loadClass( resourceName );
		}
		catch ( Throwable e )
		{
			// here throwable is used to catch exception and error
		}
		return aClass;
	}

	/**
	 * Check whether the given class implemented <tt>java.sql.Driver</tt> 
	 * @param aClass the class to be checked
	 * @return <tt>true</tt> if <tt>aClass</tt> implemented
	 *         <tt>java.sql.Driver</tt>,else <tt>false</tt>;
	 */
	private static boolean isImplementedDriver( Class aClass )
	{
		return aClass != null && implementsSQLDriverClass( aClass ) &&
				! Modifier.isAbstract( aClass.getModifiers( ));
	}
	
	/**
	 * Get all resources included in a jar file
	 * @param jarFile
	 * @return
	 */
	private static String[] getAllResouceNames( File jarFile )
	{
		ArrayList jarEntries = new ArrayList( );
		try
		{
			ZipFile zf = new ZipFile( jarFile );
			Enumeration e = zf.entries( );
			while ( e.hasMoreElements( ) )
			{
				ZipEntry ze = (ZipEntry) e.nextElement( );
				if ( !ze.isDirectory( ) )
				{
					jarEntries.add( ze.getName( ) );
				}
			}
			zf.close( );
		}
		catch ( IOException e1 )
		{
		}
		catch ( Exception e )
		{
			ExceptionHandler.showException( PlatformUI.getWorkbench( )
					.getDisplay( )
					.getActiveShell( ),
					JdbcPlugin.getResourceString( "exceptionHandler.title.error" ),
					e.getLocalizedMessage( ),
					e );

		}
		return (String[]) jarEntries.toArray( new String[jarEntries.size( )] );
	}

	/**
	 * Determine aClass implements java.sql.Driver interface
	 * @param aClass
	 * @return
	 */
	private static boolean implementsSQLDriverClass( Class aClass )
	{
		if ( DriverClass.isAssignableFrom( aClass ) )
		{
			return true;
		}
		return false;
	}

}