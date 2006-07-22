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

package ubic.gemma.web.util.progress;

import java.lang.reflect.Method;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.aop.support.StaticMethodMatcherPointcut;

/**
 * <hr>
 * <p>
 * Copyright (c) 2006 UBC Pavlab
 * 
 * @author klc
 * @version $Id$
 */
public class ProgressPointCut extends StaticMethodMatcherPointcut {

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.aop.MethodMatcher#matches(java.lang.reflect.Method, java.lang.Class) Used for
     *      autoproxing any class that implements the progres interface
     */

    private final transient Log log = LogFactory.getLog( ProgressPointCut.class );

    @SuppressWarnings("unused")
    public boolean matches( Method arg0, Class arg1 ) {

        // for some reason this could be null....
        if ( arg1 == null ) return false;

        boolean found = /*(Progress.class.isAssignableFrom( arg1 ) && */ arg0.getName().equals("updatePercent");

        return ( found );

    }

}
