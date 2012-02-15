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
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.BaseDao;

/**
 * The interface for managing groupings of genes.
 * 
 * @author kelsey
 * @version $Id$
 */
public interface GeneSetDao extends BaseDao<GeneSet> {

    /**
     * Creates all the the given GeneSets in the given collection
     * 
     * @param sets
     * @return
     */
    @Secured( { "GROUP_USER" })
    Collection<? extends GeneSet> create( final Collection<? extends GeneSet> entities );

    /**
     * Creates the given geneset in the DB
     * 
     * @param geneset
     * @return
     */
    @Secured( { "GROUP_USER" })
    @Override
    GeneSet create( GeneSet geneset );

    /**
     * @param gene
     * @return
     */
    @Secured( { "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    Collection<GeneSet> findByGene( Gene gene );
    
    /**
     * @param name  uses the given name to do a name* search in the db
     * @return a collection of geneSets that match the given search term. 
     */
    @Secured( { "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    Collection<GeneSet> findByName( String name);

    /**
     * @param name
     * @param taxon
     * @return
     */
    @Secured( { "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    Collection<GeneSet> findByName( String name, Taxon taxon );
    
    @Override
    @Secured( { "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_READ" })
    public GeneSet load( Long id );

    @Override
    @Secured( { "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    public Collection<? extends GeneSet> load( Collection<Long> ids );

    @Override
    @Secured( { "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    public Collection<? extends GeneSet> loadAll();

    @Secured( { "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    Collection<GeneSet> loadAll( Taxon tax );

    
    /**
     * Returns the {@link GeneSet}s for the currently logged in {@link User} - i.e, ones for which the current user has
     * specific read permissions on (as opposed to data sets which are public). Important: This method will return all
     * gene sets if security is not enabled.
     * <p>
     * Implementation note: Via a methodInvocationFilter. See AclAfterFilterCollectionForMyData for
     * processConfigAttribute.
     * 
     * @return
     */
    @Secured( { "GROUP_USER", "AFTER_ACL_FILTER_MY_DATA" })
    public Collection<? extends GeneSet> loadMyGeneSets();

    /**
     * @see ubic.gemma.model.genome.gene.GeneSetDao.loadMyGeneSets()
     * @param tax
     * @return
     */
    @Secured( { "GROUP_USER", "AFTER_ACL_FILTER_MY_DATA" })
    public Collection<? extends GeneSet> loadMyGeneSets( Taxon tax );

    /**
     * @return
     */
    @Secured( { "GROUP_USER", "AFTER_ACL_FILTER_MY_PRIVATE_DATA" })
    public Collection<? extends GeneSet> loadMySharedGeneSets();
    
    
    @Override
    @Secured( { "GROUP_USER", "ACL_SECURABLE_COLLECTION_EDIT" })
    public void remove( Collection<? extends GeneSet> entities );

    @Override
    @Secured( { "GROUP_USER", "ACL_SECURABLE_EDIT" })
    public void remove( GeneSet entity );
    
    @Override
    @Secured( { "GROUP_USER", "ACL_SECURABLE_COLLECTION_EDIT" })
    public void update( final Collection<? extends GeneSet> entities );
    
    @Override
    @Secured( { "GROUP_USER", "ACL_SECURABLE_EDIT" })
    public void update( GeneSet entity );

    /**
     * This method does not do any permissions filtering. It assumes that id the user can see the set, they can see all the members.
     * @param id gene set id
     * @return integer count of genes in set
     */
    public int getGeneCount( Long id );

    /**
     * This method does not do any permissions filtering. It assumes that id the user can see the set, they can see all the members.
     * @param id gene set id
     * @return collection of ids for the genes in this set
     */
    public Collection<Long> getGeneIds( Long id );

    /**
     * Returns the taxon id of a random member of the set, the taxon of the set may be a parent taxon of the one returned.
     * @param id
     * @return taxon id of a random member of the set or -1
     */
    public Long getTaxonId( Long id );

    /**
     * Returns the taxon of a random member of the set, the taxon of the set may be a parent taxon of the one returned.
     * @param id
     * @return taxon of a random member of the set or null
     */    
    public Taxon getTaxon( Long id );

}
