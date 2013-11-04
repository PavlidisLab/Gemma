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
package ubic.gemma.web.feed;

import gemma.gsec.SecurityService;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Random;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.social.twitter.api.StatusDetails;
import org.springframework.social.twitter.api.Twitter;
import org.springframework.social.twitter.api.impl.TwitterTemplate;
import org.springframework.stereotype.Component;

import ubic.gemma.analysis.report.WhatsNew;
import ubic.gemma.analysis.report.WhatsNewService;
import ubic.gemma.expression.experiment.service.ExpressionExperimentService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.util.Settings;

/**
 * @author sshao
 * @version $Id$
 */
@Component
public class TwitterOutbound {
    private static Log log = LogFactory.getLog( TwitterOutbound.class.getName() );

    @Autowired
    private WhatsNewService whatsNewService;

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private SecurityService securityService;

    /**
     * Send Tweet.
     */
    @Secured({ "GROUP_AGENT" })
    public void sendDailyFeed() {
        log.debug( "Checking if Twitter is enabled" );
        if ( !Settings.getBoolean( "gemma.twitter.enabled" ) ) {
            return;
        }

        String feed = generateDailyFeed();
        log.info( "Twitter is enabled. Checking if Twitter feed is empty." );

        if ( StringUtils.isNotBlank( feed ) ) {
            log.info( "Sending out tweet: '" + feed + "'" );
            String consumerKey = Settings.getString( "gemma.twitter.consumer-key" );
            String consumerSecret = Settings.getString( "gemma.twitter.consumer-secret" );
            String accessToken = Settings.getString( "gemma.twitter.access-token" );
            String accessTokenSecret = Settings.getString( "gemma.twitter.access-token-secret" );

            Twitter twitter = new TwitterTemplate( consumerKey, consumerSecret, accessToken, accessTokenSecret );
            StatusDetails metadata = new StatusDetails();
            metadata.setWrapLinks( true );
            twitter.timelineOperations().updateStatus( feed, metadata );
        }

    }

    /**
     * Generate content for the tweet
     * 
     * @return
     */
    String generateDailyFeed() {

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
        } else {
            if ( experiments.isEmpty() ) {
                log.warn( "There are no valid experiments to tweet about" );
                return null;
            }

            experiment = ( ExpressionExperiment ) experiments.toArray()[rand.nextInt( experiments.size() )];
        }

        assert experiment != null;

        String status = statusWithExperiment(
                StringUtils.abbreviate( experiment.getShortName() + ": " + experiment.getName(), 90 ),
                updatedExperimentsCount, newExperimentsCount );

        return StringUtils.abbreviate( status, 140 ); // this will look a bit weird, and might chop off the url...but
                                                      // have to ensure.
    }

    /**
     * @param experimentName
     * @param updatedExperimentsCount
     * @param newExperimentsCount
     * @return a status that provides the number of updated and new experiments, a "randomly" chosen experiment and a
     *         link back to Gemma
     */
    private String statusWithExperiment( String experimentName, int updatedExperimentsCount, int newExperimentsCount ) {
        if ( updatedExperimentsCount == 0 && newExperimentsCount == 0 ) {
            return "Experiment of the day: " + experimentName + "; View all latest at www.chibi.ubc.ca/Gemma/rssfeed";
        }

        if ( updatedExperimentsCount == 0 ) {
            return "Experiment of the day: " + experimentName + "; View all " + newExperimentsCount
                    + " new experiments at www.chibi.ubc.ca/Gemma/rssfeed";
        } else if ( newExperimentsCount == 0 ) {
            return "Experiment of the day: " + experimentName + "; View all " + updatedExperimentsCount
                    + " updated experiments at www.chibi.ubc.ca/Gemma/rssfeed";
        } else {
            return "Experiment of the day: " + experimentName + "; View all " + updatedExperimentsCount
                    + " updated and " + newExperimentsCount + " new at www.chibi.ubc.ca/Gemma/rssfeed";
        }
    }
}
