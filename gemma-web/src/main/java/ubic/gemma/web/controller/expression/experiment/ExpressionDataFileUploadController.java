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
 */
package ubic.gemma.web.controller.expression.experiment;

import org.apache.commons.text.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.basecode.util.FileTools;
import ubic.gemma.core.analysis.preprocess.PreprocessingException;
import ubic.gemma.core.analysis.preprocess.PreprocessorService;
import ubic.gemma.core.job.TaskResult;
import ubic.gemma.core.job.TaskRunningService;
import ubic.gemma.core.loader.expression.simple.SimpleExpressionDataLoaderService;
import ubic.gemma.core.job.AbstractTask;
import ubic.gemma.model.common.quantitationtype.GeneralType;
import ubic.gemma.model.common.quantitationtype.StandardQuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Replaces SimpleExpressionExperimentLoadController
 *
 * @author Paul
 */
@Controller
public class ExpressionDataFileUploadController {

    private static final Log log = LogFactory.getLog( ExpressionDataFileUploadController.class.getName() );
    @Autowired
    private TaskRunningService taskRunningService;
    @Autowired
    private ArrayDesignService arrayDesignService;
    @Autowired
    private ExpressionExperimentService expressionExperimentService;
    @Autowired
    private PreprocessorService preprocessorService;
    @Autowired
    private SimpleExpressionDataLoaderService simpleExpressionDataLoaderService;
    @Autowired
    private TaxonService taxonService;

    public String load( SimpleExpressionExperimentLoadTaskCommand loadEECommand ) {
        loadEECommand.setValidateOnly( false );
        return taskRunningService.submitTask( new SimpleEELoadLocalTask( loadEECommand ) );
    }

    @RequestMapping(value = "/expressionExperiment/upload.html", method = { RequestMethod.GET, RequestMethod.HEAD })
    public ModelAndView show() {
        return new ModelAndView( "dataUpload" );
    }

    public String validate( SimpleExpressionExperimentLoadTaskCommand command ) {
        assert command != null;
        command.setValidateOnly( true );
        return taskRunningService.submitTask( new SimpleEEValidateLocalTask( command ) );
    }

    private SimpleExpressionExperimentCommandValidation doValidate(
            SimpleExpressionExperimentLoadTaskCommand command ) {

        this.scrub( command );
        ExpressionExperiment existing = expressionExperimentService.findByShortName( command.getShortName() );
        SimpleExpressionExperimentCommandValidation result = new SimpleExpressionExperimentCommandValidation();

        ExpressionDataFileUploadController.log.info( "Checking for valid name and files" );

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
            parse = simpleExpressionDataLoaderService
                    .parse( FileTools.getInputStreamFromPlainOrCompressedFile( file.getAbsolutePath() ) );
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

            ExpressionDataFileUploadController.log.info( "Checking if probe labels match design" );

            result.setNumRows( parse.rows() );
            result.setNumColumns( parse.columns() );

            Long arrayDesignId = arrayDesignIds.iterator().next();

            ArrayDesign design = arrayDesignService.loadOrFail( arrayDesignId );
            design = arrayDesignService.thaw( design );

            // check that the probes can be matched up...
            int numRowsMatchingArrayDesign = 0;
            int numRowsNotMatchingArrayDesign = 0;
            int i = 0;
            List<String> mismatches = new ArrayList<>();
            for ( CompositeSequence cs : design.getCompositeSequences() ) {
                if ( parse.containsRowName( cs.getName() ) ) {
                    numRowsMatchingArrayDesign++;
                } else {
                    numRowsNotMatchingArrayDesign++;
                    mismatches.add( cs.getName() );
                }
                if ( ++i % 2000 == 0 ) {
                    ExpressionDataFileUploadController.log
                            .info( i + " probes checked, " + numRowsMatchingArrayDesign + " match" );
                }
            }

            result.setNumberMatchingProbes( numRowsMatchingArrayDesign );
            result.setNumberOfNonMatchingProbes( numRowsNotMatchingArrayDesign );
            if ( !mismatches.isEmpty() ) {
                result.setNonMatchingProbeNameExamples(
                        mismatches.subList( 0, Math.min( 10, mismatches.size() - 1 ) ) );
            }

        }

        return result;
    }

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
        o.setName( this.scrub( o.getName() ) );
        o.setDescription( this.scrub( o.getDescription() ) );
        o.setShortName( this.scrub( o.getShortName() ) );
    }

    private String scrub( String s ) {
        return StringEscapeUtils.escapeHtml4( s );
    }

    private class SimpleEELoadLocalTask extends AbstractTask<SimpleExpressionExperimentLoadTaskCommand> {

        public SimpleEELoadLocalTask( SimpleExpressionExperimentLoadTaskCommand commandObj ) {
            super( commandObj );
        }

        @Override
        public TaskResult call() {
            File file = ExpressionDataFileUploadController.this.getFile( taskCommand );

            try ( InputStream stream = FileTools.getInputStreamFromPlainOrCompressedFile( file.getAbsolutePath() ) ) {

                this.populateCommandObject( taskCommand );

                if ( stream == null ) {
                    throw new IllegalStateException( "Could not read from file " + file );
                }

                /*
                 * Main action here!
                 */
                ExpressionDataFileUploadController.this.scrub( taskCommand );
                ExpressionExperiment result = simpleExpressionDataLoaderService.create( taskCommand, stream );
                stream.close();

                ExpressionDataFileUploadController.log.info( "Preprocessing the data for analysis" );
                try {
                    preprocessorService.process( result );
                } catch ( PreprocessingException e ) {
                    ExpressionDataFileUploadController.log.error( "Error during postprocessing", e );
                }
                // In theory we could do the link analysis right away. However, when a data set has new array designs,
                // we won't be ready yet.

                ExpressionExperimentUploadResponse eeUploadResponse = new ExpressionExperimentUploadResponse();
                eeUploadResponse.setTaskId( result.getId() );
                eeUploadResponse.setError( false );

                return new TaskResult( taskCommand, eeUploadResponse );
            } catch ( IOException e ) {
                // log.info( "There was an error opening an uploaded file:" + e.getMessage() );
                ExpressionExperimentUploadResponse eeUploadResponse = new ExpressionExperimentUploadResponse();
                eeUploadResponse.setError( true );
                eeUploadResponse
                        .setErrorMessage( "There was an error opening your uploaded file, please re-upload the file." );
                return new TaskResult( taskCommand, eeUploadResponse );
            } catch ( Exception e ) {
                // log.warn( "There was an error submitting your dataset, exception:" + e.toString() );
                ExpressionExperimentUploadResponse eeUploadResponse = new ExpressionExperimentUploadResponse();
                eeUploadResponse.setError( true );
                eeUploadResponse.setErrorMessage( e.getMessage() );
                return new TaskResult( taskCommand, eeUploadResponse );
            }
        }

        private void populateCommandObject( SimpleExpressionExperimentLoadTaskCommand commandObject ) {
            Collection<ArrayDesign> arrayDesigns = commandObject.getArrayDesigns();

            // might have used instead of actual ADs.
            Collection<Long> arrayDesignIds = commandObject.getArrayDesignIds();

            Long taxonId = commandObject.getTaxonId();

            if ( taxonId != null ) {
                commandObject.setTaxon( taxonService.load( taxonId ) );
            }

            if ( arrayDesignIds != null && !arrayDesignIds.isEmpty() ) {
                for ( Long adid : arrayDesignIds ) {
                    arrayDesigns.add( arrayDesignService.load( adid ) );
                }
            } else if ( arrayDesigns == null || arrayDesigns.isEmpty() ) {
                ExpressionDataFileUploadController.log
                        .info( "Platform " + commandObject.getArrayDesignName() + " is new, will create from data." );
                ArrayDesign arrayDesign = ArrayDesign.Factory.newInstance();
                arrayDesign.setName( commandObject.getArrayDesignName() );
                arrayDesign.setPrimaryTaxon( commandObject.getTaxon() );
                commandObject.getArrayDesigns().add( arrayDesign );
            }

            commandObject.setType( StandardQuantitationType.AMOUNT ); // FIXME might need to be COUNT for some data.
            commandObject.setGeneralType( GeneralType.QUANTITATIVE );
            commandObject.setIsMaskedPreferred( true );
        }
    }

    private class SimpleEEValidateLocalTask
            extends AbstractTask<SimpleExpressionExperimentLoadTaskCommand> {

        public SimpleEEValidateLocalTask( SimpleExpressionExperimentLoadTaskCommand commandObj ) {
            super( commandObj );
        }

        @Override
        public TaskResult call() {
            SimpleExpressionExperimentCommandValidation result = ExpressionDataFileUploadController.this
                    .doValidate( taskCommand );
            return new TaskResult( taskCommand, result );
        }
    }

}