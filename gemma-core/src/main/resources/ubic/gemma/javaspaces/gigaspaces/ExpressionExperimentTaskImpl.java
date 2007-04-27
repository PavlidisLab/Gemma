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

import net.jini.core.lease.Lease;

import org.acegisecurity.context.SecurityContextHolder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.springmodules.javaspaces.gigaspaces.GigaSpacesTemplate;

import ubic.gemma.loader.expression.geo.GeoDomainObjectGenerator;
import ubic.gemma.loader.expression.geo.service.GeoDatasetService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.util.progress.GigaspacesProgressJobImpl;
import ubic.gemma.util.progress.ProgressManager;

/**
 * @author keshav
 * @version $Id$
 */
public class ExpressionExperimentTaskImpl implements ExpressionExperimentTask {
    private Log log = LogFactory.getLog( this.getClass().getName() );

    private long counter = 0;
    private ExpressionExperimentService expressionExperimentService = null;
    private GeoDatasetService geoDatasetService = null;
    private GigaSpacesTemplate gigaSpacesTemplate = null;

    // progress framework stuff
    Logger log4jLogger;
    Level oldLevel;

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.javaspaces.gigaspaces.ExpressionExperimentTask#execute(ubic.gemma.model.expression.experiment.ExpressionExperiment)
     */
    public Result execute( ExpressionExperiment expressionExperiment ) {

        ExpressionExperiment persistedExpressionExperiment = expressionExperimentService.create( expressionExperiment );
        Long id = persistedExpressionExperiment.getId();
        counter++;
        Result result = new Result();
        result.setTaskID( counter );
        result.setAnswer( id );

        return result;

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.javaspaces.gigaspaces.ExpressionExperimentTask#execute(java.lang.String, boolean, boolean)
     */
    public Result execute( String geoAccession, boolean loadPlatformOnly, boolean doSampleMatching ) {

        log.info( "executing task " + this.getClass().getName() );

        log.debug( "Current Thread: " + Thread.currentThread().getName() + " Authentication: "
                + SecurityContextHolder.getContext().getAuthentication() );

        // TODO - this is a test - will move into fetchAndLoad or into interceptor
        // when finished.
        Lease[] lease = new Lease[10];
        // LoggingEntry entry = null;
        GigaspacesProgressJobImpl entry = null;
        for ( int i = 0; i < 5; i++ ) {

            if ( entry == null ) {
                log.info( "Could not find entry.  Writing a new entry." );
                entry = ( GigaspacesProgressJobImpl ) ProgressManager.createGigaspacesProgressJob( null,
                        SecurityContextHolder.getContext().getAuthentication().getName(), "Logging Server Task" );

                lease[i] = gigaSpacesTemplate.write( entry, Lease.FOREVER, 5000 );
            } else {
                log.info( "Updating entry: " + entry );
                try {
                    entry = ( GigaspacesProgressJobImpl ) gigaSpacesTemplate.read( entry, 1000 );
                    ProgressManager.updateCurrentThreadsProgressJob( entry.getProgressData() );
                    // entry.setMessage( String.valueOf( i ) + "% complete" );
                    log.info( String.valueOf( i ) + "% complete" );
                    gigaSpacesTemplate.update( entry, Lease.FOREVER, 1000 );
                } catch ( Exception e ) {
                    e.printStackTrace();
                }
            }
        }
        // end test

        Collection datasets = geoDatasetService.fetchAndLoad( geoAccession, loadPlatformOnly, doSampleMatching );

        // TODO figure out what to store in the result for collections
        counter++;
        Result result = new Result();
        result.setAnswer( datasets.size() );
        result.setTaskID( counter );

        log.info( "Task execution complete ... returning result " + result.getAnswer() + " with id "
                + result.getTaskID() );
        return result;
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
        this.gigaSpacesTemplate = gigaSpacesTemplate;
    }
}
