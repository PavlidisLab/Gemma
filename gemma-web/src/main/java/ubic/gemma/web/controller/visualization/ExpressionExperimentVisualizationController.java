package ubic.gemma.web.controller.visualization;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

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