/*******************************************************************************
 * Copyright (c) 2008 Actuate Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Actuate Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.birt.report.data.oda.jdbc.dbprofile.ui.internal.sqb;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.birt.report.data.oda.jdbc.dbprofile.ui.nls.Messages;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.datatools.connectivity.IConnectionProfile;
import org.eclipse.datatools.connectivity.internal.ui.dialogs.ExceptionHandler;
import org.eclipse.datatools.connectivity.oda.OdaException;
import org.eclipse.datatools.connectivity.oda.design.DataSetDesign;
import org.eclipse.datatools.connectivity.oda.design.DesignFactory;
import org.eclipse.datatools.connectivity.oda.design.DesignerState;
import org.eclipse.datatools.connectivity.oda.design.ui.designsession.DesignSessionUtil;
import org.eclipse.datatools.connectivity.oda.design.ui.wizards.DataSetWizardPage;
import org.eclipse.datatools.connectivity.oda.profile.OdaProfileExplorer;
import org.eclipse.datatools.modelbase.sql.query.helper.StatementHelper;
import org.eclipse.datatools.sqltools.editor.core.connection.ISQLEditorConnectionInfo;
import org.eclipse.datatools.sqltools.sqlbuilder.input.ISQLBuilderEditorInput;
import org.eclipse.datatools.sqltools.sqlbuilder.input.ISQLBuilderEditorInputUsageOptions;
import org.eclipse.datatools.sqltools.sqlbuilder.input.SQLBuilderEditorInputUsageOptions;
import org.eclipse.datatools.sqltools.sqlbuilder.input.SQLBuilderStorageEditorInput;
import org.eclipse.datatools.sqltools.sqlbuilder.model.ControlStateInfo;
import org.eclipse.datatools.sqltools.sqlbuilder.model.IControlStateInfo;
import org.eclipse.datatools.sqltools.sqlbuilder.model.IWindowStateInfo;
import org.eclipse.datatools.sqltools.sqlbuilder.model.SQLBuilderConnectionInfo;
import org.eclipse.datatools.sqltools.sqlbuilder.model.WindowStateInfo;
import org.eclipse.datatools.sqltools.sqlbuilder.util.SQLBuilderEditorInputUtil;
import org.eclipse.datatools.sqltools.sqleditor.SQLEditorStorage;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

public class SQBDataSetWizardPage extends DataSetWizardPage
{
    private static final String SQB_STATE_CURRENT_VERSION = "1.0"; //$NON-NLS-1$
    private static final String NEWLINE_CHAR = "\n"; //$NON-NLS-1$
    private static final String EMPTY_STR = ""; //$NON-NLS-1$
    
    private IConnectionProfile m_dataSourceProfile;
    private ISQLBuilderEditorInput m_sqbInput;    
	private CustomSQLBuilderDialog m_sqbDialog;
	private boolean m_updatedQueryInput = false;

	public SQBDataSetWizardPage( String pageName )
	{
		super( pageName );	
	}    

	private IConnectionProfile getConnectionProfile( boolean raiseErrorIfNull )
	{
	    if( m_dataSourceProfile == null )
	    {
            java.util.Properties connProps = null;
            String exceptionMessage = EMPTY_STR;
            try
            {
                connProps = DesignSessionUtil.getEffectiveDataSourceProperties( 
                                getEditingDesign().getDataSourceDesign() );
            }
            catch( OdaException ex )
            {
                exceptionMessage = ex.getLocalizedMessage();
            }

            m_dataSourceProfile = OdaProfileExplorer.getInstance()
               .getProfileByName( connProps, null );
            
            if( m_dataSourceProfile == null && raiseErrorIfNull )
                MessageDialog.openError( getShell(), Messages.sqbWizPage_dataSourceDesignError, 
                        Messages.sqbWizPage_noConnProfileMsg + NEWLINE_CHAR + exceptionMessage );
	    }

	    return m_dataSourceProfile;
	}
    
    private String getDataSetDesignName()
    {
        return getEditingDesign().getName();
    }
	
	private String getDataSetDesignQueryText()
	{
	    return getEditingDesign().getQueryText();
	}
	
	/*
	 * @see org.eclipse.datatools.connectivity.oda.design.ui.wizards.DataSetWizardPage#createPageCustomControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createPageCustomControl( Composite parent )
	{
        IConnectionProfile connProfile = getConnectionProfile( true );
        if( connProfile == null )
            return;
        
		m_sqbInput = createSQBInput( parent, connProfile );
		setControl( createSQBControl( parent, m_sqbInput ) );
	}

	private ISQLBuilderEditorInput createSQBInput( Composite parent, IConnectionProfile connProfile )
	{
	    m_updatedQueryInput = false;
	    SQLBuilderStorageEditorInput sqbInput = restoreSQLBuilderStateFromDesign( parent.getShell() );
	    if( sqbInput == null )
	    {
	        // create a default input with empty SQL statement
	        return new DefaultSQBInput( connProfile );
	    }

        // replace restored storage with input's query text (to workaround problem with
	    // position of storage content stream);
        // if restored input has a different SQL statement, first ask user if ok to replace 
	    // with data set design's query text
        String sqlText = sqbInput.exists() ? sqbInput.getSQL() : EMPTY_STR;
        if( sqlText.length() > 0 &&
            StatementHelper.compareSQL( sqlText, getDataSetDesignQueryText() ) != 0 )
        {
            if( openReplaceSQLMessageBox( parent.getShell() ) )
            {
                sqlText = getDataSetDesignQueryText();
                m_updatedQueryInput = true;
            }
        }
        sqbInput.setStorage( new SQLEditorStorage( getDataSetDesignName(), sqlText ));
        
        
        // replace with latest data set design name
        sqbInput.setName( getDataSetDesignName() );
        
        // replace with specified connection profile
        sqbInput.setConnectionInfo( new SQLBuilderConnectionInfo( connProfile ) );
        
        // override option to always use window state
        setUseWindowState( sqbInput, true );

        return sqbInput;	    
	}

	private SQLBuilderStorageEditorInput restoreSQLBuilderStateFromDesign( Shell parentShell )
	{
	    DesignerState designerState = getInitializationDesignerState();
	    if( designerState == null || designerState.getStateContent() == null )
	        return null;
	    
	    // check the version compatibility of designerState
	    String designStateVersion = designerState.getVersion();
	    if( designStateVersion == null || 
	        ! designStateVersion.equals( SQB_STATE_CURRENT_VERSION ) ) // supports only one version
	    {
	        openInvalidInputMessageBox( parentShell, false );
	        return null;
	    }
	    
	    // get the designer state content
	    String designStateValue = designerState.getStateContent().getStateContentAsString();
	    if( designStateValue == null )
            return null;

	    return SQLBuilderEditorInputUtil.createSQLBuilderStorageEditorInput( designStateValue );
	}
	
    private static void setUseWindowState( SQLBuilderStorageEditorInput sqbInput, 
            boolean useWindowState )
    {       
        ISQLBuilderEditorInputUsageOptions usageOption = sqbInput.getInputUsageOptions();
        if( usageOption != null )
        {
            usageOption.setUseWindowState( useWindowState );
            return;     // done
        }
        
        usageOption = new SQLBuilderEditorInputUsageOptions( useWindowState );
        sqbInput.setInputUsageOptions( usageOption );
    }
    
	private Control createSQBControl( Composite parent, ISQLBuilderEditorInput sqbInput )
	{
		Composite pageContainer = new Composite( parent, SWT.NONE );
		GridLayout layout = new GridLayout( );
		layout.numColumns = 3;
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		layout.horizontalSpacing = 2;
		pageContainer.setLayout( layout );
		pageContainer.setLayoutData( new GridData( GridData.FILL_BOTH ) );
		 
		// initiate connect before setting input for better error handling
		Shell parentShell = parent.getShell();
		boolean isConnected = connect( parentShell, sqbInput );
		if( ! isConnected )
		{
	        setPageComplete( false );
            return pageContainer; // returns empty composite
		}
		
        m_sqbDialog = new CustomSQLBuilderDialog( parentShell );        
		boolean isInputLoaded = m_sqbDialog.setInput( sqbInput );
		if( ! isInputLoaded && ! (sqbInput instanceof DefaultSQBInput ) )
		{
		    // raise user message that the preserved state is no longer valid
		    if( openInvalidInputMessageBox( parentShell, true ) )
		    {
    		    // substitute with default input instead, re-using same connection profile
    		    isInputLoaded = m_sqbDialog.setInput( 
		                        new DefaultSQBInput( sqbInput.getConnectionInfo() ) );
    		    m_updatedQueryInput = true;
		    }
		}

		if( isInputLoaded )
		{
		    m_sqbDialog.createDialogArea( pageContainer );
		    if( m_updatedQueryInput )
		        m_sqbDialog.setDirty( true );
		}
		else
		    m_sqbDialog = null;

		setPageComplete( isInputLoaded );
		return pageContainer;
	}

	/**
	 * Attempts to connect based on the specified ISQLBuilderEditorInput's connection info.
	 * If connection fails, raise an error message dialog with the connection failure messages.
	 * @return true if connect succeeds; false otherwise
	 */
    private boolean connect( Shell parentShell, ISQLBuilderEditorInput sqbInput )
    {
        IConnectionProfile connProfile = sqbInput.getConnectionInfo().getConnectionProfile();
		if( connProfile.supportsWorkOfflineMode() && connProfile.canWorkOffline() )
		    return true;

		return runConnect( connProfile, parentShell ); 		
    }

    /**
     * Connect to database in a runnable with progress bar dialog.
     * @param connProfile
     * @param parentShell
     */
    private boolean runConnect( final IConnectionProfile connProfile, final Shell parentShell )
	{
		IRunnableWithProgress runnable = new IRunnableWithProgress( ) 
		{
			public void run( IProgressMonitor monitor )
					throws InvocationTargetException, InterruptedException
			{
				monitor.beginTask( Messages.sqbWizPage_connectingDB, IProgressMonitor.UNKNOWN );
				// TODO - this connect task cannot be cancelled; replace with a cancelable one
				IStatus status = connProfile.connectWithoutJob( );
				monitor.done( );
				
				if( status == null || ! status.isOK() )
				    throw new InvocationTargetException( getStatusException( status ) );
			}
		};

		try
		{
			new ProgressMonitorDialog( parentShell ) {}
			    .run( true, false, runnable );
		}
		catch ( InvocationTargetException e )
		{
		    raiseConnectionErrorMessage( parentShell, e );
			return false;
		}
		catch ( InterruptedException e )
		{
            raiseConnectionErrorMessage( parentShell, e );
            return false;
		}
		
		return true;
	}
    
    /**
     * For use with async connect job.
     */
    private class ConnectionJobListener implements IJobChangeListener
    {
        private Shell m_parentShell;
        
        private ConnectionJobListener( Shell parentShell )
        {
            m_parentShell = parentShell;
        }
        
        /*
         * (non-Javadoc)
         * @see org.eclipse.core.runtime.jobs.IJobChangeListener#done(org.eclipse.core.runtime.jobs.IJobChangeEvent)
         */
        public void done( IJobChangeEvent event ) 
        {
            IStatus connectStatus = event.getResult();
            done( connectStatus );
        }
        
        /**
         * Format and raise an user message if the specified status is not OK.
         * @param connectStatus
         */
        void done( IStatus connectStatus ) 
        {
            if( connectStatus != null && connectStatus.isOK() )
                return;
            
            // failed to connect, raise error message dialog
            raiseConnectionErrorMessage( m_parentShell, getStatusException( connectStatus ) );
        }

        // ignored events
        public void aboutToRun( IJobChangeEvent event ) {} 

        public void awake( IJobChangeEvent event ) {} 

        public void running( IJobChangeEvent event ) {}  

        public void scheduled( IJobChangeEvent event ) {}  

        public void sleeping( IJobChangeEvent event ) {} 
    }

    /**
     * Collects the first exception from the specified status.
     * @param status    may be null
     */
    private static Throwable getStatusException( IStatus status )
    {
        if( status == null )
            return null;
        Throwable ex = status.getException( );
        if( ex != null )
            return ex;

        // find first exception from its children
        IStatus[] childrenStatus = status.getChildren();
        for( int i=0; i < childrenStatus.length && ex == null; i++ )
        {
            ex = childrenStatus[i].getException( );
        }
        return ex;
    }
    
    /**
     * Raises an error message dialog associated with the given parent shell and displays the
     * error messages from the given exception.
     * @param parentShell
     * @param connectException  may be null
     */
    private static void raiseConnectionErrorMessage( Shell parentShell, Throwable connectException )
    {
        String errorMessage = Messages.sqbWizPage_cannotOpenConnectionMsg;
        
        if( connectException != null )
        {
            String dbMessage = connectException.getMessage();
            if( dbMessage != null )
                errorMessage += NEWLINE_CHAR + Messages.sqbWizPage_dbErrorMsg + dbMessage;
        }
        
        ExceptionHandler.showException( parentShell, 
                Messages.sqbWizPage_cannotOpenConnectionTitle, errorMessage, connectException );
    }
    
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.datatools.connectivity.oda.design.ui.wizards.DataSetWizardPage#collectResponseState()
	 */
    protected void collectResponseState()
    {
        if ( getControl() == null || getControl().isDisposed() || m_sqbDialog == null )
            return;

        super.collectResponseState();
        
        String sqbState = m_sqbDialog.saveSQBState( getDataSetDesignName() );
        if( sqbState != null && sqbState.length() > 0 )
        {
            DesignerState designerState = DesignFactory.eINSTANCE.createDesignerState();
            designerState.setNewStateContentAsString( sqbState );
            designerState.setVersion( SQB_STATE_CURRENT_VERSION );
            
            setResponseDesignerState( designerState );
        }
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.datatools.connectivity.oda.design.ui.wizards.DataSetWizardPage#collectDataSetDesign(org.eclipse.datatools.connectivity.oda.design.DataSetDesign)
     */
	protected DataSetDesign collectDataSetDesign( DataSetDesign design )
	{	
        if ( getControl() == null || getControl().isDisposed() || m_sqbDialog == null )
	        return design;
		
	    // saves query and its metadata in DataSetDesign
        if( m_sqbDialog.isDirty() )
        {
            SQLQueryUtility.updateDataSetDesign( design, m_sqbDialog.getSQLQueryStatement(),
                                            getConnectionProfile( false ) );
            m_sqbDialog.setDirty( false );
        }

		return design;
	}

    /* (non-Javadoc)
     * @see org.eclipse.datatools.connectivity.oda.design.ui.wizards.DataSetWizardPage#cleanup()
     */
    protected void cleanup()
    {
         IConnectionProfile connProfile = getConnectionProfile( false );
        if( connProfile != null && connProfile.getConnectionState() == IConnectionProfile.CONNECTED_STATE )
            connProfile.disconnect( null );
        
        if( m_sqbDialog != null )
        {
            m_sqbDialog.close();
            m_sqbDialog = null;
        }
    }

    private static boolean openInvalidInputMessageBox( Shell parentShell, boolean askUser )
    {
        String userMessage = Messages.sqbWizPage_invalidSqbStateMsg;
        if( askUser )
        {
            userMessage += NEWLINE_CHAR
                            + Messages.sqbWizPage_inputFailOnOpenAskUserMessage;
            return MessageDialog.openQuestion( parentShell,
                        Messages.sqbWizPage_invalidSqbStateTitle, userMessage );
        }
        
        // not an user option, raise warning and continue
        MessageDialog.openWarning( parentShell,
                        Messages.sqbWizPage_invalidSqbStateTitle, userMessage );
        return true; // continue
    }

    private static boolean openReplaceSQLMessageBox( Shell parentShell )
    {
        return MessageDialog.openQuestion( parentShell, 
                Messages.sqbWizPage_detectSqlTextChangedTitle,
                Messages.sqbWizPage_detectExternalSqlTextChangedMsg );
    }
    
	/**
	 * Internal class for a default SQB Input used when no existing input is available.
	 */
    private class DefaultSQBInput extends SQLBuilderStorageEditorInput
    {
        private static final int SQBCONTROL_HEIGHT = 400;

		DefaultSQBInput( IConnectionProfile connProfile )
        {
            this( new SQLBuilderConnectionInfo( connProfile ) );   
        }
        
        DefaultSQBInput( ISQLEditorConnectionInfo connInfo )
        {
            super( getDataSetDesignName(), getDataSetDesignQueryText() );   
            init( connInfo );
        }
        
        private void init( ISQLEditorConnectionInfo connInfo )
        {
            setConnectionInfo( connInfo );
            setOmitSchemaInfo( null );  // use setting from SQB preference page
            setWindowStateInfo( createDefaultWindowState() );
            setUseWindowState( this, true );
        }
        
        private IWindowStateInfo createDefaultWindowState()
        {
            // hide Outline control by default
            IControlStateInfo outlineControlState = 
                new ControlStateInfo( IControlStateInfo.OUTLINE_CONTROL );
            outlineControlState.setIsVisible( false );

            IWindowStateInfo windowState = new WindowStateInfo();
            windowState.put( outlineControlState.getControlType(), outlineControlState );
           
	        windowState.setHeight( SQBCONTROL_HEIGHT );
            return windowState;
        }
    };

}