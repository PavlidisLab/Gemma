/*
 * The Gemma project
 * 
 * Copyright (c) 2011 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.web.controller;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import ubic.gemma.association.phenotype.PhenotypeAssociationManagerService;
import ubic.gemma.model.common.description.BibliographicReferenceValueObject;
import ubic.gemma.model.genome.gene.GeneValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.CharacteristicValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.EvidenceValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.LiteratureEvidenceValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.SecurityInfoValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.ValidateEvidenceValueObject;
import ubic.gemma.security.authentication.UserManager;
import ubic.gemma.util.JSONUtil;
import ubic.gemma.web.remote.JsonReaderResponse;

/**
 * Controller for phenotype
 * 
 * @author frances
 * @version $Id$
 */
@Controller
public class PhenotypeController extends BaseController {

    @Autowired
    private PhenotypeAssociationManagerService phenotypeAssociationManagerService;

    @Autowired
    private UserManager userManager;

    private static EvidenceValueObject createEvidenceValueObject(Integer geneNCBI, String[] phenotypeValueUris, String evidenceClassName, 
    		String pubmedId, String description, String evidenceCode, boolean isPublic, Long evidenceId, Long lastUpdated) {
    	
        Set<CharacteristicValueObject> phenotypes = new HashSet<CharacteristicValueObject>();
        for (int i = 0; i < phenotypeValueUris.length; ++i) {
        	phenotypes.add(new CharacteristicValueObject(phenotypeValueUris[i]));
        }
    	
        EvidenceValueObject evidenceValueObject = null; 
        if (evidenceClassName.equals(LiteratureEvidenceValueObject.class.getSimpleName())) {
        	SecurityInfoValueObject securityInfoValueObject = new SecurityInfoValueObject();
        	securityInfoValueObject.setPublic(isPublic);
        	
        	boolean isNegativeEvidence = false;
        	evidenceValueObject = new LiteratureEvidenceValueObject(geneNCBI,
        			phenotypes, pubmedId, description, evidenceCode, isNegativeEvidence);
        	evidenceValueObject.setSecurityInfoValueObject(securityInfoValueObject);
        	
        	if (evidenceId != null) {
        		evidenceValueObject.setId(evidenceId);
        		evidenceValueObject.setLastUpdated(lastUpdated);
        	}
        }
        
        return evidenceValueObject;
    }
    
    private ValidateEvidenceValueObject generateValidateEvidenceValueObject(Throwable throwable) {
    	final ValidateEvidenceValueObject validateEvidenceValueObject = new ValidateEvidenceValueObject();
    	
    	if (throwable instanceof AccessDeniedException) {
    		if ( userManager.loggedIn() ) {
    			validateEvidenceValueObject.setAccessDenied(true);
    		} else {
    			validateEvidenceValueObject.setUserNotLoggedIn(true);
    		}
    	} else {
    		// If type of throwable is not known, log it.
			log.error(throwable.getMessage());
    	}
    	
    	return validateEvidenceValueObject;
    }
    
    @RequestMapping(value = "/phenotypes.html", method = RequestMethod.GET)
    public ModelAndView showAllPhenotypes(HttpServletRequest request, HttpServletResponse response) {
        ModelAndView mav = new ModelAndView("phenotypes");

        mav.addObject("phenotypeUrlId", request.getParameter("phenotypeUrlId"));
        mav.addObject("geneId", request.getParameter("geneId"));

        return mav;
    }

    /**
     * Returns all genes that have given phenotypes.
     * 
     * @param phenotypes
     * @return all genes that have given phenotypes
     */
    public JsonReaderResponse<GeneValueObject> findCandidateGenes(String[] phenotypes) {
        Set<String> myPhenotypes = new HashSet<String>();
        for (String pheno : phenotypes) {
            myPhenotypes.add(pheno);
        }

        return new JsonReaderResponse<GeneValueObject>(new ArrayList<GeneValueObject>(
                phenotypeAssociationManagerService.findCandidateGenes(myPhenotypes)));
    }

    /**
     * Returns all phenotypes in the system.
     * 
     * @return all phenotypes in the system
     */
    public JsonReaderResponse<CharacteristicValueObject> loadAllPhenotypes() {
        return new JsonReaderResponse<CharacteristicValueObject>(new ArrayList<CharacteristicValueObject>(
                phenotypeAssociationManagerService.loadAllPhenotypes()));
    }

    /**
     * Returns all phenotypes satisfied the given search criteria. 
     * 
     * @param query
     * @param geneId
     * @return Collection of phenotypes
     */
    public Collection<CharacteristicValueObject> searchOntologyForPhenotypes(String query, Long geneId) {
        return phenotypeAssociationManagerService.searchOntologyForPhenotypes(query, geneId);
    }

    /**
     * Finds bibliographic reference with the given pubmed id. 
     * 
     * @param pubMedId
     * @return bibliographic reference with the given pubmed id
     */
    public Collection<BibliographicReferenceValueObject> findBibliographicReference(String pubMedId) {
    	BibliographicReferenceValueObject valueObject = phenotypeAssociationManagerService.findBibliographicReference(pubMedId);
    	
		ArrayList<BibliographicReferenceValueObject> valueObjects = new ArrayList<BibliographicReferenceValueObject>(1); // Contain at most 1 element.
		
		if (valueObject != null) {
			valueObjects.add(valueObject);
		}

        return valueObjects;
    }

    // Process both create and edit phenotype association form.
    @RequestMapping(value = "/processPhenotypeAssociationForm.html", method = RequestMethod.POST)
    public void processPhenotypeAssociationForm( HttpServletRequest request, HttpServletResponse response ) throws Exception {

        Integer geneNCBI = new Integer(request.getParameter( "geneNCBI" ));
        String[] phenotypeValueUris = request.getParameterValues( "phenotypes[]" );
        String evidenceClassName = request.getParameter( "evidenceClassName" );
        String pubmedId = request.getParameter( "pubmedId" );
        String description = request.getParameter( "description" );
        String evidenceCode = request.getParameter( "evidenceCode" );
        boolean isPublic = (request.getParameter( "isPublic" ) != null);
        
        String evidenceId = request.getParameter("evidenceId");  // hidden field
        
        ValidateEvidenceValueObject validateEvidenceValueObject;
        
        if (evidenceId.equals("")) { // if the form is a "create evidence" form
        	Long newEvidenceId = null;
        	Long lastUpdated = null;

    		try {
    			validateEvidenceValueObject = phenotypeAssociationManagerService.create(
    					createEvidenceValueObject(geneNCBI, phenotypeValueUris, evidenceClassName,
    							pubmedId, description, evidenceCode, isPublic, newEvidenceId, lastUpdated));
    		} catch (Throwable throwable) {
    			validateEvidenceValueObject = generateValidateEvidenceValueObject(throwable);
    		}
		} else { // if the form is an "edit evidence" form
			Long lastUpdated = new Long(request.getParameter("lastUpdated")); // hidden field
		
    		try {
    			validateEvidenceValueObject = phenotypeAssociationManagerService.update(
    					createEvidenceValueObject(geneNCBI, phenotypeValueUris, evidenceClassName,  
    				    		pubmedId, description, evidenceCode, isPublic, new Long(evidenceId), lastUpdated));
    		} catch (Throwable throwable) {
    			validateEvidenceValueObject = generateValidateEvidenceValueObject(throwable);
    		}
		}

	    final String jsonText;
		if (validateEvidenceValueObject == null) {
			jsonText = "{success:true}";
		} else {
			// Use the substring method to get json text after the first character "{".
			jsonText = "{success:false," + new ObjectMapper().writeValueAsString(validateEvidenceValueObject).substring(1);
		}

	    JSONUtil jsonUtil = new JSONUtil( request, response );
	    jsonUtil.writeToResponse( jsonText );
    }
    
    public ValidateEvidenceValueObject removePhenotypeAssociation(Long evidenceId) {
    	ValidateEvidenceValueObject validateEvidenceValueObject;
		try {
			validateEvidenceValueObject = phenotypeAssociationManagerService.remove(evidenceId);
		} catch (Throwable throwable) {
			validateEvidenceValueObject = generateValidateEvidenceValueObject(throwable);
		}
		return validateEvidenceValueObject;
    }
    
    public ValidateEvidenceValueObject validatePhenotypeAssociation(Integer geneNCBI, String[] phenotypeValueUris, String evidenceClassName, 
    		String pubmedId, String description, String evidenceCode, boolean isPublic, Long evidenceId, Long lastUpdated) {
    	ValidateEvidenceValueObject validateEvidenceValueObject;
		try {
			validateEvidenceValueObject = phenotypeAssociationManagerService.validateEvidence(createEvidenceValueObject(geneNCBI, phenotypeValueUris,  
	        		evidenceClassName, pubmedId, description, evidenceCode, isPublic, evidenceId, lastUpdated));
		} catch (Throwable throwable) {
			validateEvidenceValueObject = generateValidateEvidenceValueObject(throwable);
		}
		return validateEvidenceValueObject;
    }
}
