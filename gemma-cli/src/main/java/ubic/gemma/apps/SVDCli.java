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

package ubic.gemma.apps;

import org.apache.commons.cli.Options;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.analysis.preprocess.svd.SVDException;
import ubic.gemma.core.analysis.preprocess.svd.SVDService;
import ubic.gemma.model.common.auditAndSecurity.eventType.PCAAnalysisEvent;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * @author paul
 */
public class SVDCli extends ExpressionExperimentManipulatingCLI {

    @Autowired
    private SVDService svdService;

    @Override
    public String getCommandName() {
        return "pca";
    }

    @Override
    public String getShortDesc() {
        return "Run PCA (using SVD) on data sets";
    }

    @Override
    protected void buildExperimentOptions( Options options ) {
        super.addForceOption( options );
    }

    @Override
    protected void processExpressionExperiment( ExpressionExperiment ee ) {
        if ( this.noNeedToRun( ee, PCAAnalysisEvent.class ) ) {
            return;
        }
        log.info( "Processing: " + ee );
        try {
            svdService.svd( ee );
        } catch ( SVDException e ) {
            throw new RuntimeException( e );
        }
    }
}
