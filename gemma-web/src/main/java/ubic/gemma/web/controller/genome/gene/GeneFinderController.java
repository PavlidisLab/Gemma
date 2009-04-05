/*
 * The Gemma project
 * 
 * Copyright (c) 2006 University of British Columbia
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
package ubic.gemma.web.controller.genome.gene;

import java.util.Collection;
import java.util.Comparator;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;

import ubic.gemma.model.genome.Gene;
import ubic.gemma.search.SearchResult;
import ubic.gemma.search.SearchService;
import ubic.gemma.search.SearchSettings;
import ubic.gemma.web.controller.BaseFormController;

/**
 * @author joseph
 * @version $Id$
 * @spring.bean id="geneFinderController"
 * @spring.property name="formView" value="geneFinder"
 * @spring.property name="successView" value="geneFinder"
 * @spring.property name="searchService" ref="searchService"
 */
public class GeneFinderController extends BaseFormController {

    private static Log log = LogFactory.getLog( GeneFinderController.class.getName() );

    private SearchService searchService;

    /**
     * @return the searchService
     */
    public SearchService getSearchService() {
        return searchService;
    }

    @Override
    public ModelAndView onSubmit( HttpServletRequest request, HttpServletResponse response, Object command,
            BindException errors ) throws Exception {

        String searchString = request.getParameter( "searchString" );

        // first check - searchString should allow searches of 3 characters or more ONLY
        // this is to prevent a huge wildcard search
        if ( !searchStringValidator( searchString ) ) {
            this.saveMessage( request, "Must use at least three characters for search" );
            log.info( "User entered an invalid search" );
            return new ModelAndView( "geneFinder" );
        }

        log.info( "Attempting gene search" );
        Collection<SearchResult> geneResults = searchService.search( SearchSettings.GeneSearch( searchString, null ) )
                .get( Gene.class );

        ModelAndView mav = new ModelAndView( "geneFinderList" );
        mav.addObject( "searchParameter", searchString );
        mav.addObject( "numGenes", geneResults.size() );

        mav.addObject( "genes", geneResults );

        return mav;
    }

    /**
     * 
     */
    @Override
    public ModelAndView processFormSubmission( HttpServletRequest request, HttpServletResponse response,
            Object command, BindException errors ) throws Exception {
        return this.onSubmit( request, response, command, errors );
    }

    /**
     * @param searchService the searchService to set
     */
    public void setSearchService( SearchService searchService ) {
        this.searchService = searchService;
    }

    /**
     * This is needed or you will have to specify a commandClass in the DispatcherServlet's context
     * 
     * @param request
     * @return Object
     * @throws Exception
     */
    @Override
    protected Object formBackingObject( HttpServletRequest request ) throws Exception {
        return request;
    }

    /**
     * Validates the query string
     * 
     * @param query
     * @return
     */
    protected boolean searchStringValidator( String query ) {

        if ( StringUtils.isBlank( query ) ) return false;

        if ( ( query.charAt( 0 ) == '%' ) || ( query.charAt( 0 ) == '*' ) ) return false;

        return true;
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.web.servlet.mvc.SimpleFormController#showForm(javax.servlet.http.HttpServletRequest,
     * javax.servlet.http.HttpServletResponse, org.springframework.validation.BindException)
     */
    @Override
    protected ModelAndView showForm( HttpServletRequest request, HttpServletResponse response, BindException errors )
            throws Exception {
        if ( request.getParameter( "searchString" ) != null ) {
            return this.onSubmit( request, response, this.formBackingObject( request ), errors );
        }

        return super.showForm( request, response, errors );
    }

    class GeneComparator implements Comparator<Gene> {

        public int compare( Gene arg0, Gene arg1 ) {
            Gene obj0 = arg0;
            Gene obj1 = arg1;

            return obj0.getName().compareTo( obj1.getName() );
        }
    }

}
