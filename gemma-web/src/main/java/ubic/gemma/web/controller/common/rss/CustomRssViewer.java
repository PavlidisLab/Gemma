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
package ubic.gemma.web.controller.common.rss;

import com.sun.syndication.feed.rss.Channel;
import com.sun.syndication.feed.rss.Content;
import com.sun.syndication.feed.rss.Item;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.view.feed.AbstractRssFeedView;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.core.config.Settings;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

/**
 * @author sshao
 */
@Component
public class CustomRssViewer extends AbstractRssFeedView {

    @Autowired
    ExpressionExperimentService expressionExperimentService;

    @Override
    protected List<Item> buildFeedItems( Map<String, Object> model, HttpServletRequest request,
            HttpServletResponse response ) {

        @SuppressWarnings("unchecked") Map<ExpressionExperiment, String> experiments = ( Map<ExpressionExperiment, String> ) model
                .get( "feedContent" );
        List<Item> items = new ArrayList<>( experiments.size() );

        // Set content of each expression experiment
        for ( Map.Entry<ExpressionExperiment, String> entry : experiments.entrySet() ) {
            ExpressionExperiment e = entry.getKey();

            String title = e.getShortName() + " (" + entry.getValue() + "): " + e.getName();
            String link = Settings.getHostUrl() + getServletContext().getContextPath() + "/expressionExperiment/showExpressionExperiment.html?id=" + e.getId()
                    .toString();

            int maxLength = 500;
            if ( e.getDescription().length() < 500 ) {
                maxLength = e.getDescription().length();
            }

            Item item = new Item();
            Content content = new Content();
            content.setValue( e.getDescription().substring( 0, maxLength ) + " ..." );
            item.setContent( content );
            item.setTitle( title );
            item.setLink( link );
            item.setPubDate( e.getCurationDetails().getLastUpdated() );
            items.add( item );
        }
        return items;
    }

    @Override
    protected void buildFeedMetadata( Map<String, Object> model, Channel feed, HttpServletRequest request ) {

        Calendar c = Calendar.getInstance();
        Date date = c.getTime();
        date = DateUtils.addWeeks( date, -1 );

        int updateCount = ( Integer ) model.get( "updateCount" );
        int newCount = ( Integer ) model.get( "newCount" );
        feed.setTitle( "RSS | Gemma" );
        feed.setDescription( updateCount + " updated experiments and " + newCount + " new experiments since " + date );
        feed.setLink( Settings.getHostUrl() + getServletContext().getContextPath() + "/" );

        super.buildFeedMetadata( model, feed, request );
    }

}
