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

import java.rmi.RemoteException;

import net.jini.core.event.RemoteEvent;
import net.jini.core.event.RemoteEventListener;
import net.jini.core.event.UnknownEventException;
import net.jini.core.lease.Lease;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springmodules.javaspaces.gigaspaces.GigaSpacesTemplate;

import com.j_spaces.core.client.NotifyModifiers;

import ubic.gemma.job.TaskCommand;
import ubic.gemma.job.grid.util.SpacesJobObserver;
import ubic.gemma.job.progress.grid.SpacesProgressEntry;

/**
 * This runs on the client (master) before (around) the endpoint interceptor. See TaskMethodAdvice for the interceptor
 * that wraps the remote calls.
 * 
 * @author paul
 * @version $Id$
 */
public class GridTaskInterceptor implements MethodInterceptor {

    @SuppressWarnings("unused")
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
     * @see org.aopalliance.intercept.MethodInterceptor#invoke(org.aopalliance.intercept.MethodInvocation)
     */
    public Object invoke( MethodInvocation invocation ) throws Throwable {

        final TaskCommand command = ( TaskCommand ) invocation.getArguments()[0];

        String taskId = command.getTaskId();

        /*
         * fill in the command information
         */
        command.setTaskInterface( invocation.getMethod().getDeclaringClass().getName() );
        command.setTaskMethod( invocation.getMethod().getName() );
        command.setWillRunOnGrid( true );

        /*
         * register this "spaces client" to receive notifications (logging, basically)
         */
        SpacesJobObserver javaSpacesJobObserver = new SpacesJobObserver( taskId );
        SpacesProgressEntry entry = new SpacesProgressEntry();
        entry.setTaskId( taskId );
        javaSpaceTemplate.addNotifyDelegatorListener( javaSpacesJobObserver, entry, null, true, Lease.FOREVER,
                NotifyModifiers.NOTIFY_UPDATE ); // if you use NOTIFY_ALL, wiping of entry causes repeated logging

        /*
         * Here's how we figure out when the job has actually started.
         */
        javaSpaceTemplate.addNotifyDelegatorListener( new RemoteEventListener() {
            public void notify( RemoteEvent arg0 ) throws UnknownEventException, RemoteException {
                command.setStartTime();
            }
        }, entry, null, true, Lease.FOREVER, NotifyModifiers.NOTIFY_WRITE );

        // all this does it put it in the space - the job has not actually started.
        Object retVal = invocation.proceed();

        return retVal;
    }

}
