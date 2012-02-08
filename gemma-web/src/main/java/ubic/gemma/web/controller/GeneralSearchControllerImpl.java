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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindException;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ubic.gemma.model.analysis.expression.ExpressionExperimentSet;
import ubic.gemma.model.association.Gene2GOAssociationService;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.BibliographicReferenceService;
import ubic.gemma.model.common.description.BibliographicReferenceValueObject;
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
import ubic.gemma.model.genome.gene.GeneService;
import ubic.gemma.model.genome.gene.GeneSet;
import ubic.gemma.expression.experiment.DatabaseBackedExpressionExperimentSetValueObject;
import ubic.gemma.expression.experiment.ExpressionExperimentSetService;
import ubic.gemma.genome.gene.DatabaseBackedGeneSetValueObject;
import ubic.gemma.genome.gene.service.GeneSetService;
import ubic.gemma.model.genome.gene.GeneValueObject;
import ubic.gemma.model.genome.sequenceAnalysis.BioSequenceValueObject;
import ubic.gemma.search.SearchResult;
import ubic.gemma.search.SearchService;
import ubic.gemma.search.SearchSettings;
import ubic.gemma.security.SecurityServiceImpl;
import ubic.gemma.security.audit.AuditableUtil;
import ubic.gemma.util.EntityUtils;
import ubic.gemma.web.propertyeditor.TaxonPropertyEditor;
import ubic.gemma.web.remote.JsonReaderResponse;

/**
 * @author klc
 * @version $Id$
 */
@Controller
public class GeneralSearchControllerImpl extends BaseFormController implements GeneralSearchController {
    @Autowired
    private SearchService searchService;

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private ArrayDesignService arrayDesignService;

    @Autowired
    private Gene2GOAssociationService gene2GOAssociationService;

    @Autowired
    private GeneService geneService;

    @Autowired
    private BibliographicReferenceService bibliographicReferenceService;

    @Autowired
    private TaxonService taxonService;

    @Autowired
    private AuditableUtil auditableUtil;

    @Autowired
    private GeneSetService geneSetService;

    @Autowired
    private ExpressionExperimentSetService experimentSetService;

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.web.controller.GeneralSearchController#doSearch(javax.servlet.http.HttpServletRequest,
     * javax.servlet.http.HttpServletResponse, ubic.gemma.search.SearchSettings,
     * org.springframework.validation.BindException)
     */
    @Override
    @RequestMapping(value = "/searcher.html", method = RequestMethod.POST)
    public ModelAndView doSearch( HttpServletRequest request, HttpServletResponse response, SearchSettings command,
            BindException errors ) throws Exception {

        SearchSettings settings = command;

        settings.setQuery( StringUtils.trim( settings.getQuery().trim() ) );

        ModelAndView mav = new ModelAndView( "generalSearch" );

        if ( !searchStringValidator( settings.getQuery() ) ) {
            this.saveMessage( request, "Invalid search string" );
            log.info( "User entered an invalid search: " + settings.getQuery() );
            return mav;
        }

        // Need this for the bookmarkable links
        mav.addObject( "SearchString", settings.getQuery() );
        if ( ( settings.getTaxon() != null ) && ( settings.getTaxon().getId() != null ) )
            mav.addObject( "searchTaxon", settings.getTaxon().getScientificName() );

        return mav;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.web.controller.GeneralSearchController#processFormSubmission(javax.servlet.http.HttpServletRequest,
     * javax.servlet.http.HttpServletResponse, ubic.gemma.search.SearchSettings,
     * org.springframework.validation.BindException)
     */
    @Override
    public ModelAndView processFormSubmission( HttpServletRequest request, HttpServletResponse response,
            SearchSettings command, BindException errors ) throws Exception {

        if ( request.getParameter( "query" ) != null ) {
            ModelAndView mav = new ModelAndView();
            mav.addObject( "query", request.getParameter( "query" ) );
            return mav;
        }

        if ( request.getParameter( "cancel" ) != null ) {
            this.saveMessage( request, "Cancelled Search" );
            return new ModelAndView( new RedirectView( WebConstants.HOME_PAGE ) );
        }

        return this.doSearch( request, response, command, errors );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.web.controller.GeneralSearchController#search(ubic.gemma.search.SearchSettings)
     */
    @Override
    public JsonReaderResponse<SearchResult> search( SearchSettings settings ) {
        List<SearchResult> finalResults = new ArrayList<SearchResult>();
        if ( settings == null || StringUtils.isBlank( settings.getQuery() )
                || StringUtils.isBlank( settings.getQuery().replaceAll( "\\*", "" ) ) ) {
            // FIXME validate input better, and return error.
            log.info( "No query or invalid." );
            // return new ListRange( finalResults );
            throw new IllegalArgumentException( "Query '" + settings + "' was invalid" );
        }
        StopWatch watch = new StopWatch();
        watch.start();
        Map<Class<?>, List<SearchResult>> searchResults = searchService.search( settings );
        watch.stop();

        if ( watch.getTime() > 500 ) {
            log.info( "Search service work on: " + settings + " took " + watch.getTime() + " ms" );
        }

        /*
         * FIXME sort by the number of hits per class, so smallest number of hits is at the top.
         */
        watch.reset();
        watch.start();

        if ( searchResults != null ) {
            for ( Class<?> clazz : searchResults.keySet() ) {
                List<SearchResult> results = searchResults.get( clazz );

                if ( results.size() == 0 ) continue;

                log.info( "Search for: " + settings + "; result: " + results.size() + " " + clazz.getSimpleName() + "s" );

                /*
                 * Now put the valueObjects inside the SearchResults in score order.
                 */
                Collections.sort( results );
                fillValueObjects( clazz, results, settings );
                finalResults.addAll( results );
            }
        }

        if ( watch.getTime() > 500 ) {
            log.info( "Final unpacking of results for query:" + settings + " took " + watch.getTime() + " ms" );
        }

        return new JsonReaderResponse<SearchResult>( finalResults );
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
    @InitBinder
    protected void initBinder( WebDataBinder binder ) {
        super.initBinder( binder );
        binder.registerCustomEditor( Taxon.class, new TaxonPropertyEditor( this.taxonService ) );
    }

    @Override
    protected Map<String, List<? extends Object>> referenceData( HttpServletRequest request ) {
        Map<String, List<? extends Object>> mapping = new HashMap<String, List<? extends Object>>();

        // add species
        populateTaxonReferenceData( mapping );

        return mapping;
    }

    /*
     * This is where "GET" requests go, e.g. from a 'bookmarkable link'.
     * 
     * @see org.springframework.web.servlet.mvc.SimpleFormController#showForm(javax.servlet.http.HttpServletRequest,
     * javax.servlet.http.HttpServletResponse, org.springframework.validation.BindException)
     */
    @Override
    @RequestMapping(value = "/searcher.html", method = RequestMethod.GET)
    protected ModelAndView showForm( HttpServletRequest request, HttpServletResponse response, BindException errors )
            throws Exception {
        if ( request.getParameter( "query" ) != null || request.getParameter( "termUri" ) != null ) {
            SearchSettings csc = ( SearchSettings ) this.formBackingObject( request );
            csc.setQuery( request.getParameter( "query" ) );
            csc.setTermUri( request.getParameter( "termUri" ) );
            String taxon = request.getParameter( "taxon" );
            if ( taxon != null ) csc.setTaxon( taxonService.findByScientificName( taxon ) );

            String scope = request.getParameter( "scope" );
            if ( StringUtils.isNotBlank( scope ) ) {
                char[] scopes = scope.toCharArray();
                for ( int i = 0; i < scopes.length; i++ ) {
                    switch ( scopes[i] ) {
                        case 'G':
                            csc.setSearchGenes( true );
                            break;
                        case 'E':
                            csc.setSearchExperiments( true );
                            break;
                        case 'S':
                            csc.setSearchBioSequences( true );
                            break;
                        case 'P':
                            csc.setSearchProbes( true );
                            break;
                        case 'A':
                            csc.setSearchArrays( true );
                            break;
                        case 'M':
                            csc.setSearchGeneSets( true );
                            break;
                        case 'N':
                            csc.setSearchExperimentSets( true );
                            break;
                        default:
                            break;
                    }
                }
            } else {
                csc.setGeneralSearch( true );
            }

            return this.doSearch( request, response, csc, errors );
        }

        return new ModelAndView( "generalSearch" );
    }

    /**
     * @param entityClass
     * @param results
     * @return ValueObjects for the entities (in some cases, this is just the entities again). They are returned in the
     *         same order as the entities.
     */
    @SuppressWarnings("unchecked")
    private void fillValueObjects( Class entityClass, List<SearchResult> results, SearchSettings settings ) {
        StopWatch timer = new StopWatch();
        timer.start();
        Collection vos = null;

        if ( ExpressionExperiment.class.isAssignableFrom( entityClass ) ) {
            vos = filterEE( expressionExperimentService.loadValueObjects( EntityUtils.getIds( results ) ), settings );

            if ( !SecurityServiceImpl.isUserAdmin() ) {
                auditableUtil.removeTroubledEes( vos );
            }

        } else if ( ArrayDesign.class.isAssignableFrom( entityClass ) ) {
            vos = filterAD( arrayDesignService.loadValueObjects( EntityUtils.getIds( results ) ), settings );

            if ( !SecurityServiceImpl.isUserAdmin() ) {
                auditableUtil.removeTroubledArrayDesigns( vos );
            }
        } else if ( CompositeSequence.class.isAssignableFrom( entityClass ) ) {
            return;
        } else if ( BibliographicReference.class.isAssignableFrom( entityClass ) ) {
            vos = BibliographicReferenceValueObject.convert2ValueObjects( bibliographicReferenceService
                    .loadMultiple( EntityUtils.getIds( results ) ) );
        } else if ( Gene.class.isAssignableFrom( entityClass ) ) {
            vos = GeneValueObject.convert2ValueObjects( geneService.loadMultiple( EntityUtils.getIds( results ) ) );
        } else if ( Characteristic.class.isAssignableFrom( entityClass ) ) {
            return;
        } else if ( BioSequenceValueObject.class.isAssignableFrom( entityClass ) ) {
            return;
        } else if ( GeneSet.class.isAssignableFrom( entityClass ) ) {
            vos = geneSetService.getValueObjects( EntityUtils.getIds( results ) );
        } else if ( ExpressionExperimentSet.class.isAssignableFrom( entityClass ) ) {
            Collection<ExpressionExperimentSet> eeSets = experimentSetService.validateForFrontEnd( experimentSetService
                    .load( EntityUtils.getIds( results ) ) );
            vos = DatabaseBackedExpressionExperimentSetValueObject.makeValueObjects( eeSets );
        } else {
            throw new UnsupportedOperationException( "Don't know how to make value objects for class=" + entityClass );
        }

        // retained objects...
        Map<Long, Object> idMap = EntityUtils.getIdMap( vos );

        for ( Iterator<SearchResult> it = results.iterator(); it.hasNext(); ) {
            SearchResult sr = it.next();
            if ( !idMap.containsKey( sr.getId() ) ) {
                it.remove();
                continue;
            }
            sr.setResultObject( idMap.get( sr.getId() ) );
        }

        if ( timer.getTime() > 1000 ) {
            log.info( "Value object conversion after search: " + timer.getTime() + "ms" );
        }
    }

    /**
     * @param toFilter
     * @param tax
     * @return
     */
    private Collection<ArrayDesignValueObject> filterAD( final Collection<ArrayDesignValueObject> toFilter,
            SearchSettings settings ) {
        Taxon tax = settings.getTaxon();
        if ( tax == null ) return toFilter;
        Collection<ArrayDesignValueObject> filtered = new HashSet<ArrayDesignValueObject>();
        for ( ArrayDesignValueObject aavo : toFilter ) {
            if ( ( aavo.getTaxon() == null ) || ( aavo.getTaxon().equalsIgnoreCase( tax.getCommonName() ) ) ) {
                filtered.add( aavo );
            }
        }

        return filtered;
    }

    /**
     * @param toFilter
     * @param settings
     * @return
     */
    private Collection<ExpressionExperimentValueObject> filterEE(
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
        for ( Taxon taxon : taxonService.loadAll() ) {
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
