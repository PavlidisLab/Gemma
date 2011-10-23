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

import java.io.File;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ubic.gemma.analysis.service.ExpressionDataFileService;
import ubic.gemma.job.AbstractTaskService;
import ubic.gemma.job.BackgroundJob;
import ubic.gemma.job.TaskCommand;
import ubic.gemma.job.TaskResult;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisService;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.QuantitationTypeService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.web.view.DownloadBinaryFileView;

/**
 * For the download of data files from the browser. We can send the 'raw' data for any one quantitation type, with gene
 * annotations, OR the 'filtered masked' matrix for the expression experiment.
 * 
 * @author pavlidis
 * @version $Id$
 */
@Controller
public class ExpressionExperimentDataFetchController extends AbstractTaskService {

    // ==========================================================
    class CoExpressionDataWriterJob extends BackgroundJob<ExpressionExperimentDataFetchCommand> {

        public CoExpressionDataWriterJob( ExpressionExperimentDataFetchCommand eeId ) {
            super( eeId );
        }

        @Override
        public TaskResult processJob() {

            StopWatch watch = new StopWatch();
            watch.start();

            assert this.command != null;
            Long eeId = this.command.getExpressionExperimentId();
            ExpressionExperiment ee = expressionExperimentService.load( eeId );

            if ( ee == null ) {
                throw new RuntimeException(
                        "No data available (either due to lack of authorization, or use of an invalid entity identifier)" );
            }

            File f = expressionDataFileService.writeOrLocateCoexpressionDataFile( ee, false );

            watch.stop();
            log.debug( "Finished getting co-expression file; done in " + watch.getTime() + " milliseconds" );

            String url = "/Gemma/getData.html?file=" + f.getName();

            ModelAndView mav = new ModelAndView( new RedirectView( url ) );

            return new TaskResult( command, mav );

        }

    }

    /**
     * @author keshav
     */
    class DataWriterJob extends BackgroundJob<ExpressionExperimentDataFetchCommand> {

        public DataWriterJob( ExpressionExperimentDataFetchCommand command ) {
            super( command );
        }

        @Override
        public TaskResult processJob() {

            StopWatch watch = new StopWatch();
            watch.start();

            /* 'do yer thang' */
            assert this.command != null;

            Long qtId = command.getQuantitationTypeId();
            Long eeId = command.getExpressionExperimentId();
            Long eedId = command.getExperimentalDesignId();
            String format = command.getFormat();
            boolean filtered = command.isFilter();

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
            ExpressionExperiment ee = null;

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
                    f = expressionDataFileService.writeOrLocateDesignFile( ee, true ); // overwrite, it's fast.
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

            assert f != null;

            watch.stop();
            log.debug( "Finished writing and downloading a file; done in " + watch.getTime() + " milliseconds" );

            String url = "/Gemma/getData.html?file=" + f.getName();

            ModelAndView mav = new ModelAndView( new RedirectView( url ) );

            return new TaskResult( command, mav );

        }

    }

    // ==========================================================
    class DiffExpressionDataWriterJob extends BackgroundJob<ExpressionExperimentDataFetchCommand> {

        public DiffExpressionDataWriterJob( ExpressionExperimentDataFetchCommand command ) {
            super( command );
        }

        @Override
        public TaskResult processJob() {

            StopWatch watch = new StopWatch();
            watch.start();
            File f;
            assert this.command != null;

            if ( this.command.getAnalysisId() != null ) {

                DifferentialExpressionAnalysis analysis = differentialExpressionAnalysisService.load(command.getAnalysisId());
                f = expressionDataFileService.writeOrLocateDiffExpressionDataFile( analysis, command.isForceRewrite() );

                
            } else if ( this.command.getExpressionExperimentId() != null ) {

                Long eeId = this.command.getExpressionExperimentId();
                ExpressionExperiment ee = expressionExperimentService.load( eeId );

                if ( ee == null ) {
                    throw new RuntimeException(
                            "No data available (either due to lack of authorization, or use of an invalid entity identifier)" );
                }

                f = expressionDataFileService.writeOrLocateDiffExpressionDataFile( ee, command.isForceRewrite() );

            } else {
                throw new IllegalArgumentException( "Must provide either experiment or specific analysis to provide" );
            }

            watch.stop();
            log.debug( "Finished writing and downloading differential expression file; done in " + watch.getTime()
                    + " milliseconds" );

            String url = "/Gemma/getData.html?file=" + f.getName();

            ModelAndView mav = new ModelAndView( new RedirectView( url ) );

            return new TaskResult( command, mav );

        }

    }

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private ExpressionDataFileService expressionDataFileService;

    @Autowired
    private QuantitationTypeService quantitationTypeService;
    
    @Autowired
    private DifferentialExpressionAnalysisService differentialExpressionAnalysisService;

    /**
     * Regular spring MVC request to fetch a file that already has been generated. It is assumed that the file is in the
     * DATA_DIR.
     * 
     * @param request
     * @param response
     * @return
     */
    @RequestMapping(value = "/getData.html", method = RequestMethod.GET)
    public ModelAndView downloadFile( String file ) {
        ModelAndView mav = new ModelAndView( new DownloadBinaryFileView() );
        String fullFilePath = ExpressionDataFileService.DATA_DIR + file;
        mav.addObject( DownloadBinaryFileView.PATH_PARAM, fullFilePath );
        return mav;
    }

    /**
     * AJAX Method - kicks off a job to start generating (if need be) the text based tab delimited co-expression data
     * file
     * 
     * @param eeId
     * @return
     */
    public String getCoExpressionDataFile( Long eeId ) {
        ExpressionExperimentDataFetchCommand tc = new ExpressionExperimentDataFetchCommand();
        tc.setExpressionExperimentId( eeId );
        CoExpressionDataWriterJob runner = new CoExpressionDataWriterJob( tc );
        return startTask( runner );
    }

    /**
     * AJAX Method - kicks off a job to start generating (if need be) the text based tab delimited experiment design
     * data file
     * 
     * @param command
     * @return
     * @throws InterruptedException
     */
    public String getDataFile( ExpressionExperimentDataFetchCommand command ) throws InterruptedException {
        DataWriterJob runner = new DataWriterJob( command );
        return startTask( runner );
    }

    /**
     * AJAX Method - kicks off a job to start generating (if need be) the text based tab delimited differential
     * expression data file
     * 
     * @param eeId
     * @return
     */
    public String getDiffExpressionDataFile( Long eeId ) {
        ExpressionExperimentDataFetchCommand tc = new ExpressionExperimentDataFetchCommand();
        tc.setExpressionExperimentId( eeId );
        DiffExpressionDataWriterJob runner = new DiffExpressionDataWriterJob( tc );
        return startTask( runner );
    }

    public void setExpressionDataFileService( ExpressionDataFileService expressionDataFileService ) {
        this.expressionDataFileService = expressionDataFileService;
    }

    public void setExpressionExperimentService( ExpressionExperimentService expressionExperimentService ) {
        this.expressionExperimentService = expressionExperimentService;
    }

    // ========================================================

    public void setQuantitationTypeService( QuantitationTypeService quantitationTypeService ) {
        this.quantitationTypeService = quantitationTypeService;
    }

    @Override
    protected BackgroundJob<?> getInProcessRunner( TaskCommand command ) {
        return null;
    }

    @Override
    protected BackgroundJob<?> getSpaceRunner( TaskCommand command ) {
        return null;
    }

}
