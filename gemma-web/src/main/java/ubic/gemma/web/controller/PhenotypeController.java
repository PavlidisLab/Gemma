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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
import ubic.gemma.model.genome.gene.phenotype.valueObject.TreeCharacteristicValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.ValidateEvidenceValueObject;
import ubic.gemma.security.authentication.UserManager;
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

    public Collection<TreeCharacteristicValueObject> loadAllPhenotypesByTree() {
      return phenotypeAssociationManagerService.loadAllPhenotypesByTree();
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
     * Returns all genes that have given phenotypes.
     * 
     * @param phenotypes
     * @return all genes that have given phenotypes
     */
    public Collection<GeneValueObject> findCandidateGenes(String[] phenotypes) {
        return phenotypeAssociationManagerService.findCandidateGenes(new HashSet<String>(Arrays.asList(phenotypes)),null);
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
    public Collection<BibliographicReferenceValueObject> findBibliographicReference( String pubMedId, Long evidenceId ) {
        BibliographicReferenceValueObject valueObject = phenotypeAssociationManagerService.findBibliographicReference(
                pubMedId, evidenceId );
    	
		ArrayList<BibliographicReferenceValueObject> valueObjects = new ArrayList<BibliographicReferenceValueObject>(1); // Contain at most 1 element.
		
		if (valueObject != null) {
			valueObjects.add(valueObject);
		}

        return valueObjects;
    }

    /**
     * Returns mged category terms. 
     * 
     * @return Collection<CharacteristicValueObject>
     */
    public Collection<CharacteristicValueObject> findExperimentMgedCategory() {
        return phenotypeAssociationManagerService.findExperimentMgedCategory();
    }

    public Collection<CharacteristicValueObject> findExperimentOntologyValue( String givenQueryString,
            String categoryUri, Long taxonId ) {
    	return phenotypeAssociationManagerService.findExperimentOntologyValue(givenQueryString,
    			categoryUri, taxonId);
    }
    
    public ValidateEvidenceValueObject validatePhenotypeAssociationForm(EvidenceValueObject evidenceValueObject) {
    	ValidateEvidenceValueObject validateEvidenceValueObject;
		try {
			validateEvidenceValueObject = phenotypeAssociationManagerService.validateEvidence(evidenceValueObject);
		} catch (Throwable throwable) {
			validateEvidenceValueObject = generateValidateEvidenceValueObject(throwable);
		}
		return validateEvidenceValueObject;
    }
    
    public ValidateEvidenceValueObject processPhenotypeAssociationForm(EvidenceValueObject evidenceValueObject) {
        ValidateEvidenceValueObject validateEvidenceValueObject;
        
		try {
	        if (evidenceValueObject.getId() == null) { // if the form is a "create evidence" form
	        	validateEvidenceValueObject = phenotypeAssociationManagerService.create(evidenceValueObject);
	        } else { // if the form is an "edit evidence" form
	        	validateEvidenceValueObject = phenotypeAssociationManagerService.update(evidenceValueObject);
	        }
		} catch (Throwable throwable) {
			validateEvidenceValueObject = generateValidateEvidenceValueObject(throwable);
		}

		return validateEvidenceValueObject;
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
}
