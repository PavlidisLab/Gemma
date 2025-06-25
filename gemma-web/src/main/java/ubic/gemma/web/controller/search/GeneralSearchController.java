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
import org.springframework.web.util.UriComponentsBuilder;
import ubic.gemma.core.search.*;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSet;
import ubic.gemma.model.blacklist.BlacklistedEntity;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.model.common.IdentifiableValueObject;
import ubic.gemma.model.common.search.SearchSettings;
import ubic.gemma.model.common.search.SearchSettingsValueObject;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.GeneSet;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;
import ubic.gemma.web.controller.util.view.JsonReaderResponse;

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
    private static final Scope[] scopes = {
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
    private TaxonService taxonService;
    @Autowired
    private MessageSource messageSource;

    @Autowired
    private HttpServletRequest request;

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
            searchService.loadValueObjects( results ).stream()
                    .sorted()
                    .map( SearchResultValueObject::new )
                    .forEachOrdered( finalResults::add );
        }

        fillVosTimer.stop();

        timer.stop();

        if ( timer.getTime() > 500 ) {
            GeneralSearchController.log.warn( String.format( "Searching for query: %s took %d ms (searching: %d ms, filling VOs: %d ms).",
                    searchSettings, timer.getTime(), searchTimer.getTime(), fillVosTimer.getTime() ) );
        }

        return new JsonReaderResponse<>( finalResults );
    }

    @RequestMapping(value = "/searcher.html", method = { RequestMethod.GET, RequestMethod.HEAD })
    public ModelAndView search(
            @RequestParam(value = "query", required = false) String query,
            @RequestParam(value = "termUri", required = false) String termUri,
            @RequestParam(value = "taxon", required = false) String taxon,
            @RequestParam(value = "scope", required = false) String scope ) {
        ModelAndView mav = new ModelAndView( "generalSearch" );
        if ( query != null || termUri != null ) {
            if ( query != null ) {
                query = StringUtils.strip( query );
                if ( StringUtils.isBlank( query ) ) {
                    throw new IllegalArgumentException( "The query cannot be blank." );
                } else if ( query.charAt( 0 ) == '*' ) {
                    throw new IllegalArgumentException( "The query cannot start with a wildcard." );
                }
            }
            if ( termUri != null ) {
                try {
                    new URI( termUri );
                } catch ( URISyntaxException e ) {
                    throw new IllegalArgumentException( "Invalid term URI: " + termUri, e );
                }
            }
            Taxon t = null;
            if ( taxon != null ) {
                t = getTaxon( taxon );
                if ( t == null ) {
                    throw new IllegalArgumentException( "Unknown taxon " + taxon );
                }
            }
            if ( StringUtils.isNotBlank( scope ) ) {
                for ( char s : scope.toCharArray() ) {
                    boolean found = false;
                    for ( Scope s2 : GeneralSearchController.scopes ) {
                        if ( s2.scope == s ) {
                            found = true;
                            break;
                        }
                    }
                    if ( !found ) {
                        throw new IllegalArgumentException( String.format( "Unsupported value for scope: %c.", s ) );
                    }
                }
            }
            // Need this for the bookmarkable links
            mav.addObject( "SearchString", query != null ? query : termUri );
            mav.addObject( "SearchURI", termUri );
            if ( t != null ) {
                mav.addObject( "searchTaxon", t.getScientificName() );
            }
        }
        return mav;
    }

    private Taxon getTaxon( String taxon ) {
        try {
            return taxonService.load( Long.parseLong( taxon ) );
        } catch ( NumberFormatException e ) {
            // ignored
        }
        Taxon t = taxonService.findByScientificName( taxon );
        if ( t == null ) {
            t = taxonService.findByCommonName( taxon );
        }
        return t;
    }

    private SearchSettings searchSettingsFromVo( SearchSettingsValueObject settingsValueObject ) {
        return SearchSettings.builder()
                .query( !StringUtils.isBlank( settingsValueObject.getQuery() ) ? settingsValueObject.getQuery() : settingsValueObject.getTermUri() )
                .platformConstraint( settingsValueObject.getPlatformConstraint() )
                .taxonConstraint( settingsValueObject.getTaxon() )
                .maxResults( settingsValueObject.getMaxResults() != null ? settingsValueObject.getMaxResults() : SearchSettings.DEFAULT_MAX_RESULTS_PER_RESULT_TYPE )
                .resultTypes( resultTypesFromVo( settingsValueObject ) )
                .resultType( BlacklistedEntity.class )
                .useIndices( settingsValueObject.getUseIndices() )
                .useDatabase( settingsValueObject.getUseDatabase() )
                .useCharacteristics( settingsValueObject.getUseCharacteristics() )
                .useGo( settingsValueObject.getUseGo() )
                .build();
    }

    private String scopeFromVo( SearchSettingsValueObject settingsValueObject ) {
        Set<Class<? extends Identifiable>> resultTypes = resultTypesFromVo( settingsValueObject );
        StringBuilder scope = new StringBuilder();
        for ( Class<? extends Identifiable> resultType : resultTypes ) {
            for ( Scope s : scopes ) {
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
        if ( valueObject.getSearchProbes() ) {
            ret.add( CompositeSequence.class );
        }
        if ( valueObject.getSearchGeneSets() ) {
            ret.add( GeneSet.class );
        }
        return ret;
    }

    @Value
    public static class SearchResultValueObject<T extends IdentifiableValueObject<?>> {

        Class<?> resultClass;
        double score;
        String highlightedText;
        IdentifiableValueObject<?> resultObject;

        public SearchResultValueObject( SearchResult<T> result ) {
            this.resultClass = result.getResultType();
            this.score = result.getScore();
            if ( result.getHighlights() != null ) {
                this.highlightedText = result.getHighlights().entrySet().stream()
                        .map( e -> String.format( "Tagged %s: %s", e.getKey(), e.getValue() ) )
                        .collect( Collectors.joining( "<br/>" ) );
            } else {
                this.highlightedText = null;
            }
            this.resultObject = result.getResultObject();
        }
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
