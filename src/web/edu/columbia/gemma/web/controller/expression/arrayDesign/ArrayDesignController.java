package edu.columbia.gemma.web.controller.expression.arrayDesign;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

import edu.columbia.gemma.expression.arrayDesign.ArrayDesignService;

/**
 * 
 * 
 *
 * <hr>
 * <p>Copyright (c) 2004 - 2005 Columbia University
 * @author keshav
 * @version $Id$
 * @spring.bean id="arrayDesignController" name="/arrayDesigns.htm"
 * @spring.property name="arrayDesignService" ref="arrayDesignService"
 */
public class ArrayDesignController implements Controller{
    private static Log log = LogFactory.getLog(ArrayDesignController.class);
    private ArrayDesignService arrayDesignService = null;
    
 
    /**
     * @return Returns the arrayDesignService.
     */
    public ArrayDesignService getArrayDesignService() {
        return arrayDesignService;
    }
    /**
     * @param arrayDesignService The arrayDesignService to set.
     */
    public void setArrayDesignService( ArrayDesignService arrayDesignService ) {
        this.arrayDesignService = arrayDesignService;
    }
    public ModelAndView handleRequest( HttpServletRequest request, HttpServletResponse response ) throws Exception {
        return new ModelAndView( "arrayDesign.GetAll.results.view", "arrayDesigns", arrayDesignService.getAllArrayDesigns());
    }
    
    
}
