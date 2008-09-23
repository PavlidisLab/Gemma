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

import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.springframework.security.context.SecurityContextHolder;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

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
import ubic.gemma.web.controller.common.auditAndSecurity.FileUpload;
import ubic.gemma.web.controller.grid.AbstractSpacesController;
import ubic.gemma.web.util.upload.FileUploadUtil;

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

        ExpressionExperiment existing = expressionExperimentService.findByShortName( ed.getShortName() );

        if ( existing != null ) {
            throw new IllegalArgumentException( "There is already an experiment with short name " + ed.getShortName()
                    + "; please choose something unique." );
        }

        /*
         * FIXME get the path to the file, which has already been uploaded, some other safer way.
         */

        FileUpload fileUpload = ed.getDataFile();

        if ( fileUpload == null || ( fileUpload.getFile() == null && StringUtils.isBlank( fileUpload.getLocalPath() ) ) ) {
            throw new IllegalArgumentException( "Must provide a file to upload" );
        }

        if ( StringUtils.isBlank( fileUpload.getLocalPath() ) ) {
            FileUploadUtil.copyUploadedFile( null, "dataFile.file" );
        }

        return this.run( ed );
    }

    /**
     * @param ed
     * @return
     * @throws Exception
     */
    public String validate( SimpleExpressionExperimentLoadCommand ed ) throws Exception {
        ed.setValidateOnly( true );
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

    @Override
    protected BackgroundControllerJob<ModelAndView> getRunner( String jobId, Object command ) {
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

    class SimpleEEValidateJob extends BackgroundControllerJob<ModelAndView> {

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
        public ModelAndView call() throws Exception {
            // TODO Auto-generated method stub
            SecurityContextHolder.setContext( securityContext );
            Map<Object, Object> model = new HashMap<Object, Object>();
            SimpleExpressionExperimentLoadCommand commandObject = ( SimpleExpressionExperimentLoadCommand ) command;
            Collection<ArrayDesign> arrayDesigns = commandObject.getArrayDesigns();
            // might have used instead of actual ADs.
            Collection<Long> arrayDesignIds = commandObject.getArrayDesignIds();

            /*
             * Check that 1) Data file is basically valid and parseable 2) The array design matches the data files.F
             */

            return null;
        }
    }

    class SimpleEELoadJob extends BackgroundControllerJob<ModelAndView> {
        SimpleExpressionDataLoaderService simpleExpressionDataLoaderService;

        public SimpleEELoadJob( String taskId, Object commandObj,
                SimpleExpressionDataLoaderService simpleExpressionDataLoaderService ) {
            super( taskId, commandObj );
            this.simpleExpressionDataLoaderService = simpleExpressionDataLoaderService;
        }

        @SuppressWarnings("synthetic-access")
        public ModelAndView call() throws Exception {
            SecurityContextHolder.setContext( securityContext );
            Map<Object, Object> model = new HashMap<Object, Object>();

            SimpleExpressionExperimentLoadCommand commandObject = ( SimpleExpressionExperimentLoadCommand ) command;

            FileUpload fileUpload = commandObject.getDataFile();

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

            ProgressJob job = ProgressManager.createProgressJob( this.getTaskId(), securityContext.getAuthentication()
                    .getName(), "Loading data from " + fileUpload.getLocalPath() );

            String dataFilePath = fileUpload.getLocalPath();

            assert dataFilePath != null;

            InputStream stream = FileTools.getInputStreamFromPlainOrCompressedFile( dataFilePath );

            if ( stream == null ) {
                throw new IllegalStateException( "Could not read from file " + dataFilePath );
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
            return new ModelAndView( new RedirectView( "/Gemma/expressionExperiment/showExpressionExperiment.html?id="
                    + result.getId() ), model );
        }
    }

}