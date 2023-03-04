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
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.DatabaseBackedGeneSetValueObject;
import ubic.gemma.model.genome.gene.GeneSet;
import ubic.gemma.persistence.service.BaseVoEnabledDao;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collection;
import java.util.List;

/**
 * The interface for managing groupings of genes.
 *
 * @author kelsey
 */
@ParametersAreNonnullByDefault
public interface GeneSetDao extends BaseVoEnabledDao<GeneSet, DatabaseBackedGeneSetValueObject> {

    /**
     * This method does not do any permissions filtering. It assumes that id the user can see the set, they can see all
     * the members.
     *
     * @param id gene set id
     * @return integer count of genes in set
     */
    int getGeneCount( Long id );

    /**
     * Returns the taxon of a random member of the set, the taxon of the set may be a parent taxon of the one returned.
     *
     * @param id id
     * @return taxon of a random member of the set or null
     */
    Taxon getTaxon( Long id );

    /**
     * Returns the {@link GeneSet}s for the currently logged in {@link User} - i.e, ones for which the current user has
     * specific read permissions on (as opposed to data sets which are public). Important: This method will return all
     * gene sets if security is not enabled.
     * Implementation note: Via a methodInvocationFilter. See AclAfterFilterCollectionForMyData for
     * processConfigAttribute.
     *
     * @return gene sets
     */
    @Secured({ "GROUP_USER", "AFTER_ACL_FILTER_MY_DATA" })
    Collection<GeneSet> loadMyGeneSets();

    @Secured({ "GROUP_USER", "AFTER_ACL_FILTER_MY_DATA" })
    Collection<GeneSet> loadMyGeneSets( Taxon tax );

    @Secured({ "GROUP_USER", "AFTER_ACL_FILTER_MY_PRIVATE_DATA" })
    Collection<GeneSet> loadMySharedGeneSets();

    @Secured({ "GROUP_USER", "AFTER_ACL_FILTER_MY_PRIVATE_DATA" })
    Collection<GeneSet> loadMySharedGeneSets( Taxon tax );

    DatabaseBackedGeneSetValueObject loadValueObjectByIdLite( Long id );

    List<DatabaseBackedGeneSetValueObject> loadValueObjectsByIdsLite( Collection<Long> geneSetIds );

    @Override
    @Secured({ "GROUP_USER" })
    Collection<GeneSet> create( final Collection<GeneSet> entities );

    @Secured({ "GROUP_USER" })
    @Override
    GeneSet create( GeneSet geneset );

    @Override
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    Collection<GeneSet> load( Collection<Long> ids );

    @Override
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_READ" })
    GeneSet load( Long id );

    @Override
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    Collection<GeneSet> loadAll();

    @Override
    @Secured({ "GROUP_USER", "ACL_SECURABLE_COLLECTION_EDIT" })
    void remove( Collection<GeneSet> entities );

    @Override
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    void remove( GeneSet entity );

    @Override
    @Secured({ "GROUP_USER", "ACL_SECURABLE_COLLECTION_EDIT" })
    void update( final Collection<GeneSet> entities );

    @Override
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    void update( GeneSet entity );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    Collection<GeneSet> findByGene( Gene gene );

    /**
     * @param name uses the given name to do a name* search in the db
     * @return a collection of geneSets that match the given search term.
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    Collection<GeneSet> findByName( String name );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    Collection<GeneSet> findByName( String name, @Nullable Taxon taxon );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    Collection<GeneSet> loadAll( @Nullable Taxon tax );

    /**
     * @param geneSet gene set
     */
    void thaw( GeneSet geneSet );
}
