/*
 * The Gemma project
 * 
 * Copyright (c) 2006 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package ubic.gemma.web.controller.expression.experiment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.StopWatch;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ubic.gemma.analysis.report.ExpressionExperimentReportService;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisService;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.AuditEventService;
import ubic.gemma.model.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.model.common.auditAndSecurity.eventType.SampleRemovalEvent;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.VocabCharacteristic;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExperimentalFactorService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSetService;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonService;
import ubic.gemma.ontology.OntologyResource;
import ubic.gemma.ontology.OntologyService;
import ubic.gemma.search.SearchResult;
import ubic.gemma.search.SearchService;
import ubic.gemma.search.SearchSettings;
import ubic.gemma.security.SecurityService;
import ubic.gemma.util.progress.ProgressJob;
import ubic.gemma.util.progress.ProgressManager;
import ubic.gemma.web.controller.BackgroundControllerJob;
import ubic.gemma.web.controller.BackgroundProcessingMultiActionController;
import ubic.gemma.web.remote.EntityDelegator;
import ubic.gemma.web.taglib.displaytag.ExpressionExperimentValueObjectComparator;
import ubic.gemma.web.util.EntityNotFoundException;

/**
 * @author keshav
 * @version $Id: ExpressionExperimentController.java,v 1.118 2008/07/05 01:17:58
 *          paul Exp $
 * @spring.bean id="expressionExperimentController"
 * @spring.property name = "expressionExperimentService"
 *                  ref="expressionExperimentService"
 * @spring.property name = "expressionExperimentSubSetService"
 *                  ref="expressionExperimentSubSetService"
 * @spring.property name = "expressionExperimentReportService"
 *                  ref="expressionExperimentReportService"
 * @spring.property name="differentialExpressionAnalysisService"
 *                  ref="differentialExpressionAnalysisService"
 * @spring.property name="methodNameResolver" ref="expressionExperimentActions"
 * @spring.property name="searchService" ref="searchService"
 * @spring.property name="ontologyService" ref="ontologyService"
 * @spring.property name="taxonService" ref="taxonService"
 * @spring.property name="auditTrailService" ref="auditTrailService"
 * @spring.property name="experimentalFactorService"
 *                  ref="experimentalFactorService"
 * @spring.property name="securityService" ref="securityService"
 * @spring.property name="auditEventService" ref="auditEventService"
 */
public class ExpressionExperimentController extends
		BackgroundProcessingMultiActionController {

	private static final Boolean AJAX = true;

	/*
	 * If this is too long, tooltips break.
	 */
	private static final int MAX_EVENT_DESCRIPTION_LENGTH = 200;

	private DifferentialExpressionAnalysisService differentialExpressionAnalysisService;
	private ExpressionExperimentService expressionExperimentService = null;
	private ExperimentalFactorService experimentalFactorService;

	private ExpressionExperimentSubSetService expressionExperimentSubSetService = null;
	private ExpressionExperimentReportService expressionExperimentReportService = null;

	private TaxonService taxonService;
	private SearchService searchService;

	private OntologyService ontologyService;

	private AuditTrailService auditTrailService;

	private AuditEventService auditEventService;

	private SecurityService securityService;

	private final String identifierNotFound = "Must provide a valid ExpressionExperiment identifier";

	/**
	 * @param request
	 * @param response
	 * @return ModelAndView
	 */
	@SuppressWarnings("unused")
	public ModelAndView delete(HttpServletRequest request,
			HttpServletResponse response) {

		Long id = null;
		try {
			id = Long.parseLong(request.getParameter("id"));
		} catch (NumberFormatException e) {
			throw new EntityNotFoundException("There was no valid identifier.");
		}

		if (id == null) {
			// should be a validation error.
			throw new EntityNotFoundException(identifierNotFound);
		}

		ExpressionExperiment expressionExperiment = expressionExperimentService
				.load(id);
		if (expressionExperiment == null) {
			throw new EntityNotFoundException(expressionExperiment
					+ " not found");
		}

		RemoveExpressionExperimentJob removeExpressionExperimentJob = new RemoveExpressionExperimentJob(
				expressionExperiment, expressionExperimentService);

		return startJob(removeExpressionExperimentJob);

	}

	/**
	 * Exposed for AJAX calls.
	 * 
	 * @param id
	 * @return taskId
	 */
	public String deleteById(Long id) {
		ExpressionExperiment expressionExperiment = expressionExperimentService
				.load(id);
		if (expressionExperiment == null)
			return null;
		RemoveExpressionExperimentJob removeExpressionExperimentJob = new RemoveExpressionExperimentJob(
				expressionExperiment, expressionExperimentService);
		return run(removeExpressionExperimentJob);
	}

	/**
	 * @param request
	 * @param response
	 * @return
	 */
	public ModelAndView filter(HttpServletRequest request,
			HttpServletResponse response) {
		String searchString = request.getParameter("filter");

		// Validate the filtering search criteria.
		if (StringUtils.isBlank(searchString)) {
			this.saveMessage(request, "No search criteria provided");
			return showAll(request, response);
		}

		Collection<SearchResult> searchResults = searchService.search(
				SearchSettings.ExpressionExperimentSearch(searchString)).get(
				ExpressionExperiment.class);

		if ((searchResults == null) || (searchResults.size() == 0)) {
			this.saveMessage(request, "Your search yielded no results.");
			return showAll(request, response);
		}

		if (searchResults.size() == 1) {
			this.saveMessage(request, "Search Criteria: " + searchString + "; "
					+ searchResults.size() + " Datasets matched.");
			return new ModelAndView(new RedirectView(
					"/Gemma/expressionExperiment/showExpressionExperiment.html?id="
							+ searchResults.iterator().next().getId()));
		}

		String list = "";
		for (SearchResult ee : searchResults)
			list += ee.getId() + ",";

		this.saveMessage(request, "Search Criteria: " + searchString + "; "
				+ searchResults.size() + " Datasets matched.");
		return new ModelAndView(new RedirectView(
				"/Gemma/expressionExperiment/showAllExpressionExperiments.html?id="
						+ list));
	}

	public String updateReport(Long id) {
		ExpressionExperiment expressionExperiment = expressionExperimentService
				.load(id);
		if (expressionExperiment == null)
			return null;
		Collection<Long> ids = new HashSet<Long>();
		ids.add(id);
		GenerateSummary runner = new GenerateSummary(
				expressionExperimentReportService, ids);
		runner.setDoForward(false);
		return (String) super.startJob(runner).getModel().get("taskId");
	}

	/**
	 * @param request
	 * @param response
	 * @return
	 */
	@SuppressWarnings( { "unused", "unchecked" })
	public ModelAndView generateSummary(HttpServletRequest request,
			HttpServletResponse response) {

		String sId = request.getParameter("id");

		// if no IDs are specified, then load all expressionExperiments and show
		// the summary (if available)
		if (sId == null) {
			return startJob(new GenerateSummary(
					expressionExperimentReportService));
		}
		Collection ids = new ArrayList<Long>();

		String[] idList = StringUtils.split(sId, ',');
		for (int i = 0; i < idList.length; i++) {
			if (StringUtils.isNotBlank(idList[i])) {
				ids.add(new Long(idList[i]));
			}
		}
		expressionExperimentReportService.generateSummaryObjects(ids);
		String idStr = StringUtils.join(ids.toArray(), ",");
		return new ModelAndView(
				new RedirectView(
						"/Gemma/expressionExperiment/showAllExpressionExperimentLinkSummaries.html"));
	}

	/**
	 * AJAX
	 * 
	 * @param query
	 *            search string
	 * @param taxonId
	 *            (if null, all taxa are searched)
	 * @return EE ids that match
	 */
	@SuppressWarnings("unchecked")
	public Collection<Long> find(String query, Long taxonId) {
		log.info("Search: " + query + " taxon=" + taxonId);
		return searchService.searchExpressionExperiments(query, taxonId);
	}

	/**
	 * @param expressionExperimentReportService
	 *            the expressionExperimentReportService to set
	 */
	public void setExpressionExperimentReportService(
			ExpressionExperimentReportService expressionExperimentReportService) {
		this.expressionExperimentReportService = expressionExperimentReportService;
	}

	/**
	 * @param expressionExperimentService
	 */
	public void setExpressionExperimentService(
			ExpressionExperimentService expressionExperimentService) {
		this.expressionExperimentService = expressionExperimentService;
	}

	/**
	 * @param expressionExperimentSubSetService
	 */
	public void setExpressionExperimentSubSetService(
			ExpressionExperimentSubSetService expressionExperimentSubSetService) {
		this.expressionExperimentSubSetService = expressionExperimentSubSetService;
	}

	/**
	 * @param searchService
	 *            the searchService to set
	 */
	public void setSearchService(SearchService searchService) {
		this.searchService = searchService;
	}

	/**
	 * @param ontologyService
	 *            the ontologyService to set
	 */
	public void setOntologyService(OntologyService ontologyService) {
		this.ontologyService = ontologyService;
	}

	public void setexperimentalFactorService(
			ExperimentalFactorService experimentalFactorService) {
		this.experimentalFactorService = experimentalFactorService;
	}

	/**
	 * @param ausitTrailService
	 *            the auditTrailService to set
	 */
	public void setAuditTrailService(AuditTrailService auditTrailService) {
		this.auditTrailService = auditTrailService;
	}

	/**
	 * AJAX
	 * 
	 * @param e
	 * @return
	 */
	public Collection<AnnotationValueObject> getAnnotation(EntityDelegator e) {
		if (e == null || e.getId() == null)
			return null;
		ExpressionExperiment expressionExperiment = expressionExperimentService
				.load(e.getId());

		Collection<AnnotationValueObject> annotation = new ArrayList<AnnotationValueObject>();
		for (Characteristic c : expressionExperiment.getCharacteristics()) {
			AnnotationValueObject annotationValue = new AnnotationValueObject();
			annotationValue.setId(c.getId());
			annotationValue.setClassName(c.getCategory());
			annotationValue.setTermName(c.getValue());
			annotationValue.setEvidenceCode(c.getEvidenceCode().toString());
			if (c instanceof VocabCharacteristic) {
				VocabCharacteristic vc = (VocabCharacteristic) c;
				annotationValue.setClassUri(vc.getCategoryUri());
				String className = getLabelFromUri(vc.getCategoryUri());
				if (className != null)
					annotationValue.setClassName(className);
				annotationValue.setTermUri(vc.getValueUri());
				String termName = getLabelFromUri(vc.getValueUri());
				if (termName != null)
					annotationValue.setTermName(termName);
			}
			annotation.add(annotationValue);
		}
		return annotation;
	}

	/**
	 * @param uri
	 * @return
	 */
	private String getLabelFromUri(String uri) {
		OntologyResource resource = ontologyService.getResource(uri);
		if (resource != null)
			return resource.getLabel();

		return null;
	}

	/**
	 * @param ee
	 * @return
	 */
	private AuditEvent getLastTroubleEvent(ExpressionExperiment ee) {
		// Why doesn't this use expressionExperimentService.getLastTroubleEvent
		// ???
		AuditEvent event = auditTrailService.getLastTroubleEvent(ee);
		if (event != null)
			return event;

		// See if array design have trouble.
		for (Object o : expressionExperimentService.getArrayDesignsUsed(ee)) {
			event = auditTrailService.getLastTroubleEvent((ArrayDesign) o);
			if (event != null)
				return event;
		}

		return null;
	}

	/**
	 * @param ee
	 * @return
	 */
	private Collection<AuditEvent> getSampleRemovalEvents(
			ExpressionExperiment ee) {
		Collection<AuditEvent> result = new HashSet<AuditEvent>();
		for (BioAssay ba : ee.getBioAssays()) {
			for (AuditEvent e : ba.getAuditTrail().getEvents()) {
				if (e.getEventType() != null
						&& e.getEventType() instanceof SampleRemovalEvent) {
					result.add(e);
				}
			}
		}
		return result;
	}

	private AuditEvent getLastValidationEvent(ExpressionExperiment ee) {
		return auditTrailService.getLastValidationEvent(ee);
	}

	/**
	 * @param request
	 * @param response
	 * @param errors
	 * @return ModelAndView
	 */
	@SuppressWarnings( { "unused", "unchecked" })
	public ModelAndView show(HttpServletRequest request,
			HttpServletResponse response) {

		if (request.getParameter("id") == null) {
			// should be a validator error on submit
			return redirectToList(request);
		}

		Long id = Long.parseLong(request.getParameter("id"));

		if (id == null) {
			// should be a validator error on submit
			return redirectToList(request);
		}

		ExpressionExperiment expressionExperiment = expressionExperimentService
				.load(id);

		if (expressionExperiment == null) {
			return redirectToList(request);
		}

		expressionExperimentService.thawLite(expressionExperiment); // need to
		// get at
		// bioassays.

		Collection<Long> ids = new HashSet<Long>();
		ids.add(id);

		request.setAttribute("id", id);

		ModelAndView mav = new ModelAndView("expressionExperiment.detail")
				.addObject("expressionExperiment", expressionExperiment);

		QuantitationType prefQt = (QuantitationType) expressionExperimentService
				.getPreferredQuantitationType(expressionExperiment).iterator()
				.next();

		mav.addObject("prefQt", prefQt.getId());

		getEventsOfInterest(expressionExperiment, mav);

		Collection characteristics = expressionExperiment.getCharacteristics();
		mav.addObject("characteristics", characteristics);

		// Collection s =
		// expressionExperimentService.getQuantitationTypeCountById( id
		// ).entrySet();
		// mav.addObject( "qtCountSet", s );
		Collection quantitationTypes = expressionExperimentService
				.getQuantitationTypes(expressionExperiment);
		mav.addObject("quantitationTypes", quantitationTypes);
		mav.addObject("qtCount", quantitationTypes.size());

		// add arrayDesigns used, by name
		Collection<ArrayDesign> arrayDesigns = new ArrayList<ArrayDesign>();
		Collection<BioAssay> bioAssays = expressionExperiment.getBioAssays();
		for (BioAssay assay : bioAssays) {
			ArrayDesign design = assay.getArrayDesignUsed();
			if (!arrayDesigns.contains(design)) {
				arrayDesigns.add(design);
			}
		}

		mav.addObject("arrayDesigns", arrayDesigns);

		// add count of designElementDataVectors
		Long designElementDataVectorCount = new Long(
				expressionExperimentService
						.getDesignElementDataVectorCountById(id));
		mav.addObject("designElementDataVectorCount",
				designElementDataVectorCount);

		// load coexpression link count from cache
		Collection<Long> eeId = new ArrayList<Long>();
		eeId.add(id);
		Collection<ExpressionExperimentValueObject> eeVos = expressionExperimentReportService
				.retrieveSummaryObjects(eeId);

		AuditEvent lastArrayDesignUpdate = expressionExperimentService
				.getLastArrayDesignUpdate(expressionExperiment, null);
		mav.addObject("lastArrayDesignUpdate", lastArrayDesignUpdate);

		if (eeVos != null && eeVos.size() > 0) {
			ExpressionExperimentValueObject vo = eeVos.iterator().next();
			String eeLinks = vo.getCoexpressionLinkCount().toString()
					+ " (as of " + vo.getDateCached() + ")";
			mav.addObject("eeCoexpressionLinks", eeLinks);
		}

		mav.addObject("eeId", id);
		mav.addObject("eeClass", ExpressionExperiment.class.getName());

		boolean isPrivate = securityService.isPrivate(expressionExperiment);
		mav.addObject("isPrivate", isPrivate);

		return mav;
	}

	/**
	 * Trouble, validation, sample removal.
	 * 
	 * @param expressionExperiment
	 * @param mav
	 */
	private void getEventsOfInterest(ExpressionExperiment expressionExperiment,
			ModelAndView mav) {
		AuditEvent troubleEvent = getLastTroubleEvent(expressionExperiment);
		if (troubleEvent != null) {
			mav.addObject("troubleEvent", troubleEvent);
			auditEventService.thaw(troubleEvent);
			mav.addObject("troubleEventDescription", StringUtils.abbreviate(
					StringEscapeUtils.escapeXml(troubleEvent.toString()),
					MAX_EVENT_DESCRIPTION_LENGTH));
		}
		AuditEvent validatedEvent = getLastValidationEvent(expressionExperiment);
		if (validatedEvent != null) {
			mav.addObject("validatedEvent", validatedEvent);
			auditEventService.thaw(validatedEvent);
			mav.addObject("validatedEventDescription", StringUtils.abbreviate(
					StringEscapeUtils.escapeXml(validatedEvent.toString()),
					MAX_EVENT_DESCRIPTION_LENGTH));
		}

		Collection<AuditEvent> sampleRemovalEvents = this
				.getSampleRemovalEvents(expressionExperiment);
		if (sampleRemovalEvents.size() > 0) {
			AuditEvent event = sampleRemovalEvents.iterator().next();
			mav.addObject("samplesRemoved", event); // todo: handle multiple
			auditEventService.thaw(event);
			mav.addObject("samplesRemovedDescription", StringUtils.abbreviate(
					StringEscapeUtils.escapeXml(validatedEvent.toString()),
					MAX_EVENT_DESCRIPTION_LENGTH));
		}
	}

	/**
	 * Show all experiments (optionally conditioned on either a taxon, or a list
	 * of ids)
	 * 
	 * @param request
	 * @param response
	 * @return ModelAndView
	 */
	@SuppressWarnings( { "unused", "unchecked" })
	public ModelAndView showAll(HttpServletRequest request,
			HttpServletResponse response) {

		String sId = request.getParameter("id");
		String taxonId = request.getParameter("taxonId");
		Collection<ExpressionExperimentValueObject> expressionExperiments = new ArrayList<ExpressionExperimentValueObject>();
		Collection<ExpressionExperimentValueObject> eeValObjectCol;
		ModelAndView mav = new ModelAndView("expressionExperiments");

		if (taxonId != null) {
			// if a taxon ID is specified, load all expression experiments for
			// this taxon
			try {
				Long tId = Long.parseLong(taxonId);
				Taxon taxon = taxonService.load(tId);
				eeValObjectCol = this
						.getExpressionExperimentValueObjects(expressionExperimentService
								.findByTaxon(taxon));
				mav.addObject("showAll", false);
				mav.addObject("taxon", taxon);
			} catch (NumberFormatException e) {
				this.saveMessage(request,
						"Invalid taxon id, must be an integer");
				return mav;
			}
		} else if (sId == null) {
			this.saveMessage(request, "Displaying all Datasets");
			mav.addObject("showAll", true);
			// if no IDs are specified, then load all expressionExperiments
			eeValObjectCol = this
					.getFilteredExpressionExperimentValueObjects(null);
		} else {
			// if ids are specified, then display only those
			// expressionExperiments
			Collection<Long> eeIdList = new ArrayList<Long>();
			String[] idList = StringUtils.split(sId, ',');
			try {
				for (int i = 0; i < idList.length; i++) {
					if (StringUtils.isNotBlank(idList[i])) {
						eeIdList.add(Long.parseLong(idList[i]));
					}
				}
			} catch (NumberFormatException e) {
				this
						.saveMessage(request,
								"Invalid ids, must be a list of integers separated by commas.");
				return mav;
			}
			mav.addObject("showAll", false);
			eeValObjectCol = this
					.getFilteredExpressionExperimentValueObjects(eeIdList);
		}
		expressionExperiments.addAll(eeValObjectCol);

		// sort expression experiments by name first
		Collections.sort(
				(List<ExpressionExperimentValueObject>) expressionExperiments,
				new ExpressionExperimentValueObjectComparator());

		if (SecurityService.isUserAdmin()) {
			expressionExperimentReportService
					.fillEventInformation(expressionExperiments);
		}

		Long numExpressionExperiments = new Long(expressionExperiments.size());

		mav.addObject("expressionExperiments", expressionExperiments);
		mav.addObject("numExpressionExperiments", numExpressionExperiments);
		return mav;

	}

	/**
	 * @param request
	 * @param response
	 * @return ModelAndView
	 */
	@SuppressWarnings( { "unused", "unchecked" })
	public ModelAndView showAllLinkSummaries(HttpServletRequest request,
			HttpServletResponse response) {
		log.info("Processing link summary request");

		String sId = request.getParameter("id");
		Collection<ExpressionExperimentValueObject> expressionExperiments = new ArrayList<ExpressionExperimentValueObject>();
		Collection<ExpressionExperimentValueObject> eeValObjectCol;

		StopWatch timer = new StopWatch();
		timer.start();

		// if no IDs are specified, then load all expressionExperiments
		if (sId == null) {
			this.saveMessage(request, "Displaying all Datasets");
			eeValObjectCol = this
					.getFilteredExpressionExperimentValueObjects(null);
		} else { // if ids are specified, then display only those
			// expressionExperiments
			Collection<Long> ids = parseIdParameterString(sId);
			eeValObjectCol = this
					.getFilteredExpressionExperimentValueObjects(ids);
		}

		if (timer.getTime() > 1000) {
			log.info("Phase 1 done in " + timer.getTime() + "ms");
		}

		expressionExperiments.addAll(eeValObjectCol);

		expressionExperimentReportService
				.fillLinkStatsFromCache(expressionExperiments);
		expressionExperimentReportService
				.fillEventInformation(expressionExperiments);
		expressionExperimentReportService
				.fillAnnotationInformation(expressionExperiments);

		Collections.sort(
				(List<ExpressionExperimentValueObject>) expressionExperiments,
				new Comparator() {
					public int compare(Object o1, Object o2) {
						String s1 = ((ExpressionExperimentValueObject) o1)
								.getName();
						String s2 = ((ExpressionExperimentValueObject) o2)
								.getName();
						int comparison = s1.compareToIgnoreCase(s2);
						return comparison;
					}
				});
		Long numExpressionExperiments = new Long(expressionExperiments.size());
		ModelAndView mav = new ModelAndView("expressionExperimentLinkSummary");
		mav.addObject("expressionExperiments", expressionExperiments);
		mav.addObject("numExpressionExperiments", numExpressionExperiments);

		timer.stop();
		if (timer.getTime() > 1000) {
			log.info("Ready with link reports in " + timer.getTime() + "ms");
		}

		return mav;

	}

	/**
	 * @param sId
	 * @return
	 */
	private Collection<Long> parseIdParameterString(String sId) {
		Collection<Long> ids = new ArrayList<Long>();
		String[] idList = StringUtils.split(sId, ',');
		for (int i = 0; i < idList.length; i++) {
			if (StringUtils.isNotBlank(idList[i])) {
				ids.add(new Long(idList[i]));
			}
		}
		return ids;
	}

	/**
	 * @param request
	 * @param response
	 * @param errors
	 * @return ModelAndView
	 */
	@SuppressWarnings("unused")
	public ModelAndView showBioAssays(HttpServletRequest request,
			HttpServletResponse response) {
		String idStr = request.getParameter("id");

		if (idStr == null) {
			// should be a validation error, on 'submit'.
			throw new EntityNotFoundException(identifierNotFound);
		}
		Long id = Long.parseLong(idStr);

		ExpressionExperiment expressionExperiment = expressionExperimentService
				.load(id);
		if (expressionExperiment == null) {
			throw new EntityNotFoundException(id + " not found");
		}

		request.setAttribute("id", id);
		ModelAndView mv = new ModelAndView("bioAssays").addObject("bioAssays",
				expressionExperiment.getBioAssays());
		mv.addObject("expressionExperiment", expressionExperiment);
		return mv;
	}

	/**
	 * @param request
	 * @param response
	 * @param errors
	 * @return ModelAndView
	 */
	@SuppressWarnings("unused")
	public ModelAndView showBioMaterials(HttpServletRequest request,
			HttpServletResponse response) {
		String idStr = request.getParameter("id");

		if (idStr == null) {
			// should be a validation error, on 'submit'.
			throw new EntityNotFoundException(identifierNotFound);
		}
		Long id = Long.parseLong(idStr);

		ExpressionExperiment expressionExperiment = expressionExperimentService
				.load(id);
		if (expressionExperiment == null) {
			throw new EntityNotFoundException(id + " not found");
		}

		Collection<BioAssay> bioAssays = expressionExperiment.getBioAssays();
		Collection<BioMaterial> bioMaterials = new ArrayList<BioMaterial>();
		for (BioAssay assay : bioAssays) {
			Collection<BioMaterial> materials = assay.getSamplesUsed();
			if (materials != null) {
				bioMaterials.addAll(materials);
			}
		}

		ModelAndView mav = new ModelAndView("bioMaterials");
		if (AJAX) {
			StringBuilder buf = new StringBuilder();
			for (BioMaterial bm : bioMaterials) {
				buf.append(bm.getId());
				buf.append(",");
			}
			mav.addObject("bioMaterialIdList", buf.toString().replaceAll(",$",
					""));
		}

		Long numBioMaterials = new Long(bioMaterials.size());
		mav.addObject("numBioMaterials", numBioMaterials);
		mav.addObject("bioMaterials", bioMaterials);

		return mav;
	}

	/**
	 * Shows a bioassay view of a single expression experiment subset.
	 * 
	 * @param request
	 * @param response
	 * @param errors
	 * @return ModelAndView
	 */
	@SuppressWarnings("unused")
	public ModelAndView showExpressionExperimentSubSet(
			HttpServletRequest request, HttpServletResponse response) {
		Long id = Long.parseLong(request.getParameter("id"));

		if (id == null) {
			// should be a validation error, on 'submit'.
			throw new EntityNotFoundException(identifierNotFound);
		}

		ExpressionExperiment expressionExperiment = expressionExperimentService
				.load(id);
		if (expressionExperiment == null) {
			throw new EntityNotFoundException(id + " not found");
		}

		request.setAttribute("id", id);
		return new ModelAndView("bioAssays").addObject("bioAssays",
				expressionExperiment.getBioAssays());
	}

	/**
	 * shows a list of BioAssays for an expression experiment subset
	 * 
	 * @param request
	 * @param response
	 * @param errors
	 * @return ModelAndView
	 */
	@SuppressWarnings("unused")
	public ModelAndView showSubSet(HttpServletRequest request,
			HttpServletResponse response) {
		Long id = Long.parseLong(request.getParameter("id"));
		if (id == null) {
			// should be a validation error, on 'submit'.
			throw new EntityNotFoundException(identifierNotFound);
		}

		ExpressionExperimentSubSet subset = expressionExperimentSubSetService
				.load(id);
		if (subset == null) {
			throw new EntityNotFoundException(id + " not found");
		}

		// request.setAttribute( "id", id );
		return new ModelAndView("bioAssays").addObject("bioAssays", subset
				.getBioAssays());
	}

	/**
	 * @param securedEEs
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private Collection<ExpressionExperimentValueObject> getExpressionExperimentValueObjects(
			Collection<ExpressionExperiment> securedEEs) {
		StopWatch timer = new StopWatch();
		timer.start();
		Collection<Long> ids = new HashSet<Long>();
		for (ExpressionExperiment ee : securedEEs) {
			ids.add(ee.getId());
		}

		Collection<ExpressionExperimentValueObject> valueObjs = expressionExperimentService
				.loadValueObjects(ids);

		if (timer.getTime() > 1000) {
			log.info("Value objects in " + timer.getTime() + "ms");
		}

		return valueObjs;
	}

	/**
	 * Get the expression experiment value objects for the expression
	 * experiments.
	 * 
	 * @param eeCol
	 * @return Collection<ExpressionExperimentValueObject>
	 */
	@SuppressWarnings("unchecked")
	private Collection<ExpressionExperimentValueObject> getFilteredExpressionExperimentValueObjects(
			Collection<Long> eeIds) {

		Collection<ExpressionExperiment> securedEEs = new ArrayList<ExpressionExperiment>();

		StopWatch timer = new StopWatch();
		timer.start();

		/* Filtering for security happens here. */
		if (eeIds == null) {
			securedEEs = expressionExperimentService.loadAll();
		} else {
			securedEEs = expressionExperimentService.loadMultiple(eeIds);
		}

		if (timer.getTime() > 1000) {
			log.info("EEs in " + timer.getTime() + "ms");
		}

		log.info("Loading value objects ...");
		return getExpressionExperimentValueObjects(securedEEs);
	}

	/**
	 * @param request
	 * @return
	 */
	private ModelAndView redirectToList(HttpServletRequest request) {
		this.addMessage(request, "errors.objectnotfound",
				new Object[] { "Expression Experiment" });
		return new ModelAndView(
				new RedirectView(
						"/Gemma/expressionExperiment/showAllExpressionExperiments.html"));
	}

	/**
	 * Generates summary reports of expression experiments
	 * 
	 * @author pavlidis
	 * @version $Id: ExpressionExperimentController.java,v 1.118 2008/07/05
	 *          01:17:58 paul Exp $
	 */
	private class GenerateSummary extends BackgroundControllerJob<ModelAndView> {

		private ExpressionExperimentReportService expressionExperimentReportService;
		private Collection ids;

		public GenerateSummary(
				ExpressionExperimentReportService expressionExperimentReportService) {
			super(getMessageUtil());
			this.expressionExperimentReportService = expressionExperimentReportService;
			ids = null;
		}

		public GenerateSummary(
				ExpressionExperimentReportService expressionExperimentReportService,
				Collection id) {
			super(getMessageUtil());
			this.expressionExperimentReportService = expressionExperimentReportService;
			this.ids = id;
		}

		@SuppressWarnings("unchecked")
		public ModelAndView call() throws Exception {

			init();

			ProgressJob job = ProgressManager.createProgressJob(this
					.getTaskId(),
					securityContext.getAuthentication().getName(),
					"Expression experiment report  generating");

			if (ids == null) {
				saveMessage("Generating report for all experiments");
				job.updateProgress("Generating report for all experiments");
				expressionExperimentReportService.generateSummaryObjects();
			} else {
				saveMessage("Generating report for experiment");
				job
						.updateProgress("Generating report for specified experiment");
				expressionExperimentReportService.generateSummaryObjects(ids);
			}
			ProgressManager.destroyProgressJob(job);
			if (ids != null) {
				String idStr = StringUtils.join(ids.toArray(), ",");
				return new ModelAndView(new RedirectView(
						"/Gemma/expressionExperiment/showAllExpressionExperimentLinkSummaries.html?id="
								+ idStr));
			}
			return new ModelAndView(
					new RedirectView(
							"/Gemma/expressionExperiment/showAllExpressionExperimentLinkSummaries.html"));

		}
	}

	/**
	 * Delete expression experiments.
	 * 
	 * @author pavlidis
	 * @version $Id: ExpressionExperimentController.java,v 1.118 2008/07/05
	 *          01:17:58 paul Exp $
	 */
	private class RemoveExpressionExperimentJob extends
			BackgroundControllerJob<ModelAndView> {

		ExpressionExperimentService expressionExperimentService;
		ExpressionExperiment ee;

		public RemoveExpressionExperimentJob(ExpressionExperiment ee,
				ExpressionExperimentService expressionExperimentService) {
			super(getMessageUtil());
			this.expressionExperimentService = expressionExperimentService;
			this.ee = ee;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.util.concurrent.Callable#call()
		 */
		@SuppressWarnings("unchecked")
		public ModelAndView call() throws Exception {

			init();

			expressionExperimentService.thawLite(ee);
			ProgressJob job = ProgressManager.createProgressJob(this
					.getTaskId(),
					securityContext.getAuthentication().getName(),
					"Deleting dataset: " + ee.getId());

			expressionExperimentService.delete(ee);
			saveMessage("Dataset " + ee.getShortName()
					+ " removed from Database");
			ee = null;

			ProgressManager.destroyProgressJob(job);
			return new ModelAndView(
					new RedirectView(
							"/Gemma/expressionExperiment/showAllExpressionExperiments.html"));

		}
	}

	/**
	 * AJAX
	 * 
	 * @param eeId
	 * @return a collectino of factor value objects that represent the factors
	 *         of a given experiment
	 */
	public Collection<FactorValueObject> getExperimentalFactors(
			EntityDelegator e) {

		if (e == null || e.getId() == null)
			return null;

		ExpressionExperiment ee = this.expressionExperimentService.load(e
				.getId());
		Collection<FactorValueObject> result = new HashSet<FactorValueObject>();

		if (ee.getExperimentalDesign() == null)
			return null;

		Collection<ExperimentalFactor> factors = ee.getExperimentalDesign()
				.getExperimentalFactors();

		for (ExperimentalFactor factor : factors)
			result.add(new FactorValueObject(factor));

		return result;
	}

	/**
	 * AJAX call
	 * 
	 * @param id
	 * @return a more informative description than the regular description 1st
	 *         120 characters of ee.description + Experimental Design
	 *         information returned string contains HTML tags.
	 * 
	 * TODO: Would be more generic if passed back a DescriptionValueObject that
	 * contains all the info necessary to reconstruct the HTML on the client
	 * side Currently only used by ExpressionExperimentGrid.js (row expander)
	 */

	private static final int TRIM_SIZE = 120;

	public String getDescription(Long id) {
		ExpressionExperiment ee = expressionExperimentService.load(id);
		if (ee == null)
			return null;

		Collection<ExperimentalFactor> efs = ee.getExperimentalDesign()
				.getExperimentalFactors();

		StringBuffer descriptive = new StringBuffer();
		String eeDescription = ee.getDescription().trim();

		// Need to trim?
		if (eeDescription.length() < TRIM_SIZE + 1)
			descriptive.append(eeDescription);
		else
			descriptive.append(eeDescription.substring(0, TRIM_SIZE) + "...");

		// Is there any factor info to add?
		if ((efs != null) && (efs.size() < 1))
			return descriptive.append("<b> No Factors </b>").toString();

		String efUri = "  <a target='_blank' href='/Gemma/experimentalDesign/showExperimentalDesign.html?id="
				+ ee.getExperimentalDesign().getId() + "'> (details) </a >";

		descriptive.append("<b> Factors: </b>");
		for (ExperimentalFactor ef : efs) {
			descriptive.append(ef.getName() + ", ");
		}

		// remove trailing "," and return as a string
		return descriptive.substring(0, descriptive.length() - 2) + efUri;

	}

	/**
	 * AJAX
	 * 
	 * @param id
	 *            of an experimental factor
	 * @return A collection of factor value objects for the specified
	 *         experimental factor
	 */
	public Collection<FactorValueObject> getFactorValues(EntityDelegator e) {

		if (e == null || e.getId() == null)
			return null;

		ExperimentalFactor ef = this.experimentalFactorService.load(e.getId());
		if (ef == null)
			return null;

		Collection<FactorValueObject> result = new HashSet<FactorValueObject>();

		Collection<FactorValue> values = ef.getFactorValues();
		for (FactorValue value : values) {
			result.add(new FactorValueObject(value));
		}

		return result;
	}

	/**
	 * AJAX
	 * 
	 * @param ids
	 *            of EEs to load
	 * @return security-filtered set of value objects.
	 */
	@SuppressWarnings("unchecked")
	public Collection<ExpressionExperimentValueObject> loadExpressionExperiments(
			Collection<Long> ids) {

		// required for security filtering.
		Collection<ExpressionExperiment> ees;
		Collection<Long> filteredIds = new HashSet<Long>();
		if (ids == null) {
			ees = expressionExperimentService.loadAll();
		} else if (ids.isEmpty()) {
			return new HashSet<ExpressionExperimentValueObject>();
		} else {
			ees = expressionExperimentService.loadMultiple(ids);
		}
		for (ExpressionExperiment ee : ees) {
			filteredIds.add(ee.getId());
		}
		Collection<ExpressionExperimentValueObject> result = expressionExperimentService
				.loadValueObjects(filteredIds);

		populateAnalyses(ids, result); // FIXME make this optional.

		return result;
	}

	/**
	 * Fill in information about analyses done on the experiments.
	 * 
	 * @param result
	 */
	@SuppressWarnings("unchecked")
	private void populateAnalyses(Collection<Long> eeids,
			Collection<ExpressionExperimentValueObject> result) {
		Map<Long, DifferentialExpressionAnalysis> analysisMap = differentialExpressionAnalysisService
				.findByInvestigationIds(eeids);
		for (ExpressionExperimentValueObject eevo : result) {
			if (!analysisMap.containsKey(eevo.getId())) {
				continue;
			}
			eevo.setDifferentialExpressionAnalysisId(analysisMap.get(
					eevo.getId()).getId());
		}
	}

	/**
	 * AJAX
	 * 
	 * @param e
	 * @return
	 */
	public Collection<DesignMatrixRowValueObject> getDesignMatrixRows(
			EntityDelegator e) {

		if (e == null || e.getId() == null)
			return null;
		ExpressionExperiment ee = this.expressionExperimentService.load(e
				.getId());
		if (ee == null)
			return null;

		return DesignMatrixRowValueObject.Factory.getDesignMatrix(ee);
	}

	/**
	 * @param taxonService
	 */
	public void setTaxonService(TaxonService taxonService) {
		this.taxonService = taxonService;
	}

	/**
	 * @param securityService
	 */
	public void setSecurityService(SecurityService securityService) {
		this.securityService = securityService;
	}

	public void setAuditEventService(AuditEventService auditEventService) {
		this.auditEventService = auditEventService;
	}

	public void setDifferentialExpressionAnalysisService(
			DifferentialExpressionAnalysisService differentialExpressionAnalysisService) {
		this.differentialExpressionAnalysisService = differentialExpressionAnalysisService;
	}

}