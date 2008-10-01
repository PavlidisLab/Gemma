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
package ubic.gemma.grid.javaspaces.expression.experiment;

import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springmodules.javaspaces.gigaspaces.GigaSpacesTemplate;

import ubic.gemma.grid.javaspaces.BaseSpacesTask;
import ubic.gemma.grid.javaspaces.SpacesResult;
import ubic.gemma.loader.expression.arrayExpress.ArrayExpressLoadService;
import ubic.gemma.loader.expression.geo.GeoDomainObjectGenerator;
import ubic.gemma.loader.expression.geo.service.GeoDatasetService;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.util.progress.TaskRunningService;

/**
 * @author keshav
 * @version $Id$
 */
public class ExpressionExperimentLoadTaskImpl extends BaseSpacesTask implements ExpressionExperimentLoadTask {
    private Log log = LogFactory.getLog( this.getClass().getName() );

    private long counter = 0;
    private GeoDatasetService geoDatasetService = null;
    ArrayExpressLoadService arrayExpressLoadService;

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.grid.javaspaces.SpacesTask#execute(java.lang.Object)
     */
    @SuppressWarnings("unchecked")
    public SpacesResult execute( SpacesExpressionExperimentLoadCommand jsEeLoadCommand ) {

        super.initProgressAppender( this.getClass() );

        String accession = jsEeLoadCommand.getAccession();
        boolean loadPlatformOnly = jsEeLoadCommand.isLoadPlatformOnly();
        boolean doSampleMatching = jsEeLoadCommand.isSuppressMatching();
        boolean aggressiveQtRemoval = jsEeLoadCommand.isAggressiveQtRemoval();
        boolean splitIncompatiblePlatforms = jsEeLoadCommand.isSplitIncompatiblePlatforms();
        boolean allowSuperSeriesLoad = jsEeLoadCommand.isAllowSuperSeriesLoad();

        SpacesResult result = new SpacesResult();
        if ( jsEeLoadCommand.isArrayExpress() ) {
            ExpressionExperiment dataset = arrayExpressLoadService.load( accession, jsEeLoadCommand
                    .getArrayDesignName() );
            result.setAnswer( dataset );
        } else if ( loadPlatformOnly ) {
            Collection<ArrayDesign> arrayDesigns = geoDatasetService.fetchAndLoad( accession, true, doSampleMatching,
                    aggressiveQtRemoval, splitIncompatiblePlatforms, allowSuperSeriesLoad );
            result.setAnswer( arrayDesigns );
        } else {
            Collection<ExpressionExperiment> datasets = geoDatasetService.fetchAndLoad( accession, loadPlatformOnly,
                    doSampleMatching, aggressiveQtRemoval, splitIncompatiblePlatforms, allowSuperSeriesLoad );
            result.setAnswer( datasets );
        }
        counter++;
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
     * @param arrayExpressLoadService the arrayExpressLoadService to set
     */
    public void setArrayExpressLoadService( ArrayExpressLoadService arrayExpressLoadService ) {
        this.arrayExpressLoadService = arrayExpressLoadService;
    }

    /**
     * @param gigaSpacesTemplate
     */
    @Override
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
}