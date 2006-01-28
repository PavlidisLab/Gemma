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
package edu.columbia.gemma.web.controller.genome.gene;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.SimpleFormController;

import edu.columbia.gemma.genome.gene.CandidateGeneListService;
import edu.columbia.gemma.genome.gene.GeneService;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004 - 2006 University of British Columbia
 * 
 * @author daq2101
 * @version $Id$
 * @spring.bean id="geneFinderController" name="/geneFinder.htm"
 * @spring.property name="formView" value="geneFinder"
 * @spring.property name="successView" value="geneFinder"
 * @spring.property name="geneService" ref="geneService"
 * @spring.property name="candidateGeneListService" ref="candidateGeneListService"
 */
public class GeneFinderController extends SimpleFormController {
    private GeneService geneService;
    public CandidateGeneListService candidateGeneListService;

    /**
     * @return Returns the bibliographicReferenceService.
     */
    public GeneService getGeneService() {
        return geneService;
    }

    /**
     * @param geneService The geneService to set.
     */
    public void setGeneService( GeneService geneService ) {
        this.geneService = geneService;
    }

    /**
     * @return Returns the candidateGeneListService.
     */
    public CandidateGeneListService getCandidateGeneListService() {
        return candidateGeneListService;
    }

    /**
     * @param geneService The geneService to set.
     */
    public void setCandidateGeneListService( CandidateGeneListService candidateGeneListService ) {
        this.candidateGeneListService = candidateGeneListService;
    }

    @Override
    @SuppressWarnings("unused")
    public ModelAndView onSubmit( HttpServletRequest request, HttpServletResponse response, Object command,
            BindException errors ) throws Exception {

        String view = "geneFinder";
        String act = request.getParameter( "action" );
        String searchType = request.getParameter( "searchtype" );
        String lookup = request.getParameter( "lookup" );
        String geneID = request.getParameter( "geneID" );
        String listID = request.getParameter( "listID" );
        if ( act == null ) act = "all";
        Map<String, Collection> geneModel = new HashMap<String, Collection>();

        if ( searchType.compareTo( "all" ) == 0 ) {
            geneModel.put( "genes", this.getGeneService().findAll() );
        }
        if ( searchType.compareTo( "bySymbol" ) == 0 ) {
            geneModel.put( "genes", this.getGeneService().findByOfficialSymbol( lookup ) );
        }
        if ( searchType.compareTo( "bySymbolInexact" ) == 0 ) {
            lookup = "%" + lookup + "%";
            geneModel.put( "genes", this.getGeneService().findByOfficialSymbolInexact( lookup ) );
        }
        if ( searchType.compareTo( "byName" ) == 0 ) {
            geneModel.put( "genes", this.getGeneService().findByOfficialName( lookup ) );
        }

        return new ModelAndView( view, "model", geneModel );

    }

    /**
     * This is needed or you will have to specify a commandClass in the DispatcherServlet's context
     * 
     * @param request
     * @return Object
     * @throws Exception
     */
    protected Object formBackingObject( HttpServletRequest request ) throws Exception {
        return request;
    }
}
