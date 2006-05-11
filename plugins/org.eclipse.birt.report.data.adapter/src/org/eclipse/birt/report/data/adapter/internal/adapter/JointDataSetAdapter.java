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
package org.eclipse.birt.report.data.adapter.internal.adapter;

import java.util.Iterator;
import org.eclipse.birt.core.exception.BirtException;
import org.eclipse.birt.data.engine.api.IJoinCondition;
import org.eclipse.birt.data.engine.api.IJointDataSetDesign;
import org.eclipse.birt.data.engine.api.querydefn.JoinCondition;
import org.eclipse.birt.data.engine.api.querydefn.JointDataSetDesign;
import org.eclipse.birt.data.engine.api.querydefn.ScriptExpression;
import org.eclipse.birt.report.model.api.JoinConditionHandle;
import org.eclipse.birt.report.model.api.JointDataSetHandle;
import org.eclipse.birt.report.model.api.elements.DesignChoiceConstants;

/**
 * Adapts a Model joint data set handle
 */
public class JointDataSetAdapter extends JointDataSetDesign
{
	public JointDataSetAdapter( JointDataSetHandle handle ) throws BirtException
	{
		super( handle.getName( ));
		
		Iterator it = handle.joinConditionsIterator( );
	
		JoinConditionHandle jc = null;
		
		while( it.hasNext( ) )
		{
			jc = (JoinConditionHandle)it.next( );
			addJoinCondition( new JoinCondition( 
					new ScriptExpression( jc.getLeftExpression( )), 
					new ScriptExpression(jc.getRightExpression( )),
					adaptJoinOperator(jc.getOperator( )))) ;
		}
		
		if ( jc != null )
		{
			setLeftDataSetDesignName( jc.getLeftDataSet());
			setRightDataSetDesignName( jc.getRightDataSet());
			setJoinType( adaptJoinType(jc.getJoinType()) );
		}
		
		DataAdapterUtil.adaptBaseDataSet( handle, this );
	}

	/**
	 * Converts a Model join type (String) to Dte join type enumeration (int)
	 */
	public static int adaptJoinType ( String joinType )
	{
		if( joinType.equals( DesignChoiceConstants.JOIN_TYPE_INNER ))
		{
			return IJointDataSetDesign.INNER_JOIN;
		}
		else if( joinType.equals( DesignChoiceConstants.JOIN_TYPE_LEFT_OUT ))
		{
			return IJointDataSetDesign.LEFT_OUTER_JOIN;
		}
		else if( joinType.equals( DesignChoiceConstants.JOIN_TYPE_RIGHT_OUT ))
		{
			return IJointDataSetDesign.RIGHT_OUTER_JOIN;
		}
		return -1;
	}
	
	/**
	 * Converts a Model join operator (String) to DtE joint operator (int)
	 */
	public static int adaptJoinOperator( String operator )
	{
		if( operator.equals( DesignChoiceConstants.JOIN_OPERATOR_EQALS ))
			return IJoinCondition.OP_EQ;
		return -1;
	}

}
