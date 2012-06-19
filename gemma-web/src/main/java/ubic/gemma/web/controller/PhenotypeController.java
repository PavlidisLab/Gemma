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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import ubic.gemma.association.phenotype.PhenotypeAssociationManagerService;
import ubic.gemma.model.common.description.BibliographicReferenceValueObject;
import ubic.gemma.model.genome.gene.GeneValueObject;
import ubic.gemma.model.genome.gene.phenotype.EvidenceFilter;
import ubic.gemma.model.genome.gene.phenotype.valueObject.CharacteristicValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.EvidenceValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.SimpleTreeValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.ValidateEvidenceValueObject;
import ubic.gemma.security.authentication.UserManager;

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

    private ValidateEvidenceValueObject generateValidateEvidenceValueObject( Throwable throwable ) {
        final ValidateEvidenceValueObject validateEvidenceValueObject = new ValidateEvidenceValueObject();

        if ( throwable instanceof AccessDeniedException ) {
            if ( this.userManager.loggedIn() ) {
                validateEvidenceValueObject.setAccessDenied( true );
            } else {
                validateEvidenceValueObject.setUserNotLoggedIn( true );
            }
        } else {
            // If type of throwable is not known, log it.
            this.log.error( throwable.getMessage() );
        }

        return validateEvidenceValueObject;
    }

    @RequestMapping(value = "/phenotypes.html", method = RequestMethod.GET)
    public ModelAndView showAllPhenotypes( HttpServletRequest request ) {
        ModelAndView mav = new ModelAndView( "phenotypes" );

        mav.addObject( "phenotypeUrlId", request.getParameter( "phenotypeUrlId" ) );
        mav.addObject( "geneId", request.getParameter( "geneId" ) );

        return mav;
    }

    public Collection<SimpleTreeValueObject> loadAllPhenotypesByTree( String taxonCommonName, boolean showOnlyEditable ) {
        return this.phenotypeAssociationManagerService.loadAllPhenotypesByTree( new EvidenceFilter( taxonCommonName,
                showOnlyEditable ) );
    }

    /**
     * Returns all genes that have given phenotypes.
     * 
     * @param phenotypes
     * @return all genes that have given phenotypes
     */
    public Collection<GeneValueObject> findCandidateGenes( String taxonCommonName, boolean showOnlyEditable,
            String[] phenotypes ) {
        return this.phenotypeAssociationManagerService.findCandidateGenes( new EvidenceFilter( taxonCommonName,
                showOnlyEditable ), new HashSet<String>( Arrays.asList( phenotypes ) ) );
    }

    /**
     * Returns all phenotypes satisfied the given search criteria.
     * 
     * @param query
     * @param geneId
     * @return Collection of phenotypes
     */
    public Collection<CharacteristicValueObject> searchOntologyForPhenotypes( String query, Long geneId ) {
        return this.phenotypeAssociationManagerService.searchOntologyForPhenotypes( query, geneId );
    }

    /**
     * Finds bibliographic reference with the given pubmed id.
     * 
     * @param pubMedId
     * @return bibliographic reference with the given pubmed id
     */
    public Collection<BibliographicReferenceValueObject> findBibliographicReference( String pubMedId, Long evidenceId ) {
        BibliographicReferenceValueObject valueObject = this.phenotypeAssociationManagerService.findBibliographicReference(
                pubMedId, evidenceId );

        // Contain at most 1 element.
        ArrayList<BibliographicReferenceValueObject> valueObjects = new ArrayList<BibliographicReferenceValueObject>( 1 );
        if ( valueObject != null ) {
            valueObjects.add( valueObject );
        }

        return valueObjects;
    }

    /**
     * Returns mged category terms.
     * 
     * @return Collection<CharacteristicValueObject>
     */
    public Collection<CharacteristicValueObject> findExperimentMgedCategory() {
        return this.phenotypeAssociationManagerService.findExperimentMgedCategory();
    }

    public Collection<CharacteristicValueObject> findExperimentOntologyValue( String givenQueryString,
            String categoryUri, Long taxonId ) {
        return this.phenotypeAssociationManagerService.findExperimentOntologyValue( givenQueryString, categoryUri, taxonId );
    }

    public ValidateEvidenceValueObject validatePhenotypeAssociationForm( EvidenceValueObject evidenceValueObject ) {
        ValidateEvidenceValueObject validateEvidenceValueObject;
        try {
            validateEvidenceValueObject = this.phenotypeAssociationManagerService.validateEvidence( evidenceValueObject );
        } catch ( Throwable throwable ) {
            validateEvidenceValueObject = generateValidateEvidenceValueObject( throwable );
        }
        return validateEvidenceValueObject;
    }

    public ValidateEvidenceValueObject processPhenotypeAssociationForm( EvidenceValueObject evidenceValueObject ) {
        ValidateEvidenceValueObject validateEvidenceValueObject;

        try {
            if ( evidenceValueObject.getId() == null ) { // if the form is a "create evidence" form
                validateEvidenceValueObject = this.phenotypeAssociationManagerService.create( evidenceValueObject );
            } else { // if the form is an "edit evidence" form
                validateEvidenceValueObject = this.phenotypeAssociationManagerService.update( evidenceValueObject );
            }
        } catch ( Throwable throwable ) {
            validateEvidenceValueObject = generateValidateEvidenceValueObject( throwable );
        }

        return validateEvidenceValueObject;
    }

    public ValidateEvidenceValueObject removePhenotypeAssociation( Long evidenceId ) {
        ValidateEvidenceValueObject validateEvidenceValueObject;
        try {
            validateEvidenceValueObject = this.phenotypeAssociationManagerService.remove( evidenceId );
        } catch ( Throwable throwable ) {
            validateEvidenceValueObject = generateValidateEvidenceValueObject( throwable );
        }
        return validateEvidenceValueObject;
    }
}
