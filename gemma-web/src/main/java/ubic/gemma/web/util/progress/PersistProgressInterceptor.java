/*
 * The Gemma project
 * 
 * Copyright (c) 2006 Columbia University
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
import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.aop.AfterReturningAdvice;

/**
 * <hr>
 * <p>
 * Copyright (c) 2006 UBC Pavlab
 * 
 * @author klc
 * @version $Id$
 */
public class PersistProgressInterceptor extends ProgressInterceptor implements AfterReturningAdvice {

    protected static final Log logger = LogFactory.getLog( PersistProgressInterceptor.class );
    protected int finishingValue; // used to determine the end of the progress metre
    protected int progress; // just a count of the progress made
    protected int percent;

    /**
     * General constructor.
     */
    public PersistProgressInterceptor() {
        super( "Saving Data to Database..." );
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.aop.AfterReturningAdvice#afterReturning(java.lang.Object, java.lang.reflect.Method,
     *      java.lang.Object[], java.lang.Object) Should be called after the a single persist is called on a given
     *      object.
     */
    @SuppressWarnings("unused")
    public void afterReturning( Object arg0, Method arg1, Object[] arg2, Object arg3 ) throws Throwable {
        logger.debug( "afterreturning in dbinterceptor got called." );
        this.progress++;
        this.updateSession( ( progress / finishingValue ) * 100 );

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.aop.MethodBeforeAdvice#before(java.lang.reflect.Method, java.lang.Object[],
     *      java.lang.Object) this gets called to initlize the progress bar. Just needs to set the finishing value of
     *      the progress meter. eg) when a collection of objects is commited to the database it's called, finds out the
     *      number of entities in in the collection and uses that value as the finishing value.
     */
    @SuppressWarnings("unused")
    public void before( Method arg0, Object[] arg1, Object arg2 ) throws Throwable {

        // Collection object = (Collection) arg1[0];
        // object.size();
        logger.debug( "before in dbinterceptor got called." );

        // Just in case this gets called more than once. It shouldn't but...i just wanna check
        if ( this.finishingValue != 0 ) {
            logger
                    .debug( "finishing value is non-zero. Most likely ProgressInterceptor is getting called multiple times." );
            return;

        }
        Collection object = null;
        if ( Collection.class.isAssignableFrom( arg1[0].getClass() ) ) object = ( Collection ) arg1[0];
        this.finishingValue = object.size();

    }
}
