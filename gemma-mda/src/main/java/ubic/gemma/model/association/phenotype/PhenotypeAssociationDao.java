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
package ubic.gemma.model.association.phenotype;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.gene.phenotype.valueObject.CharacteristicValueObject;
import ubic.gemma.persistence.BaseDao;

/**
 * @author nicolas
 * @version $Id$
 */
public interface PhenotypeAssociationDao extends BaseDao<PhenotypeAssociation> {

    /** find Genes link to a phenotype */
    public Collection<Gene> findGeneWithPhenotypes( Set<String> phenotypesValueUri, String taxon );

    /** load all valueURI of Phenotype in the database */
    public Set<String> loadAllPhenotypesUri();

    /** find PhenotypeAssociations associated with a BibliographicReference */
    public Collection<PhenotypeAssociation> findPhenotypesForBibliographicReference( String pubMedID );

    /** find all PhenotypeAssociation for a specific gene id */
    public Collection<PhenotypeAssociation> findPhenotypeAssociationForGeneId( Long geneId );

    /** find all PhenotypeAssociation for a specific NCBI id */
    public Collection<PhenotypeAssociation> findPhenotypeAssociationForGeneNCBI( Integer geneNCBI );

    /** find MGED category terms currently used in the database by evidence */
    public Collection<CharacteristicValueObject> findEvidenceMgedCategoryTerms();

    /** find all evidences from a specific external database */
    public Collection<PhenotypeAssociation> findEvidencesWithExternalDatabaseName( String externalDatabaseName );

    /** find all public phenotypes associated with genes */
    public HashMap<String, HashSet<Integer>> findPublicPhenotypesGenesAssociations();

    /** find all public phenotypes associated with genes on a specific taxon and containing the valuesUri */
    public HashMap<String, HashSet<Integer>> findPublicPhenotypesGenesAssociations( String taxon, Set<String> valuesUri );

    /** find all phenotypes associated with genes for a user */
    public HashMap<String, HashSet<Integer>> findPrivatePhenotypesGenesAssociations( String userName );

    /** find all phenotypes associated with genes */
    public HashMap<String, HashSet<Integer>> findAllPhenotypesGenesAssociations();

}
