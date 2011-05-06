/*
 * The Gemma project
 * 
 * Copyright (c) 2006-2010 University of British Columbia
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
package ubic.gemma.web.controller.coexpressionSearch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.servlet.ModelAndView;

import ubic.gemma.analysis.expression.coexpression.CoexpressionMetaValueObject;
import ubic.gemma.analysis.expression.coexpression.CoexpressionValueObjectExt;
import ubic.gemma.analysis.expression.coexpression.GeneCoexpressionService;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSet;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSetService;
import ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysis;
import ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysisService;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.gene.GeneService;
import ubic.gemma.model.genome.gene.GeneValueObject;
import ubic.gemma.search.SearchService;
import ubic.gemma.util.EntityUtils;
import ubic.gemma.web.controller.BaseFormController;
import ubic.gemma.web.view.TextView;

/**
 * @author luke
 * @version $Id$
 */
@Controller
public class CoexpressionSearchController extends BaseFormController {

    private static final int DEFAULT_STRINGENCY = 2;

    private static final int MAX_GENES_PER_QUERY = 20;

    private static final int MAX_RESULTS = 200;

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private ExpressionExperimentSetService expressionExperimentSetService;

    @Autowired
    private GeneCoexpressionAnalysisService geneCoexpressionAnalysisService;

    @Autowired
    private GeneCoexpressionService geneCoexpressionService;

    @Autowired
    private GeneService geneService = null;

    @Autowired
    private SearchService searchService = null;

    /**
     * @param searchOptions
     * @return
     */
    public CoexpressionMetaValueObject doQuickSearch( CoexpressionSearchCommand searchOptions ) {

        CoexpressionMetaValueObject result = new CoexpressionMetaValueObject();

        if ( searchOptions.getGeneIds().size() != 1 ) {
            result.setErrorState( "Too many genes selected, please limit searches to one" );
            return result;
        }

        Gene gene = geneService.load( searchOptions.getGeneIds().iterator().next() );

        if ( gene == null ) {
            result.setErrorState( "Invalid gene id(s) - no genes found" );
            return result;

        }

        log.info( "Coexpression search: " + searchOptions );

        gene = this.geneService.thaw( gene ); // need to thaw externalDB in taxon for marshling back to client...

        Long eeSetId = getEESet( searchOptions, gene );

        if ( eeSetId == null ) {
            result.setErrorState( "No coexpression results available" );
            log.info( "No expression experiment set results for query: " + searchOptions );
            return result;
        }

        List<Gene> genes = new ArrayList<Gene>();
        genes.add( gene );
        result.setQueryGenes( GeneValueObject.convert2ValueObjects( genes ) );

        Collection<CoexpressionValueObjectExt> geneResults = geneCoexpressionService.coexpressionSearchQuick( eeSetId,
                genes, 2, 20, false, true );
        result.setKnownGeneResults( geneResults );

        if ( result.getKnownGeneResults() == null || result.getKnownGeneResults().isEmpty() ) {
            result.setErrorState( "Sorry, No genes are currently coexpressed under the selected search conditions " );
            log.info( "No search results for query: " + searchOptions );
        }

        return result;

    }

    /**
     * Main AJAX entry point
     * 
     * @param searchOptions
     * @return
     */
    public CoexpressionMetaValueObject doSearch( CoexpressionSearchCommand searchOptions ) {

        CoexpressionMetaValueObject result = new CoexpressionMetaValueObject();
        Collection<ExpressionExperiment> myEE = null;

        if ( searchOptions.getGeneIds() == null || searchOptions.getGeneIds().isEmpty() ) {
            return getEmptyResult();
        }

        if ( searchOptions.isQuick() ) {
            return doQuickSearch( searchOptions );
        }

        if ( searchOptions.getGeneIds().size() > MAX_GENES_PER_QUERY ) {
            result
                    .setErrorState( "Too many genes selected, please limit searches to " + MAX_GENES_PER_QUERY
                            + " genes" );
            return result;
        }

        Collection<Gene> genes = geneService.loadThawed( searchOptions.getGeneIds() );

        if ( genes.size() == 0 ) {
            result.setErrorState( "Invalid gene id(s) - no genes found" );
            return result;
        }

        /*
         * Validation ...
         */
        if ( searchOptions.getTaxonId() != null ) {
            for ( Gene gene : genes ) {
                if ( !gene.getTaxon().getId().equals( searchOptions.getTaxonId() ) ) {
                    result
                            .setErrorState( "Search for gene from wrong taxon. Please check the genes match the selected taxon" );
                    return result;
                }
            }
        }
        Long eeSetId = searchOptions.getEeSetId();
        Collection<Long> eeIds = new HashSet<Long>();

        if ( searchOptions.getEeIds() != null ) {
            eeIds = searchOptions.getEeIds();
        }

        /*
         * User provided a eeset by name.
         */
        if ( ( eeSetId == null && searchOptions.getEeSetName() != null )
                || ( eeSetId != null && eeSetId < 0 && StringUtils.isNotBlank( searchOptions.getEeSetName() ) ) ) {
            Collection<ExpressionExperimentSet> eeSets = expressionExperimentSetService.findByName( searchOptions
                    .getEeSetName() );
            if ( eeSets.size() == 1 ) {
                eeSetId = eeSets.iterator().next().getId();

            } else {
                result.setErrorState( "Unknown or ambiguous set name: " + searchOptions.getEeSetName() );
                return result;
            }
        }

        /*
         * Got an ee set.
         */
        if ( eeSetId != null ) {
            ExpressionExperimentSet eeSet = expressionExperimentSetService.load( eeSetId );
            if ( eeSet == null ) {
                result.setErrorState( "No such set with id=" + eeSetId );
                return result;
            }
            for ( BioAssaySet b : eeSet.getExperiments() ) {
                eeIds.add( b.getId() );
            }
        }

        // Add the users datasets to the selected datasets
        if ( searchOptions.isUseMyDatasets() ) {
            myEE = expressionExperimentService.loadMyExpressionExperiments();
            if ( myEE != null && !myEE.isEmpty() ) {
                for ( ExpressionExperiment ee : myEE ) {
                    eeIds.add( ee.getId() );
                }

                searchOptions.setForceProbeLevelSearch( true );
            } else
                log.info( "No user data to add" );
        }

        Collection<ExpressionExperiment> ees = expressionExperimentService.loadMultiple( eeIds );
        eeIds = EntityUtils.getIds( ees );

        if ( eeIds.isEmpty() ) {
            result.setErrorState( "No experiments were available" );
        }

        log.info( "Coexpression search: " + searchOptions );

        result = geneCoexpressionService.coexpressionSearch( eeIds, genes, searchOptions.getStringency(), MAX_RESULTS,
                searchOptions.getQueryGenesOnly(), searchOptions.isForceProbeLevelSearch() );

        if ( searchOptions.isUseMyDatasets() ) {
            addMyDataFlag( result, myEE );
        }

        if ( result.getKnownGeneResults() == null || result.getKnownGeneResults().isEmpty() ) {
            result
                    .setErrorState( "<b> Sorry, No genes are currently coexpressed under the selected search conditions </b>" );
            log.info( "No search results for query: " + searchOptions );
        }
        return result;

    }

    /**
     * @param query
     * @param taxonId
     * @return
     * @deprecated redundant with method in ExpressionExperimentController.
     */
    @Deprecated
    public Collection<Long> findExpressionExperiments( String query, Long taxonId ) {
        log.info( "Search: " + query + " taxon=" + taxonId );
        return searchService.searchExpressionExperiments( query, taxonId );
    }

    public CoexpressionMetaValueObject getEmptyResult() {
        return new CoexpressionMetaValueObject();
    }

    /*
     * Handle case of text export of the results.
     * 
     * @seeorg.springframework.web.servlet.mvc.AbstractFormController#handleRequestInternal(javax.servlet.http.
     * HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    @Override
    protected ModelAndView handleRequestInternal( HttpServletRequest request, HttpServletResponse response )
            throws Exception {

        if ( request.getParameter( "export" ) != null ) {

            Collection<Long> geneIds = extractIds( request.getParameter( "g" ) );
            Collection<Gene> genes = geneService.loadMultiple( geneIds );
            genes = geneService.thawLite( genes );

            boolean queryGenesOnly = request.getParameter( "q" ) != null;
            int stringency = DEFAULT_STRINGENCY;
            try {
                stringency = Integer.parseInt( request.getParameter( "s" ) );
            } catch ( Exception e ) {
                log.warn( "invalid stringency; using default " + stringency );
            }
            Collection<Long> eeIds = new HashSet<Long>();
            Long eeSetId = null;

            String eeSetIdString = request.getParameter( "a" );
            if ( StringUtils.isNotBlank( eeSetIdString ) ) {
                try {
                    eeSetId = Long.parseLong( eeSetIdString );
                } catch ( NumberFormatException e ) {
                    log.warn( "Invalid eeSet id: " + eeSetIdString );
                    return new ModelAndView( this.getFormView() );
                }

                ExpressionExperimentSet eeSet = this.expressionExperimentSetService.load( eeSetId );
                if ( eeSet == null ) {
                    throw new IllegalArgumentException( "Cannot load EE set with id=" + eeSetId );
                }

                eeIds = EntityUtils.getIds( eeSet.getExperiments() );
            } else if ( StringUtils.isNotBlank( request.getParameter( "an" ) ) ) {
                Collection<ExpressionExperimentSet> eeSets = expressionExperimentSetService.findByName( request
                        .getParameter( "an" ) );
                if ( eeSets.size() == 1 ) {
                    eeSetId = eeSets.iterator().next().getId();

                    ExpressionExperimentSet eeSet = this.expressionExperimentSetService.load( eeSetId );
                    for ( BioAssaySet b : eeSet.getExperiments() ) {
                        eeIds.add( b.getId() );
                    }
                } else {
                    log.warn( "Unknown or ambiguous set name: : " + request.getParameter( "an" ) );
                    return new ModelAndView( this.getFormView() );
                }
            } else {
                eeIds = extractIds( request.getParameter( "ee" ) );
            }

            CoexpressionMetaValueObject result = geneCoexpressionService.coexpressionSearch( eeIds, genes, stringency,
                    MAX_RESULTS, queryGenesOnly, false );
            ModelAndView mav = new ModelAndView( new TextView() );
            String output = result.toString();
            mav.addObject( "text", output.length() > 0 ? output : "no results" );
            return mav;

        }
        return new ModelAndView( this.getFormView() );

    }

    private void addMyDataFlag( CoexpressionMetaValueObject vo, Collection<ExpressionExperiment> eesToFlag ) {

        Collection<Long> eesToFlagIds = new ArrayList<Long>();
        for ( ExpressionExperiment ee : eesToFlag ) {
            eesToFlagIds.add( ee.getId() );
        }

        if ( vo == null || vo.getKnownGeneResults() == null || vo.getKnownGeneResults().isEmpty() ) return;

        for ( CoexpressionValueObjectExt covo : vo.getKnownGeneResults() ) {
            for ( Long eeToFlag : eesToFlagIds ) {
                if ( covo.getSupportingExperiments().contains( eeToFlag ) ) {
                    covo.setContainsMyData( true );
                    break;
                }
            }
        }

    }

    /**
     * Locate an appropriate EESet to use.
     * 
     * @param searchOptions
     * @param gene
     * @return ID of the EESet, or null if none can be found.
     */
    private Long getEESet( CoexpressionSearchCommand searchOptions, Gene gene ) {
        ExpressionExperimentSet eeSet = null;
        Long eeSetId = null;

        if ( searchOptions.getEeSetId() != null ) {
            eeSet = expressionExperimentSetService.load( searchOptions.getEeSetId() );
            if ( eeSet != null ) eeSetId = eeSet.getId();

        } else {

            if ( searchOptions.getEeSetName() != null ) {
                Collection<ExpressionExperimentSet> eeSets = expressionExperimentSetService.findByName( searchOptions
                        .getEeSetName() );

                if ( eeSets == null || eeSets.size() == 0 ) {
                    return null;
                }
                if ( eeSets.size() > 1 ) {
                    log.warn( "more than one set found using 1st." );
                }

                eeSetId = eeSets.iterator().next().getId();

            } else {
                GeneCoexpressionAnalysis analysis = null;
                String analysisName = "All " + gene.getTaxon().getCommonName();
                Collection<GeneCoexpressionAnalysis> analyses = geneCoexpressionAnalysisService
                        .findByName( analysisName );

                if ( analyses.isEmpty() ) {
                    return null;
                }

                /*
                 * Find the first enabled one.
                 */
                for ( GeneCoexpressionAnalysis a : analyses ) {
                    if ( a.getEnabled() ) {
                        analysis = a;
                    }
                }

                if ( analysis == null ) {
                    throw new IllegalStateException( "No analysis is enabled" );
                }

                eeSet = analysis.getExpressionExperimentSetAnalyzed();

                assert eeSet != null;

                eeSetId = eeSet.getId();

            }
        }
        return eeSetId;
    }

}