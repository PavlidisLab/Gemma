/*
 * Copyright 2004-2006 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ubic.gemma.web.controller.compass;

import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.compass.gps.spi.CompassGpsInterfaceDevice;
import org.compass.spring.web.mvc.AbstractCompassGpsCommandController;
import org.compass.spring.web.mvc.CompassIndexCommand;
import org.compass.spring.web.mvc.CompassIndexResults;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;

import ubic.gemma.util.CompassUtils;

/**
 * A general Spring's MVC Controller that perform the index operation of <code>CompassGps</code>. The indexing here
 * rebuilds the database. That is, the index is deleted, then rebuilt (at which time the indicies exist but are empty),
 * and then updated.
 * <p>
 * Will perform the index operation if the {@link org.compass.spring.web.mvc.CompassIndexCommand} <code>doIndex</code>
 * property is set to true.
 * <p>
 * The controller has two views to be set, the <code>indexView</code>, which is the view that holds the screen which
 * the user will initiate the index operation, and the <code>indexResultsView</code>, which will show the results of
 * the index operation.
 * <p>
 * The results of the index operation will be saved under the <code>indexResultsName</code>, which defaults to
 * "indexResults".
 * 
 * @uthor keshav
 * @author kimchy
 */
public class CustomCompassIndexController extends AbstractCompassGpsCommandController {

    private String indexView;

    private String indexResultsView;

    private String indexResultsName = "indexResults";

    public CustomCompassIndexController() {
        setCommandClass( CompassIndexCommand.class );
    }

    public void afterPropertiesSet() throws Exception {
        super.afterPropertiesSet();
        if ( indexView == null ) {
            throw new IllegalArgumentException( "Must set the indexView property" );
        }
        if ( indexResultsView == null ) {
            throw new IllegalArgumentException( "Must set hte indexResultsView property" );
        }
    }

    protected ModelAndView handle( HttpServletRequest request, HttpServletResponse response, Object command,
            BindException errors ) throws Exception {
        CompassIndexCommand indexCommand = ( CompassIndexCommand ) command;

        if ( !StringUtils.hasText( indexCommand.getDoIndex() ) || !indexCommand.getDoIndex().equalsIgnoreCase( "true" ) ) {
            return new ModelAndView( getIndexView(), getCommandName(), indexCommand );
        }

        long time = System.currentTimeMillis();

        CompassUtils.rebuildCompassIndex( ( CompassGpsInterfaceDevice ) this.getWebApplicationContext().getBean(
                "compassGps" ) );
        getCompassGps().index();

        time = System.currentTimeMillis() - time;
        CompassIndexResults indexResults = new CompassIndexResults( time );
        HashMap data = new HashMap();
        data.put( getCommandName(), indexCommand );
        data.put( getIndexResultsName(), indexResults );
        return new ModelAndView( getIndexResultsView(), data );
    }

    /**
     * Returns the view that holds the screen which the user will initiate the index operation.
     */
    public String getIndexView() {
        return indexView;
    }

    /**
     * Sets the view that holds the screen which the user will initiate the index operation.
     */
    public void setIndexView( String indexView ) {
        this.indexView = indexView;
    }

    /**
     * Returns the name of the results that the {@link CompassIndexResults} will be saved under. Defaults to
     * "indexResults".
     */
    public String getIndexResultsName() {
        return indexResultsName;
    }

    /**
     * Sets the name of the results that the {@link CompassIndexResults} will be saved under. Defaults to
     * "indexResults".
     * 
     * @param indexResultsName
     */
    public void setIndexResultsName( String indexResultsName ) {
        this.indexResultsName = indexResultsName;
    }

    /**
     * Returns the view which will show the results of the index operation.
     */
    public String getIndexResultsView() {
        return indexResultsView;
    }

    /**
     * Sets the view which will show the results of the index operation.
     * 
     * @param indexResultsView
     */
    public void setIndexResultsView( String indexResultsView ) {
        this.indexResultsView = indexResultsView;
    }
}
