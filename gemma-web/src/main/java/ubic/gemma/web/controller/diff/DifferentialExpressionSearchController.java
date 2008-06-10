/*
 * The Gemma project
 * 
 * Copyright (c) 2006-2008 University of British Columbia
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
package ubic.gemma.web.controller.diff;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.servlet.ModelAndView;

import ubic.basecode.math.metaanalysis.MetaAnalysis;
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
import ubic.gemma.model.genome.gene.GeneService;
import ubic.gemma.util.GemmaLinkUtils;
import ubic.gemma.web.controller.BaseFormController;
import ubic.gemma.web.controller.expression.experiment.ExperimentalFactorValueObject;
import ubic.gemma.web.view.TextView;
import cern.colt.list.DoubleArrayList;

/**
 * @author keshav
 * @version $Id$ *
 * @spring.bean id="differentialExpressionSearchController"
 * @spring.property name = "commandName" value="diffExpressionSearchCommand"
 * @spring.property name = "commandClass" value="ubic.gemma.web.controller.diff.DiffExpressionSearchCommand"
 * @spring.property name = "formView" value="diffExpressionSearchForm"
 * @spring.property name = "successView" value="diffExpressionResultsByExperiment"
 * @spring.property name = "differentialExpressionAnalysisService" ref="differentialExpressionAnalysisService"
 * @spring.property name = "differentialExpressionAnalysisResultService"
 *                  ref="differentialExpressionAnalysisResultService"
 * @spring.property name = "geneService" ref="geneService"
 */
public class DifferentialExpressionSearchController extends BaseFormController {

    private Log log = LogFactory.getLog( this.getClass() );

    private static final double DEFAULT_THRESHOLD = 0.01;

    private static final String FV_SEP = ", ";

    private DifferentialExpressionAnalysisService differentialExpressionAnalysisService = null;

    private DifferentialExpressionAnalysisResultService differentialExpressionAnalysisResultService = null;

    private GeneService geneService = null;

    private final int MAX_PVAL = 1;

    /**
     * 
     */
    public DifferentialExpressionSearchController() {
        /*
         * if true, reuses the same command object across the edit-submit-process (get-post-process).
         */
        setSessionForm( true );
    }

    /**
     * AJAX entry.
     * <p>
     * Returns a metadata diff expression value object, which is useful for printing the results to a text view.
     * 
     * @param geneIds
     * @param threshold
     * @return
     */
    public DifferentialExpressionMetaValueObject getDifferentialExpressionMeta( Collection<Long> geneIds,
            double threshold ) {

        List<DifferentialExpressionValueObject> devos = new ArrayList<DifferentialExpressionValueObject>();

        for ( Long geneId : geneIds ) {

            DifferentialExpressionMetaAnalysisValueObject mavo = getDifferentialExpression( geneId, null, threshold );

            devos.addAll( mavo.getProbeResults() );
        }

        DifferentialExpressionMetaValueObject meta = new DifferentialExpressionMetaValueObject( devos );

        return meta;
    }

    /**
     * When n probes map to the same gene, penalize by multiplying each pval by n and then take the 'best' value.
     * 
     * @param results
     * @return A map keyed by the result with the lowest penalized pval (there is only one key). The value is the
     *         collection the 'other' probe analysis results.
     */
    private Map<ProbeAnalysisResult, Collection<ProbeAnalysisResult>> findMinPenalizedProbeResult(
            Collection<ProbeAnalysisResult> results ) {

        Map<ProbeAnalysisResult, Collection<ProbeAnalysisResult>> contribAndNonContribPvals = new HashMap<ProbeAnalysisResult, Collection<ProbeAnalysisResult>>();

        ProbeAnalysisResult minResult = null;

        int numProbesForGene = results.size();
        if ( numProbesForGene == 1 ) {
            contribAndNonContribPvals.put( results.iterator().next(), new HashSet<ProbeAnalysisResult>() );
            return contribAndNonContribPvals;
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

        /* set up the non-contrib results ... */
        Collection<ProbeAnalysisResult> nonContribResults = new HashSet<ProbeAnalysisResult>();
        for ( ProbeAnalysisResult r : results ) {
            if ( r.equals( minResult ) ) continue;
            nonContribResults.add( r );
        }

        contribAndNonContribPvals.put( minResult, nonContribResults );

        return contribAndNonContribPvals;

    }

    /**
     * AJAX entry. Returns the meta-analysis results.
     * <p>
     * Gets the differential expression results for the genes in {@link DiffExpressionSearchCommand}.
     * 
     * @param command
     * @return
     */
    public Collection<DifferentialExpressionMetaAnalysisValueObject> getDiffExpressionForGenes(
            DiffExpressionSearchCommand command ) {

        Collection<Long> geneIds = command.getGeneIds();

        Collection<Long> eeIds = command.getEeIds();

        double threshold = command.getThreshold();

        Collection<DifferentialExpressionMetaAnalysisValueObject> mavos = new ArrayList<DifferentialExpressionMetaAnalysisValueObject>();
        for ( long geneId : geneIds ) {
            DifferentialExpressionMetaAnalysisValueObject mavo = getDifferentialExpression( geneId, eeIds, threshold );
            mavo.setSortKey();
            if ( eeIds != null && !eeIds.isEmpty() ) mavo.setNumSearchedExperiments( eeIds.size() );
            mavos.add( mavo );

        }

        return mavos;
    }

    /**
     * Returns the results of the meta-analysis.
     * 
     * @param geneId
     * @param eeIds
     * @param threshold
     * @return
     */
    @SuppressWarnings("unchecked")
    private DifferentialExpressionMetaAnalysisValueObject getDifferentialExpression( Long geneId,
            Collection<Long> eeIds, double threshold ) {

        Gene g = geneService.load( geneId );

        if ( g == null ) return null;

        /* find the analyzed experiments (those that had the diff cli run on it and have the gene g) */
        Collection<ExpressionExperiment> experimentsAnalyzed = differentialExpressionAnalysisService
                .findExperimentsWithAnalyses( g );

        /* find the 'active' experiments (those that had the diff cli run on it and are in the scope of eeIds) */
        Collection<ExpressionExperiment> activeExperiments = null;
        if ( eeIds == null || eeIds.isEmpty() ) {
            activeExperiments = experimentsAnalyzed;
        } else {
            activeExperiments = new ArrayList<ExpressionExperiment>();
            for ( ExpressionExperiment ee : experimentsAnalyzed ) {
                if ( eeIds.contains( ee.getId() ) ) {
                    activeExperiments.add( ee );
                }
            }
        }

        DifferentialExpressionMetaAnalysisValueObject mavo = new DifferentialExpressionMetaAnalysisValueObject();

        DoubleArrayList pvaluesToCombine = new DoubleArrayList();

        /* a gene can have multiple probes that map to it, so store one diff value object for each probe */
        Collection<DifferentialExpressionValueObject> devos = new ArrayList<DifferentialExpressionValueObject>();

        /* each gene will have a row, and each row will have a row expander with supporting datasets */
        for ( ExpressionExperiment ee : activeExperiments ) {

            ExpressionExperimentValueObject eevo = new ExpressionExperimentValueObject();
            eevo.setId( ee.getId() );
            eevo.setShortName( ee.getShortName() );
            eevo.setName( ee.getName() );
            eevo.setExternalUri( GemmaLinkUtils.getExpressionExperimentUrl( eevo.getId() ) );

            /*
             * use the threshold for regular diff expression (handling the threhold check below since we ignore this for
             * the meta analysis)
             */
            Collection<ProbeAnalysisResult> results = differentialExpressionAnalysisService.find( g, ee );

            /*
             * For the diff expression meta analysis, ignore threshold. Select the 'best' penalized probe if multiple
             * probes map to the same gene.
             */
            Map<ProbeAnalysisResult, Collection<ProbeAnalysisResult>> fisherContribAndNonContribProbes = findMinPenalizedProbeResult( results );

            ProbeAnalysisResult res = fisherContribAndNonContribProbes.keySet().iterator().next();
            Double p = res.getPvalue();
            pvaluesToCombine.add( p );

            Collection<ProbeAnalysisResult> nonContributingProbes = fisherContribAndNonContribProbes.get( res );

            Map<DifferentialExpressionAnalysisResult, Collection<ExperimentalFactor>> dearToEf = differentialExpressionAnalysisResultService
                    .getExperimentalFactors( results );

            /* for each result, set up a devo */
            for ( ProbeAnalysisResult r : results ) {
                DifferentialExpressionValueObject devo = new DifferentialExpressionValueObject();

                Boolean metThreshold = r.getCorrectedPvalue() <= threshold ? true : false;
                devo.setMetThreshold( metThreshold );

                Boolean fisherContribution = nonContributingProbes.contains( r ) ? true : false;
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
                for ( ExperimentalFactor ef : efs ) {
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

                    efvo.setFactorValues( factorValuesAsString );

                    devo.getExperimentalFactors().add( efvo );
                }
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
        mavo.setSortKey();

        return mavo;
    }

    /**
     * AJAX entry which returns results on a non-meta analysis basis. That is, the differential expression results for
     * the gene with the id, geneId, are returned.
     * 
     * @param geneId
     * @param threshold
     * @return
     */
    @SuppressWarnings("unchecked")
    public Collection<DifferentialExpressionValueObject> getDifferentialExpression( Long geneId, double threshold ) {
        Collection<DifferentialExpressionValueObject> devos = new ArrayList<DifferentialExpressionValueObject>();
        Gene g = geneService.load( geneId );
        if ( g == null ) return devos;
        Collection<ExpressionExperiment> experimentsAnalyzed = differentialExpressionAnalysisService
                .findExperimentsWithAnalyses( g );
        for ( ExpressionExperiment ee : experimentsAnalyzed ) {
            ExpressionExperimentValueObject eevo = new ExpressionExperimentValueObject();
            eevo.setId( ee.getId() );
            eevo.setShortName( ee.getShortName() );
            eevo.setName( ee.getName() );
            eevo.setExternalUri( GemmaLinkUtils.getExpressionExperimentUrl( eevo.getId() ) );

            Collection<ProbeAnalysisResult> results = differentialExpressionAnalysisService.find( g, ee, threshold );

            Map<DifferentialExpressionAnalysisResult, Collection<ExperimentalFactor>> dearToEf = differentialExpressionAnalysisResultService
                    .getExperimentalFactors( results );

            for ( ProbeAnalysisResult r : results ) {
                DifferentialExpressionValueObject devo = new DifferentialExpressionValueObject();
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
                for ( ExperimentalFactor ef : efs ) {
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

                    efvo.setFactorValues( factorValuesAsString );

                    devo.getExperimentalFactors().add( efvo );
                }
                devo.setP( r.getCorrectedPvalue() );
                devos.add( devo );

            }

        }
        return devos;
    }

    /*
     * Handles the case exporting results as text.
     * 
     * @see org.springframework.web.servlet.mvc.AbstractFormController#handleRequestInternal(javax.servlet.http.HttpServletRequest,
     *      javax.servlet.http.HttpServletResponse)
     */
    @SuppressWarnings( { "unchecked", "unused" })
    @Override
    protected ModelAndView handleRequestInternal( HttpServletRequest request, HttpServletResponse response )
            throws Exception {

        if ( request.getParameter( "export" ) != null ) {

            double threshold = DEFAULT_THRESHOLD;
            try {
                threshold = Double.parseDouble( request.getParameter( "t" ) );
            } catch ( Exception e ) {
                log.warn( "invalid threshold; using default " + threshold );
            }

            Collection<Long> geneIds = extractIds( request.getParameter( "g" ) );

            DifferentialExpressionMetaValueObject result = getDifferentialExpressionMeta( geneIds, threshold );

            ModelAndView mav = new ModelAndView( new TextView() );
            String output = result.toString();
            mav.addObject( "text", output.length() > 0 ? output : "no results" );
            return mav;
        } else {
            return new ModelAndView( this.getFormView() );
        }
    }

    /**
     * @param differentialExpressionAnalyzerService
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

    /**
     * @param geneService
     */
    public void setGeneService( GeneService geneService ) {
        this.geneService = geneService;
    }
}
