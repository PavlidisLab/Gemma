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
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.aop.MethodBeforeAdvice;

import uk.ltd.getahead.dwr.ExecutionContext;

/**
 * <hr>
 * <p>
 * Copyright (c) 2006 UBC Pavlab
 * 
 * @author klc
 * @version $Id$ This interceptor will be invoked
 *          whenever the updateProgress method in the progress interface gets called. This class is a singleton and will
 *          deal with the monitoring of several progress bars. Currently just uses the session object to store progress
 *          bar data. At some point this needs to be changed. What if the session object changes for a given user
 *          (expires)? What about getting all the progress information for all monitored processes for a given user?
 *          What if a job takes 5 days? Monitored process perhaps need to be persisted to the database after some
 *          duration? Ie what happens if the server goes down?
 */
public class ProgressInterceptor implements MethodBeforeAdvice {

    protected static final Log logger = LogFactory.getLog( ProgressInterceptor.class );
    private Map<Object, HttpServletRequest> monitoredProgress;

    public ProgressInterceptor() {
        super();
        monitoredProgress = new HashMap<Object, HttpServletRequest>();

    }

    @SuppressWarnings("unused")
    public void before( Method arg0, Object[] arg1, Object arg2 ) throws Throwable {

        logger.debug( "before in dbinterceptor got called." );

        HttpServletRequest req;

        if ( !monitoredProgress.containsKey( arg2 ) ) {
            monitoredProgress.put( arg2, ExecutionContext.get().getHttpServletRequest() );
        }

        req = monitoredProgress.get( arg2 );
        req.setAttribute( "ProgressInfo", arg1[0] );

    }

}
