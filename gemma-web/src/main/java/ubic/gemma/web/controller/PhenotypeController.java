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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import ubic.gemma.core.association.phenotype.PhenotypeAssociationManagerService;
import ubic.gemma.core.association.phenotype.PhenotypeAssociationManagerServiceImpl;
import ubic.gemma.core.search.SearchException;
import ubic.gemma.core.security.authentication.UserManager;
import ubic.gemma.model.analysis.expression.diff.GeneDifferentialExpressionMetaAnalysis;
import ubic.gemma.model.association.phenotype.PhenotypeAssociation;
import ubic.gemma.model.common.description.BibliographicReferenceValueObject;
import ubic.gemma.model.common.description.CharacteristicValueObject;
import ubic.gemma.model.common.description.ExternalDatabaseValueObject;
import ubic.gemma.model.genome.gene.phenotype.EvidenceFilter;
import ubic.gemma.model.genome.gene.phenotype.valueObject.*;
import ubic.gemma.web.controller.common.auditAndSecurity.SecurityController;
import ubic.gemma.web.controller.common.auditAndSecurity.UserValueObject;
import ubic.gemma.web.remote.EntityDelegator;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

/**
 * Controller for phenotype
 *
 * @author frances
 */
@Controller
public class PhenotypeController extends BaseController {

    private static final Log logNeurocarta = LogFactory.getLog( PhenotypeAssociationManagerServiceImpl.class );

    @Autowired
    private PhenotypeAssociationManagerService phenotypeAssociationManagerService;
    @Autowired
    private UserManager userManager;
    @Autowired
    private SecurityController securityController;

    public Collection<ExternalDatabaseStatisticsValueObject> calculateExternalDatabasesStatistics() {
        return this.phenotypeAssociationManagerService.loadNeurocartaStatistics();
    }

    /**
     * Finds bibliographic reference with the given pubmed id.
     *
     * @return bibliographic reference with the given pubmed id
     */
    public Collection<BibliographicReferenceValueObject> findBibliographicReference( String pubMedId,
            Long evidenceId ) {
        BibliographicReferenceValueObject valueObject = this.phenotypeAssociationManagerService
                .findBibliographicReference( pubMedId );

        // Contain at most 1 element.
        ArrayList<BibliographicReferenceValueObject> valueObjects = new ArrayList<>( 1 );
        if ( valueObject != null ) {
            valueObjects.add( valueObject );
        }

        return valueObjects;
    }

    /**
     * @param showOnlyEditable show only editable
     * @param taxonId          taxon id
     * @param databaseIds      db ids
     * @param phenotypes       phenotypes
     * @return all genes that have given phenotypes.
     */
    public Collection<GeneEvidenceValueObject> findCandidateGenes( Long taxonId, boolean showOnlyEditable,
            Collection<Long> databaseIds, String[] phenotypes ) {
        return this.phenotypeAssociationManagerService
                .findCandidateGenes( new EvidenceFilter( taxonId, showOnlyEditable, databaseIds ),
                        new HashSet<>( Arrays.asList( phenotypes ) ) );
    }

    public Collection<EvidenceValueObject<? extends PhenotypeAssociation>> findEvidenceByFilters( Long taxonId,
            Integer limit, String userName ) {
        return this.phenotypeAssociationManagerService.findEvidenceByFilters( taxonId, limit, userName );
    }

    /**
     * Returns a collection of users who own evidence. Note that a collection of value objects instead of strings is
     * returned for front end convenience.
     *
     * @return a collection of users who own evidence
     */
    public Collection<UserValueObject> findEvidenceOwners() {
        Collection<UserValueObject> userVOs = new ArrayList<>();

        for ( String userName : this.phenotypeAssociationManagerService.findEvidenceOwners() ) {
            UserValueObject userVO = new UserValueObject();
            userVO.setUserName( userName );
            userVOs.add( userVO );
        }
        return userVOs;
    }

    /**
     * Returns available efo category terms.
     *
     * @return Collection
     */
    public Collection<CharacteristicValueObject> findExperimentCategory() {
        return this.phenotypeAssociationManagerService.findExperimentCategory();
    }

    public Collection<CharacteristicValueObject> findExperimentOntologyValue( String givenQueryString,
            String categoryUri, Long taxonId ) throws SearchException {
        return this.phenotypeAssociationManagerService.findExperimentOntologyValue( givenQueryString );
    }

    public Collection<ExternalDatabaseValueObject> findExternalDatabaseName() {
        return this.phenotypeAssociationManagerService.findExternalDatabasesWithEvidence();
    }

    /**
     * @param showOnlyEditable show only editable
     * @param taxonId          taxon id
     * @param databaseIds      db ids
     * @return the big 'tree' data structure shown in phenocarta's page.
     */
    public Collection<SimpleTreeValueObject> loadAllPhenotypesByTree( Long taxonId, boolean showOnlyEditable,
            Collection<Long> databaseIds ) {

        return this.phenotypeAssociationManagerService
                .loadAllPhenotypesByTree( new EvidenceFilter( taxonId, showOnlyEditable, databaseIds ) );
    }

    public ValidateEvidenceValueObject makeDifferentialExpressionEvidencesFromDiffExpressionMetaAnalysis(
            Long geneDifferentialExpressionMetaAnalysisId, SortedSet<CharacteristicValueObject> phenotypes,
            Double selectionThreshold ) {
        ValidateEvidenceValueObject validateEvidenceValueObject;
        try {
            validateEvidenceValueObject = this.phenotypeAssociationManagerService
                    .makeDifferentialExpressionEvidencesFromDiffExpressionMetaAnalysis(
                            geneDifferentialExpressionMetaAnalysisId, phenotypes, selectionThreshold );

            // get the permission of the metaAnalysis
            EntityDelegator<GeneDifferentialExpressionMetaAnalysis> ed = new EntityDelegator<>();
            ed.setId( geneDifferentialExpressionMetaAnalysisId );
            ed.setClassDelegatingFor( GeneDifferentialExpressionMetaAnalysis.class.getName() );

            // update the permission of the meta analysis,(this will update all evidence linked to it)
            this.securityController.updatePermission( this.securityController.getSecurityInfo( ed ) );

        } catch ( Throwable throwable ) {
            validateEvidenceValueObject = this.generateValidateEvidenceValueObject( throwable );
        }
        return validateEvidenceValueObject;
    }

    public ValidateEvidenceValueObject processPhenotypeAssociationForm( EvidenceValueObject<?> evidenceValueObject ) {
        ValidateEvidenceValueObject validateEvidenceValueObject;

        try {
            if ( evidenceValueObject.getId() == null ) { // if the form is a "create evidence" form
                validateEvidenceValueObject = this.phenotypeAssociationManagerService
                        .makeEvidence( evidenceValueObject );
            } else { // if the form is an "edit evidence" form
                validateEvidenceValueObject = this.phenotypeAssociationManagerService.update( evidenceValueObject );
            }
        } catch ( Throwable throwable ) {
            validateEvidenceValueObject = this.generateValidateEvidenceValueObject( throwable );
        }

        return validateEvidenceValueObject;
    }

    public ValidateEvidenceValueObject removeAllEvidenceFromMetaAnalysis( Long metaAnalysisId ) {
        ValidateEvidenceValueObject validateEvidenceValueObject;
        try {
            validateEvidenceValueObject = this.phenotypeAssociationManagerService
                    .removeAllEvidenceFromMetaAnalysis( metaAnalysisId );
        } catch ( Throwable throwable ) {
            validateEvidenceValueObject = this.generateValidateEvidenceValueObject( throwable );
        }
        return validateEvidenceValueObject;
    }

    public ValidateEvidenceValueObject removePhenotypeAssociation( Long evidenceId ) {
        ValidateEvidenceValueObject validateEvidenceValueObject;
        try {
            validateEvidenceValueObject = this.phenotypeAssociationManagerService.remove( evidenceId );
        } catch ( Throwable throwable ) {
            validateEvidenceValueObject = this.generateValidateEvidenceValueObject( throwable );
        }
        return validateEvidenceValueObject;
    }

    /**
     * Returns all phenotypes satisfied the given search criteria.
     *
     * @return Collection of phenotypes
     */
    public Collection<CharacteristicValueObject> searchOntologyForPhenotypes( String query, Long geneId ) throws SearchException {
        return this.phenotypeAssociationManagerService.searchOntologyForPhenotypes( query, geneId );
    }

    @RequestMapping(value = "/phenotypes.html", method = RequestMethod.GET)
    public ModelAndView showAllPhenotypes( HttpServletRequest request ) {
        ModelAndView mav = new ModelAndView( "phenotypes" );

        mav.addObject( "phenotypeUrlId", request.getParameter( "phenotypeUrlId" ) );
        mav.addObject( "geneId", request.getParameter( "geneId" ) );
        mav.addObject( "ncbiId", request.getParameter( "ncbiId" ) );

        return mav;
    }

    @RequestMapping(value = "/neurocartaStatistics.html", method = RequestMethod.GET)
    public ModelAndView showNeurocartaStatistics() {
        return new ModelAndView( "neurocartaStatistics" );
    }

    @RequestMapping(value = "/phenotypeAssociationManager.html", method = RequestMethod.GET)
    public ModelAndView showPhenotypeAssociationManager() {
        return new ModelAndView( "phenotypeAssociationManager" );
    }

    public ValidateEvidenceValueObject validatePhenotypeAssociationForm( EvidenceValueObject<PhenotypeAssociation> evidenceValueObject ) {
        ValidateEvidenceValueObject validateEvidenceValueObject;
        try {
            validateEvidenceValueObject = this.phenotypeAssociationManagerService
                    .validateEvidence( evidenceValueObject );
        } catch ( Throwable throwable ) {
            validateEvidenceValueObject = this.generateValidateEvidenceValueObject( throwable );
        }
        return validateEvidenceValueObject;
    }

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
            this.log.error( throwable.getMessage(), throwable );
            // put it also in neuroCarta logs
            PhenotypeController.logNeurocarta.error( throwable.getMessage(), throwable );
        }

        return validateEvidenceValueObject;
    }
}
