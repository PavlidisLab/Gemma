/*
 * The Gemma project
 * 
 * Copyright (c) 2008 University of British Columbia
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
 */package ubic.gemma.web.controller.expression.experiment;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.basecode.util.FileTools;
import ubic.gemma.analysis.preprocess.ProcessedExpressionDataVectorCreateService;
import ubic.gemma.expression.experiment.service.ExpressionExperimentService;
import ubic.gemma.genome.taxon.service.TaxonService;
import ubic.gemma.job.BackgroundJob;
import ubic.gemma.job.TaskResult;
import ubic.gemma.job.TaskRunningService;
import ubic.gemma.loader.expression.simple.SimpleExpressionDataLoaderService;
import ubic.gemma.model.common.quantitationtype.GeneralType;
import ubic.gemma.model.common.quantitationtype.StandardQuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * Replaces SimpleExpressionExperimentLoadController
 * 
 * @author Paul
 * @version $Id$
 */
@Controller
public class ExpressionDataFileUploadController {

    private class SimpleEELoadLocalJob extends BackgroundJob<SimpleExpressionExperimentLoadTaskCommand, TaskResult> {

        public SimpleEELoadLocalJob( SimpleExpressionExperimentLoadTaskCommand commandObj ) {
            super( commandObj );
        }

        @Override
        public TaskResult processJob() {
            try {
                File file = getFile( command );

                populateCommandObject( command );

                InputStream stream = FileTools.getInputStreamFromPlainOrCompressedFile( file.getAbsolutePath() );

                if ( stream == null ) {
                    throw new IllegalStateException( "Could not read from file " + file );
                }

                /*
                 * Main action here!
                 */
                scrub( command );
                ExpressionExperiment result = simpleExpressionDataLoaderService.create( command, stream );
                stream.close();

                log.info( "Preprocessing the data for analysis" );
                processedExpressionDataVectorCreateService.computeProcessedExpressionData( result );

                // In theory we could do the link analysis right away. However, when a data set has new array designs,
                // we won't be ready yet.

                ExpressionExperimentUploadResponse eeUploadResponse = new ExpressionExperimentUploadResponse();
                eeUploadResponse.setTaskId( result.getId() );
                eeUploadResponse.setError( false );

                return new TaskResult( command, eeUploadResponse );
            } catch ( IOException e ) {
                // log.info( "There was an error opening an uploaded file:" + e.getMessage() );
                ExpressionExperimentUploadResponse eeUploadResponse = new ExpressionExperimentUploadResponse();
                eeUploadResponse.setError( true );
                eeUploadResponse
                        .setErrorMessage( "There was an error opening your uploaded file, please re-upload the file." );
                return new TaskResult( command, eeUploadResponse );
            } catch ( Exception e ) {
                // log.warn( "There was an error submitting your dataset, exception:" + e.toString() );
                ExpressionExperimentUploadResponse eeUploadResponse = new ExpressionExperimentUploadResponse();
                eeUploadResponse.setError( true );
                eeUploadResponse.setErrorMessage( e.getMessage() );
                return new TaskResult( command, eeUploadResponse );
            }
        }

        /**
         * @param commandObject
         */
        private void populateCommandObject( SimpleExpressionExperimentLoadTaskCommand commandObject ) {
            Collection<ArrayDesign> arrayDesigns = commandObject.getArrayDesigns();

            // might have used instead of actual ADs.
            Collection<Long> arrayDesignIds = commandObject.getArrayDesignIds();

            Long taxonId = commandObject.getTaxonId();

            if ( taxonId != null ) {
                commandObject.setTaxon( taxonService.load( taxonId ) );
            }

            if ( arrayDesignIds != null && arrayDesignIds.size() > 0 ) {
                for ( Long adid : arrayDesignIds ) {
                    arrayDesigns.add( arrayDesignService.load( adid ) );
                }
            } else if ( arrayDesigns == null || arrayDesigns.size() == 0 ) {
                log.info( "Platform " + commandObject.getArrayDesignName() + " is new, will create from data." );
                ArrayDesign arrayDesign = ArrayDesign.Factory.newInstance();
                arrayDesign.setName( commandObject.getArrayDesignName() );
                arrayDesign.setPrimaryTaxon( commandObject.getTaxon() );
                commandObject.getArrayDesigns().add( arrayDesign );
            }

            commandObject.setType( StandardQuantitationType.AMOUNT );
            commandObject.setGeneralType( GeneralType.QUANTITATIVE );
            commandObject.setIsMaskedPreferred( true );

        }
    }

    /**
     *  
     */
    private class SimpleEEValidateLocalJob extends BackgroundJob<SimpleExpressionExperimentLoadTaskCommand, TaskResult> {

        public SimpleEEValidateLocalJob( SimpleExpressionExperimentLoadTaskCommand commandObj ) {
            super( commandObj );
        }

        @Override
        public TaskResult processJob() {
            SimpleExpressionExperimentCommandValidation result = doValidate( this.command );
            return new TaskResult( command, result );
        }
    }

    private static final Log log = LogFactory.getLog( ExpressionDataFileUploadController.class.getName() );

    @Autowired private TaskRunningService taskRunningService;
    @Autowired private ArrayDesignService arrayDesignService;
    @Autowired private ExpressionExperimentService expressionExperimentService;
    @Autowired private ProcessedExpressionDataVectorCreateService processedExpressionDataVectorCreateService;
    @Autowired private SimpleExpressionDataLoaderService simpleExpressionDataLoaderService;
    @Autowired private TaxonService taxonService;

    /**
     * AJAX
     * 
     * @param loadEECommand
     * @return the taskid
     */
    public String load( SimpleExpressionExperimentLoadTaskCommand loadEECommand) {
        loadEECommand.setValidateOnly( false );
        return taskRunningService.submitLocalJob(new SimpleEELoadLocalJob(loadEECommand));
    }

    /**
     * @param arrayDesignService the arrayDesignService to set
     */
    public void setArrayDesignService( ArrayDesignService arrayDesignService ) {
        this.arrayDesignService = arrayDesignService;
    }

    public void setExpressionExperimentService( ExpressionExperimentService expressionExperimentService ) {
        this.expressionExperimentService = expressionExperimentService;
    }

    public void setProcessedExpressionDataVectorCreateService(
            ProcessedExpressionDataVectorCreateService processedExpressionDataVectorCreateService ) {
        this.processedExpressionDataVectorCreateService = processedExpressionDataVectorCreateService;
    }

    /**
     * @param simpleExpressionDataLoaderService the simpleExpressionDataLoaderService to set
     */
    public void setSimpleExpressionDataLoaderService(
            SimpleExpressionDataLoaderService simpleExpressionDataLoaderService ) {
        this.simpleExpressionDataLoaderService = simpleExpressionDataLoaderService;
    }

    public void setTaxonService( TaxonService taxonService ) {
        this.taxonService = taxonService;
    }

    @RequestMapping("/expressionExperiment/upload.html")
    @SuppressWarnings("unused")
    public ModelAndView show( HttpServletRequest request, HttpServletResponse response ) {
        return new ModelAndView( "dataUpload" );
    }

    /**
     * @param command
     * @return taskId
     * @throws Exception
     */
    public String validate( SimpleExpressionExperimentLoadTaskCommand command ) throws Exception {
        assert command != null;
        command.setValidateOnly(true);
        return taskRunningService.submitLocalJob(new SimpleEEValidateLocalJob( command ));
    }

    /**
     * @param command
     * @return
     */
    private SimpleExpressionExperimentCommandValidation doValidate( SimpleExpressionExperimentLoadTaskCommand command ) {

        scrub( command );
        ExpressionExperiment existing = expressionExperimentService.findByShortName( command.getShortName() );
        SimpleExpressionExperimentCommandValidation result = new SimpleExpressionExperimentCommandValidation();

        log.info( "Checking for valid name and files" );

        result.setShortNameIsUnique( existing == null );

        String localPath = command.getServerFilePath();
        if ( StringUtils.isBlank( localPath ) ) {
            result.setDataFileIsValidFormat( false );
            result.setDataFileFormatProblemMessage( "File is missing" );
            return result;
        }

        File file = new File( localPath );

        if ( !file.canRead() ) {
            result.setDataFileIsValidFormat( false );
            result.setDataFileFormatProblemMessage( "Cannot read from file" );
            return result;
        }

        Collection<Long> arrayDesignIds = command.getArrayDesignIds();

        if ( arrayDesignIds.isEmpty() ) {
            result.setArrayDesignMatchesDataFile( false );
            result.setArrayDesignMismatchProblemMessage( "Platform must be provided" );
            return result;
        }

        DoubleMatrix<String, String> parse = null;
        try {
            parse = simpleExpressionDataLoaderService.parse( FileTools.getInputStreamFromPlainOrCompressedFile( file
                    .getAbsolutePath() ) );
        } catch ( FileNotFoundException e ) {
            result.setDataFileIsValidFormat( false );
            result.setDataFileFormatProblemMessage( "File is missing" );
        } catch ( IOException e ) {
            result.setDataFileIsValidFormat( false );
            result.setDataFileFormatProblemMessage( "File is invalid: " + e.getMessage() );
        } catch ( IllegalArgumentException e ) {
            result.setDataFileIsValidFormat( false );
            result.setDataFileFormatProblemMessage( "File is invalid: " + e.getMessage() );
        } catch ( Exception e ) {
            result.setDataFileIsValidFormat( false );
            result.setDataFileFormatProblemMessage( "Error Validating: " + e.getMessage() );
        }

        if ( parse != null ) {

            log.info( "Checking if probe labels match design" );

            result.setNumRows( parse.rows() );
            result.setNumColumns( parse.columns() );

            Long arrayDesignId = arrayDesignIds.iterator().next();

            ArrayDesign design = arrayDesignService.load( arrayDesignId );
            design = arrayDesignService.thaw( design );

            // check that the probes can be matched up...
            int numRowsMatchingArrayDesign = 0;
            int numRowsNotMatchingArrayDesign = 0;
            int i = 0;
            List<String> mismatches = new ArrayList<String>();
            for ( CompositeSequence cs : design.getCompositeSequences() ) {
                if ( parse.containsRowName( cs.getName() ) ) {
                    numRowsMatchingArrayDesign++;
                } else {
                    numRowsNotMatchingArrayDesign++;
                    mismatches.add( cs.getName() );
                }
                if ( ++i % 2000 == 0 ) {
                    log.info( i + " probes checked, " + numRowsMatchingArrayDesign + " match" );
                }
            }

            result.setNumberMatchingProbes( numRowsMatchingArrayDesign );
            result.setNumberOfNonMatchingProbes( numRowsNotMatchingArrayDesign );
            if ( mismatches.size() > 0 ) {
                result.setNonMatchingProbeNameExamples( mismatches.subList( 0, Math.min( 10, mismatches.size() - 1 ) ) );
            }

        }

        return result;

    }

    /**
     * @param ed
     */
    private File getFile( SimpleExpressionExperimentLoadTaskCommand ed ) {
        File file;
        String localPath = ed.getServerFilePath();
        if ( StringUtils.isBlank( localPath ) ) {
            throw new IllegalArgumentException( "Must provide the file" );
        }

        file = new File( localPath );

        if ( !file.canRead() ) {
            throw new IllegalArgumentException( "Cannot read from file:" + file );
        }

        return file;
    }

    private void scrub( SimpleExpressionExperimentLoadTaskCommand o ) {
        o.setName( scrub( o.getName() ) );
        o.setDescription( scrub( o.getDescription() ) );
        o.setShortName( scrub( o.getShortName() ) );

    }

    private String scrub( String s ) {
        return StringEscapeUtils.escapeHtml( s );
    }

}