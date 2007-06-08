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
package ubic.gemma.javaspaces.gigaspaces;

import java.rmi.RemoteException;

import net.jini.core.event.RemoteEvent;
import net.jini.core.event.RemoteEventListener;
import net.jini.core.event.UnknownEventException;
import net.jini.core.lease.Lease;
import net.jini.space.JavaSpace;

import org.acegisecurity.context.SecurityContextHolder;
import org.apache.commons.lang.math.RandomUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;
import org.springmodules.javaspaces.gigaspaces.GigaSpacesTemplate;

import ubic.gemma.javaspaces.CustomDelegatingWorker;
import ubic.gemma.util.SecurityUtil;
import ubic.gemma.util.javaspaces.GemmaSpacesCancellationEntry;
import ubic.gemma.util.javaspaces.GemmaSpacesRegistrationEntry;
import ubic.gemma.util.javaspaces.gigaspaces.GemmaSpacesEnum;
import ubic.gemma.util.javaspaces.gigaspaces.GigaSpacesUtil;

import com.j_spaces.core.IJSpace;
import com.j_spaces.core.client.EntryArrivedRemoteEvent;
import com.j_spaces.core.client.ExternalEntry;
import com.j_spaces.core.client.NotifyModifiers;

/**
 * This command line interface is used to take {@link ExpressionExperimentTask} tasks from the {@link JavaSpace} and
 * returns the results.
 * 
 * @author keshav
 * @version $Id$
 */
public class ExpressionExperimentGemmaSpacesWorkerCLI extends AbstractGemmaSpacesWorkerCLI implements
        RemoteEventListener {

    private static Log log = LogFactory.getLog( ExpressionExperimentGemmaSpacesWorkerCLI.class );

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.util.AbstractCLI#buildOptions()
     */
    @Override
    protected void buildOptions() {
        // TODO Auto-generated method stub
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.javaspaces.gigaspaces.AbstractBaseGemmaSpacesWorkerCLI#init()
     */
    @Override
    protected void init() throws Exception {

        /* register the shutdown hook so cleanup occurs even if VM is incorrectly terminated */
        ShutdownHook shutdownHook = new ShutdownHook();
        Runtime.getRuntime().addShutdownHook( shutdownHook );

        GigaSpacesUtil gigaspacesUtil = ( GigaSpacesUtil ) this.getBean( "gigaSpacesUtil" );
        ApplicationContext updatedContext = gigaspacesUtil
                .addGigaspacesToApplicationContext( GemmaSpacesEnum.DEFAULT_SPACE.getSpaceUrl() );

        if ( !updatedContext.containsBean( "gigaspacesTemplate" ) )
            throw new RuntimeException( "Gigaspaces beans could not be loaded. Cannot start worker." );

        template = ( GigaSpacesTemplate ) updatedContext.getBean( "gigaspacesTemplate" );

        worker = ( CustomDelegatingWorker ) updatedContext.getBean( "worker" );

        template.addNotifyDelegatorListener( this, new GemmaSpacesCancellationEntry(), null, true, Lease.FOREVER,
                NotifyModifiers.NOTIFY_ALL );

        space = ( IJSpace ) template.getSpace();

        workerRegistrationId = RandomUtils.nextLong();
        registrationEntry = new GemmaSpacesRegistrationEntry();
        registrationEntry.message = ExpressionExperimentTask.class.getName();
        registrationEntry.registrationId = workerRegistrationId;
        Lease lease = space.write( registrationEntry, null, 600000000 );
        log.info( this.getClass().getSimpleName() + " registered with space " + template.getUrl() );
        if ( lease == null ) log.error( "Null Lease returned" );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.javaspaces.gigaspaces.AbstractBaseGemmaSpacesWorkerCLI#start()
     */
    @Override
    protected void start() {
        log.debug( "Authentication: " + SecurityContextHolder.getContext().getAuthentication() );

        itbThread = new Thread( worker );
        itbThread.start();

        log.info( this.getClass().getSimpleName() + " started successfully." );

    }

    /**
     * Starts the command line interface.
     * 
     * @param args
     */
    public static void main( String[] args ) {
        log.info( "Running GigaSpaces worker to load expression experiments ... \n" );

        SecurityUtil.passAuthenticationToChildThreads();

        ExpressionExperimentGemmaSpacesWorkerCLI p = new ExpressionExperimentGemmaSpacesWorkerCLI();
        try {
            Exception ex = p.doWork( args );
            if ( ex != null ) {
                ex.printStackTrace();
            }
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.util.AbstractSpringAwareCLI#processOptions()
     */
    @Override
    protected void processOptions() {
        super.processOptions();
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.jini.core.event.RemoteEventListener#notify(net.jini.core.event.RemoteEvent)
     */
    public void notify( RemoteEvent remoteEvent ) throws UnknownEventException, RemoteException {
        log.info( "notified ..." );

        try {
            EntryArrivedRemoteEvent arrivedRemoteEvent = ( EntryArrivedRemoteEvent ) remoteEvent;

            log.debug( "event: " + arrivedRemoteEvent );
            ExternalEntry entry = ( ExternalEntry ) arrivedRemoteEvent.getEntry( true );
            Object taskId = ( Object ) entry.getFieldValue( "taskId" );

            if ( taskId.equals( worker.getTaskId() ) ) {
                log.info( "Stopping execution of task: " + taskId );
                itbThread.stop();
                // itbThread.destroy();
            }

        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }

    }
}
