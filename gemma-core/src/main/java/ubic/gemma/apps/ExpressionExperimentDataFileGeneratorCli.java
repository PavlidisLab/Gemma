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

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import ubic.gemma.analysis.service.ExpressionDataFileService;
import ubic.gemma.model.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType;
import ubic.gemma.model.common.auditAndSecurity.eventType.CommentedEvent;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * TODO Document Me
 * 
 * @author ?
 * @version $Id$
 */
public class ExpressionExperimentDataFileGeneratorCli extends ExpressionExperimentManipulatingCLI {

    /**
     * @param args
     */
    public static void main( String[] args ) {
        ExpressionExperimentDataFileGeneratorCli p = new ExpressionExperimentDataFileGeneratorCli();
        Exception e = p.doWork( args );
        if ( e != null ) {
            log.fatal( e, e );
        }
    }

    private boolean force_write = false;

    ExpressionDataFileService expressionDataFileService;

    private String DESCRIPTION = "Generate Flat data files (diff expression, co-expression) for a given set of experiments";

    @Override
    public String getShortDesc() {
        return DESCRIPTION;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.util.AbstractCLI#buildOptions()
     */
    @SuppressWarnings("static-access")
    @Override
    protected void buildOptions() {
        super.buildOptions();

        Option forceWriteOption = OptionBuilder.hasArg().withArgName( "ForceWrite" )
                .withDescription( "Overwrites exsiting files if this option is set" ).withLongOpt( "forceWrite" )
                .create( 'w' );

        addThreadsOption();
        addOption( forceWriteOption );
    }

    @Override
    protected Exception doWork( String[] args ) {

        Exception exp = processCommandLine( DESCRIPTION, args );
        if ( exp != null ) {
            return exp;
        }

        BlockingQueue<BioAssaySet> queue = new ArrayBlockingQueue<BioAssaySet>( expressionExperiments.size() );

        // Add the Experiments to the queue for processing
        for ( BioAssaySet ee : expressionExperiments ) {
            if ( ee instanceof ExpressionExperiment ) {
                try {
                    queue.put( ee );
                } catch ( InterruptedException ie ) {
                    log.info( ie );
                }
            } else {
                throw new UnsupportedOperationException( "Can't handle non-EE BioAssaySets yet" );
            }

        }

        // Inner class for processing the experiments
        class Worker extends Thread {
            BlockingQueue<BioAssaySet> q;
            SecurityContext context;

            Worker( BlockingQueue<BioAssaySet> q, SecurityContext context ) {
                this.context = context;
                this.q = q;
            }

            @Override
            public void run() {

                SecurityContextHolder.setContext( this.context );

                while ( true ) {
                    BioAssaySet ee = q.poll();
                    if ( ee == null ) {
                        break;
                    }
                    log.info( "Processing Experiment: " + ee.getName() );
                    processExperiment( ( ExpressionExperiment ) ee );

                }

            }
        }

        final SecurityContext context = SecurityContextHolder.getContext();

        Collection<Thread> threads = new ArrayList<Thread>();

        for ( int i = 1; i <= this.numThreads; i++ ) {
            Worker worker = new Worker( queue, context );
            threads.add( worker );
            log.info( "Starting thread " + i );
            worker.start();
        }

        waitForThreadPoolCompletion( threads );

        summarizeProcessing();

        return null;

    }

    @Override
    protected void processOptions() {
        super.processOptions();

        if ( hasOption( THREADS_OPTION ) ) {
            this.numThreads = this.getIntegerOptionValue( "threads" );
        }

        if ( hasOption( 'w' ) ) {
            this.force_write = true;
        }

        expressionDataFileService = ( ExpressionDataFileService ) this.getBean( "expressionDataFileService" );
    }

    private void processExperiment( ExpressionExperiment ee ) {

        try {
            ee = this.eeService.thawLite( ee );

            AuditTrailService ats = ( AuditTrailService ) this.getBean( "auditTrailService" );
            AuditEventType type = CommentedEvent.Factory.newInstance();

            expressionDataFileService.writeOrLocateCoexpressionDataFile( ee, force_write );
            expressionDataFileService.writeOrLocateDiffExpressionDataFile( ee, force_write );

            ats.addUpdateEvent( ee, type, "Generated Flat data files for downloading" );
            super.successObjects.add( "Success:  generated data file for " + ee.getShortName() + " ID=" + ee.getId() );

        } catch ( Exception e ) {
            log.info( "Caught runtime error: " + e );
            super.errorObjects.add( "FAILED: for ee: " + ee.getShortName() + " ID= " + ee.getId() + " Error: "
                    + e.getMessage() );
        }
    }

}
