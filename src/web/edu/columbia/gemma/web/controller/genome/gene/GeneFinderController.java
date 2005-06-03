package edu.columbia.gemma.web.controller.genome.gene;


import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.SimpleFormController;
import org.springframework.web.servlet.view.RedirectView;
import edu.columbia.gemma.genome.gene.GeneService;
import edu.columbia.gemma.genome.gene.CandidateGeneList;
import edu.columbia.gemma.genome.gene.CandidateGeneListService;
/**
 * <hr>
 * <p>
 * Copyright (c) 2004 - 2005 Columbia University
 * 
 * @author daq2101
 * @version $Id$
 * @spring.bean id="geneFinderController" name="/geneFinder.htm"
 * @spring.property name="formView" value="geneFinder"
 * @spring.property name="successView" value="geneFinder"
 * @spring.property name="geneService" ref="geneService"
 * @spring.property name="candidateGeneListService" ref="candidateGeneListService"
 */
public class GeneFinderController extends SimpleFormController {
    private GeneService geneService;
    public CandidateGeneListService candidateGeneListService;
    
    /**
     * @return Returns the bibliographicReferenceService.
     */
    public GeneService getGeneService() {
        return geneService;
    }
    /**
     * @param geneService The geneService to set.
     */
    public void setGeneService( GeneService geneService ) {
        this.geneService = geneService;
    }
    /**
     * @return Returns the candidateGeneListService.
     */
    public CandidateGeneListService getCandidateGeneListService() {
        return candidateGeneListService;
    }
    /**
     * @param geneService The geneService to set.
     */
    public void setCandidateGeneListService( CandidateGeneListService candidateGeneListService) {
        this.candidateGeneListService = candidateGeneListService;
    }    

    public ModelAndView onSubmit( HttpServletRequest request, HttpServletResponse response, Object command,
            BindException errors ) throws Exception {

        String view = "geneFinder";
        String act = request.getParameter("action");
        String searchType = request.getParameter("searchtype");
        String lookup = request.getParameter("lookup");
        String geneID = request.getParameter("geneID");
        String listID = request.getParameter("listID");
        if(act==null)
        	act="all";
        Map geneModel = new HashMap();
        
        if( searchType.compareTo("all")==0 ){
        	geneModel.put( "genes", this.getGeneService().getAllGenes());
        }
        if( searchType.compareTo("bySymbol")==0 ){
        	geneModel.put( "genes", this.getGeneService().findByOfficialSymbol(lookup));
        }
        if( searchType.compareTo("bySymbolInexact")==0 ){
        	lookup = "%" + lookup + "%";
        	geneModel.put( "genes", this.getGeneService().findByOfficialSymbolInexact(lookup));
        }
        if( searchType.compareTo("byName")==0 ){
        	geneModel.put( "genes", this.getGeneService().findByOfficialName(lookup));
        }
        
        return new ModelAndView( view, "model", geneModel );

    }

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
