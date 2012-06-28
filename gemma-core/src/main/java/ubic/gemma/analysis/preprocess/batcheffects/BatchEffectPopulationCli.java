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
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * For bulk processing of batch-info-fetching.
 * 
 * @author paul
 * @version $Id$
 */
public class BatchEffectPopulationCli extends ExpressionExperimentManipulatingCLI {

    public static void main( String[] args ) {
        BatchEffectPopulationCli b = new BatchEffectPopulationCli();
        b.doWork( args );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.apps.ExpressionExperimentManipulatingCLI#buildOptions()
     */
    @Override
    protected void buildOptions() {
        super.buildOptions();
        addForceOption();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.util.AbstractCLI#doWork(java.lang.String[])
     */
    @Override
    protected Exception doWork( String[] args ) {

        Exception ex = super.processCommandLine( "BatchEffectPopulation", args );
        if ( ex != null ) return ex;

        BatchInfoPopulationService ser = getBean( BatchInfoPopulationService.class );

        for ( BioAssaySet bas : this.expressionExperiments ) {
            if ( bas instanceof ExpressionExperiment ) {

                if ( !force && !needToRun( bas, BatchInformationFetchingEvent.class ) ) {
                    log.debug( "Can't or don't need to run " + bas );
                    continue;
                }

                log.info( "Processing: " + bas );

                try {
                    ExpressionExperiment ee = ( ExpressionExperiment ) bas;
                    ee = this.eeService.thawLite( ee );
                    boolean success = ser.fillBatchInformation( ee, force );
                    if ( success ) {
                        this.successObjects.add( bas.toString() );
                    } else {
                        this.errorObjects.add( bas.toString() );
                    }

                } catch ( Exception e ) {
                    log.error( e, e );
                    this.errorObjects.add( bas + ": " + e.getMessage() );
                }

            }
        }

        summarizeProcessing();
        return null;
    }

}
