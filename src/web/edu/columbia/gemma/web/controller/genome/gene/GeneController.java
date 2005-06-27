package edu.columbia.gemma.web.controller.genome.gene;


import java.util.HashMap;
import java.util.Map;
import java.util.Collection;
import java.util.Iterator;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.BaseCommandController;
import org.springframework.web.servlet.view.RedirectView;

import edu.columbia.gemma.genome.gene.GeneService;
import edu.columbia.gemma.common.description.BibliographicReference;
import edu.columbia.gemma.genome.Gene;
import edu.columbia.gemma.common.description.BibliographicReferenceService;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004 - 2005 Columbia University
 * 
 * @author daq2101
 * @version $Id$
 * @spring.bean id="geneController" name="/geneDetail.htm"
 * @spring.property name="geneService" ref="geneService"
 * @spring.property name="bibliographicReferenceService" ref="bibliographicReferenceService"
 */
public class GeneController extends BaseCommandController {
	private GeneService geneService = null;
	private BibliographicReferenceService bibliographicReferenceService = null;
	

	/**
	 * @return Returns the geneService.
	 */
	public GeneService getGeneService() {
		return geneService;
	}
	/**
	 * @param geneService
	 *            The geneService to set.
	 */
	public void setGeneService(GeneService geneService) {
		this.geneService = geneService;
	}
	/**
	 * @return Returns the bibliographicReferenceService.
	 */
	public BibliographicReferenceService getBibliographicReferenceService() {
		return bibliographicReferenceService;
	}
	/**
	 * @param bibliographicReferenceService
	 *            The bibliographicReferenceService to set.
	 */
	public void setBibliographicReferenceService(BibliographicReferenceService bibliographicReferenceService) {
		this.bibliographicReferenceService = bibliographicReferenceService;
	}	
	public ModelAndView handleRequestInternal(HttpServletRequest request,
			HttpServletResponse response) throws Exception {

		Map geneModel = new HashMap();
		long geneID=0;
		String action = request.getParameter("action");
		if (request.getParameter("geneID") != null)
			geneID = new Long(request.getParameter("geneID")).longValue();
		if( geneID==0)
			throw new Exception("Error: Must pass geneID parameter");
		if( action==null)
			action="view";

		Gene g = this.getGeneService().findByID(geneID);
		

		if(action.equals("addcitation")){
			String pubmedID = request.getParameter("pubmedID");
			BibliographicReference br = this.getBibliographicReferenceService().findByExternalId(pubmedID, "PUBMED");
	        if( br==null )
	        	br = this.getBibliographicReferenceService().createBibliographicReferenceByLookup(pubmedID, "PUBMED");
	        
			if( br!= null){
				java.util.Collection cites = g.getCitations();
				cites.add(br);
				g.setCitations(cites);
				g = this.getGeneService().updateGene(g);
				return new ModelAndView(new RedirectView("candidateGeneListActionComplete.htm?target=geneDetail&geneID=" + g.getId()));
			}
		}
		
		if(action.equals("removecitation")){
			long citationID = new Long(request.getParameter("citationID")).longValue();
			java.util.Collection cites = g.getCitations();
			BibliographicReference br = null;
			for(java.util.Iterator iter=cites.iterator();iter.hasNext();){
				br = (BibliographicReference) iter.next();
				if(br.getId().longValue()==citationID){
					cites.remove(br);
				}
			}
			g.setCitations(cites);
			g = this.getGeneService().updateGene(g);
			return new ModelAndView(new RedirectView("candidateGeneListActionComplete.htm?target=geneDetail&geneID=" + g.getId()));
		}
		
		if( action.equals("updatecitation")){
			long citationID = new Long(request.getParameter("citationID")).longValue();
			String description = request.getParameter("description");
			Collection<BibliographicReference> cites = g.getCitations();
			BibliographicReference br = null;
			for(Iterator<BibliographicReference> iter=cites.iterator();iter.hasNext();){
				br = (BibliographicReference) iter.next();
				if(br.getId().longValue()==citationID){
					br.setDescription(description);
				}
			}
			g.setCitations(cites);
			g = this.getGeneService().updateGene(g);
			return new ModelAndView(new RedirectView("candidateGeneListActionComplete.htm?target=geneDetail&geneID=" + g.getId()));
			
		}
		geneModel.put("gene", g);
		return new ModelAndView("geneDetail", "model", geneModel);
	}

}