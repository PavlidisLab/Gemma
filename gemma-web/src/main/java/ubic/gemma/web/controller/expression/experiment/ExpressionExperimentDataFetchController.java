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

import lombok.extern.apachecommons.CommonsLog;
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
import ubic.gemma.core.analysis.preprocess.filter.FilteringException;
import ubic.gemma.core.analysis.service.ExpressionDataFileService;
import ubic.gemma.core.job.AbstractTask;
import ubic.gemma.core.job.TaskResult;
import ubic.gemma.core.job.TaskRunningService;
import ubic.gemma.core.util.locking.LockedPath;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.common.quantitationtype.QuantitationTypeService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentMetaFileType;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.web.controller.util.EntityNotFoundException;

import javax.annotation.Nullable;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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
@CommonsLog
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
    @Autowired
    private ServletContext servletContext;

    @Value("${gemma.appdata.home}/dataFiles")
    private Path dataDir;

    @Value("${tomcat.sendfile.enabled}")
    private boolean enableTomcatSendfile;

    /**
     * Regular spring MVC request to fetch a file that already has been generated. It is assumed that the file is in the
     * DATA_DIR.
     */
    @RequestMapping(value = "/getData.html", method = { RequestMethod.GET, RequestMethod.HEAD })
    public void downloadFile( @RequestParam("file") String filename, HttpServletRequest request, HttpServletResponse response ) throws IOException {
        if ( StringUtils.isBlank( filename ) ) {
            throw new IllegalArgumentException( "The 'file' parameter must not be empty." );
        }
        // prevent lock files from being downloaded
        if ( filename.endsWith( ".lock" ) ) {
            throw new EntityNotFoundException( "There is not data file named " + filename + " available for download." );
        }
        // exclude any paths leading to the filename
        filename = FilenameUtils.getName( filename );
        try ( LockedPath file = expressionDataFileService.getDataFile( filename, false ) ) {
            if ( !Files.exists( file.getPath() ) ) {
                throw new EntityNotFoundException( "There is not data file named " + filename + " available for download." );
            }
            this.download( file.getPath(), null, MediaType.APPLICATION_OCTET_STREAM_VALUE, request, response, true );
        }
    }

    @RequestMapping(value = "/getMetaData.html", method = { RequestMethod.GET, RequestMethod.HEAD })
    public void downloadMetaFile( @RequestParam("eeId") Long eeId, @RequestParam("typeId") Integer typeId, HttpServletRequest request, HttpServletResponse response ) throws IOException {
        ExpressionExperiment ee = expressionExperimentService.loadOrFail( eeId );
        ExpressionExperimentMetaFileType type = this.getType( typeId );
        if ( type == null ) {
            throw new IllegalArgumentException( "The experiment ID and file type ID parameters must be valid identifiers." );
        }
        String missingMessage = ee.getShortName() + " does not have metadata of type " + type + ".";
        try ( LockedPath file = expressionDataFileService.getMetadataFile( ee, type, true )
                // only happens for metadata files organized as directories
                .orElseThrow( () -> new EntityNotFoundException( missingMessage ) ) ) {
            if ( !Files.exists( file.getPath() ) ) {
                throw new EntityNotFoundException( missingMessage );
            }
            this.download( file.getPath(), type.getDownloadName( ee ), type.getContentType(), request, response,
                    type != ExpressionExperimentMetaFileType.MULTIQC_REPORT );
        }
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
            if ( type == ExpressionExperimentMetaFileType.MULTIQC_DATA || type == ExpressionExperimentMetaFileType.MULTIQC_LOG ) {
                // don't display these in the GUI
                continue;
            }
            // Some files are prefixed with the experiments accession
            expressionDataFileService.getMetadataFile( ee, type, false )
                    .map( LockedPath::closeAndGetPath )
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

    /**
     * @param f            the file to download from
     * @param downloadName this string will be used as a download name for the downloaded file. If null, the filesystem name
     *                     of the file will be used.
     * @param response     the http response to download to.
     * @throws IOException if the file in the given path can not be read.
     */
    private void download( Path f, @Nullable String downloadName, String contentType, HttpServletRequest request, HttpServletResponse response, boolean downloadAsAttachment ) throws IOException {
        if ( StringUtils.isBlank( downloadName ) ) {
            downloadName = f.getFileName().toString();
        }
        response.setContentType( contentType );
        response.setContentLengthLong( Files.size( f ) );
        if ( downloadAsAttachment ) {
            response.addHeader( "Content-Disposition", "attachment; filename=\"" + downloadName + "\"" );
        }
        if ( enableTomcatSendfile ) {
            if ( Boolean.TRUE.equals( request.getAttribute( "org.apache.tomcat.sendfile.support" ) ) ) {
                downloadViaSendfile( f, request, response );
                return;
            } else {
                log.warn( "Tomcat sendfile is not supported for this request. Falling back to stream download." );
            }
        }
        downloadViaStream( f, response );
    }

    /**
     * Uses Tomcat sendfile to download the file.
     */
    private void downloadViaSendfile( Path f, HttpServletRequest request, HttpServletResponse response ) throws IOException {
        request.setAttribute( "org.apache.tomcat.sendfile.filename", f.toString() );
        request.setAttribute( "org.apache.tomcat.sendfile.start", 0L );
        request.setAttribute( "org.apache.tomcat.sendfile.end", Files.size( f ) );
    }

    private void downloadViaStream( Path f, HttpServletResponse response ) throws IOException {
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

        protected final Log log = LogFactory.getLog( this.getClass().getName() );

        CoExpressionDataWriterJob( ExpressionExperimentDataFetchCommand eeId ) {
            super( eeId );
        }

        @Override
        public TaskResult call() {
            StopWatch watch = new StopWatch();
            watch.start();

            Long eeId = this.getTaskCommand().getExpressionExperimentId();
            ExpressionExperiment ee = expressionExperimentService.load( eeId );

            if ( ee == null ) {
                throw new RuntimeException(
                        "No data available (either due to lack of authorization, or use of an invalid entity identifier)" );
            }

            Path f;
            try ( LockedPath lockedPath = expressionDataFileService.writeOrLocateCoexpressionDataFile( ee, false ) ) {
                f = lockedPath.getPath();
            } catch ( IOException e ) {
                throw new RuntimeException( e );
            }

            watch.stop();
            log.debug( "Finished getting co-expression file; done in " + watch.getTime() + " milliseconds" );

            return newTaskResult( servletContext.getContextPath() + "/getData.html?file=" + f.getFileName() );
        }
    }

    /**
     * @author keshav
     */
    class DataWriterJob extends AbstractTask<ExpressionExperimentDataFetchCommand> {

        protected final Log log = LogFactory.getLog( this.getClass().getName() );

        DataWriterJob( ExpressionExperimentDataFetchCommand command ) {
            super( command );
        }

        @Override
        public TaskResult call() {

            StopWatch watch = new StopWatch();
            watch.start();

            Long qtId = getTaskCommand().getQuantitationTypeId();
            Long eeId = getTaskCommand().getExpressionExperimentId();
            Long eedId = getTaskCommand().getExperimentalDesignId();
            String format = getTaskCommand().getFormat();
            boolean filtered = getTaskCommand().isFilter();

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
                    ExpressionExperiment finalEe = ee;
                    try ( LockedPath lockedPath = expressionDataFileService.writeOrLocateDesignFile( ee, false )
                            .orElseThrow( () -> new IllegalStateException( finalEe + " does not have an experimental design" ) ) ) {
                        f = lockedPath.getPath();
                    } catch ( IOException e ) {
                        throw new RuntimeException( e );
                    }
                }
                /* the data file */
                else {
                    if ( qType != null ) {
                        log.debug( "Using quantitation type to create matrix." );
                        try ( LockedPath lockedPath = expressionDataFileService.writeOrLocateRawExpressionDataFile( ee, qType, false ) ) {
                            f = lockedPath.getPath();
                        } catch ( IOException e ) {
                            throw new RuntimeException( e );
                        }
                    } else {
                        ExpressionExperiment finalEe1 = ee;
                        try ( LockedPath lockedPath = expressionDataFileService.writeOrLocateProcessedDataFile( ee, filtered, false )
                                .orElseThrow( () -> new IllegalStateException( finalEe1 + " does not have an experimental design" ) ) ) {
                            f = lockedPath.getPath();
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
                    try ( LockedPath lockedPath = expressionDataFileService.writeOrLocateJSONRawExpressionDataFile( ee, qType, false ) ) {
                        f = lockedPath.getPath();
                    } catch ( IOException e ) {
                        throw new RuntimeException( e );
                    }
                } else {
                    ExpressionExperiment finalEe2 = ee;
                    try ( LockedPath lockedPath = expressionDataFileService.writeOrLocateJSONProcessedExpressionDataFile( ee, false, filtered )
                            .orElseThrow( () -> new IllegalStateException( finalEe2 + " does not have processed vectors." ) ) ) {
                        f = lockedPath.getPath();
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

            return newTaskResult( servletContext.getContextPath() + "/getData.html?file=" + f.getFileName() );
        }

    }

    class DiffExpressionDataWriterTask extends AbstractTask<ExpressionExperimentDataFetchCommand> {

        protected final Log log = LogFactory.getLog( this.getClass().getName() );

        DiffExpressionDataWriterTask( ExpressionExperimentDataFetchCommand command ) {
            super( command );
        }

        @Override
        public TaskResult call() {

            StopWatch watch = new StopWatch();
            watch.start();
            Collection<Path> files = new HashSet<>();

            if ( this.getTaskCommand().getAnalysisId() != null ) {

                Path f;
                try ( LockedPath lockedPath = expressionDataFileService.writeOrLocateDiffExAnalysisArchiveFileById( getTaskCommand().getAnalysisId(), getTaskCommand().isForceRewrite() ) ) {
                    f = lockedPath.getPath();
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

            // if ( files.isEmpty() ) {
            //    throw new EntityNotFoundException( "No data available (either due to no analyses being present, lack of authorization, or use of an invalid entity identifier)" );
            // } else if ( files.size() > 1 ) {
            //     throw new UnsupportedOperationException( "Sorry, you can't get multiple analyses at once using this method." );
            // }

            return newTaskResult( servletContext.getContextPath() + "/getData.html?file=" + files.iterator().next().getFileName() );
        }
    }
}

