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

import javax.servlet.http.HttpServletRequest;

import ubic.gemma.grid.javaspaces.index.IndexerTask;
import ubic.gemma.grid.javaspaces.index.IndexerTaskCommand;
import ubic.gemma.search.IndexService;
import ubic.gemma.util.grid.javaspaces.SpacesEnum;
import ubic.gemma.web.controller.BaseFormController;

/**
 * A general Spring's MVC Controller that perform the index operation of <code>CompassGps</code>. The indexing here
 * rebuilds the database. That is, the index is deleted, then rebuilt (at which time the indicies exist but are empty),
 * and then updated.
 * <p>
 * Will perform the index operation if the {@link org.compass.spring.web.mvc.CompassIndexCommand} <code>doIndex</code>
 * property is set to true.
 * <p>
 * The controller has two views to be set, the <code>indexView</code>, which is the view that holds the screen which the
 * user will initiate the index operation, and the <code>indexResultsView</code>, which will show the results of the
 * index operation.
 * <p>
 * The results of the index operation will be saved under the <code>indexResultsName</code>, which defaults to
 * "indexResults".
 * 
 * @author keshav, klc
 * @version $Id$
 * @spring.bean id="indexController"
 * @spring.property name="formView" value="indexer"
 * @spring.property name="successView" value="indexer"
 * @spring.property name="indexService" ref="indexService"
 */
public class CustomCompassIndexController extends BaseFormController {

    private IndexService indexService;

    /**
     * Main entry point for AJAX calls.
     * 
     * @param IndexGemmaCommand
     * @return
     */
    public String run( IndexerTaskCommand command ) {
        return indexService.run( command, SpacesEnum.DEFAULT_SPACE.getSpaceUrl(), IndexerTask.class.getName(), true );

    }

    /**
     * Entry point for quartz
     * 
     * @param IndexGemmaCommand
     * @return
     */
    public String indexAllInSpace() {

        IndexerTaskCommand command = new IndexerTaskCommand();
        command.setAll( true );

        return indexService.run( command, SpacesEnum.DEFAULT_SPACE.getSpaceUrl(), IndexerTask.class.getName(), true );
    }

    /**
     * This is needed or you will have to specify a commandClass in the DispatcherServlet's context
     * 
     * @param request
     * @return Object
     * @throws Exception
     */
    @Override
    protected Object formBackingObject( HttpServletRequest request ) throws Exception {
        return request;
    }

    public void setIndexService( IndexService indexService ) {
        this.indexService = indexService;
    }

}
