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
package ubic.gemma.web.controller.coexpressionSearch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List; 
import java.util.Map;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.validation.BindException;
import org.springframework.web.bind.ServletRequestDataBinder;
import org.springframework.web.servlet.ModelAndView;

import ubic.gemma.loader.genome.taxon.SupportedTaxa;
import ubic.gemma.model.expression.designElement.CompositeSequenceService;
import ubic.gemma.model.expression.designElement.DesignElement;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonService;
import ubic.gemma.model.genome.gene.GeneService;
import ubic.gemma.web.controller.BaseFormController;
import ubic.gemma.web.propertyeditor.TaxonPropertyEditor;
import ubic.gemma.web.util.ConfigurationCookie;

/**
 * A <link>SimpleFormController<link> providing search functionality of genes or design elements (probe sets). The
 * success view returns either a visual representation of the result set or a downloadable data file.
 * <p>
 * {@link stringency} sets the number of data sets the link must be seen in before it is listed in the results, and
 * {@link species} sets the type of species to search. 
 * {@link keywords} restrict the search.
 * 
 * @author keshav
 * @version $Id$
 * @spring.bean id="coexpressionSearchController"  
 * @spring.property name = "commandName" value="coexpressionSearchCommand"
 * @spring.property name = "commandClass"
 *                  value="ubic.gemma.web.controller.coexpressionSearch.CoexpressionSearchCommand"
 * @spring.property name = "formView" value="searchCoexpression"
 * @spring.property name = "successView" value="showCoexpressionSearchResults"
 * @spring.property name = "expressionExperimentService" ref="expressionExperimentService"
 * @spring.property name = "compositeSequenceService" ref="compositeSequenceService"
 * @spring.property name = "geneService" ref="geneService"
 * @spring.property name = "taxonService" ref="taxonService"
 * @spring.property name = "validator" ref="genericBeanValidator"
 */
public class CoexpressionSearchController extends BaseFormController {
    private static Log log = LogFactory.getLog( CoexpressionSearchController.class.getName() );

    private static final String COOKIE_NAME = "coexpressionSearchCookie";
    
    private ExpressionExperimentService expressionExperimentService = null;
    private CompositeSequenceService compositeSequenceService = null;
    private GeneService geneService = null;
    private TaxonService taxonService = null;
    private Map<DesignElement, Collection<Gene>> designElementToGeneMap = null;
    private List<DesignElement> compositeSequences = null;

    public CoexpressionSearchController() {
        /*
         * if true, reuses the same command object across the edit-submit-process (get-post-process).
         */
        setSessionForm( true );
    }

    /**
     * @param request
     * @return Object
     * @throws ServletException
     */
    @Override
    protected Object formBackingObject( HttpServletRequest request ) {

        CoexpressionSearchCommand csc = new CoexpressionSearchCommand();
        loadCookie( request, csc );
        return csc;

    }


    /**
     * Mock function - do not use.
     * @param request
     * @param response
     * @param command
     * @param errors
     * @return ModelAndView
     * @throws Exception
     */
    @SuppressWarnings({ "unused", "unchecked" })
    @Override
    public ModelAndView onSubmit( HttpServletRequest request, HttpServletResponse response, Object command,
            BindException errors ) throws Exception {
        
        CoexpressionSearchCommand csc = ( ( CoexpressionSearchCommand ) command );

        Cookie cookie = new CoexpressionSearchCookie( csc );
        response.addCookie( cookie );
        
        Collection<Gene> genesFound;
        // find the genes specified by the search
        if (csc.isExactSearch()) {
            genesFound = geneService.findByOfficialSymbol( csc.getSearchString( ) );
        }
        else {
            genesFound = geneService.findByOfficialSymbolInexact( csc.getSearchString() );
        }
        
        // filter genes by Taxon
        Collection<Gene> genesToRemove = new ArrayList<Gene>();
        for ( Gene gene : genesFound ) {
            if ( gene.getTaxon().getId() != csc.getTaxon().getId() ) {
                genesToRemove.add( gene );
            }
        }
        genesFound.removeAll( genesToRemove );

        // if no genes found
        // return error 
        if (genesFound.size() == 0) {
            saveMessage( request, "No genes found based on criteria." );
            return showForm( request, response, errors );
        }
        
        // check if more than 1 gene found
        // if yes, then query user for gene to be used
        if (genesFound.size() > 1) {
            saveMessage( request, "Multiple genes matched. Choose which gene to use." );        
            ModelAndView mav = new ModelAndView(getFormView());
            mav.addObject( "coexpressionSearchCommand", csc );
            mav.addObject( "genes", genesFound );
            return mav;
        }

        // find expressionExperiments via lucene
        Collection<ExpressionExperiment> ees = new ArrayList<ExpressionExperiment>();
        // only one gene found, find coexpressed genes
        Gene sourceGene = (Gene) (genesFound.toArray())[0];

        Collection<Gene> coexpressedGenes = geneService.getCoexpressedGenes( sourceGene, ees );

        
        // no genes are coexpressed
        // return error 
        if (coexpressedGenes.size() == 0) {
           saveMessage( request, "No genes are coexpressed with the given stringency." );
           return showForm( request, response, errors );
        }
        
        ModelAndView mav = new ModelAndView(getSuccessView());
        mav.addObject( "coexpressedGenes", coexpressedGenes );
 //       mav.addObject( "coexpressionSearchCommand", csc );
        return mav;
    }

    /**
     * @param request
     * @return Map
     */
    @SuppressWarnings("unused")
    @Override
    protected Map referenceData( HttpServletRequest request ) {
        Map<String, List<? extends Object>> mapping = new HashMap<String, List<? extends Object>>();       

        
        // add species
        populateTaxonReferenceData( mapping );      
        
        return mapping;
    }
    
    /**
     * @param mapping
     */
    @SuppressWarnings("unchecked")
    private void populateTaxonReferenceData( Map<String, List<? extends Object>> mapping ) {
        List<Taxon> taxa = new ArrayList<Taxon>();
        for ( Taxon taxon : ( Collection<Taxon> ) taxonService.loadAll() ) {
            if ( !SupportedTaxa.contains( taxon ) ) {
                continue;
            }
            taxa.add( taxon );
        }
        Collections.sort( taxa, new Comparator<Taxon>() {
            public int compare( Taxon o1, Taxon o2 ) {
                return ( o1 ).getScientificName().compareTo( ( o2 ).getScientificName() );
            }
        } );
        mapping.put( "taxa", taxa );
    }
    
    @Override
    protected void initBinder( HttpServletRequest request, ServletRequestDataBinder binder ) {
        super.initBinder( request, binder );
        binder.registerCustomEditor( Taxon.class, new TaxonPropertyEditor( this.taxonService ) );
    }
    
    /**
     * @param request
     * @param adsac
     */
    private void loadCookie( HttpServletRequest request, CoexpressionSearchCommand csc ) {

        // cookies aren't all that important, if they're missing we just go on.
        if ( request == null || request.getCookies() == null ) return;

        for ( Cookie cook : request.getCookies() ) {
            if ( cook.getName().equals( COOKIE_NAME ) ) {
                try {
                    ConfigurationCookie cookie = new ConfigurationCookie( cook );
                    csc.setEeSearchString( cookie.getString( "eeSearchString" ) );
                    csc.setSearchString( cookie.getString( "searchString" ) );
                    csc.setStringency( cookie.getInt( "stringency" ) );
                    Taxon taxon = taxonService.findByScientificName(cookie.getString( "taxonScientificName" ));
                    csc.setTaxon( taxon );
                } catch ( Exception e ) {
                    log.warn( "Cookie could not be loaded: " + e.getMessage() );
                    // that's okay, we just don't get a cookie.
                }
            }
        }
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

    /**
     * @param expressionExperimentService
     */
    public void setExpressionExperimentService( ExpressionExperimentService expressionExperimentService ) {
        this.expressionExperimentService = expressionExperimentService;
    }

    /**
     * @param compositeSequenceService
     */
    public void setCompositeSequenceService( CompositeSequenceService compositeSequenceService ) {
        this.compositeSequenceService = compositeSequenceService;
    }
    
    /**
     * @param geneService
     */
    public void setGeneService( GeneService geneService ) {
        this.geneService = geneService;
    }

    /**
     * @param taxonService the taxonService to set
     */
    public void setTaxonService( TaxonService taxonService ) {
        this.taxonService = taxonService;
    }    
    
    class CoexpressionSearchCookie extends ConfigurationCookie {

        public CoexpressionSearchCookie( CoexpressionSearchCommand command ) {
            super( COOKIE_NAME );

            this.setProperty( "eeSearchString", command.getEeSearchString() );
            this.setProperty( "searchString", command.getSearchString() );
            this.setProperty( "stringency", command.getStringency() );
            this.setProperty( "taxonScientificName", command.getTaxon().getScientificName() );
            
            this.setMaxAge( 100000 );
            this.setComment( "Information for coexpression search form" );
        }

    }
}