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
package ubic.gemma.web.controller.expression.arrayDesign;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.validation.BindException;
import org.springframework.web.bind.RequestUtils;
import org.springframework.web.servlet.ModelAndView;

import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.web.controller.BaseFormController;

/**
 * <hr>
 * <p>
 * 
 * @author keshav
 * @version $Id$
 * @spring.bean id="arrayDesignFormController" name="/arrayDesign/editArrayDesign.html"
 * @spring.property name = "commandName" value="arrayDesign"
 * @spring.property name = "formView" value="arrayDesign.edit"
 * @spring.property name = "successView" value="redirect:/arrayDesign/showAllArrayDesigns.html"
 * @spring.property name = "arrayDesignService" ref="arrayDesignService"
 */
public class ArrayDesignFormController extends BaseFormController {
    private static Log log = LogFactory.getLog( ArrayDesignFormController.class.getName() );

    ArrayDesignService arrayDesignService = null;

    public ArrayDesignFormController() {
        /* if true, reuses the same command object across the edit-submit-process (get-post-process). */
        setSessionForm( true );
        setCommandClass( ArrayDesign.class );
    }

    /**
     * Case = GET: Step 1 - return instance of command class (from database). This is not called in the POST case
     * because the sessionForm is set to 'true' in the constructor. This means the command object was already bound to
     * the session in the GET case.
     * 
     * @param request
     * @return Object
     * @throws ServletException
     */
    protected Object formBackingObject( HttpServletRequest request ) {

        String name = RequestUtils.getStringParameter( request, "name", "" );

        log.debug( name );

        if ( !"".equals( name ) ) return arrayDesignService.findArrayDesignByName( name );

        return ArrayDesign.Factory.newInstance();
    }

    /**
     * Case = POST: Step 5 - Used to process the form action (ie. clicking on the 'save' button or the 'cancel' button.
     * 
     * @param request
     * @param response
     * @param command
     * @param errors
     * @return ModelAndView
     * @throws Exception
     */
    public ModelAndView processFormSubmission( HttpServletRequest request, HttpServletResponse response,
            Object command, BindException errors ) throws Exception {

        log.debug( "entering processFormSubmission" );

        return super.processFormSubmission( request, response, command, errors );
    }

    /**
     * Case = POST: Step 5 - Custom logic is here. For instance, this is where you would actually save or delete the
     * object.
     * 
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

        ArrayDesign ad = ( ArrayDesign ) command;
        arrayDesignService.update( ad );

        saveMessage( request, getText( "arrayDesign.saved", new Object[] { ad.getName() }, request.getLocale() ) );

        return new ModelAndView( getSuccessView() );
    }

    /**
     * @param arrayDesignService The arrayDesignService to set.
     */
    public void setArrayDesignService( ArrayDesignService arrayDesignService ) {
        this.arrayDesignService = arrayDesignService;
    }

}
