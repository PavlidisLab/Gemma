/*
 * The Gemma project
 * 
 * Copyright (c) 2011 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.web.services.rest;

import java.io.File;
import java.io.IOException;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ubic.gemma.analysis.service.ExpressionDataFileService;
import ubic.gemma.expression.experiment.service.ExpressionExperimentService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * REST webservice to access experiment related data using an experiments eeId The code writes to and reads from files
 * because it is leveraging existing code (technically no file creation is necessary for these tasks)
 */
@Service
@Path("/experimentData")
public class ExperimentDataWebService {

    protected Log log = LogFactory.getLog( this.getClass().getName() );

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private ExpressionDataFileService expressionDataFileService;

    @GET
    @Path("/findExpressionDataByEeId/{eeId},{filtered}")
    @Produces(MediaType.TEXT_PLAIN)
    public String getExpressionDataByEeId( @PathParam("eeId") Long eeId, @PathParam("filtered") boolean filtered ) {

        ExpressionExperiment ee = null;

        ee = expressionExperimentService.load( eeId );

        if ( ee == null ) {
            return "No data available (either due to lack of authorization, or use of an invalid experiment identifier)";
        }

        return getOutputForExpressionData( ee, filtered );

    }

    @GET
    @Path("/findExpressionDataByEeName/{eeName},{filtered}")
    @Produces(MediaType.TEXT_PLAIN)
    public String getExpressionDataByEeName( @PathParam("eeName") String eeName, @PathParam("filtered") boolean filtered ) {

        ExpressionExperiment ee = null;

        ee = expressionExperimentService.findByShortName( eeName );

        if ( ee == null ) {
            return "No data available (either due to lack of authorization, or use of an invalid experiment identifier)";
        }

        return getOutputForExpressionData( ee, filtered );

    }

    private String getOutputForExpressionData( ExpressionExperiment ee, boolean filtered ) {

        StopWatch watch = new StopWatch();
        watch.start();

        ee = expressionExperimentService.thawLite( ee );

        File f = null;

        /* write out the file using text format */

        f = expressionDataFileService.writeTemporaryDataFile( ee, filtered );

        if ( f == null ) throw new IllegalStateException( "No file was obtained" );

        watch.stop();
        log.info( "Finished writing a file; done in " + watch.getTime() + " milliseconds" );

        String output = "";

        try {
            output = FileUtils.readFileToString( f, "UTF-8" );
        } catch ( IOException e ) {
            return "There has been an error";

        }

        // delete file
        if ( f.canWrite() && f.delete() ) {
            log.info( "Deleted: " + f );
        }

        return output;

    }

    @GET
    @Path("/findExperimentDesignByEeId/{eeId}")
    @Produces(MediaType.TEXT_PLAIN)
    public String getExperimentDataByEeId( @PathParam("eeId") Long eeId ) {

        StopWatch watch = new StopWatch();
        watch.start();

        ExpressionExperiment ee = null;

        log.info( "Request is for design file for eeId:" + eeId );
        ee = expressionExperimentService.load( eeId );
        if ( ee == null ) {
            return "Expression experiment id " + eeId
                    + " was invalid: doesn't exist in system, or you lack authorization.";
        }

        ee = expressionExperimentService.thawLite( ee );

        File f = expressionDataFileService.writeTemporaryDesignFile( ee );

        if ( f == null || !f.exists() ) throw new IllegalStateException( "No file was obtained" );

        watch.stop();
        log.info( "Finished writing a file; done in " + watch.getTime() + " milliseconds" );

        String output = "";

        try {
            output = FileUtils.readFileToString( f, "UTF-8" );
        } catch ( IOException e ) {
            return "There has been an error";

        }

        // delete file
        if ( f.canWrite() && f.delete() ) {
            log.info( "Deleted: " + f );
        }

        return output;

    }

}
