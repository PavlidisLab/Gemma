/*
 * The Gemma project
 * 
 * Copyright (c) 2008 University of British Columbia
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
package ubic.gemma.web.feed;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.InitializingBean;

import ubic.gemma.util.ConfigUtils;

import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.fetcher.FeedFetcher;
import com.sun.syndication.fetcher.impl.FeedFetcherCache;
import com.sun.syndication.fetcher.impl.HashMapFeedInfoCache;
import com.sun.syndication.fetcher.impl.HttpURLFeedFetcher;

/**
 * @spring.bean id="feedReader"
 * @author paul
 * @version $Id$
 */
public class FeedReader implements InitializingBean {

    private static final String GEMMA_HOME_FEEDURL_CONFIG_PARAM = "gemma.home.feedurl";

    private static final String DEFAULT_FEED = "http://bioinformatics.ubc.ca/confluence/createrssfeed.action?types=blogpost&sort=created&showContent=true&spaces=gemma&labelString=&rssType=atom&maxResults=1&timeSpan=600&publicFeed=true&title=Gemma+news";

    FeedFetcher feedFetcher;
    String feedUrl;

    /**
     * @return List of news items in HTML format.
     */
    @SuppressWarnings("unchecked")
    public List<NewsItem> getLatestNews() {

        /*
         * reformat the feed.
         */
        Pattern p = Pattern.compile( "<div.*?</div>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE );

        List<NewsItem> result = new ArrayList<NewsItem>();
        try {
            SyndFeed feed = feedFetcher.retrieveFeed( new URL( feedUrl ) );

            for ( SyndEntry k : ( Collection<SyndEntry> ) feed.getEntries() ) {
                NewsItem n = new NewsItem();

                /*
                 * This code is specific for confluence feeds.
                 */
                String title = k.getTitle();
                title = title.replaceAll( "\\s\\(created\\)$", "" );
                title = title.replaceAll( "\\s\\(updated\\)$", "" );

                /*
                 * remove the date etc. crap
                 */
                String body = k.getDescription().getValue();
                Matcher m = p.matcher( body );

                body = m.replaceAll( "" );

                n.setBody( body );
                n.setTitle( title );

                n.setDate( k.getPublishedDate() );

                result.add( n );
            }
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
        return result;

    }

    public void afterPropertiesSet() throws Exception {
        FeedFetcherCache feedInfoCache = HashMapFeedInfoCache.getInstance();
        feedFetcher = new HttpURLFeedFetcher( feedInfoCache );
        this.feedUrl = ConfigUtils.getString( GEMMA_HOME_FEEDURL_CONFIG_PARAM );
        if ( StringUtils.isBlank( feedUrl ) ) {
            this.feedUrl = DEFAULT_FEED;
        }
    }
}
