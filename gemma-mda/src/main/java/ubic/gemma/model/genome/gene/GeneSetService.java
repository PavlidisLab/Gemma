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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;

/**
 * Service for managing gene sets
 * 
 * @author kelsey
 * @version $Id: GeneSetService.java,
 */
@Service
public class GeneSetService {

    @Autowired
    GeneSetDao geneSetDao = null;

    @Autowired
    GeneSetMemberDao geneSetMember = null;

    /**
     * Creates the given geneset in the DB
     * 
     * @param geneset
     * @return
     */
    @Secured( { "GROUP_USER" })
    public GeneSet create( GeneSet geneset ) {

        return this.geneSetDao.create( geneset );
    }

    /**
     * Creates all the the given GeneSets in the given collection
     * 
     * @param sets
     * @return
     */
    @SuppressWarnings("unchecked")
    @Secured( { "GROUP_USER" })
    public Collection<GeneSet> create( Collection<GeneSet> sets ) {

        return ( Collection<GeneSet> ) this.geneSetDao.create( sets );

    }

    /**
     * IF the user has permisson to remove the Set, set will be removed.
     * 
     * @param geneset
     */
    @Secured( { "GROUP_USER", "ACL_SECURABLE_EDIT" })
    public void remove( GeneSet geneset ) {

        this.geneSetDao.remove( geneset );
    }

    /**
     * Given a collection of genesets remove them all from the db
     * 
     * @param sets
     */
    @Secured( { "GROUP_USER", "ACL_SECURABLE_COLLECTION_EDIT" })
    public void remove( Collection<GeneSet> sets ) {

        this.geneSetDao.remove( sets );
    }

    /**
     * Update the given geneset with the new information in the DB
     * 
     * @param geneset
     */
    @Secured( { "GROUP_USER", "ACL_SECURABLE_EDIT" })
    public void update( GeneSet geneset ) {
        this.geneSetDao.update( geneset );

    }

    /**
     * Update all the genesets given in the Collection
     * 
     * @param sets
     */
    @Secured( { "GROUP_USER", "ACL_SECURABLE_COLLECTION_EDIT" })
    public void update( Collection<GeneSet> sets ) {
        this.geneSetDao.update( sets );

    }

    /**
     * Loads the geneset with the given id
     * 
     * @param id
     * @return geneSet witht he given ID
     */
    @Secured( { "GROUP_USER", "AFTER_ACL_READ" })
    public GeneSet load( Long id ) {

        return this.geneSetDao.load( id );
    }

    /**
     * Load all the genesets with the given IDs
     * 
     * @param ids
     * @return
     */
    @SuppressWarnings("unchecked")
    @Secured( { "GROUP_USER", "AFTER_ACL_COLLECTION_READ" })
    public Collection<GeneSet> load( Collection<Long> ids ) {
        return ( Collection<GeneSet> ) this.geneSetDao.load( ids );

    }

    /**
     * Load all the GeneSets that the user are the permissions to see.
     * 
     * @param id
     * @return
     */
    @SuppressWarnings("unchecked")
    @Secured( { "GROUP_USER", "AFTER_ACL_COLLECTION_READ" })
    public Collection<GeneSet> loadAll() {

        return ( Collection<GeneSet> ) this.geneSetDao.loadAll();
    }

    public void setGeneSetDao( GeneSetDao geneSetDao ) {
        this.geneSetDao = geneSetDao;
    }

    public void setGeneSetMember( GeneSetMemberDao geneSetMember ) {
        this.geneSetMember = geneSetMember;
    }
}
