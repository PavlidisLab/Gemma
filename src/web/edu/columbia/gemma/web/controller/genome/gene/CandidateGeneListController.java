package edu.columbia.gemma.web.controller.genome.gene;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.ModelAndView;
//import org.springframework.web.servlet.mvc.SimpleFormController;
import org.springframework.web.servlet.mvc.BaseCommandController;
import edu.columbia.gemma.genome.gene.CandidateGeneListService;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004 - 2005 Columbia University
 * 
 * @author daq2101
 * @version $Id$
 * @spring.bean id="candidateGeneListController" name="/candidateGeneList.htm"
 * @spring.property name="candidateGeneListService" ref="candidateGeneListService"
 */
public class CandidateGeneListController extends BaseCommandController {
    private CandidateGeneListService candidateGeneListService;

    /**
     * @return Returns the candidateGeneListService.
     */
    public CandidateGeneListService getCandidateGeneListService() {
        return candidateGeneListService;
    }

    /**
     * @param candidateGeneListService The candidateGeneListService to set.
     */
    public void setCandidateGeneListService( CandidateGeneListService candidateGeneListService) {
        this.candidateGeneListService = candidateGeneListService;
    }
    public ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response)
    throws Exception{
    	String view = "candidateGeneList";
        Map candidateGeneListModel = new HashMap();
        candidateGeneListModel.put("candidateGeneLists", this.getCandidateGeneListService().getAll());
        return new ModelAndView( view, "model", candidateGeneListModel );
    	
    }
/*
    public ModelAndView onSubmit( HttpServletRequest request, HttpServletResponse response, Object command,
            BindException errors ) throws Exception {
        String view = "candidateGeneListForm";
        Map candidateGeneListModel = new HashMap();
        candidateGeneListModel.put("candidateGeneLists", this.getCandidateGeneListService().getAll());
        return new ModelAndView( view, "model", candidateGeneListModel );

    }
*/
    /**
     * This is needed or you will have to specify a commandClass in the DispatcherServlet's context
     * 
     * @param request
     * @return Object
     * @throws Exception
     */
    protected Object formBackingObject( HttpServletRequest request ) throws Exception {
        return request;
    }
}
