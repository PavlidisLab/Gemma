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

    private int stringency = 2;

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
            devos.addAll( getDifferentialExpression( geneId, threshold ) );
        }

        DifferentialExpressionMetaValueObject meta = new DifferentialExpressionMetaValueObject( devos );

        return meta;
    }

    /**
     * AJAX entry.
     * <p>
     * Gets the differential expression results for the genes in {@link DiffExpressionSearchCommand}.
     * 
     * @param command
     * @return
     */
    public Collection<DifferentialExpressionValueObject> getDiffExpressionForGenes( DiffExpressionSearchCommand command ) {

        Collection<Long> geneIds = command.getGeneIds();

        double threshold = command.getThreshold();

        Collection<DifferentialExpressionValueObject> devos = new ArrayList<DifferentialExpressionValueObject>();
        for ( long geneId : geneIds ) {
            Collection<DifferentialExpressionValueObject> devosForGene = getDifferentialExpression( geneId, threshold );
            devos.addAll( devosForGene );
        }

        return devos;

    }

    /**
     * AJAX entry.
     * <p>
     * 
     * @param command
     * @return
     */
    public Collection<DifferentialExpressionMetaAnalysisValueObject> getDiffMetaAnalysisForGenes(
            DiffExpressionSearchCommand command ) {

        Collection<Long> geneIds = command.getGeneIds();

        Collection<DifferentialExpressionMetaAnalysisValueObject> demavos = new ArrayList<DifferentialExpressionMetaAnalysisValueObject>();
        for ( long geneId : geneIds ) {
            DifferentialExpressionMetaAnalysisValueObject demavoForGene = getDifferentialExpressionMetaAnalysis( geneId );
            demavos.add( demavoForGene );
        }

        return demavos;

    }

    /**
     * @param geneId
     * @return
     */
    @SuppressWarnings("unchecked")
    public DifferentialExpressionMetaAnalysisValueObject getDifferentialExpressionMetaAnalysis( Long geneId ) {

        Gene g = geneService.load( geneId );
        if ( g == null ) return null;

        Collection<ExpressionExperiment> experimentsAnalyzed = differentialExpressionAnalysisService
                .findExperimentsWithAnalyses( g );

        DifferentialExpressionMetaAnalysisValueObject demavo = null;

        /* check to see we have at least 'stringency' experiments confirming the diff expression */
        if ( experimentsAnalyzed.size() >= stringency ) {

            /* get fisher pval */
            double fisherPVal = fisherCombinePvalues( g, experimentsAnalyzed );

            demavo = new DifferentialExpressionMetaAnalysisValueObject();
            demavo.setGene( g );
            demavo.setNumSupportingDataSets( experimentsAnalyzed.size() );
            demavo.setFisherPValue( fisherPVal );
        }

        return demavo;
    }

    /**
     * @param g
     * @param experiments
     * @return
     */
    @SuppressWarnings("unchecked")
    private Double fisherCombinePvalues( Gene g, Collection<ExpressionExperiment> experiments ) {

        DoubleArrayList pvalues = new DoubleArrayList();
        for ( ExpressionExperiment ee : experiments ) {

            Collection<ProbeAnalysisResult> results = differentialExpressionAnalysisService.find( g, ee );
            for ( ProbeAnalysisResult r : results ) {
                /*
                 * if multiple probes map to same gene, correct by multiplying each pval by num probes that map to the
                 * gene
                 */
                int numProbesForGene = results.size();
                double pval = r.getPvalue() * numProbesForGene;
                if ( pval > MAX_PVAL ) pval = MAX_PVAL;

                pvalues.add( pval );
            }
        }
        return MetaAnalysis.fisherCombinePvalues( pvalues );
    }

    /**
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
