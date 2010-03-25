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

import ubic.gemma.model.genome.Gene;

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
     * @return geneSet witht he given ID
     */
    @Secured( { "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_READ" })
    public GeneSet load( Long id );

    /**
     * Load all the GeneSets that the user are the permissions to see.
     * 
     * @param id
     * @return
     */
    @Secured( { "GROUP_ADMIN", "AFTER_ACL_COLLECTION_READ" })
    public Collection<GeneSet> loadAll();

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
     * Returns the {@link GeneSet}s for the currently logged in {@link User} - i.e, ones for which the
     * current user has specific read permissions on (as opposed to data sets which are public). Important: This method
     * will return all gene sets if security is not enabled.
     * <p>
     * Implementation note: Via a methodInvocationFilter. See AclAfterFilterCollectionForMyData for
     * processConfigAttribute. (in Gemma-core)
     * 
     * @return
     */
    @Secured( { "GROUP_USER", "AFTER_ACL_FILTER_MY_DATA" })
    public Collection<GeneSet> loadMyGeneSets();
    


}