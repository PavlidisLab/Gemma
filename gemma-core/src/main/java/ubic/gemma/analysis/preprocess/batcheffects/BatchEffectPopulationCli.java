/*
 * The Gemma project
 * 
 * Copyright (c) 2011 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.analysis.preprocess.batcheffects;

import ubic.gemma.apps.ExpressionExperimentManipulatingCLI;
import ubic.gemma.model.common.auditAndSecurity.eventType.BatchInformationFetchingEvent;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * For bulk processing of batch-info-fetching.
 * 
 * @author paul
 * @version $Id$
 */
public class BatchEffectPopulationCli extends ExpressionExperimentManipulatingCLI {

    @Override
    protected void buildOptions() {
        super.buildOptions();
        addForceOption();
    }

    @Override
    protected Exception doWork( String[] args ) {

        super.processCommandLine( "BatchEffectPopulation", args );

        BatchInfoPopulationService ser = ( BatchInfoPopulationService ) getBean( "batchInfoPopulationService" );

        for ( BioAssaySet bas : this.expressionExperiments ) {
            if ( bas instanceof ExpressionExperiment ) {
                bas = eeService.thawLite( ( ExpressionExperiment ) bas );

                /*
                 * If we're not using the database, always run it.
                 */
                if ( !force && !needToRun( bas, BatchInformationFetchingEvent.class ) ) {
                    log.info( "Can't or don't need to run " + bas );
                    continue;
                }
                log.info( "Processing: " + bas );

                try {
                    ExperimentalFactor ef = ser.fillBatchInformation( ( ExpressionExperiment ) bas, force );
                    if ( ef == null ) {
                        this.errorObjects.add( bas.toString() + ": Null factor returned" );
                    } else {
                        this.successObjects.add( bas.toString() );
                    }

                } catch ( Exception e ) {
                    log.error( e, e );
                    this.errorObjects.add( bas + ": " + e.getMessage() );
                }

            }
        }

        return null;
    }

    public static void main( String[] args ) {
        BatchEffectPopulationCli b = new BatchEffectPopulationCli();
        b.doWork( args );
    }

}
