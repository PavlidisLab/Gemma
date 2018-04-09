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
import ubic.gemma.model.association.Gene2GOAssociation;
import ubic.gemma.model.common.description.VocabCharacteristic;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.BaseService;

import java.util.Collection;
import java.util.Map;

/**
 * @author kelsey
 */
public interface Gene2GOAssociationService extends BaseService<Gene2GOAssociation> {

    @Override
    @Secured({ "GROUP_ADMIN" })
    Gene2GOAssociation findOrCreate( Gene2GOAssociation gene2GOAssociation );

    @Override
    @Secured({ "GROUP_ADMIN" })
    Gene2GOAssociation create( Gene2GOAssociation gene2GOAssociation );

    Collection<Gene2GOAssociation> findAssociationByGene( Gene gene );

    Collection<VocabCharacteristic> findByGene( Gene gene );

    Map<Gene, Collection<VocabCharacteristic>> findByGenes( Collection<Gene> genes );

    Collection<Gene> findByGOTerm( String goID, Taxon taxon );

    @Secured({ "GROUP_ADMIN" })
    void removeAll();

    /**
     * @param termsToFetch terms
     * @param taxon        constraint
     * @return all the genes that match any of the terms. Used to fetch genes associated with a term + children.
     */
    Collection<Gene> findByGOTerms( Collection<String> termsToFetch, Taxon taxon );

    Map<Taxon, Collection<Gene>> findByGOTermsPerTaxon( Collection<String> termsToFetch );

}
