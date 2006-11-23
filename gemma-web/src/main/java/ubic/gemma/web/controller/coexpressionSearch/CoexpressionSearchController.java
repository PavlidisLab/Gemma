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

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List; 
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.validation.BindException;
import org.springframework.web.bind.ServletRequestDataBinder;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ubic.gemma.loader.genome.taxon.SupportedTaxa;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequenceService;
import ubic.gemma.model.expression.designElement.DesignElement;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonService;
import ubic.gemma.model.genome.gene.GeneService;
import ubic.gemma.web.controller.BaseFormController;
import ubic.gemma.web.propertyeditor.ArrayDesignPropertyEditor;
import ubic.gemma.web.propertyeditor.TaxonPropertyEditor;

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
        return csc;

    }

    /**
     * @param request
     * @param response
     * @param command
     * @param errors
     * @return ModelAndView
     * @throws Exception
     */
/*
    @SuppressWarnings("unchecked")
    @Override
    public ModelAndView processFormSubmission( HttpServletRequest request, HttpServletResponse response,
            Object command, BindException errors ) throws Exception {

        log.debug( "entering processFormSubmission" );

        CoexpressionSearchCommand csc = ( ( CoexpressionSearchCommand ) command );

        if ( request.getParameter( "cancel" ) != null ) {
            log.info( "Canceled" );
            return new ModelAndView( new RedirectView( "/Gemma/mainMenu.html" ));

        }

        return super.processFormSubmission( request, response, command, errors );
    }
*/
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

        log.debug( "entering onSubmit" );
        
        CoexpressionSearchCommand csc = ( ( CoexpressionSearchCommand ) command );
        
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
            if (!gene.getTaxon().getCommonName().equalsIgnoreCase( csc.getTaxon().getCommonName() ) ) {
                genesToRemove.add( gene );
            }
        }
        genesFound.removeAll( genesToRemove );

        // if no genes found
        // return error 
        if (genesFound.size() == 0) {
            saveMessage( request, "No genes found based on criteria." );
            ModelAndView mav = new ModelAndView(getFormView());
            mav.addObject( "coexpressionSearchCommand", csc );
            return mav;
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
}