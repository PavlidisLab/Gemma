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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.text.StringEscapeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.basecode.io.reader.DoubleMatrixReader;
import ubic.basecode.util.FileTools;
import ubic.gemma.core.job.AbstractTask;
import ubic.gemma.core.job.TaskResult;
import ubic.gemma.core.job.TaskRunningService;
import ubic.gemma.core.loader.expression.simple.SimpleExpressionDataLoaderService;
import ubic.gemma.core.loader.expression.simple.model.SimpleExpressionExperimentMetadata;
import ubic.gemma.core.loader.expression.simple.model.SimplePlatformMetadata;
import ubic.gemma.core.loader.expression.simple.model.SimpleQuantitationTypeMetadata;
import ubic.gemma.core.loader.expression.simple.model.SimpleTaxonMetadata;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;

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
    private SimpleExpressionDataLoaderService simpleExpressionDataLoaderService;

    @RequestMapping(value = "/expressionExperiment/upload.html", method = { RequestMethod.GET, RequestMethod.HEAD })
    public ModelAndView show() {
        return new ModelAndView( "dataUpload" );
    }

    public String load( SimpleExpressionExperimentLoadTaskCommand loadEECommand ) {
        loadEECommand.setValidateOnly( false );
        return taskRunningService.submitTask( new SimpleEELoadLocalTask( loadEECommand ) );
    }

    private class SimpleEELoadLocalTask extends AbstractTask<SimpleExpressionExperimentLoadTaskCommand> {

        public SimpleEELoadLocalTask( SimpleExpressionExperimentLoadTaskCommand commandObj ) {
            super( commandObj );
        }

        @Override
        public TaskResult call() {
            try {
                SimpleExpressionExperimentMetadata metadata = getMetadata( getTaskCommand() );

                File file = getFile( getTaskCommand() );
                DoubleMatrix<String, String> matrix = getData( getTaskCommand() );

                /*
                 * Main action here!
                 */
                ExpressionExperiment result = simpleExpressionDataLoaderService.create( metadata, matrix );

                // In theory we could do the link analysis right away. However, when a data set has new array designs,
                // we won't be ready yet.

                ExpressionExperimentUploadResponse eeUploadResponse = new ExpressionExperimentUploadResponse();
                eeUploadResponse.setTaskId( result.getId() );
                eeUploadResponse.setError( false );

                return newTaskResult( eeUploadResponse );
            } catch ( IOException e ) {
                // log.info( "There was an error opening an uploaded file:" + e.getMessage() );
                ExpressionExperimentUploadResponse eeUploadResponse = new ExpressionExperimentUploadResponse();
                eeUploadResponse.setError( true );
                eeUploadResponse
                        .setErrorMessage( "There was an error opening your uploaded file, please re-upload the file." );
                return newTaskResult( eeUploadResponse );
            } catch ( Exception e ) {
                // log.warn( "There was an error submitting your dataset, exception:" + e.toString() );
                ExpressionExperimentUploadResponse eeUploadResponse = new ExpressionExperimentUploadResponse();
                eeUploadResponse.setError( true );
                eeUploadResponse.setErrorMessage( e.getMessage() );
                return newTaskResult( eeUploadResponse );
            }
        }
    }

    public String validate( SimpleExpressionExperimentLoadTaskCommand command ) {
        assert command != null;
        command.setValidateOnly( true );
        // TODO: retrieve the locale from DWR
        return taskRunningService.submitTask( new SimpleEEValidateLocalTask( command ) );
    }

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    private class SimpleEEValidateLocalTask
            extends AbstractTask<SimpleExpressionExperimentLoadTaskCommand> {

        public SimpleEEValidateLocalTask( SimpleExpressionExperimentLoadTaskCommand commandObj ) {
            super( commandObj );
        }

        @Override
        public TaskResult call() {
            SimpleExpressionExperimentCommandValidation result = new SimpleExpressionExperimentCommandValidation();

            ExpressionDataFileUploadController.log.info( "Checking for valid name and files" );
            SimpleExpressionExperimentMetadata metadata = getMetadata( getTaskCommand() );

            if ( StringUtils.isBlank( metadata.getShortName() ) ) {
                result.setValid( false );
                result.setOtherProblemsMessage( "Short name cannot be blank." );
            } else if ( expressionExperimentService.findByShortName( metadata.getShortName() ) != null ) {
                result.setValid( false );
                result.setShortNameIsUnique( false );
            }

            if ( StringUtils.isBlank( metadata.getName() ) ) {
                result.setValid( false );
                result.setOtherProblemsMessage( "Name cannot be blank." );
            }

            if ( metadata.getQuantitationType() == null ) {
                result.setValid( false );
                result.setQuantitationTypeIsValid( false );
                result.setQuantitationTypeProblemMessage( "Quantitation type is missing." );
            } else {
                checkQuantitationType( metadata.getQuantitationType(), result );
            }

            checkDataFile( metadata, result );

            return newTaskResult( result );
        }

        private void checkQuantitationType( SimpleQuantitationTypeMetadata quantitationType, SimpleExpressionExperimentCommandValidation result ) {
            if ( StringUtils.isBlank( quantitationType.getName() ) ) {
                result.setValid( false );
                result.setQuantitationTypeIsValid( false );
                result.setQuantitationTypeProblemMessage( "Quantitation type name cannot be blank." );
            }
        }

        private void checkDataFile( SimpleExpressionExperimentMetadata metadata, SimpleExpressionExperimentCommandValidation result ) {
            if ( StringUtils.isBlank( getTaskCommand().getServerFilePath() ) ) {
                result.setValid( false );
                result.setDataFileIsValidFormat( false );
                result.setDataFileFormatProblemMessage( "No data file provided." );
                return;
            }

            try {
                DoubleMatrix<String, String> data = getData( getTaskCommand() );

                ExpressionDataFileUploadController.log.info( "Checking if probe labels match design" );
                result.setNumRows( data.rows() );
                result.setNumColumns( data.columns() );
                Long arrayDesignId = metadata.getArrayDesigns().iterator().next().getId();
                ArrayDesign design = arrayDesignService.loadOrFail( arrayDesignId );
                design = arrayDesignService.thaw( design );
                // check that the probes can be matched up...
                int numRowsMatchingArrayDesign = 0;
                int numRowsNotMatchingArrayDesign = 0;
                int i = 0;
                List<String> mismatches = new ArrayList<>();
                for ( CompositeSequence cs : design.getCompositeSequences() ) {
                    if ( data.containsRowName( cs.getName() ) ) {
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
                    result.setArrayDesignMatchesDataFile( false );
                    result.setArrayDesignMismatchProblemMessage( "Some of the probes from " + design.getShortName() + " were not matched in the supplied data file." );
                    result.setNonMatchingProbeNameExamples(
                            mismatches.subList( 0, Math.min( 10, mismatches.size() - 1 ) ) );
                }

            } catch ( FileNotFoundException e ) {
                result.setValid( false );
                result.setDataFileIsValidFormat( false );
                result.setDataFileFormatProblemMessage( "File is missing" );
            } catch ( Exception e ) {
                result.setValid( false );
                result.setDataFileIsValidFormat( false );
                result.setDataFileFormatProblemMessage( e.getMessage() );
            }
        }
    }

    private SimpleExpressionExperimentMetadata getMetadata( SimpleExpressionExperimentLoadTaskCommand commandObject ) {
        SimpleExpressionExperimentMetadata metadata = new SimpleExpressionExperimentMetadata();

        metadata.setShortName( scrub( commandObject.getShortName() ) );
        metadata.setName( scrub( commandObject.getName() ) );
        metadata.setDescription( scrub( commandObject.getDescription() ) );

        // might have used instead of actual ADs.
        Collection<Long> arrayDesignIds = commandObject.getArrayDesignIds();

        Long taxonId = commandObject.getTaxonId();
        if ( taxonId != null ) {
            metadata.setTaxon( SimpleTaxonMetadata.forId( taxonId ) );
        }

        Collection<SimplePlatformMetadata> arrayDesigns = metadata.getArrayDesigns();
        if ( arrayDesignIds != null && !arrayDesignIds.isEmpty() ) {
            for ( Long adid : arrayDesignIds ) {
                arrayDesigns.add( SimplePlatformMetadata.forId( adid ) );
            }
        }

        SimpleQuantitationTypeMetadata qtm = new SimpleQuantitationTypeMetadata();
        qtm.setName( commandObject.getQuantitationTypeName() );
        qtm.setDescription( commandObject.getQuantitationTypeDescription() );
        qtm.setScale( commandObject.getScale() );
        qtm.setIsRatio( commandObject.getIsRatio() );
        qtm.setIsPreferred( true );
        metadata.setQuantitationType( qtm );

        return metadata;
    }

    private DoubleMatrix<String, String> getData( SimpleExpressionExperimentLoadTaskCommand taskCommand ) throws IOException {
        File file = getFile( taskCommand );
        try ( InputStream stream = FileTools.getInputStreamFromPlainOrCompressedFile( file.getAbsolutePath() ) ) {
            if ( stream == null ) {
                throw new IllegalStateException( "Could not read from file " + file );
            }
            return new DoubleMatrixReader().read( stream );
        }
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

    private String scrub( String s ) {
        return StringEscapeUtils.escapeHtml4( s );
    }
}