/*
 * The Gemma project
 * 
 * Copyright (c) 2006 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package ubic.gemma.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.time.DateUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Date Utility Class This is used to convert Strings to Dates and Timestamps. In part adapted from Appfuse
 * 
 * @author pavlidis
 * @author <a href="mailto:matt@raibledesigns.com">Matt Raible</a> Modified by <a href="mailto:dan@getrolling.com">Dan
 *         Kibler </a> to correct time pattern. Minutes should be mm not MM (MM is month).
 * @version $Revision$ $Date$
 */
public class DateUtil {
    // ~ Static fields/initializers =============================================

    private static Log log = LogFactory.getLog( DateUtil.class );
    private static String datePattern = "MM/dd/yyyy";
    private static String timePattern = "HH:mm";

    // ~ Methods ================================================================

    /**
     * This method generates a string representation of a date/time in the format you specify on input
     * 
     * @param aMask the date pattern the string is in
     * @param strDate a string representation of a date
     * @return a converted Date object
     * @see java.text.SimpleDateFormat
     * @throws ParseException
     */
    public static final Date convertStringToDate( String aMask, String strDate ) throws ParseException {
        SimpleDateFormat df = null;
        Date date = null;
        df = new SimpleDateFormat( aMask );

        if ( log.isDebugEnabled() ) {
            log.debug( "converting '" + strDate + "' to date with mask '" + aMask + "'" );
        }

        try {
            date = df.parse( strDate );
        } catch ( ParseException pe ) {
            throw new ParseException( pe.getMessage(), pe.getErrorOffset() );
        }

        return ( date );
    }

    /**
     * This method returns the current date time in the format: MM/dd/yyyy HH:MM a
     * 
     * @param theTime the current time
     * @return the current date/time
     */
    public static String getTimeNow( Date theTime ) {
        return getDateTime( timePattern, theTime );
    }

    /**
     * This method returns the current date in the format: MM/dd/yyyy
     * 
     * @return the current date
     * @throws ParseException
     */
    public static Calendar getToday() throws ParseException {
        Date today = new Date();
        SimpleDateFormat df = new SimpleDateFormat( datePattern );

        // This seems like quite a hack (date -> string -> date),
        // but it works ;-)
        String todayAsString = df.format( today );
        Calendar cal = new GregorianCalendar();
        cal.setTime( convertStringToDate( todayAsString ) );

        return cal;
    }

    /**
     * This method generates a string representation of a date's date/time in the format you specify on input
     * 
     * @param aMask the date pattern the string is in
     * @param aDate a date object
     * @return a formatted string representation of the date
     * @see java.text.SimpleDateFormat
     */
    public static final String getDateTime( String aMask, Date aDate ) {
        SimpleDateFormat df = null;
        String returnValue = "";

        if ( aDate == null ) {
            log.error( "aDate is null!" );
        } else {
            df = new SimpleDateFormat( aMask );
            returnValue = df.format( aDate );
        }

        return ( returnValue );
    }

    /**
     * This method generates a string representation of a date based on the System Property 'dateFormat' in the format
     * you specify on input
     * 
     * @param aDate A date to convert
     * @return a string representation of the date
     */
    public static final String convertDateToString( Date aDate ) {
        return getDateTime( datePattern, aDate );
    }

    /**
     * This method converts a String to a date using the datePattern
     * 
     * @param strDate the date to convert (in format MM/dd/yyyy)
     * @return a date object
     * @throws ParseException
     */
    public static Date convertStringToDate( String strDate ) throws ParseException {
        Date aDate = null;

        try {
            if ( log.isDebugEnabled() ) {
                log.debug( "converting date with pattern: " + datePattern );
            }

            aDate = convertStringToDate( datePattern, strDate );
        } catch ( ParseException pe ) {
            log.error( "Could not convert '" + strDate + "' to a date, throwing exception" );
            pe.printStackTrace();
            throw new ParseException( pe.getMessage(), pe.getErrorOffset() );

        }

        return aDate;
    }

    /**
     * Turn a string like '-7d' into the date equivalent to "seven days ago". Supports 'd' for day, 'h' for hour, 'm'
     * for minutes, "M" for months and "y" for years. Start with a '-' to indicate times in the past ('+' is not
     * necessary for future). Values must be integers.
     * 
     * @param date to be added/subtracted to
     * @param dateString
     * @author Paul Pavlidis
     * @return Date relative to 'now' as modified by the input date string.
     */
    public static Date getRelativeDate( Date date, String dateString ) {

        if ( date == null ) throw new IllegalArgumentException( "Null date" );

        Pattern pat = Pattern.compile( "([+-]?[0-9]+)([dmhMy])" );

        Matcher match = pat.matcher( dateString );
        boolean matches = match.matches();

        if ( !matches ) {
            throw new IllegalArgumentException( "Couldn't make sense of " + dateString
                    + ", please use something like -7d or -8h" );
        }

        int amount = Integer.parseInt( match.group( 1 ).replace( "+", "" ) );
        String unit = match.group( 2 );

        if ( unit.equals( "h" ) ) {
            return DateUtils.addHours( date, amount );
        } else if ( unit.equals( "m" ) ) {
            return DateUtils.addMinutes( date, amount );
        } else if ( unit.equals( "d" ) ) {
            return DateUtils.addDays( date, amount );
        } else if ( unit.equals( "y" ) ) {
            return DateUtils.addYears( date, amount );
        } else if ( unit.equals( "M" ) ) {
            return DateUtils.addMonths( date, amount );
        } else {
            throw new IllegalArgumentException( "Couldn't make sense of units in " + dateString
                    + ", please use something like -7d or -8h" );
        }

    }

}
