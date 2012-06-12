/*
 * The Gemma project
 * 
 * Copyright (c) 2007 Columbia University
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

import java.util.Comparator;

/**
 * Comparator for simple numbers in displayTag.
 * 
 * @author jsantos
 * @version $Id$
 */
public class NumberComparator implements Comparator<Object> {

    /*
     * (non-Javadoc)
     * 
     * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
     */
    @Override
    public int compare( Object arg0, Object arg1 ) {
        String s1 = "0";
        String s2 = "0";
        double num1;
        double num2;

        // do some reflection to determine if the numbers need to be parsed from String
        // if it is not an integer, assume it is a number of some sort. Convert to string.
        try {
            if ( arg0.getClass() == Class.forName( "java.lang.String" )
                    && arg1.getClass() == Class.forName( "java.lang.String" ) ) {
                s1 = ( String ) arg0;
                s2 = ( String ) arg1;
            } else {
                Class<?> c1 = arg0.getClass();
                Class<?> c2 = arg1.getClass();

                s1 = c1.cast( arg0 ).toString();
                s2 = c2.cast( arg1 ).toString();
            }
        } catch ( ClassNotFoundException e ) {
            // fallback on lexigraphic sort.
            return s1.compareTo( s2 );
        }

        num1 = Double.parseDouble( s1 );
        num2 = Double.parseDouble( s2 );
        if ( num1 < num2 ) {
            return -1;
        } else if ( num1 > num2 ) {
            return 1;
        } else {
            return 0;
        }

    }

}
