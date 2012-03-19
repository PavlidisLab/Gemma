package ubic.gemma.web.controller.common.description.bibref;

import javax.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.servlet.ModelAndView;

@Controller("/bibRefSearch.html")
public interface PubMedQueryController {

    @RequestMapping(method = RequestMethod.GET)
    public abstract String getView();

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.web.servlet.mvc.SimpleFormController#onSubmit(javax.servlet.http.HttpServletRequest,
     * javax.servlet.http.HttpServletResponse, java.lang.Object, org.springframework.validation.BindException)
     */
    @RequestMapping(method = RequestMethod.POST)
    public abstract ModelAndView onSubmit( HttpServletRequest request, PubMedSearchCommand command,
            BindingResult result, SessionStatus status ) throws Exception;

}