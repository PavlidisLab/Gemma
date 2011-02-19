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

package ubic.gemma.analysis.preprocess.svd;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;

import ubic.gemma.apps.ExpressionExperimentManipulatingCLI;
import ubic.gemma.model.common.auditAndSecurity.eventType.PCAAnalysisEvent;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * @author paul
 * @version $Id$
 */
public class SVDCli extends ExpressionExperimentManipulatingCLI {

    private boolean postAnalysisOnly = false;

    @SuppressWarnings("static-access")
    @Override
    protected void buildOptions() {
        super.buildOptions();
        super.addForceOption();

        Option postanalyzeOnlyOpt = OptionBuilder.withLongOpt( "post" ).withDescription(
                "Don't perform SVD, just update the statistics of comparisons with factors etc. Implies -force" )
                .create();
        super.addOption( postanalyzeOnlyOpt );
    }

    @Override
    protected void processOptions() {
        super.processOptions();
        if ( hasOption( "post" ) ) {
            this.postAnalysisOnly = true;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.util.AbstractCLI#doWork(java.lang.String[])
     */
    @Override
    protected Exception doWork( String[] args ) {
        Exception err = super.processCommandLine( "SVD", args );

        if ( err != null ) return err;

        SVDService svdser = ( SVDService ) this.getBean( "sVDService" );

        for ( BioAssaySet bas : this.expressionExperiments ) {

            if ( !postAnalysisOnly && !force && !needToRun( bas, PCAAnalysisEvent.class ) ) {
                this.errorObjects.add( bas + ": Already has PCA; use -force to override" );
                continue;
            }

            try {
                log.info( "Processing: " + bas );
                if ( postAnalysisOnly ) {
                    SVDValueObject svdo = svdser.retrieveSvd( bas.getId() );
                    if ( svdo == null ) {
                        this.errorObjects.add( bas + ": Did not have an existing PCA; don't use the -post option." );
                    } else {
                        svdser.svdFactorAnalysis( ( ExpressionExperiment ) bas, svdo );
                    }
                } else {
                    svdser.svd( ( ExpressionExperiment ) bas );
                    this.successObjects.add( bas.toString() );
                }
            } catch ( Exception e ) {
                log.error( e, e );
                this.errorObjects.add( bas + ": " + e.getMessage() );
            }
        }
        summarizeProcessing();
        return null;
    }

    /**
     * @param args
     */
    public static void main( String[] args ) {
        SVDCli s = new SVDCli();
        Exception e = s.doWork( args );

        if ( e != null ) {
            log.error( e, e );
        }

    }

}
