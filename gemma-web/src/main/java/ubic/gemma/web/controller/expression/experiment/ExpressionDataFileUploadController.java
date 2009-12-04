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

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.servlet.ModelAndView;

import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.basecode.util.FileTools;
import ubic.gemma.analysis.preprocess.ProcessedExpressionDataVectorCreateService;
import ubic.gemma.loader.expression.simple.SimpleExpressionDataLoaderService;
import ubic.gemma.model.common.quantitationtype.GeneralType;
import ubic.gemma.model.common.quantitationtype.StandardQuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.genome.TaxonService;
import ubic.gemma.util.progress.ProgressJob;
import ubic.gemma.util.progress.ProgressManager;
import ubic.gemma.web.controller.BackgroundControllerJob;
import ubic.gemma.web.controller.grid.AbstractSpacesController;

/**
 * Replaces SimpleExpressionExperimentLoadController
 * 
 * @author Paul
 * @version $Id$
 */
@Controller
public class ExpressionDataFileUploadController extends AbstractSpacesController {

    class SimpleEELoadJob extends BackgroundControllerJob<Long> {

        public SimpleEELoadJob( String taskId, Object commandObj ) {
            super( taskId );
            this.setCommand( commandObj );
        }

        @SuppressWarnings("synthetic-access")
        public Long call() throws Exception {

            SimpleExpressionExperimentLoadCommand commandObject = ( SimpleExpressionExperimentLoadCommand ) command;

            File file = getFile( commandObject );

            populateCommandObject( commandObject );

            ProgressJob job = init( "Loading data from " + file.getName() );

            InputStream stream = FileTools.getInputStreamFromPlainOrCompressedFile( file.getAbsolutePath() );

            if ( stream == null ) {
                throw new IllegalStateException( "Could not read from file " + file );
            }

            /*
             * Main action here!
             */
            ExpressionExperiment result = simpleExpressionDataLoaderService.load( commandObject, stream );
            stream.close();

            log.info( "Preprocessing the data for analysis" );
            processedExpressionDataVectorCreateService.computeProcessedExpressionData( result );

            ProgressManager.destroyProgressJob( job );

            // In theory we could do the link analysis right away. However, when a data set has new array designs, we
            // won't be ready yet.

            return result.getId();
        }

        /**
         * @param commandObject
         */
        private void populateCommandObject( SimpleExpressionExperimentLoadCommand commandObject ) {
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
                log.info( "Array design " + commandObject.getArrayDesignName() + " is new, will create from data." );
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
    class SimpleEEValidateJob extends BackgroundControllerJob<SimpleExpressionExperimentCommandValidation> {

        public SimpleEEValidateJob( String taskId, Object commandObj ) {
            super( taskId );
            this.setCommand( commandObj );
        }

        /*
         * (non-Javadoc)
         * @see java.util.concurrent.Callable#call()
         */
        public SimpleExpressionExperimentCommandValidation call() throws Exception {
            ProgressJob job = init( "Validating" );

            /*
             * Check that 1) Data file is basically valid and parseable 2) The array design matches the data files.
             */
            SimpleExpressionExperimentCommandValidation result = doValidate( ( SimpleExpressionExperimentLoadCommand ) this.command );

            ProgressManager.destroyProgressJob( job );

            return result;

        }
    }

    @Autowired
    private ArrayDesignService arrayDesignService;

    @Autowired
    private SimpleExpressionDataLoaderService simpleExpressionDataLoaderService;

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private TaxonService taxonService;

    @Autowired
    private ProcessedExpressionDataVectorCreateService processedExpressionDataVectorCreateService;

    /**
     * AJAX
     * 
     * @param ed
     * @return the taskid
     */
    public String load( SimpleExpressionExperimentLoadCommand ed ) throws Exception {
        ed.setValidateOnly( false );
        return this.run( ed );
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

    /**
     * @param ed
     * @return taskId
     * @throws Exception
     */
    public String validate( SimpleExpressionExperimentLoadCommand ed ) throws Exception {
        assert ed != null;
        ed.setValidateOnly( true );
        return this.run( ed );
    }

    @Override
    protected BackgroundControllerJob getRunner( String jobId, Object command ) {
        if ( ( ( SimpleExpressionExperimentLoadCommand ) command ).isValidateOnly() ) {
            return new SimpleEEValidateJob( jobId, command );
        }
        return new SimpleEELoadJob( jobId, command );
    }

    @Override
    protected BackgroundControllerJob<ModelAndView> getSpaceRunner( String jobId, Object command ) {
        throw new UnsupportedOperationException( "Not implemented yet" );
    }

    /*
     * (non-Javadoc)
     * @seeorg.springframework.web.servlet.mvc.AbstractUrlViewController#getViewNameForRequest(javax.servlet.http.
     * HttpServletRequest)
     */
    @Override
    protected String getViewNameForRequest( HttpServletRequest request ) {
        return "dataUpload";
    }

    /**
     * @param ed
     * @return
     */
    private SimpleExpressionExperimentCommandValidation doValidate( SimpleExpressionExperimentLoadCommand ed ) {

        ExpressionExperiment existing = expressionExperimentService.findByShortName( ed.getShortName() );
        SimpleExpressionExperimentCommandValidation result = new SimpleExpressionExperimentCommandValidation();

        result.setShortNameIsUnique( existing == null );

        String localPath = ed.getServerFilePath();
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

        Collection<Long> arrayDesignIds = ed.getArrayDesignIds();

        if ( arrayDesignIds.isEmpty() ) {
            result.setArrayDesignMatchesDataFile( false );
            result.setDataFileFormatProblemMessage( "Array design must be provided" );
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
        }

        if ( parse != null ) {

            result.setNumRows( parse.rows() );
            result.setNumColumns( parse.columns() );

            assert arrayDesignIds.size() == 1;
            Long arrayDesignId = arrayDesignIds.iterator().next();

            ArrayDesign design = arrayDesignService.load( arrayDesignId );
            arrayDesignService.thawLite( design );

            // check that the probes can be matched up...
            int numRowsMatchingArrayDesign = 0;
            int numRowsNotMatchingArrayDesign = 0;
            List<String> mismatches = new ArrayList<String>();
            for ( CompositeSequence cs : design.getCompositeSequences() ) {
                if ( parse.containsRowName( cs.getName() ) ) {
                    numRowsMatchingArrayDesign++;
                } else {
                    numRowsNotMatchingArrayDesign++;
                    mismatches.add( cs.getName() );
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
    private File getFile( SimpleExpressionExperimentLoadCommand ed ) {
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

}