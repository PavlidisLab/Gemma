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
package ubic.gemma.javaspaces.gigaspaces;

import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springmodules.javaspaces.gigaspaces.GigaSpacesTemplate;

import ubic.gemma.loader.expression.geo.GeoDomainObjectGenerator;
import ubic.gemma.loader.expression.geo.service.GeoDatasetService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.util.progress.TaskRunningService;

/**
 * @author keshav
 * @version $Id$
 */
public class ExpressionExperimentTaskImpl extends BaseJavaSpacesTask implements ExpressionExperimentTask,
        InitializingBean {
    private Log log = LogFactory.getLog( this.getClass().getName() );

    private long counter = 0;
    private ExpressionExperimentService expressionExperimentService = null;
    private GeoDatasetService geoDatasetService = null;
    private String taskId = null;

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.javaspaces.JavaSpacesTask#execute(java.lang.Object)
     */
    public GigaSpacesResult execute( Object obj ) {
        // TODO Unsed, but will need to load expression experiments directly (as opposed to using fetchAndLoad).
        if ( !( obj instanceof ExpressionExperiment ) ) {
            throw new RuntimeException( "Task of type " + this.getClass().getSimpleName()
                    + " cannot execute on objects of type " + obj.getClass().getSimpleName() );
        }

        ExpressionExperiment expressionExperiment = ( ExpressionExperiment ) obj;
        ExpressionExperiment persistedExpressionExperiment = expressionExperimentService.create( expressionExperiment );
        Long id = persistedExpressionExperiment.getId();
        counter++;
        GigaSpacesResult result = new GigaSpacesResult();
        result.setTaskID( counter );
        result.setAnswer( id );

        return result;

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.javaspaces.gigaspaces.ExpressionExperimentTask#execute(java.lang.String, boolean, boolean)
     */
    @SuppressWarnings("unchecked")
    public GigaSpacesResult execute( String geoAccession, boolean loadPlatformOnly, boolean doSampleMatching ) {

        super.initProgressAppender( this.getClass() );

        Collection<ExpressionExperiment> datasets = geoDatasetService.fetchAndLoad( geoAccession, loadPlatformOnly,
                doSampleMatching );

        counter++;
        GigaSpacesResult result = new GigaSpacesResult();
        result.setAnswer( datasets );
        result.setTaskID( counter );

        log.info( "Task execution complete ... returning result " + result.getAnswer() + " with id "
                + result.getTaskID() );
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.javaspaces.gigaspaces.ExpressionExperimentTask#execute(java.lang.String, java.lang.String,
     *      boolean, boolean)
     */
    public GigaSpacesResult execute( String taskId, String geoAccession, boolean loadPlatformOnly,
            boolean doSampleMatching ) {
        // TODO - remove this method when you figure out how to get the taskId from the ExpressionExperimentImpl in the
        // CustomDelegatorWorker
        return execute( geoAccession, loadPlatformOnly, doSampleMatching );
    }

    /**
     * @param expressionExperimentService
     */
    public void setExpressionExperimentService( ExpressionExperimentService expressionExperimentService ) {
        this.expressionExperimentService = expressionExperimentService;
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