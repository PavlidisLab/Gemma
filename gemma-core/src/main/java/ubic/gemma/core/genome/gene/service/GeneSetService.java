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
package ubic.gemma.core.genome.gene.service;

import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.userdetails.User;
import ubic.gemma.core.genome.gene.SessionBoundGeneSetValueObject;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonValueObject;
import ubic.gemma.model.genome.gene.DatabaseBackedGeneSetValueObject;
import ubic.gemma.model.genome.gene.GeneSet;
import ubic.gemma.model.genome.gene.GeneSetValueObject;
import ubic.gemma.model.genome.gene.GeneValueObject;
import ubic.gemma.persistence.service.genome.gene.GeneSetDao;

import java.util.Collection;

/**
 * Service for managing gene sets
 *
 * @author kelsey, paul
 */
@SuppressWarnings("unused") // Possible external use
public interface GeneSetService {

    @Secured({ "GROUP_USER" })
    Collection<GeneSet> create( Collection<GeneSet> sets );

    @Secured({ "GROUP_USER" })
    GeneSet create( GeneSet geneset );

    /**
     * Return all sets that contain the given gene Security filtering done at DAO level see
     * {@link ubic.gemma.persistence.service.genome.gene.GeneSetDao}
     *
     * @param  gene gene
     * @return gene sets
     * @see         GeneSetDao GeneSetDao for security filtering
     */
    Collection<GeneSet> findByGene( Gene gene );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_VALUE_OBJECT_READ" })
    DatabaseBackedGeneSetValueObject loadValueObject( GeneSet geneSet );

    /**
     * The ids of member genes will not be filled in
     *
     * @param  ids ids
     * @return gene set value object
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_VALUE_OBJECT_COLLECTION_READ" })
    Collection<DatabaseBackedGeneSetValueObject> loadValueObjectsLite( Collection<Long> ids );


    /**
     * Ids of member genes will be filled in
     *
     * @param  ids ids
     * @return gene set value object
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_VALUE_OBJECT_COLLECTION_READ" })
    Collection<DatabaseBackedGeneSetValueObject> loadValueObjects( Collection<Long> ids );

    /**
     * Security filtering done at DAO level see {@link ubic.gemma.persistence.service.genome.gene.GeneSetDao}
     *
     * @param  name name
     * @return gene sets
     * @see         GeneSetDao GeneSetDao for security filtering
     */
    Collection<GeneSet> findByName( String name );

    /**
     * Security filtering done at DAO level see {@link ubic.gemma.persistence.service.genome.gene.GeneSetDao}
     *
     * @param  name  name
     * @param  taxon taxon
     * @return gene sets
     * @see          GeneSetDao GeneSetDao for security filtering
     */
    Collection<GeneSet> findByName( String name, Taxon taxon );

    /**
     * Load all the genesets with the given IDs Security filtering done at DAO level see
     * {@link ubic.gemma.persistence.service.genome.gene.GeneSetDao}
     *
     * @param  ids ids
     * @return gene sets
     * @see        GeneSetDao GeneSetDao for security filtering
     */
    Collection<GeneSet> load( Collection<Long> ids );

    /**
     * Loads the geneset with the given id Security filtering done at DAO level see
     * {@link ubic.gemma.persistence.service.genome.gene.GeneSetDao}
     *
     * @param  id id
     * @return geneSet with he given ID or null
     * @see       GeneSetDao GeneSetDao for security filtering
     */
    GeneSet load( Long id );

    /**
     * Load all the GeneSets that the user has permission to see. Security filtering done at DAO level see
     * {@link ubic.gemma.persistence.service.genome.gene.GeneSetDao}
     *
     * @return gene sets
     * @see    GeneSetDao GeneSetDao for security filtering
     */
    Collection<GeneSet> loadAll();

    /**
     * Security filtering done at DAO level see {@link ubic.gemma.persistence.service.genome.gene.GeneSetDao}
     *
     * @param  tax taxon
     * @return gene sets
     * @see        GeneSetDao GeneSetDao for security filtering
     */
    Collection<GeneSet> loadAll( Taxon tax );

    /**
     * Returns the {@link GeneSet}s for the currently logged in {@link User} - i.e, ones for which the current user has
     * specific read permissions on (as opposed to data sets which are public). Important: This method will return all
     * gene sets if security is not enabled.
     * Implementation note: Via a methodInvocationFilter. See AclAfterFilterCollectionForMyData for
     * processConfigAttribute. Security filtering done at DAO level see
     * {@link ubic.gemma.persistence.service.genome.gene.GeneSetDao}
     *
     * @return gene sets
     * @see    GeneSetDao GeneSetDao for security filtering
     */
    Collection<GeneSet> loadMyGeneSets();

    /**
     * Security filtering done at DAO level see {@link ubic.gemma.persistence.service.genome.gene.GeneSetDao}
     *
     * @param  tax taxon
     * @return gene sets
     * @see        GeneSetDao GeneSetDao for security filtering
     */
    Collection<GeneSet> loadMyGeneSets( Taxon tax );

    /**
     * Security filtering done at DAO level see {@link ubic.gemma.persistence.service.genome.gene.GeneSetDao}
     *
     * @return gene sets
     * @see    GeneSetDao GeneSetDao for security filtering
     */
    Collection<GeneSet> loadMySharedGeneSets();

    /**
     * Security filtering done at DAO level see {@link ubic.gemma.persistence.service.genome.gene.GeneSetDao}
     *
     * @param  tax taxon
     * @return gene sets
     * @see        GeneSetDao GeneSetDao for security filtering
     */
    Collection<GeneSet> loadMySharedGeneSets( Taxon tax );

    /**
     * Given a collection of genesets remove them all from the db Security filtering done at DAO level see
     * {@link ubic.gemma.persistence.service.genome.gene.GeneSetDao}
     *
     * @param sets gene sets
     * @see        GeneSetDao GeneSetDao for security filtering
     */
    void remove( Collection<GeneSet> sets );

    /**
     * IF the user has permisson to remove the Set, set will be removed. Security filtering done at DAO level see
     * {@link ubic.gemma.persistence.service.genome.gene.GeneSetDao}
     *
     * @param geneset gene set
     * @see           GeneSetDao GeneSetDao for security filtering
     */
    void remove( GeneSet geneset );

    /**
     * Update all the genesets given in the Collection Security filtering done at DAO level see
     * {@link ubic.gemma.persistence.service.genome.gene.GeneSetDao}
     *
     * @param sets gene sets
     * @see        GeneSetDao GeneSetDao for security filtering
     */
    void update( Collection<GeneSet> sets );

    /**
     * Update the given geneset with the new information in the DB Security filtering done at DAO level see
     * {@link ubic.gemma.persistence.service.genome.gene.GeneSetDao}
     *
     * @param geneset gene set
     * @see           GeneSetDao GeneSetDao for security filtering
     */
    void update( GeneSet geneset );

    /**
     * Get a value object for the id param
     *
     * @param  id id
     * @return null if id doesn't match an genes set
     */
    DatabaseBackedGeneSetValueObject getValueObject( Long id );

    /**
     * create an entity in the database based on the value object parameter
     *
     * @param  gsvo gene set value object
     * @return value object converted from the newly created entity
     */
    DatabaseBackedGeneSetValueObject createDatabaseEntity( GeneSetValueObject gsvo );

    /**
     * Given a Gemma Gene Id, find all the gene groups it is a member of (filtering is handled when gene sets are
     * loaded)
     *
     * @param  geneId gene id
     * @return collection of geneSetValueObject
     */
    Collection<DatabaseBackedGeneSetValueObject> findGeneSetsByGene( Long geneId );

    /**
     * AJAX Updates the database entity (permission permitting) with the name and description fields of the param value
     * object
     *
     * @param  geneSetVO gene set value object
     * @return value objects for the updated entities
     */
    DatabaseBackedGeneSetValueObject updateDatabaseEntityNameDesc( DatabaseBackedGeneSetValueObject geneSetVO );

    /**
     * Updates the database record for the param gene set value object (permission permitting) with the members
     * specified of the set, not the name or description etc.
     *
     * @param groupId group id
     * @param geneIds gene ids
     */
    void updateDatabaseEntityMembers( Long groupId, Collection<Long> geneIds );

    /**
     * AJAX Updates the database entity (permission permitting) with the fields of the param value object
     *
     * @param  geneSetVos gene sets
     * @return value objects for the updated entities
     */
    Collection<DatabaseBackedGeneSetValueObject> updateDatabaseEntity(
            Collection<DatabaseBackedGeneSetValueObject> geneSetVos );

    /**
     * Security is handled within method, when the set is loaded
     *
     * @param geneSetVO gene set VO
     */
    void deleteDatabaseEntity( DatabaseBackedGeneSetValueObject geneSetVO );

    /**
     * Security is handled within method
     *
     * @param vos gene set value objects
     */
    void deleteDatabaseEntities( Collection<DatabaseBackedGeneSetValueObject> vos );

    /**
     * @param  privateOnly      only return private sets owned by the user or private sets shared with the user
     * @param  taxonId          if non-null, restrict the groups by ones which have genes in the given taxon (can be
     *                          null)
     * @param  sharedPublicOnly if true, the only public sets returned will be those that are owned by the user or have
     *                          been shared with the user. If param privateOnly is true, this will have no effect.
     * @return all the gene sets user can see, with optional restrictions based on taxon and whether
     *                          the set is public
     *                          or private
     */
    @Secured({ "GROUP_USER" })
    Collection<GeneSet> getUsersGeneGroups( boolean privateOnly, Long taxonId, boolean sharedPublicOnly );

    /**
     * Returns just the current users gene sets
     *
     * @param  privateOnly only private
     * @param  taxonId     if non-null, restrict the groups by ones which have genes in the given taxon.
     * @return gene set value objects
     */
    @Secured({ "GROUP_USER", "AFTER_ACL_VALUE_OBJECT_COLLECTION_READ" })
    Collection<DatabaseBackedGeneSetValueObject> getUsersGeneGroupsValueObjects( boolean privateOnly, Long taxonId );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    Collection<Gene> getGenesInGroup( GeneSet gs );

    /**
     * Get the gene value objects for the members of the group param
     *
     * @param  object can be just a wrapper to trigger security
     * @return gene value object
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    Collection<GeneValueObject> getGenesInGroup( GeneSetValueObject object );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    Collection<Long> getGeneIdsInGroup( GeneSetValueObject geneSetVO );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    int getSize( GeneSet gs );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    int getSize( DatabaseBackedGeneSetValueObject geneSetVO );

    /**
     * @param  query   string to match to gene sets
     * @param  taxonId taxon id
     * @return collection of GeneSetValueObjects that match name query
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_VALUE_OBJECT_COLLECTION_READ" })
    Collection<DatabaseBackedGeneSetValueObject> findGeneSetsByName( String query, Long taxonId );

    /**
     * get the taxon for the gene set parameter, assumes that the taxon of the first gene will be representational of
     * all the genes
     *
     * @param  geneSetVO gene set value object
     * @return the taxon or null if the gene set param was null
     */
    TaxonValueObject getTaxonVOforGeneSetVO( SessionBoundGeneSetValueObject geneSetVO );

    /**
     * get the taxon for the gene set parameter, assumes that the taxon of the first gene will be representational of
     * all the genes
     *
     * @param  geneSet gene set
     * @return the taxon or null if the gene set param was null
     */
    Taxon getTaxon( GeneSet geneSet );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_VALUE_OBJECT_COLLECTION_READ" })
    Collection<DatabaseBackedGeneSetValueObject> getValueObjects( Collection<Long> id );
}