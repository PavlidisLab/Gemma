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
package ubic.gemma.web.util;

/**
 * @author kelsey
 *
 */

// Private inner class for converting doubple point to a point of strings.
public class StringPoint {

    private static int PERCISSION = 4;

    private String x;
    private String y;

    StringPoint() {
        super();
    }

    StringPoint( Double x, Double y ) {
        this();
        this.x = Double.toString( x );
        this.y = Double.toString( y );

        if ( this.x.length() > PERCISSION ) this.x = this.x.substring( 0, PERCISSION );

        if ( this.y.length() > PERCISSION ) this.y = this.y.substring( 0, PERCISSION );

    }
}
