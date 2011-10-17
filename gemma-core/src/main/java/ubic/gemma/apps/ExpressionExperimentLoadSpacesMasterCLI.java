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
package ubic.gemma.apps;

import java.rmi.RemoteException;

import net.jini.core.event.RemoteEvent;
import net.jini.core.event.RemoteEventListener;
import net.jini.core.event.UnknownEventException;
import net.jini.core.lease.Lease;
import net.jini.space.JavaSpace;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.StopWatch;
import org.springframework.context.ApplicationContext;
import org.springmodules.javaspaces.gigaspaces.GigaSpacesTemplate;

import ubic.gemma.job.TaskResult;
import ubic.gemma.job.grid.util.SpacesUtil;
import ubic.gemma.job.progress.grid.SpacesProgressEntry;
import ubic.gemma.tasks.analysis.expression.ExpressionExperimentLoadTask;
import ubic.gemma.tasks.analysis.expression.ExpressionExperimentLoadTaskCommand;

import com.j_spaces.core.client.EntryArrivedRemoteEvent;
import com.j_spaces.core.client.ExternalEntry;
import com.j_spaces.core.client.NotifyModifiers;

/**
 * This is an example of a CLI that makes use of the grid. This command line interface (CLI) serves as a handy tool/test
 * to submit a task (@see ExpressionExperimentTask) to a {@link JavaSpace}. The CLI implements
 * {@link RemoteEventListener} to be able to receive notifications from the server side.
 * 
 * @author keshav
 * @version $Id$
 */
public class ExpressionExperimentLoadSpacesMasterCLI extends LoadExpressionDataCli implements RemoteEventListener {

    /**
     * Starts the command line interface.
     * 
     * @param args
     */
    public static void main( String[] args ) {
        log.info( "Running GemmaSpaces Master ... \n" );
        ExpressionExperimentLoadSpacesMasterCLI p = new ExpressionExperimentLoadSpacesMasterCLI();
        try {
            Exception ex = p.doWork( args );
            if ( ex != null ) {
                ex.printStackTrace();
            }
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
    }

    private SpacesUtil spacesUtil = null;

    private GigaSpacesTemplate template = null;

    private ExpressionExperimentLoadTask proxy = null;

    /*
     * (non-Javadoc)
     * 
     * @see net.jini.core.event.RemoteEventListener#notify(net.jini.core.event.RemoteEvent)
     */
    @Override
    public void notify( RemoteEvent remoteEvent ) throws UnknownEventException, RemoteException {
        log.debug( "notified ..." );

        try {
            EntryArrivedRemoteEvent arrivedRemoteEvent = ( EntryArrivedRemoteEvent ) remoteEvent;

            log.debug( "event: " + arrivedRemoteEvent );
            ExternalEntry entry = ( ExternalEntry ) arrivedRemoteEvent.getEntry( true );
            log.debug( "entry: " + entry );
            log.debug( "id: " + arrivedRemoteEvent.getID() );
            log.debug( "sequence number: " + arrivedRemoteEvent.getSequenceNumber() );
            log.debug( "notify type: " + arrivedRemoteEvent.getNotifyType() );

            String message = ( String ) entry.getFieldValue( "message" );
            log.info( "message: " + message );

        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.apps.LoadExpressionDataCli#buildOptions()
     */
    @Override
    protected void buildOptions() {
        super.buildOptions();

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.apps.LoadExpressionDataCli#doWork(java.lang.String[])
     */
    @Override
    protected Exception doWork( String[] args ) {
        Exception err = processCommandLine( this.getClass().getName(), args );
        try {

            if ( accessions == null && accessionFile == null ) {
                return new IllegalArgumentException(
                        "You must specific either a file or accessions on the command line" );
            }
            init();
            start();
        } catch ( Exception e ) {
            log.error( "Transformation error..." + e.getMessage() );
            e.printStackTrace();
        }

        return err;
    }

    /**
     * Initialization of spring beans.
     * 
     * @throws Exception
     */
    protected void init() throws Exception {

        spacesUtil = ( SpacesUtil ) this.getBean( "spacesUtil" );
        ApplicationContext updatedContext = spacesUtil.addGemmaSpacesToApplicationContext();

        if ( !updatedContext.containsBean( "gigaspacesTemplate" ) )
            throw new RuntimeException( "GemmaSpaces beans could not be loaded. Cannot start master." );

        template = ( GigaSpacesTemplate ) updatedContext.getBean( "gigaspacesTemplate" );

        proxy = ( ExpressionExperimentLoadTask ) updatedContext.getBean( "javaspaceProxyInterfaceFactory" );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.apps.LoadExpressionDataCli#processOptions()
     */
    @Override
    protected void processOptions() {
        super.processOptions();
    }

    /**
     * Submits the task to the space and retrieves the result.
     */
    protected void start() {

        log.debug( "Got accession(s) from command line " + accessions );

        TaskResult res = null;
        if ( accessions != null ) {
            String[] accsToRun = StringUtils.split( accessions, ',' );

            for ( String accession : accsToRun ) {

                log.info( "processing accession " + accession );
                StopWatch stopwatch = new StopWatch();
                stopwatch.start();

                accession = StringUtils.strip( accession );

                if ( StringUtils.isBlank( accession ) ) {
                    continue;
                }

                /* configure this client to receive notifications */
                try {

                    template.addNotifyDelegatorListener( this, new SpacesProgressEntry(), null, true, Lease.FOREVER,
                            NotifyModifiers.NOTIFY_ALL );

                } catch ( Exception e ) {
                    throw new RuntimeException( e );
                }

                if ( !spacesUtil.canServiceTask( ExpressionExperimentLoadTask.class.getName() ) ) continue;

                ExpressionExperimentLoadTaskCommand command = new ExpressionExperimentLoadTaskCommand( platformOnly,
                        !doMatching, accession, aggressive, false, null );

                res = proxy.execute( command );

                stopwatch.stop();
                long wt = stopwatch.getTime();
                log.info( "Job with id " + res.getTaskID() + " completed in " + wt
                        + " ms.  Number of expression experiments persisted: " + res.getAnswer() + "." );

            }

            /*
             * Terminate the VM after you get the result. This is needed else the VM will wait for the timeout millis
             * that is set in the spring context.
             */
            System.exit( 0 );
        }
    }
}
