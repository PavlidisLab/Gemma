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
package ubic.gemma.gemmaspaces.expression.experiment;

import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springmodules.javaspaces.gigaspaces.GigaSpacesTemplate;

import ubic.gemma.gemmaspaces.BaseGemmaSpacesTask;
import ubic.gemma.gemmaspaces.GemmaSpacesResult;
import ubic.gemma.loader.expression.geo.GeoDomainObjectGenerator;
import ubic.gemma.loader.expression.geo.service.GeoDatasetService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.util.progress.TaskRunningService;

/**
 * @author keshav
 * @version $Id$
 */
public class ExpressionExperimentLoadTaskImpl extends BaseGemmaSpacesTask implements ExpressionExperimentLoadTask,
        InitializingBean {
    private Log log = LogFactory.getLog( this.getClass().getName() );

    private long counter = 0;
    private GeoDatasetService geoDatasetService = null;
    private String taskId = null;

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.javaspaces.gigaspaces.ExpressionExperimentTask#execute(java.lang.String, boolean, boolean)
     */
    @SuppressWarnings("unchecked")
    public GemmaSpacesResult execute(
            GemmaSpacesExpressionExperimentLoadCommand javaSpacesExpressionExperimentLoadCommand ) {

        if ( !( javaSpacesExpressionExperimentLoadCommand instanceof GemmaSpacesExpressionExperimentLoadCommand ) ) {
            throw new RuntimeException( "Cannot handle objects of type "
                    + javaSpacesExpressionExperimentLoadCommand.getClass() );
        }

        super.initProgressAppender( this.getClass() );

        GemmaSpacesExpressionExperimentLoadCommand jsEeLoadCommand = ( GemmaSpacesExpressionExperimentLoadCommand ) javaSpacesExpressionExperimentLoadCommand;
        String geoAccession = jsEeLoadCommand.getAccession();
        boolean loadPlatformOnly = jsEeLoadCommand.isLoadPlatformOnly();
        boolean doSampleMatching = jsEeLoadCommand.isSuppressMatching();
        boolean aggressiveQtRemoval = jsEeLoadCommand.isAggressiveQtRemoval();

        Collection<ExpressionExperiment> datasets = geoDatasetService.fetchAndLoad( geoAccession, loadPlatformOnly,
                doSampleMatching, aggressiveQtRemoval );

        counter++;
        GemmaSpacesResult result = new GemmaSpacesResult();
        result.setAnswer( datasets );
        result.setTaskID( counter );

        log.info( "Task execution complete ... returning result " + result.getAnswer() + " with id "
                + result.getTaskID() );
        return result;
    }

    /**
     * @param geoDatasetService
     */
    public void setGeoDatasetService( GeoDatasetService geoDatasetService ) {
        this.geoDatasetService = geoDatasetService;
        this.geoDatasetService.setGeoDomainObjectGenerator( new GeoDomainObjectGenerator() );
    }

    /**
     * @param gigaSpacesTemplate
     */
    public void setGigaSpacesTemplate( GigaSpacesTemplate gigaSpacesTemplate ) {
        super.setGigaSpacesTemplate( gigaSpacesTemplate );
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
     */
    public void afterPropertiesSet() throws Exception {
        this.taskId = TaskRunningService.generateTaskId();
    }

    /**
     * Returns the taskId for this task.
     * 
     * @return
     */
    public String getTaskId() {
        return taskId;
    }
}