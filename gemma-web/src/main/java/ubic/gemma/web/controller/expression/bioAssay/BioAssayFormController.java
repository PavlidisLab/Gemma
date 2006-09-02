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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.validation.BindException;
import org.springframework.web.bind.RequestUtils;
import org.springframework.web.servlet.ModelAndView;

import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.common.description.ExternalDatabaseDao;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssay.BioAssayImpl;
import ubic.gemma.model.expression.bioAssay.BioAssayService;
import ubic.gemma.web.controller.BaseFormController;
import ubic.gemma.web.validation.expression.bioAssay.BioAssayValidator;

/**
 * @author keshav
 * @version $Id$
 * @spring.bean id="bioAssayFormController" name="/bioAssay/editBioAssay.html"
 * @spring.property name = "commandName" value="bioAssayImpl"
 * @spring.property name = "formView" value="bioAssay.edit"
 * @spring.property name = "successView" value="redirect:/expressionExperiment/showAllExpressionExperiments.html"
 * @spring.property name = "bioAssayService" ref="bioAssayService"
 * @spring.property name = "externalDatabaseDao" ref="externalDatabaseDao"
 * @spring.property name = "validator" ref="bioAssayValidator"
 */
public class BioAssayFormController extends BaseFormController {

    private Log log = LogFactory.getLog( this.getClass() );
    private final String messagePrefix = "Bioassay with id ";

    BioAssayService bioAssayService = null;

    ExternalDatabaseDao externalDatabaseDao = null; // FIXME Use Service. Methods have been put in model for

    // service
    // but when I use them I get NonUniqueObjectException. This seems to be documented here:
    // http://saloon.javaranch.com/cgi-bin/ubb/ultimatebb.cgi?ubb=get_topic&f=78&t=000475.
    // It works if you call the dao layer directly from your controller (I know we are not supposed to do this, but it
    // works).
    // Will fix this later.

    BioAssayValidator bioAssayValidator = null;

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
    protected Object formBackingObject( HttpServletRequest request ) {
        BioAssay ba = null;

        log.debug( "entering formBackingObject" );

        String id_param = RequestUtils.getStringParameter( request, "id", "" );

        Long id = Long.parseLong( id_param );

        if ( !id.equals( null ) )
            ba = bioAssayService.findById( id );
        else
            ba = BioAssay.Factory.newInstance();

        saveMessage( request, getText( "object.editing", new Object[] { messagePrefix, ba.getId() }, request
                .getLocale() ) );

        return ba;
    }

    /**
     * @param request
     * @return Map
     */
    @SuppressWarnings("unchecked")
    protected Map referenceData( HttpServletRequest request ) {
        Collection<ExternalDatabase> edCol = externalDatabaseDao.loadAll();
        Map edMap = new HashMap();
        edMap.put( "externalDatabases", edCol );// FIXME - parameterize the map
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
            ed = externalDatabaseDao.findOrCreate( ed );
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
    @SuppressWarnings("unused")
    public ModelAndView onSubmit( HttpServletRequest request, HttpServletResponse response, Object command,
            BindException errors ) throws Exception {

        log.debug( "entering onSubmit" );

        BioAssay ba = ( BioAssay ) command;
        bioAssayService.update( ba );

        saveMessage( request, getText( "object.saved", new Object[] { messagePrefix, ba.getId() }, request.getLocale() ) );

        return new ModelAndView( getSuccessView() );
    }

    /**
     * @param bioAssayService
     */
    public void setBioAssayService( BioAssayService bioAssayService ) {
        this.bioAssayService = bioAssayService;
    }

    /**
     * @param bioAssayValidator
     */
    public void setBioAssayValidator( BioAssayValidator bioAssayValidator ) {
        this.bioAssayValidator = bioAssayValidator;
    }

    /**
     * @param externalDatabaseDao
     */
    public void setExternalDatabaseDao( ExternalDatabaseDao externalDatabaseDao ) {
        this.externalDatabaseDao = externalDatabaseDao;
    }

}
