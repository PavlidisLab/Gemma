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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ubic.gemma.model.common.auditAndSecurity.ContactService;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.common.description.ExternalDatabaseDao;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.web.controller.BaseFormController;

/**
 * @author keshav
 * @version $Id$
 * @spring.bean id="expressionExperimentFormController"
 * @spring.property name = "commandName" value="expressionExperiment"
 * @spring.property name = "formView" value="expressionExperiment.edit"
 * @spring.property name = "successView" value="redirect:/expressionExperiment/showAllExpressionExperiments.html"
 * @spring.property name = "expressionExperimentService" ref="expressionExperimentService"
 * @spring.property name = "contactService" ref="contactService"
 * @spring.property name = "externalDatabaseDao" ref="externalDatabaseDao"
 * @spring.property name = "validator" ref="expressionExperimentValidator"
 */
public class ExpressionExperimentFormController extends BaseFormController {
    private static Log log = LogFactory.getLog( ExpressionExperimentFormController.class.getName() );

    ExpressionExperimentService expressionExperimentService = null;
    ContactService contactService = null;

    private Long id = null;

    private ExternalDatabaseDao externalDatabaseDao = null;

    // FIXME Use ExternalDatabaseService instead of ExternalDatabaseDao. Methods have been put in model for service
    // but when I use them I get NonUniqueObjectException. This seems to be documented here:
    // http://saloon.javaranch.com/cgi-bin/ubb/ultimatebb.cgi?ubb=get_topic&f=78&t=000475.
    // It works if you call the dao layer directly from your controller (I know we are not supposed to do this, but it
    // works).
    // Will fix this later.

    public ExpressionExperimentFormController() {
        /*
         * if true, reuses the same command object across the edit-submit-process (get-post-process).
         */
        setSessionForm( true );
        setCommandClass( ExpressionExperiment.class );
    }

    /**
     * @param request
     * @return Object
     * @throws ServletException
     */
    @Override
    protected Object formBackingObject( HttpServletRequest request ) {

        id = Long.parseLong( request.getParameter( "id" ) );

        ExpressionExperiment ee = null;

        log.debug( id );

        if ( !"".equals( id ) )
            ee = expressionExperimentService.findById( id );

        else
            ee = ExpressionExperiment.Factory.newInstance();

        saveMessage( request, "object.editing", new Object[] { ee.getClass().getSimpleName(), ee.getId() }, "Editing" );

        return ee;
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
    public ModelAndView processFormSubmission( HttpServletRequest request, HttpServletResponse response,
            Object command, BindException errors ) throws Exception {

        log.debug( "entering processFormSubmission" );

        id = ( ( ExpressionExperiment ) command ).getId();

        if ( request.getParameter( "cancel" ) != null ) {
            if ( id != null ) {
                return new ModelAndView( new RedirectView( "http://" + request.getServerName() + ":"
                        + request.getServerPort() + request.getContextPath()
                        + "/expressionExperiment/showExpressionExperiment.html?id=" + id ) );
            }

            log.warn( "Cannot find details view due to null id.  Redirecting to overview" );
            return new ModelAndView( new RedirectView( "http://" + request.getServerName() + ":"
                    + request.getServerPort() + request.getContextPath()
                    + "/expressionExperiment/showAllExpressionExperiments.html" ) );
        }

        String accession = request.getParameter( "expressionExperiment.accession.accession" );

        if ( accession == null ) {
            // do nothing
        } else {
            /* database entry */
            ( ( ExpressionExperiment ) command ).getAccession().setAccession( accession );

            /* external database */
            ExternalDatabase ed = ( ( ( ExpressionExperiment ) command ).getAccession().getExternalDatabase() );
            ed = externalDatabaseDao.findOrCreate( ed );
            ( ( ExpressionExperiment ) command ).getAccession().setExternalDatabase( ed );
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
    @SuppressWarnings("unused")
    public ModelAndView onSubmit( HttpServletRequest request, HttpServletResponse response, Object command,
            BindException errors ) throws Exception {

        log.debug( "entering onSubmit" );

        ExpressionExperiment ee = ( ExpressionExperiment ) command;

        expressionExperimentService.update( ee );

        saveMessage( request, "object.saved", new Object[] { ee.getClass().getSimpleName(), ee.getId() }, "Saved" );

        return new ModelAndView( getSuccessView() );
    }

    /**
     * @param request
     * @return Map
     */
    @Override
    @SuppressWarnings( { "unused", "unchecked" })
    protected Map referenceData( HttpServletRequest request ) {
        Collection<ExternalDatabase> edCol = externalDatabaseDao.loadAll();
        Map<String, Collection<ExternalDatabase>> edMap = new HashMap<String, Collection<ExternalDatabase>>();
        edMap.put( "externalDatabases", edCol );
        return edMap;
    }

    /**
     * @param expressionExperimentService
     */
    public void setExpressionExperimentService( ExpressionExperimentService expressionExperimentService ) {
        this.expressionExperimentService = expressionExperimentService;
    }

    /**
     * @param contactService
     */
    public void setContactService( ContactService contactService ) {
        this.contactService = contactService;
    }

    /**
     * @param externalDatabaseDao
     */
    public void setExternalDatabaseDao( ExternalDatabaseDao externalDatabaseDao ) {
        this.externalDatabaseDao = externalDatabaseDao;
    }
}
