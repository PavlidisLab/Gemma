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
package ubic.gemma.web.controller.coexpressionSearch;

import java.util.Collection;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.servlet.ModelAndView;

import ubic.gemma.analysis.expression.coexpression.CannedAnalysisValueObject;
import ubic.gemma.analysis.expression.coexpression.CoexpressionMetaValueObject;
import ubic.gemma.analysis.expression.coexpression.GeneCoexpressionService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.gene.GeneService;
import ubic.gemma.search.SearchService;
import ubic.gemma.web.controller.BaseFormController;
import ubic.gemma.web.view.TextView;

/**
 * @author luke
 * @version $Id$
 * @spring.bean id="coexpressionSearchController"
 * @spring.property name = "geneService" ref="geneService"
 * @spring.property name = "searchService" ref="searchService"
 * @spring.property name="geneCoexpressionService" ref="geneCoexpressionService"
 */
public class CoexpressionSearchController extends BaseFormController {

    private static final int MAX_RESULTS = 500;

    private static final int DEFAULT_STRINGENCY = 2;

    private static Log log = LogFactory.getLog( CoexpressionSearchController.class.getName() );

    private GeneService geneService = null;
    private SearchService searchService = null;

    private GeneCoexpressionService geneCoexpressionService;

    /**
     * Main AJAX entry point
     * 
     * @param searchOptions
     * @return
     */
    @SuppressWarnings("unchecked")
    public CoexpressionMetaValueObject doSearch( CoexpressionSearchCommand searchOptions ) {
        Collection<Gene> genes = geneService.loadMultiple( searchOptions.getGeneIds() );
        this.geneService.thawLite( genes ); // need to thaw externalDB in taxon for marshling back to client...s
        log.info( "Coexpression search: " + searchOptions );
        if ( genes == null || genes.isEmpty() ) {
            return getEmptyResult();
        } else {
            Long eeSetId = searchOptions.getEeSetId();
            if ( eeSetId != null && eeSetId >= 0 ) {
                return geneCoexpressionService.getCannedAnalysisResults( eeSetId, genes, searchOptions.getStringency(),
                        MAX_RESULTS, searchOptions.getQueryGenesOnly() );
            } else {
                return geneCoexpressionService.getCustomAnalysisResults( searchOptions.getEeIds(), genes, searchOptions
                        .getStringency(), MAX_RESULTS, searchOptions.getQueryGenesOnly() );
            }
        }
    }

    /**
     * @param query
     * @param taxonId
     * @return
     * @deprecated redundant with method in ExpressionExperimentController.
     */
    @SuppressWarnings("unchecked")
    public Collection<Long> findExpressionExperiments( String query, Long taxonId ) {
        log.info( "Search: " + query + " taxon=" + taxonId );
        return searchService.searchExpressionExperiments( query, taxonId );
    }

    /**
     * Get the set of available canned analyses (for all taxa)
     * 
     * @return
     */
    public Collection<CannedAnalysisValueObject> getCannedAnalyses() {
        return geneCoexpressionService.getCannedAnalyses( true, true );
    }

    public CoexpressionMetaValueObject getEmptyResult() {
        return new CoexpressionMetaValueObject();
    }

    public void setGeneCoexpressionService( GeneCoexpressionService geneCoexpressionService ) {
        this.geneCoexpressionService = geneCoexpressionService;
    }

    public void setGeneService( GeneService geneService ) {
        this.geneService = geneService;
    }

    public void setSearchService( SearchService searchService ) {
        this.searchService = searchService;
    }

    /*
     * Handle case of text export of the results.
     * 
     * @see org.springframework.web.servlet.mvc.AbstractFormController#handleRequestInternal(javax.servlet.http.HttpServletRequest,
     *      javax.servlet.http.HttpServletResponse)
     */
    @SuppressWarnings( { "unchecked", "unused" })
    @Override
    protected ModelAndView handleRequestInternal( HttpServletRequest request, HttpServletResponse response )
            throws Exception {

        if ( request.getParameter( "export" ) != null ) {

            Collection<Long> geneIds = extractIds( request.getParameter( "g" ) );
            Collection<Gene> genes = geneService.loadMultiple( geneIds );

            boolean queryGenesOnly = request.getParameter( "q" ) != null;
            int stringency = DEFAULT_STRINGENCY;
            try {
                stringency = Integer.parseInt( request.getParameter( "s" ) );
            } catch ( Exception e ) {
                log.warn( "invalid stringency; using default " + stringency );
            }

            Long eeSetId = null;
            String eeSetIdString = request.getParameter( "a" );
            if ( StringUtils.isNotBlank( eeSetIdString ) ) {
                try {
                    eeSetId = Long.parseLong( eeSetIdString );
                } catch ( NumberFormatException e ) {
                    log.warn( "Invalid eeSet id: " + eeSetIdString );
                    return new ModelAndView( this.getFormView() );
                }
            }

            CoexpressionMetaValueObject result;
            if ( eeSetId != null ) {
                result = geneCoexpressionService.getCannedAnalysisResults( eeSetId, genes, stringency, 500,
                        queryGenesOnly );
            } else {
                Collection<Long> eeIds = extractIds( request.getParameter( "ee" ) );
                result = geneCoexpressionService.getCustomAnalysisResults( eeIds, genes, stringency, MAX_RESULTS,
                        queryGenesOnly );
            }

            ModelAndView mav = new ModelAndView( new TextView() );
            String output = result.toString();
            mav.addObject( "text", output.length() > 0 ? output : "no results" );
            return mav;

        } else {
            return new ModelAndView( this.getFormView() );
        }
    }
}