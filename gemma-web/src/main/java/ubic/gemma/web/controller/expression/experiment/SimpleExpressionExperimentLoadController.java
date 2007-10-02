/*
 * The Gemma project
 * 
 * Copyright (c) 2006 University of British Columbia
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
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.acegisecurity.context.SecurityContext;
import org.acegisecurity.context.SecurityContextHolder;
import org.apache.commons.lang.StringUtils;
import org.springframework.validation.BindException;
import org.springframework.web.bind.ServletRequestDataBinder;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ubic.basecode.util.FileTools;
import ubic.gemma.loader.expression.simple.SimpleExpressionDataLoaderService;
import ubic.gemma.loader.expression.simple.model.SimpleExpressionExperimentMetaData;
import ubic.gemma.loader.genome.taxon.SupportedTaxa;
import ubic.gemma.model.common.quantitationtype.GeneralType;
import ubic.gemma.model.common.quantitationtype.ScaleType;
import ubic.gemma.model.common.quantitationtype.StandardQuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonService;
import ubic.gemma.util.gemmaspaces.GemmaSpacesUtil;
import ubic.gemma.util.progress.ProgressJob;
import ubic.gemma.util.progress.ProgressManager;
import ubic.gemma.util.progress.TaskRunningService;
import ubic.gemma.web.controller.BackgroundControllerJob;
import ubic.gemma.web.controller.common.auditAndSecurity.FileUpload;
import ubic.gemma.web.controller.gemmaspaces.AbstractGemmaSpacesFormController;
import ubic.gemma.web.propertyeditor.ArrayDesignPropertyEditor;
import ubic.gemma.web.propertyeditor.TaxonPropertyEditor;
import ubic.gemma.web.util.ConfigurationCookie;
import ubic.gemma.web.util.MessageUtil;
import ubic.gemma.web.util.upload.FileUploadUtil;

/**
 * For creating expression experiments from flat files.
 * 
 * @author pavlidis
 * @version $Id$
 * @spring.bean id="simpleExpressionExperimentLoadController"
 * @spring.property name="commandName" value="simpleExpressionExperimentLoadCommand"
 * @spring.property name="commandClass"
 *                  value="ubic.gemma.web.controller.expression.experiment.SimpleExpressionExperimentLoadCommand"
 * @spring.property name="validator" ref="simpleExpressionExperimentLoadValidator"
 * @spring.property name="formView" value="simpleExpressionExperimentForm"
 * @spring.property name="successView" value="loadExpressionExperimentProgress.html"
 * @spring.property name="simpleExpressionDataLoaderService" ref="simpleExpressionDataLoaderService"
 * @spring.property name="arrayDesignService" ref="arrayDesignService"
 * @spring.property name="taxonService" ref="taxonService"
 */
public class SimpleExpressionExperimentLoadController extends AbstractGemmaSpacesFormController {

    private static final String COOKIE_NAME = "simpleExpressionExperimentLoadCookie";

    TaxonService taxonService;

    ArrayDesignService arrayDesignService;

    SimpleExpressionDataLoaderService simpleExpressionDataLoaderService;

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
     * @param taxonService the taxonService to set
     */
    public void setTaxonService( TaxonService taxonService ) {
        this.taxonService = taxonService;
    }

    /**
     * @param request
     * @param adsac
     */
    private void loadCookie( HttpServletRequest request, SimpleExpressionExperimentMetaData command ) {

        // cookies aren't all that important, if they're missing we just go on.
        if ( request == null || request.getCookies() == null ) return;

        for ( Cookie cook : request.getCookies() ) {
            if ( cook.getName().equals( COOKIE_NAME ) ) {
                try {
                    ConfigurationCookie cookie = new ConfigurationCookie( cook );
                    TaxonPropertyEditor taxed = new TaxonPropertyEditor( taxonService );
                    taxed.setAsText( cookie.getString( "taxon" ) );
                    command.setTaxon( ( Taxon ) taxed.getValue() );

                    command.setType( StandardQuantitationType.fromString( cookie.getString( "type" ) ) );
                    command.setScale( ScaleType.fromString( cookie.getString( "scale" ) ) );
                    command.setIsRatio( Boolean.parseBoolean( cookie.getString( "isRatio" ) ) );
                    command.setQuantitationTypeName( cookie.getString( "quantitationTypeName" ) );
                    command.setQuantitationTypeDescription( cookie.getString( "quantitationTypeDescription" ) );

                } catch ( Exception e ) {
                    log.debug( "Cookie could not be loaded: " + e.getMessage() );
                    // that's okay, we just don't get a cookie.
                }
            }
        }
    }

    /**
     * @param mapping
     */
    @SuppressWarnings("unchecked")
    private void populateArrayDesignReferenceData( Map<String, List<? extends Object>> mapping ) {
        // FIXME replicated code. also in ExpressionExperimentLoad controller.
        // Need to factor out. Also need to remove AD's that are subsumed or merged into other AD's.

        List<ArrayDesign> arrayDesigns = new ArrayList<ArrayDesign>();
        for ( ArrayDesign arrayDesign : ( Collection<ArrayDesign> ) arrayDesignService.loadAll() ) {
            arrayDesigns.add( arrayDesign );
        }
        Collections.sort( arrayDesigns, new Comparator<ArrayDesign>() {
            public int compare( ArrayDesign o1, ArrayDesign o2 ) {
                return ( o1 ).getName().compareTo( ( o2 ).getName() );
            }
        } );
        mapping.put( "arrayDesigns", arrayDesigns );
    }

    /**
     * @param mapping
     */
    @SuppressWarnings("unchecked")
    private void populateTaxonReferenceData( Map<String, List<? extends Object>> mapping ) {
        List<Taxon> taxa = new ArrayList<Taxon>();
        for ( Taxon taxon : ( Collection<Taxon> ) taxonService.loadAll() ) {
            if ( !SupportedTaxa.contains( taxon ) ) {
                continue;
            }
            taxa.add( taxon );
        }
        Collections.sort( taxa, new Comparator<Taxon>() {
            public int compare( Taxon o1, Taxon o2 ) {
                return ( o1 ).getScientificName().compareTo( ( o2 ).getScientificName() );
            }
        } );
        mapping.put( "taxa", taxa );
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.web.servlet.mvc.AbstractFormController#formBackingObject(javax.servlet.http.HttpServletRequest)
     */
    @Override
    protected Object formBackingObject( HttpServletRequest request ) throws Exception {
        SimpleExpressionExperimentMetaData command = new SimpleExpressionExperimentLoadCommand();
        loadCookie( request, command );
        return command;
    }

    // /*
    // * (non-Javadoc)
    // *
    // * @see
    // ubic.gemma.web.controller.BackgroundProcessingFormController#getRunner(org.acegisecurity.context.SecurityContext,
    // * java.lang.Object, java.lang.String)
    // */
    // @Override
    // protected BackgroundControllerJob<ModelAndView> getRunner( String taskId, SecurityContext securityContext,
    // Object command, MessageUtil messenger ) {
    // BackgroundControllerJob job = new SimpleEELoadJob( null, this.simpleExpressionDataLoaderService );
    // job.setTaskId( taskId );
    // }

    class SimpleEELoadJob extends BackgroundControllerJob<ModelAndView> {
        SimpleExpressionDataLoaderService simpleExpressionDataLoaderService;

        public SimpleEELoadJob( String taskId, SecurityContext parentSecurityContext, Object commandObj,
                MessageUtil messenger, SimpleExpressionDataLoaderService simpleExpressionDataLoaderService ) {
            super( taskId, parentSecurityContext, commandObj, messenger );
            this.simpleExpressionDataLoaderService = simpleExpressionDataLoaderService;
        }

        @SuppressWarnings("synthetic-access")
        public ModelAndView call() throws Exception {
            SecurityContextHolder.setContext( securityContext );
            Map<Object, Object> model = new HashMap<Object, Object>();

            SimpleExpressionExperimentLoadCommand commandObject = ( SimpleExpressionExperimentLoadCommand ) command;

            FileUpload fileUpload = commandObject.getDataFile();

            Collection<ArrayDesign> arrayDesigns = commandObject.getArrayDesigns();
            Collection<Long> arrayDesignIds = commandObject.getArrayDesignIds(); // might have used instead of actual
            // ADs.
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
                // log.info( "Taxon " + commandObject.getTaxonName() + " is new, will create" );
                taxon = Taxon.Factory.newInstance();
                taxon.setScientificName( commandObject.getTaxonName() );
                commandObject.setTaxon( taxon );
            }

            ProgressJob job = ProgressManager.createProgressJob( this.getTaskId(), securityContext.getAuthentication()
                    .getName(), "Loading data from " + fileUpload.getLocalPath() );

            String filePath = fileUpload.getLocalPath();

            assert filePath != null;

            InputStream stream = FileTools.getInputStreamFromPlainOrCompressedFile( filePath );

            if ( stream == null ) {
                throw new IllegalStateException( "Could not read from file " + filePath );
            }

            ExpressionExperiment result = simpleExpressionDataLoaderService.load( commandObject, stream );
            stream.close();

            this.saveMessage( "Successfully loaded " + result );

            model.put( "expressionExperiment", result );

            ProgressManager.destroyProgressJob( job );

            // Forward to the details view for the new experiment.
            return new ModelAndView( new RedirectView( "/Gemma/expressionExperiment/showExpressionExperiment.html?id="
                    + result.getId() ), model );
        }
    }

    @Override
    protected void initBinder( HttpServletRequest request, ServletRequestDataBinder binder ) {
        super.initBinder( request, binder );
        binder.registerCustomEditor( ArrayDesign.class, new ArrayDesignPropertyEditor( this.arrayDesignService ) );
        binder.registerCustomEditor( Taxon.class, new TaxonPropertyEditor( this.taxonService ) );
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.web.servlet.mvc.SimpleFormController#onSubmit(javax.servlet.http.HttpServletRequest,
     *      javax.servlet.http.HttpServletResponse, java.lang.Object, org.springframework.validation.BindException)
     */
    @Override
    protected ModelAndView onSubmit( HttpServletRequest request, HttpServletResponse response, Object command,
            BindException errors ) throws Exception {

        /*
         * This needs to wait for the file upload to be finished.
         */
        String taskId = request.getParameter( "taskId" );
        log.info( "Upload task id : " + taskId );

        SimpleExpressionExperimentLoadCommand commandObject = ( SimpleExpressionExperimentLoadCommand ) command;
        Cookie cookie = new SimpleExpressionExperimentLoadCookie( commandObject );
        response.addCookie( cookie );

        FileUpload fileUpload = commandObject.getDataFile();

        if ( fileUpload == null || fileUpload.getFile() == null ) {
            errors.rejectValue( "dataFile", "errors.required", "Data file is required" );
            return showForm( request, response, errors );
        }

        File file = FileUploadUtil.copyUploadedFile( request, fileUpload, "dataFile.file" );

        if ( !file.canRead() ) {
            errors.rejectValue( "dataFile", "errors.required", "Data file was not uploaded successfully?" );
            return showForm( request, response, errors );
        }

        return startJob( commandObject );

    }

    /**
     * AJAX
     * 
     * @param ed
     * @return the taskid
     */
    public String load( SimpleExpressionExperimentLoadCommand ed ) throws Exception {

        FileUpload fileUpload = ed.getDataFile();

        if ( fileUpload == null || ( fileUpload.getFile() == null && StringUtils.isBlank( fileUpload.getLocalPath() ) ) ) {
            throw new IllegalArgumentException( "Must provide a file to upload" );
        }

        if ( StringUtils.isBlank( fileUpload.getLocalPath() ) ) {
            FileUploadUtil.copyUploadedFile( null, fileUpload, "dataFile.file" );
        }

        return ( String ) this.run( ed );
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.web.servlet.mvc.SimpleFormController#referenceData(javax.servlet.http.HttpServletRequest)
     */
    @SuppressWarnings("unused")
    @Override
    protected Map referenceData( HttpServletRequest request ) throws Exception {
        Map<String, List<? extends Object>> mapping = new HashMap<String, List<? extends Object>>();

        populateArrayDesignReferenceData( mapping );

        populateTaxonReferenceData( mapping );

        List<String> typesToList = new ArrayList<String>();
        typesToList.add( "AMOUNT" ); // for now this is the only thing that makes sense.
        mapping.put( "standardQuantitationTypes", typesToList );

        mapping.put( "scaleTypes", new ArrayList<String>( ScaleType.literals() ) );

        // in reality currently this has to be "QUANTITIATIVE" (we're not even listing this on the web interface)
        mapping.put( "generalQuantitationTypes", new ArrayList<String>( GeneralType.literals() ) );

        return mapping;
    }

    /**
     */
    class SimpleExpressionExperimentLoadCookie extends ConfigurationCookie {

        public SimpleExpressionExperimentLoadCookie( SimpleExpressionExperimentMetaData command ) {
            super( COOKIE_NAME );
            if ( command.getGeneralType() != null ) {
                this.setProperty( "generalType", command.getGeneralType().toString() );
            }
            if ( command.getTaxon() != null ) this.setProperty( "taxon", command.getTaxon().getScientificName() );

            if ( command.getType() != null ) {
                this.setProperty( "type", command.getType().toString() );
            }

            if ( command.getScale() != null ) {
                this.setProperty( "scale", command.getScale().toString() );
            }

            if ( command.getIsRatio() == true ) {
                this.setProperty( "isRatio", "true" );
            } else {
                this.setProperty( "isRatio", "false" );
            }

            if ( StringUtils.isNotBlank( command.getQuantitationTypeName() ) ) {
                this.setProperty( "quantitationTypeName", command.getQuantitationTypeName() );
            }

            if ( StringUtils.isNotBlank( command.getQuantitationTypeDescription() ) ) {
                this.setProperty( "quantitationTypeDescription", command.getQuantitationTypeDescription() );
            }

            this.setMaxAge( 100000 );
            this.setComment( "Information for the Simple Expression Experiment Loading form" );
        }

    }

    @Override
    protected BackgroundControllerJob<ModelAndView> getRunner( String jobId, SecurityContext securityContext,
            Object command, MessageUtil messenger ) {
        return new SimpleEELoadJob( jobId, securityContext, command, messenger, this.simpleExpressionDataLoaderService );
    }

    @Override
    protected BackgroundControllerJob<ModelAndView> getSpaceRunner( String jobId, SecurityContext securityContex,
            Object command, MessageUtil messenger ) {
        throw new UnsupportedOperationException( "Not implemented yet" );
    }

    @Override
    protected ModelAndView showForm( HttpServletRequest request, HttpServletResponse response, BindException errors,
            Map controlModel ) throws Exception {
        request.getSession().setAttribute( "tmpTaskId", TaskRunningService.generateTaskId() );
        return super.showForm( request, response, errors, controlModel );
    }

    @Override
    protected void setGemmaSpacesUtil( GemmaSpacesUtil gemmaSpacesUtil ) {
        this.injectGemmaSpacesUtil( gemmaSpacesUtil );
    }
}
