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

import org.apache.commons.lang.time.DateUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Compare string representations of dates. If the date cannot be parsed, lexigraphic sorting is the fallback.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class DateStringComparator implements Comparator {

    /**
     * We can add formats to this.
     */
    private final String[] formats = new String[] { "yyyy.MMM.dd hh:mm aa" };

    private static Log log = LogFactory.getLog( DateStringComparator.class.getName() );

    /*
     * (non-Javadoc)
     * 
     * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
     */
    public int compare( Object arg0, Object arg1 ) {
        String d1 = ( String ) arg0;
        String d2 = ( String ) arg1;

        Date date1;
        Date date2;

        try {
            date1 = DateUtils.parseDate( d1, formats );
            date2 = DateUtils.parseDate( d2, formats );
        } catch ( ParseException e ) {
            log.debug( "Failed to parse dates, returning lexigraphic ordering" );
            return d1.compareTo( d2 );
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
