/*
 * The Gemma project
 *
 * Copyright (c) 2007 University of British Columbia
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
package ubic.gemma.web.controller.expression.experiment;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import ubic.gemma.core.analysis.service.ExpressionDataFileService;
import ubic.gemma.core.job.TaskResult;
import ubic.gemma.core.job.executor.webapp.TaskRunningService;
import ubic.gemma.core.tasks.AbstractTask;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.common.quantitationtype.QuantitationTypeService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.ParseException;
import java.util.*;

/**
 * For the download of data files from the browser. We can send the 'raw' data for any one quantitation type, with gene
 * annotations, OR the 'filtered masked' matrix for the expression experiment.
 *
 * @author pavlidis
 */
@SuppressWarnings("unused") // Called from JS
@Controller
public class ExpressionExperimentDataFetchController {

    private static final String[] META_FILES = { //
            "%s.alignment.metadata",                                    // Alignment metadata
            "%s.base.metadata",                                         // Base metadata
            "MultiQCReports" + File.separatorChar + "multiqc_report.html",  // Multi-QC report
            "configurations" + File.separatorChar                        // Configuration
    };

    @Autowired
    private TaskRunningService taskRunningService;
    @Autowired
    private ExpressionExperimentService expressionExperimentService;
    @Autowired
    private ExpressionDataFileService expressionDataFileService;
    @Autowired
    private QuantitationTypeService quantitationTypeService;

    /**
     * Regular spring MVC request to fetch a file that already has been generated. It is assumed that the file is in the
     * DATA_DIR.
     *
     * @param response response
     * @param request  request
     */
    @RequestMapping(value = "/getData.html", method = RequestMethod.GET, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void downloadFile( HttpServletRequest request, HttpServletResponse response ) throws IOException {
        String dir = ExpressionDataFileService.DATA_DIR;
        this.downloadFromDir( request, response, dir );
    }

    @RequestMapping(value = "/getMetaData.html", method = RequestMethod.GET, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void downloadMetaFile( HttpServletRequest request, HttpServletResponse response ) throws IOException {
        IllegalArgumentException e = new IllegalArgumentException( "The experiment ID parameter is invalid." );
        ExpressionExperiment ee;

        try {

            String eeId = request.getParameter( "id" );
            if ( StringUtils.isBlank( eeId ) ) {
                throw e;
            }

            ee = expressionExperimentService.load( Long.parseLong( eeId ) );
        } catch( NumberFormatException ne ){
            throw e;
        }

        String dir = ExpressionDataFileService.METADATA_DIR + ee.getShortName() + File.separatorChar;
        this.downloadFromDir( request, response, dir );
    }

    /**
     * Scans the metadata directory for any files associated with the given experiment.
     *
     * @param eeId the id of the experiment to scan for the metadata for.
     * @return an array of file names available in the metadata directory for the given experiment.
     */
    public String[] getMetadataFileUris( Long eeId ) {

        ExpressionExperiment ee = expressionExperimentService.load( eeId );
        if ( ee == null ) {
            throw new IllegalArgumentException( "Experiment with given ID does not exist." );
        }

        String[] uris = new String[ExpressionExperimentDataFetchController.META_FILES.length];

        int i = 0;
        for ( String path : ExpressionExperimentDataFetchController.META_FILES ) {

            // Some files are prefixed with the experiments accession
            String fPath = path.contains( "%" ) ? String.format( path, ee.getShortName() ) : path;
            File file = new File(
                    ExpressionDataFileService.METADATA_DIR + ee.getShortName() + File.separatorChar + fPath );

            if ( !file.exists() || !file.canRead() ) {
                continue;
            }

            String uri = null;

            if ( file.isDirectory() ) {
                // We expect this for the Configurations directory
                File[] files = file.listFiles();
                if ( files != null && files.length > 0 ) {
                    List<File> fList = Arrays.asList( files );

                    // Sort by last modified, we only want the newest file
                    Collections.sort( fList, new Comparator<File>() {
                        @Override
                        public int compare( File file, File t1 ) {
                            return Long.compare( file.lastModified(), t1.lastModified() );
                        }
                    } );

                    uri = fPath + fList.get( 0 ).getName();
                }
            } else {
                uri = fPath;
            }

            if ( uri != null ) {
                uris[i++] = uri;
            }
        }

        return uris;
    }

    /**
     * AJAX Method - kicks off a job to start generating (if need be) the text based tab delimited co-expression data
     * file
     */
    public String getCoExpressionDataFile( Long eeId ) {
        ExpressionExperimentDataFetchCommand tc = new ExpressionExperimentDataFetchCommand();
        tc.setExpressionExperimentId( eeId );
        CoExpressionDataWriterJob job = new CoExpressionDataWriterJob( tc );
        return taskRunningService.submitLocalTask( job );
    }

    /**
     * AJAX Method - kicks off a job to start generating (if need be) the text based tab delimited experiment design
     * data file
     */
    public String getDataFile( ExpressionExperimentDataFetchCommand command ) {
        DataWriterJob job = new DataWriterJob( command );
        return taskRunningService.submitLocalTask( job );
    }

    /**
     * AJAX Method - kicks off a job to start generating (if need be) the text based tab delimited differential
     * expression data file
     */
    public String getDiffExpressionDataFile( Long analysisId ) {
        ExpressionExperimentDataFetchCommand tc = new ExpressionExperimentDataFetchCommand();
        tc.setAnalysisId( analysisId );
        DiffExpressionDataWriterTask job = new DiffExpressionDataWriterTask( tc );
        return taskRunningService.submitLocalTask( job );
    }

    public File getOutputFile( String filename ) {
        String fullFilePath = ExpressionDataFileService.DATA_DIR + filename;
        File f = new File( fullFilePath );
        File parentDir = f.getParentFile();
        if ( !parentDir.exists() ) {
            //noinspection ResultOfMethodCallIgnored
            parentDir.mkdirs();
        }
        return f;
    }

    public void setQuantitationTypeService( QuantitationTypeService quantitationTypeService ) {
        this.quantitationTypeService = quantitationTypeService;
    }

    private void downloadFromDir( HttpServletRequest request, HttpServletResponse response, String dir )
            throws IOException {
        String file = request.getParameter( "file" );

        if ( StringUtils.isBlank( file ) ) {
            throw new IllegalArgumentException( "The file name cannot be blank" );
        }

        File f = new File( dir + file );

        if ( !f.canRead() ) {
            throw new IOException( "Cannot read from " + dir + file );
        }

        response.setContentLength( ( int ) f.length() );
        response.addHeader( "Content-disposition", "attachment; filename=\"" + f.getName() + "\"" );
        FileInputStream in = new FileInputStream( f );
        FileCopyUtils.copy( in, response.getOutputStream() );
        response.flushBuffer();

        try {
            in.close();
        } catch ( IOException ignored ) {

        }
    }

    class CoExpressionDataWriterJob extends AbstractTask<TaskResult, ExpressionExperimentDataFetchCommand> {

        protected Log log = LogFactory.getLog( this.getClass().getName() );

        CoExpressionDataWriterJob( ExpressionExperimentDataFetchCommand eeId ) {
            super( eeId );
        }

        @Override
        public TaskResult execute() {
            StopWatch watch = new StopWatch();
            watch.start();

            assert this.taskCommand != null;
            Long eeId = this.taskCommand.getExpressionExperimentId();
            ExpressionExperiment ee = expressionExperimentService.load( eeId );

            if ( ee == null ) {
                throw new RuntimeException(
                        "No data available (either due to lack of authorization, or use of an invalid entity identifier)" );
            }

            File f = expressionDataFileService.writeOrLocateCoexpressionDataFile( ee, false );

            watch.stop();
            log.debug( "Finished getting co-expression file; done in " + watch.getTime() + " milliseconds" );

            ModelAndView mav = new ModelAndView( new RedirectView( "/getData.html?file=" + f.getName(), true ) );

            return new TaskResult( taskCommand, mav );
        }
    }

    /**
     * @author keshav
     */
    class DataWriterJob extends AbstractTask<TaskResult, ExpressionExperimentDataFetchCommand> {

        protected Log log = LogFactory.getLog( this.getClass().getName() );

        DataWriterJob( ExpressionExperimentDataFetchCommand command ) {
            super( command );
        }

        @Override
        public TaskResult execute() {

            StopWatch watch = new StopWatch();
            watch.start();

            /* 'do yer thang' */
            assert this.taskCommand != null;

            Long qtId = taskCommand.getQuantitationTypeId();
            Long eeId = taskCommand.getExpressionExperimentId();
            Long eedId = taskCommand.getExperimentalDesignId();
            String format = taskCommand.getFormat();
            boolean filtered = taskCommand.isFilter();

            String usedFormat = "text";

            /* make sure a valid/recognizable format is used */
            if ( StringUtils.isNotBlank( format ) ) {
                if ( !( format.equals( "text" ) || format.equals( "json" ) ) ) {
                    throw new RuntimeException( "Format " + format + " is not recognized." );
                }
                usedFormat = format;
            }

            /* if qt is not set, send the data for the filtered matrix */
            QuantitationType qType = null;
            ExpressionExperiment ee;

            /* design file */
            if ( eedId != null ) {
                log.debug( "Request is for design file." );
                ee = expressionExperimentService.load( eeId );
                if ( ee == null ) {
                    throw new RuntimeException( "Expression experiment id " + eeId
                            + " was invalid: doesn't exist in system, or you lack authorization." );
                }
            }
            /* data file */
            else {
                if ( qtId != null ) {
                    qType = quantitationTypeService.load( qtId );
                    if ( qType == null ) {
                        throw new RuntimeException( "Quantitation type ID " + qtId
                                + " was invalid: doesn't exist in system, or you lack authorization." );
                    }

                    /* paranoia: in case QTs aren't secured properly, make sure we have access to the EE */
                    ee = expressionExperimentService.findByQuantitationType( qType );
                } else {
                    ee = expressionExperimentService.load( eeId );

                }
            }

            if ( ee == null ) {
                throw new RuntimeException(
                        "No data available (either due to lack of authorization, or use of an invalid entity identifier)" );
            }

            ee = expressionExperimentService.thawLite( ee );

            File f = null;

            /* write out the file using text format */
            if ( usedFormat.equals( "text" ) ) {

                /* the design file */
                if ( eedId != null ) {
                    f = expressionDataFileService.writeOrLocateDesignFile( ee, false );
                }
                /* the data file */
                else {
                    if ( qType != null ) {
                        log.debug( "Using quantitation type to create matrix." );
                        f = expressionDataFileService.writeOrLocateDataFile( qType, false );
                    } else {

                        f = expressionDataFileService.writeOrLocateDataFile( ee, false, filtered );

                    }
                }

            }
            /* json format */
            else if ( usedFormat.equals( "json" ) ) {

                if ( qType != null ) {
                    f = expressionDataFileService.writeOrLocateJSONDataFile( qType, false );
                } else {
                    f = expressionDataFileService.writeOrLocateJSONDataFile( ee, false, filtered );

                }
            }

            if ( f == null )
                throw new IllegalStateException( "No file was obtained" );

            watch.stop();
            log.debug( "Finished writing and downloading a file; done in " + watch.getTime() + " milliseconds" );

            ModelAndView mav = new ModelAndView( new RedirectView( "/getData.html?file=" + f.getName(), true ) );

            return new TaskResult( taskCommand, mav );

        }

    }

    class DiffExpressionDataWriterTask extends AbstractTask<TaskResult, ExpressionExperimentDataFetchCommand> {

        protected Log log = LogFactory.getLog( this.getClass().getName() );

        DiffExpressionDataWriterTask( ExpressionExperimentDataFetchCommand command ) {
            super( command );
        }

        @Override
        public TaskResult execute() {

            StopWatch watch = new StopWatch();
            watch.start();
            Collection<File> files = new HashSet<>();

            assert this.taskCommand != null;

            if ( this.taskCommand.getAnalysisId() != null ) {

                File f = expressionDataFileService.getDiffExpressionAnalysisArchiveFile( taskCommand.getAnalysisId(),
                        taskCommand.isForceRewrite() );

                files.add( f );

                // TODO: Support this case (Do we really use it from somewhere?)
                // } else if ( this.command.getExpressionExperimentId() != null ) {
                //
                // Long eeId = this.command.getExpressionExperimentId();
                // ExpressionExperiment ee = expressionExperimentService.load( eeId );
                //
                // if ( ee == null ) {
                // throw new RuntimeException(
                // "No data available (either due to lack of authorization, or use of an invalid entity identifier)" );
                // }
                //
                // files = expressionDataFileService.writeOrLocateDiffExpressionDataFiles( ee, command.isForceRewrite()
                // );

            } else {
                throw new IllegalArgumentException( "Must provide either experiment or specific analysis to provide" );
            }

            watch.stop();
            log.debug( "Finished writing and downloading differential expression file(s); done in " + watch.getTime()
                    + " milliseconds" );

            if ( files.isEmpty() ) {
                throw new IllegalArgumentException(
                        "No data available (either due to no analyses being present, lack of authorization, or use of an invalid entity identifier)" );
                // } else if ( files.size() > 1 ) {
                // throw new UnsupportedOperationException(
                // "Sorry, you can't get multiple analyses at once using this method." );
            }

            ModelAndView mav = new ModelAndView(
                    new RedirectView( "/getData.html?file=" + files.iterator().next().getName(), true ) );
            return new TaskResult( taskCommand, mav );

        }

    }

}
