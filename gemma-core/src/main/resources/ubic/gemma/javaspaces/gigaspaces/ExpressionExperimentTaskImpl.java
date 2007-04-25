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
import org.springmodules.javaspaces.gigaspaces.GigaSpacesTemplate;

import ubic.gemma.loader.expression.geo.GeoDomainObjectGenerator;
import ubic.gemma.loader.expression.geo.service.GeoDatasetService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;

import com.j_spaces.core.LeaseProxy;

/**
 * @author keshav
 * @version $Id$
 */
public class ExpressionExperimentTaskImpl implements ExpressionExperimentTask {
    private Log log = LogFactory.getLog( this.getClass() );

    private long counter = 0;
    private ExpressionExperimentService expressionExperimentService = null;
    private GeoDatasetService geoDatasetService = null;
    private GigaSpacesTemplate gigaSpacesTemplate = null;

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

        // TODO - test - remove it when finished.
        Lease[] lease = new Lease[10];
        LoggingEntry entry = null;

        for ( int i = 0; i < 5; i++ ) {

            for ( int j = 0; j < 10000; j++ ) {
                // delay
            }

            if ( entry == null ) {
                log.info( "Could not find entry.  Writing a new entry." );
                entry = new LoggingEntry();
                // entry.message = String.valueOf( i );
                lease[i] = gigaSpacesTemplate.write( entry, Lease.FOREVER, 5000 );
                log.debug( "expiration " + lease[i].getExpiration() );

            } else {
                log.info( "Updating entry: " + entry );
                try {
                    entry = ( LoggingEntry ) gigaSpacesTemplate.read( entry, 1000 );
                    String uid = entry.__getEntryUID();
                    log.debug( "uid " + uid );
                    // entry.message = String.valueOf( i );

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

        log.info( "task execution complete ... returning result " + result.getAnswer() + " with id "
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
