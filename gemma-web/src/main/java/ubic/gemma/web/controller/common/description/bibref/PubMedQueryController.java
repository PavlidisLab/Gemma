package ubic.gemma.web.controller.common.description.bibref;

import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;

/**
 * Note: do not use parameterized collections as parameters for ajax methods in this class! Type information is lost
 * during proxy creation so DWR can't figure out what type of collection the method should take. See bug 2756. Use
 * arrays instead.
 */
@Controller("/bibRefSearch.html")
public interface PubMedQueryController {

    @RequestMapping(method = RequestMethod.GET)
    String getView();

    @RequestMapping(method = RequestMethod.POST)
    ModelAndView onSubmit( HttpServletRequest request, PubMedSearchCommand command, BindingResult result,
            SessionStatus status ) throws Exception;

}