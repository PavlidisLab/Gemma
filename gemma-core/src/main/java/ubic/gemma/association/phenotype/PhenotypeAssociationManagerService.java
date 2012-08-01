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
package ubic.gemma.association.phenotype;

import java.util.Collection;
import java.util.Set;

import ubic.gemma.model.common.description.BibliographicReferenceValueObject;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.GeneValueObject;
import ubic.gemma.model.genome.gene.phenotype.EvidenceFilter;
import ubic.gemma.model.genome.gene.phenotype.valueObject.CharacteristicValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.EvidenceValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.GeneEvidenceValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.SimpleTreeValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.ValidateEvidenceValueObject;

/**
 * High Level Service used to add Candidate Gene Management System capabilities
 * 
 * @author paul
 * @version $Id$
 */
public interface PhenotypeAssociationManagerService {

    /**
     * Links an Evidence to a Gene
     * 
     * @param geneNCBI The Gene NCBI we want to add the evidence
     * @param evidence The evidence
     * @return Status of the operation
     */
    public abstract ValidateEvidenceValueObject makeEvidence( EvidenceValueObject evidence );

    /**
     * Return evidence satisfying the specified filters. If the current user has not logged in, empty container is
     * returned.
     * 
     * @param taxonId taxon id
     * @param limit number of evidence value objects to return
     * @param userName user name
     * @return evidence satisfying the specified filters
     */
    public abstract Collection<EvidenceValueObject> findEvidenceByFilters( Long taxonId, Integer limit, String userName );

    /**
     * Return all evidence for a specific gene NCBI
     * 
     * @param geneNCBI The Evidence id
     * @return The Gene we are interested in
     */
    public abstract Collection<EvidenceValueObject> findEvidenceByGeneNCBI( Integer geneNCBI );

    /**
     * Return all evidence for a specific gene id
     * 
     * @param geneId The Evidence id
     * @return The Gene we are interested in
     */
    public abstract Collection<EvidenceValueObject> findEvidenceByGeneId( Long geneId );

    /**
     * Return all evidence for a specific gene id with evidence flagged, indicating more information
     * 
     * @param geneId The Evidence id
     * @param phenotypesValuesUri the chosen phenotypes
     * @param evidenceFilter can specify a taxon and to show modifiable evidence (optional)
     * @return The Gene we are interested in
     */
    public abstract Collection<EvidenceValueObject> findEvidenceByGeneId( Long geneId, Set<String> phenotypesValuesUri,
            EvidenceFilter evidenceFilter );

    /**
     * Given an set of phenotypes returns the genes that have all those phenotypes or children phenotypes
     * 
     * @param phenotypesValuesUri the roots phenotype of the query
     * @param taxon the name of the taxon (optinal)
     * @return A collection of the genes found
     */
    public abstract Collection<GeneValueObject> findCandidateGenes( Set<String> phenotypesValuesUri, Taxon taxon );

    /**
     * Given an set of phenotypes returns the genes that have all those phenotypes or children phenotypes
     * 
     * @param phenotypesValuesUri the roots phenotype of the query
     * @param evidenceFilter can specify a taxon and to show modifiable evidence (optional)
     * @return A collection of the genes found
     */
    public abstract Collection<GeneValueObject> findCandidateGenes( EvidenceFilter evidenceFilter,
            Set<String> phenotypesValuesUri );

    /**
     * Removes an evidence
     * 
     * @param id The Evidence database id
     */
    public abstract ValidateEvidenceValueObject remove( Long id );

    /**
     * Load an evidence
     * 
     * @param id The Evidence database id
     */
    public abstract EvidenceValueObject load( Long id );

    /**
     * Modify an existing evidence
     * 
     * @param evidenceValueObject the evidence with modified fields
     * @return Status of the operation
     */
    public abstract ValidateEvidenceValueObject update( EvidenceValueObject evidenceValueObject );

    /**
     * Giving a phenotype searchQuery, returns a selection choice to the user
     * 
     * @param searchQuery query typed by the user
     * @param geneId the id of the chosen gene
     * @return Collection<CharacteristicValueObject> list of choices returned
     */
    public abstract Collection<CharacteristicValueObject> searchOntologyForPhenotypes( String searchQuery, Long geneId );

    /**
     * Does a Gene search (by name or symbol) for a query and return only Genes with evidence
     * 
     * @param query
     * @param taxonId, can be null to not constrain by taxon
     * @return Collection<GeneEvidenceValueObject> list of Genes
     */
    public abstract Collection<GeneEvidenceValueObject> findGenesWithEvidence( String query, Long taxonId );

    /**
     * Find all phenotypes associated to a pubmedID
     * 
     * @param pubMedId
     * @param evidenceId optional, used if we are updating to know current annotation
     * @return BibliographicReferenceValueObject
     */
    public abstract BibliographicReferenceValueObject findBibliographicReference( String pubMedId, Long evidenceId );

    /**
     * Validate an Evidence before we create it
     * 
     * @param geneNCBI The Gene NCBI we want to add the evidence
     * @param evidence The evidence
     * @return ValidateEvidenceValueObject flags of information to show user messages
     */
    public abstract ValidateEvidenceValueObject validateEvidence( EvidenceValueObject evidence );

    /**
     * Find mged category term that were used in the database, used to annotated Experiments
     * 
     * @return Collection<CharacteristicValueObject> the terms found
     */
    public abstract Collection<CharacteristicValueObject> findExperimentMgedCategory();

    /**
     * for a given search string look in the database and Ontology for matches
     * 
     * @param givenQueryString the search query
     * @param categoryUri the mged category (can be null)
     * @param taxonId the taxon id (can be null)
     * @return Collection<CharacteristicValueObject> the terms found
     */
    public abstract Collection<CharacteristicValueObject> findExperimentOntologyValue( String givenQueryString,
            String categoryUri, Long taxonId );

    /**
     * This method can be used if we want to reimport data from a specific external Database
     * 
     * @param externalDatabaseName
     */
    public abstract Collection<EvidenceValueObject> loadEvidenceWithExternalDatabaseName( String externalDatabaseName );

    /**
     * find all evidence that doesn't come from an external source
     */
    public Collection<EvidenceValueObject> loadEvidenceWithoutExternalDatabaseName();

    /**
     * This method loads all phenotypes in the database and counts their occurence using the database It builts the tree
     * using parents of terms, and will return 3 trees representing Disease, HP and MP
     * 
     * @param taxonCommonName specify a taxon (optional)
     * @return A collection of the phenotypes with the gene occurence
     */
    public abstract Collection<SimpleTreeValueObject> loadAllPhenotypesByTree( EvidenceFilter evidenceFilter );

    /**
     * For a given search string find all Ontology terms related, and then count their gene occurence by taxon,
     * including ontology children terms
     * 
     * @param searchQuery the query search that was type by the user
     * @return Collection<CharacteristicValueObject> the terms found in the database with taxon and gene occurence
     */
    public abstract Collection<CharacteristicValueObject> searchInDatabaseForPhenotype( String searchQuery );

    /** return the list of the owners that have evidence in the system */
    public  Collection<String> findEvidenceOwners();

}
