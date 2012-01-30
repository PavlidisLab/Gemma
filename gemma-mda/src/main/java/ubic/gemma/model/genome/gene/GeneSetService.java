/*
 * The Gemma project
 * 
 * Copyright (c) 2009 University of British Columbia
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
package ubic.gemma.model.genome.gene;

import java.util.Collection;

import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.userdetails.User;

import ubic.gemma.genome.gene.DatabaseBackedGeneSetValueObject;
import ubic.gemma.genome.gene.GeneSetValueObject;
import ubic.gemma.model.TaxonValueObject;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.GeneSet;

/**
 * Service for managing gene sets
 * 
 * @author kelsey,paul
 * @version $Id$
 */
public interface GeneSetService {

    /**
     * Creates all the the given GeneSets in the given collection
     * 
     * @param sets
     * @return
     */
    @Secured( { "GROUP_USER" })
    public Collection<GeneSet> create( Collection<GeneSet> sets );

    /**
     * Creates the given geneset in the DB
     * 
     * @param geneset
     * @return
     */
    @Secured( { "GROUP_USER" })
    public GeneSet create( GeneSet geneset );

    /**
     * Return all sets that contain the given gene
     * 
     * @param gene
     * @return
     */
    @Secured( { "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    public Collection<GeneSet> findByGene( Gene gene );

    /**
     * @param name
     * @return
     */
    @Secured( { "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    public Collection<GeneSet> findByName( String name );

    /**
     * @param name
     * @param taxon
     * @return
     */
    @Secured( { "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    public Collection<GeneSet> findByName( String name, Taxon taxon );

    /**
     * Load all the genesets with the given IDs
     * 
     * @param ids
     * @return
     */
    @Secured( { "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    public Collection<GeneSet> load( Collection<Long> ids );

    /**
     * Loads the geneset with the given id
     * 
     * @param id
     * @return geneSet witht he given ID or null
     */
    @Secured( { "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_READ" })
    public GeneSet load( Long id );

    /**
     * Load all the GeneSets that the user are the permissions to see.
     * 
     * @param id
     * @return
     */
    @Secured( { "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    public Collection<GeneSet> loadAll();

    /**
     * @param tax
     * @return
     */
    @Secured( { "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    public Collection<GeneSet> loadAll( Taxon tax );

    /**
     * Returns the {@link GeneSet}s for the currently logged in {@link User} - i.e, ones for which the current user has
     * specific read permissions on (as opposed to data sets which are public). Important: This method will return all
     * gene sets if security is not enabled.
     * <p>
     * Implementation note: Via a methodInvocationFilter. See AclAfterFilterCollectionForMyData for
     * processConfigAttribute. (in Gemma-core)
     * 
     * @return
     */
    @Secured( { "GROUP_USER", "AFTER_ACL_FILTER_MY_DATA" })
    public Collection<GeneSet> loadMyGeneSets();

    /**
     * @param tax
     * @return
     */
    @Secured( { "GROUP_USER", "AFTER_ACL_FILTER_MY_DATA" })
    public Collection<GeneSet> loadMyGeneSets( Taxon tax );

    /**
     * @return
     */
    @Secured( { "GROUP_USER", "AFTER_ACL_FILTER_MY_PRIVATE_DATA" })
    public Collection<GeneSet> loadMySharedGeneSets();

    /**
     * Given a collection of genesets remove them all from the db
     * 
     * @param sets
     */
    @Secured( { "GROUP_USER", "ACL_SECURABLE_COLLECTION_EDIT" })
    public void remove( Collection<GeneSet> sets );

    /**
     * IF the user has permisson to remove the Set, set will be removed.
     * 
     * @param geneset
     */
    @Secured( { "GROUP_USER", "ACL_SECURABLE_EDIT" })
    public void remove( GeneSet geneset );

    /**
     * Update all the genesets given in the Collection
     * 
     * @param sets
     */
    @Secured( { "GROUP_USER", "ACL_SECURABLE_COLLECTION_EDIT" })
    public void update( Collection<GeneSet> sets );

    /**
     * Update the given geneset with the new information in the DB
     * 
     * @param geneset
     */
    @Secured( { "GROUP_USER", "ACL_SECURABLE_EDIT" })
    public void update( GeneSet geneset );
    
    /**
     * Get a value object for the id param
     * @param id
     * @return null if id doesn't match an experiment set
     */
    public DatabaseBackedGeneSetValueObject getValueObject( Long id );
    
    /**
     * create an entity in the database based on the value object parameter
     * 
     * @param gsvo
     * @return value object converted from the newly created entity
     */
    public GeneSetValueObject createDatabaseEntity( GeneSetValueObject gsvo );
    

    /**
     * Given a Gemma Gene Id, find all the gene groups it is a member of
     * (filtering is handled when gene sets are loaded)
     * @param geneId
     * @return collection of geneSetValueObject
     */
    public Collection<GeneSetValueObject> findGeneSetsByGene( Long geneId );


    /**
     * Updates the database record for the param experiment set value object 
     * (permission permitting) with the value object's name and description.
     * @param eeSetVO
     * @return
     */
    public DatabaseBackedGeneSetValueObject updateDatabaseEntityNameDesc(
            DatabaseBackedGeneSetValueObject geneSetVO );
    
    /**
     * Updates the database record for the param gene set value object 
     * (permission permitting) with the members specified of the set, not the 
     * name or description etc.
     * @param groupId
     * @param gene ids
     * @return
     */
    public String updateDatabaseEntityMembers( Long groupId, Collection<Long> geneIds );

    public Collection<DatabaseBackedGeneSetValueObject> updateDatabaseEntity( Collection<DatabaseBackedGeneSetValueObject> geneSetVos );
    
    /**
     * Security is handled within method, when the set is loaded
     */
    public void deleteDatabaseEntity( DatabaseBackedGeneSetValueObject geneSetVO );

    public void deleteDatabaseEntities( Collection<DatabaseBackedGeneSetValueObject> vos );

    /**
     * Returns just the current users gene sets
     * 
     * @param privateOnly
     * @param taxonId if non-null, restrict the groups by ones which have genes in the given taxon.
     * @return
     */
    public Collection<DatabaseBackedGeneSetValueObject> getUsersGeneGroups( boolean privateOnly, Long taxonId );

    /**
     * Get the gene value objects for the members of the group param
     * 
     * @param groupId
     * @return
     */
    public Collection<GeneValueObject> getGenesInGroup( Long groupId );
    
    /**
     * @param query string to match to gene sets
     * @param taxonId
     * @return collection of GeneSetValueObjects that match name query
     */
    public Collection<GeneSetValueObject> findGeneSetsByName( String query, Long taxonId );

    /**
     * get the taxon for the gene set parameter, assumes that the taxon of the first gene will be representational of all the genes
     * @param geneSetVos
     * @return
     */
    public TaxonValueObject getGeneSetTaxon( GeneSetValueObject geneSetVO );
}