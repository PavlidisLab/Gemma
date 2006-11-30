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
package ubic.gemma.web.controller.compass;

import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.compass.core.CompassCallback;
import org.compass.core.CompassDetachedHits;
import org.compass.core.CompassException;
import org.compass.core.CompassHits;
import org.compass.core.CompassQuery;
import org.compass.core.CompassSession;
import org.compass.core.CompassTransaction;
import org.compass.spring.web.mvc.AbstractCompassCommandController;
import org.compass.spring.web.mvc.CompassSearchCommand;
import org.compass.spring.web.mvc.CompassSearchResults;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;

/**
 * A general Spring's MVC Controller that perform the search operation of <code>Compass</code>. <p/> Will perform the
 * search operation on the <code>Compass</code> instance using the query supplied by the {@link CompassSearchCommand}.
 * <p/> If you wish to enable the pagination feature, you must set the <code>pageSize</code> property on the
 * controller, as well as providing the <code>page</code> number property on the {@link CompassSearchCommand}. <p/>
 * The controller has two views to be set, the <code>searchView</code>, which is the view that holds the screen which
 * the user will initiate the search operation, and the <code>searchResultsView</code>, which will show the results
 * of the search operation (they can be the same page). <p/> The results of the search operation will be saved under the
 * <code>searchResultsName<code>, which defaults to "searchResults".
 *
 * @author kimchy
 * @author keshav
 * @version $Id$
 * @spring.bean id="searchController"
 * @spring.property name = "compass" ref="compassExpression"
 * @spring.property name = "searchView" value="searcher"
 * @spring.property name = "searchResultsView" value="searcher"
 * @spring.property name = "pageSize" value="10"
 */
public class CustomCompassSearchController extends AbstractCompassCommandController {

    private String searchView;

    private String searchResultsView;

    private String searchResultsName = "searchResults";

    private Integer pageSize;

    /**
     * 
     *
     */
    public CustomCompassSearchController() {
        setCommandClass( CompassSearchCommand.class );
    }

    /**
     * @throws Exception
     */
    public void afterPropertiesSet() throws Exception {
        super.afterPropertiesSet();
        if ( searchView == null ) {
            throw new IllegalArgumentException( "Must set the searchView property" );
        }
        if ( searchResultsView == null ) {
            throw new IllegalArgumentException( "Must set hte serachResultsView property" );
        }
    }

    /**
     * @param request
     * @param response
     * @param command
     * @param errors
     * @return ModelAndView
     * @throws Exception
     */
    protected ModelAndView handle( HttpServletRequest request, HttpServletResponse response, Object command,
            BindException errors ) throws Exception {
        final CompassSearchCommand searchCommand = ( CompassSearchCommand ) command;
        if ( !StringUtils.hasText( searchCommand.getQuery() ) ) {
            return new ModelAndView( getSearchView(), getCommandName(), searchCommand );
        }
        CompassSearchResults searchResults;
        searchResults = ( CompassSearchResults ) getCompassTemplate().execute(
                CompassTransaction.TransactionIsolation.READ_ONLY_READ_COMMITTED, new CompassCallback() {
                    public Object doInCompass( CompassSession session ) throws CompassException {
                        return performSearch( searchCommand, session );
                    }
                } );
        HashMap data = new HashMap();
        data.put( getCommandName(), searchCommand );
        data.put( getSearchResultsName(), searchResults );
        return new ModelAndView( getSearchResultsView(), data );
    }

    /**
     * @param searchCommand
     * @param session
     * @return CompassSearchResults
     */
    protected CompassSearchResults performSearch( CompassSearchCommand searchCommand, CompassSession session ) {
        long time = System.currentTimeMillis();
        CompassQuery query = buildQuery( searchCommand, session );
        CompassHits hits = query.hits();
        CompassDetachedHits detachedHits;
        CompassSearchResults.Page[] pages = null;
        if ( pageSize == null ) {
            doProcessBeforeDetach( searchCommand, session, hits, -1, -1 );
            detachedHits = hits.detach();
        } else {
            int iPageSize = pageSize.intValue();
            int page = 0;
            int hitsLength = hits.getLength();
            if ( searchCommand.getPage() != null ) {
                page = searchCommand.getPage().intValue();
            }
            int from = page * iPageSize;
            if ( from > hits.getLength() ) {
                from = hits.getLength() - iPageSize;
                doProcessBeforeDetach( searchCommand, session, hits, from, hitsLength );
                detachedHits = hits.detach( from, hitsLength );
            } else if ( ( from + iPageSize ) > hitsLength ) {
                doProcessBeforeDetach( searchCommand, session, hits, from, hitsLength );
                detachedHits = hits.detach( from, hitsLength );
            } else {
                doProcessBeforeDetach( searchCommand, session, hits, from, iPageSize );
                detachedHits = hits.detach( from, iPageSize );
            }
            doProcessAfterDetach( searchCommand, session, detachedHits );
            int numberOfPages = ( int ) Math.ceil( ( float ) hitsLength / iPageSize );
            pages = new CompassSearchResults.Page[numberOfPages];
            for ( int i = 0; i < pages.length; i++ ) {
                pages[i] = new CompassSearchResults.Page();
                pages[i].setFrom( i * iPageSize + 1 );
                pages[i].setSize( iPageSize );
                pages[i].setTo( ( i + 1 ) * iPageSize );
                if ( from >= ( pages[i].getFrom() - 1 ) && from < pages[i].getTo() ) {
                    pages[i].setSelected( true );
                } else {
                    pages[i].setSelected( false );
                }
            }
            if ( numberOfPages > 0 ) {
                CompassSearchResults.Page lastPage = pages[numberOfPages - 1];
                if ( lastPage.getTo() > hitsLength ) {
                    lastPage.setSize( hitsLength - lastPage.getFrom() );
                    lastPage.setTo( hitsLength );
                }
            }
        }
        time = System.currentTimeMillis() - time;
        CompassSearchResults searchResults = new CompassSearchResults( detachedHits.getHits(), time );
        searchResults.setPages( pages );
        return searchResults;
    }

    /**
     * Acts as an extension point for search controller that wish to build different CompassQueries. <p/> The default
     * implementation uses the session to create a query builder and use the queryString option, i.e.:
     * <code>session.queryBuilder().queryString(searchCommand.getQuery().trim()).toQuery();</code>. <p/> Some other
     * interesting options might be to add sorting to the query, adding other queries using a boolean query, or
     * executing a different query.
     */
    protected CompassQuery buildQuery( CompassSearchCommand searchCommand, CompassSession session ) {
        return session.queryBuilder().queryString( searchCommand.getQuery().trim() ).toQuery();
    }

    /**
     * An option to perform any type of processing before the hits are detached.
     */
    protected void doProcessBeforeDetach( CompassSearchCommand searchCommand, CompassSession session, CompassHits hits,
            int from, int size ) {

    }

    /**
     * An option to perform any type of processing before the hits are detached.
     */
    protected void doProcessAfterDetach( CompassSearchCommand searchCommand, CompassSession session,
            CompassDetachedHits hits ) {

    }

    /**
     * Returns the view that holds the screen which the user will initiate the search operation.
     */
    public String getSearchView() {
        return searchView;
    }

    /**
     * Sets the view that holds the screen which the user will initiate the search operation.
     */
    public void setSearchView( String searchView ) {
        this.searchView = searchView;
    }

    /**
     * Returns the name of the results that the {@link CompassSearchResults} will be saved under. Defaults to
     * "searchResults".
     */
    public String getSearchResultsName() {
        return searchResultsName;
    }

    /**
     * Sets the name of the results that the {@link CompassSearchResults} will be saved under. Defaults to
     * "searchResults".
     */
    public void setSearchResultsName( String searchResultsName ) {
        this.searchResultsName = searchResultsName;
    }

    /**
     * Returns the view which will show the results of the search operation.
     */
    public String getSearchResultsView() {
        return searchResultsView;
    }

    /**
     * Sets the view which will show the results of the search operation.
     */
    public void setSearchResultsView( String resultsView ) {
        this.searchResultsView = resultsView;
    }

    /**
     * Sets the page size for the pagination of the results. If not set, not pagination will be used.
     */
    public Integer getPageSize() {
        return pageSize;
    }

    /**
     * Returns the page size for the pagination of the results. If not set, not pagination will be used.
     * 
     * @param pageSize
     */
    public void setPageSize( Integer pageSize ) {
        this.pageSize = pageSize;
    }

}