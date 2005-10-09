package edu.columbia.gemma.web.controller.expression.experiment;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

import edu.columbia.gemma.expression.experiment.ExpressionExperimentService;
import edu.columbia.gemma.web.controller.common.description.BibliographicReferenceController;

/**
 * 
 * 
 *
 * <hr>
 * <p>Copyright (c) 2004 - 2005 Columbia University
 * @author keshav
 * @author daq2101
 * @version $Id$
 * 
 * @spring.bean id="expressionExperimentController" name="/expressionExperiments.htm"
 * @spring.property name = "expressionExperimentService" ref="expressionExperimentService"
 */
public class ExpressionExperimentController implements Controller{
    
    private static Log log = LogFactory.getLog( BibliographicReferenceController.class.getName() );

    private ExpressionExperimentService expressionExperimentService = null;
    
    /**
     * @param request
     * @param response
     * @return ModelAndView - view, name of object in model, the object itself.
     * @throws Exception
     */
    public ModelAndView handleRequest( HttpServletRequest request, HttpServletResponse response ) throws Exception {
        if ( log.isDebugEnabled() )
            log.debug( "entered 'handleRequest'" + " HttpServletRequest: " + request + " HttpServletResponse: "
                    + response );
        
        log.info(expressionExperimentService.getAllExpressionExperiments().size());
        return new ModelAndView("expressionExperiment.GetAll.results.view", "expressionExperiments",
                expressionExperimentService.getAllExpressionExperiments());
    }

    /**
     * @param expressionExperimentService The expressionExperimentService to set.
     */
    public void setExpressionExperimentService( ExpressionExperimentService expressionExperimentService ) {
        this.expressionExperimentService = expressionExperimentService;
    }

}
