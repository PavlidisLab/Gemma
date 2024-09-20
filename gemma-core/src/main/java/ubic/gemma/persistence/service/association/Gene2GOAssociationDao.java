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

import ubic.gemma.model.association.Gene2GOAssociation;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.BaseDao;

import java.util.Collection;
import java.util.Map;

/**
 * @see Gene2GOAssociation
 */
public interface Gene2GOAssociationDao extends BaseDao<Gene2GOAssociation> {

    @Override
    Gene2GOAssociation find( Gene2GOAssociation gene2GOAssociation );

    @Override
    Gene2GOAssociation findOrCreate( Gene2GOAssociation gene2GOAssociation );

    Collection<Gene2GOAssociation> findAssociationByGene( Gene gene );

    Collection<Gene2GOAssociation> findAssociationByGenes( Collection<Gene> genes );

    Collection<Characteristic> findByGene( Gene gene );

    Map<Gene, Collection<Characteristic>> findByGenes( Collection<Gene> genes );

    Collection<Gene> findByGoTermUris( Collection<String> uris );

    Collection<Gene> findByGoTermUris( Collection<String> uris, Taxon taxon );

    Map<Taxon, Collection<Gene>> findByGoTermUrisPerTaxon( Collection<String> uris );

    int removeAll();
}
