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

import gemma.gsec.util.SecurityUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindException;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import ubic.gemma.core.annotation.reference.BibliographicReferenceService;
import ubic.gemma.core.genome.gene.service.GeneService;
import ubic.gemma.core.genome.gene.service.GeneSetService;
import ubic.gemma.core.search.SearchException;
import ubic.gemma.core.search.SearchResult;
import ubic.gemma.core.search.SearchService;
import ubic.gemma.core.security.audit.AuditableUtil;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSet;
import ubic.gemma.model.association.phenotype.PhenotypeAssociation;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.search.SearchSettings;
import ubic.gemma.model.common.search.SearchSettingsValueObject;
import ubic.gemma.model.expression.BlacklistedEntity;
import ubic.gemma.model.expression.BlacklistedValueObject;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.gene.GeneSet;
import ubic.gemma.model.genome.gene.phenotype.valueObject.CharacteristicValueObject;
import ubic.gemma.model.genome.sequenceAnalysis.BioSequenceValueObject;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.designElement.CompositeSequenceService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentSetService;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;
import ubic.gemma.persistence.util.EntityUtils;
import ubic.gemma.web.propertyeditor.TaxonPropertyEditor;
import ubic.gemma.web.remote.JsonReaderResponse;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Note: do not use parametrized collections as parameters for ajax methods in this class! Type information is lost
 * during proxy creation so DWR can't figure out what type of collection the method should take. See bug 2756. Use
 * arrays instead.
 *
 * @author klc
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
    @Autowired
    private CompositeSequenceService compositeSequenceService;

    @Override
    public JsonReaderResponse<SearchResult> ajaxSearch( SearchSettingsValueObject settingsValueObject ) {
        StopWatch timer = new StopWatch();
        StopWatch searchTimer = new StopWatch();
        StopWatch fillVosTimer = new StopWatch();

        if ( settingsValueObject == null || StringUtils.isBlank( settingsValueObject.getQuery() ) || StringUtils
                .isBlank( settingsValueObject.getQuery().replaceAll( "\\*", "" ) ) ) {
            // FIXME validate input better, and return error.
            BaseFormController.log.info( "No query or invalid." );
            // return new ListRange( finalResults );
            throw new IllegalArgumentException( "Query '" + settingsValueObject + "' was invalid" );
        }

        timer.start();

        SearchSettings searchSettings = searchSettingsFromVo( settingsValueObject )
                .withDoHighlighting( true );

        searchTimer.start();
        Map<Class<?>, List<SearchResult<?>>> searchResults;
        try {
            searchResults = searchService.search( searchSettings );
        } catch ( SearchException e ) {
            throw new IllegalArgumentException( "Invalid search settings.", e );
        }
        searchTimer.stop();

        // FIXME: sort by the number of hits per class, so the smallest number of hits is at the top.
        fillVosTimer.start();
        List<SearchResult> finalResults = new ArrayList<>();
        if ( searchResults != null ) {
            for ( Class<?> clazz : searchResults.keySet() ) {
                List<SearchResult<?>> results = searchResults.get( clazz );

                if ( results.size() == 0 )
                    continue;

                BaseFormController.log

                        .info( "Search for: " + searchSettings + "; result: " + results.size() + " " + clazz.getSimpleName()
                                + "s" );

                /*
                 * Now put the valueObjects inside the SearchResults in score order.
                 */
                results.sort( SearchResult.getComparator() );
                this.fillValueObjects( clazz, results, searchSettings );
                finalResults.addAll( results );
            }
        }
        fillVosTimer.stop();

        timer.stop();

        if ( timer.getTime() > 500 ) {
            BaseFormController.log
                    .info( "Searching for query: " + searchSettings + " took " + timer.getTime() + " ms ("
                            + "searching: " + searchTimer.getTime() + " ms, "
                            + "filling VOs: " + fillVosTimer.getTime() + " ms)." );
        }

        return new JsonReaderResponse<>( finalResults );
    }

    @Override
    @RequestMapping(value = "/searcher.html", method = RequestMethod.POST)
    public ModelAndView doSearch( HttpServletRequest request, HttpServletResponse response, SearchSettings command,
            BindException errors ) {

        command.setQuery( StringUtils.trim( command.getQuery().trim() ) );

        ModelAndView mav = new ModelAndView( "generalSearch" );

        if ( !this.searchStringValidator( command.getQuery() ) && StringUtils.isBlank( command.getTermUri() ) ) {
            throw new IllegalArgumentException( "Invalid query" );
        }

        // Need this for the bookmarkable links
        mav.addObject( "SearchString", command.getQuery() );
        mav.addObject( "SearchURI", command.getTermUri() );
        if ( ( command.getTaxon() != null ) && ( command.getTaxon().getId() != null ) )
            mav.addObject( "searchTaxon", command.getTaxon().getScientificName() );

        return mav;
    }

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
            return new ModelAndView( new RedirectView( WebConstants.HOME_PAGE, true ) );
        }

        return this.doSearch( request, response, command, errors );
    }

    /**
     * This is needed or you will have to specify a commandClass in the DispatcherServlet's context
     */
    @Override
    protected SearchSettings formBackingObject( HttpServletRequest request ) {
        SearchSettings.SearchSettingsBuilder csc = SearchSettings.builder();
        csc.query( !StringUtils.isBlank( request.getParameter( "query" ) ) ? request.getParameter( "query" ) : request.getParameter( "termUri" ) );
        String taxon = request.getParameter( "taxon" );
        if ( taxon != null )
            csc.taxon( taxonService.findByScientificName( taxon ) );

        String scope = request.getParameter( "scope" );
        if ( StringUtils.isNotBlank( scope ) ) {
            char[] scopes = scope.toCharArray();
            for ( char scope1 : scopes ) {
                switch ( scope1 ) {
                    case 'G':
                        csc.resultType( Gene.class );
                        break;
                    case 'E':
                        csc.resultType( ExpressionExperiment.class );
                        break;
                    case 'S':
                        csc.resultType( BioSequence.class );
                        break;
                    case 'P':
                        csc.resultType( CompositeSequence.class );
                        break;
                    case 'A':
                        csc.resultType( ArrayDesign.class );
                        break;
                    case 'M':
                        csc.resultType( GeneSet.class );
                        break;
                    case 'N':
                        csc.resultType( ExpressionExperimentSet.class );
                        break;
                    case 'H':
                    case 'B':
                        // FIXME: these two are passed by the frontend, but obviously not supported
                        break;
                    case ',':
                        break;
                    default:
                        // TODO: 400 Bad Request error?
                        log.warn( String.format( "Unsupported value for scope: %c.", scope1 ) );
                        break;
                }
            }
        }
        return csc.build();
    }

    @Override
    @InitBinder
    protected void initBinder( WebDataBinder binder ) {
        super.initBinder( binder );
        binder.registerCustomEditor( Taxon.class, new TaxonPropertyEditor( this.taxonService ) );
    }

    @Deprecated
    @Override
    @RequestMapping(value = "/searcher.html", method = RequestMethod.GET)
    protected ModelAndView showForm( HttpServletRequest request, HttpServletResponse response, BindException errors )
            throws Exception {
        if ( request.getParameter( "query" ) != null || request.getParameter( "termUri" ) != null ) {
            SearchSettings searchSettings = this.formBackingObject( request );
            return this.doSearch( request, response, searchSettings, errors );
        }

        return new ModelAndView( "generalSearch" );
    }

    @Override
    protected Map<String, List<?>> referenceData( HttpServletRequest request ) {
        Map<String, List<?>> mapping = new HashMap<>();

        // add species
        this.populateTaxonReferenceData( mapping );

        return mapping;
    }

    /**
     * Populate the search results with the value objects - we generally only have the entity class and ID (or, in some
     * cases, possibly the entity)
     * @param entityClass
     * @param results
     * @param settings
     */
    @SuppressWarnings("unchecked")
    private void fillValueObjects( Class<?> entityClass, List<SearchResult<?>> results, SearchSettings settings ) {
        StopWatch timer = StopWatch.createStarted();
        Collection<?> vos;

        Collection<Long> ids = new ArrayList<>();
        for ( SearchResult r : results ) {
            ids.add( r.getResultId() );
        }

        if ( ExpressionExperiment.class.isAssignableFrom( entityClass ) ) {
            vos = expressionExperimentService.loadValueObjectsByIds( ids );
            if ( !SecurityUtil.isUserAdmin() ) {
                auditableUtil.removeTroubledEes( ( Collection<ExpressionExperimentValueObject> ) vos );
            }

        } else if ( ArrayDesign.class.isAssignableFrom( entityClass ) ) {
            vos = this.filterAD( arrayDesignService.loadValueObjectsByIds( ids ), settings );

            if ( !SecurityUtil.isUserAdmin() ) {
                auditableUtil.removeTroubledArrayDesigns( ( Collection<ArrayDesignValueObject> ) vos );
            }
        } else if ( CompositeSequence.class.isAssignableFrom( entityClass ) ) {
            Collection<CompositeSequence> compositeSequences = results.stream().map( SearchResult::getResultObject )
                    .map( o -> ( CompositeSequence ) o )
                    .collect( Collectors.toSet() );
            vos = compositeSequenceService
                    .loadValueObjectsWithoutGeneMappingSummary( compositeSequences );
        } else if ( BibliographicReference.class.isAssignableFrom( entityClass ) ) {
            Collection<BibliographicReference> bss = bibliographicReferenceService
                    .load( ids );
            bss = bibliographicReferenceService.thaw( bss );
            vos = bibliographicReferenceService.loadValueObjects( bss );
        } else if ( Gene.class.isAssignableFrom( entityClass ) ) {
            Collection<Gene> genes = geneService.load( ids );
            genes = geneService.thawLite( genes );
            vos = geneService.loadValueObjects( genes );
        } else if ( CharacteristicValueObject.class.isAssignableFrom( entityClass ) ) {
            // This is used for phenotypes.
            Collection<CharacteristicValueObject> cvos = new ArrayList<>();
            for ( SearchResult sr : results ) {
                CharacteristicValueObject ch = ( CharacteristicValueObject ) sr.getResultObject();
                cvos.add( ch );
            }
            vos = cvos;
        } else if ( BioSequenceValueObject.class.isAssignableFrom( entityClass ) ) {
            return; // why?
        } else if ( GeneSet.class.isAssignableFrom( entityClass ) ) {
            vos = geneSetService.getValueObjects( ids );
        } else if ( ExpressionExperimentSet.class.isAssignableFrom( entityClass ) ) {
            vos = experimentSetService.loadValueObjects( experimentSetService.load( ids ) );
        } else if ( BlacklistedEntity.class.isAssignableFrom( entityClass ) ) {
            Collection<BlacklistedValueObject> bvos = new ArrayList<>();
            for ( SearchResult sr : results ) {
                bvos.add( BlacklistedValueObject.fromEntity( ( BlacklistedEntity ) sr.getResultObject() ) );
            }
            vos = bvos;
        } else {
            throw new UnsupportedOperationException( "Don't know how to make value objects for class=" + entityClass );
        }

        if ( vos == null || vos.isEmpty() ) {

            // bug 3475: if there are search results but they are all removed because they are troubled, then results
            // has ExpressionExperiments in
            // it causing front end errors, if vos is empty make sure to get rid of all search results
            for ( Iterator<SearchResult<?>> it = results.iterator(); it.hasNext(); ) {
                it.next();
                it.remove();
            }

            return;
        }

        // retained objects...
        Map<Long, Object> idMap = EntityUtils.getIdMap( vos );

        for ( Iterator<SearchResult<?>> it = results.iterator(); it.hasNext(); ) {
            SearchResult sr = it.next();
            if ( !idMap.containsKey( sr.getResultId() ) ) {
                it.remove();
                continue;
            }
            sr.setResultObject( idMap.get( sr.getResultId() ) );
        }

        timer.stop();

        if ( timer.getTime() > 200 ) {
            BaseFormController.log.info( "Value object conversion for " + ids.size() + " " + entityClass + " after search took " + timer.getTime() + " ms." );
        }
    }

    private Collection<ArrayDesignValueObject> filterAD( final Collection<ArrayDesignValueObject> toFilter,
            SearchSettings settings ) {
        // Note: if possible we should move filtering into the search service (as is done for EEs) (this is not a big deal)
        Taxon tax = settings.getTaxon();
        if ( tax == null )
            return toFilter;
        Collection<ArrayDesignValueObject> filtered = new HashSet<>();
        for ( ArrayDesignValueObject aavo : toFilter ) {
            if ( ( aavo.getTaxon() == null ) || ( aavo.getTaxon().equalsIgnoreCase( tax.getCommonName() ) ) ) {
                filtered.add( aavo );
            }
        }

        return filtered;
    }

    //    private Collection<ExpressionExperimentValueObject> filterEE(
    //            final Collection<ExpressionExperimentValueObject> toFilter, SearchSettings settings ) {
    //        Taxon tax = settings.getTaxon();
    //        if ( tax == null )
    //            return toFilter;
    //        Collection<ExpressionExperimentValueObject> filtered = new HashSet<>();
    //        for ( ExpressionExperimentValueObject eevo : toFilter ) {
    //            if ( eevo.getTaxon().equalsIgnoreCase( tax.getCommonName() ) )
    //                filtered.add( eevo );
    //        }
    //
    //        return filtered;
    //    }

    private void populateTaxonReferenceData( Map<String, List<?>> mapping ) {
        List<Taxon> taxa = new ArrayList<>();
        taxa.addAll( taxonService.loadAll() );
        Collections.sort( taxa, new Comparator<Taxon>() {
            @Override
            public int compare( Taxon o1, Taxon o2 ) {
                return ( o1 ).getScientificName().compareTo( ( o2 ).getScientificName() );
            }
        } );
        mapping.put( "taxa", taxa );
    }

    private static boolean searchStringValidator( String query ) {
        return !StringUtils.isBlank( query ) && !( ( query.charAt( 0 ) == '%' ) || ( query.charAt( 0 ) == '*' ) );
    }

    private static SearchSettings searchSettingsFromVo( SearchSettingsValueObject settingsValueObject ) {
        return SearchSettings.builder()
                .query( !StringUtils.isBlank( settingsValueObject.getQuery() ) ? settingsValueObject.getQuery() : settingsValueObject.getTermUri() )
                .platformConstraint( settingsValueObject.getPlatformConstraint() )
                .taxon( settingsValueObject.getTaxon() )
                .maxResults( settingsValueObject.getMaxResults() )
                .resultTypes( resultTypesFromVo( settingsValueObject ) )
                .useIndices( settingsValueObject.getUseIndices() )
                .useDatabase( settingsValueObject.getUseDatabase() )
                .useCharacteristics( settingsValueObject.getUseCharacteristics() )
                .useGo( settingsValueObject.getUseGo() )
                .build();
    }

    private static Set<Class<?>> resultTypesFromVo( SearchSettingsValueObject valueObject ) {
        Set<Class<?>> ret = new HashSet<>();
        if ( valueObject.getSearchExperiments() ) {
            ret.add( ExpressionExperiment.class );
        }
        if ( valueObject.getSearchGenes() ) {
            ret.add( Gene.class );
        }
        if ( valueObject.getSearchPlatforms() ) {
            ret.add( ArrayDesign.class );
        }
        if ( valueObject.getSearchExperimentSets() ) {
            ret.add( ExpressionExperimentSet.class );
        }
        if ( valueObject.getSearchPhenotypes() ) {
            ret.add( PhenotypeAssociation.class );
        }
        if ( valueObject.getSearchProbes() ) {
            ret.add( CompositeSequence.class );
        }
        if ( valueObject.getSearchGeneSets() ) {
            ret.add( GeneSet.class );
        }
        if ( valueObject.getSearchBioSequences() ) {
            ret.add( BioSequence.class );
        }
        if ( valueObject.getSearchBibrefs() ) {
            ret.add( BibliographicReference.class );
        }
        return ret;
    }
}
