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
package ubic.gemma.core.apps;

import ubic.gemma.core.analysis.preprocess.batcheffects.BatchInfoPopulationService;
import ubic.gemma.core.apps.GemmaCLI.CommandGroup;
import ubic.gemma.core.util.AbstractCLI;
import ubic.gemma.model.common.auditAndSecurity.eventType.BatchInformationFetchingEvent;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * For bulk processing of batch-info-fetching.
 *
 * @author paul
 */
public class BatchEffectPopulationCli extends ExpressionExperimentManipulatingCLI {

    public static int main( String[] args ) {
        BatchEffectPopulationCli b = new BatchEffectPopulationCli();
        return executeCommand( b, args );
    }

    @Override
    public CommandGroup getCommandGroup() {
        return CommandGroup.EXPERIMENT;
    }

    @Override
    protected void buildOptions() {
        super.buildOptions();
        this.addForceOption();
    }

    @Override
    public String getCommandName() {
        return "fillBatchInfo";
    }

    @Override
    protected void doWork( String[] args ) throws Exception {
        super.processCommandLine( args );

        BatchInfoPopulationService ser = this.getBean( BatchInfoPopulationService.class );

        for ( BioAssaySet bas : this.expressionExperiments ) {
            if ( bas instanceof ExpressionExperiment ) {

                if ( !force && this.noNeedToRun( bas, BatchInformationFetchingEvent.class ) ) {
                    AbstractCLI.log.debug( "Can't or don't need to run " + bas );
                    continue;
                }

                AbstractCLI.log.info( "Processing: " + bas );

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
                    AbstractCLI.log.error( e, e );
                    this.errorObjects.add( bas + ": " + e.getMessage() );
                }

            }
        }

        this.summarizeProcessing();
    }

    @Override
    public String getShortDesc() {
        return "Populate the batch information for experiments (if possible)";
    }

}
