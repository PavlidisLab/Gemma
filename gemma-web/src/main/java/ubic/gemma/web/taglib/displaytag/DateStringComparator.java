/*
 * The Gemma project
 * 
 * Copyright (c) 2007 University of British Columbia
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
package ubic.gemma.web.taglib.displaytag;

import java.text.ParseException;
import java.util.Comparator;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.time.DateUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Compare string representations of dates. If the date cannot be parsed, lexigraphic sorting is the fallback.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class DateStringComparator implements Comparator<Object> {

    /**
     * This is what the decorators will send for sorting.
     */
    private static final String DATE_HTML_PATTERN = "<span\\s+(style.+?){0,1}\\s+title='(.+?)\\.\\d'>.+";

    /**
     * We can add formats to this.
     */
    private final String[] formats = new String[] { "yyyy.MM.dd HH:mm aa", "yyyy-MM-dd", "yyyy-MM-dd HH:mm:ss" };

    private static Log log = LogFactory.getLog( DateStringComparator.class.getName() );

    /*
     * (non-Javadoc)
     * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
     */
    @Override
    public int compare( Object arg0, Object arg1 ) {
        String d1 = arg0.toString();
        String d2 = arg1.toString();

        Pattern pat = Pattern.compile( DATE_HTML_PATTERN );
        Matcher m1 = pat.matcher( d1 );
        Matcher m2 = pat.matcher( d2 );
        if ( m1.matches() ) {
            String string = m1.group( 2 );
            if ( string != null ) {
                d1 = string;
            }
        }

        if ( m2.matches() ) {
            String string2 = m2.group( 2 );
            if ( string2 != null ) {
                d2 = string2;
            }
        }

        Date date1;
        Date date2;

        try {
            log.debug( d1 + " " + d2 );
            date1 = DateUtils.parseDate( d1, formats );
            date2 = DateUtils.parseDate( d2, formats );
        } catch ( ParseException e ) {
            log.debug( "Failed to parse dates, returning lexigraphic ordering" );
            if ( d1.contains( "NA" ) ) {
                return -1;
            } else if ( d2.contains( "NA" ) ) {
                return 1;
            } else {
                return d1.compareTo( d2 );
            }
        }

        if ( date1.before( date2 ) ) {
            return -1;
        } else if ( date1.after( date2 ) ) {
            return 1;
        } else {
            return 0;
        }

    }
}
