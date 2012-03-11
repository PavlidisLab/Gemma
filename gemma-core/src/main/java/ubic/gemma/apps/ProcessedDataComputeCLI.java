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

import org.apache.commons.lang.time.StopWatch;

import ubic.gemma.analysis.preprocess.ProcessedExpressionDataVectorCreateService;
import ubic.gemma.expression.experiment.service.ExpressionExperimentService;
import ubic.gemma.model.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.model.common.auditAndSecurity.eventType.ProcessedVectorComputationEvent;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * Prepare the "processed" expression data vectors. This also computes the ranks (replaces the old DEDVRank stuff0.
 * 
 * @author xwan, paul
 * @version $Id$
 * @see ubic.gemma.analysis.preprocess.ProcessedExpressionDataVectorCreateServiceImpl
 */
public class ProcessedDataComputeCLI extends ExpressionExperimentManipulatingCLI {

    /**
     * @param args
     */
    public static void main( String[] args ) {
        ProcessedDataComputeCLI computing = new ProcessedDataComputeCLI();
        StopWatch watch = new StopWatch();
        watch.start();
        try {
            Exception ex = computing.doWork( args );
            if ( ex != null ) {
                ex.printStackTrace();
            }
            watch.stop();
            log.info( "Elapsed time: " + watch.getTime() / 1000 + " seconds" );
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
    }

    private ProcessedExpressionDataVectorCreateService processedExpressionDataVectorCreateService;

    @Override
    public String getShortDesc() {
        return "Updates the 'processed expression data', including computing the 'ranks' for each expression vector.";
    }

    /**
     * 
     */
    @Override
    protected void buildOptions() {

        super.buildOptions();

        super.addForceOption();
        addDateOption();
    }

    /**
     * 
     */
    @Override
    protected Exception doWork( String[] args ) {
        Exception err = processCommandLine( "processed expression data updater ", args );
        if ( err != null ) {
            return err;
        }

        if ( expressionExperiments.size() == 0 ) {
            log.error( "You did not select any usable expression experiments" );
            return null;
        }

        for ( BioAssaySet ee : expressionExperiments ) {
            processExperiment( ( ExpressionExperiment ) ee );
        }
        summarizeProcessing();
        return null;
    }

    /**
     * 
     */
    @Override
    protected void processOptions() {
        super.processOptions();
        processedExpressionDataVectorCreateService = ( ProcessedExpressionDataVectorCreateService ) this
                .getBean( "processedExpressionDataVectorCreateService" );
        this.auditTrailService = ( AuditTrailService ) this.getBean( "auditTrailService" );
        eeService = ( ExpressionExperimentService ) this.getBean( "expressionExperimentService" );
    }

    /**
     * @param errorObjects
     * @param persistedObjects
     * @param ee
     */
    private void processExperiment( ExpressionExperiment ee ) {
        if ( isTroubled( ee ) && !force ) {
            log.info( "Skipping troubled experiment " + ee.getShortName() );
            return;
        }
        try {
            ee = eeService.thawLite( ee );
            boolean needToRun = needToRun( ee, ProcessedVectorComputationEvent.class );

            if ( !needToRun && !force ) {
                log.info( "No need to run " + ee + "(use 'force' to override)" );
                return;
            }
            this.processedExpressionDataVectorCreateService.computeProcessedExpressionData( ee );
            // Note tha tauditing is done by the service.
            successObjects.add( ee.toString() );
        } catch ( Exception e ) {
            errorObjects.add( ee + ": " + e.getMessage() );
            log.error( "**** Exception while processing " + ee + ": " + e.getMessage() + " ********", e );
        }
    }
}
