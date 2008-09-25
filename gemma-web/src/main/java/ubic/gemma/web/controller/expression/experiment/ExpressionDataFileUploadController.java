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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.basecode.util.FileTools;
import ubic.gemma.loader.expression.simple.SimpleExpressionDataLoaderService;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.util.progress.ProgressJob;
import ubic.gemma.util.progress.ProgressManager;
import ubic.gemma.web.controller.BackgroundControllerJob;

import ubic.gemma.web.controller.grid.AbstractSpacesController;

/**
 * Replaces SimpleExpressionExperimentLoadController
 * 
 * @spring.bean id="expressionDataFileUploadController"
 * @spring.property name="simpleExpressionDataLoaderService" ref="simpleExpressionDataLoaderService"
 * @spring.property name="arrayDesignService" ref="arrayDesignService"
 * @spring.property name="expressionExperimentService" ref="expressionExperimentService"
 * @author Paul
 * @version $Id$
 */
public class ExpressionDataFileUploadController extends AbstractSpacesController {

    private ArrayDesignService arrayDesignService;

    private SimpleExpressionDataLoaderService simpleExpressionDataLoaderService;

    private ExpressionExperimentService expressionExperimentService;

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

    /**
     * @param simpleExpressionDataLoaderService the simpleExpressionDataLoaderService to set
     */
    public void setSimpleExpressionDataLoaderService(
            SimpleExpressionDataLoaderService simpleExpressionDataLoaderService ) {
        this.simpleExpressionDataLoaderService = simpleExpressionDataLoaderService;
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
            parse = simpleExpressionDataLoaderService.parse( new FileInputStream( file ) );
        } catch ( FileNotFoundException e ) {
            result.setDataFileIsValidFormat( false );
            result.setDataFileFormatProblemMessage( "File is missing" );
        } catch ( IOException e ) {
            result.setDataFileIsValidFormat( false );
            result.setDataFileFormatProblemMessage( "File is invalid: " + e.getMessage() );
        }

        if ( parse != null ) {
            assert arrayDesignIds.size() == 1;
            Long arrayDesignId = arrayDesignIds.iterator().next();

            ArrayDesign design = arrayDesignService.load( arrayDesignId );
            arrayDesignService.thawLite( design );

            // check that the probes can be matched up...

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

    /**
     * @param ed
     * @return taskId
     * @throws Exception
     */
    public String validate( SimpleExpressionExperimentLoadCommand ed ) throws Exception {
        ed.setValidateOnly( true );
        return this.run( ed );
    }

    @Override
    protected BackgroundControllerJob getRunner( String jobId, Object command ) {
        if ( ( ( SimpleExpressionExperimentLoadCommand ) command ).isValidateOnly() ) {
            return new SimpleEEValidateJob( jobId, command, this.simpleExpressionDataLoaderService );
        } else {
            return new SimpleEELoadJob( jobId, command, this.simpleExpressionDataLoaderService );
        }
    }

    @Override
    protected BackgroundControllerJob<ModelAndView> getSpaceRunner( String jobId, Object command ) {
        throw new UnsupportedOperationException( "Not implemented yet" );
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.web.servlet.mvc.AbstractUrlViewController#getViewNameForRequest(javax.servlet.http.HttpServletRequest)
     */
    @Override
    @SuppressWarnings("unused")
    protected String getViewNameForRequest( HttpServletRequest request ) {
        return "dataUpload";
    }

    class SimpleEELoadJob extends BackgroundControllerJob<Long> {
        SimpleExpressionDataLoaderService simpleExpressionDataLoaderService;

        public SimpleEELoadJob( String taskId, Object commandObj,
                SimpleExpressionDataLoaderService simpleExpressionDataLoaderService ) {
            super( taskId, commandObj );
            this.simpleExpressionDataLoaderService = simpleExpressionDataLoaderService;
        }

        @SuppressWarnings("synthetic-access")
        public Long call() throws Exception {
            super.init();
            Map<Object, Object> model = new HashMap<Object, Object>();

            SimpleExpressionExperimentLoadCommand commandObject = ( SimpleExpressionExperimentLoadCommand ) command;

            File file = getFile( commandObject );

            populateCommandObject( commandObject );

            ProgressJob job = ProgressManager.createProgressJob( this.getTaskId(), securityContext.getAuthentication()
                    .getName(), "Loading data from " + file );

            InputStream stream = FileTools.getInputStreamFromPlainOrCompressedFile( file.getAbsolutePath() );

            if ( stream == null ) {
                throw new IllegalStateException( "Could not read from file " + file );
            }

            /*
             * Main action here!
             */
            ExpressionExperiment result = simpleExpressionDataLoaderService.load( commandObject, stream );

            stream.close();

            /*
             * FIXME this will fail.
             */
            this.saveMessage( "Successfully loaded " + result );

            model.put( "expressionExperiment", result );

            ProgressManager.destroyProgressJob( job );

            /*
             * Forward to the details view for the new experiment.
             */
            return result.getId();
        }

        /**
         * @param commandObject
         */
        private void populateCommandObject( SimpleExpressionExperimentLoadCommand commandObject ) {
            Collection<ArrayDesign> arrayDesigns = commandObject.getArrayDesigns();

            // might have used instead of actual ADs.
            Collection<Long> arrayDesignIds = commandObject.getArrayDesignIds();

            if ( arrayDesignIds != null && arrayDesignIds.size() > 0 ) {
                for ( Long adid : arrayDesignIds ) {
                    arrayDesigns.add( arrayDesignService.load( adid ) );
                }
            } else if ( arrayDesigns == null || arrayDesigns.size() == 0 ) {
                log.info( "Array design " + commandObject.getArrayDesignName() + " is new, will create from data." );
                ArrayDesign arrayDesign = ArrayDesign.Factory.newInstance();
                arrayDesign.setName( commandObject.getArrayDesignName() );
                commandObject.getArrayDesigns().add( arrayDesign );
            }

            Taxon taxon = commandObject.getTaxon();
            if ( taxon == null || StringUtils.isBlank( taxon.getScientificName() ) ) {
                taxon = Taxon.Factory.newInstance();
                taxon.setScientificName( commandObject.getTaxonName() );
                commandObject.setTaxon( taxon );
            }
        }
    }

    /**
     *  
     */
    class SimpleEEValidateJob extends BackgroundControllerJob<SimpleExpressionExperimentCommandValidation> {

        SimpleExpressionDataLoaderService simpleExpressionDataLoaderService;

        public SimpleEEValidateJob( String taskId, Object commandObj,
                SimpleExpressionDataLoaderService simpleExpressionDataLoaderService ) {
            super( taskId, commandObj );
            this.simpleExpressionDataLoaderService = simpleExpressionDataLoaderService;
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.util.concurrent.Callable#call()
         */
        public SimpleExpressionExperimentCommandValidation call() throws Exception {
            super.init();

            ProgressJob job = ProgressManager.createProgressJob( this.getTaskId(), securityContext.getAuthentication()
                    .getName(), "Validation" );

            /*
             * Check that 1) Data file is basically valid and parseable 2) The array design matches the data files.F
             */
            SimpleExpressionExperimentCommandValidation result = doValidate( ( SimpleExpressionExperimentLoadCommand ) this.command );

            log.info( "Validation done" );

            ProgressManager.destroyProgressJob( job );

            return result;

        }
    }

    public void setExpressionExperimentService( ExpressionExperimentService expressionExperimentService ) {
        this.expressionExperimentService = expressionExperimentService;
    }

}