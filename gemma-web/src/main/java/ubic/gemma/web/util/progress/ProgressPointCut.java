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

        boolean found = Progress.class.isAssignableFrom(arg1);
        
        if ( found ) log.debug( "Creating proxy for progress class:  " + arg1 + "  Method: " + arg0 );

        return ( found );

    }

}
