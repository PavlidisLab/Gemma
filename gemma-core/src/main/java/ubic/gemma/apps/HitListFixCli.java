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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import ubic.gemma.analysis.expression.diff.DiffExAnalyzer;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisService;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionResultService;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.analysis.expression.diff.HitListSize;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.CompositeSequenceService;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;

/**
 * Used to delete results that were not run after institution of precomputed hitlists, and for whatever reason will not
 * be ru-run automagically.
 * 
 * @author Paul
 * @version $Id$
 */
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

        DifferentialExpressionAnalysisService diffS = this.getBean( DifferentialExpressionAnalysisService.class );
        DifferentialExpressionResultService diffrS = this.getBean( DifferentialExpressionResultService.class );
        CompositeSequenceService compositeSequenceService = this.getBean( CompositeSequenceService.class );

        Map<BioAssaySet, Collection<DifferentialExpressionAnalysis>> allAnalyses = diffS
                .getAnalyses( this.expressionExperiments );

        DiffExAnalyzer lma = this.getBean( DiffExAnalyzer.class );

        for ( BioAssaySet bas : this.expressionExperiments ) {

            if ( !( bas instanceof ExpressionExperiment ) ) {
                log.warn( "Subsets not supported yet (" + bas + "), skipping" );
                continue;
            }

            ExpressionExperiment expressionExperiment = ( ExpressionExperiment ) bas;

            Collection<DifferentialExpressionAnalysis> analyses = allAnalyses.get( expressionExperiment );
            if ( analyses == null ) {
                log.debug( "No analyses for " + expressionExperiment );
                continue;
            }

            log.info( "Processing analyses for " + bas );

            diffS.thaw( analyses );
            for ( DifferentialExpressionAnalysis analysis : analyses ) {

                Map<CompositeSequence, Collection<Gene>> probe2GeneMap = new HashMap<CompositeSequence, Collection<Gene>>();

                for ( ExpressionAnalysisResultSet resultSet : analysis.getResultSets() ) {

                    diffrS.thaw( resultSet );

                    List<DifferentialExpressionAnalysisResult> results = new ArrayList<DifferentialExpressionAnalysisResult>(
                            resultSet.getResults() );
                    for ( DifferentialExpressionAnalysisResult d : results ) {
                        CompositeSequence probe = d.getProbe();
                        probe2GeneMap.put( probe, new HashSet<Gene>() );
                    }
                }

                probe2GeneMap = compositeSequenceService.getGenes( probe2GeneMap.keySet() );
                log.info( "Got probe/gene info" );

                assert !probe2GeneMap.isEmpty();

                for ( ExpressionAnalysisResultSet resultSet : analysis.getResultSets() ) {
                    List<DifferentialExpressionAnalysisResult> results = new ArrayList<DifferentialExpressionAnalysisResult>(
                            resultSet.getResults() );
                    Collection<HitListSize> hitlists = lma.computeHitListSizes( results, probe2GeneMap );
                    resultSet.getHitListSizes().clear();
                    resultSet.getHitListSizes().addAll( hitlists );
                    diffS.update( resultSet );
                    log.info( "Did result set" );
                }

            }

            log.info( "Done with " + bas );

        }

        return null;
    }
}
