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

import ubic.gemma.persistence.service.analysis.expression.sampleCoexpression.SampleCoexpressionAnalysisService;
import ubic.gemma.core.util.AbstractCLI;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * Create correlation visualizations for expression experiments
 *
 * @author paul
 */
public class ExpressionDataCorrMatCli extends ExpressionExperimentManipulatingCLI {

    public static void main( String[] args ) {
        try {
            ExpressionDataCorrMatCli e = new ExpressionDataCorrMatCli();
            Exception ex = e.doWork( args );
            if ( ex != null )
                AbstractCLI.log.info( ex, ex );
        } catch ( Exception e ) {
            AbstractCLI.log.info( e, e );
        }
    }

    @Override
    public String getCommandName() {
        return "corrMat";
    }

    @Override
    protected Exception doWork( String[] args ) {
        Exception exception = this.processCommandLine( args );

        for ( BioAssaySet ee : expressionExperiments ) {
            try {
                if ( !( ee instanceof ExpressionExperiment ) ) {
                    errorObjects.add( ee );
                    continue;
                }
                this.processExperiment( ( ExpressionExperiment ) ee );
                successObjects.add( ee );
            } catch ( Exception e ) {
                AbstractCLI.log.error( "Error while processing " + ee, e );
                errorObjects.add( ee );
            }

        }
        this.summarizeProcessing();
        return exception;
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
        successObjects.add( ee.toString() );
    }

    private void processExperiment( ExpressionExperiment ee ) {
        if ( !force && this.noNeedToRun( ee, null ) ) {
            return;
        }

        SampleCoexpressionAnalysisService sampleCoexpressionAnalysisService = this
                .getBean( SampleCoexpressionAnalysisService.class );

        if ( this.force ) {
            sampleCoexpressionAnalysisService.compute( ee);
        } else {
            sampleCoexpressionAnalysisService.load( ee);
        }

        this.audit( ee );
    }

}
