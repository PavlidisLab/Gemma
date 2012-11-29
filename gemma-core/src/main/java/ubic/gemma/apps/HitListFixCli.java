/*
 * The Gemma project
 * 
 * Copyright (c) 2012 University of British Columbia
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

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import ubic.gemma.analysis.expression.diff.DifferentialExpressionAnalyzerService;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisService;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * Used to delete results that were not run after institution of precomputed hitlists, and for whatever reason will not
 * be re-run automagically.
 * 
 * @author Paul
 * @version $Id$
 * @deprecated because it should not be needed any more; for isolated problems it is easier to simply rerun the
 *             analysis.
 */
@Deprecated
public class HitListFixCli extends ExpressionExperimentManipulatingCLI {

    /**
     * @param args
     */
    public static void main( String[] args ) {
        HitListFixCli c = new HitListFixCli();
        c.doWork( args );

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.util.AbstractCLI#doWork(java.lang.String[])
     */
    @Override
    protected Exception doWork( String[] args ) {

        super.processCommandLine( "populate differential expression hit lists", args );

        DifferentialExpressionAnalyzerService diffXs = this.getBean( DifferentialExpressionAnalyzerService.class );
        DifferentialExpressionAnalysisService diffS = this.getBean( DifferentialExpressionAnalysisService.class );

        Map<BioAssaySet, Collection<DifferentialExpressionAnalysis>> allAnalyses = diffS
                .getAnalyses( this.expressionExperiments );

        for ( Iterator<BioAssaySet> it = this.expressionExperiments.iterator(); it.hasNext(); ) {
            BioAssaySet bas = it.next();
            if ( !( bas instanceof ExpressionExperiment ) ) {
                log.warn( "Subsets not supported yet (" + bas + "), skipping" );
                continue;
            }

            ExpressionExperiment expressionExperiment = ( ExpressionExperiment ) bas;
            try {

                Collection<DifferentialExpressionAnalysis> analyses = allAnalyses.get( expressionExperiment );
                if ( analyses == null ) {
                    log.debug( "No analyses for " + expressionExperiment );
                    continue;
                }

                log.info( "Processing analyses for " + bas );

                for ( DifferentialExpressionAnalysis analysis : analyses ) {

                    diffXs.updateSummaries( analysis );

                }

                log.info( "Done with " + bas );
                it.remove();
                allAnalyses.remove( bas ); // allow garbage to be collected.
                this.successObjects.add( bas );
            } catch ( Exception e ) {
                log.error( e, e );
                this.errorObjects.add( bas + " " + e.getMessage() );
            }
        }
        summarizeProcessing();
        return null;
    }

}
