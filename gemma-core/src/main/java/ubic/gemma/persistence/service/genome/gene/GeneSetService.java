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
package ubic.gemma.persistence.service.genome.gene;

import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.userdetails.User;
import ubic.gemma.model.genome.gene.SessionBoundGeneSetValueObject;
import ubic.gemma.core.search.SearchException;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonValueObject;
import ubic.gemma.model.genome.gene.DatabaseBackedGeneSetValueObject;
import ubic.gemma.model.genome.gene.GeneSet;
import ubic.gemma.model.genome.gene.GeneSetValueObject;
import ubic.gemma.model.genome.gene.GeneValueObject;
import ubic.gemma.persistence.service.BaseService;
import ubic.gemma.persistence.service.BaseVoEnabledService;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Service for managing gene sets
 *
 * @author kelsey, paul
 */
@ParametersAreNonnullByDefault
public interface GeneSetService extends BaseService<GeneSet>, BaseVoEnabledService<GeneSet, DatabaseBackedGeneSetValueObject> {

    @Override
    @Secured({ "GROUP_USER" })
    Collection<GeneSet> create( Collection<GeneSet> sets );

    @Override
    @Secured({ "GROUP_USER" })
    GeneSet create( GeneSet geneset );

    @Override
    @Secured({ "GROUP_USER" })
    GeneSet save( GeneSet entity );

    @Override
    @Secured({ "GROUP_USER" })
    Collection<GeneSet> save( Collection<GeneSet> entities );

    /**
     * Return all sets that contain the given gene.
     * {@link ubic.gemma.persistence.service.genome.gene.GeneSetDao}
     *
     * @param  gene gene
     * @return gene sets
     * @see         GeneSetDao GeneSetDao for security filtering
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    Collection<GeneSet> findByGene( Gene gene );

    @Nullable
    @Override
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_VALUE_OBJECT_READ" })
    DatabaseBackedGeneSetValueObject loadValueObject( GeneSet geneSet );

    @Nullable
    @Override
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_VALUE_OBJECT_READ" })
    DatabaseBackedGeneSetValueObject loadValueObjectById( Long entityId );

    @Nullable
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_VALUE_OBJECT_READ" })
    DatabaseBackedGeneSetValueObject loadValueObjectByIdLite( Long id );

    @Override
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_VALUE_OBJECT_COLLECTION_READ" })
    List<DatabaseBackedGeneSetValueObject> loadValueObjects( Collection<GeneSet> entities );

    /**
     * Ids of member genes will be filled in
     *
     * @param  ids ids
     * @return gene set value object
     */
    @Override
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_VALUE_OBJECT_COLLECTION_READ" })
    List<DatabaseBackedGeneSetValueObject> loadValueObjectsByIds( Collection<Long> ids );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_VALUE_OBJECT_COLLECTION_READ" })
    List<DatabaseBackedGeneSetValueObject> loadValueObjectsByIdsLite( Collection<Long> geneSetIds );

    @Override
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_VALUE_OBJECT_COLLECTION_READ" })
    List<DatabaseBackedGeneSetValueObject> loadAllValueObjects();

    /**
     *
     * @param  name name
     * @return gene sets
     * @see         GeneSetDao GeneSetDao for security filtering
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    Collection<GeneSet> findByName( String name );

    /**
     *
     * @param  name  name
     * @param  taxon taxon
     * @return gene sets
     * @see          GeneSetDao GeneSetDao for security filtering
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    Collection<GeneSet> findByName( String name, Taxon taxon );

    @Override
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    Collection<GeneSet> load( Collection<Long> ids );

    @Nonnull
    @Override
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_READ" })
    GeneSet loadOrFail( Long id ) throws NullPointerException;

    @Override
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_READ" })
    GeneSet load( Long id );

    @Override
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    Collection<GeneSet> loadAll();

    /**
     *
     * @param  tax taxon
     * @return gene sets
     * @see        GeneSetDao GeneSetDao for security filtering
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    Collection<GeneSet> loadAll( @Nullable Taxon tax );

    /**
     * Returns the {@link GeneSet}s for the currently logged in {@link User} - i.e, ones for which the current user has
     * specific read permissions on (as opposed to data sets which are public). Important: This method will return all
     * gene sets if security is not enabled.
     * Implementation note: Via a methodInvocationFilter. See AclAfterFilterCollectionForMyData for
     * processConfigAttribute.
     *
     * @return gene sets
     * @see    GeneSetDao GeneSetDao for security filtering
     */
    @Secured({ "GROUP_USER", "AFTER_ACL_FILTER_MY_DATA" })
    Collection<GeneSet> loadMyGeneSets();

    /**
     *
     * @param  tax taxon
     * @return gene sets
     * @see        GeneSetDao GeneSetDao for security filtering
     */
    @Secured({ "GROUP_USER", "AFTER_ACL_FILTER_MY_DATA" })
    Collection<GeneSet> loadMyGeneSets( Taxon tax );

    /**
     *
     * @param  tax taxon
     * @return gene sets
     * @see        GeneSetDao GeneSetDao for security filtering
     */
    @Secured({ "GROUP_USER", "AFTER_ACL_FILTER_MY_PRIVATE_DATA" })
    Collection<GeneSet> loadMySharedGeneSets( Taxon tax );

    /**
     * create an entity in the database based on the value object parameter
     *
     * @param  gsvo gene set value object
     * @return value object converted from the newly created entity
     */
    GeneSetValueObject createDatabaseEntity( GeneSetValueObject gsvo );

    /**
     * Given a Gemma Gene Id, find all the gene groups it is a member of (filtering is handled when gene sets are
     * loaded)
     *
     * @param  geneId gene id
     * @return collection of geneSetValueObject
     */
    Collection<GeneSetValueObject> findGeneSetsByGene( Long geneId );

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
    int getSize( GeneSetValueObject geneSetVO );

    /**
     * @param  query   string to match to gene sets
     * @param  taxonId taxon id
     * @return collection of GeneSetValueObjects that match name query
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_VALUE_OBJECT_COLLECTION_READ" })
    Collection<GeneSetValueObject> findGeneSetsByName( String query, @Nullable Long taxonId ) throws SearchException;

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
     * @return a taxon, or null if the gene set has no member
     */
    @Nullable
    Taxon getTaxon( GeneSet geneSet );

    /**
     * Obtain all the taxa for the members of a given gene set.
     */
    Set<Taxon> getTaxa( GeneSet geneSet );

    @Override
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    void update( GeneSet entity );

    @Override
    @Secured({ "GROUP_USER", "ACL_SECURABLE_COLLECTION_EDIT" })
    void update( Collection<GeneSet> entities );

    @Override
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    void remove( GeneSet entity );

    @Override
    @Secured({ "GROUP_USER", "ACL_SECURABLE_COLLECTION_EDIT" })
    void remove( Collection<GeneSet> entities );

    @Override
    @Secured({ "GROUP_ADMIN" })
    void remove( Long id );

    @Secured({ "GROUP_ADMIN" })
    int removeAll();
}