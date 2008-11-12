package ubic.gemma.analysis.expression.diff;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.basecode.math.metaanalysis.MetaAnalysis;
import ubic.gemma.analysis.expression.experiment.ExperimentalFactorValueObject;
import ubic.gemma.model.analysis.expression.ProbeAnalysisResult;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResultService;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisService;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.VocabCharacteristic;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.util.AnchorTagUtil;
import cern.colt.list.DoubleArrayList;

/**
 * Provides access to {@link DifferentialExpressionAnalysisResult}s and meta-analysis results.
 * 
 * @author keshav
 * @version $Id$
 * @spring.bean id="geneDifferentialExpressionService"
 * @spring.property name="differentialExpressionAnalysisService" ref="differentialExpressionAnalysisService"
 * @spring.property name="differentialExpressionAnalysisResultService" ref="differentialExpressionAnalysisResultService"
 */
public class GeneDifferentialExpressionService {

    private Log log = LogFactory.getLog( this.getClass() );

    private final int MAX_PVAL = 1;

    private static final String FV_SEP = ", ";

    private DifferentialExpressionAnalysisService differentialExpressionAnalysisService = null;
    private DifferentialExpressionAnalysisResultService differentialExpressionAnalysisResultService = null;

    /**
     * Get the differential expression analysis results for the gene in the activeExperiments.
     * 
     * @param threshold
     * @param g
     * @param eeFactorsMap
     * @param activeExperiments
     * @return
     */
    public DifferentialExpressionMetaAnalysisValueObject getDifferentialExpressionMetaAnalysis( double threshold,
            Gene g, Map<Long, Long> eeFactorsMap, Collection<ExpressionExperiment> activeExperiments ) {
        /*
         * Get results for each active experiment on given gene. Handling the threshold check below since we ignore this
         * for the meta analysis. The results returned are for all factors, not just the factors we are seeking.
         */
        Map<ExpressionExperiment, Collection<ProbeAnalysisResult>> resultsMap = differentialExpressionAnalysisService
                .findResultsForGeneInExperiments( g, activeExperiments );

        log.debug( resultsMap.size() + " results for " + g + " in " + activeExperiments );

        DifferentialExpressionMetaAnalysisValueObject mavo = new DifferentialExpressionMetaAnalysisValueObject();

        DoubleArrayList pvaluesToCombine = new DoubleArrayList();

        /* a gene can have multiple probes that map to it, so store one diff value object for each probe */
        Collection<DifferentialExpressionValueObject> devos = new ArrayList<DifferentialExpressionValueObject>();

        int numMetThreshold = 0;
        /* each gene will have a row, and each row will have a row expander with supporting datasets */
        for ( ExpressionExperiment ee : resultsMap.keySet() ) {

            ExpressionExperimentValueObject eevo = configExpressionExperimentValueObject( ee );

            Collection<ProbeAnalysisResult> probes = resultsMap.get( ee );
            /* filter results for duplicate probes (those from experiments that had 2 way anova) */
            Map<DifferentialExpressionAnalysisResult, Collection<ExperimentalFactor>> dearToEf = differentialExpressionAnalysisResultService
                    .getExperimentalFactors( probes );

            Collection<ProbeAnalysisResult> filteredResults = new HashSet<ProbeAnalysisResult>();
            for ( ProbeAnalysisResult r : probes ) {
                Collection<ExperimentalFactor> efs = dearToEf.get( r );
                assert efs.size() > 0;
                if ( efs.size() > 1 ) {
                    // We always ignore interaction effects.
                    continue;
                }

                ExperimentalFactor ef = efs.iterator().next();

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
            ProbeAnalysisResult res = findMinPenalizedProbeResult( filteredResults );

            Double p = res.getPvalue();
            pvaluesToCombine.add( p );

            /* for each filtered result, set up a devo (contains only results with chosen factor) */
            for ( ProbeAnalysisResult r : filteredResults ) {
                DifferentialExpressionValueObject devo = new DifferentialExpressionValueObject();

                Boolean metThreshold = r.getCorrectedPvalue() <= threshold ? true : false;
                devo.setMetThreshold( metThreshold );

                if ( metThreshold ) numMetThreshold++;

                Boolean fisherContribution = r.equals( res ) ? true : false;
                devo.setFisherContribution( fisherContribution );

                devo.setGene( g );
                devo.setExpressionExperiment( eevo );
                devo.setProbe( r.getProbe().getName() );
                devo.setProbeId( r.getProbe().getId() );
                devo.setExperimentalFactors( new HashSet<ExperimentalFactorValueObject>() );
                Collection<ExperimentalFactor> efs = dearToEf.get( r );
                if ( efs == null ) {
                    // This should not happen any more, but just in case.
                    log.warn( "No experimentalfactor(s) for ProbeAnalysisResult: " + r.getId() );
                    continue;
                }
                ExperimentalFactor ef = efs.iterator().next();

                ExperimentalFactorValueObject efvo = configExperimentalFactorValueObject( ef );
                devo.getExperimentalFactors().add( efvo );

                devo.setP( r.getCorrectedPvalue() );
                devo.setSortKey();
                devos.add( devo );
            }

        }

        double fisherPval = MetaAnalysis.fisherCombinePvalues( pvaluesToCombine );
        mavo.setFisherPValue( fisherPval );
        mavo.setGene( g );
        mavo.setActiveExperiments( activeExperiments );
        mavo.setProbeResults( devos );
        mavo.setNumMetThreshold( numMetThreshold );
        mavo.setSortKey();
        return mavo;
    }

    /**
     * Get differential expression for a gene, constrained to a specific set of factors. Note that interactions are
     * ignored, only main effects (the factorMap can only have one factor per experiment)
     * 
     * @param ees
     * @param gene
     * @param threshold
     * @param factorMap
     * @return
     */
    public Collection<DifferentialExpressionValueObject> getDifferentialExpression(
            Collection<ExpressionExperiment> ees, Gene gene, double threshold,
            Collection<DiffExpressionSelectedFactorCommand> factorMap ) {

        Collection<DifferentialExpressionValueObject> result = new ArrayList<DifferentialExpressionValueObject>();

        if ( gene == null ) return result;

        Map<ExpressionExperiment, Collection<ProbeAnalysisResult>> rawDiffEx = differentialExpressionAnalysisService
                .findResultsForGeneInExperimentsMetThreshold( gene, ees, threshold );

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

        return result;
    }

    /**
     * Get the differential expression results for the given gene.
     * 
     * @param gene
     * @param threshold
     * @return
     */
    @SuppressWarnings("unchecked")
    public Collection<DifferentialExpressionValueObject> getDifferentialExpression( Gene gene, double threshold ) {

        Collection<DifferentialExpressionValueObject> devos = new ArrayList<DifferentialExpressionValueObject>();

        if ( gene == null ) return devos;

        Collection<ExpressionExperiment> experimentsAnalyzed = differentialExpressionAnalysisService
                .findExperimentsWithAnalyses( gene );

        Map<ExpressionExperiment, Collection<ProbeAnalysisResult>> results = differentialExpressionAnalysisService
                .findResultsForGeneInExperimentsMetThreshold( gene, experimentsAnalyzed, threshold );

        return postProcessDiffExResults( gene, threshold, results );
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
            Map<ExpressionExperiment, Collection<ProbeAnalysisResult>> results ) {
        Collection<DifferentialExpressionValueObject> devos = new ArrayList<DifferentialExpressionValueObject>();

        for ( ExpressionExperiment ee : results.keySet() ) {
            ExpressionExperimentValueObject eevo = configExpressionExperimentValueObject( ee );

            Collection<ProbeAnalysisResult> probeResults = results.get( ee );
            Map<DifferentialExpressionAnalysisResult, Collection<ExperimentalFactor>> dearToEf = differentialExpressionAnalysisResultService
                    .getExperimentalFactors( probeResults );

            for ( ProbeAnalysisResult r : probeResults ) {
                DifferentialExpressionValueObject devo = new DifferentialExpressionValueObject();
                devo.setGene( gene );
                devo.setExpressionExperiment( eevo );
                devo.setProbe( r.getProbe().getName() );
                devo.setProbeId( r.getProbe().getId() );
                devo.setExperimentalFactors( new HashSet<ExperimentalFactorValueObject>() );
                Collection<ExperimentalFactor> efs = dearToEf.get( r );
                if ( efs == null ) {
                    // This should not happen any more, but just in case.
                    log.warn( "No experimentalfactor(s) for ProbeAnalysisResult: " + r.getId() );
                    continue;
                }
                for ( ExperimentalFactor ef : efs ) {
                    ExperimentalFactorValueObject efvo = configExperimentalFactorValueObject( ef );

                    devo.getExperimentalFactors().add( efvo );
                    devo.setSortKey();
                }
                devo.setP( r.getCorrectedPvalue() );
                devo.setMetThreshold( r.getCorrectedPvalue() < threshold );
                devos.add( devo );

            }

        }
        return devos;
    }

    /**
     * @param ef
     * @return
     */
    public ExperimentalFactorValueObject configExperimentalFactorValueObject( ExperimentalFactor ef ) {
        ExperimentalFactorValueObject efvo = new ExperimentalFactorValueObject();
        efvo.setId( ef.getId() );
        efvo.setName( ef.getName() );
        efvo.setDescription( ef.getDescription() );
        Characteristic category = ef.getCategory();
        if ( category != null ) {
            efvo.setCategory( category.getCategory() );
            if ( category instanceof VocabCharacteristic )
                efvo.setCategoryUri( ( ( VocabCharacteristic ) category ).getCategoryUri() );
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

    /**
     * @param ee
     * @return
     */
    public ExpressionExperimentValueObject configExpressionExperimentValueObject( ExpressionExperiment ee ) {
        ExpressionExperimentValueObject eevo = new ExpressionExperimentValueObject();
        eevo.setId( ee.getId() );
        eevo.setShortName( ee.getShortName() );
        eevo.setName( ee.getName() );
        eevo.setExternalUri( AnchorTagUtil.getExpressionExperimentUrl( eevo.getId() ) );
        return eevo;
    }

    /**
     * When n probes map to the same gene, penalize by multiplying each pval by n and then take the 'best' value.
     * 
     * @param results
     * @return the result with the min p value.
     */
    private ProbeAnalysisResult findMinPenalizedProbeResult( Collection<ProbeAnalysisResult> results ) {

        ProbeAnalysisResult minResult = null;

        int numProbesForGene = results.size();
        if ( numProbesForGene == 1 ) {
            return results.iterator().next();
        }

        double min = 0;
        int i = 0;
        for ( ProbeAnalysisResult r : results ) {

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
     * @param differentialExpressionAnalysisService
     */
    public void setDifferentialExpressionAnalysisService(
            DifferentialExpressionAnalysisService differentialExpressionAnalysisService ) {
        this.differentialExpressionAnalysisService = differentialExpressionAnalysisService;
    }

    /**
     * @param differentialExpressionAnalysisResultService
     */
    public void setDifferentialExpressionAnalysisResultService(
            DifferentialExpressionAnalysisResultService differentialExpressionAnalysisResultService ) {
        this.differentialExpressionAnalysisResultService = differentialExpressionAnalysisResultService;
    }

}
