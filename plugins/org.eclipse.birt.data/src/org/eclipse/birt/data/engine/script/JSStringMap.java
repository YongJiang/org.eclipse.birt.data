/*
 *************************************************************************
 * Copyright (c) 2004-2005 Actuate Corporation.
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
package org.eclipse.birt.data.engine.script;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

/**
 * Javascript wrapper of a Map object which maps either String to String, or
 * String to Set of Strings.
 * 
 * It is used to implement ROM script access to the public properties of 
 * extended data set and data source. 
 */
public class JSStringMap extends ScriptableObject
{
	 private Map map;
	 private boolean mapToSet;
	
	 private static Logger logger = Logger.getLogger( JSStringMap.class.getName( ) );

	 /**
	 * Constructor
	 * @param map Java Map to wrap
	 * @param mapToSet If true, map is a mapping from String to Set
	 */
	public JSStringMap( Map map, boolean mapToSet )
	{
		logger.entering( JSStringMap.class.getName( ), "JSStringMap" );
		this.map = map;
		this.mapToSet = mapToSet;
	}
	
	/**
	 * @see org.mozilla.javascript.Scriptable#getClassName()
	 */
	public String getClassName()
	{
		return "StringMap";
	}
	
	/**
	 * @see org.mozilla.javascript.Scriptable#delete(java.lang.String)
	 */
	public void delete(String name)
	{
		logger.entering( JSStringMap.class.getName( ), "delete", name );
		if ( map.containsKey( name ))
		{
			map.remove( name );
		}
		logger.exiting( JSStringMap.class.getName( ), "delete" );
	}
	
	/**
	 * @see org.mozilla.javascript.Scriptable#get(java.lang.String, org.mozilla.javascript.Scriptable)
	 */
	public Object get(String name, Scriptable start)
	{
		logger.entering( JSStringMap.class.getName( ), "get", name );
		if ( map.containsKey(name) )
		{
			Object result = map.get(name);
			if ( mapToSet && result != null )
			{
				// For now, JS Script can only access the first value of this property 
				// if it is duplicately defined
				// TODO: add support for multiple definitions of the same 
				// properties, perhaps return as array?
				assert result instanceof Set;
				Iterator it = ((Set) result).iterator();
				if ( it.hasNext() ){
					Object obj = it.next();
					logger.exiting( JSStringMap.class.getName( ), "get", obj );
					return obj;
				}
				else{
					logger.exiting( JSStringMap.class.getName( ), "get", null );
					return null;
				}
			}
			logger.exiting( JSStringMap.class.getName( ), "get", result );			
			return result;
		}
		else{
			if ( logger.isLoggable( Level.FINER ) )
				logger.exiting( JSStringMap.class.getName( ),
						"get",
						super.get( name, start ) );			
			return super.get(name, start);
		}
	}
	
	/**
	 * @see org.mozilla.javascript.Scriptable#getIds()
	 */
	public Object[] getIds()
	{
		return map.keySet().toArray( new String[0]);
	}
	
	/**
	 * @see org.mozilla.javascript.Scriptable#has(java.lang.String, org.mozilla.javascript.Scriptable)
	 */
	public boolean has(String name, Scriptable start)
	{
		logger.entering( JSStringMap.class.getName( ), "has", name );
		if ( logger.isLoggable( Level.FINER ) )
			logger.exiting( JSStringMap.class.getName( ),
					"has",
					new Boolean( map.containsKey( name ) ) );
		return map.containsKey(name);
	}
	
	/**
	 * @see org.mozilla.javascript.Scriptable#put(java.lang.String, org.mozilla.javascript.Scriptable, java.lang.Object)
	 */
	public void put(String name, Scriptable start, Object value)
	{
		logger.entering( JSStringMap.class.getName( ), "put", name );
		String valStr = value.toString();
		if ( mapToSet )
		{
			// For now, JS Script can only access the first value of this property 
			// if it is duplicately defined
			// TODO: allow multiple values to be assigned to the same property
			// perhaps by allowing value to be a JS array?
			HashSet valueSet = new HashSet();
			valueSet.add( valStr);
			map.put( name, valueSet );
		}
		else
		{
			map.put( name, valStr);
		}
		logger.exiting( JSStringMap.class.getName( ), "put" );
	}
}
