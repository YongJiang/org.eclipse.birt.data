/*******************************************************************************
 * Copyright (c) 2004, 2005 Actuate Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Actuate Corporation  - initial API and implementation
 *******************************************************************************/

package org.eclipse.birt.core.script.function.bre;

import java.sql.Timestamp;
import java.util.Date;

import org.eclipse.birt.core.data.DataTypeUtil;
import org.eclipse.birt.core.exception.BirtException;
import org.eclipse.birt.core.script.function.i18n.Messages;
import org.eclipse.birt.core.script.functionservice.IScriptFunctionContext;
import org.eclipse.birt.core.script.functionservice.IScriptFunctionExecutor;

import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.TimeZone;
import com.ibm.icu.util.ULocale;

/**
 * 
 */
public class BirtDateTime implements IScriptFunctionExecutor
{
	//TODO Change these values according to the locale.
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	static private SimpleDateFormat abbrMonthFormat = null;
	static private SimpleDateFormat monthFormat = null;
	static private SimpleDateFormat abbrWeekFormat = null;
	static private SimpleDateFormat weekFormat = null;
	
	private IScriptFunctionExecutor executor;
	
	private static IScriptFunctionContext scriptContext;
	private static ULocale defaultLocale = null;
	private static TimeZone timeZone = null;
	
	/**
	 * 
	 * @return
	 */
	private static SimpleDateFormat getAbbrMonthFormat( )
	{
		if ( abbrMonthFormat == null )
			abbrMonthFormat = new SimpleDateFormat( "MMM", defaultLocale );
		return abbrMonthFormat;
	}
	
	/**
	 * 
	 * @return
	 */
	private static SimpleDateFormat getMonthFormat( )
	{
		if ( monthFormat == null )
			monthFormat = new SimpleDateFormat( "MMMM", defaultLocale );
		return monthFormat;
	}
	
	/**
	 * 
	 * @return
	 */
	private static SimpleDateFormat getAbbrWeekFormat( )
	{
		if ( abbrWeekFormat == null )
			abbrWeekFormat = new SimpleDateFormat( "EEE", defaultLocale );
		return abbrWeekFormat;
	}
	
	/**
	 * 
	 * @return
	 */
	private static SimpleDateFormat getWeekFormat( )
	{
		if ( weekFormat == null )
			weekFormat = new SimpleDateFormat( "EEEE", defaultLocale );
		return weekFormat;
	}

	/**
	 * @throws BirtException 
	 * 
	 *
	 */
	BirtDateTime( String functionName ) throws BirtException
	{
		if( "year".equals( functionName ))
			this.executor = new Function_Year( );
		else if( "quarter".equals( functionName ))
			this.executor =  new Function_Quarter( );
		else if( "month".equals( functionName ))
			this.executor =  new Function_Month( );
		else if( "week".equals( functionName ))
			this.executor = new Function_Week( );
		else if( "day".equals( functionName ))
			this.executor = new Function_Day( );
		else if( "weekDay".equals( functionName ))
			this.executor = new Function_WeekDay( );
		else if( "today".equals( functionName ))
			this.executor = new Function_Today( );
		else if( "now".equals( functionName ))
			this.executor = new Function_Now( );
		else if( "diffYear".equals( functionName ))
			this.executor = new Function_DiffYear( );
		else if( "diffMonth".equals( functionName ))
			this.executor = new Function_DiffMonth( );
		else if( "diffQuarter".equals( functionName ))
			this.executor = new Function_DiffQuarter( );
		else if( "diffWeek".equals( functionName ))
			this.executor = new Function_DiffWeek( );
		else if( "diffDay".equals( functionName ))
			this.executor = new Function_DiffDay( );
		else if( "diffHour".equals( functionName ))
			this.executor = new Function_DiffHour( );
		else if( "diffMinute".equals( functionName ))
			this.executor = new Function_DiffMinute( );
		else if( "diffSecond".equals( functionName ))
			this.executor = new Function_DiffSecond( );
		else if( "addYear".equals( functionName ))
			this.executor = new Function_AddYear( );
		else if( "addMonth".equals( functionName ))
			this.executor = new Function_AddMonth( );
		else if( "addQuarter".equals( functionName ))
			this.executor = new Function_AddQuarter( );
		else if( "addWeek".equals( functionName ))
			this.executor = new Function_AddWeek( );
		else if( "addDay".equals( functionName ))
			this.executor = new Function_AddDay( );
		else if( "addHour".equals( functionName ))
			this.executor = new Function_AddHour( );
		else if( "addMinute".equals( functionName ))
			this.executor = new Function_AddMinute( );
		else if( "addSecond".equals( functionName ))
			this.executor = new Function_AddSecond( );
		else
			throw new BirtException( "org.eclipse.birt.core.script.function.bre",
					null,
					Messages.getString( "invalid.function.name" )
							+ "BirtDateTime." + functionName );	}

	private static class Function_Year extends Function_temp
	{
		Function_Year()
		{
			minParamCount = 1;
			maxParamCount = 1;
		}
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		protected Object getValue( Object[] args ) throws BirtException
		{
			if( existNullValue( args ) )
			{
				return null;
			}
			return Integer.valueOf( year(DataTypeUtil.toDate(args[0])));
		}
	}		
	
	/**
	 * 4-digit year number of date/time value d
	 * 
	 * @param d
	 * @return
	 */
	private static int year( Date d )
	{
		if ( d == null )
			throw new java.lang.IllegalArgumentException( "date value is null!" );

		return getCalendar( d ).get( Calendar.YEAR );
	}

	private static class Function_Quarter extends Function_temp
	{
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		Function_Quarter( )
		{
			minParamCount = 1;
			maxParamCount = 1;
		}
		
		protected Object getValue( Object[] args ) throws BirtException
		{
			if( existNullValue( args ) )
			{
				return null;
			}
			return Integer.valueOf( quarter(DataTypeUtil.toDate(args[0])));
		}
	}		
	
	/**
	 * Quarter number (1 to 4) of date/time value d
	 * 
	 * @param d
	 * @return
	 */
	private static int quarter( Date d )
	{
		if ( d == null )
			throw new java.lang.IllegalArgumentException( Messages.getString( "error.BirtDateTime.cannotBeNull.DateValue" ) );

		int month = getCalendar( d ).get( Calendar.MONTH );
		switch ( month )
		{
			case Calendar.JANUARY :
			case Calendar.FEBRUARY :
			case Calendar.MARCH :
				return 1;
			case Calendar.APRIL :
			case Calendar.MAY :
			case Calendar.JUNE :
				return 2;
			case Calendar.JULY :
			case Calendar.AUGUST :
			case Calendar.SEPTEMBER :
				return 3;
			case Calendar.OCTOBER :
			case Calendar.NOVEMBER :
			case Calendar.DECEMBER :
				return 4;
			default :
				return -1;
		}
	}

	private static class Function_Month extends Function_temp
	{
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		Function_Month()
		{
			minParamCount = 1;
			maxParamCount = 2;
		}
		
		protected Object getValue( Object[] args ) throws BirtException
		{
			if( existNullValue( args ) )
			{
				return null;
			}
			if( args.length == 1 )
				return Integer.valueOf( month(DataTypeUtil.toDate(args[0])));
			else
				return month(DataTypeUtil.toDate(args[0]), ((Number)args[1]).intValue( ));
		}
	}	
	/**
	 * Month of date/time value d. Return month number (1-12)
	 * 
	 * @param d
	 * @return
	 */
	private static int month( Date d )
	{
		if ( d == null )
			throw new java.lang.IllegalArgumentException( Messages.getString( "error.BirtDateTime.cannotBeNull.DateValue" ) );

		return getCalendar( d ).get( Calendar.MONTH ) + 1;
	}

	/**
	 * Month of date/time value d. Option is an integer value: 1 (default):
	 * return month number (1-12) 2: return full month name as per user locale
	 * (e.g., January to December for English locale). 3: return short month
	 * name as per user locale (e.g., Jan to Dec for English locale)
	 * 
	 * @param d
	 * @param option
	 * @return
	 */
	private static String month( Date d, int option )
	{
		// TODO: finish me.
		if ( d == null )
			throw new java.lang.IllegalArgumentException( Messages.getString( "error.BirtDateTime.cannotBeNull.DateValue" ) );

		Calendar c = getCalendar( d );
		int month = c.get( Calendar.MONTH );
		if ( option == 1 )
		{
			return String.valueOf( month + 1 );
		}
		else if ( option == 2 )
		{
			return getMonthFormat().format( d );
		}
		else if ( option == 3 )
		{
			return getAbbrMonthFormat().format( d );
		}
		return null;
	}
	
	private static class Function_Week extends Function_temp
	{
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		Function_Week( )
		{
			minParamCount = 1;
			maxParamCount = 1;
		}
		
		protected Object getValue( Object[] args ) throws BirtException
		{
			if( existNullValue( args ) )
			{
				return null;
			}
			return Integer.valueOf( week(DataTypeUtil.toDate(args[0])));
		}
	}	

	/**
	 * Week number of the year (1 to 52) of date/time value d.
	 * 
	 * @param d
	 * @return
	 */
	private static int week( Date d )
	{
		if ( d == null )
			throw new java.lang.IllegalArgumentException( Messages.getString( "error.BirtDateTime.cannotBeNull.DateValue" ) );

		return getCalendar( d ).get( Calendar.WEEK_OF_YEAR );
	}

	private static class Function_Day extends Function_temp
	{
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		Function_Day( )
		{
			minParamCount = 1;
			maxParamCount = 1;
		}
		
		protected Object getValue( Object[] args ) throws BirtException
		{
			if( existNullValue( args ) )
			{
				return null;
			}
			return Integer.valueOf( day(DataTypeUtil.toDate(args[0])));
		}
	}	
	
	/**
	 * Day number of the month(1 to 31).
	 * 
	 * @param d
	 * @return
	 */
	private static int day( Date d )
	{
		if ( d == null )
			throw new java.lang.IllegalArgumentException( Messages.getString( "error.BirtDateTime.cannotBeNull.DateValue" ) );

		return getCalendar( d ).get( Calendar.DAY_OF_MONTH );
	}
	
	private static class Function_WeekDay extends Function_temp
	{
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		Function_WeekDay( )
		{
			minParamCount = 1;
			maxParamCount = 2;
		}
		
		protected Object getValue( Object[] args ) throws BirtException
		{
			if( existNullValue( args ) )
			{
				return null;
			}
			if( args.length == 1 )
				return weekDay(DataTypeUtil.toDate(args[0]));
			else
				return weekDay(DataTypeUtil.toDate(args[0]), ((Number)args[1]).intValue( ));
		}
	}

	/**
	 * Day the week. Return a number 1 (Sunday) to 7 (Saturday).
	 * 
	 * @param d
	 * @return
	 */
	private static String weekDay( Date d )
	{
		if ( d == null )
			throw new java.lang.IllegalArgumentException( Messages.getString( "error.BirtDateTime.cannotBeNull.DateValue" ) );

		return String.valueOf(getCalendar( d ).get( Calendar.DAY_OF_WEEK ));
	}

	/**
	 * Day the week. Option is an integer value: 1: return a number 1 (Sunday)
	 * to 7 (Saturday) 2: return a number 1 (Monday) to 7 (Sunday) 3: return a
	 * number 0 (Monday) to 6 (Sunday) 4: return the weekday name as per user
	 * locale (e.g., Sunday Saturday for English) 5: return the abbreviated
	 * weekday name as per user locale (e.g., Sun Sat for English)
	 * 
	 * @param d
	 * @param option
	 * @return
	 */
	private static String weekDay( Date d, int option )
	{
		if ( d == null )
			throw new java.lang.IllegalArgumentException( Messages.getString( "error.BirtDateTime.cannotBeNull.DateValue" ) );
		switch ( option )
		{
			case 1 :
				return String.valueOf( getWeekDay( d, Calendar.SUNDAY ) );
			case 2 :
				return String.valueOf( getWeekDay( d, Calendar.MONDAY ) );
			case 3 :
				return String.valueOf( getWeekDay( d, Calendar.MONDAY ) - 1 );
			case 4 :
				return getWeekFormat().format( d );
			case 5 :
				return getAbbrWeekFormat().format( d );
		}
		return null;

	}

	private static class Function_Today extends Function_temp
	{
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		Function_Today( )
		{
			minParamCount = 0;
			maxParamCount = 0;
		}
		
		protected Object getValue( Object[] args ) throws BirtException
		{
			if( existNullValue( args ) )
			{
				return null;
			}
			return today();
		}
	}
	
	/**
	 * Returns a timestamp value which is midnight of the current date.
	 * 
	 * @return
	 */
	private static Date today( )
	{
		Calendar calendar = Calendar.getInstance( timeZone );
		calendar.set( Calendar.HOUR_OF_DAY, 0 );
		calendar.clear( Calendar.MINUTE );
		calendar.clear( Calendar.SECOND );
		calendar.clear( Calendar.MILLISECOND );
		
		return calendar.getTime( );
	}

	private static class Function_Now extends Function_temp
	{
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		Function_Now( )
		{
			minParamCount = 0;
			maxParamCount = 0;
		}
		
		protected Object getValue( Object[] args ) throws BirtException
		{
			return now();
		}
	}
	
	/**
	 * Returns the current timestamp
	 * 
	 * @return
	 */
	private static Timestamp now( )
	{
		Date now = new Date( );
		return new Timestamp( now.getTime( ) );
	}

	private static class Function_DiffYear extends Function_temp
	{
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		Function_DiffYear( )
		{
			minParamCount = 2;
			maxParamCount = 2;
		}
		
		protected Object getValue( Object[] args ) throws BirtException
		{
			if( existNullValue( args ) )
			{
				return null;
			}
			return Integer.valueOf(diffYear(DataTypeUtil.toDate(args[0]), DataTypeUtil.toDate(args[1])));
		}
	}
	
	/**
	 * Return difference in number of years
	 * 
	 * @param d1
	 * @param d2
	 * @return
	 */
	private static int diffYear( Date d1, Date d2 )
	{
		if ( d1 == null || d2 == null )
		{
			throw new java.lang.IllegalArgumentException( Messages.getString( "error.BirtDateTime.cannotBeNull.DateValue" ) );
		}
		int startYear = year( d1 );
		int endYear = year( d2 );

		return endYear - startYear;
	}

	private static class Function_DiffMonth extends Function_temp
	{
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		Function_DiffMonth( )
		{
			minParamCount = 2;
			maxParamCount = 2;
		}
		
		protected Object getValue( Object[] args ) throws BirtException
		{
			if( existNullValue( args ) )
			{
				return null;
			}
			return Integer.valueOf( diffMonth(DataTypeUtil.toDate(args[0]), DataTypeUtil.toDate(args[1])));
		}
	}
	
	/**
	 * Return difference in number of months
	 * 
	 * @param d1
	 * @param d2
	 * @return
	 */
	private static int diffMonth( Date d1, Date d2 )
	{
		if ( d1 == null || d2 == null )
		{
			throw new java.lang.IllegalArgumentException( Messages.getString( "error.BirtDateTime.cannotBeNull.DateValue" ) );
		}

		int startMonth = year( d1 ) * 12 + month( d1 );
		int endMonth = year( d2 ) * 12 + month( d2 );

		return endMonth - startMonth;
	}

	private static class Function_DiffQuarter extends Function_temp
	{
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		Function_DiffQuarter( )
		{
			minParamCount = 2;
			maxParamCount = 2;
		}
		
		protected Object getValue( Object[] args ) throws BirtException
		{
			if( existNullValue( args ) )
			{
				return null;
			}
			return Integer.valueOf(diffQuarter(DataTypeUtil.toDate(args[0]), DataTypeUtil.toDate(args[1])));
		}
	}
	
	/**
	 * Return difference in number of quarters
	 * 
	 * @param d1
	 * @param d2
	 * @return
	 */
	private static int diffQuarter( Date d1, Date d2 )
	{
		if ( d1 == null || d2 == null )
		{
			throw new java.lang.IllegalArgumentException( Messages.getString( "error.BirtDateTime.cannotBeNull.DateValue" ) );
		}

		int startQuter = year( d1 ) * 4 + quarter( d1 );
		int endQuter = year( d2 ) * 4 + quarter( d2 );

		return endQuter - startQuter;
	}

	
	private static class Function_DiffWeek extends Function_temp
	{
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		Function_DiffWeek( )
		{
			minParamCount = 2;
			maxParamCount = 2;
		}
		
		protected Object getValue( Object[] args ) throws BirtException
		{
			if( existNullValue( args ) )
			{
				return null;
			}
			return Long.valueOf(diffWeek(DataTypeUtil.toDate(args[0]), DataTypeUtil.toDate(args[1])));
		}
	}
	
	/**
	 * Return difference in number of weeks
	 * 
	 * @param d1
	 * @param d2
	 * @return
	 */
	private static long diffWeek( Date d1, Date d2 )
	{	
		Date sd1 = getWeekStartDay( d1 );
		Date sd2 = getWeekStartDay( d2 );
		return diffDay( sd1, sd2 ) / 7;
	}
	
	/**
	 * 
	 * @param date
	 * @return
	 */
	static public Date getWeekStartDay( Date date )
	{
		int diffDay = 1 - Integer.valueOf( weekDay( date ) ).intValue( );

		return addDay( date, diffDay );
	}

	private static class Function_DiffDay extends Function_temp
	{
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		Function_DiffDay( )
		{
			minParamCount = 2;
			maxParamCount = 2;
		}
		
		protected Object getValue( Object[] args ) throws BirtException
		{
			if( existNullValue( args ) )
			{
				return null;
			}
			return Long.valueOf( diffDay(DataTypeUtil.toDate(args[0]), DataTypeUtil.toDate(args[1])));
		}
	}
	
	/**
	 * Return difference in number of days
	 * 
	 * @param d1
	 * @param d2
	 * @return
	 */
	private static long diffDay( Date d1, Date d2 )
	{
		Calendar c1 = Calendar.getInstance( timeZone );
		c1.setTime( d1 );
		Calendar c2 = Calendar.getInstance( timeZone );
		c2.setTime( d2 );
		if ( c1.after( c2 ) )
		{
			return diffDay( c1, c2 ) * -1;
		}
		else
		{
			return diffDay( c1, c2 );
		}
	}
	
	/**
	 * 
	 * @param d1
	 * @param d2
	 * @return
	 */
	static private long diffDay( Calendar d1, Calendar d2 )
	{
		if ( d1.after( d2 ) )
		{ // swap dates so that d1 is start and d2 is end
			Calendar swap = d1;
			d1 = d2;
			d2 = swap;
		}
		int days = d2.get( Calendar.DAY_OF_YEAR )
				- d1.get( Calendar.DAY_OF_YEAR );
		int y2 = d2.get( Calendar.YEAR );
		if ( d1.get( Calendar.YEAR ) != y2 )
		{
			do
			{
				days += d1.getActualMaximum( Calendar.DAY_OF_YEAR );
				d1.add( Calendar.YEAR, 1 );
			} while ( d1.get( Calendar.YEAR ) != y2 );
		}
		return days;
	}

	private static class Function_DiffHour extends Function_temp
	{
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		Function_DiffHour( )
		{
			minParamCount = 2;
			maxParamCount = 2;
		}
		
		protected Object getValue( Object[] args ) throws BirtException
		{
			if( existNullValue( args ) )
			{
				return null;
			}
			return Long.valueOf( diffHour(DataTypeUtil.toDate(args[0]), DataTypeUtil.toDate(args[1])));
		}
	}
	
	/**
	 * Return difference in number of hours
	 * 
	 * @param d1
	 * @param d2
	 * @return
	 */
	private static long diffHour( Date d1, Date d2 )
	{
		Calendar c = getClearedCalendarInstance( 0, 0, 1 );

		return ( diffSecond( new Date( c.getTimeInMillis( ) ), d2 ) + 3000
				* 60 * 60 * 24 * 7 )
				/ ( 60 * 60 )
				- ( diffSecond( new Date( c.getTimeInMillis( ) ), d1 ) + 3000
						* 60 * 60 * 24 * 7 ) / ( 60 * 60 );
	}

	private static class Function_DiffMinute extends Function_temp
	{
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		Function_DiffMinute( )
		{
			minParamCount = 2;
			maxParamCount = 2;
		}
		
		protected Object getValue( Object[] args ) throws BirtException
		{
			if( existNullValue( args ) )
			{
				return null;
			}
			return Long.valueOf(diffMinute(DataTypeUtil.toDate(args[0]), DataTypeUtil.toDate(args[1])));
		}
	}
	
	/**
	 * Return difference in number of minutes
	 * 
	 * @param d1
	 * @param d2
	 * @return
	 */
	private static long diffMinute( Date d1, Date d2 )
	{
		Calendar c = getClearedCalendarInstance( 0, 0, 1  );
		
		return ( diffSecond( new Date( c.getTimeInMillis( ) ), d2 ) + 3000
				* 60 * 60 * 24 * 7 )
				/ ( 60 )
				- ( diffSecond( new Date( c.getTimeInMillis( ) ), d1 ) + 3000
						* 60 * 60 * 24 * 7 ) / ( 60 );
	}

	/**
	 * The Calendar by default will give you an instance of
	 * current time. This is however not expected. The method clear()
	 * has to be invoked to re-init the Calendar instance.
	 * @return
	 */
	private static Calendar getClearedCalendarInstance(int year, int month, int date )
	{
		Calendar c = Calendar.getInstance( timeZone );
		
		c.clear( );
		
		c.set( year, month, date );
		return c;
	}

	/**
	 * 
	 * @author xyi
	 *
	 */
	private static class Function_DiffSecond extends Function_temp
	{
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		Function_DiffSecond( )
		{
			minParamCount = 2;
			maxParamCount = 2;
		}
		
		protected Object getValue( Object[] args ) throws BirtException
		{
			if( existNullValue( args ) )
			{
				return null;
			}
			return Long.valueOf(diffSecond(DataTypeUtil.toDate(args[0]), DataTypeUtil.toDate(args[1])));
		}
	}
	
	/**
	 * Return difference in number of seconds
	 * 
	 * @param d1
	 * @param d2
	 * @return
	 */
	private static long diffSecond( Date d1, Date d2 )
	{
		if ( d1 == null || d2 == null )
		{
			throw new java.lang.IllegalArgumentException( Messages.getString( "error.BirtDateTime.cannotBeNull.DateValue" ) );
		}
		long diff = d2.getTime( ) - d1.getTime( );
		
		if( timeZone != null && timeZone.inDaylightTime( d1 ) )
			diff -= timeZone.getDSTSavings( );
		if( timeZone != null && timeZone.inDaylightTime( d2 ) )
			diff += timeZone.getDSTSavings( );
		
		return   diff / 1000l ;
	}

	private static class Function_AddYear extends Function_temp
	{
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		Function_AddYear( )
		{
			minParamCount = 2;
			maxParamCount = 2;
		}
			
		protected Object getValue( Object[] args ) throws BirtException
		{
			if( existNullValue( args ) )
			{
				return null;
			}
			return addYear(DataTypeUtil.toDate(args[0]), ((Number)args[1]).intValue( ));
		}
	}
	
	/**
	 * Add num years
	 * 
	 * @param date
	 * @param num
	 * @return
	 */
	private static Date addYear( Date date, int num )
	{
		Calendar startCal = getCalendar( date );

		startCal.add( Calendar.YEAR, num );

		return startCal.getTime( );
	}

	private static class Function_AddMonth extends Function_temp
	{
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		Function_AddMonth( )
		{
			minParamCount = 2;
			maxParamCount = 2;
		}
			
		protected Object getValue( Object[] args ) throws BirtException
		{
			if( existNullValue( args ) )
			{
				return null;
			}
			return addMonth(DataTypeUtil.toDate(args[0]), ((Number)args[1]).intValue( ));
		}
	}
	
	/**
	 * Add num months
	 * 
	 * @param date
	 * @param num
	 * @return
	 */
	private static Date addMonth( Date date, int num )
	{
		Calendar startCal = getCalendar( date );

		startCal.add( Calendar.MONTH, num );

		return startCal.getTime( );
	}

	private static class Function_AddQuarter extends Function_temp
	{
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		Function_AddQuarter( )
		{
			minParamCount = 2;
			maxParamCount = 2;
		}
			
		protected Object getValue( Object[] args ) throws BirtException
		{
			if( existNullValue( args ) )
			{
				return null;
			}
			return addQuarter(DataTypeUtil.toDate(args[0]), ((Number)args[1]).intValue( ));
		}
	}
	
	/**
	 * Add num quarters
	 * 
	 * @param date
	 * @param num
	 * @return
	 */
	private static Date addQuarter( Date date, int num )
	{
		return addMonth( date, num * 3 );
	}

	private static class Function_AddWeek extends Function_temp
	{
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		Function_AddWeek( )
		{
			minParamCount = 2;
			maxParamCount = 2;
		}
			
		protected Object getValue( Object[] args ) throws BirtException
		{
			if( existNullValue( args ) )
			{
				return null;
			}
			return addWeek(DataTypeUtil.toDate(args[0]), ((Number)args[1]).intValue( ));
		}
	}
	
	/**
	 * Add num weeks
	 * 
	 * @param date
	 * @param num
	 * @return
	 */
	private static Date addWeek( Date date, int num )
	{
		return addDay( date, num * 7 );
	}

	private static class Function_AddDay extends Function_temp
	{
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		Function_AddDay( )
		{
			minParamCount = 2;
			maxParamCount = 2;
		}
			
		protected Object getValue( Object[] args ) throws BirtException
		{
			if( existNullValue( args ) )
			{
				return null;
			}
			return addDay(DataTypeUtil.toDate(args[0]), ((Number)args[1]).intValue( ));
		}
	}
	
	/**
	 * Add num days
	 * 
	 * @param date
	 * @param num
	 * @return
	 */
	private static Date addDay( Date date, int num )
	{
		Calendar startCal = getCalendar( date );

		startCal.add( Calendar.DATE, num );

		return startCal.getTime( );
	}

	private static class Function_AddHour extends Function_temp
	{
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		Function_AddHour( )
		{
			minParamCount = 2;
			maxParamCount = 2;
		}
			
		protected Object getValue( Object[] args ) throws BirtException
		{
			if( existNullValue( args ) )
			{
				return null;
			}
			return addHour(DataTypeUtil.toDate(args[0]), ((Number)args[1]).intValue( ));
		}
	}
	
	/**
	 * Add num hours
	 * 
	 * @param date
	 * @param num
	 * @return
	 */
	private static Date addHour( Date date, int num )
	{
		Calendar startCal = getCalendar( date );

		startCal.add( Calendar.HOUR_OF_DAY, num );

		return startCal.getTime( );
	}

	private static class Function_AddMinute extends Function_temp
	{
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		Function_AddMinute( )
		{
			minParamCount = 2;
			maxParamCount = 2;
		}
			
		protected Object getValue( Object[] args ) throws BirtException
		{
			if( existNullValue( args ) )
			{
				return null;
			}
			return addMinute(DataTypeUtil.toDate(args[0]), ((Number)args[1]).intValue( ));
		}
	}
	
	/**
	 * Add num minutes
	 * 
	 * @param date
	 * @param num
	 * @return
	 */
	private static Date addMinute( Date date, int num )
	{
		Calendar startCal = getCalendar( date );

		startCal.add( Calendar.MINUTE, num );

		return startCal.getTime( );
	}

	private static class Function_AddSecond extends Function_temp
	{
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		Function_AddSecond( )
		{
			minParamCount = 2;
			maxParamCount = 2;
		}
			
		protected Object getValue( Object[] args ) throws BirtException
		{
			if( existNullValue( args ) )
			{
				return null;
			}
			return addSecond(DataTypeUtil.toDate(args[0]), ((Number)args[1]).intValue( ));
		}
	}	
	
	/**
	 * Add num seconds
	 * 
	 * @param date
	 * @param num
	 * @return
	 */
	private static Date addSecond( Date date, int num )
	{
		Calendar startCal = getCalendar( date );

		startCal.add( Calendar.SECOND, num );

		return startCal.getTime( );
	}

	
	/**
	 * 
	 * @param d
	 * @param startDay
	 * @return
	 */
	private static int getWeekDay( Date d, int startDay )
	{
		int dayOfWeek = getCalendar( d ).get( Calendar.DAY_OF_WEEK );
		if ( dayOfWeek >= startDay )
		{
			return dayOfWeek - startDay + 1;
		}
		else
		{
			return ( dayOfWeek - startDay + 1 + 7 ) % 8;
		}

	}

	/**
	 * 
	 * @param d
	 * @return
	 */
	private static Calendar getCalendar( Date d )
	{
		Calendar c = null;
		if( d instanceof java.sql.Date )
		{
			c = Calendar.getInstance( TimeZone.getDefault( ), defaultLocale );
		}
		else
		{
			c = Calendar.getInstance( timeZone, defaultLocale );
		}
		if ( d == null )
		{
			c.clear( );
			c.set( 1970, 0, 1 );
		}
		else
		{
			c.setTime( d );
		}
		return c;
	}
	
	/**
	 * 
	 * @param args
	 * @return
	 */
	private static boolean existNullValue( Object[] args )
	{
		if ( args != null )
		{
			for ( int i = 0; i < args.length; i++ )
			{
				if ( args[i] == null )
					return true;
			}
		}
		return false;
	}

	public Object execute( Object[] arguments, IScriptFunctionContext context )
			throws BirtException
	{
		scriptContext = context;
		if ( scriptContext != null )
		{
			ULocale locale = (ULocale) scriptContext.findProperty( org.eclipse.birt.core.script.functionservice.IScriptFunctionContext.LOCALE );
			if ( defaultLocale != locale )
			{
				abbrMonthFormat = null;
				monthFormat = null;
				abbrWeekFormat = null;
				weekFormat = null;
				defaultLocale = locale;
			}
			
			timeZone = (TimeZone) scriptContext.findProperty( org.eclipse.birt.core.script.functionservice.IScriptFunctionContext.TIMEZONE );
		}
		if( timeZone == null )
		{
			timeZone = TimeZone.getDefault( );
		}
		return this.executor.execute( arguments, context );
	}

}
