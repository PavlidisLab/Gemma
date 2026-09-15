/*
 * The baseCode project
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
package ubic.gemma.core.util;

import org.apache.commons.lang3.time.DateUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Date helpers.
 * <p>
 * Ported in-tree from {@code ubic.basecode.util.DateUtil} as part of the Phase 3
 * baseCode util retirement (see {@code BASECODE_DEP_AUDIT.md}). Only the three
 * methods actually used by Gemma have been carried over:
 * {@link #convertStringToDate}, {@link #getRelativeDate}, and
 * {@link #numberOfSecondsBetweenDates}. Dropped: {@code convertDateToString},
 * {@code getDateTime}, {@code getTodayDate}, and the dead {@code datePattern}
 * static.
 *
 * @author pavlidis
 */
public class DateUtil {

    /**
     * This method generates a string representation of a date/time in the format you specify on input.
     *
     * @param aMask   the date pattern the string is in
     * @param strDate a string representation of a date
     * @return a converted Date object
     * @see java.text.SimpleDateFormat
     */
    public static Date convertStringToDate( String aMask, String strDate ) throws ParseException {
        SimpleDateFormat df = new SimpleDateFormat( aMask );
        try {
            return df.parse( strDate );
        } catch ( ParseException pe ) {
            throw new ParseException( pe.getMessage(), pe.getErrorOffset() );
        }
    }

    /**
     * Turn a string like '-7d' into the date equivalent to "seven days ago". Supports 'd' for day, 'h' for hour, 'm'
     * for minutes, "M" for months and "y" for years. Start with a '-' to indicate times in the past ('+' is not
     * necessary for future). Values must be integers.
     *
     * @param date       to be added/subtracted to
     * @return Date relative to 'now' as modified by the input date string.
     * @author Paul Pavlidis
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

        switch ( unit ) {
            case "h": return DateUtils.addHours( date, amount );
            case "m": return DateUtils.addMinutes( date, amount );
            case "d": return DateUtils.addDays( date, amount );
            case "y": return DateUtils.addYears( date, amount );
            case "M": return DateUtils.addMonths( date, amount );
            default:
                throw new IllegalArgumentException( "Couldn't make sense of units in " + dateString
                    + ", please use something like -7d or -8h" );
        }
    }

    /**
     * Compute the number of seconds spanned by the given dates. If no or a single date is provided, returns 0.
     */
    public static long numberOfSecondsBetweenDates( Collection<Date> dates ) {
        if ( dates == null ) throw new IllegalArgumentException();
        if ( dates.size() < 2 ) return 0;
        // dates we are sure are safe...
        Date max = DateUtils.addYears( new Date(), -500 );
        Date min = DateUtils.addYears( new Date(), 500 );
        for ( Date d : dates ) {
            if ( d.before( min ) ) {
                min = d;
            }
            if ( d.after( max ) ) {
                max = d;
            }
        }
        return Math.round( ( max.getTime() - min.getTime() ) / 1000.00 );
    }
}
