/*
 * The Gemma project
 * 
 * Copyright (c) 2010 University of British Columbia
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
package ubic.gemma.job.grid;

import net.jini.core.event.RemoteEvent;
import net.jini.core.event.RemoteEventListener;
import net.jini.core.lease.Lease;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springmodules.javaspaces.gigaspaces.GigaSpacesTemplate;

import ubic.gemma.job.TaskCommand;
import ubic.gemma.job.grid.util.SpacesJobObserver;
import ubic.gemma.job.progress.grid.SpacesProgressEntry;

import com.j_spaces.core.client.NotifyDelegator;
import com.j_spaces.core.client.NotifyModifiers;

/**
 * This runs on the client (master) before (around) the endpoint interceptor. See TaskMethodAdvice for the interceptor
 * that wraps the remote calls.
 * 
 * @author paul
 * @version $Id$
 */
public class GridTaskInterceptor implements MethodInterceptor {

    private static Log log = LogFactory.getLog( GridTaskInterceptor.class );

    private GigaSpacesTemplate javaSpaceTemplate;

    /**
     * @param javaSpaceTemplate the javaSpaceTemplate to set
     */
    public void setJavaSpaceTemplate( GigaSpacesTemplate javaSpaceTemplate ) {
        this.javaSpaceTemplate = javaSpaceTemplate;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.aopalliance.intercept.MethodInterceptor#invoke(org.aopalliance.intercept.MethodInvocation)
     */
    @Override
    public Object invoke( MethodInvocation invocation ) throws Throwable {

        final TaskCommand command = ( TaskCommand ) invocation.getArguments()[0];

        String taskId = command.getTaskId();

        /*
         * fill in the command information
         */
        command.setTaskInterface( invocation.getMethod().getDeclaringClass().getName() );
        command.setTaskMethod( invocation.getMethod().getName() );
        command.setWillRunOnGrid( true );

        SpacesJobObserver javaSpacesJobObserver = new SpacesJobObserver( taskId );
        SpacesProgressEntry loggingEntryTemplate = new SpacesProgressEntry();
        loggingEntryTemplate.setTaskId( taskId );

        /*
         * FIXME Note we should set the leases to be not Lease.FOREVER so that we eventually clean these up, even if
         * something goes wrong in the regular cleanup phase. Unforutnately this doesn't help with the thread leak. when
         * fifo =true
         */

        /*
         * Logging notifications
         */
        boolean forceOrderedLogging = false; // fifo
        NotifyDelegator loggingNotifyDelegator = javaSpaceTemplate.addNotifyDelegatorListener( javaSpacesJobObserver,
                loggingEntryTemplate, null, forceOrderedLogging, Lease.FOREVER, NotifyModifiers.NOTIFY_UPDATE );

        /*
         * Here's how we figure out when the job has actually started.
         */
        NotifyDelegator jobStartNotifyDelegator = javaSpaceTemplate.addNotifyDelegatorListener(
                new RemoteEventListener() {
                    @Override
                    public void notify( RemoteEvent arg0 ) {
                        log.debug( "got start" );
                        command.setStartTime();
                    }
                }, loggingEntryTemplate, null, false, Lease.FOREVER, NotifyModifiers.NOTIFY_WRITE );

        log.debug( "initialized, starting call" );

        /*
         * This blocks while the job gets run by the endpoint JavaSpaceInterceptor. If it queues, then it gets stuck
         * here. See gigaspaces.xml where we have 'synchronous=true'.
         */
        Object retVal = null;
        try {
            retVal = invocation.proceed();
        } catch ( Exception e ) {
            log.debug( "Job threw an exception: " + e.getMessage() );
            throw ( e );
        } finally {

            log.debug( "job done" );

            javaSpaceTemplate.clear( loggingEntryTemplate );

            /*
             * Remove the listeners - otherwise we leak resources. See
             * http://www.gigaspaces.com/docs/JavaDoc/com/j_spaces/core/client/NotifyDelegator.html. However, this
             * doesn't stop threads from leaking if we have fifo enabled for the notifier.
             */
            loggingNotifyDelegator.getEventRegistration().getLease().cancel();
            loggingNotifyDelegator.close();
            // loggingNotifyDelegator.finalize();
            loggingNotifyDelegator = null;

            jobStartNotifyDelegator.getEventRegistration().getLease().cancel();
            jobStartNotifyDelegator.close();
            // jobStartNotifyDelegator.finalize();
            jobStartNotifyDelegator = null;
        }
        return retVal;
    }

}
