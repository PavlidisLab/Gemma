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
package ubic.gemma.loader.expression.smd.util;

import ubic.gemma.util.ConfigUtils;
import ubic.gemma.util.NetDatasourceUtil;

/**
 * @author pavlidis
 * @version $Id$
 */
@Deprecated
public class SmdUtil extends NetDatasourceUtil {

    public static final String SMD_DELIM = "\n";

    /**
     * Split a SMD-formatted key-value string. These are preceded by 0 or more white-space, a "!", and then a
     * "="-delimited key-value pair.
     * 
     * @param k
     * @return String array containing the key and value, or null if the input was not a valid SMD-formatted key-value.
     */
    public static String[] smdSplit( String k ) {
        String f = k.trim();

        if ( !f.startsWith( "!" ) ) return null;
        f = f.replaceFirst( "^!", "" );
        String[] vals = f.split( "=" ); // could be nothing after the equals.
        if ( vals.length < 1 ) throw new IllegalStateException( "Could not parse " + k );
        return vals;
    }

    @Override
    public void init() {
        this.setHost( ConfigUtils.getString( "smd.host" ) );
    }

}