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

package ubic.gemma.analysis.expression.diff;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ubic.basecode.math.MultipleTestCorrection;
import ubic.basecode.math.metaanalysis.MetaAnalysis;
import ubic.gemma.model.analysis.expression.diff.ContrastResult;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionResultService;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.analysis.expression.diff.GeneDiffExMetaAnalysisService;
import ubic.gemma.model.analysis.expression.diff.GeneDifferentialExpressionMetaAnalysis;
import ubic.gemma.model.analysis.expression.diff.GeneDifferentialExpressionMetaAnalysisResult;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.CompositeSequenceService;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.genome.Gene;
import cern.colt.list.DoubleArrayList;
import cern.jet.stat.Descriptive;

/**
 * @author Paul
 * @version $Id$
 */
@Component
public class DiffExMetaAnalyzerServiceImpl implements DiffExMetaAnalyzerService {

    private static Log log = LogFactory.getLog( DiffExMetaAnalyzerServiceImpl.class );

    private static final double QVALUE_FOR_STORAGE_THRESHOLD = 0.1;

    @Autowired
    private GeneDiffExMetaAnalysisService analysisService;

    @Autowired
    private CompositeSequenceService compositeSequenceService;

    @Autowired
    private DifferentialExpressionResultService differentialExpressionResultService;

    @Override
    public GeneDifferentialExpressionMetaAnalysis persist( GeneDifferentialExpressionMetaAnalysis analysis ) {
        return analysisService.create( analysis );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.analysis.expression.diff.DiffExMetaAnalyserService#analyze(java.util.Collection)
     */
    @Override
    public GeneDifferentialExpressionMetaAnalysis analyze( Collection<Long> analysisResultSetIds ) {

        Collection<ExpressionAnalysisResultSet> resultSets = loadAnalysisResultSet( analysisResultSetIds );

        if ( resultSets.size() < 2 ) {
            throw new IllegalArgumentException( "Must have at least two result sets to meta-analyze" );
        }

        log.info( "Preparing to meta-analyze " + resultSets.size() + " resultSets ..." );
        Map<DifferentialExpressionAnalysisResult, ExpressionAnalysisResultSet> res2set = new HashMap<DifferentialExpressionAnalysisResult, ExpressionAnalysisResultSet>();
        Collection<CompositeSequence> probes = new HashSet<CompositeSequence>();
        for ( ExpressionAnalysisResultSet rs : resultSets ) {

            differentialExpressionResultService.thaw( rs );

            validate( rs );

            Collection<DifferentialExpressionAnalysisResult> results = rs.getResults();
            for ( DifferentialExpressionAnalysisResult r : results ) {
                assert r != null;
                CompositeSequence probe = r.getProbe();
                probes.add( probe );
                res2set.put( r, rs ); // temporary data structure
            }
        }

        log.info( "Matching up by genes ..." );
        Map<CompositeSequence, Collection<Gene>> cs2genes = compositeSequenceService.getGenes( probes );
        Map<Gene, Collection<DifferentialExpressionAnalysisResult>> gene2result = new HashMap<Gene, Collection<DifferentialExpressionAnalysisResult>>();

        // second pass: organize by gene
        int numWithGenes = 0;
        for ( ExpressionAnalysisResultSet rs : resultSets ) {
            Collection<DifferentialExpressionAnalysisResult> results = rs.getResults();
            for ( DifferentialExpressionAnalysisResult r : results ) {
                assert r != null;
                CompositeSequence probe = r.getProbe();
                Collection<Gene> genes = cs2genes.get( probe );
                if ( genes == null || genes.isEmpty() ) continue;
                if ( genes.size() > 1 ) continue;
                Gene gene = genes.iterator().next();
                if ( !gene2result.containsKey( gene ) ) {
                    gene2result.put( gene, new HashSet<DifferentialExpressionAnalysisResult>() );
                }
                gene2result.get( gene ).add( r );
                numWithGenes++;
            }
        }

        if ( numWithGenes == 0 ) {
            log.warn( "No probes were associated with genes" );
            return null;
        }

        log.info( "Computing pvalues ..." );
        DoubleArrayList pvaluesUp = new DoubleArrayList();
        DoubleArrayList pvaluesDown = new DoubleArrayList();
        // third pass: collate to get pvalues. First we have to aggregate within experiment
        List<GeneDifferentialExpressionMetaAnalysisResult> metaAnalysisResultsUp = new ArrayList<GeneDifferentialExpressionMetaAnalysisResult>();
        List<GeneDifferentialExpressionMetaAnalysisResult> metaAnalysisResultsDown = new ArrayList<GeneDifferentialExpressionMetaAnalysisResult>();
        for ( Gene g : gene2result.keySet() ) {
            Collection<DifferentialExpressionAnalysisResult> res4gene = gene2result.get( g );

            Map<ExpressionAnalysisResultSet, Collection<DifferentialExpressionAnalysisResult>> results4geneInOneResultSet = new HashMap<ExpressionAnalysisResultSet, Collection<DifferentialExpressionAnalysisResult>>();
            for ( DifferentialExpressionAnalysisResult r : res4gene ) {
                Collection<ContrastResult> contrasts = r.getContrasts();
                if ( contrasts.size() > 1 ) {
                    // This is not allowed!
                    continue;
                }

                ExpressionAnalysisResultSet rs = res2set.get( r );
                // detect when the result set is repeated.
                if ( !results4geneInOneResultSet.containsKey( rs ) ) {
                    results4geneInOneResultSet.put( rs, new HashSet<DifferentialExpressionAnalysisResult>() );
                }
                results4geneInOneResultSet.get( rs ).add( r );

            }

            /*
             * Compute the pvalues for each resultset.
             */

            DoubleArrayList pvalues4geneUp = new DoubleArrayList();
            DoubleArrayList pvalues4geneDown = new DoubleArrayList();
            DoubleArrayList foldChanges4gene = new DoubleArrayList();
            Collection<DifferentialExpressionAnalysisResult> resultsUsed = new HashSet<DifferentialExpressionAnalysisResult>();

            for ( ExpressionAnalysisResultSet rs : results4geneInOneResultSet.keySet() ) {
                Collection<DifferentialExpressionAnalysisResult> res = results4geneInOneResultSet.get( rs );

                if ( res.isEmpty() ) {
                    // shouldn't happen?
                    log.warn( "Unexpectedly no results in resultSet for gene " + g );
                    continue;
                }

                Double foldChange4GeneInOneResultSet = aggregateFoldChangeForGeneWithinResultSet( res );

                if ( foldChange4GeneInOneResultSet == null ) {
                    // we can't go on.
                    continue;
                }

                // we use the pvalue for the 'best' direction, and set the other to be 1- that. An alternative would be
                // to use _only_ the best one.
                Double pvalue4GeneInOneResultSetUp;
                Double pvalue4GeneInOneResultSetDown;
                if ( foldChange4GeneInOneResultSet < 0 ) {
                    pvalue4GeneInOneResultSetDown = aggregatePvaluesForGeneWithinResultSet( res, false );
                    assert pvalue4GeneInOneResultSetDown != null;
                    pvalue4GeneInOneResultSetUp = 1.0 - pvalue4GeneInOneResultSetDown;
                } else {
                    pvalue4GeneInOneResultSetUp = aggregatePvaluesForGeneWithinResultSet( res, true );
                    assert pvalue4GeneInOneResultSetUp != null;
                    pvalue4GeneInOneResultSetDown = 1.0 - pvalue4GeneInOneResultSetUp;
                }

                // If we have missing values, skip them. (this should be impossible!)
                if ( Double.isNaN( pvalue4GeneInOneResultSetUp ) || Double.isNaN( pvalue4GeneInOneResultSetDown ) ) {
                    continue;
                }

                /*
                 * Now when we correct, we have to 1) bonferroni correct for multiple probes and 2) clip really small
                 * pvalues. We do this now, so that we don't skew the converse pvalues (up vs down).
                 */
                pvalue4GeneInOneResultSetDown = correctAndClip( res, pvalue4GeneInOneResultSetDown );
                pvalue4GeneInOneResultSetUp = correctAndClip( res, pvalue4GeneInOneResultSetUp );

                // results used for this one gene's meta-analysis.
                resultsUsed.addAll( res );

                pvalues4geneUp.add( pvalue4GeneInOneResultSetUp );
                pvalues4geneDown.add( pvalue4GeneInOneResultSetDown );
                foldChanges4gene.add( foldChange4GeneInOneResultSet );

                if ( log.isDebugEnabled() )
                    log.debug( String.format( "%s %.4f %.4f %.1f", g.getOfficialSymbol(), pvalue4GeneInOneResultSetUp,
                            pvalue4GeneInOneResultSetDown, foldChange4GeneInOneResultSet ) );
            }

            /*
             * FIXME what to do if there is just one pvalue for the gene? Is this good enough?
             */
            if ( pvalues4geneUp.size() < 2 ) {
                continue;
            }

            /*
             * Note that this value can be misleading. It should not be used to determine the change that was
             * "looked for". Use the 'upperTail' field instead.
             */
            Double meanLogFoldChange = Descriptive.mean( foldChanges4gene );
            assert meanLogFoldChange != null;

            double fisherPvalueUp = MetaAnalysis.fisherCombinePvalues( pvalues4geneUp );
            double fisherPvalueDown = MetaAnalysis.fisherCombinePvalues( pvalues4geneDown );

            if ( Double.isNaN( fisherPvalueUp ) || Double.isNaN( fisherPvalueDown ) ) {
                continue;
            }

            pvaluesUp.add( fisherPvalueUp );
            GeneDifferentialExpressionMetaAnalysisResult metaAnalysisResultUp = makeMetaAnalysisResult( g, resultsUsed,
                    meanLogFoldChange, fisherPvalueUp, Boolean.TRUE );
            metaAnalysisResultsUp.add( metaAnalysisResultUp );

            pvaluesDown.add( fisherPvalueDown );
            GeneDifferentialExpressionMetaAnalysisResult metaAnalysisResultDown = makeMetaAnalysisResult( g,
                    resultsUsed, meanLogFoldChange, fisherPvalueDown, Boolean.FALSE );
            metaAnalysisResultsDown.add( metaAnalysisResultDown );

            // debug code.
            System.err.println( "Up\t" + g.getOfficialSymbol() + "\t"
                    + StringUtils.join( pvalues4geneUp.toList(), '\t' ) );
            System.err.println( "Down\t" + g.getOfficialSymbol() + "\t"
                    + StringUtils.join( pvalues4geneDown.toList(), '\t' ) );

            if ( log.isDebugEnabled() )
                log.debug( String.format( "Meta-results for %s: pUp=%.4g pdown=%.4g", g.getOfficialSymbol(),
                        fisherPvalueUp, fisherPvalueDown ) );
        } // end loop over genes.

        if ( metaAnalysisResultsDown.isEmpty() ) {
            // can happen if platforms don't have any genes that match etc.
            log.warn( "No meta-analysis results were obtained" );
            return null;
        }

        assert metaAnalysisResultsUp.size() == metaAnalysisResultsDown.size();

        log.info( metaAnalysisResultsUp.size() + " initial meta-analysis results" );

        DoubleArrayList qvaluesUp = MultipleTestCorrection.benjaminiHochberg( pvaluesUp );
        assert qvaluesUp.size() == metaAnalysisResultsUp.size();

        DoubleArrayList qvaluesDown = MultipleTestCorrection.benjaminiHochberg( pvaluesDown );
        assert qvaluesDown.size() == metaAnalysisResultsDown.size();

        /*
         * create the analysis object
         */
        GeneDifferentialExpressionMetaAnalysis analysis = GeneDifferentialExpressionMetaAnalysis.Factory.newInstance();
        analysis.setNumGenesAnalyzed( metaAnalysisResultsUp.size() ); // should be the same for both.
        analysis.setQvalueThresholdForStorage( QVALUE_FOR_STORAGE_THRESHOLD );
        analysis.getResultSetsIncluded().addAll( resultSets );

        // reject values that don't meet the threshold
        selectValues( metaAnalysisResultsUp, qvaluesUp, analysis );
        selectValues( metaAnalysisResultsDown, qvaluesDown, analysis );

        if ( analysis.getResults().isEmpty() ) {
            log.warn( "No results were significant, the analysis will not be completed" );
            return null;
        }

        log.info( "Found " + analysis.getResults().size() + " results meeting meta-qvalue of "
                + QVALUE_FOR_STORAGE_THRESHOLD );

        return analysis;
    }

    /**
     * Bonferonni correct across multiple pvalues. and clip really small pvalues to avoid them taking over.
     * 
     * @param res
     * @param pvalue4GeneInOneResultSetDown
     * @return FIXME make clipping adjustable.
     */
    private Double correctAndClip( Collection<DifferentialExpressionAnalysisResult> res,
            Double pvalue4GeneInOneResultSetDown ) {

        pvalue4GeneInOneResultSetDown = Math.min( pvalue4GeneInOneResultSetDown * res.size(), 1.0 );

        pvalue4GeneInOneResultSetDown = Math.max( pvalue4GeneInOneResultSetDown,
                GeneDifferentialExpressionService.PVALUE_CLIP_THRESHOLD );
        return pvalue4GeneInOneResultSetDown;
    }

    /**
     * @param res
     * @return
     */
    private Double aggregateFoldChangeForGeneWithinResultSet( Collection<DifferentialExpressionAnalysisResult> res ) {
        assert !res.isEmpty();
        Double bestPvalue = Double.MAX_VALUE;
        DifferentialExpressionAnalysisResult best = null;

        for ( DifferentialExpressionAnalysisResult r : res ) {
            Double pvalue = r.getPvalue();
            if ( pvalue == null ) continue;

            assert r.getContrasts().size() < 2 : "Wrong number of contrasts: " + r.getContrasts().size();

            if ( pvalue < bestPvalue ) {
                bestPvalue = pvalue;
                best = r;
            }
        }

        if ( best == null ) return null;

        assert best != null && best.getContrasts().size() == 1;
        return best.getContrasts().iterator().next().getLogFoldChange();
    }

    /**
     * For cases where there is more than one result for a gene in a data set (due to multiple probes), we aggregate
     * them. Method: take the best pvalue.
     * 
     * @param res
     * @return
     */
    private Double aggregatePvaluesForGeneWithinResultSet( Collection<DifferentialExpressionAnalysisResult> res,
            boolean upperTail ) {
        assert !res.isEmpty();
        Double bestPvalue = Double.MAX_VALUE;
        for ( DifferentialExpressionAnalysisResult r : res ) {

            Double pvalue = r.getPvalue();

            if ( pvalue == null || Double.isNaN( pvalue ) ) {
                continue;
            }

            /*
             * P-values stored with the per-experiment analyses are two-sided. To convert to a one-sided value, we
             * consider just one tail.
             */
            pvalue /= 2.0;

            /*
             * Next decide if we should use the "up" or "down" part of the pvalue. We examine the fold change, and
             * associate the original pvalue with that tail. We then switch tails if necessary.
             */
            assert r.getContrasts().size() < 2 : "Wrong number of contrasts: " + r.getContrasts().size();
            Double logFoldChange = r.getContrasts().iterator().next().getLogFoldChange();

            if ( ( upperTail && logFoldChange < 0 ) || ( !upperTail && logFoldChange > 0 ) ) {
                pvalue = 1.0 - pvalue;
            }

            if ( pvalue < bestPvalue ) {
                bestPvalue = pvalue;
            }
        }

        assert bestPvalue <= 1.0 && bestPvalue >= 0.0;

        /*
         * Because each gene gets two chances to be significant (up and down) we considered to _also_ do a Bonferroni
         * correction. This would be too conservative because the pair of pvalues are correlated. If one pvalue is good
         * the other is bad. There is no need for this correction.
         */
        // return Math.min( 1.0, 2.0 * bestPvalue );
        return bestPvalue;
    }

    /**
     * @param analysisResultSetIds
     * @return
     */
    private Collection<ExpressionAnalysisResultSet> loadAnalysisResultSet( Collection<Long> analysisResultSetIds ) {
        Collection<ExpressionAnalysisResultSet> resultSets = new HashSet<ExpressionAnalysisResultSet>();

        for ( Long analysisResultSetId : analysisResultSetIds ) {
            ExpressionAnalysisResultSet expressionAnalysisResultSet = this.differentialExpressionResultService
                    .loadAnalysisResultSet( analysisResultSetId );

            if ( expressionAnalysisResultSet == null ) {
                log.warn( "No diff ex result set with ID=" + analysisResultSetId );
                throw new IllegalArgumentException( "No diff ex result set with ID=" + analysisResultSetId );
            }

            resultSets.add( expressionAnalysisResultSet );
        }
        return resultSets;
    }

    /**
     * @param g
     * @param resultsUsed
     * @param meanLogFoldChange
     * @param fisherPvalue
     * @param upperTail
     * @return
     */
    private GeneDifferentialExpressionMetaAnalysisResult makeMetaAnalysisResult( Gene g,
            Collection<DifferentialExpressionAnalysisResult> resultsUsed, Double meanLogFoldChange,
            double fisherPvalue, boolean upperTail ) {

        GeneDifferentialExpressionMetaAnalysisResult metaAnalysisResult = GeneDifferentialExpressionMetaAnalysisResult.Factory
                .newInstance();
        metaAnalysisResult.setMetaPvalue( fisherPvalue );
        metaAnalysisResult.getResultsUsed().addAll( resultsUsed );
        metaAnalysisResult.setGene( g );
        metaAnalysisResult.setUpperTail( upperTail );
        return metaAnalysisResult;
    }

    /**
     * Extract the results we keep, that meet the threshold for qvalue
     * 
     * @param metaAnalysisResults
     * @param qvalues
     * @param analysis
     */
    private void selectValues( List<GeneDifferentialExpressionMetaAnalysisResult> metaAnalysisResults,
            DoubleArrayList qvalues, GeneDifferentialExpressionMetaAnalysis analysis ) {
        //
        int i = 0;
        assert metaAnalysisResults.size() == qvalues.size();
        for ( GeneDifferentialExpressionMetaAnalysisResult r : metaAnalysisResults ) {
            double metaQvalue = qvalues.get( i );
            r.setMetaQvalue( metaQvalue );

            if ( metaQvalue < QVALUE_FOR_STORAGE_THRESHOLD ) {
                analysis.getResults().add( r );
                if ( log.isDebugEnabled() )
                    log.debug( "Keeping " + r.getGene().getOfficialSymbol() + ", q=" + metaQvalue );
            }

            i++;
        }
    }

    /**
     * @param rs
     */
    private void validate( ExpressionAnalysisResultSet rs ) {
        if ( rs.getExperimentalFactors().size() > 1 ) {
            throw new IllegalArgumentException( "Cannot do a meta-analysis on interaction terms" );
        }

        ExperimentalFactor factor = rs.getExperimentalFactors().iterator().next();
        if ( factor.getFactorValues().size() > 2 ) {
            /*
             * Note that this doesn't account for continuous factors.
             */
            throw new IllegalArgumentException(
                    "Cannot do a meta-analysis including a factor that has more than two levels: " + factor + " has "
                            + factor.getFactorValues().size() + " levels" );
        }
    }
}
