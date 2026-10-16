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

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.analysis.preprocess.svd.SVDException;
import ubic.gemma.core.analysis.preprocess.svd.SVDService;
import ubic.gemma.model.common.auditAndSecurity.eventType.PCAAnalysisEvent;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * @author paul
 */
public class SVDCli extends ExpressionExperimentManipulatingCLI {

    private static final String COMPONENTS_OPTION = "k";

    @Autowired
    private SVDService svdService;

    /**
     * Leading components to compute; 0 computes the full decomposition.
     */
    private int numComponents = 0;

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
        options.addOption( Option.builder( COMPONENTS_OPTION ).longOpt( "components" )
                .hasArg().type( Number.class )
                .desc( "Compute only this many leading components instead of the full decomposition, for datasets "
                        + "too large for it (e.g. single-cell pseudo-bulks with thousands of samples). PCA consumers "
                        + "read the first 5; asking for 10 keeps those exact. The stored PCA then has this many "
                        + "eigenvalues rather than one per sample. By default the full decomposition is computed." )
                .get() );
    }

    @Override
    protected void processExperimentOptions( CommandLine commandLine ) throws ParseException {
        if ( commandLine.hasOption( COMPONENTS_OPTION ) ) {
            this.numComponents = ( ( Number ) commandLine.getParsedOptionValue( COMPONENTS_OPTION ) ).intValue();
            if ( this.numComponents < 1 ) {
                throw new ParseException( "-" + COMPONENTS_OPTION + " must be at least 1." );
            }
        }
    }

    @Override
    protected void processExpressionExperiment( ExpressionExperiment ee ) throws SVDException {
        if ( this.noNeedToRun( ee, PCAAnalysisEvent.class ) ) {
            addSuccessObject( ee, "No need to perform SVD, experiment already has an up-to-date PCA analysis event." );
            return;
        }
        log.info( "Processing: " + ee );
        svdService.svd( ee, numComponents );
        addSuccessObject( ee, numComponents > 0 ? "Performed SVD, top " + numComponents + " components." : "Performed SVD." );
    }
}
