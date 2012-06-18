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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import ubic.gemma.model.analysis.Direction;
import ubic.gemma.model.analysis.expression.diff.ContrastResult;
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
 * Used to fix result sets that were not run after institution of precomputed hitlists. Steals code liberally from
 * LinearModelAnalyzer.
 * 
 * @author Paul
 * @version $Id$
 */
public class PopulateHitListsCli extends ExpressionExperimentManipulatingCLI {

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.util.AbstractCLI#doWork(java.lang.String[])
     */
    @Override
    protected Exception doWork( String[] args ) {

        super.processCommandLine( "populate differential expression hit lists", args );

        DifferentialExpressionAnalysisService diffS = this.getBean( DifferentialExpressionAnalysisService.class );

        DifferentialExpressionResultService resultService = this.getBean( DifferentialExpressionResultService.class );

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
                for ( ExpressionAnalysisResultSet resultSet : analysis.getResultSets() ) {
                    if ( resultSet.getNumberOfGenesTested() == null ) {
                        log.info( "Info needs to be filled in for " + expressionExperiment );
                        Collection<DifferentialExpressionAnalysisResult> results = resultSet.getResults();

                        log.info( "Thawing ..." );
                        resultService.thaw( resultSet );

                        log.info( "Getting probe-gene mapping ..." );
                        Map<CompositeSequence, Collection<Gene>> probeToGeneMap = getProbeToGeneMap( results );

                        log.info( "Computing ..." );
                        Collection<HitListSize> hitListSizes = computeHitListSizes( results, probeToGeneMap );

                        int numberOfProbesTested = results.size();
                        int numberOfGenesTested = getNumberOfGenesTested( results, probeToGeneMap );

                        /*
                         * the baseline group should be populated too, but I'm a little afraid of trying to guess this
                         * now, because if it's wrong, things are quite royally screwed up. So we update that by
                         * re-running the analysis.
                         */
                        // Map<ExperimentalFactor, FactorValue> baselineConditions = ExperimentalDesignUtils
                        // .getBaselineConditions( samplesUsed, resultSet.getExperimentalFactors() );

                        resultSet.setNumberOfGenesTested( numberOfGenesTested );
                        resultSet.setNumberOfProbesTested( numberOfProbesTested );
                        resultSet.getHitListSizes().clear();
                        resultSet.getHitListSizes().addAll( hitListSizes );

                        log.info( "Updating ..." );
                        resultService.update( resultSet );

                    }
                }
            }

        }

        return null;
    }

    /**
     * @param resultList
     * @param probeToGeneMap
     * @return
     */
    private int getNumberOfGenesTested( Collection<DifferentialExpressionAnalysisResult> resultList,
            Map<CompositeSequence, Collection<Gene>> probeToGeneMap ) {

        Collection<Gene> gs = new HashSet<Gene>();
        for ( DifferentialExpressionAnalysisResult d : resultList ) {
            CompositeSequence probe = d.getProbe();
            if ( probeToGeneMap.containsKey( probe ) ) {
                gs.addAll( probeToGeneMap.get( probe ) );
            }
        }
        return gs.size();
    }

    /**
     * @param args
     */
    public static void main( String[] args ) {
        PopulateHitListsCli c = new PopulateHitListsCli();
        c.doWork( args );

    }

    /**
     * Needed to compute the number of genes tested/detected.
     * 
     * @param resultLists
     * @return
     */
    private Map<CompositeSequence, Collection<Gene>> getProbeToGeneMap(
            Collection<DifferentialExpressionAnalysisResult> resultList ) {
        CompositeSequenceService compositeSequenceService = this.getBean( CompositeSequenceService.class );

        Map<CompositeSequence, Collection<Gene>> result = new HashMap<CompositeSequence, Collection<Gene>>();

        for ( DifferentialExpressionAnalysisResult d : resultList ) {
            CompositeSequence probe = d.getProbe();
            result.put( probe, new HashSet<Gene>() );
        }

        assert !result.isEmpty();
        return compositeSequenceService.getGenes( result.keySet() );

    }

    private Collection<HitListSize> computeHitListSizes( Collection<DifferentialExpressionAnalysisResult> results,
            Map<CompositeSequence, Collection<Gene>> probeToGeneMap ) {
        Collection<HitListSize> hitListSizes = new HashSet<HitListSize>();

        assert probeToGeneMap != null;

        // maps from Doubles are a bit dodgy...
        Map<Double, Integer> upCounts = new HashMap<Double, Integer>();
        Map<Double, Integer> downCounts = new HashMap<Double, Integer>();
        Map<Double, Integer> eitherCounts = new HashMap<Double, Integer>();

        Map<Double, Integer> upCountGenes = new HashMap<Double, Integer>();
        Map<Double, Integer> downCountGenes = new HashMap<Double, Integer>();
        Map<Double, Integer> eitherCountGenes = new HashMap<Double, Integer>();

        for ( DifferentialExpressionAnalysisResult r : results ) {

            Double corrP = r.getCorrectedPvalue();
            if ( corrP == null ) continue;

            int numGenes = 0;

            CompositeSequence probe = r.getProbe();
            if ( probeToGeneMap.containsKey( probe ) ) {
                Collection<Gene> genes = probeToGeneMap.get( probe );
                numGenes = genes.size();
            }

            Collection<ContrastResult> crs = r.getContrasts();
            boolean up = false;
            boolean down = false;
            for ( ContrastResult cr : crs ) {
                Double lf = cr.getLogFoldChange();
                if ( lf < 0 ) {
                    down = true;
                } else if ( lf > 0 ) {
                    up = true;
                }
            }

            for ( double thresh : qValueThresholdsForHitLists ) {

                if ( !upCounts.containsKey( thresh ) ) {
                    upCounts.put( thresh, 0 );
                    upCountGenes.put( thresh, 0 );
                }
                if ( !downCounts.containsKey( thresh ) ) {
                    downCounts.put( thresh, 0 );
                    downCountGenes.put( thresh, 0 );
                }
                if ( !eitherCounts.containsKey( thresh ) ) {
                    eitherCounts.put( thresh, 0 );
                    eitherCountGenes.put( thresh, 0 );
                }

                if ( corrP < thresh ) {
                    if ( up ) {
                        upCounts.put( thresh, upCounts.get( thresh ) + 1 );
                        upCountGenes.put( thresh, upCountGenes.get( thresh ) + numGenes );
                    }
                    if ( down ) {
                        downCounts.put( thresh, downCounts.get( thresh ) + 1 );
                        downCountGenes.put( thresh, downCountGenes.get( thresh ) + numGenes );
                    }

                    eitherCounts.put( thresh, eitherCounts.get( thresh ) + 1 );
                    eitherCountGenes.put( thresh, eitherCountGenes.get( thresh ) + numGenes );
                }
            }

        }

        for ( double thresh : qValueThresholdsForHitLists ) {

            // Ensure we don't set values to null.
            Integer up = upCounts.get( thresh ) == null ? 0 : upCounts.get( thresh );
            Integer down = downCounts.get( thresh ) == null ? 0 : downCounts.get( thresh );
            Integer either = eitherCounts.get( thresh ) == null ? 0 : eitherCounts.get( thresh );

            Integer upGenes = upCountGenes.get( thresh ) == null ? 0 : upCountGenes.get( thresh );
            Integer downGenes = downCountGenes.get( thresh ) == null ? 0 : downCountGenes.get( thresh );
            Integer eitherGenes = eitherCountGenes.get( thresh ) == null ? 0 : eitherCountGenes.get( thresh );

            HitListSize upS = HitListSize.Factory.newInstance( thresh, up, Direction.UP, upGenes );
            HitListSize downS = HitListSize.Factory.newInstance( thresh, down, Direction.DOWN, downGenes );
            HitListSize eitherS = HitListSize.Factory.newInstance( thresh, either, Direction.EITHER, eitherGenes );

            hitListSizes.add( upS );
            hitListSizes.add( downS );
            hitListSizes.add( eitherS );
        }
        return hitListSizes;
    }

    /**
     * Preset levels for which we will store the HitListSizes.
     */
    private static final double[] qValueThresholdsForHitLists = new double[] { 0.001, 0.005, 0.01, 0.05, 0.1 };

}
