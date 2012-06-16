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
package ubic.gemma.annotation.geommtx;

import ubic.gemma.apps.ExpressionExperimentManipulatingCLI;
import ubic.gemma.model.common.auditAndSecurity.eventType.AutomatedAnnotationEvent;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * A class that starts with a experiment ID and in the end outputs the predicted annotation URL's, after filtering
 * 
 * @author leon
 * @version $Id$
 */
public class AnnotateExperimentCLI extends ExpressionExperimentManipulatingCLI {
    /**
     * @param args
     */
    public static void main( String[] args ) {
        AnnotateExperimentCLI p = new AnnotateExperimentCLI();

        try {
            Exception ex = p.doWork( args );
            if ( ex != null ) {
                ex.printStackTrace();
            }
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
    }

    ExpressionExperimentAnnotator expressionExperimentAnnotator;

    @Override
    protected void buildOptions() {
        super.buildOptions();
        addForceOption();
    }

    @Override
    protected Exception doWork( String[] args ) {

        Exception err = processCommandLine( "Expression experiment annotator pipeline", args );
        if ( err != null ) return err;

        long time = System.currentTimeMillis();
        time = System.currentTimeMillis();

        for ( BioAssaySet bas : this.expressionExperiments ) {

            ExpressionExperiment experiment = ( ExpressionExperiment ) bas;

            boolean needToRun = force || this.needToRun( experiment, AutomatedAnnotationEvent.class );

            if ( !needToRun ) {
                log.debug( "Skipping " + experiment + ", no need to run" );
                continue;
            }

            // ees.thawLite( experiment );
            log.info( "Processing: " + experiment );

            expressionExperimentAnnotator.annotate( experiment, force );

        }
        log.info( "Total Time:" + ( System.currentTimeMillis() - time ) + "ms" );
        return null;
    }

    @Override
    protected void processOptions() {
        super.processOptions();

        expressionExperimentAnnotator = this.getBean( ExpressionExperimentAnnotator.class );

        expressionExperimentAnnotator.init();
        log.info( "Initializing MMTx..." );

        while ( !ExpressionExperimentAnnotatorImpl.ready() || !PredictedCharacteristicFactoryImpl.ready() ) {
            try {
                Thread.sleep( 10000 );
            } catch ( InterruptedException e ) {
            }
            log.info( "Waiting for MMTx..." );
        }

        log.info( " **** MMTx ready ***" );
    }

}
