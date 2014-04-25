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
package ubic.gemma.analysis.report;

import gemma.gsec.SecurityService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.social.twitter.api.StatusDetails;
import org.springframework.social.twitter.api.Tweet;
import org.springframework.social.twitter.api.Twitter;
import org.springframework.social.twitter.api.impl.TwitterTemplate;
import org.springframework.stereotype.Component;

import ubic.gemma.expression.experiment.service.ExpressionExperimentService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.util.Settings;

/**
 * @author sshao
 * @version $Id$
 */
@Component
public class TwitterOutboundImpl implements TwitterOutbound {
    private static AtomicBoolean enabled = new AtomicBoolean( Settings.getBoolean( "gemma.twitter.enabled", false ) );

    /**
     * 
     */
    private static final String EXPERIMENT_URL_BASE = "http://www.chibi.ubc.ca/Gemma/expressionExperiment/showExpressionExperiment.html?id=";

    private static Log log = LogFactory.getLog( TwitterOutboundImpl.class.getName() );

    /**
     * 
     */
    private static final String SHORTENER = "http://to.ly/api.php?json=0&longurl=";

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private SecurityService securityService;

    @Autowired
    private WhatsNewService whatsNewService;

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.web.feed.TwitterOutbound#disable()
     */
    @Override
    public void disable() {
        enabled.set( false );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.web.feed.TwitterOutbound#enable()
     */
    @Override
    public void enable() {
        enabled.set( true );

    }

    /**
     * Generate content for the tweet; exposed for testing.
     * 
     * @return
     */
    @Override
    public String generateDailyFeed() {

        Calendar c = Calendar.getInstance();
        Date date = c.getTime();
        date = DateUtils.addDays( date, -1 );
        WhatsNew whatsNew = whatsNewService.getReport( date );

        Collection<ExpressionExperiment> experiments = new ArrayList<ExpressionExperiment>();
        int updatedExperimentsCount = 0;
        int newExperimentsCount = 0;

        Random rand = new Random();

        // Query for all updated / new expression experiments to store into a experiments collection
        if ( whatsNew != null ) {
            Collection<ExpressionExperiment> updatedExperiments = whatsNew.getUpdatedExpressionExperiments();
            Collection<ExpressionExperiment> newExperiments = whatsNew.getNewExpressionExperiments();
            experiments.addAll( updatedExperiments );
            experiments.addAll( newExperiments );
            updatedExperimentsCount = updatedExperiments.size();
            newExperimentsCount = newExperiments.size();
        }

        ExpressionExperiment experiment = null;

        // Query latest experiments if there are no updated / new experiments
        if ( updatedExperimentsCount == 0 && newExperimentsCount == 0 ) {
            Collection<ExpressionExperiment> latestExperiments = expressionExperimentService.findByUpdatedLimit( 10 );
            Collection<ExpressionExperiment> publicExperiments = securityService.choosePublic( latestExperiments );

            if ( publicExperiments.isEmpty() ) {
                log.warn( "There are no valid experiments to tweet about" );
                return null;
            }

            experiment = ( ExpressionExperiment ) publicExperiments.toArray()[rand.nextInt( publicExperiments.size() )];

            // this shouldn't be needed.
            if ( experiment.getStatus().getLastUpdateDate().before( DateUtils.addDays( new Date(), -1 ) ) ) {
                log.info( "No updates in the last day, no tweeting" );
                return null;
            }

        } else {
            if ( experiments.isEmpty() ) {
                log.warn( "There are no valid experiments to tweet about" );
                return null;
            }

            experiment = ( ExpressionExperiment ) experiments.toArray()[rand.nextInt( experiments.size() )];
        }

        assert experiment != null;

        String status = statusWithExperiment(
                StringUtils.abbreviate( experiment.getShortName() + ": " + experiment.getName(), 60 ),
                formExperimentUrl( experiment ), updatedExperimentsCount, newExperimentsCount );

        return StringUtils.abbreviate( status, 140 ); // this will look a bit weird, and might chop off the url...but
                                                      // have to ensure.
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.web.feed.TwitterOutbound#sendDailyFeed()
     */
    @Override
    @Secured({ "GROUP_AGENT" })
    public void sendDailyFeed() {
        log.debug( "Checking if Twitter is enabled" );
        if ( !enabled.get() ) {
            return;
        }

        String feed = generateDailyFeed();
        log.info( "Twitter is enabled. Checking if Twitter feed is empty." );

        if ( StringUtils.isNotBlank( feed ) ) {
            log.info( "Sending out tweet: '" + feed + "'" );
            String consumerKey = Settings.getString( "twitter.consumer-key" );
            String consumerSecret = Settings.getString( "twitter.consumer-secret" );
            String accessToken = Settings.getString( "twitter.access-token" );
            String accessTokenSecret = Settings.getString( "twitter.access-token-secret" );

            Twitter twitter = new TwitterTemplate( consumerKey, consumerSecret, accessToken, accessTokenSecret );
            StatusDetails metadata = new StatusDetails();
            metadata.setWrapLinks( true );
            try {
                Tweet tweet = twitter.timelineOperations().updateStatus( feed, metadata );
                log.info( "tweet info:" + tweet.toString() );
            } catch ( Exception e ) {
                log.info( e.toString() );
            }
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.web.feed.TwitterOutbound#sendManualTweet(java.lang.String)
     */
    @Override
    @Secured({ "GROUP_ADMIN" })
    public void sendManualTweet( String feed ) {
        log.debug( "Checking if Twitter is enabled" );
        if ( !Settings.getBoolean( "gemma.twitter.enabled" ) ) {

            log.info( "Twitter is disabled." );
            return;
        }

        if ( StringUtils.isNotBlank( feed ) ) {
            log.info( "Sending out tweet: '" + feed + "'" );

            String consumerKey = Settings.getString( "twitter.consumer-key" );
            String consumerSecret = Settings.getString( "twitter.consumer-secret" );
            String accessToken = Settings.getString( "twitter.access-token" );
            String accessTokenSecret = Settings.getString( "twitter.access-token-secret" );

            Twitter twitter = new TwitterTemplate( consumerKey, consumerSecret, accessToken, accessTokenSecret );
            StatusDetails metadata = new StatusDetails();
            metadata.setWrapLinks( true );

            try {
                Tweet tweet = twitter.timelineOperations().updateStatus( feed, metadata );
                log.info( "tweet info:" + tweet.toString() );
            } catch ( Exception e ) {

                log.info( e.toString() );

                e.printStackTrace();

            }
        }

    }

    private String formExperimentUrl( ExpressionExperiment ee ) {
        return shortenUrl( EXPERIMENT_URL_BASE + ee.getId() );
    }

    private String shortenUrl( String url ) {

        URL toGet;
        try {
            toGet = new URL( SHORTENER + url );
        } catch ( MalformedURLException e1 ) {
            throw new IllegalArgumentException( "URL could not be shortened!" );
        }

        // stupid warning, it is managed!
        try (@SuppressWarnings("resource")
        BufferedReader in = new BufferedReader( new InputStreamReader( toGet.openStream() ) );) {
            StringBuilder buf = new StringBuilder();

            // should be just one line...
            String inputLine;

            while ( ( inputLine = in.readLine() ) != null ) {
                buf.append( inputLine );
            }
            in.close();

            return buf.toString();
        } catch ( IOException e ) {
            throw new RuntimeException( "URL could not be shortened!", e );
        }
    }

    /**
     * @param experimentName
     * @param url link to experiment
     * @param updatedExperimentsCount
     * @param newExperimentsCount
     * @return a status that provides the number of updated and new experiments, a "randomly" chosen experiment and a
     *         link back to Gemma
     */
    private String statusWithExperiment( String experimentName, String url, int updatedExperimentsCount,
            int newExperimentsCount ) {

        assert url != null && url.startsWith( "http" );

        if ( updatedExperimentsCount == 0 && newExperimentsCount == 0 ) {
            return "Experiment of the day: " + experimentName + " " + url + "; View more at gemma.chibi.ubc.ca";
        }

        if ( updatedExperimentsCount == 0 ) {
            return "Experiment of the day: " + experimentName + " " + url + "; View all " + newExperimentsCount
                    + " new experiments at gemma.chibi.ubc.ca";
        } else if ( newExperimentsCount == 0 ) {
            return "Experiment of the day: " + experimentName + " " + url + "; View all " + updatedExperimentsCount
                    + " updated experiments at gemma.chibi.ubc.ca";
        } else {
            return "Experiment of the day: " + experimentName + " " + url + "; View all " + updatedExperimentsCount
                    + " updated and " + newExperimentsCount + " new at gemma.chibi.ubc.ca";
        }
    }

}
