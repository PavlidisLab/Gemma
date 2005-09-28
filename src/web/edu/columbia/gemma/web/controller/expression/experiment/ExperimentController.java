/*
 * The Gemma project
 * 
 * Copyright (c) 2005 Columbia University
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
package edu.columbia.gemma.web.controller.expression.experiment;

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
import org.springframework.web.servlet.mvc.SimpleFormController;

import edu.columbia.gemma.common.auditAndSecurity.Contact;
import edu.columbia.gemma.common.auditAndSecurity.Person;
import edu.columbia.gemma.common.auditAndSecurity.User;
import edu.columbia.gemma.common.auditAndSecurity.UserService;
import edu.columbia.gemma.common.description.BibliographicReference;
import edu.columbia.gemma.common.description.BibliographicReferenceService;
import edu.columbia.gemma.expression.experiment.ExpressionExperiment;
import edu.columbia.gemma.expression.experiment.ExpressionExperimentService;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004 - 2005 Columbia University
 * 
 * @author daq2101
 * @version $Id$
 * @spring.bean id="experimentController" name="/ExperimentDetail.html /ExperimentList.html"
 * @spring.property name="expressionExperimentService" ref="expressionExperimentService"
 * @spring.property name="userService" ref="userService"
 * @spring.property name="bibliographicReferenceService" ref="bibliographicReferenceService"
 */
public class ExperimentController extends SimpleFormController {

    private static Log log = LogFactory.getLog( ExperimentController.class.getName() );

    private ExpressionExperimentService expressionExperimentService = null;
    private UserService userService = null;
    private BibliographicReferenceService bibliographicReferenceService = null;

    /**
     * @return Returns the ExpressionExperimentService.
     */
    public ExpressionExperimentService getExpressionExperimentService() {
        return expressionExperimentService;
    }

    /**
     * @param ExpressionExperimentService The ExpressionExperimentService to set.
     */
    public void setExpressionExperimentService( ExpressionExperimentService expressionExperimentService ) {
        this.expressionExperimentService = expressionExperimentService;
    }

    /**
     * @param UserService The UserService to set.
     */
    public UserService getUserService() {
        return userService;
    }

    /**
     * @param userService The UserService to set.
     */
    public void setUserService( UserService userService ) {
        this.userService = userService;
    }

    /**
     * @return Returns the bibliographicReferenceService.
     */
    public BibliographicReferenceService getBibliographicReferenceService() {
        return bibliographicReferenceService;
    }

    /**
     * @param bibliographicReferenceService The bibliographicReferenceService to set.
     */
    public void setBibliographicReferenceService( BibliographicReferenceService bibliographicReferenceService ) {
        this.bibliographicReferenceService = bibliographicReferenceService;
    }

    @Override
    @SuppressWarnings( { "unused", "unchecked" })
    public ModelAndView onSubmit( HttpServletRequest request, HttpServletResponse response, Object command,
            BindException errors ) throws Exception {

        Map<String, Object> experimentModel = new HashMap<String, Object>();
        long experimentID = 0;
        String action = request.getParameter( "action" );
        if ( request.getParameter( "experimentID" ) != null && request.getParameter( "experimentID" ).length() > 0 ) {
            experimentID = new Long( request.getParameter( "experimentID" ) ).longValue();
        }
        String view = "ExperimentList";
        if ( action == null ) action = "view";

        if ( action.equals( "view" ) ) {
            if ( experimentID > 0 ) {
                experimentModel.put( "experiments", this.getExpressionExperimentService().findById( experimentID ) );
                view = "ExperimentDetail";
            } else {
                experimentModel
                        .put( "experiments", this.getExpressionExperimentService().getAllExpressionExperiments() );
            }
        } else if ( action.equals( "setPI" ) ) {
            ExpressionExperiment ee = this.getExpressionExperimentService().findById( experimentID );
            String UserName = request.getParameter( "username" );
            User u = this.getUserService().getUser( UserName );
            if ( u == null ) {
                errors.reject( "ExperimentController", "User Not Found." ); // Spring MVC error handling
            } else {
                ee.setOwner( u );
                this.expressionExperimentService.saveExpressionExperiment( ee );
            }
            experimentModel.put( "experiments", this.getExpressionExperimentService().findById( experimentID ) );
            view = "ExperimentDetail";
        } else if ( action.equals( "add" ) ) {
            String eName = request.getParameter( "newName" );
            ExpressionExperiment ee = ExpressionExperiment.Factory.newInstance();
            ee.setName( eName );
            User usr = userService.getUser( request.getRemoteUser() );
            ee.setOwner( usr );
            this.getExpressionExperimentService().createExpressionExperiment( ee );
            experimentModel.put( "experiments", this.getExpressionExperimentService().getAllExpressionExperiments() );
        } else if ( action.equals( "delete" ) ) {
            ExpressionExperiment ee = this.getExpressionExperimentService().findById( experimentID );
            this.getExpressionExperimentService().removeExpressionExperiment( ee );
            experimentModel.put( "experiments", this.getExpressionExperimentService().getAllExpressionExperiments() );
        } else if ( action.equals( "removeParticipant" ) ) {
            ExpressionExperiment ee = this.getExpressionExperimentService().findById( experimentID );
            long userID = new Long( request.getParameter( "userID" ) ).longValue();
            Person p = null;
            Person pNuke = null;
            java.util.Collection par = ee.getInvestigators();
            for ( java.util.Iterator iter = par.iterator(); iter.hasNext(); ) {
                p = ( Person ) iter.next();
                if ( p.getId().longValue() == userID ) pNuke = p;
            }
            if ( pNuke != null ) {
                par.remove( pNuke );
                this.expressionExperimentService.saveExpressionExperiment( ee );
            }
            experimentModel.put( "experiments", this.getExpressionExperimentService().findById( experimentID ) );
            view = "ExperimentDetail";
        } else if ( action.equals( "addParticipant" ) ) {
            ExpressionExperiment ee = this.getExpressionExperimentService().findById( experimentID );
            String UserName = request.getParameter( "username" );
            User u = this.getUserService().getUser( UserName );
            if ( u == null ) {
                experimentModel.put( "error", "User Not Found" );
            } else {
                Collection<Contact> par = ee.getInvestigators();
                if ( par.contains( u ) ) {
                    errors.reject( "ExperimentController", "The user " + UserName + " is already an investigator." );
                } else {
                    par.add( u );
                    this.expressionExperimentService.saveExpressionExperiment( ee );
                }
            }
            experimentModel.put( "experiments", this.getExpressionExperimentService().findById( experimentID ) );
            view = "ExperimentDetail";

        } else if ( action.equals( "update" ) ) {
            ExpressionExperiment ee = this.getExpressionExperimentService().findById( experimentID );
            ee.setName( request.getParameter( "eName" ).toString() );
            ee.setDescription( request.getParameter( "eDesc" ).toString() );
            String pp = request.getParameter( "primaryPubmed" ).toString();
            try {
                Integer pi = new Integer( pp );
                pp = pi.toString();
            } catch ( java.lang.NumberFormatException nf ) {
                pp = null;
            }
            if ( pp != null ) {
                BibliographicReference br = this.getBibliographicReferenceService().findByExternalId( pp, "PUBMED" );
                if ( br == null ) {
                    br = this.getBibliographicReferenceService().saveBibliographicReferenceByLookup( pp, "PUBMED" );
                }
                ee.setPrimaryPublication( br );
            }

            this.getExpressionExperimentService().saveExpressionExperiment( ee );
            experimentModel.put( "experiments", this.getExpressionExperimentService().findById( experimentID ) );
            view = "ExperimentDetail";
        }

        // default: put all experiments into model

        return new ModelAndView( view, "model", experimentModel );
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.web.servlet.mvc.AbstractFormController#formBackingObject(javax.servlet.http.HttpServletRequest)
     */
    @Override
    protected Object formBackingObject( HttpServletRequest request ) throws Exception {
        log.debug( "Entering formBackingObject" );
        if ( request.getParameter( "experimentID" ) != null && request.getParameter( "experimentID" ).length() > 0 ) {
            long experimentID = new Long( request.getParameter( "experimentID" ) ).longValue();
            return expressionExperimentService.findById( experimentID );
        }
        // FIXME we need to separate out the list all functionality to a new controller.
        return ExpressionExperiment.Factory.newInstance();

    }

}