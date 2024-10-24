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

import org.apache.commons.cli.Options;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.analysis.preprocess.batcheffects.BatchInfoPopulationService;
import ubic.gemma.model.common.auditAndSecurity.eventType.BatchInformationFetchingEvent;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * For bulk processing of batch-info-fetching.
 *
 * @author paul
 */
public class BatchEffectPopulationCli extends ExpressionExperimentManipulatingCLI {

    @Autowired
    private BatchInfoPopulationService ser;

    @Override
    public String getCommandName() {
        return "fillBatchInfo";
    }

    @Override
    public String getShortDesc() {
        return "Populate the batch information for experiments (if possible)";
    }

    @Override
    protected void buildExperimentOptions( Options options ) {
        this.addForceOption( options );
    }

    @Override
    protected void processExpressionExperiment( ExpressionExperiment ee ) {
        if ( this.noNeedToRun( ee, BatchInformationFetchingEvent.class ) ) {
            return;
        }

        log.info( "Processing: " + ee );

        ser.fillBatchInformation( ee, isForce() );
        try {
            refreshExpressionExperimentFromGemmaWeb( ee, true, true );
        } catch ( Exception e ) {
            log.error( "Failed to refresh " + ee + " from Gemma Web.", e );
        }
    }
}
