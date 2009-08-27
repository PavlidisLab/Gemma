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

import java.io.File;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import ubic.gemma.analysis.service.ExpressionDataFileService;
import ubic.gemma.model.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType;
import ubic.gemma.model.common.auditAndSecurity.eventType.CommentedEvent;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

public class ExpressionExperimentDataFileGeneratorCli extends ExpressionExperimentManipulatingCLI {

    private static final boolean FORCE_WRITE = true;

    ExpressionDataFileService expressionDataFileService;

    private String DESCRIPTION = "Generate Flat data files (diff expression, co-expression) for a given set of experiments";

    @Override
    protected Exception doWork( String[] args ) {

        Exception exp = processCommandLine( DESCRIPTION, args );
        if ( exp != null ) {
            return exp;
        }

        BlockingQueue<BioAssaySet> queue = new ArrayBlockingQueue<BioAssaySet>( expressionExperiments.size() );

        //Add the Experiments to the queue for processing 
        for ( BioAssaySet ee : expressionExperiments ) {
            if ( ee instanceof ExpressionExperiment ) {
                try {
                    queue.put( ( ExpressionExperiment ) ee );
                } catch ( InterruptedException ie ) {
                    //FIXME
                }
            } else {
                throw new UnsupportedOperationException( "Can't handle non-EE BioAssaySets yet" );
            }

        }

        //Inner class for processing the experiments
        class Worker extends Thread {
            BlockingQueue<BioAssaySet> q;

            Worker( BlockingQueue<BioAssaySet> q ) {
                this.q = q;
            }

            public void run() {
                try {
                    while ( true ) {
                        BioAssaySet ee = q.take();
                        if ( ee == null ) {
                            summarizeProcessing();                            
                            break;
                        }

                        processExperiment( ( ExpressionExperiment ) ee );
                     
                    }
                } catch ( InterruptedException e ) {
                    //FIXME
                }
            }
        }

        //
        int numWorkers = 6;
        Worker[] workers = new Worker[numWorkers];
        for ( int i = 0; i < workers.length; i++ ) {
            workers[i] = new Worker( queue );
            workers[i].start();
        }
        
        return null;

    }

    private void processExperiment( ExpressionExperiment ee ) {

        try {
            this.eeService.thawLite( ee );

            AuditTrailService auditEventService = ( AuditTrailService ) this.getBean( "auditTrailService" );
            AuditEventType type = CommentedEvent.Factory.newInstance();

            File coexpressionFile = expressionDataFileService.writeOrLocateCoexpressionDataFile( ee, FORCE_WRITE );
            File diffExpressionFile = expressionDataFileService.writeOrLocateDiffExpressionDataFile( ee, FORCE_WRITE );

            auditEventService.addUpdateEvent( ee, type, "Generated Flat data files for downloading" );
            super.successObjects.add( "Success:  generated data file for " + ee.getShortName() + " ID=" + ee.getId() );

        } catch ( Exception e ) {
            super.errorObjects.add( "FAILED: for ee: " + ee.getShortName() + " ID= " + ee.getId() + " Error: "
                    + e.getMessage() );
        }
    }

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

    @Override
    protected void processOptions() {
        super.processOptions();

        expressionDataFileService = ( ExpressionDataFileService ) this.getBean( "expressionDataFileService" );
    }

    @Override
    public String getShortDesc() {
        return DESCRIPTION;
    }

}
