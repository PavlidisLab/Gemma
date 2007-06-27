/*
 * The Gemma project
 * 
 * Copyright (c) 2007 Columbia University
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
package ubic.gemma.web.controller;

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

import org.apache.commons.lang.StringUtils;
import org.springframework.validation.BindException;
import org.springframework.web.bind.ServletRequestDataBinder;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ubic.gemma.loader.genome.taxon.SupportedTaxa;
import ubic.gemma.model.association.Gene2GOAssociationService;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonService;
import ubic.gemma.search.SearchService;
import ubic.gemma.web.controller.coexpressionSearch.CoexpressionSearchCommand;
import ubic.gemma.web.propertyeditor.TaxonPropertyEditor;

/**
 * @author klc
 * @version $Id$
 * @spring.bean id="generalSearchController"
 * @spring.property name = "commandName" value="coexpressionSearchCommand"
 * @spring.property name = "commandClass" value="ubic.gemma.web.controller.coexpressionSearch.CoexpressionSearchCommand"
 * @spring.property name="formView" value="generalSearch"
 * @spring.property name="successView" value="generalSearch"
 * @spring.property name="searchService" ref="searchService"
 * @spring.property name = "expressionExperimentService" ref="expressionExperimentService"
 * @spring.property name = "arrayDesignService" ref="arrayDesignService"
 * @spring.property name = "gene2GOAssociationService" ref="gene2GOAssociationService"
 * @spring.property name = "taxonService" ref="taxonService"
 */
public class GeneralSearchController extends BaseFormController {

    protected SearchService searchService;
    protected ExpressionExperimentService expressionExperimentService;
    protected ArrayDesignService arrayDesignService;
    protected Gene2GOAssociationService gene2GOAssociationService;
    protected TaxonService taxonService;

    @Override
    @SuppressWarnings("unused")
    public ModelAndView onSubmit( HttpServletRequest request, HttpServletResponse response, Object command,
            BindException errors ) throws Exception {

        CoexpressionSearchCommand csc = ( CoexpressionSearchCommand ) command;
        String searchString = request.getParameter( "searchString" );
        String[] advanced = request.getParameterValues( "advancedSelect" );
        Map test = request.getParameterMap();
        ModelAndView mav = super.showForm( request, errors, getSuccessView() );

        // TODO create a search command object replacing all this parsing junk
        boolean dataset = false;
        boolean gene = false;
        boolean array = false;
        boolean goID = false;
        boolean ontology = false;
        boolean goArray = false;

        if ( ( advanced == null ) || ( advanced.length == 0 ) ) {
            dataset = true;
            gene = true;
            array = true;
            goID = false;
            ontology = false;
            goArray = false;

            mav.addObject( "searchDataset", "DataSet" );
            mav.addObject( "searchGene", "Gene" );
            mav.addObject( "searchArray", "Array" );

        } else {
            for ( String types : advanced ) {
                if ( types.equalsIgnoreCase( "DataSet" ) ) {
                    dataset = true;
                    mav.addObject( "searchDataset", "DataSet" );
                }

                if ( types.equalsIgnoreCase( "Gene" ) ) {
                    gene = true;
                    mav.addObject( "searchGene", "Gene" );
                }

                if ( types.equalsIgnoreCase( "Array" ) ) {
                    array = true;
                    mav.addObject( "searchArray", "Array" );
                }

                if ( types.equalsIgnoreCase( "GoID" ) ) {
                    goID = true;
                    mav.addObject( "searchGoID", "GoID" );
                }

                if ( types.equalsIgnoreCase( "ontology" ) ) {
                    ontology = true;
                    mav.addObject( "searchOntology", "ontology" );
                }

                if ( types.equalsIgnoreCase( "ADbyGoID" ) ) {
                    goArray = true;
                    mav.addObject( "searchADbyGoID", "ADbyGoID" );
                }
            }
        }
        // first check - searchString should allow searches of 3 characters or more ONLY
        // this is to prevent a huge wildcard search

        if ( !searchStringValidator( searchString ) ) {
            this.saveMessage( request, "Must use at least three characters for search" );
            log.info( "User entered an invalid search" );
            return super.showForm( request, errors, getSuccessView() );
        }

        log.info( "Attempting general search" );

        // Need this infor for the bookmarkable links
        mav.addObject( "SearchString", searchString );
        if ( ( csc.getTaxon() != null ) && ( csc.getTaxon().getId() != null ) )
            mav.addObject( "searchTaxon", csc.getTaxon().getScientificName() );

        if ( gene ) {
            Collection<Gene> foundGenes = searchService.geneSearch( searchString );
            if ( ( csc.getTaxon() != null ) && ( csc.getTaxon().getId() != null ) )
                foundGenes = filterGenesByTaxon( foundGenes, csc.getTaxon() );
            mav.addObject( "geneList", foundGenes );
            mav.addObject( "numGenes", foundGenes.size() );
        }

        if ( dataset ) {
            Collection<ExpressionExperiment> foundEEs = searchService.compassExpressionSearch( searchString );
            Collection<ExpressionExperimentValueObject> valueEEs = expressionExperimentService
                    .loadValueObjects( generateEEIdList( foundEEs ) );

            if ( ( csc.getTaxon() != null ) && ( csc.getTaxon().getId() != null ) )
                valueEEs = filterEEByTaxon( valueEEs, csc.getTaxon() );

            mav.addObject( "expressionList", valueEEs );
            mav.addObject( "numEEs", valueEEs.size() );
        }

        if ( array ) {
            Collection<ArrayDesign> foundADs = searchService.arrayDesignSearch( searchString );
            Collection<ArrayDesignValueObject> valueADs = arrayDesignService
                    .loadValueObjects( generateADIdList( foundADs ) );

            if ( ( csc.getTaxon() != null ) && ( csc.getTaxon().getId() != null ) )
                valueADs = filterADByTaxon( valueADs, csc.getTaxon() );

            mav.addObject( "arrayList", valueADs );
            mav.addObject( "numADs", valueADs.size() );
        }

        if ( goID ) {
            Collection<Gene> ontolgyGenes = gene2GOAssociationService.findByGOTerm( searchString, csc.getTaxon() );
            mav.addObject( "goGeneList", ontolgyGenes );
            mav.addObject( "numGoGenes", ontolgyGenes.size() );

        }

        if ( ontology ) {
            Collection<Characteristic> ontolgyEntries = searchService.compassOntologySearch( searchString );
            mav.addObject( "ontologyList", ontolgyEntries );
            mav.addObject( "numOntologyList", ontolgyEntries.size() );

        }

        if ( goArray ) {
            Collection<ArrayDesign> foundADs = arrayDesignService.findByGoId( searchString );
            Collection<ArrayDesignValueObject> valueADs = arrayDesignService
                    .loadValueObjects( generateADIdList( foundADs ) );

            if ( ( csc.getTaxon() != null ) && ( csc.getTaxon().getId() != null ) )
                valueADs = filterADByTaxon( valueADs, csc.getTaxon() );

            mav.addObject( "goArrayList", valueADs );
            mav.addObject( "numGoADs", valueADs.size() );
        }

        return mav;
    }

    private Collection<Gene> filterGenesByTaxon( final Collection<Gene> toFilter, Taxon tax ) {
        Collection<Gene> filtered = new HashSet<Gene>();
        for ( Gene g : toFilter ) {
            if ( g.getTaxon() == tax ) filtered.add( g );
        }

        return filtered;
    }

    private Collection<ExpressionExperimentValueObject> filterEEByTaxon(
            final Collection<ExpressionExperimentValueObject> toFilter, Taxon tax ) {
        Collection<ExpressionExperimentValueObject> filtered = new HashSet<ExpressionExperimentValueObject>();
        for ( ExpressionExperimentValueObject eevo : toFilter ) {
            if ( eevo.getTaxon().equalsIgnoreCase( tax.getCommonName() ) ) filtered.add( eevo );
        }

        return filtered;
    }

    private Collection<ArrayDesignValueObject> filterADByTaxon( final Collection<ArrayDesignValueObject> toFilter,
            Taxon tax ) {
        Collection<ArrayDesignValueObject> filtered = new HashSet<ArrayDesignValueObject>();
        for ( ArrayDesignValueObject eevo : toFilter ) {
            if ( ( eevo.getTaxon() == null ) || ( eevo.getTaxon().equalsIgnoreCase( tax.getCommonName() ) ) )
                filtered.add( eevo );
        }

        return filtered;
    }

    /**
     * 
     */
    @Override
    public ModelAndView processFormSubmission( HttpServletRequest request, HttpServletResponse response,
            Object command, BindException errors ) throws Exception {

        if ( request.getParameter( "cancel" ) != null ) {
            this.saveMessage( request, "Cancelled Search" );
            return new ModelAndView( new RedirectView( "mainMenu.html" ) );
        }

        return super.processFormSubmission( request, response, command, errors );
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.web.servlet.mvc.SimpleFormController#showForm(javax.servlet.http.HttpServletRequest,
     *      javax.servlet.http.HttpServletResponse, org.springframework.validation.BindException)
     */
    @Override
    protected ModelAndView showForm( HttpServletRequest request, HttpServletResponse response, BindException errors )
            throws Exception {
        if ( request.getParameter( "searchString" ) != null ) {
            CoexpressionSearchCommand csc = ( CoexpressionSearchCommand ) this.formBackingObject( request );
            String taxon = request.getParameter( "taxon" );
            if ( taxon != null ) csc.setTaxon( taxonService.findByScientificName( taxon ) );
            return this.onSubmit( request, response, csc, errors );
        }

        return super.showForm( request, response, errors );
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
        return new CoexpressionSearchCommand();
    }

    /**
     * @param searchService the searchService to set
     */
    public void setSearchService( SearchService searchService ) {
        this.searchService = searchService;
    }

    protected boolean searchStringValidator( String query ) {

        if ( StringUtils.isBlank( query ) ) return false;

        if ( ( query.charAt( 0 ) == '%' ) || ( query.charAt( 0 ) == '*' ) ) return false;

        return true;
    }

    protected Collection<Long> generateEEIdList( Collection<ExpressionExperiment> searchResults ) {
        Collection<Long> list = new ArrayList<Long>();

        for ( ExpressionExperiment ee : searchResults ) {
            if ( ee == null ) continue;
            list.add( ee.getId() );
        }

        return list;
    }

    protected Collection<Long> generateADIdList( Collection<ArrayDesign> searchResults ) {
        Collection<Long> list = new ArrayList<Long>();

        for ( ArrayDesign ad : searchResults ) {
            if ( ad == null ) continue;
            list.add( ad.getId() );
        }

        return list;
    }

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
    private void populateTaxonReferenceData( Map mapping ) {
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
     * @return the arrayDesignService
     */
    public ArrayDesignService getArrayDesignService() {
        return arrayDesignService;
    }

    /**
     * @param arrayDesignService the arrayDesignService to set
     */
    public void setArrayDesignService( ArrayDesignService arrayDesignService ) {
        this.arrayDesignService = arrayDesignService;
    }

    /**
     * @return the expressionExperimentService
     */
    public ExpressionExperimentService getExpressionExperimentService() {
        return expressionExperimentService;
    }

    /**
     * @param expressionExperimentService the expressionExperimentService to set
     */
    public void setExpressionExperimentService( ExpressionExperimentService expressionExperimentService ) {
        this.expressionExperimentService = expressionExperimentService;
    }

    /**
     * @param taxonService the taxonService to set
     */
    public void setTaxonService( TaxonService taxonService ) {
        this.taxonService = taxonService;
    }

    /**
     * @return the gene2GOAssociationService
     */
    public Gene2GOAssociationService getGene2GOAssociationService() {
        return gene2GOAssociationService;
    }

    /**
     * @param gene2GOAssociationService the gene2GOAssociationService to set
     */
    public void setGene2GOAssociationService( Gene2GOAssociationService gene2GOAssociationService ) {
        this.gene2GOAssociationService = gene2GOAssociationService;
    }

}
