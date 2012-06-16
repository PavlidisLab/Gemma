package ubic.gemma.analysis.expression.diff;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ubic.basecode.math.metaanalysis.MetaAnalysis;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionResultService;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.VocabCharacteristic;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExperimentalFactorValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.gene.GeneValueObject;
import ubic.gemma.util.AnchorTagUtil;
import cern.colt.list.DoubleArrayList;

/**
 * Provides access to {@link DifferentialExpressionAnalysisResult}s and meta-analysis results.
 * 
 * @author keshav
 * @version $Id$
 */
@Component
public class GeneDifferentialExpressionServiceImpl implements GeneDifferentialExpressionService {

    private static final String FV_SEP = ", ";

    /**
     * p values smaller than this will be treated as this value in a meta-analysis. The reason is to avoid extremely low
     * pvalues from driving meta-pvalues down too fast. This is suggested by the fact that very small pvalues presume an
     * extremely high precision in agreement between the tails of the true null distribution and the analytic
     * distribution used to compute the pvalues (e.g., F or t).
     */
    private static final double PVALUE_CLIP_THRESHOLD = 1e-8;

    @Autowired
    private DifferentialExpressionResultService differentialExpressionResultService = null;

    private Log log = LogFactory.getLog( this.getClass() );

    private final int MAX_PVAL = 1;

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.analysis.expression.diff.GeneDifferentialExpressionService#configExperimentalFactorValueObject(ubic
     * .gemma.model.expression.experiment.ExperimentalFactor)
     */
    @Override
    public ExperimentalFactorValueObject configExperimentalFactorValueObject( ExperimentalFactor ef ) {
        ExperimentalFactorValueObject efvo = new ExperimentalFactorValueObject();
        efvo.setId( ef.getId() );
        efvo.setName( ef.getName() );
        efvo.setDescription( ef.getDescription() );
        Characteristic category = ef.getCategory();
        if ( category != null ) {
            efvo.setCategory( category.getCategory() );
            if ( category instanceof VocabCharacteristic ) {
                efvo.setCategoryUri( ( ( VocabCharacteristic ) category ).getCategoryUri() );
            }
        }
        Collection<FactorValue> fvs = ef.getFactorValues();
        String factorValuesAsString = StringUtils.EMPTY;

        for ( FactorValue fv : fvs ) {
            String fvName = fv.toString();
            if ( StringUtils.isNotBlank( fvName ) ) {
                factorValuesAsString += fvName + FV_SEP;
            }
        }

        /* clean up the start and end of the string */
        factorValuesAsString = StringUtils.remove( factorValuesAsString, ef.getName() + ":" );
        factorValuesAsString = StringUtils.removeEnd( factorValuesAsString, FV_SEP );

        /*
         * Preformat the factor name; due to Ext PropertyGrid limitations we can't do this on the client.
         */
        efvo.setName( ef.getName() + " (" + StringUtils.abbreviate( factorValuesAsString, 50 ) + ")" );

        efvo.setFactorValues( factorValuesAsString );
        return efvo;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.analysis.expression.diff.GeneDifferentialExpressionService#configExpressionExperimentValueObject(ubic
     * .gemma.model.expression.experiment.BioAssaySet)
     */
    @Override
    public ExpressionExperimentValueObject configExpressionExperimentValueObject( BioAssaySet ee ) {
        ExpressionExperimentValueObject eevo = new ExpressionExperimentValueObject();
        eevo.setId( ee.getId() );

        if ( ee instanceof ExpressionExperiment ) {
            eevo.setShortName( ( ( ExpressionExperiment ) ee ).getShortName() );
        } else if ( ee instanceof ExpressionExperimentSubSet ) {
            eevo.setShortName( "Subset of "
                    + ( ( ExpressionExperimentSubSet ) ee ).getSourceExperiment().getShortName() );
            eevo.setSourceExperiment( ( ( ExpressionExperimentSubSet ) ee ).getSourceExperiment().getId() );
        }
        eevo.setName( ee.getName() );
        eevo.setExternalUri( AnchorTagUtil.getExpressionExperimentUrl( eevo.getId() ) );
        return eevo;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.analysis.expression.diff.GeneDifferentialExpressionService#getDifferentialExpression(ubic.gemma.model
     * .genome.Gene, java.util.Collection)
     */
    @Override
    public Collection<DifferentialExpressionValueObject> getDifferentialExpression( Gene gene,
            Collection<BioAssaySet> ees ) {
        StopWatch timer = new StopWatch();
        timer.start();
        Collection<DifferentialExpressionValueObject> devos = new ArrayList<DifferentialExpressionValueObject>();

        if ( gene == null ) return devos;

        Map<BioAssaySet, List<DifferentialExpressionAnalysisResult>> results = differentialExpressionResultService
                .find( gene, ees );
        timer.stop();
        if ( timer.getTime() > 1000 ) {
            log.info( "Diff ex results: " + timer.getTime() + " ms" );
        }
        return postProcessDiffExResults( gene, -1, results );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.analysis.expression.diff.GeneDifferentialExpressionService#getDifferentialExpression(ubic.gemma.model
     * .genome.Gene, java.util.Collection, double, java.lang.Integer)
     */
    @Override
    public Collection<DifferentialExpressionValueObject> getDifferentialExpression( Gene gene,
            Collection<BioAssaySet> ees, double threshold, Integer limit ) {
        StopWatch timer = new StopWatch();
        timer.start();
        Collection<DifferentialExpressionValueObject> devos = new ArrayList<DifferentialExpressionValueObject>();

        if ( gene == null ) return devos;

        Map<BioAssaySet, List<DifferentialExpressionAnalysisResult>> results = differentialExpressionResultService
                .find( gene, ees, threshold, limit );
        timer.stop();
        if ( timer.getTime() > 1000 ) {
            log.info( "Diff ex results: " + timer.getTime() + " ms" );
        }
        return postProcessDiffExResults( gene, threshold, results );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.analysis.expression.diff.GeneDifferentialExpressionService#getDifferentialExpression(ubic.gemma.model
     * .genome.Gene, double, java.util.Collection)
     */
    @Override
    public Collection<DifferentialExpressionValueObject> getDifferentialExpression( Gene gene, double threshold,
            Collection<DiffExpressionSelectedFactorCommand> factorMap ) {
        StopWatch timer = new StopWatch();
        timer.start();
        Collection<DifferentialExpressionValueObject> result = new ArrayList<DifferentialExpressionValueObject>();

        if ( gene == null ) return result;

        Map<BioAssaySet, List<DifferentialExpressionAnalysisResult>> rawDiffEx = differentialExpressionResultService
                .find( gene, threshold, null );

        Collection<DifferentialExpressionValueObject> rawProcResults = postProcessDiffExResults( gene, threshold,
                rawDiffEx );

        Map<Long, DiffExpressionSelectedFactorCommand> eeId2FactorCommand = new HashMap<Long, DiffExpressionSelectedFactorCommand>();
        for ( DiffExpressionSelectedFactorCommand dsfc : factorMap ) {
            eeId2FactorCommand.put( dsfc.getEeId(), dsfc );
        }

        for ( DifferentialExpressionValueObject raw : rawProcResults ) {
            if ( eeId2FactorCommand.containsKey( raw.getExpressionExperiment().getId() ) ) {
                DiffExpressionSelectedFactorCommand factorCommandForEE = eeId2FactorCommand.get( raw
                        .getExpressionExperiment().getId() );

                assert !raw.getExperimentalFactors().isEmpty();

                // interaction term?
                if ( raw.getExperimentalFactors().size() > 1 ) {
                    continue;
                }

                ExperimentalFactorValueObject efvo = raw.getExperimentalFactors().iterator().next();
                if ( factorCommandForEE.getEfId().equals( efvo.getId() ) ) {
                    result.add( raw );
                    continue;
                }
            }
        }
        timer.stop();
        if ( timer.getTime() > 1000 ) {
            log.info( "Diff ex results: " + timer.getTime() + " ms" );
        }
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.analysis.expression.diff.GeneDifferentialExpressionService#getDifferentialExpression(ubic.gemma.model
     * .genome.Gene, double, java.lang.Integer)
     */
    @Override
    public Collection<DifferentialExpressionValueObject> getDifferentialExpression( Gene gene, double threshold,
            Integer limit ) {

        StopWatch timer = new StopWatch();
        timer.start();

        Collection<DifferentialExpressionValueObject> devos = new ArrayList<DifferentialExpressionValueObject>();

        if ( gene == null ) return devos;

        if ( timer.getTime() > 1000 ) {
            log.info( "Find experiments: " + timer.getTime() + " ms" );
        }
        timer.reset();
        timer.start();

        Map<BioAssaySet, List<DifferentialExpressionAnalysisResult>> results = differentialExpressionResultService
                .find( gene, threshold, limit );
        timer.stop();

        if ( timer.getTime() > 1000 ) {
            log.info( "Diff raw ex results: " + timer.getTime() + " ms" );
        }

        return postProcessDiffExResults( gene, threshold, results );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.analysis.expression.diff.GeneDifferentialExpressionService#getDifferentialExpressionMetaAnalysis(double
     * , ubic.gemma.model.genome.Gene, java.util.Map, java.util.Collection)
     */
    @Override
    public DifferentialExpressionMetaAnalysisValueObject getDifferentialExpressionMetaAnalysis( double threshold,
            Gene g, Map<Long, Long> eeFactorsMap, Collection<BioAssaySet> activeExperiments ) {

        StopWatch timer = new StopWatch();
        timer.start();

        /*
         * Get results for each active experiment on given gene. Handling the threshold check below since we ignore this
         * for the meta analysis. The results returned are for all factors, not just the factors we are seeking.
         */
        Map<BioAssaySet, List<DifferentialExpressionAnalysisResult>> resultsMap = differentialExpressionResultService
                .find( g, activeExperiments );

        Map<DifferentialExpressionAnalysisResult, Collection<ExperimentalFactor>> dearToEf = getFactors( resultsMap );

        log.debug( resultsMap.size() + " results for " + g + " in " + activeExperiments );

        DifferentialExpressionMetaAnalysisValueObject mavo = new DifferentialExpressionMetaAnalysisValueObject();

        DoubleArrayList pvaluesToCombine = new DoubleArrayList();

        /* a gene can have multiple probes that map to it, so store one diff value object for each probe */
        Collection<DifferentialExpressionValueObject> devos = new ArrayList<DifferentialExpressionValueObject>();

        Collection<Long> eesThatMetThreshold = new HashSet<Long>();

        for ( BioAssaySet ee : resultsMap.keySet() ) {

            ExpressionExperimentValueObject eevo = configExpressionExperimentValueObject( ee );

            Collection<DifferentialExpressionAnalysisResult> proberesults = resultsMap.get( ee );

            Collection<DifferentialExpressionAnalysisResult> filteredResults = new HashSet<DifferentialExpressionAnalysisResult>();
            for ( DifferentialExpressionAnalysisResult r : proberesults ) {
                Collection<ExperimentalFactor> efs = dearToEf.get( r );
                assert efs.size() > 0;
                if ( efs.size() > 1 ) {
                    // We always ignore interaction effects.
                    continue;
                }

                ExperimentalFactor ef = efs.iterator().next();

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
                log.warn( "No result for ee=" + ee );
                continue;
            }

            /*
             * For the diff expression meta analysis, ignore threshold. Select the 'best' penalized probe if multiple
             * probes map to the same gene.
             */
            DifferentialExpressionAnalysisResult res = findMinPenalizedProbeResult( filteredResults );
            if ( res == null ) continue;

            Double p = res.getPvalue();
            if ( p == null ) continue;

            /*
             * Moderate the pvalues by setting all values to be no smaller than PVALUE_CLIP_THRESHOLD
             */
            pvaluesToCombine.add( Math.max( p, PVALUE_CLIP_THRESHOLD ) );

            /* for each filtered result, set up a devo (contains only results with chosen factor) */

            differentialExpressionResultService.thaw( filteredResults );

            for ( DifferentialExpressionAnalysisResult r : filteredResults ) {

                Collection<ExperimentalFactor> efs = dearToEf.get( r );
                if ( efs == null ) {
                    // This should not happen any more, but just in case.
                    log.warn( "No experimentalfactor(s) for DifferentialExpressionAnalysisResult: " + r.getId() );
                    continue;
                }

                DifferentialExpressionValueObject devo = diffExResultToValueObject( r, g, eevo, efs );

                Boolean metThreshold = r.getCorrectedPvalue() != null
                        && ( r.getCorrectedPvalue() <= threshold ? true : false );
                devo.setMetThreshold( metThreshold );

                if ( metThreshold ) {
                    eesThatMetThreshold.add( eevo.getId() );
                }

                Boolean fisherContribution = r.equals( res ) ? true : false;
                devo.setFisherContribution( fisherContribution );

                devo.setP( r.getPvalue() );
                devo.setCorrP( r.getCorrectedPvalue() );
                devo.setSortKey();
                devos.add( devo );
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
            log.info( "Meta-analysis results: " + timer.getTime() + " ms" );
        }

        return mavo;
    }

    private DifferentialExpressionValueObject diffExResultToValueObject( DifferentialExpressionAnalysisResult r,
            Gene gene, ExpressionExperimentValueObject eevo, Collection<ExperimentalFactor> efs ) {

        DifferentialExpressionValueObject devo = new DifferentialExpressionValueObject();
        devo.setGene( new GeneValueObject( gene ) );
        devo.setExpressionExperiment( eevo );
        CompositeSequence probe = r.getProbe();
        devo.setProbe( probe.getName() );
        devo.setProbeId( probe.getId() );
        devo.setExperimentalFactors( new HashSet<ExperimentalFactorValueObject>() );
        devo.setId( r.getId() );

        for ( ExperimentalFactor ef : efs ) {
            ExperimentalFactorValueObject efvo = configExperimentalFactorValueObject( ef );

            devo.getExperimentalFactors().add( efvo );
            devo.setSortKey();
        }
        return devo;
    }

    /**
     * When n probes map to the same gene, penalize by multiplying each pval by n and then take the 'best' value.
     * 
     * @param results
     * @return the result with the min p value.
     */
    private DifferentialExpressionAnalysisResult findMinPenalizedProbeResult(
            Collection<DifferentialExpressionAnalysisResult> results ) {

        DifferentialExpressionAnalysisResult minResult = null;

        int numProbesForGene = results.size();
        if ( numProbesForGene == 1 ) {
            return results.iterator().next();
        }

        double min = 0;
        int i = 0;
        for ( DifferentialExpressionAnalysisResult r : results ) {

            /*
             * FIXME use the contrasts.
             */
            r.getContrasts();

            if ( r.getPvalue() == null ) continue;
            /* penalize pvals */
            double pval = r.getPvalue() * numProbesForGene;
            if ( pval > MAX_PVAL ) pval = MAX_PVAL;

            /* find the best pval */
            if ( i == 0 || pval <= min ) {
                min = pval;
                minResult = r;
                minResult.setPvalue( min );
            }

            i++;
        }

        return minResult;

    }

    /**
     * Do this in one query to avoid extra db round trips.
     * 
     * @param resultsMap
     * @return
     */
    private Map<DifferentialExpressionAnalysisResult, Collection<ExperimentalFactor>> getFactors(
            Map<BioAssaySet, List<DifferentialExpressionAnalysisResult>> resultsMap ) {

        Collection<DifferentialExpressionAnalysisResult> allRes = new HashSet<DifferentialExpressionAnalysisResult>();
        for ( Collection<DifferentialExpressionAnalysisResult> p : resultsMap.values() ) {
            allRes.addAll( p );
        }

        Map<DifferentialExpressionAnalysisResult, Collection<ExperimentalFactor>> dearToEf = differentialExpressionResultService
                .getExperimentalFactors( allRes );

        return dearToEf;
    }

    /**
     * Convert the raw results into DifferentialExpressionValueObjects
     * 
     * @param gene
     * @param threshold
     * @param devos
     * @param results
     */
    private Collection<DifferentialExpressionValueObject> postProcessDiffExResults( Gene gene, double threshold,
            Map<BioAssaySet, List<DifferentialExpressionAnalysisResult>> results ) {
        StopWatch timer = new StopWatch();
        timer.start();

        Collection<DifferentialExpressionValueObject> devos = new ArrayList<DifferentialExpressionValueObject>();

        Map<DifferentialExpressionAnalysisResult, Collection<ExperimentalFactor>> dearToEf = getFactors( results );

        /*
         * convert to DEVOs
         */
        for ( BioAssaySet ee : results.keySet() ) {

            ExpressionExperimentValueObject eevo = configExpressionExperimentValueObject( ee );

            Collection<DifferentialExpressionAnalysisResult> probeResults = results.get( ee );

            differentialExpressionResultService.thaw( probeResults );

            for ( DifferentialExpressionAnalysisResult r : probeResults ) {

                Collection<ExperimentalFactor> efs = dearToEf.get( r );

                if ( efs == null ) {
                    // This should not happen any more, but just in case.
                    log.warn( "No experimentalfactor(s) for DifferentialExpressionAnalysisResult: " + r.getId() );
                    continue;
                }

                DifferentialExpressionValueObject devo = diffExResultToValueObject( r, gene, eevo, efs );

                if ( r.getCorrectedPvalue() == null ) {
                    log.warn( "No p-value for DifferentialExpressionAnalysisResult: " + r.getId() );
                    continue;
                }

                devo.setP( r.getPvalue() );
                devo.setCorrP( r.getCorrectedPvalue() );
                devo.setMetThreshold( r.getCorrectedPvalue() < threshold );
                devos.add( devo );

            }

        }
        timer.stop();
        if ( timer.getTime() > 1000 ) {
            log.info( "Postprocess Diff ex results: " + timer.getTime() + " ms" );
        }
        return devos;
    }

}
