package edu.columbia.gemma.web.controller.expression.arrayDesign;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.validation.BindException;
import org.springframework.web.bind.RequestUtils;
import org.springframework.web.servlet.ModelAndView;

import edu.columbia.gemma.expression.arrayDesign.ArrayDesign;
import edu.columbia.gemma.expression.arrayDesign.ArrayDesignService;
import edu.columbia.gemma.web.controller.BaseFormController;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004 - 2006 University of British Columbia
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
    protected Object formBackingObject( HttpServletRequest request ) throws ServletException {

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
