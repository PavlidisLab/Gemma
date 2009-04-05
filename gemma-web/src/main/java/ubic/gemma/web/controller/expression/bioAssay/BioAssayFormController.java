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
package ubic.gemma.web.controller.expression.bioAssay;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.validation.BindException;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.servlet.ModelAndView;

import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.common.description.ExternalDatabaseService;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssay.BioAssayImpl;
import ubic.gemma.model.expression.bioAssay.BioAssayService;
import ubic.gemma.web.controller.BaseFormController;

/**
 * @author keshav
 * @version $Id$
 * @spring.bean id="bioAssayFormController"
 * @spring.property name = "commandName" value="bioAssayImpl"
 * @spring.property name = "formView" value="bioAssay.edit"
 * @spring.property name = "successView" value="redirect:/expressionExperiment/showAllExpressionExperiments.html"
 * @spring.property name = "bioAssayService" ref="bioAssayService"
 * @spring.property name = "externalDatabaseService" ref="externalDatabaseService"
 * @spring.property name = "validator" ref="bioAssayValidator"
 */
public class BioAssayFormController extends BaseFormController {

    private Log log = LogFactory.getLog( this.getClass() );

    BioAssayService bioAssayService = null;

    ExternalDatabaseService externalDatabaseService = null;

    /**
     * 
     *
     */
    public BioAssayFormController() {
        /* if true, reuses the same command object across the edit-submit-process (get-post-process). */
        setSessionForm( true );
        setCommandClass( BioAssayImpl.class );
    }

    /**
     * @param request
     * @return Object
     * @throws ServletException
     * @see org.springframework.web.servlet.mvc.AbstractFormController#formBackingObject(javax.servlet.http.HttpServletRequest)
     */
    @Override
    protected Object formBackingObject( HttpServletRequest request ) {
        BioAssay ba = null;

        log.debug( "entering formBackingObject" );

        String id_param = ServletRequestUtils.getStringParameter( request, "id", "" );

        Long id = Long.parseLong( id_param );

        if ( !id.equals( null ) )
            ba = bioAssayService.load( id );
        else
            ba = BioAssay.Factory.newInstance();

        return ba;
    }

    /**
     * @param request
     * @return Map
     */
    @Override
    @SuppressWarnings( { "unchecked", "unused" })
    protected Map<String, Collection<ExternalDatabase>> referenceData( HttpServletRequest request ) {
        Collection<ExternalDatabase> edCol = externalDatabaseService.loadAll();
        Map<String, Collection<ExternalDatabase>> edMap = new HashMap<String, Collection<ExternalDatabase>>();
        edMap.put( "externalDatabases", edCol );
        return edMap;
    }

    /**
     * @param request
     * @param response
     * @param command
     * @param errors
     * @return ModelAndView
     * @throws Exception
     * @see org.springframework.web.servlet.mvc.AbstractFormController#processFormSubmission(javax.servlet.http.HttpServletRequest,
     *      javax.servlet.http.HttpServletResponse, java.lang.Object, org.springframework.validation.BindException)
     */
    @Override
    public ModelAndView processFormSubmission( HttpServletRequest request, HttpServletResponse response,
            Object command, BindException errors ) throws Exception {

        log.debug( "entering processFormSubmission" );

        String accession = request.getParameter( "bioAssayImpl.accession.accession" );

        if ( accession == null ) {
            // do nothing
        } else {
            if ( request.getParameter( "cancel" ) != null ) {
                /*
                 * return new ModelAndView( new RedirectView( "http://" + request.getServerName() + ":" +
                 * request.getServerPort() + request.getContextPath() + "/bioAssay/showBioAssay.html?id=" +
                 * id.toString() ) );
                 */
                return new ModelAndView( "bioAssay.detail" ).addObject( "bioAssay", command );
            }

            /* database entry */
            ( ( BioAssay ) command ).getAccession().setAccession( accession );

            /* external database */
            ExternalDatabase ed = ( ( ( BioAssay ) command ).getAccession().getExternalDatabase() );
            ed = externalDatabaseService.findOrCreate( ed );
            ( ( BioAssay ) command ).getAccession().setExternalDatabase( ed );
        }
        return super.processFormSubmission( request, response, command, errors );
    }

    /**
     * @param request
     * @param response
     * @param command
     * @param errors
     * @return ModelAndView
     * @throws Exception
     */
    @Override
    public ModelAndView onSubmit( HttpServletRequest request, HttpServletResponse response, Object command,
            BindException errors ) throws Exception {

        log.debug( "entering onSubmit" );

        BioAssay ba = ( BioAssay ) command;
        bioAssayService.update( ba );

        saveMessage( request, "object.saved", new Object[] { ba.getClass().getSimpleName(), ba.getId() }, "Saved" );

        return new ModelAndView( getSuccessView() );
    }

    /**
     * @param bioAssayService
     */
    public void setBioAssayService( BioAssayService bioAssayService ) {
        this.bioAssayService = bioAssayService;
    }

    /**
     * @param externalDatabaseDao
     */
    public void setExternalDatabaseService( ExternalDatabaseService externalDatabaseService ) {
        this.externalDatabaseService = externalDatabaseService;
    }

}
