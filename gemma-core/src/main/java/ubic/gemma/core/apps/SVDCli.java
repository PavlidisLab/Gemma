/*
 * The Gemma project
 *
 * Copyright (c) 2011 University of British Columbia
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

package ubic.gemma.core.apps;

import ubic.gemma.core.analysis.preprocess.svd.SVDService;
import ubic.gemma.core.apps.GemmaCLI.CommandGroup;
import ubic.gemma.core.util.AbstractCLI;
import ubic.gemma.model.common.auditAndSecurity.eventType.PCAAnalysisEvent;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * @author paul
 */
public class SVDCli extends ExpressionExperimentManipulatingCLI {

    public static int main( String[] args ) {
        SVDCli s = new SVDCli();
        return executeCommand( s, args );
    }

    @Override
    public String getShortDesc() {
        return "Run PCA (using SVD) on data sets";
    }

    @Override
    public CommandGroup getCommandGroup() {
        return CommandGroup.EXPERIMENT;
    }

    @Override
    protected void buildOptions() {
        super.buildOptions();
        super.addForceOption();
    }

    @Override
    public String getCommandName() {
        return "pca";
    }

    @Override
    protected void doWork( String[] args ) throws Exception {
        super.processCommandLine( args );

        SVDService svdService = this.getBean( SVDService.class );

        for ( BioAssaySet bas : this.expressionExperiments ) {

            if ( !force && this.noNeedToRun( bas, PCAAnalysisEvent.class ) ) {
                this.errorObjects.add( bas + ": Already has PCA; use -force to override" );
                continue;
            }

            try {
                AbstractCLI.log.info( "Processing: " + bas );
                ExpressionExperiment ee = ( ExpressionExperiment ) bas;

                svdService.svd( ee.getId() );

                this.successObjects.add( bas.toString() );
            } catch ( Exception e ) {
                AbstractCLI.log.error( e, e );
                this.errorObjects.add( bas + ": " + e.getMessage() );
            }
        }
        this.summarizeProcessing();
    }

}
