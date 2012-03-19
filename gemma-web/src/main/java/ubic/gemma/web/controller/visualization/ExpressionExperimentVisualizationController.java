package ubic.gemma.web.controller.visualization;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

/**
 * Note: do not use parameterized collections as parameters for ajax methods in this class! Type information is lost
 * during proxy creation so DWR can't figure out what type of collection the method should take. See bug 2756. Use
 * arrays instead.
 * 
 * @version $Id$
 */
public interface ExpressionExperimentVisualizationController {

    /**
     * @param request
     * @param response
     * @param errors
     * @return ModelAndView
     */
    @RequestMapping("/expressionExperiment/visualizeDataMatrix.html")
    public abstract ModelAndView show( HttpServletRequest request, HttpServletResponse response );

}