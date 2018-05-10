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
package ubic.gemma.persistence.service.association;

import org.springframework.security.access.annotation.Secured;
import ubic.gemma.model.association.Gene2GeneProteinAssociation;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.persistence.service.BaseService;

import java.util.Collection;

/**
 * Service class for Gene2GeneProteinAssociation classes.
 *
 * @author ldonnison
 */
public interface Gene2GeneProteinAssociationService extends BaseService<Gene2GeneProteinAssociation> {

    @Secured({ "GROUP_ADMIN" })
    void removeAll( Collection<Gene2GeneProteinAssociation> gene2GeneProteinAssociation );

    @Override
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_READ" })
    Gene2GeneProteinAssociation find( Gene2GeneProteinAssociation gene2GeneProteinAssociation );

    @Override
    @Secured({ "GROUP_ADMIN" })
    Gene2GeneProteinAssociation create( Gene2GeneProteinAssociation gene2GeneProteinAssociation );

    @Override
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_READ" })
    Collection<Gene2GeneProteinAssociation> loadAll();

    @Override
    @Secured({ "GROUP_ADMIN" })
    void remove( Gene2GeneProteinAssociation gene2GeneProteinAssociation );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_READ" })
    void thaw( Gene2GeneProteinAssociation association );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_READ" })
    Collection<Gene2GeneProteinAssociation> findProteinInteractionsForGene( Gene gene );

}
