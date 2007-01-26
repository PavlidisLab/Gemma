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
package ubic.gemma.web.controller.security;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;

import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.security.SecurityService;
import ubic.gemma.util.progress.ProgressJob;
import ubic.gemma.util.progress.ProgressManager;
import ubic.gemma.web.controller.BaseFormController;

/**
 * Manages data-level security (ie. can make data private).
 * 
 * @author keshav
 * @version $Id$
 * @spring.bean id="securityFormController"
 * @spring.property name="securityService" ref="securityService"
 * @spring.property name="expressionExperimentService" ref="expressionExperimentService"
 * @spring.property name="arrayDesignService" ref="arrayDesignService"
 * @spring.property name="formView" value="securityManager"
 * @spring.property name="successView" value="securityManager"
 */
public class SecurityFormController extends BaseFormController {

    private SecurityService securityService = null;
    private ExpressionExperimentService expressionExperimentService = null;
    private ArrayDesignService arrayDesignService = null;

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.web.servlet.mvc.SimpleFormController#onSubmit(javax.servlet.http.HttpServletRequest,
     *      javax.servlet.http.HttpServletResponse, java.lang.Object, org.springframework.validation.BindException)
     */
    @Override
    @SuppressWarnings("unused")
    protected ModelAndView onSubmit( HttpServletRequest request, HttpServletResponse response, Object command,
            BindException error ) throws Exception {

        ProgressJob job = ProgressManager.createProgressJob( null, request.getRemoteUser(), "Making data private." );

        // TODO add in the command object
        // Object target = command.getTarget();
        // securityService.makePrivate( target );

        ProgressManager.destroyProgressJob( job );

        ModelAndView mav = new ModelAndView( getSuccessView() );
        // mav.addObject( "securityManager.detail", securityCommand );
        return mav;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.web.servlet.mvc.AbstractFormController#formBackingObject(javax.servlet.http.HttpServletRequest)
     */
    @Override
    @SuppressWarnings("unused")
    protected Object formBackingObject( HttpServletRequest request ) throws Exception {
        // SecurityCommand securityCommand = new SecurityCommand();
        // request.getParameter( "id" );
        // Object target = securityCommand.setTarget();
        return null;
    }

    /**
     * @param securityService
     */
    public void setSecurityService( SecurityService securityService ) {
        this.securityService = securityService;
    }

    /**
     * @param expressionExperimentService
     */
    public void setExpressionExperimentService( ExpressionExperimentService expressionExperimentService ) {
        this.expressionExperimentService = expressionExperimentService;
    }

    /**
     * @param arrayDesignService
     */
    public void setArrayDesignService( ArrayDesignService arrayDesignService ) {
        this.arrayDesignService = arrayDesignService;
    }

}
