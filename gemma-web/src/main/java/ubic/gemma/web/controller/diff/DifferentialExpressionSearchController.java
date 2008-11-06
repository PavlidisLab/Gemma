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
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.springframework.web.servlet.ModelAndView;

import ubic.basecode.math.metaanalysis.MetaAnalysis;
import ubic.gemma.model.analysis.expression.FactorAssociatedAnalysisResultSet;
import ubic.gemma.model.analysis.expression.ProbeAnalysisResult;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResultService;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisService;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.VocabCharacteristic;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.gene.GeneService;
import ubic.gemma.util.AnchorTagUtil;
import ubic.gemma.web.controller.BaseFormController;
import ubic.gemma.web.controller.expression.experiment.ExperimentalFactorValueObject;
import ubic.gemma.web.controller.expression.experiment.ExpressionExperimentExperimentalFactorValueObject;
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
 * @spring.property name = "expressionExperimentService" ref="expressionExperimentService"
 */
public class DifferentialExpressionSearchController extends BaseFormController {

    private static final double DEFAULT_THRESHOLD = 0.01;

    private static final String FV_SEP = ", ";

    private DifferentialExpressionAnalysisService differentialExpressionAnalysisService = null;
    private DifferentialExpressionAnalysisResultService differentialExpressionAnalysisResultService = null;
    private GeneService geneService = null;
    private ExpressionExperimentService expressionExperimentService = null;

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
     * AJAX entry. Returns the meta-analysis results.
     * <p>
     * Gets the differential expression results for the genes in {@link DiffExpressionSearchCommand}.
     * 
     * @param command
     * @return
     */
    public Collection<DifferentialExpressionMetaAnalysisValueObject> getDiffExpressionForGenes(
            DiffExpressionSearchCommand command ) {

        Collection<Long> eeScopeIds = command.getEeIds();

        Collection<Long> geneIds = command.getGeneIds();

        Collection<DiffExpressionSelectedFactorCommand> selectedFactors = command.getSelectedFactors();

        double threshold = command.getThreshold();

        Collection<DifferentialExpressionMetaAnalysisValueObject> mavos = new ArrayList<DifferentialExpressionMetaAnalysisValueObject>();
        for ( long geneId : geneIds ) {
            DifferentialExpressionMetaAnalysisValueObject mavo = getDifferentialExpressionMetaAnalysis( geneId,
                    selectedFactors, threshold );
            mavo.setSortKey();
            if ( selectedFactors != null && !selectedFactors.isEmpty() ) {
                mavo.setNumSearchedExperiments( selectedFactors.size() );
            }

            mavo.setNumExperimentsInScope( eeScopeIds.size() );

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
    private DifferentialExpressionMetaAnalysisValueObject getDifferentialExpressionMetaAnalysis( Long geneId,
            Collection<DiffExpressionSelectedFactorCommand> selectedFactors, double threshold ) {

        Gene g = geneService.load( geneId );

        if ( g == null ) {
            log.warn( "No Gene with id=" + geneId );
            return null;
        }

        /* find experiments that have had the diff cli run on it and have the gene g (analyzed) */
        Collection<ExpressionExperiment> experimentsAnalyzed = differentialExpressionAnalysisService
                .findExperimentsWithAnalyses( g );

        /* the 'chosen' factors (and their associated experiments) */
        Map<Long, Long> eeFactorsMap = new HashMap<Long, Long>();
        for ( DiffExpressionSelectedFactorCommand selectedFactor : selectedFactors ) {
            eeFactorsMap.put( selectedFactor.getEeId(), selectedFactor.getEfId() );
            log.debug( selectedFactor.getEeId() + " --> " + selectedFactor.getEfId() );
        }

        /* filter experiments that had the diff cli run on it and are in the scope of eeFactorsMap eeIds (active) */
        Collection<ExpressionExperiment> activeExperiments = null;
        if ( eeFactorsMap.keySet() == null || eeFactorsMap.isEmpty() ) {
            activeExperiments = experimentsAnalyzed;
        } else {
            activeExperiments = new ArrayList<ExpressionExperiment>();
            for ( ExpressionExperiment ee : experimentsAnalyzed ) {
                if ( eeFactorsMap.keySet().contains( ee.getId() ) ) {
                    activeExperiments.add( ee );
                }
            }
        }

        DifferentialExpressionMetaAnalysisValueObject mavo = new DifferentialExpressionMetaAnalysisValueObject();

        DoubleArrayList pvaluesToCombine = new DoubleArrayList();

        /* a gene can have multiple probes that map to it, so store one diff value object for each probe */
        Collection<DifferentialExpressionValueObject> devos = new ArrayList<DifferentialExpressionValueObject>();

        int numMetThreshold = 0;
        /* each gene will have a row, and each row will have a row expander with supporting datasets */
        for ( ExpressionExperiment ee : activeExperiments ) {

            ExpressionExperimentValueObject eevo = configExpressionExperimentValueObject( ee );

            /*
             * Get results for experiment on given gene. Handling the threshold check below since we ignore this for the
             * meta analysis. The results returned are for all factors, not just the factors we are seeking.
             */
            Collection<ProbeAnalysisResult> results = differentialExpressionAnalysisService.find( g, ee );

            log.debug( results.size() + " results for " + g + " in " + ee );

            /* filter results for duplicate probes (those from experiments that had 2 way anova) */
            Map<DifferentialExpressionAnalysisResult, Collection<ExperimentalFactor>> dearToEf = differentialExpressionAnalysisResultService
                    .getExperimentalFactors( results );

            Collection<ProbeAnalysisResult> filteredResults = new HashSet<ProbeAnalysisResult>();
            for ( ProbeAnalysisResult r : results ) {
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
            ExpressionExperimentValueObject eevo = configExpressionExperimentValueObject( ee );

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
    private ExperimentalFactorValueObject configExperimentalFactorValueObject( ExperimentalFactor ef ) {
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
    private ExpressionExperimentValueObject configExpressionExperimentValueObject( ExpressionExperiment ee ) {
        ExpressionExperimentValueObject eevo = new ExpressionExperimentValueObject();
        eevo.setId( ee.getId() );
        eevo.setShortName( ee.getShortName() );
        eevo.setName( ee.getName() );
        eevo.setExternalUri( AnchorTagUtil.getExpressionExperimentUrl( eevo.getId() ) );
        return eevo;
    }

    /**
     * AJAX entry.
     * <p>
     * Value objects returned contain experiments that have 2 factors and have had the diff analysis run on it.
     * 
     * @param eeIds
     */
    @SuppressWarnings("unchecked")
    public Collection<ExpressionExperimentExperimentalFactorValueObject> getFactors( final Collection<Long> eeIds ) {

        Collection<ExpressionExperimentExperimentalFactorValueObject> result = new HashSet<ExpressionExperimentExperimentalFactorValueObject>();

        final Collection<Long> securityFilteredIds = securityFilterExpressionExperimentIds( eeIds );

        if ( securityFilteredIds.size() == 0 ) {
            return result;
        }

        log.debug( "Getting factors for experiments with ids: " + securityFilteredIds.toString() );

        Collection<Long> filteredEeIds = new HashSet<Long>();

        Map<Long, DifferentialExpressionAnalysis> diffAnalyses = differentialExpressionAnalysisService
                .findByInvestigationIds( securityFilteredIds );

        Collection<ExpressionExperimentValueObject> eevos = this.expressionExperimentService
                .loadValueObjects( diffAnalyses.keySet() );

        Map<Long, ExpressionExperimentValueObject> eevoMap = new HashMap<Long, ExpressionExperimentValueObject>();
        for ( ExpressionExperimentValueObject eevo : eevos ) {
            eevoMap.put( eevo.getId(), eevo );
        }

        for ( Long id : diffAnalyses.keySet() ) {

            DifferentialExpressionAnalysis analysis = diffAnalyses.get( id );

            Collection<ExperimentalFactor> factors = new HashSet<ExperimentalFactor>();
            for ( FactorAssociatedAnalysisResultSet fars : analysis.getResultSets() ) {
                // FIXME includes factors making up interaction terms, but shouldn't
                // matter, because they will be included as main effects too. If not, this will be wrong!
                factors.addAll( fars.getExperimentalFactor() );
            }

            filteredEeIds.add( id );
            ExpressionExperimentValueObject eevo = eevoMap.get( id );
            ExpressionExperimentExperimentalFactorValueObject eeefvo = new ExpressionExperimentExperimentalFactorValueObject();
            eeefvo.setExpressionExperiment( eevo );
            for ( ExperimentalFactor ef : factors ) {
                ExperimentalFactorValueObject efvo = configExperimentalFactorValueObject( ef );
                eeefvo.getExperimentalFactors().add( efvo );
            }

            result.add( eeefvo );
        }
        log.info( "Filtered experiments.  Returning factors for experiments with ids: " + filteredEeIds.toString() );
        return result;
    }

    @SuppressWarnings("unchecked")
    private Collection<Long> securityFilterExpressionExperimentIds( Collection<Long> ids ) {
        /*
         * Because this method returns the results, we have to screen.
         */
        Collection<ExpressionExperiment> securityScreened = expressionExperimentService.loadMultiple( ids );

        Collection<Long> filteredIds = new HashSet<Long>();
        for ( ExpressionExperiment ee : securityScreened ) {
            filteredIds.add( ee.getId() );
        }
        return filteredIds;
    }

    /*
     * Handles the case exporting results as text.
     * @seeorg.springframework.web.servlet.mvc.AbstractFormController#handleRequestInternal(javax.servlet.http.
     * HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    @Override
    protected ModelAndView handleRequestInternal( HttpServletRequest request, HttpServletResponse response )
            throws Exception {

        if ( request.getParameter( "export" ) != null ) {

            double threshold = DEFAULT_THRESHOLD;
            try {
                threshold = Double.parseDouble( request.getParameter( "t" ) );
            } catch ( NumberFormatException e ) {
                log.warn( "invalid threshold; using default " + threshold );
            }

            Collection<Long> geneIds = extractIds( request.getParameter( "g" ) );

            Long eeSetId = null;
            try {
                eeSetId = Long.parseLong( request.getParameter( "a" ) );
            } catch ( NumberFormatException e ) {
                //
            }

            String fs = request.getParameter( "fm" );
            Collection<DiffExpressionSelectedFactorCommand> selectedFactors = extractFactorInfo( fs );

            DiffExpressionSearchCommand command = new DiffExpressionSearchCommand();
            command.setGeneIds( geneIds );
            command.setEeSetId( eeSetId );
            command.setSelectedFactors( selectedFactors );
            command.setThreshold( threshold );

            Collection<DifferentialExpressionMetaAnalysisValueObject> result = getDiffExpressionForGenes( command );

            ModelAndView mav = new ModelAndView( new TextView() );

            StringBuilder buf = new StringBuilder();
            for ( DifferentialExpressionMetaAnalysisValueObject demavo : result ) {
                buf.append( demavo );
            }

            String output = buf.toString();

            mav.addObject( "text", output.length() > 0 ? output : "no results" );
            return mav;
        }
        return new ModelAndView( this.getFormView() );

    }

    /**
     * @param fs
     * @return
     */
    private Collection<DiffExpressionSelectedFactorCommand> extractFactorInfo( String fs ) {
        Collection<DiffExpressionSelectedFactorCommand> selectedFactors = new HashSet<DiffExpressionSelectedFactorCommand>();
        try {
            if ( fs != null ) {
                String[] fss = fs.split( "," );
                for ( String fm : fss ) {
                    String[] m = fm.split( "\\." );
                    if ( m.length != 2 ) {
                        continue;
                    }
                    String eeIdStr = m[0];
                    String efIdStr = m[1];

                    Long eeId = Long.parseLong( eeIdStr );
                    Long efId = Long.parseLong( efIdStr );
                    DiffExpressionSelectedFactorCommand dsfc = new DiffExpressionSelectedFactorCommand( eeId, efId );
                    selectedFactors.add( dsfc );
                }
            }
        } catch ( NumberFormatException e ) {
            log.warn( "Error parsing factor info" );
        }
        return selectedFactors;
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

    /**
     * @param expressionExperimentService
     */
    public void setExpressionExperimentService( ExpressionExperimentService expressionExperimentService ) {
        this.expressionExperimentService = expressionExperimentService;
    }
}
