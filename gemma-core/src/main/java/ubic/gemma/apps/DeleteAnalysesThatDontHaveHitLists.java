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
import java.util.Map;

import ubic.gemma.analysis.expression.diff.DifferentialExpressionAnalysisHelperService;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisService;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * Used to delete results that were not run after institution of precomputed hitlists, and for whatever reason will not
 * be ru-run automagically.
 * 
 * @author Paul
 * @version $Id$
 */
public class DeleteAnalysesThatDontHaveHitLists extends ExpressionExperimentManipulatingCLI {

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.util.AbstractCLI#doWork(java.lang.String[])
     */
    @Override
    protected Exception doWork( String[] args ) {

        super.processCommandLine( "populate differential expression hit lists", args );

        DifferentialExpressionAnalysisService diffS = this.getBean( DifferentialExpressionAnalysisService.class );
        DifferentialExpressionAnalysisHelperService analyzerHelper = this
                .getBean( DifferentialExpressionAnalysisHelperService.class );

        Map<BioAssaySet, Collection<DifferentialExpressionAnalysis>> allAnalyses = diffS
                .getAnalyses( this.expressionExperiments );

        for ( BioAssaySet bas : this.expressionExperiments ) {

            if ( !( bas instanceof ExpressionExperiment ) ) {
                log.warn( "Subsets not supported yet (" + bas + "), skipping" );
            }

            ExpressionExperiment expressionExperiment = ( ExpressionExperiment ) bas;

            Collection<DifferentialExpressionAnalysis> analyses = allAnalyses.get( expressionExperiment );
            if ( analyses == null ) {
                log.info( "No analyses for " + expressionExperiment );
                continue;
            }

            for ( DifferentialExpressionAnalysis analysis : analyses ) {
                boolean doDelete = false;
                for ( ExpressionAnalysisResultSet resultSet : analysis.getResultSets() ) {
                    if ( resultSet.getNumberOfGenesTested() == null ) {

                        doDelete = true;
                        break;

                    }
                }
                if ( doDelete ) {
                    analyzerHelper.deleteOldAnalysis( expressionExperiment, analysis );
                }
            }

        }

        return null;
    }

    /**
     * @param args
     */
    public static void main( String[] args ) {
        DeleteAnalysesThatDontHaveHitLists c = new DeleteAnalysesThatDontHaveHitLists();
        c.doWork( args );

    }

}
