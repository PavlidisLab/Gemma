package edu.columbia.gemma.web.controller.expression.arrayDesign;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.bind.RequestUtils;

import edu.columbia.gemma.expression.arrayDesign.ArrayDesign;
import edu.columbia.gemma.expression.arrayDesign.ArrayDesignService;
import edu.columbia.gemma.web.controller.BaseFormController;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004 - 2005 Columbia University
 * 
 * @author keshav
 * @version $Id$
 * @spring.bean id="arrayDesignFormController" name="arrayDesign/editArrayDesign.html"
 * @spring.property name = "commandName" value="arrayDesign"
 * @spring.property name = "formView" value="arrayDesign.edit"
 * @spring.property name = "successView" value="redirect:arrayDesign/arrayDesignDetail.html"
 * @spring.property name = "arrayDesignService" ref="arrayDesignService"
 */
public class ArrayDesignFormController extends BaseFormController {
    private static Log log = LogFactory.getLog( ArrayDesignFormController.class.getName() );

    ArrayDesignService arrayDesignService = null;

    public ArrayDesignFormController() {
        /* allows me to reuse the command object across the edit-submit-process. */
        setSessionForm( true );
        setCommandClass( ArrayDesign.class );
    }

    protected Object formBackingObject( HttpServletRequest request ) throws ServletException {

        String name = RequestUtils.getStringParameter( request, "name", "" );
        
        log.debug(name);
        
        if ( !"".equals( name ) ) return arrayDesignService.findArrayDesignByName( name );

        return ArrayDesign.Factory.newInstance();
    }

    /**
     * @param arrayDesignService The arrayDesignService to set.
     */
    public void setArrayDesignService( ArrayDesignService arrayDesignService ) {
        this.arrayDesignService = arrayDesignService;
    }

}
