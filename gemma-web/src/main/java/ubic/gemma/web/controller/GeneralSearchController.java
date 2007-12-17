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
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonService;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.search.SearchResult;
import ubic.gemma.search.SearchService;
import ubic.gemma.search.SearchSettings;
import ubic.gemma.util.EntityUtils;
import ubic.gemma.web.propertyeditor.TaxonPropertyEditor;
import ubic.gemma.web.remote.ListRange;

/**
 * @author klc
 * @version $Id$
 * @spring.bean id="generalSearchController"
 * @spring.property name = "commandName" value="searchSettings"
 * @spring.property name = "commandClass" value="ubic.gemma.search.SearchSettings"
 * @spring.property name="formView" value="generalSearch"
 * @spring.property name="successView" value="generalSearch"
 * @spring.property name="searchService" ref="searchService"
 * @spring.property name = "expressionExperimentService" ref="expressionExperimentService"
 * @spring.property name = "arrayDesignService" ref="arrayDesignService"
 * @spring.property name = "gene2GOAssociationService" ref="gene2GOAssociationService"
 * @spring.property name = "taxonService" ref="taxonService"
 */
public class GeneralSearchController extends BaseFormController {
    private SearchService searchService;

    private ExpressionExperimentService expressionExperimentService;

    private ArrayDesignService arrayDesignService;

    private Gene2GOAssociationService gene2GOAssociationService;

    private TaxonService taxonService;

    /**
     * @return the arrayDesignService
     */
    public ArrayDesignService getArrayDesignService() {
        return arrayDesignService;
    }

    /**
     * @return the expressionExperimentService
     */
    public ExpressionExperimentService getExpressionExperimentService() {
        return expressionExperimentService;
    }

    /**
     * @return the gene2GOAssociationService
     */
    public Gene2GOAssociationService getGene2GOAssociationService() {
        return gene2GOAssociationService;
    }

    @Override
    @SuppressWarnings( { "unused", "unchecked" })
    public ModelAndView onSubmit( HttpServletRequest request, HttpServletResponse response, Object command,
            BindException errors ) throws Exception {

        SearchSettings settings = ( SearchSettings ) command;

        String[] advanced = request.getParameterValues( "advancedSelect" );
        Map test = request.getParameterMap();
        ModelAndView mav = super.showForm( request, errors, getSuccessView() );

        mav.addObject( "searchDataset", "DataSet" );
        mav.addObject( "searchGene", "Gene" );
        mav.addObject( "searchArray", "Array" );
        mav.addObject( "searchCompositeSequence", "CompositeSequence" );
        mav.addObject( "searchBioSequence", "bioSequence" );
        mav.addObject( "searchGoID", "GoID" );
        mav.addObject( "searchOntology", "ontology" );
        mav.addObject( "searchBibliographicReference", "bibliographicReference" );
        mav.addObject( "searchBioSequence", "bioSequence" );

        // first check - searchString should allow searches of 3 characters or
        // more ONLY
        // this is to prevent a huge wildcard search

        if ( !searchStringValidator( settings.getQuery() ) ) {
            this.saveMessage( request, "Must use at least three characters for search" );
            log.info( "User entered an invalid search: " + settings.getQuery() );
            return super.showForm( request, errors, getSuccessView() );
        }

        log.info( "General search for " + settings );

        // Need this infor for the bookmarkable links
        mav.addObject( "SearchString", settings.getQuery() );
        if ( ( settings.getTaxon() != null ) && ( settings.getTaxon().getId() != null ) )
            mav.addObject( "searchTaxon", settings.getTaxon().getScientificName() );

        /*
         * Here is where the magic happens.
         */
        Map<Class, List<SearchResult>> searchResults = searchService.search( settings );

        for ( Class clazz : searchResults.keySet() ) {
            List<SearchResult> results = searchResults.get( clazz );

            if ( results.size() == 0 ) continue;

            log.info( "Search result: " + results.size() + " " + clazz.getSimpleName() + "'s" );

            /*
             * Now put the valueObjects inside the SearchResults
             */
            fillValueObjects( clazz, results, settings );

            mav.addObject( clazz.getSimpleName() + "_results", results );
            mav.addObject( clazz.getSimpleName() + "_count", results.size() );
        }

        return mav;
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

    /**
     * AJAX - all objects in one collection.
     * 
     * @param settings
     * @return
     */
    public ListRange search( SearchSettings settings ) {
        List<SearchResult> finalResults = new ArrayList<SearchResult>();
        if ( settings == null || StringUtils.isBlank( settings.getQuery() )
                || StringUtils.isBlank( settings.getQuery().replaceAll( "\\*", "" ) ) ) {
            // FIXME validate input better, and return error.
            log.info( "No query or invalid." );
            // return new ListRange( finalResults );
            throw new IllegalArgumentException( "Query " + settings.getQuery() + " was invalid" );
        }
        log.info( "Search initiated: " + settings );
        Map<Class, List<SearchResult>> searchResults = searchService.search( settings );

        /*
         * FIXME sort by the number of hits per class, so smallest number of hits is at the top.
         */
        
        for ( Class clazz : searchResults.keySet() ) {
            List<SearchResult> results = searchResults.get( clazz );
            
            if ( results.size() == 0 ) continue;

            log.info( "Search result: " + results.size() + " " + clazz.getSimpleName() + "'s" );

            /*
             * Now put the valueObjects inside the SearchResults in score order.
             */
            Collections.sort( results );
            fillValueObjects( clazz, results, settings );
            finalResults.addAll( results );
        }
      
        return new ListRange( finalResults );
    }

    /**
     * @param arrayDesignService the arrayDesignService to set
     */
    public void setArrayDesignService( ArrayDesignService arrayDesignService ) {
        this.arrayDesignService = arrayDesignService;
    }

    /**
     * @param expressionExperimentService the expressionExperimentService to set
     */
    public void setExpressionExperimentService( ExpressionExperimentService expressionExperimentService ) {
        this.expressionExperimentService = expressionExperimentService;
    }

    /**
     * @param gene2GOAssociationService the gene2GOAssociationService to set
     */
    public void setGene2GOAssociationService( Gene2GOAssociationService gene2GOAssociationService ) {
        this.gene2GOAssociationService = gene2GOAssociationService;
    }

    /**
     * @param searchService the searchService to set
     */
    public void setSearchService( SearchService searchService ) {
        this.searchService = searchService;
    }

    /**
     * @param taxonService the taxonService to set
     */
    public void setTaxonService( TaxonService taxonService ) {
        this.taxonService = taxonService;
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
        return new SearchSettings();
    }

    @Override
    protected void initBinder( HttpServletRequest request, ServletRequestDataBinder binder ) {
        super.initBinder( request, binder );
        binder.registerCustomEditor( Taxon.class, new TaxonPropertyEditor( this.taxonService ) );
    }

    @SuppressWarnings("unused")
    @Override
    protected Map referenceData( HttpServletRequest request ) {
        Map<String, List<? extends Object>> mapping = new HashMap<String, List<? extends Object>>();

        // add species
        populateTaxonReferenceData( mapping );

        return mapping;
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
            SearchSettings csc = ( SearchSettings ) this.formBackingObject( request );
            String taxon = request.getParameter( "taxon" );
            if ( taxon != null ) csc.setTaxon( taxonService.findByScientificName( taxon ) );
            return this.onSubmit( request, response, csc, errors );
        }

        return super.showForm( request, response, errors );
    }

    /**
     * @param entityClass
     * @param results
     * @return ValueObjects for the entities (in some cases, this is just the entities again). They are returned in the
     *         same order as the entities.
     */
    @SuppressWarnings("unchecked")
    private void fillValueObjects( Class entityClass, List<SearchResult> results, SearchSettings settings ) {
        Collection vos = null;

        Map<Long, SearchResult> rMap = new HashMap<Long, SearchResult>();
        for ( SearchResult searchResult : results ) {
            rMap.put( searchResult.getId(), searchResult );
        }

        if ( ExpressionExperiment.class.isAssignableFrom( entityClass ) ) {
            vos = filterEEByTaxon( expressionExperimentService.loadValueObjects( EntityUtils.getIds( results ) ),
                    settings );
        } else if ( ArrayDesign.class.isAssignableFrom( entityClass ) ) {
            vos = filterADByTaxon( arrayDesignService.loadValueObjects( EntityUtils.getIds( results ) ), settings );
        } else if ( CompositeSequence.class.isAssignableFrom( entityClass ) ) {
            return;
        } else if ( BibliographicReference.class.isAssignableFrom( entityClass ) ) {
            return;
        } else if ( Gene.class.isAssignableFrom( entityClass ) ) {
            return;
        } else if ( Characteristic.class.isAssignableFrom( entityClass ) ) {
            return;
        } else if ( BioSequence.class.isAssignableFrom( entityClass ) ) {
            return;
        } else {
            throw new UnsupportedOperationException( "Don't know how to make value objects for class=" + entityClass );
        }

        for ( Object o : vos ) {
            Long id = EntityUtils.getId( o );
            rMap.get( id ).setResultObject( o );
        }
    }

    /**
     * @param toFilter
     * @param tax
     * @return
     */
    private Collection<ArrayDesignValueObject> filterADByTaxon( final Collection<ArrayDesignValueObject> toFilter,
            SearchSettings settings ) {
        Taxon tax = settings.getTaxon();
        if ( tax == null ) return toFilter;
        Collection<ArrayDesignValueObject> filtered = new HashSet<ArrayDesignValueObject>();
        for ( ArrayDesignValueObject eevo : toFilter ) {
            if ( ( eevo.getTaxon() == null ) || ( eevo.getTaxon().equalsIgnoreCase( tax.getCommonName() ) ) )
                filtered.add( eevo );
        }

        return filtered;
    }

    /**
     * @param toFilter
     * @param settings
     * @return
     */
    private Collection<ExpressionExperimentValueObject> filterEEByTaxon(
            final Collection<ExpressionExperimentValueObject> toFilter, SearchSettings settings ) {
        Taxon tax = settings.getTaxon();
        if ( tax == null ) return toFilter;
        Collection<ExpressionExperimentValueObject> filtered = new HashSet<ExpressionExperimentValueObject>();
        for ( ExpressionExperimentValueObject eevo : toFilter ) {
            if ( eevo.getTaxon().equalsIgnoreCase( tax.getCommonName() ) ) filtered.add( eevo );
        }

        return filtered;
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

    private boolean searchStringValidator( String query ) {
        if ( StringUtils.isBlank( query ) ) return false;
        if ( ( query.charAt( 0 ) == '%' ) || ( query.charAt( 0 ) == '*' ) ) return false;
        return true;
    }

}
