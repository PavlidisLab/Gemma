/*
 * The Gemma project.
 * 
 * Copyright (c) 2006-2007 University of British Columbia
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
package ubic.gemma.model.association;

import java.util.Collection;

import org.springframework.security.access.annotation.Secured;

import ubic.gemma.model.genome.Gene;

/**
 * Service class for Gene2GeneProteinAssociation classes.
 * 
 * @author ldonnison
 * @version $Id$
 */
public interface Gene2GeneProteinAssociationService {

    /**
     * Create a gene2geneProteinAssociation
     */
    @Secured({ "GROUP_ADMIN" })
    public Gene2GeneProteinAssociation create( Gene2GeneProteinAssociation gene2GeneProteinAssociation );

    /**
     * delete the given gene2geneProteinAssociation
     */
    @Secured({ "GROUP_ADMIN" })
    public void delete( Gene2GeneProteinAssociation gene2GeneProteinAssociation );

    /**
     * Delete all gene2geneProteinAssociation
     */
    @Secured({ "GROUP_ADMIN" })
    public void deleteAll( Collection<Gene2GeneProteinAssociation> gene2GeneProteinAssociation );

    /**
     * Find a gene2geneProteinAssociation
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_READ" })
    public Gene2GeneProteinAssociation find( Gene2GeneProteinAssociation gene2GeneProteinAssociation );

    /**
     * Load all gene2geneProteinAssociation
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_READ" })
    public Collection<Gene2GeneProteinAssociation> loadAll();

    /**
     * Does a 'thaw' of a Gene2GeneProteinAssociation
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_READ" })
    public void thaw( Gene2GeneProteinAssociation association );

    /**
     * Finds Gene2GeneProteinAssociation for a given gene
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_READ" })
    Collection<Gene2GeneProteinAssociation> findProteinInteractionsForGene( Gene gene );

}
