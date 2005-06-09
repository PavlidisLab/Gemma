package edu.columbia.gemma.web.controller.genome.gene;

import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.servlet.mvc.BaseCommandController;

import edu.columbia.gemma.common.auditAndSecurity.AuditTrail;
import edu.columbia.gemma.common.auditAndSecurity.User;
import edu.columbia.gemma.common.auditAndSecurity.UserService;
import edu.columbia.gemma.genome.gene.CandidateGeneListService;
import edu.columbia.gemma.genome.gene.GeneService;
import edu.columbia.gemma.genome.Gene;
import edu.columbia.gemma.genome.gene.CandidateGeneList;
import edu.columbia.gemma.genome.gene.CandidateGene;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004 - 2005 Columbia University
 * 
 * @author daq2101
 * @version $Id: CandidateGeneListController.java,v 1.3 2005/06/03 20:24:39
 *          daq2101 Exp $
 * @spring.bean id="candidateGeneListController" name="/candidateGeneList.htm
 *              /candidateGeneListDetail.htm
 *              /candidateGeneListActionComplete.htm"
 * @spring.property name="candidateGeneListService"
 *                  ref="candidateGeneListService"
 * @spring.property name="geneService" ref="geneService"
 * @spring.property name="userService" ref="userService"
 */
public class CandidateGeneListController extends BaseCommandController {
	private CandidateGeneListService candidateGeneListService = null;

	private GeneService geneService = null;

	private UserService userService = null;

	/**
	 * @return Returns the candidateGeneListService.
	 */
	public CandidateGeneListService getCandidateGeneListService() {
		return candidateGeneListService;
	}

	/**
	 * @param candidateGeneListService
	 *            The candidateGeneListService to set.
	 */
	public void setCandidateGeneListService(
			CandidateGeneListService candidateGeneListService) {
		this.candidateGeneListService = candidateGeneListService;
	}

	/**
	 * @return Returns the geneService.
	 */
	public GeneService getGeneService() {
		return geneService;
	}

	/**
	 * @param candidateGeneListService
	 *            The candidateGeneListService to set.
	 */
	public void setGeneService(GeneService geneService) {
		this.geneService = geneService;
	}

	/**
	 * @param UserService
	 *            The UserService to set.
	 */
	public UserService getUserService() {
		return userService;
	}

	/**
	 * @param userService
	 *            The UserService to set.
	 */
	public void setUserService(UserService userService) {
		this.userService = userService;
	}

	public ModelAndView handleRequestInternal(HttpServletRequest request,
			HttpServletResponse response) throws Exception {

		Map candidateGeneListModel = new HashMap();
		String view = "candidateGeneList";
		String action = request.getParameter("action");
		String target = request.getParameter("target");
		long listID = 0;
		long geneID = 0;
		if (request.getParameter("listID") != null)
			listID = new Long(request.getParameter("listID")).longValue();
		if (request.getParameter("geneID") != null)
			geneID = new Long(request.getParameter("geneID")).longValue();
		if (action == null)
			action = "view";

		User usr = userService.getUser(request.getRemoteUser());
		this.candidateGeneListService.setActor(usr);

		if (target != null) {
			candidateGeneListModel
					.put("listID", request.getParameter("listID"));
			candidateGeneListModel.put("target", target);
			return new ModelAndView("candidateGeneListActionComplete", "model",
					candidateGeneListModel);
		}

		if (action.compareTo("view") == 0) {
			if (listID > 0) {
				// requesting a specific list; next view is Detail
				view = "candidateGeneListDetail";
				candidateGeneListModel.put("candidateGeneLists", this
						.getCandidateGeneListService().findByID(listID));
			} else {
				candidateGeneListModel.put("candidateGeneLists", this
						.getCandidateGeneListService().getAll());
			}
		}
		CandidateGene cg = null;

		if (action.compareTo("movecandidateuponcandidatelist") == 0) {
			CandidateGeneList cgl = this.getCandidateGeneListService()
					.findByID(listID);
			for (java.util.Iterator iter = cgl.getCandidates().iterator(); iter
					.hasNext();) {
				cg = (CandidateGene) iter.next();
				if (cg.getId().longValue() == geneID)
					break;
			}
			cgl.increaseRanking(cg);
			cg.getAuditTrail().update("CandidateGene Increased Rank", usr);
			this.getCandidateGeneListService().saveCandidateGeneList(cgl);
			return new ModelAndView(new RedirectView(
					"candidateGeneListActionComplete.htm?target=candidateGeneListDetail&listID="
							+ request.getParameter("listID")));
		}

		if (action.compareTo("movecandidatedownoncandidatelist") == 0) {
			CandidateGeneList cgl = this.getCandidateGeneListService()
					.findByID(listID);
			for (java.util.Iterator iter = cgl.getCandidates().iterator(); iter
					.hasNext();) {
				cg = (CandidateGene) iter.next();
				if (cg.getId().longValue() == geneID)
					break;
			}
			cgl.decreaseRanking(cg);
			cg.getAuditTrail().update("CandidateGene Decreased Rank", usr);
			this.getCandidateGeneListService().saveCandidateGeneList(cgl);
			return new ModelAndView(new RedirectView(
					"candidateGeneListActionComplete.htm?target=candidateGeneListDetail&listID="
							+ request.getParameter("listID")));
		}

		if (action.compareTo("addgenetocandidatelist") == 0) {
			CandidateGeneList cgl = this.getCandidateGeneListService()
					.findByID(listID);
			Gene g = this.getGeneService().findByID(geneID);
			cg = cgl.addCandidate(g);
			cg.setAuditTrail(AuditTrail.Factory.newInstance());
			cg.getAuditTrail().start("CandidateGene Created.", usr);
			cg.setOwner(usr);
			cg.setName(cg.getGene().getName());
			cg.setDescription("");
			this.getCandidateGeneListService().saveCandidateGeneList(cgl);
			return new ModelAndView(new RedirectView(
					"candidateGeneListActionComplete.htm?target=candidateGeneListDetail&listID="
							+ request.getParameter("listID")));
		}

		if (action.compareTo("removegenefromcandidatelist") == 0) {
			CandidateGeneList cgl = this.getCandidateGeneListService()
					.findByID(listID);
			for (java.util.Iterator iter = cgl.getCandidates().iterator(); iter
					.hasNext();) {
				cg = (CandidateGene) iter.next();
				if (cg.getId().longValue() == geneID)
					break;
			}
			cgl.removeCandidate(cg);
			this.getCandidateGeneListService().saveCandidateGeneList(cgl);
			return new ModelAndView(new RedirectView(
					"candidateGeneListActionComplete.htm?target=candidateGeneListDetail&listID="
							+ request.getParameter("listID")));
		}

		if (action.compareTo("update") == 0) {
			CandidateGeneList cgl = this.getCandidateGeneListService()
					.findByID(listID);
			cgl.setName(request.getParameter("listName").toString());
			cgl.setDescription(request.getParameter("listDescription")
					.toString());
			this.getCandidateGeneListService().saveCandidateGeneList(cgl);
			return new ModelAndView(new RedirectView(
					"candidateGeneListActionComplete.htm?listID="
							+ request.getParameter("listID")));
		}

		if (action.compareTo("delete") == 0) {
			this.getCandidateGeneListService().removeCandidateGeneList(
					this.getCandidateGeneListService().findByID(listID));
			return new ModelAndView(new RedirectView(
					"candidateGeneListActionComplete.htm"));

		}

		if (action.compareTo("add") == 0) {
			String newName = request.getParameter("newName");
			CandidateGeneList cgl = this.getCandidateGeneListService()
					.createCandidateGeneList(newName);
			return new ModelAndView(new RedirectView(
					"candidateGeneListActionComplete.htm"));

		}
		return new ModelAndView(view, "model", candidateGeneListModel);
	}

	/**
	 * This is needed or you will have to specify a commandClass in the
	 * DispatcherServlet's context
	 * 
	 * @param request
	 * @return Object
	 * @throws Exception
	 */
	protected Object formBackingObject(HttpServletRequest request)
			throws Exception {
		return request;
	}

}
