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

import org.apache.commons.cli.Options;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.analysis.preprocess.filter.FilteringException;
import ubic.gemma.model.common.auditAndSecurity.eventType.FailedSampleCorrelationAnalysisEvent;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.analysis.expression.sampleCoexpression.SampleCoexpressionAnalysisService;

/**
 * Create correlation visualizations for expression experiments
 *
 * @author paul
 */
public class ExpressionDataCorrMatCli extends ExpressionExperimentManipulatingCLI {

    @Autowired
    private SampleCoexpressionAnalysisService sampleCoexpressionAnalysisService;

    @Override
    public String getCommandName() {
        return "corrMat";
    }

    @Override
    public String getShortDesc() {
        return "Create or update sample correlation matrices for expression experiments";
    }

    @Override
    protected void buildOptions( Options options ) {
        super.buildOptions( options );
        super.addForceOption( options );
    }

    @Override
    protected void processExpressionExperiment( ExpressionExperiment ee ) {
        if ( this.noNeedToRun( ee, null ) ) {
            return;
        }
        ee = eeService.thawLiter( ee );
        try {
            if ( isForce() ) {
                sampleCoexpressionAnalysisService.compute( ee, sampleCoexpressionAnalysisService.prepare( ee ) );
            } else {
                if ( sampleCoexpressionAnalysisService.retrieveExisting( ee ) == null ) {
                    sampleCoexpressionAnalysisService.compute( ee, sampleCoexpressionAnalysisService.prepare( ee ) );
                }
            }
        } catch ( FilteringException e ) {
            auditTrailService.addUpdateEvent( ee, FailedSampleCorrelationAnalysisEvent.class, null, e );
            throw new RuntimeException( e );
        } catch ( Exception e ) {
            auditTrailService.addUpdateEvent( ee, FailedSampleCorrelationAnalysisEvent.class, null, e );
            throw e;
        }
    }
}
