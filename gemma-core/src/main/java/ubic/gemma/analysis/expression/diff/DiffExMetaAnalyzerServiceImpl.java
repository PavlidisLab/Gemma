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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import cern.colt.list.DoubleArrayList;
import cern.jet.stat.Descriptive;

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
import ubic.gemma.model.genome.Gene;

/**
 * TODO Document Me
 * 
 * @author Paul
 * @version $Id$
 */
@Component
public class DiffExMetaAnalyzerServiceImpl implements DiffExMetaAnalyzerService {

    private static Log log = LogFactory.getLog( DiffExMetaAnalyzerServiceImpl.class );

    @Autowired
    private CompositeSequenceService compositeSequenceService;

    @Autowired
    private GeneDiffExMetaAnalysisService analysisService;

    @Autowired
    private DifferentialExpressionResultService differentialExpressionResultService;

    private static final double QVALUE_FOR_STORAGE_THRESHOLD = 0.1;

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.analysis.expression.diff.DiffExMetaAnalyserService#analyze(java.util.Collection)
     */
    @Override
    public GeneDifferentialExpressionMetaAnalysis analyze( Collection<Long> analysisResultSetIds,
            String name, String description ) {

    	Collection<ExpressionAnalysisResultSet> resultSets = loadAnalysisResultSet(analysisResultSetIds);
    	
        if ( resultSets.size() < 2 ) {
            throw new IllegalArgumentException( "Must have at least two result sets to meta-analyze" );
        }
        /*
         * FIXME: add validation such as checking that the factors have two levels.
         */

        log.info( "Preparing to meta-analyze " + resultSets.size() + " resultSets ..." );
        Map<DifferentialExpressionAnalysisResult, ExpressionAnalysisResultSet> res2set = new HashMap<DifferentialExpressionAnalysisResult, ExpressionAnalysisResultSet>();
        Collection<CompositeSequence> probes = new HashSet<CompositeSequence>();
        for ( ExpressionAnalysisResultSet rs : resultSets ) {
            differentialExpressionResultService.thaw( rs );
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
            }
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
                Double pvalue4GeneInOneResultSetUp = aggregatePvaluesForGeneWithinResultSet( res, true );

                if ( pvalue4GeneInOneResultSetUp == null ) continue; // FIXME temporary

                Double pvalue4GeneInOneResultSetDown = aggregatePvaluesForGeneWithinResultSet( res, false );
                Double foldChange4GeneInOneResultSet = aggregateFoldChangeForGeneWithinResultSet( res );

                if ( foldChange4GeneInOneResultSet == null ) continue; // FIXME temporary

                resultsUsed.addAll( res );

                /*
                 * FIXME blunt very low pvalues? Or do a jackknife?
                 */

                // If we have missing values, skip them.
                if ( Double.isNaN( pvalue4GeneInOneResultSetUp ) || Double.isNaN( pvalue4GeneInOneResultSetDown ) ) {
                    continue;
                }

                pvalues4geneUp.add( pvalue4GeneInOneResultSetUp );
                pvalues4geneDown.add( pvalue4GeneInOneResultSetDown );
                foldChanges4gene.add( foldChange4GeneInOneResultSet );

                if ( log.isDebugEnabled() )
                    log.debug( String.format( "%s %.4f %.4f %.1f", g.getOfficialSymbol(), pvalue4GeneInOneResultSetUp,
                            pvalue4GeneInOneResultSetDown, foldChange4GeneInOneResultSet ) );
            }

            Double meanLogFoldChange = Descriptive.mean( foldChanges4gene );

            double fisherPvalueUp = pvalues4geneUp.size() == 1 ? pvalues4geneUp.get( 0 ) : MetaAnalysis
                    .fisherCombinePvalues( pvalues4geneUp );

            double fisherPvalueDown = pvalues4geneDown.size() == 1 ? pvalues4geneDown.get( 0 ) : MetaAnalysis
                    .fisherCombinePvalues( pvalues4geneDown );

            if ( Double.isNaN( fisherPvalueUp ) || Double.isNaN( fisherPvalueDown ) || Double.isNaN( meanLogFoldChange ) ) {
                continue;
            }

            pvaluesUp.add( fisherPvalueUp );
            GeneDifferentialExpressionMetaAnalysisResult metaAnalysisResultUp = makeMetaAnalysisResult( g, resultsUsed,
                    meanLogFoldChange, fisherPvalueUp, true );
            metaAnalysisResultsUp.add( metaAnalysisResultUp );

            pvaluesDown.add( fisherPvalueDown );
            GeneDifferentialExpressionMetaAnalysisResult metaAnalysisResultDown = makeMetaAnalysisResult( g,
                    resultsUsed, meanLogFoldChange, fisherPvalueDown, false );
            metaAnalysisResultsDown.add( metaAnalysisResultDown );

            if ( log.isDebugEnabled() )
                log.debug( String.format( "Meta-results for %s: pUp=%.4g pdown=%.4g", g.getOfficialSymbol(),
                        fisherPvalueUp, fisherPvalueDown ) );
        }

        assert metaAnalysisResultsUp.size() == metaAnalysisResultsDown.size();

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

        selectValues( metaAnalysisResultsUp, qvaluesUp, analysis );
        selectValues( metaAnalysisResultsDown, qvaluesDown, analysis );

        if ( analysis.getResults().isEmpty() ) {
            log.warn( "No results were significant, the analysis will not be saved" );
            return null;
        }

        // Save results only when name is specified.
        if ( name != null ) {
            analysis.setName( name );
            analysis.setDescription( description );

            // FIXME might not want to save this here.
            log.info( "Saving " + analysis.getResults().size() + " results meeting meta-qvalue of "
                    + QVALUE_FOR_STORAGE_THRESHOLD );
            analysis = analysisService.create( analysis );
        }

        return analysis;
    }

	private Collection<ExpressionAnalysisResultSet> loadAnalysisResultSet(Collection<Long> analysisResultSetIds) {
		Collection<ExpressionAnalysisResultSet> resultSets = new HashSet<ExpressionAnalysisResultSet>();
		
		for (Long analysisResultSetId : analysisResultSetIds) {
			ExpressionAnalysisResultSet expressionAnalysisResultSet = this.differentialExpressionResultService.loadAnalysisResultSet(analysisResultSetId);
			
	        if ( expressionAnalysisResultSet == null ) {
	            log.warn( "No diff ex result set with ID=" + analysisResultSetId );
                throw new IllegalArgumentException( "No diff ex result set with ID=" + analysisResultSetId );
	        }
		
	        resultSets.add(expressionAnalysisResultSet);
		}
		return resultSets;
	}

    private Double aggregateFoldChangeForGeneWithinResultSet( Collection<DifferentialExpressionAnalysisResult> res ) {
        assert !res.isEmpty();
        Double bestPvalue = Double.MAX_VALUE;
        DifferentialExpressionAnalysisResult best = res.iterator().next();
        for ( DifferentialExpressionAnalysisResult r : res ) {
            Double pvalue = r.getPvalue();
            assert r.getContrasts().size() < 2 : "Wrong number of contrasts: " + r.getContrasts().size();

            // temporary, as my test database doesn't have all contrasts computed.
            if ( r.getContrasts().isEmpty() ) return null;

            if ( pvalue < bestPvalue ) {
                bestPvalue = pvalue;
                best = r;
            }
        }

        assert best != null && best.getContrasts().size() == 1;
        return best.getContrasts().iterator().next().getLogFoldChange();
    }

    /**
     * For cases where there is more than one result for a gene in a data set (due to multiple probes), we aggregate
     * them. Method: take the best pvalue, but Bonferonni correct it.
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
             * Pvalues stored are two-sided. To convert to a one-sided value, we consider just one tail.
             */
            pvalue /= 2.0;

            if ( pvalue < bestPvalue ) {
                assert r.getContrasts().size() < 2 : "Wrong number of contrasts: " + r.getContrasts().size();

                // temporary, as my test database doesn't have all contrasts computed.
                if ( r.getContrasts().isEmpty() ) return null;

                Double logFoldChange = r.getContrasts().iterator().next().getLogFoldChange();

                if ( upperTail ) {
                    if ( logFoldChange < 0 ) {
                        bestPvalue = 1.0 - pvalue;
                    } else {
                        bestPvalue = pvalue;
                    }
                } else {
                    if ( logFoldChange < 0 ) {
                        bestPvalue = pvalue;
                    } else {
                        bestPvalue = 1.0 - pvalue;
                    }
                }

            }
        }

        assert bestPvalue <= 1.0 && bestPvalue >= 0.0;
        // Bonferonni correct across multiple pvalues.
        return Math.min( bestPvalue * res.size(), 1.0 );
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
        metaAnalysisResult.setMeanLogFoldChange( meanLogFoldChange );
        metaAnalysisResult.setGene( g );
        metaAnalysisResult.setUpperTail( upperTail );
        return metaAnalysisResult;
    }

    /**
     * @param metaAnalysisResults
     * @param qvalues
     * @param analysis
     */
    private void selectValues( List<GeneDifferentialExpressionMetaAnalysisResult> metaAnalysisResults,
            DoubleArrayList qvalues, GeneDifferentialExpressionMetaAnalysis analysis ) {
        // Extract the results we keep
        int i = 0;
        assert metaAnalysisResults.size() == qvalues.size();
        for ( GeneDifferentialExpressionMetaAnalysisResult r : metaAnalysisResults ) {
            r.setMetaQvalue( qvalues.get( i ) );

            if ( qvalues.get( i ) < QVALUE_FOR_STORAGE_THRESHOLD ) analysis.getResults().add( r );
            i++;
        }
    }
}
