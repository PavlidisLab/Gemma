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

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springmodules.javaspaces.gigaspaces.GigaSpacesTemplate;

import ubic.gemma.apps.LoadExpressionDataCli;

import com.j_spaces.core.IJSpace;
import com.j_spaces.core.client.EntryArrivedRemoteEvent;
import com.j_spaces.core.client.NotifyDelegator;
import com.j_spaces.core.client.NotifyModifiers;

/**
 * @author keshav
 * @version $Id$
 */
public class ExpressionExperimentMasterCLI extends LoadExpressionDataCli implements RemoteEventListener {

    private static Log log = LogFactory.getLog( ExpressionExperimentMasterCLI.class );

    private GigaSpacesTemplate template;

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.apps.LoadExpressionDataCli#buildOptions()
     */
    @Override
    protected void buildOptions() {
        super.buildOptions();

    }

    /**
     * @param args
     */
    public static void main( String[] args ) {
        log.info( "Running GigaSpaces Master ... \n" );
        ExpressionExperimentMasterCLI p = new ExpressionExperimentMasterCLI();
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
     * @throws Exception
     */
    protected void init() throws Exception {
        template = ( GigaSpacesTemplate ) this.getBean( "gigaspacesTemplate" );
    }

    /**
     * Invokes the gigaspace task.
     */
    protected void start() {

        log.debug( "Got accession(s) from command line " + accessions );

        ExpressionExperimentTask proxy = ( ExpressionExperimentTask ) this.getBean( "proxy" );

        Result res = null;
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

                if ( platformOnly ) {
                    // TODO add back in
                    throw new IllegalArgumentException( "\'Platform Only\' (y) unsupported at this time." );
                    // Collection designs = geoService.fetchAndLoad( accession, true, true );
                    // for ( Object object : designs ) {
                    // assert object instanceof ArrayDesign;
                    // successObjects.add( ( ( Describable ) object ).getName()
                    // + " ("
                    // + ( ( ArrayDesign ) object ).getExternalReferences().iterator().next()
                    // .getAccession() + ")" );
                    // }
                } else {

                    /* configure this client to be receive notifications */
                    try {
                        // JavaSpaceTemplate jsTemplate = ( ( JavaSpaceTemplate ) template );
                        // jsTemplate.notify( new LoggingEntry(), this, 60000, new MarshalledObject( new LoggingEntry()
                        // ) );

                        NotifyDelegator notifyDelegator = new NotifyDelegator( ( IJSpace ) template.getSpace(),
                                new LoggingEntry(), null, this, Lease.FOREVER, null, true, NotifyModifiers.NOTIFY_ALL );

                    } catch ( Exception e ) {
                        throw new RuntimeException( e );
                    }
                    res = proxy.execute( accession, platformOnly, doMatching );

                    stopwatch.stop();
                    long wt = stopwatch.getTime();
                    log.info( "Job with id " + res.getTaskID() + " completed in " + wt
                            + " ms.  Number of expression experiments persisted: " + res.getAnswer() + "." );
                }
            }

            /*
             * Terminate the VM after you get the result. This is needed else the VM will wait for the timeout millis
             * that is set in the spring context.
             */
            System.exit( 0 );
        }
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

    /*
     * (non-Javadoc)
     * 
     * @see net.jini.core.event.RemoteEventListener#notify(net.jini.core.event.RemoteEvent)
     */
    public void notify( RemoteEvent remoteEvent ) throws UnknownEventException, RemoteException {

        log.info( "notified ..." );

        try {
            log.info( "id: " + remoteEvent.getID() );
            log.info( "sequence number: " + remoteEvent.getSequenceNumber() );
            // log.info( "registration object: " + ( ( LoggingEntry ) remoteEvent.getRegistrationObject().get() ) );
            log.info( "notify type: " + ( ( EntryArrivedRemoteEvent ) remoteEvent ).getNotifyType() );
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }

    }
}
