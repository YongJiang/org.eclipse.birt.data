/*
 *******************************************************************************
 * Copyright (c) 2004, 2005 Actuate Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Actuate Corporation  - initial API and implementation
 *******************************************************************************
 */

package org.eclipse.birt.core.script.function.i18n;

import java.text.MessageFormat;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Resource messages wrapper for the package to obtain localized message text.
 */

public class Messages
{
    private static final String BUNDLE_NAME = "org.eclipse.birt.core.script.function.i18n.messages";//$NON-NLS-1$

    public static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle
            .getBundle( BUNDLE_NAME );

    private Messages()
    {
    }

    public static String getString( String key )
    {
        try
        {
            return RESOURCE_BUNDLE.getString( key );
        }
        catch( MissingResourceException e )
        {
            return '!' + key + '!';
        }
    }
    
    public static String getFormattedString( String key, Object[] arguments )
    {
        return MessageFormat.format( getString( key ), arguments );
    }

}