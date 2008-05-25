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
     * When multiple probes map to same gene, correct by multiplying each pval by num probes that map to the gene
     * 
     * @param results
     * @return
     */
    private DoubleArrayList correctPvalsForMetaAnalysis( Collection<ProbeAnalysisResult> results ) {
        DoubleArrayList pvalues = new DoubleArrayList();

        for ( ProbeAnalysisResult r : results ) {

            int numProbesForGene = results.size();
            double pval = r.getPvalue() * numProbesForGene;
            if ( pval > MAX_PVAL ) pval = MAX_PVAL;

            pvalues.add( pval );
        }
        return pvalues;
    }

    /**
     * Combine the pvalues using the fisher method.
     * 
     * @param pvaluesToCombine
     * @return
     */
    @SuppressWarnings("unchecked")
    private Double fisherCombinePvalues( Collection<ProbeAnalysisResult> pvaluesToCombine ) {
        DoubleArrayList metaAnalysisCorrectedPvals = correctPvalsForMetaAnalysis( pvaluesToCombine );
        return MetaAnalysis.fisherCombinePvalues( metaAnalysisCorrectedPvals );
    }

    /**
     * AJAX entry.
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
            mavos.add( mavo );
        }

        return mavos;
    }

    /**
     * @param geneId
     * @param eeIds
     * @param threshold
     * @return
     */
    @SuppressWarnings("unchecked")
    public DifferentialExpressionMetaAnalysisValueObject getDifferentialExpression( Long geneId,
            Collection<Long> eeIds, double threshold ) {

        Gene g = geneService.load( geneId );

        if ( g == null ) return null;

        /* find the analyzed experiments */
        Collection<ExpressionExperiment> experimentsAnalyzed = differentialExpressionAnalysisService
                .findExperimentsWithAnalyses( g );

        /* find the 'active' experiments */
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
        mavo.setGene( g );
        mavo.setActiveExperiments( activeExperiments );

        Collection<ProbeAnalysisResult> pvaluesToCombine = new HashSet<ProbeAnalysisResult>();

        /* supporting experiments */
        Collection<ExpressionExperiment> supportingExperiments = new ArrayList<ExpressionExperiment>();

        /* a gene can have multiple probes that map to it, so store one diff value object for each probe */
        Collection<DifferentialExpressionValueObject> devos = new ArrayList<DifferentialExpressionValueObject>();

        /* each gene will have a row, and each row will have a row expander with supporting datasets */
        for ( ExpressionExperiment ee : activeExperiments ) {

            ExpressionExperimentValueObject eevo = new ExpressionExperimentValueObject();
            eevo.setId( ee.getId() );
            eevo.setShortName( ee.getShortName() );
            eevo.setName( ee.getName() );
            eevo.setExternalUri( GemmaLinkUtils.getExpressionExperimentUrl( eevo.getId() ) );

            // FIXME to compute the meta analysis results, we want to ignore the threshold altogether.

            Collection<ProbeAnalysisResult> results = differentialExpressionAnalysisService.find( g, ee, threshold );
            if ( results == null || results.isEmpty() ) {
                log.debug( "Experiment " + ee.getShortName() + " does not have diff support at threshold " + threshold );
                continue;
            }
            supportingExperiments.add( ee );

            /* for the meta analysis */
            pvaluesToCombine.addAll( results );

            Map<DifferentialExpressionAnalysisResult, Collection<ExperimentalFactor>> dearToEf = differentialExpressionAnalysisResultService
                    .getExperimentalFactors( results );

            /* for each result, set up a devo */
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
        double fisherPval = this.fisherCombinePvalues( pvaluesToCombine );
        mavo.setFisherPValue( fisherPval );
        mavo.setSupportingExperiments( supportingExperiments );
        mavo.setProbeResults( devos );

        return mavo;
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
