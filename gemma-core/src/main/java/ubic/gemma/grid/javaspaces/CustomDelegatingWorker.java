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
package ubic.gemma.grid.javaspaces;

import java.rmi.RemoteException;

import net.jini.core.entry.Entry;
import net.jini.core.entry.UnusableEntryException;
import net.jini.core.lease.Lease;
import net.jini.core.transaction.Transaction;
import net.jini.core.transaction.TransactionException;
import net.jini.space.JavaSpace;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springmodules.javaspaces.DelegatingWorker;
import org.springmodules.javaspaces.JavaSpaceCallback;
import org.springmodules.javaspaces.JavaSpaceTemplate;
import org.springmodules.javaspaces.entry.AbstractMethodCallEntry;
import org.springmodules.javaspaces.entry.MethodResultEntry;

import ubic.gemma.util.grid.javaspaces.SpacesUtil;
import ubic.gemma.util.grid.javaspaces.entry.SpacesBusyEntry;
import ubic.gemma.util.grid.javaspaces.entry.SpacesRegistrationEntry;

/**
 * The {@link DelegatingWorker} was customized to allow interrogation of the task for the taskId.
 * <p>
 * Generic worker designed to run in a thread. Takes method call entries from a JavaSpace. Can either download code or
 * work with a local delegate.
 * <p>
 * Must be configured with a JavaSpaceTemplate helper object to use to read and write from the JavaSpace, and a
 * businessInterface (usually a single interface that this worker will "implement").
 * 
 * @author Rod Johnson
 * @author keshav
 * @version $Id$
 * @see org.springmodules.javaspaces.DelegatingWorker
 */
public class CustomDelegatingWorker implements Runnable {

    private static final Log log = LogFactory.getLog( CustomDelegatingWorker.class );

    private long waitMillis = 500;

    private Object delegate;

    private JavaSpaceTemplate jsTemplate;

    private boolean running = true;

    private Object taskId = null;

    private SpacesRegistrationEntry spacesRegistrationEntry = null;

    private SpacesBusyEntry spacesBusyEntry = null;

    /**
     * Candidate that will match only this interface
     */
    private Entry methodCallEntryTemplate;

    public CustomDelegatingWorker() {
    }
    
    private Class<? extends Object> businessInterface;

    public void setBusinessInterface( Class<? extends Object> intf ) {
        if ( !intf.isInterface() ) {
            throw new IllegalArgumentException( intf + " must be an interface" );
        }
        this.businessInterface = intf;
    }

    /**
     * @return
     */
    public Class<? extends Object> getBusinessInterface() {
        return businessInterface;
    }

    /**
     * Set a delegate if we are using the "service seeking" approach. For RunnableMethodCallEntries, there is no need
     * for a service to be hosted.
     * 
     * @param delegate
     */
    public void setDelegate( Object delegate ) {
        this.delegate = delegate;
    }

    public Object getDelegate() {
        return delegate;
    }

    public void setJavaSpaceTemplate( JavaSpaceTemplate jsTemplate ) {
        this.jsTemplate = jsTemplate;
    }

    public void stop() {
        this.running = false;
    }

    public void run() {
        AbstractMethodCallEntry t = new AbstractMethodCallEntry();
        // Needed for match
        t.uid = null;
        t.className = businessInterface.getName();
        this.methodCallEntryTemplate = jsTemplate.snapshot( t );

        final boolean debug = log.isDebugEnabled();

        if ( debug ) log.debug( "Worker " + this + " starting..." );

        while ( running ) {
            if ( debug ) log.debug( "Worker " + this + " waiting..." );

            // On reading from the space, the result will be computed and
            // written
            // back into the space in one transaction
            jsTemplate.execute( new JavaSpaceCallback() {
                public Object doInSpace( JavaSpace js, Transaction transaction ) throws RemoteException,
                        TransactionException, UnusableEntryException, InterruptedException {

                    // look for method call
                    AbstractMethodCallEntry call = ( AbstractMethodCallEntry ) js.take( methodCallEntryTemplate,
                            transaction, waitMillis );

                    if ( call == null ) {
                        // TODO is this required?
                        if ( debug ) log.debug( "Skipping out of loop..." );
                        return null;
                    }

                    log.info( "Received method call entry.  Will execute task." );
                    log.debug( "Call:   " + call );
                    log.debug( "MethodCallEntryTemplate:   " + methodCallEntryTemplate );
                    log.debug( "SpacesRegistrationEntry:   " + spacesRegistrationEntry );

                    // custom
                    js.take( spacesRegistrationEntry, null, SpacesUtil.VERY_BIG_NUMBER_FOR_SOME_REASON );
                    js.write( spacesBusyEntry, null, SpacesUtil.VERY_BIG_NUMBER_FOR_SOME_REASON );
                    try {
                        Object[] args = call.getArguments();

                        if ( args[0] instanceof TaskCommand ) {
                            TaskCommand javaSpacesCommand = ( TaskCommand ) args[0];
                            /* set the task id of the worker to that of the master */
                            taskId = javaSpacesCommand.getTaskId();
                            log.info( "Task received from master has task id " + taskId );
                            ( ( SpacesTask ) delegate ).setTaskId( taskId.toString() );
                        }

                    } catch ( Exception e ) {
                        log.error( "Cannot get field taskId. Exception is " + e );
                        return null;

                    }
                    // end custom

                    try {
                        MethodResultEntry result = invokeMethod( call, delegate );
                        // push the result back to the JavaSpace
                        js.write( result, transaction, Lease.FOREVER );
                        // take the busy entry from the space
                        js.take( spacesBusyEntry, null, SpacesUtil.VERY_BIG_NUMBER_FOR_SOME_REASON );
                        // write the registration entry to the space
                        js.write( spacesRegistrationEntry, null, SpacesUtil.VERY_BIG_NUMBER_FOR_SOME_REASON );
                    } catch ( Exception ex ) {
                        // Cancelled...Don't throw an exception. See bug 1387.
                        log.error( ex, ex );
                        return null;
                    }

                    return null;
                }
            } );
        }
        if ( debug ) log.debug( "Worker " + this + " terminating" );
    }

    /**
     * Invoke the method on the delegate object in order to get the MethodResultEntry. Subclasses can extend the method
     * and add custom behavior (ex: security propagation).
     * 
     * @param localDelegate the delegate used for executing the method
     * @return the methodResultEntry
     */
    protected MethodResultEntry invokeMethod( AbstractMethodCallEntry call, Object localDelegate )
            throws IllegalAccessException {
        if ( log.isDebugEnabled() ) log.debug( "call is " + call.getClass().getName() );
        MethodResultEntry result = call.invokeMethod( localDelegate );

        if ( log.isDebugEnabled() ) log.debug( "Got result " + result.result );
        return result;
    }

    /**
     * @return
     */
    public Object getTaskId() {
        return taskId;
    }

    /**
     * @param spacesRegistrationEntry
     */
    public void setGemmaSpacesRegistrationEntry( SpacesRegistrationEntry spacesRegistrationEntry ) {
        this.spacesRegistrationEntry = spacesRegistrationEntry;
    }

    public void setGemmaSpacesBusyEntry( SpacesBusyEntry spacesBusyEntry ) {
        this.spacesBusyEntry = spacesBusyEntry;
    }
}
