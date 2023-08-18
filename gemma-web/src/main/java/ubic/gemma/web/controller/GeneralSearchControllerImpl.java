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

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindException;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import ubic.gemma.core.search.DefaultHighlighter;
import ubic.gemma.core.search.SearchException;
import ubic.gemma.core.search.SearchResult;
import ubic.gemma.core.search.SearchService;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSet;
import ubic.gemma.model.association.phenotype.PhenotypeAssociation;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.search.SearchSettings;
import ubic.gemma.model.common.search.SearchSettingsValueObject;
import ubic.gemma.model.expression.BlacklistedEntity;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.gene.GeneSet;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;
import ubic.gemma.web.propertyeditor.TaxonPropertyEditor;
import ubic.gemma.web.remote.JsonReaderResponse;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import static org.apache.commons.text.StringEscapeUtils.escapeHtml4;

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
    private TaxonService taxonService;
    @Autowired
    private MessageSource messageSource;
    @Autowired
    private ServletContext servletContext;
    @Autowired
    private HttpServletRequest request;

    @Override
    public JsonReaderResponse<SearchResultValueObject<?>> ajaxSearch( SearchSettingsValueObject settingsValueObject ) {
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
                .withHighlighter( new Highlighter( request.getLocale() ) );

        searchTimer.start();
        SearchService.SearchResultMap searchResults;
        try {
            searchResults = searchService.search( searchSettings );
        } catch ( SearchException e ) {
            throw new IllegalArgumentException( String.format( "Invalid search settings: %s.", ExceptionUtils.getRootCause( e ) ), e );
        }
        searchTimer.stop();

        // FIXME: sort by the number of hits per class, so the smallest number of hits is at the top.
        fillVosTimer.start();
        List<SearchResultValueObject<?>> finalResults = new ArrayList<>();
        for ( Class<? extends Identifiable> clazz : searchResults.getResultTypes() ) {
            List<SearchResult<?>> results = searchResults.getByResultType( clazz );

            if ( results.size() == 0 )
                continue;

            BaseFormController.log.debug( String.format( "Search for: %s; result: %d %ss",
                    searchSettings, results.size(), clazz.getSimpleName() ) );

            /*
             * Now put the valueObjects inside the SearchResults in score order.
             */
            searchService.loadValueObjects( results ).stream()
                    .sorted()
                    .map( SearchResultValueObject::new )
                    .forEachOrdered( finalResults::add );
        }

        fillVosTimer.stop();

        timer.stop();

        if ( timer.getTime() > 500 ) {
            BaseFormController.log
                    .warn( "Searching for query: " + searchSettings + " took " + timer.getTime() + " ms ("
                            + "searching: " + searchTimer.getTime() + " ms, "
                            + "filling VOs: " + fillVosTimer.getTime() + " ms)." );
        }

        return new JsonReaderResponse<>( finalResults );
    }

    private class Highlighter extends DefaultHighlighter {

        private final Locale locale;

        private Highlighter( Locale locale ) {
            this.locale = locale;
        }

        @Override
        public String highlightTerm( String uri, String value, MessageSourceResolvable className ) {
            String matchedText;
            try {
                matchedText = "<a href=\"" + servletContext.getContextPath() + "/searcher.html?query=" + URLEncoder.encode( uri, StandardCharsets.UTF_8.name() ) + "\">" + escapeHtml4( value ) + "</a> ";
            } catch ( UnsupportedEncodingException e ) {
                throw new RuntimeException( e );
            }
            if ( !ArrayUtils.contains( className.getCodes(), ExpressionExperiment.class.getName() ) ) {
                matchedText = matchedText + " via " + messageSource.getMessage( className, locale );
            }
            return matchedText;
        }

        @Override
        public Map<String, String> highlightDocument( Document document, org.apache.lucene.search.highlight.Highlighter highlighter, Analyzer analyzer, String[] fields ) {
            return super.highlightDocument( document, highlighter, analyzer, fields )
                    .entrySet().stream()
                    .collect( Collectors.toMap( e -> localizeDocumentField( document, e.getKey() ), Map.Entry::getValue, ( a, b ) -> b ) );
        }

        private String localizeDocumentField( Document document, String key ) {
            String hibernateClass = document.get( "_hibernate_class" );
            return messageSource.getMessage( StringUtils.substringAfterLast( hibernateClass, '.' ) + "." + key, null, key, locale );
        }
    }

    @Override
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
        csc.highlighter( new Highlighter( request.getLocale() ) );
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
    public ModelAndView showForm( HttpServletRequest request, HttpServletResponse response, BindException errors ) {
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
        List<Taxon> taxa = new ArrayList<>( taxonService.loadAll() );
        taxa.sort( Comparator.comparing( Taxon::getScientificName ) );
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
                .maxResults( settingsValueObject.getMaxResults() != null ? settingsValueObject.getMaxResults() : SearchSettings.DEFAULT_MAX_RESULTS_PER_RESULT_TYPE )
                .resultTypes( resultTypesFromVo( settingsValueObject ) )
                .resultType( BlacklistedEntity.class )
                .useIndices( settingsValueObject.getUseIndices() )
                .useDatabase( settingsValueObject.getUseDatabase() )
                .useCharacteristics( settingsValueObject.getUseCharacteristics() )
                .useGo( settingsValueObject.getUseGo() )
                .build();
    }

    private static Set<Class<? extends Identifiable>> resultTypesFromVo( SearchSettingsValueObject valueObject ) {
        Set<Class<? extends Identifiable>> ret = new HashSet<>();
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
