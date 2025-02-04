/*
 * The Gemma project
 *
 * Copyright (c) 2013 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.core.analysis.expression.diff;

import cern.colt.list.DoubleArrayList;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ubic.basecode.math.metaanalysis.MetaAnalysis;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionValueObject;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.expression.experiment.*;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.gene.GeneValueObject;
import ubic.gemma.persistence.service.analysis.expression.diff.DifferentialExpressionResultService;
import ubic.gemma.persistence.util.IdentifiableUtils;

import java.util.*;

/**
 * Provides access to {@link DifferentialExpressionAnalysisResult}s and meta-analysis results.
 *
 * @author keshav, anton
 */
@Component
public class GeneDifferentialExpressionServiceImpl implements GeneDifferentialExpressionService {

    private static final String FV_SEP = ", ";
    private static final int MAX_PVAL = 1;
    private static final Log log = LogFactory.getLog( GeneDifferentialExpressionServiceImpl.class );

    private final DifferentialExpressionResultService differentialExpressionResultService;

    @Autowired
    public GeneDifferentialExpressionServiceImpl(
            DifferentialExpressionResultService differentialExpressionResultService ) {
        this.differentialExpressionResultService = differentialExpressionResultService;
    }

    @Override
    public ExperimentalFactorValueObject configExperimentalFactorValueObject( ExperimentalFactor ef ) {
        ExperimentalFactorValueObject efvo = new ExperimentalFactorValueObject( ef.getId() );
        efvo.setName( ef.getName() );
        efvo.setDescription( ef.getDescription() );
        Characteristic category = ef.getCategory();
        if ( category != null ) {
            efvo.setCategory( category.getCategory() );
            efvo.setCategoryUri( category.getCategoryUri() );
        }
        Collection<FactorValue> fvs = ef.getFactorValues();
        StringBuilder factorValuesAsString = new StringBuilder( StringUtils.EMPTY );

        for ( FactorValue fv : fvs ) {
            String fvName = fv.toString();
            if ( StringUtils.isNotBlank( fvName ) ) {
                factorValuesAsString.append( fvName ).append( GeneDifferentialExpressionServiceImpl.FV_SEP );
            }
        }

        /* clean up the start and end of the string */
        factorValuesAsString = new StringBuilder(
                StringUtils.remove( factorValuesAsString.toString(), ef.getName() + ":" ) );
        factorValuesAsString = new StringBuilder( StringUtils
                .removeEnd( factorValuesAsString.toString(), GeneDifferentialExpressionServiceImpl.FV_SEP ) );

        /*
         * Preformat the factor name; due to Ext PropertyGrid limitations we can't do this on the client.
         */
        efvo.setName( ef.getName() + " (" + StringUtils.abbreviate( factorValuesAsString.toString(), 50 ) + ")" );

        efvo.setFactorValues( factorValuesAsString.toString() );
        return efvo;
    }

    private BioAssaySetValueObject configExpressionExperimentValueObject( BioAssaySetValueObject ee ) {

        // if ( ee instanceof ExpressionExperiment ) {
        // eevo.setShortName( ( ( ExpressionExperiment ) ee ).getShortName() );
        // } else if ( ee instanceof ExpressionExperimentSubSet ) {
        // eevo.setShortName( "Subset of "
        // + ( ( ExpressionExperimentSubSet ) ee ).getSourceExperiment().getShortName() );
        // eevo.setSourceExperiment( ( ( ExpressionExperimentSubSet ) ee ).getSourceExperiment().getId() );
        // }
        // ee.setExternalUri( AnchorTagUtil.getExpressionExperimentUrl( ee.getId() ) );
        return ee;
    }

    @Override
    public Collection<DifferentialExpressionValueObject> getDifferentialExpression( Gene gene,
            Collection<BioAssaySet> ees ) {
        StopWatch timer = new StopWatch();
        timer.start();
        Collection<DifferentialExpressionValueObject> devos = new ArrayList<>();

        if ( gene == null )
            return devos;

        Map<BioAssaySetValueObject, List<DifferentialExpressionValueObject>> results = differentialExpressionResultService
                .find( gene, IdentifiableUtils.getIds( ees ) );
        timer.stop();
        if ( timer.getTime() > 1000 ) {
            GeneDifferentialExpressionServiceImpl.log.info( "Diff ex results: " + timer.getTime() + " ms" );
        }
        return this.postProcessDiffExResults( results );
    }

    @Override
    public Collection<DifferentialExpressionValueObject> getDifferentialExpression( Gene gene,
            BioAssaySet ee, double threshold, int limit ) {
        StopWatch timer = new StopWatch();
        timer.start();
        Collection<DifferentialExpressionValueObject> devos = new ArrayList<>();

        if ( gene == null )
            return devos;

        Map<BioAssaySetValueObject, List<DifferentialExpressionValueObject>> results = differentialExpressionResultService
                .find( gene, Collections.singleton( ee.getId() ), threshold, limit );
        timer.stop();
        if ( timer.getTime() > 1000 ) {
            GeneDifferentialExpressionServiceImpl.log.info( "Diff ex results: " + timer.getTime() + " ms" );
        }
        return this.postProcessDiffExResults( results );
    }

    @Override
    public Collection<DifferentialExpressionValueObject> getDifferentialExpression( Gene gene, double threshold,
            Collection<DiffExpressionSelectedFactorCommand> factorMap ) {
        StopWatch timer = new StopWatch();
        timer.start();
        Collection<DifferentialExpressionValueObject> result = new ArrayList<>();

        if ( gene == null )
            return result;

        Map<BioAssaySetValueObject, List<DifferentialExpressionValueObject>> rawDiffEx = differentialExpressionResultService
                .find( gene, threshold, -1 );

        // Collection<DifferentialExpressionValueObject> rawProcResults = postProcessDiffExResults( gene, threshold,
        // rawDiffEx );

        Map<Long, DiffExpressionSelectedFactorCommand> eeId2FactorCommand = new HashMap<>();
        for ( DiffExpressionSelectedFactorCommand dsfc : factorMap ) {
            eeId2FactorCommand.put( dsfc.getEeId(), dsfc );
        }

        for ( BioAssaySetValueObject ee : rawDiffEx.keySet() ) {
            for ( DifferentialExpressionValueObject raw : rawDiffEx.get( ee ) ) {
                if ( eeId2FactorCommand.containsKey( raw.getExpressionExperiment().getId() ) ) {
                    DiffExpressionSelectedFactorCommand factorCommandForEE = eeId2FactorCommand
                            .get( raw.getExpressionExperiment().getId() );

                    assert !raw.getExperimentalFactors().isEmpty();

                    // interaction term?
                    if ( raw.getExperimentalFactors().size() > 1 ) {
                        continue;
                    }

                    ExperimentalFactorValueObject efvo = raw.getExperimentalFactors().iterator().next();
                    if ( factorCommandForEE.getEfId().equals( efvo.getId() ) ) {
                        result.add( raw );
                    }
                }
            }
        }
        timer.stop();
        if ( timer.getTime() > 1000 ) {
            GeneDifferentialExpressionServiceImpl.log.info( "Diff ex results: " + timer.getTime() + " ms" );
        }
        return result;
    }

    @Override
    public Collection<DifferentialExpressionValueObject> getDifferentialExpression( Gene gene, double threshold,
            int limit ) {

        Collection<DifferentialExpressionValueObject> devos = new ArrayList<>();

        if ( gene == null )
            return devos;

        StopWatch timer = new StopWatch();
        timer.start();

        Map<BioAssaySetValueObject, List<DifferentialExpressionValueObject>> results = differentialExpressionResultService
                .find( gene, threshold, limit );

        if ( timer.getTime() > 1000 ) {
            GeneDifferentialExpressionServiceImpl.log.info( "Diff raw ex results: " + timer.getTime() + " ms" );
        }

        return this.postProcessDiffExResults( results );
    }

    @Override
    public DifferentialExpressionMetaAnalysisValueObject getDifferentialExpressionMetaAnalysis( double threshold,
            Gene g, Map<Long, Long> eeFactorsMap, Collection<BioAssaySet> activeExperiments ) {

        StopWatch timer = new StopWatch();
        timer.start();

        /*
         * Get results for each active experiment on given gene. Handling the threshold check below since we ignore this
         * for the meta analysis. The results returned are for all factors, not just the factors we are seeking.
         */
        Map<BioAssaySetValueObject, List<DifferentialExpressionValueObject>> resultsMap = differentialExpressionResultService
                .find( g, IdentifiableUtils.getIds( activeExperiments ) );

        GeneDifferentialExpressionServiceImpl.log
                .debug( resultsMap.size() + " results for " + g + " in " + activeExperiments );

        DifferentialExpressionMetaAnalysisValueObject mavo = new DifferentialExpressionMetaAnalysisValueObject();

        DoubleArrayList pvaluesToCombine = new DoubleArrayList();

        /* a gene can have multiple probes that map to it, so store one diff value object for each probe */
        Collection<DifferentialExpressionValueObject> devos = new ArrayList<>();

        Collection<Long> eesThatMetThreshold = new HashSet<>();

        for ( BioAssaySetValueObject ee : resultsMap.keySet() ) {

            BioAssaySetValueObject eevo = this.configExpressionExperimentValueObject( ee );

            Collection<DifferentialExpressionValueObject> proberesults = resultsMap.get( ee );

            Collection<DifferentialExpressionValueObject> filteredResults = new HashSet<>();
            for ( DifferentialExpressionValueObject r : proberesults ) {
                Collection<ExperimentalFactorValueObject> efs = r.getExperimentalFactors();

                assert efs.size() > 0;
                if ( efs.size() > 1 ) {
                    // We always ignore interaction effects.
                    continue;
                }

                ExperimentalFactorValueObject ef = efs.iterator().next();

                /*
                 * note that we don't care about the reverse: the eefactorsmap can have stuff we don't need. We focus on
                 * the experiments because they are easy to select & secure. The eefactorsmap provides additional
                 * details.
                 */
                assert eeFactorsMap.containsKey( ee.getId() ) : "eeFactorsMap does not contain ee=" + ee.getId();

                Long sfId = eeFactorsMap.get( ee.getId() );
                if ( !ef.getId().equals( sfId ) ) {
                    /*
                     * Screen out factors we're not using.
                     */
                    continue;
                }

                /* filtered result with chosen factor */
                filteredResults.add( r );

            }

            if ( filteredResults.size() == 0 ) {
                GeneDifferentialExpressionServiceImpl.log.warn( "No result for ee=" + ee );
                continue;
            }

            /*
             * For the diff expression meta analysis, ignore threshold. Select the 'best' penalized probe if multiple
             * probes map to the same gene.
             */
            DifferentialExpressionValueObject res = this.findMinPenalizedProbeResult( filteredResults );
            if ( res == null )
                continue;

            Double p = res.getP();
            if ( p == null )
                continue;

            /*
             * Moderate the pvalues by setting all values to be no smaller than PVALUE_CLIP_THRESHOLD
             */
            pvaluesToCombine.add( Math.max( p, GeneDifferentialExpressionService.PVALUE_CLIP_THRESHOLD ) );

            /* for each filtered result, set up a devo (contains only results with chosen factor) */

            for ( DifferentialExpressionValueObject r : filteredResults ) {

                Collection<ExperimentalFactorValueObject> efs = r.getExperimentalFactors();
                if ( efs == null ) {
                    // This should not happen any more, but just in case.
                    GeneDifferentialExpressionServiceImpl.log
                            .warn( "No experimentalFactor(s) for DifferentialExpressionAnalysisResult: " + r.getId() );
                    continue;
                }

                Boolean metThreshold = r.getCorrP() != null && ( r.getCorrP() <= threshold );
                r.setMetThreshold( metThreshold );

                if ( metThreshold ) {
                    eesThatMetThreshold.add( eevo.getId() );
                }

                Boolean fisherContribution = r.equals( res );
                r.setFisherContribution( fisherContribution );

            }

        }

        /*
         * Meta-analysis part.
         */
        double fisherPval = MetaAnalysis.fisherCombinePvalues( pvaluesToCombine );
        mavo.setFisherPValue( fisherPval );
        mavo.setGene( new GeneValueObject( g ) );
        mavo.setActiveExperiments( activeExperiments );
        mavo.setProbeResults( devos );
        mavo.setNumMetThreshold( eesThatMetThreshold.size() );
        mavo.setSortKey();

        timer.stop();
        if ( timer.getTime() > 1000 ) {
            GeneDifferentialExpressionServiceImpl.log.info( "Meta-analysis results: " + timer.getTime() + " ms" );
        }

        return mavo;
    }

    /**
     * When n probes map to the sa
     *
     * @return the result with the min p value.
     */
    private DifferentialExpressionValueObject findMinPenalizedProbeResult(
            Collection<DifferentialExpressionValueObject> results ) {

        DifferentialExpressionValueObject minResult = null;

        int numProbesForGene = results.size();
        if ( numProbesForGene == 1 ) {
            return results.iterator().next();
        }

        double min = 0;
        int i = 0;
        for ( DifferentialExpressionValueObject r : results ) {

            /*
             * TODO use the contrasts.
             */
            //r.getContrasts();

            if ( r.getP() == null )
                continue;
            /* penalize pvals */
            double pval = r.getP() * numProbesForGene;
            if ( pval > GeneDifferentialExpressionServiceImpl.MAX_PVAL )
                pval = GeneDifferentialExpressionServiceImpl.MAX_PVAL;

            /* find the best pval */
            if ( i == 0 || pval <= min ) {
                min = pval;
                minResult = r;
                minResult.setP( min );
            }

            i++;
        }

        return minResult;

    }

    private Collection<DifferentialExpressionValueObject> postProcessDiffExResults(
            Map<BioAssaySetValueObject, List<DifferentialExpressionValueObject>> results ) {
        StopWatch timer = new StopWatch();
        timer.start();

        Collection<DifferentialExpressionValueObject> devos = new ArrayList<>();

        /*
         * convert to DEVOs
         */
        for ( BioAssaySetValueObject eevo : results.keySet() ) {

            Collection<DifferentialExpressionValueObject> probeResults = results.get( eevo );

            assert probeResults != null && !probeResults.isEmpty();

            for ( DifferentialExpressionValueObject r : probeResults ) {

                // this doesn't do anything any more since we're already working with valueobjects.

                Collection<ExperimentalFactorValueObject> efs = r.getExperimentalFactors();

                if ( efs == null ) {
                    // This should not happen any more, but just in case.
                    GeneDifferentialExpressionServiceImpl.log
                            .warn( "No experimentalfactor(s) for DifferentialExpressionAnalysisResult: " + r.getId() );
                    continue;
                }

                if ( r.getCorrP() == null ) {
                    GeneDifferentialExpressionServiceImpl.log
                            .warn( "No p-value for DifferentialExpressionAnalysisResult: " + r.getId() );
                    continue;
                }

                devos.add( r );

            }

        }
        timer.stop();
        if ( timer.getTime() > 1000 ) {
            GeneDifferentialExpressionServiceImpl.log.info( "Postprocess Diff ex results: " + timer.getTime() + " ms" );
        }

        return devos;
    }

}
