/*
 *************************************************************************
 * Copyright (c) 2004 Actuate Corporation.
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
package org.eclipse.birt.data.engine.api;

/**
 * Base type to represent a generic data expression used in a report design. An expression has
 * an optional return data type. Each expression can also be associated with a handle, which
 * is used by the Data Engine to store the compiled evaluation plan for the expression.
 */
public interface IBaseExpression
{
    /**
     * Gets the data type of the expression. Acceptable return values are those enumeration constants
     * defined in the DataType class. If the result data type of the expression is not known,
     * return DataType.UNKNOWN_TYPE.
     * @see DataType
     */
    public abstract int getDataType();

    /**
     * Returns the handle associated with the expression.   
     * 
     * @return the expression execution handle.
     */
    public abstract Object getHandle();

    /**
     * Associates the expression with the provided handle.   
     */
    public abstract void setHandle( Object handle );

}
