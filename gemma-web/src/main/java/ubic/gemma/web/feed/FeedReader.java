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

import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.fetcher.FeedFetcher;
import com.sun.syndication.fetcher.impl.FeedFetcherCache;
import com.sun.syndication.fetcher.impl.HashMapFeedInfoCache;
import com.sun.syndication.fetcher.impl.HttpURLFeedFetcher;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Controller;
import ubic.gemma.core.config.Settings;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author paul
 *
 */
@Controller
public class FeedReader implements InitializingBean {

    private static final String GEMMA_HOME_FEEDURL_CONFIG_PARAM = "gemma.home.feedurl";

    private static final String DEFAULT_FEED = "http://wiki.pavlab.msl.ubc.ca/spaces/createrssfeed.action?types=blogpost&spaces=gemma&maxResults=1&title=Gemma+News&publicFeed=true&timeSpan=600";

    FeedFetcher feedFetcher;
    String feedUrl;

    @Override
    public void afterPropertiesSet() {
        FeedFetcherCache feedInfoCache = HashMapFeedInfoCache.getInstance();
        feedFetcher = new HttpURLFeedFetcher( feedInfoCache );
        this.feedUrl = Settings.getString( GEMMA_HOME_FEEDURL_CONFIG_PARAM );
        if ( StringUtils.isBlank( feedUrl ) ) {
            this.feedUrl = DEFAULT_FEED;
        }
    }

    /**
     * @return List of news items in HTML format.
     */
    public List<NewsItem> getLatestNews() {

        /*
         * reformat the feed.
         */
        Pattern authorP = Pattern.compile( "<p>.*?News Item.*?<b>(edited|added)</b>.*?by.*?<a.*?</p>", Pattern.DOTALL
                | Pattern.CASE_INSENSITIVE );
        Pattern footerP = Pattern.compile( "<div style=.?padding: 10px 0;.*?View Online</a>.*?</div>", Pattern.DOTALL
                | Pattern.CASE_INSENSITIVE );
        Pattern borderP = Pattern.compile( "border-top: 1px solid #ddd; border-bottom: 1px solid #ddd;" );
        List<NewsItem> result = new ArrayList<NewsItem>();
        try {
            SyndFeed feed = feedFetcher.retrieveFeed( new URL( feedUrl ) );

            for ( SyndEntry k : ( Collection<SyndEntry> ) feed.getEntries() ) {
                NewsItem n = new NewsItem();

                /*
                 * This code is specific for confluence feeds.
                 */
                String title = k.getTitle();

                String body = k.getDescription().getValue();
                Matcher m = authorP.matcher( body );
                body = m.replaceAll( "" );

                Matcher b = borderP.matcher( body );
                body = b.replaceAll( "" );

                /*
                 * Confluence-specific
                 */
                Matcher footerMatch = footerP.matcher( body );
                body = footerMatch.replaceAll( "" );

                n.setBody( body );
                n.setTitle( title );

                n.setDate( k.getPublishedDate() );

                result.add( n );
            }
        } catch ( Exception e ) {
            NewsItem n = new NewsItem();
            n.setTitle( "No news" );
            n.setBody( "" );
        }
        return result;

    }
}
