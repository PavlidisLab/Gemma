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
package ubic.gemma.job.grid.worker;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springmodules.javaspaces.DelegatingWorker;
import org.springmodules.javaspaces.entry.AbstractMethodCallEntry;
import org.springmodules.javaspaces.entry.MethodResultEntry;

import ubic.gemma.job.TaskCommand;

/**
 * Runnable that triggers to run a task remotely (by a worker). The {@link DelegatingWorker} was customized to allow
 * interrogation of the task for the taskId.
 * 
 * @author keshav, paul
 * @version $Id$
 * @see org.springmodules.javaspaces.DelegatingWorker
 * @see ubic.gemma.job.grid.GridmethodAdvice
 */
public class CustomDelegatingWorker extends DelegatingWorker {

    @SuppressWarnings("unused")
    private static Log log = LogFactory.getLog( CustomDelegatingWorker.class );

    /**
     * Id of this worker.
     */
    private String registrationId = null;

    /**
     * @param registrationId the registrationId to set
     */
    public void setRegistrationId( String registrationId ) {
        this.registrationId = registrationId;
    }

    /**
     * @return the registrationId
     */
    public String getRegistrationId() {
        return registrationId;
    }

    /**
     * 
     */
    private String currentTaskId = null;

    /*
     * (non-Javadoc)
     * @see
     * org.springmodules.javaspaces.DelegatingWorker#invokeMethod(org.springmodules.javaspaces.entry.AbstractMethodCallEntry
     * , java.lang.Object)
     */
    @Override
    protected MethodResultEntry invokeMethod( AbstractMethodCallEntry call, Object localDelegate )
            throws IllegalAccessException {

        Object[] args = call.getArguments();

        assert TaskCommand.class.isAssignableFrom( args[0].getClass() );

        TaskCommand javaSpacesCommand = ( TaskCommand ) args[0];

        javaSpacesCommand.setStartTime();
        javaSpacesCommand.setTaskInterface( call.getMethod().getDeclaringClass().getName() );
        javaSpacesCommand.setTaskMethod( call.getMethod().getName() );

        this.currentTaskId = javaSpacesCommand.getTaskId();

        /*
         * security context should have been propagated by the GridmethodAdvice
         */
        assert javaSpacesCommand.getSecurityContext() != null;

        MethodResultEntry result;
        try {
            result = super.invokeMethod( call, localDelegate );
        } catch ( RuntimeException e ) {
            throw e;
        } finally {
            this.currentTaskId = null;
        }

        return result;

    }

    public String getCurrentTaskId() {
        return this.currentTaskId;
    }

}
