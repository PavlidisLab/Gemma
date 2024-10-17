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

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import ubic.gemma.core.analysis.preprocess.filter.FilteringException;
import ubic.gemma.core.analysis.service.ExpressionDataFileService;
import ubic.gemma.core.job.AbstractTask;
import ubic.gemma.core.job.TaskResult;
import ubic.gemma.core.job.TaskRunningService;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.common.quantitationtype.QuantitationTypeService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentMetaFileType;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.web.util.EntityNotFoundException;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

/**
 * For the download of data files from the browser. We can send the 'raw' data for any one quantitation type, with gene
 * annotations, OR the 'filtered masked' matrix for the expression experiment.
 *
 * @author pavlidis
 */
@SuppressWarnings("unused") // Called from JS
@Controller
public class ExpressionExperimentDataFetchController {

    @Autowired
    private TaskRunningService taskRunningService;
    @Autowired
    private ExpressionExperimentService expressionExperimentService;
    @Autowired
    private ExpressionDataFileService expressionDataFileService;
    @Autowired
    private QuantitationTypeService quantitationTypeService;

    @Value("${gemma.appdata.home}/dataFiles")
    private Path dataDir;

    /**
     * Regular spring MVC request to fetch a file that already has been generated. It is assumed that the file is in the
     * DATA_DIR.
     */
    @RequestMapping(value = "/getData.html", method = RequestMethod.GET)
    public void downloadFile( @RequestParam("file") String filename, HttpServletResponse response ) throws IOException {
        if ( StringUtils.isBlank( filename ) ) {
            throw new IllegalArgumentException( "The 'file' parameter must not be empty." );
        }
        // exclude any paths leading to the filename
        filename = FilenameUtils.getName( filename );
        this.download( dataDir.resolve( filename ), null, MediaType.APPLICATION_OCTET_STREAM_VALUE, response );
    }

    @RequestMapping(value = "/getMetaData.html", method = RequestMethod.GET)
    public void downloadMetaFile( @RequestParam("eeId") Long eeId, @RequestParam("typeId") Integer typeId, HttpServletResponse response ) throws IOException {
        ExpressionExperiment ee = expressionExperimentService.loadOrFail( eeId );
        ExpressionExperimentMetaFileType type = this.getType( typeId );
        if ( type == null ) {
            throw new IllegalArgumentException( "The experiment ID and file type ID parameters must be valid identifiers." );
        }
        Path file = expressionDataFileService.getMetadataFile( ee, type )
                .orElseThrow( () -> new EntityNotFoundException( ee.getShortName() + " does not have metadata of type " + type + "." ) );
        this.download( file, type.getDownloadName( ee ), type.getContentType(), response );
    }

    /**
     * Scans the metadata directory for any files associated with the given experiment.
     *
     * @param eeId the id of the experiment to scan for the metadata for.
     * @return an array of files available in the metadata directory for the given experiment.
     */
    public MetaFile[] getMetadataFiles( Long eeId ) throws IOException {
        ExpressionExperiment ee = expressionExperimentService.loadOrFail( eeId,
                EntityNotFoundException::new, "Experiment with given ID does not exist." );
        List<MetaFile> metaFiles = new ArrayList<>( ExpressionExperimentMetaFileType.values().length );
        for ( ExpressionExperimentMetaFileType type : ExpressionExperimentMetaFileType.values() ) {
            // Some files are prefixed with the experiments accession
            expressionDataFileService.getMetadataFile( ee, type )
                    .filter( Files::isReadable )
                    .map( f -> new MetaFile( type.getId(), type.getDisplayName() ) )
                    .ifPresent( metaFiles::add );
        }
        return metaFiles.toArray( new MetaFile[0] );
    }

    /**
     * AJAX Method - kicks off a job to start generating (if need be) the text based tab delimited co-expression data
     * file
     */
    public String getCoExpressionDataFile( Long eeId ) {
        ExpressionExperimentDataFetchCommand tc = new ExpressionExperimentDataFetchCommand();
        tc.setExpressionExperimentId( eeId );
        CoExpressionDataWriterJob job = new CoExpressionDataWriterJob( tc );
        return taskRunningService.submitTask( job );
    }

    /**
     * AJAX Method - kicks off a job to start generating (if need be) the text based tab delimited experiment design
     * data file
     */
    public String getDataFile( ExpressionExperimentDataFetchCommand command ) {
        DataWriterJob job = new DataWriterJob( command );
        return taskRunningService.submitTask( job );
    }

    /**
     * AJAX Method - kicks off a job to start generating (if need be) the text based tab delimited differential
     * expression data file
     */
    public String getDiffExpressionDataFile( Long analysisId ) {
        ExpressionExperimentDataFetchCommand tc = new ExpressionExperimentDataFetchCommand();
        tc.setAnalysisId( analysisId );
        DiffExpressionDataWriterTask job = new DiffExpressionDataWriterTask( tc );
        return taskRunningService.submitTask( job );
    }

    public File getOutputFile( String filename ) {
        // exclude any paths leading to the filename
        filename = FilenameUtils.getName( filename );
        File f = dataDir.resolve( filename ).toFile();
        try {
            FileUtils.forceMkdirParent( f );
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
        return f;
    }

    public void setQuantitationTypeService( QuantitationTypeService quantitationTypeService ) {
        this.quantitationTypeService = quantitationTypeService;
    }

    /**
     * @param f            the file to download from
     * @param downloadName this string will be used as a download name for the downloaded file. If null, the filesystem name
     *                     of the file will be used.
     * @param response     the http response to download to.
     * @throws IOException if the file in the given path can not be read.
     */
    private void download( Path f, String downloadName, String contentType, HttpServletResponse response ) throws IOException {
        if ( StringUtils.isBlank( downloadName ) ) {
            downloadName = f.getFileName().toString();
        }
        response.setContentType( contentType );
        response.setContentLength( ( int ) Files.size( f ) );
        response.addHeader( "Content-disposition", "attachment; filename=\"" + downloadName + "\"" );
        try ( InputStream in = Files.newInputStream( f ) ) {
            FileCopyUtils.copy( in, response.getOutputStream() );
            response.flushBuffer();
        }
    }

    private ExpressionExperimentMetaFileType getType( int id ) {
        for ( ExpressionExperimentMetaFileType t : ExpressionExperimentMetaFileType.values() ) {
            if ( t.getId() == id )
                return t;
        }
        return null;
    }

    class CoExpressionDataWriterJob extends AbstractTask<ExpressionExperimentDataFetchCommand> {

        protected Log log = LogFactory.getLog( this.getClass().getName() );

        CoExpressionDataWriterJob( ExpressionExperimentDataFetchCommand eeId ) {
            super( eeId );
        }

        @Override
        public TaskResult call() {
            StopWatch watch = new StopWatch();
            watch.start();

            assert this.taskCommand != null;
            Long eeId = this.taskCommand.getExpressionExperimentId();
            ExpressionExperiment ee = expressionExperimentService.load( eeId );

            if ( ee == null ) {
                throw new RuntimeException(
                        "No data available (either due to lack of authorization, or use of an invalid entity identifier)" );
            }

            Path f;
            try {
                f = expressionDataFileService.writeOrLocateCoexpressionDataFile( ee, false );
            } catch ( IOException e ) {
                throw new RuntimeException( e );
            }

            watch.stop();
            log.debug( "Finished getting co-expression file; done in " + watch.getTime() + " milliseconds" );

            ModelAndView mav = new ModelAndView( new RedirectView( "/getData.html?file=" + f.getFileName(), true ) );

            return new TaskResult( taskCommand, mav );
        }
    }

    /**
     * @author keshav
     */
    class DataWriterJob extends AbstractTask<ExpressionExperimentDataFetchCommand> {

        protected Log log = LogFactory.getLog( this.getClass().getName() );

        DataWriterJob( ExpressionExperimentDataFetchCommand command ) {
            super( command );
        }

        @Override
        public TaskResult call() {

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

            Path f;

            /* write out the file using text format */
            if ( usedFormat.equals( "text" ) ) {

                /* the design file */
                if ( eedId != null ) {
                    try {
                        ExpressionExperiment finalEe = ee;
                        f = expressionDataFileService.writeOrLocateDesignFile( ee, false )
                                .orElseThrow( () -> new IllegalStateException( finalEe + " does not have an experimental design" ) );
                    } catch ( IOException e ) {
                        throw new RuntimeException( e );
                    }
                }
                /* the data file */
                else {
                    if ( qType != null ) {
                        log.debug( "Using quantitation type to create matrix." );
                        try {
                            f = expressionDataFileService.writeOrLocateRawExpressionDataFile( ee, qType, false );
                        } catch ( IOException e ) {
                            throw new RuntimeException( e );
                        }
                    } else {

                        try {
                            ExpressionExperiment finalEe1 = ee;
                            f = expressionDataFileService.writeOrLocateProcessedDataFile( ee, false, filtered )
                                    .orElseThrow( () -> new IllegalStateException( finalEe1 + " does not have an experimental design" ) );
                        } catch ( FilteringException e ) {
                            throw new IllegalStateException( "The expression experiment data matrix could not be filtered for " + ee + ".", e );
                        } catch ( IOException e ) {
                            throw new RuntimeException( e );
                        }

                    }
                }

            }
            /* json format */
            else {
                if ( qType != null ) {
                    try {
                        f = expressionDataFileService.writeOrLocateJSONRawExpressionDataFile( ee, qType, false );
                    } catch ( IOException e ) {
                        throw new RuntimeException( e );
                    }
                } else {
                    try {
                        ExpressionExperiment finalEe2 = ee;
                        f = expressionDataFileService.writeOrLocateJSONProcessedExpressionDataFile( ee, false, filtered )
                                .orElseThrow( () -> new IllegalStateException( finalEe2 + " does not have processed vectors." ) );
                    } catch ( FilteringException e ) {
                        throw new IllegalStateException( "The expression experiment data matrix could not be filtered for " + ee + ".", e );
                    } catch ( IOException e ) {
                        throw new RuntimeException( e );
                    }
                }
            }

            if ( f == null )
                throw new IllegalStateException( "No file was obtained" );

            watch.stop();
            log.debug( "Finished writing and downloading a file; done in " + watch.getTime() + " milliseconds" );

            ModelAndView mav = new ModelAndView( new RedirectView( "/getData.html?file=" + f.getFileName(), true ) );

            return new TaskResult( taskCommand, mav );

        }

    }

    class DiffExpressionDataWriterTask extends AbstractTask<ExpressionExperimentDataFetchCommand> {

        protected Log log = LogFactory.getLog( this.getClass().getName() );

        DiffExpressionDataWriterTask( ExpressionExperimentDataFetchCommand command ) {
            super( command );
        }

        @Override
        public TaskResult call() {

            StopWatch watch = new StopWatch();
            watch.start();
            Collection<Path> files = new HashSet<>();

            assert this.taskCommand != null;

            if ( this.taskCommand.getAnalysisId() != null ) {

                Path f;
                try {
                    f = expressionDataFileService.getDiffExpressionAnalysisArchiveFile( taskCommand.getAnalysisId(),
                            taskCommand.isForceRewrite() );
                } catch ( IOException e ) {
                    throw new RuntimeException( e );
                }

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
                throw new EntityNotFoundException( "No data available (either due to no analyses being present, lack of authorization, or use of an invalid entity identifier)" );
                // } else if ( files.size() > 1 ) {
                // throw new UnsupportedOperationException(
                // "Sorry, you can't get multiple analyses at once using this method." );
            }

            ModelAndView mav = new ModelAndView(
                    new RedirectView( "/getData.html?file=" + files.iterator().next().getFileName(), true ) );
            return new TaskResult( taskCommand, mav );

        }

    }
}

