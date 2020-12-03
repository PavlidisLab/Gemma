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
package ubic.gemma.core.apps;

import ubic.gemma.core.util.AbstractCLI;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.analysis.expression.sampleCoexpression.SampleCoexpressionAnalysisService;

/**
 * Create correlation visualizations for expression experiments
 *
 * @author paul
 */
public class ExpressionDataCorrMatCli extends ExpressionExperimentManipulatingCLI {

    @Override
    public String getCommandName() {
        return "corrMat";
    }

    @Override
    protected void doWork() throws Exception {
        for ( BioAssaySet ee : expressionExperiments ) {
            try {
                if ( !( ee instanceof ExpressionExperiment ) ) {
                    addErrorObject( ee, "This is not an ExpressionExperiment!" );
                    continue;
                }
                this.processExperiment( ( ExpressionExperiment ) ee );
                addSuccessObject( ee, "Successfully processed " + ( ( ExpressionExperiment ) ee ).getShortName() );
            } catch ( Exception e ) {
                addErrorObject( ee, "Error while processing", e );
            }

        }
    }

    @Override
    public String getShortDesc() {
        return "Create or update sample correlation matrices for expression experiments";
    }

    @Override
    protected void buildOptions() {
        super.buildOptions();
        super.addForceOption();
    }

    private void audit( ExpressionExperiment ee ) {
        auditTrailService.addUpdateEvent( ee, null, "Generated sample correlation matrix" );
        addSuccessObject( ee, "Successfully processed " + ee.getShortName() );
    }

    private void processExperiment( ExpressionExperiment ee ) {
        if ( !force && this.noNeedToRun( ee, null ) ) {
            return;
        }

        SampleCoexpressionAnalysisService sampleCoexpressionAnalysisService = this
                .getBean( SampleCoexpressionAnalysisService.class );

        ee = eeService.thawLiter( ee );
        if ( !force ) {
            sampleCoexpressionAnalysisService.load( ee );
        } else {
            sampleCoexpressionAnalysisService.compute( ee );
        }

        this.audit( ee );
    }

}
