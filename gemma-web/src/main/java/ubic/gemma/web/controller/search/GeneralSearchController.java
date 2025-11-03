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
package ubic.gemma.web.controller.search;

import lombok.Value;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.util.UriComponentsBuilder;
import ubic.gemma.core.search.DefaultHighlighter;
import ubic.gemma.core.search.SearchContext;
import ubic.gemma.core.search.SearchException;
import ubic.gemma.core.search.SearchService;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSet;
import ubic.gemma.model.blacklist.BlacklistedEntity;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.model.common.search.SearchResult;
import ubic.gemma.model.common.search.SearchResultValueObject;
import ubic.gemma.model.common.search.SearchSettings;
import ubic.gemma.model.common.search.SearchSettingsValueObject;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.GeneSet;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;
import ubic.gemma.web.controller.util.view.JsonReaderResponse;
import ubic.gemma.web.util.WebEntityUrlBuilder;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.servlet.http.HttpServletRequest;
import java.net.URI;
import java.net.URISyntaxException;
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
@CommonsLog
public class GeneralSearchController {

    /**
     * Maximum number of highlighted documents.
     */
    private static final int MAX_HIGHLIGHTED_DOCUMENTS = 500;

    @Value
    private static class Scope {
        char scope;
        Class<? extends Identifiable> resultType;
    }

    /**
     * List of supported scopes (or result types) for searching.
     */
    private static final Scope[] SCOPES = {
            new Scope( 'G', Gene.class ),
            new Scope( 'E', ExpressionExperiment.class ),
            new Scope( 'P', CompositeSequence.class ),
            new Scope( 'A', ArrayDesign.class ),
            new Scope( 'M', GeneSet.class ),
            new Scope( 'N', ExpressionExperimentSet.class )
    };

    @Autowired
    private SearchService searchService;
    @Autowired
    private ArrayDesignService arrayDesignService;
    @Autowired
    private ExpressionExperimentService expressionExperimentService;
    @Autowired
    private TaxonService taxonService;
    @Autowired
    private MessageSource messageSource;
    @Autowired
    private WebEntityUrlBuilder entityUrlBuilder;

    @Autowired
    private HttpServletRequest request;

    @RequestMapping(value = "/searcher.html", method = { RequestMethod.GET, RequestMethod.HEAD })
    public Object search(
            @RequestParam(value = "dataset", required = false) String dataset,
            @RequestParam(value = "platform", required = false) String platform,
            @RequestParam(value = "taxon", required = false) String taxon,
            @RequestParam(value = "scope", required = false) String scope,
            @RequestParam(value = "searchDatabase", required = false, defaultValue = "true") boolean useDatabase,
            @RequestParam(value = "searchIndices", required = false, defaultValue = "true") boolean useFullTextIndex,
            @RequestParam(value = "searchCharacteristics", required = false, defaultValue = "true") boolean useOntology,
            @RequestParam(value = "searchGO", required = false, defaultValue = "false") boolean useGeneOntology,
            @RequestParam(value = "noRedirect", required = false, defaultValue = "false") boolean noRedirect ) {
        return search( null, null, dataset, platform, taxon, scope, useDatabase, useFullTextIndex, useOntology, useGeneOntology, noRedirect );
    }

    @RequestMapping(value = "/searcher.html", method = { RequestMethod.GET, RequestMethod.HEAD }, params = "query")
    public Object searchByQuery(
            @RequestParam(value = "query") String query,
            @RequestParam(value = "dataset", required = false) String dataset,
            @RequestParam(value = "platform", required = false) String platform,
            @RequestParam(value = "taxon", required = false) String taxon,
            @RequestParam(value = "scope", required = false) String scope,
            @RequestParam(value = "searchDatabase", required = false, defaultValue = "true") boolean useDatabase,
            @RequestParam(value = "searchIndices", required = false, defaultValue = "true") boolean useFullTextIndex,
            @RequestParam(value = "searchCharacteristics", required = false, defaultValue = "true") boolean useOntology,
            @RequestParam(value = "searchGO", required = false, defaultValue = "false") boolean useGeneOntology,
            @RequestParam(value = "noRedirect", required = false, defaultValue = "false") boolean noRedirect ) {
        return search( query, null, dataset, platform, taxon, scope, useDatabase, useFullTextIndex, useOntology, useGeneOntology, noRedirect );
    }

    @RequestMapping(value = "/searcher.html", method = { RequestMethod.GET, RequestMethod.HEAD }, params = "termUri")
    public Object searchByUri(
            @RequestParam(value = "termUri") String termUri,
            @RequestParam(value = "dataset", required = false) String dataset,
            @RequestParam(value = "platform", required = false) String platform,
            @RequestParam(value = "taxon", required = false) String taxon,
            @RequestParam(value = "scope", required = false) String scope,
            @RequestParam(value = "searchDatabase", required = false, defaultValue = "true") boolean useDatabase,
            @RequestParam(value = "searchIndices", required = false, defaultValue = "true") boolean useFullTextIndex,
            @RequestParam(value = "searchCharacteristics", required = false, defaultValue = "true") boolean useOntology,
            @RequestParam(value = "searchGO", required = false, defaultValue = "false") boolean useGeneOntology,
            @RequestParam(value = "noRedirect", required = false, defaultValue = "false") boolean noRedirect ) {
        return search( null, termUri, dataset, platform, taxon, scope, useDatabase, useFullTextIndex, useOntology, useGeneOntology, noRedirect );
    }

    private Object search( @Nullable String query, @Nullable String termUri, String dataset, @Nullable String platform, @Nullable String taxon,
            @Nullable String scope,
            boolean useDatabase,
            boolean useFullTextIndex,
            boolean useOntology,
            boolean useGeneOntology,
            boolean noRedirect ) {
        ModelAndView mav = new ModelAndView( "generalSearch" );
        if ( query != null ) {
            query = StringUtils.strip( query );
            if ( StringUtils.isBlank( query ) ) {
                throw new IllegalArgumentException( "The query cannot be blank." );
            } else if ( query.charAt( 0 ) == '*' ) {
                throw new IllegalArgumentException( "The query cannot start with a wildcard." );
            }
            mav.addObject( "SearchString", query );
        } else if ( termUri != null ) {
            try {
                new URI( termUri );
            } catch ( URISyntaxException e ) {
                throw new IllegalArgumentException( "Invalid term URI: " + termUri, e );
            }
            mav.addObject( "SearchURI", termUri );
        } else {
            // a blank search page, without prior results
        }

        ExpressionExperiment d = null;
        if ( dataset != null ) {
            d = getDataset( dataset );
            if ( d == null ) {
                throw new IllegalArgumentException( "Unknown dataset " + dataset );
            }
            mav.addObject( "searchDataset", d );
        }

        ArrayDesign p = null;
        if ( platform != null ) {
            p = getPlatform( platform );
            if ( p == null ) {
                throw new IllegalArgumentException( "Unknown platform: " + platform );
            }
            mav.addObject( "searchPlatform", p );
        }

        Taxon t = null;
        if ( taxon != null ) {
            t = getTaxon( taxon );
            if ( t == null ) {
                throw new IllegalArgumentException( "Unknown taxon " + taxon );
            }
            mav.addObject( "searchTaxon", t.getScientificName() );
        }

        // check for a quick-redirect
        Set<Scope> scopes = getScopes( scope );
        mav.addObject( "scope", scope );

        if ( ( query != null || termUri != null ) && scopes.size() == 1 && !noRedirect ) {
            Scope scopeObj = scopes.iterator().next();
            try {
                List<SearchResult<?>> exactResults = searchService.search( SearchSettings.builder()
                                .resultType( scopeObj.resultType )
                                .mode( SearchSettings.SearchMode.EXACT )
                                .query( query != null ? query : termUri )
                                .fillResults( false )
                                .experimentConstraint( d )
                                .platformConstraint( p )
                                .taxonConstraint( t )
                                .useDatabase( useDatabase )
                                .useFullTextIndex( useFullTextIndex )
                                .useOntology( useOntology )
                                .useGeneOntology( useGeneOntology )
                                .build() )
                        .getByResultType( scopeObj.resultType );
                if ( exactResults.size() == 1 ) {
                    SearchResult<?> result = exactResults.iterator().next();
                    try {
                        String url = getResultObjectUrl( result );
                        return new RedirectView( url );
                    } catch ( UnsupportedOperationException e ) {
                        log.warn( "Cannot generate a URL for " + result.getResultType() + " with ID " + result.getResultId() + ", will not perform a quick redirect.", e );
                    }
                }
            } catch ( SearchException e ) {
                log.warn( "Failed to perform search, will render the page anyway, but the client might submit the same query again via AJAX.", e );
            }
        }

        return mav;
    }

    /**
     * AJAX
     */
    @SuppressWarnings("unused")
    public JsonReaderResponse<SearchResultValueObject<?>> ajaxSearch( SearchSettingsValueObject settingsValueObject ) {
        if ( settingsValueObject == null || StringUtils.isBlank( settingsValueObject.getQuery() ) || StringUtils
                .isBlank( settingsValueObject.getQuery().replaceAll( "\\*", "" ) ) ) {
            // FIXME validate input better, and return error.
            GeneralSearchController.log.info( "No query or invalid." );
            // return new ListRange( finalResults );
            throw new IllegalArgumentException( "Query '" + settingsValueObject + "' was invalid" );
        }

        StopWatch timer = new StopWatch();
        StopWatch searchTimer = new StopWatch();
        StopWatch fillVosTimer = new StopWatch();
        timer.start();

        SearchSettings searchSettings = searchSettingsFromVo( settingsValueObject );
        Highlighter highlighter = new Highlighter( scopeFromVo( settingsValueObject ), request.getLocale() );

        searchTimer.start();
        SearchService.SearchResultMap searchResults;
        try {
            searchResults = searchService.search( searchSettings, new SearchContext( highlighter, null ) );
        } catch ( SearchException e ) {
            throw new IllegalArgumentException( String.format( "Invalid search settings: %s.", ExceptionUtils.getRootCause( e ) ), e );
        }
        searchTimer.stop();

        // FIXME: sort by the number of hits per class, so the smallest number of hits is at the top.
        fillVosTimer.start();
        List<SearchResultValueObject<?>> finalResults = new ArrayList<>();
        for ( Class<? extends Identifiable> clazz : searchResults.getResultTypes() ) {
            List<SearchResult<?>> results = searchResults.getByResultType( clazz );

            if ( results.isEmpty() )
                continue;

            GeneralSearchController.log.debug( String.format( "Search for: %s; result: %d %ss",
                    searchSettings, results.size(), clazz.getSimpleName() ) );

            /*
             * Now put the valueObjects inside the SearchResults in score order.
             */
            List<UnsupportedOperationException> exceptions = new ArrayList<>();
            searchService.loadValueObjects( results ).stream()
                    .sorted()
                    .map( sr -> new SearchResultValueObject<>( sr, getResultObjectUrlSafely( sr, exceptions ) ) )
                    .forEachOrdered( finalResults::add );
            if ( !exceptions.isEmpty() ) {
                Iterator<UnsupportedOperationException> it = exceptions.iterator();
                UnsupportedOperationException e = it.next();
                it.forEachRemaining( e::addSuppressed );
                log.warn( "Failed to generate URLs for " + exceptions.size() + " entities.", e );
            }
        }

        fillVosTimer.stop();

        timer.stop();

        if ( timer.getTime() > 500 ) {
            GeneralSearchController.log.warn( String.format( "Searching for query: %s took %d ms (searching: %d ms, filling VOs: %d ms).",
                    searchSettings, timer.getTime(), searchTimer.getTime(), fillVosTimer.getTime() ) );
        }

        return new JsonReaderResponse<>( finalResults );
    }

    @Nullable
    private ExpressionExperiment getDataset( String datasetIdentifier ) {
        ExpressionExperiment ee;
        try {
            if ( ( ee = expressionExperimentService.load( Long.parseLong( datasetIdentifier ) ) ) != null ) {
                return ee;
            }
        } catch ( NumberFormatException e ) {
            // ignore
        }
        if ( ( ee = expressionExperimentService.findByShortName( datasetIdentifier ) ) != null ) {
            return ee;
        }
        return null;
    }

    @Nullable
    private ArrayDesign getPlatform( String platformIdentifier ) {
        ArrayDesign ad;
        try {
            if ( ( ad = arrayDesignService.load( Long.parseLong( platformIdentifier ) ) ) != null ) {
                return ad;
            }
        } catch ( NumberFormatException e ) {
            // ignore
        }
        if ( ( ad = arrayDesignService.findByShortName( platformIdentifier ) ) != null ) {
            return ad;
        }
        return null;
    }

    @Nullable
    private Taxon getTaxon( String taxonIdentifier ) {
        Taxon taxon;
        try {
            if ( ( taxon = taxonService.load( Long.parseLong( taxonIdentifier ) ) ) != null ) {
                return taxon;
            }
            if ( ( taxon = taxonService.findByNcbiId( Integer.parseInt( taxonIdentifier ) ) ) != null ) {
                return taxon;
            }
        } catch ( NumberFormatException e ) {
            // ignored
        }
        if ( ( taxon = taxonService.findByScientificName( taxonIdentifier ) ) != null ) {
            return taxon;
        }
        if ( ( taxon = taxonService.findByCommonName( taxonIdentifier ) ) != null ) {
            return taxon;
        }
        return null;
    }

    private Set<Scope> getScopes( @Nullable String scope ) {
        Set<Scope> scopes = new HashSet<>();
        if ( StringUtils.isNotBlank( scope ) ) {
            for ( char s : scope.toCharArray() ) {
                boolean found = false;
                for ( Scope s2 : GeneralSearchController.SCOPES ) {
                    if ( s2.scope == s ) {
                        found = true;
                        scopes.add( s2 );
                        break;
                    }
                }
                if ( !found ) {
                    throw new IllegalArgumentException( String.format( "Unsupported value for scope: %c.", s ) );
                }
            }
        }
        return scopes;
    }

    @Nullable
    private String getResultObjectUrlSafely( SearchResult<?> searchResult, Collection<UnsupportedOperationException> exceptions ) {
        try {
            return getResultObjectUrl( searchResult );
        } catch ( UnsupportedOperationException e ) {
            exceptions.add( e );
            return null;
        }
    }

    private String getResultObjectUrl( SearchResult<?> searchResult ) {
        return entityUrlBuilder
                .fromContextPath()
                .entity( searchResult.getResultType(), searchResult.getResultId() )
                .toUriString();
    }

    private SearchSettings searchSettingsFromVo( SearchSettingsValueObject settingsValueObject ) {
        return SearchSettings.builder()
                .query( !StringUtils.isBlank( settingsValueObject.getQuery() ) ? settingsValueObject.getQuery() : settingsValueObject.getTermUri() )
                .experimentConstraint( getDataset( settingsValueObject.getDatasetConstraint() ) )
                .platformConstraint( getPlatform( settingsValueObject.getPlatformConstraint() ) )
                .taxonConstraint( getTaxon( settingsValueObject.getTaxonConstraint() ) )
                .maxResults( settingsValueObject.getMaxResults() != null ? settingsValueObject.getMaxResults() : SearchSettings.DEFAULT_MAX_RESULTS_PER_RESULT_TYPE )
                .resultTypes( resultTypesFromVo( settingsValueObject ) )
                .resultType( BlacklistedEntity.class )
                .useFullTextIndex( settingsValueObject.isUseIndices() )
                .useDatabase( settingsValueObject.isUseDatabase() )
                .useOntology( settingsValueObject.isUseCharacteristics() )
                .useGeneOntology( settingsValueObject.isUseGo() )
                .build();
    }

    private String scopeFromVo( SearchSettingsValueObject settingsValueObject ) {
        Set<Class<? extends Identifiable>> resultTypes = resultTypesFromVo( settingsValueObject );
        StringBuilder scope = new StringBuilder();
        for ( Class<? extends Identifiable> resultType : resultTypes ) {
            for ( Scope s : SCOPES ) {
                if ( resultType.equals( s.resultType ) ) {
                    scope.append( s.scope );
                    break;
                }
            }
        }
        return scope.toString();
    }

    private static Set<Class<? extends Identifiable>> resultTypesFromVo( SearchSettingsValueObject valueObject ) {
        Set<Class<? extends Identifiable>> ret = new HashSet<>();
        if ( valueObject.isSearchExperiments() ) {
            ret.add( ExpressionExperiment.class );
        }
        if ( valueObject.isSearchGenes() ) {
            ret.add( Gene.class );
        }
        if ( valueObject.isSearchPlatforms() ) {
            ret.add( ArrayDesign.class );
        }
        if ( valueObject.isSearchExperimentSets() ) {
            ret.add( ExpressionExperimentSet.class );
        }
        if ( valueObject.isSearchProbes() ) {
            ret.add( CompositeSequence.class );
        }
        if ( valueObject.isSearchGeneSets() ) {
            ret.add( GeneSet.class );
        }
        return ret;
    }

    @ParametersAreNonnullByDefault
    private class Highlighter extends DefaultHighlighter {

        @Nullable
        private final String scope;
        private final Locale locale;

        private int highlightedDocuments = 0;

        private Highlighter( @Nullable String scope, Locale locale ) {
            this.scope = scope;
            this.locale = locale;
        }

        @Override
        public Map<String, String> highlightTerm( @Nullable String uri, String value, String field ) {
            // some of the incoming requests are from AJAX, so we cannot use fromRequest
            UriComponentsBuilder builder = ServletUriComponentsBuilder.fromContextPath( request )
                    .scheme( null ).host( null ).port( -1 )
                    .path( "/searcher.html" )
                    .queryParam( "query", uri != null ? uri : value );
            if ( scope != null ) {
                builder.queryParam( "scope", scope );
            }
            String searchUrl = builder.build().toUriString();
            String matchedText = "<a href=\"" + searchUrl + "\">" + escapeHtml4( value ) + "</a> ";
            return Collections.singletonMap( localizeField( "ExpressionExperiment", field ), matchedText );
        }

        @Override
        public Map<String, String> highlightDocument( Document document, org.apache.lucene.search.highlight.Highlighter highlighter, Analyzer analyzer ) {
            if ( highlightedDocuments >= MAX_HIGHLIGHTED_DOCUMENTS ) {
                return Collections.emptyMap();
            }
            highlightedDocuments++;
            return super.highlightDocument( document, highlighter, analyzer )
                    .entrySet().stream()
                    .collect( Collectors.toMap( e -> localizeField( StringUtils.substringAfterLast( document.get( "_hibernate_class" ), '.' ), e.getKey() ), Map.Entry::getValue, ( a, b ) -> b ) );
        }

        private String localizeField( String className, String field ) {
            return messageSource.getMessage( className + "." + field, null, field, locale );
        }
    }
}
