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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;

import ubic.gemma.model.expression.designElement.CompositeSequenceService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.biosequence.BioSequenceService;
import ubic.gemma.model.genome.gene.GeneProductService;
import ubic.gemma.model.genome.gene.GeneService;
import ubic.gemma.web.controller.BaseFormController;

/** 
 * @author joseph
 * @version $Id$
 * @spring.bean id="geneFinderController"  
 * @spring.property name="formView" value="geneFinder"
 * @spring.property name="successView" value="geneFinder"
 * @spring.property name="geneService" ref="geneService"
 * @spring.property name="geneProductService" ref="geneProductService" 
 * @spring.property name="bioSequenceService" ref="bioSequenceService" 
 * @spring.property name="compositeSequenceService" ref="compositeSequenceService"  
 */
public class GeneFinderController extends BaseFormController {
    private static Log log = LogFactory.getLog( GeneFinderController.class.getName() );
    
    private GeneService geneService;
    private GeneProductService geneProductService;
    private CompositeSequenceService compositeSequenceService;
    private BioSequenceService bioSequenceService;
    
    
    /**
     * @return the bioSequenceService
     */
    public BioSequenceService getBioSequenceService() {
        return bioSequenceService;
    }

    /**
     * @param bioSequenceService the bioSequenceService to set
     */
    public void setBioSequenceService( BioSequenceService bioSequenceService ) {
        this.bioSequenceService = bioSequenceService;
    }
    
    /**
     * @return the compositeSequenceService
     */
    public CompositeSequenceService getCompositeSequenceService() {
        return compositeSequenceService;
    }

    /**
     * @param compositeSequenceService the compositeSequenceService to set
     */
    public void setCompositeSequenceService( CompositeSequenceService compositeSequenceService ) {
        this.compositeSequenceService = compositeSequenceService;
    }

    /**
     * @return the geneProductService
     */
    public GeneProductService getGeneProductService() {
        return geneProductService;
    }

    /**
     * @param geneProductService the geneProductService to set
     */
    public void setGeneProductService( GeneProductService geneProductService ) {
        this.geneProductService = geneProductService;
    }

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

    @Override
    @SuppressWarnings({ "unused", "unchecked" })
    public ModelAndView onSubmit( HttpServletRequest request, HttpServletResponse response, Object command,
            BindException errors ) throws Exception {

        String searchString = request.getParameter( "searchString" );
        
        // first check - searchString should allow searches of 3 characters or more ONLY
        // this is to prevent a huge wildcard search
        if ( StringUtils.isEmpty( searchString ) ) {
            this.saveMessage( request, "Must use at least three characters for search" );
            return new ModelAndView("geneFinder");
        }
        
        Map params = request.getParameterMap();

        String previousSearch = (String)request.getSession().getAttribute( "previousSearch");
        // check to see if the modelAndView is saved in the session
        // make sure that this is a pagination or sort (not a re-search)
        // the current search string and the previous search string should not be null
        // and the previous search should be the same as the current one.
        if ( (request.getSession().getAttribute( "modelAndView") != null) && 
                (params.size() > 1) &&
                (previousSearch != null) &&
                (previousSearch.equals( searchString ))
                ) {
            return (ModelAndView) request.getSession().getAttribute( "modelAndView");
        }
        

        
        // search by inexact symbol
        Set<Gene> geneSet =  new HashSet<Gene>();
        Set<Gene> geneMatch = new HashSet<Gene>();
        Set<Gene> aliasMatch = new HashSet<Gene>();
        Set<Gene> geneProductMatch = new HashSet<Gene>();
        Set<Gene> bioSequenceMatch = new HashSet<Gene>();
        
        geneMatch.addAll( geneService.findByOfficialSymbolInexact( searchString ) );
        aliasMatch.addAll( geneService.getByGeneAlias( searchString ) );
        
        geneProductMatch.addAll( geneProductService.getGenesByName( searchString ) );
        geneProductMatch.addAll( geneProductService.getGenesByNcbiId( searchString ) );
        
        bioSequenceMatch.addAll( bioSequenceService.getGenesByAccession( searchString ) );
        bioSequenceMatch.addAll( bioSequenceService.getGenesByName( searchString ) );
         
        geneSet.addAll(geneMatch);
        geneSet.addAll(aliasMatch);
        geneSet.addAll( geneProductMatch );
        geneSet.addAll( bioSequenceMatch );
        
        // trick to get around lazy load
        // touch all taxa in geneSet
        for (Gene g : geneSet) {
            String name = g.getTaxon().getScientificName();
        }
        
        List<Gene> geneList = new ArrayList<Gene>(geneSet);
        Comparator<Gene> comparator = new GeneComparator();
        Collections.sort( geneList, comparator );
      
        ModelAndView mav = new ModelAndView("geneFinderList");
        mav.addObject( "genes", geneList );
        mav.addObject( "geneMatch", geneMatch );
        mav.addObject( "aliasMatch", aliasMatch );
        mav.addObject( "geneProductMatch", geneProductMatch );
        mav.addObject( "bioSequenceMatch", bioSequenceMatch );
        mav.addObject( "searchParameter", searchString );
        
        // cache modelandview
        
        request.getSession().setAttribute( "modelAndView", mav );
        request.getSession().setAttribute( "previousSearch", searchString );   
        
        return mav;
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
     * 
     */
    @Override
    public ModelAndView processFormSubmission( HttpServletRequest request, HttpServletResponse response,
            Object command, BindException errors ) throws Exception {
        return this.onSubmit( request, response, command, errors );
    }
    
    
    
    
    /* (non-Javadoc)
     * @see org.springframework.web.servlet.mvc.SimpleFormController#showForm(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, org.springframework.validation.BindException)
     */
    @Override
    protected ModelAndView showForm( HttpServletRequest request, HttpServletResponse response, BindException errors ) throws Exception {
        if (request.getParameter( "searchString" ) != null) {
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
