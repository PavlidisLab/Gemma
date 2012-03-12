package ubic.gemma.web.controller.common.description.bibref;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import ubic.gemma.model.common.description.BibliographicReferenceValueObject;
import ubic.gemma.web.remote.JsonReaderResponse;
import ubic.gemma.web.remote.ListBatchCommand;

public interface BibliographicReferenceController {

    /**
     * Add or update a record.
     * 
     * @param request
     * @param response
     * @return
     */
    @RequestMapping("/bibRefAdd.html")
    public abstract ModelAndView add( HttpServletRequest request, HttpServletResponse response );

    /**
     * AJAX
     * 
     * @param batch
     * @return
     */
    public abstract JsonReaderResponse<BibliographicReferenceValueObject> browse( ListBatchCommand batch );

    /**
     * @param request
     * @param response
     * @return
     */
    @RequestMapping("/deleteBibRef.html")
    public abstract ModelAndView delete( HttpServletRequest request, HttpServletResponse response );

    /**
     * @param request
     * @param response
     * @return
     */
    @RequestMapping("/bibRefView.html")
    public abstract ModelAndView show( HttpServletRequest request, HttpServletResponse response );

    /**
     * @param request
     * @param response
     * @return
     */
    @RequestMapping("/showAllEeBibRefs.html")
    public abstract ModelAndView showAllForExperiments( HttpServletRequest request, HttpServletResponse response );

    /**
     * For AJAX calls. Refresh the Gemma entry based on information from PubMed.
     * 
     * @param id
     */
    public abstract void update( Long id );

}