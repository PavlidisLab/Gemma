/*
 * The Gemma project.
 *
 * Copyright (c) 2006 University of British Columbia
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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.association.Gene2GOAssociation;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.AbstractService;

import java.util.Collection;
import java.util.Map;

/**
 * @author klc
 * @see    Gene2GOAssociationService
 * @see    Gene2GOAssociationReadService
 */
@Service
public class Gene2GOAssociationServiceImpl extends AbstractService<Gene2GOAssociation>
        implements Gene2GOAssociationService {

    private final Gene2GOAssociationDao gene2GOAssociationDao;
    private final Gene2GOAssociationReadService gene2GOAssociationReadService;

    @Autowired
    public Gene2GOAssociationServiceImpl( Gene2GOAssociationDao mainDao,
            Gene2GOAssociationReadService gene2GOAssociationReadService ) {
        super( mainDao );
        this.gene2GOAssociationDao = mainDao;
        this.gene2GOAssociationReadService = gene2GOAssociationReadService;
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<Gene2GOAssociation> findAssociationByGene( Gene gene ) {
        return gene2GOAssociationReadService.findAssociationByGene( gene );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<Gene2GOAssociation> findAssociationByGenes( Collection<Gene> genes ) {
        return gene2GOAssociationReadService.findAssociationByGenes( genes );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<Characteristic> findByGene( Gene gene ) {
        return gene2GOAssociationReadService.findByGene( gene );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Gene, Collection<Characteristic>> findByGenes( Collection<Gene> genes ) {
        return gene2GOAssociationReadService.findByGenes( genes );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<Gene> findByGOTermUris( Collection<String> uris, @Nullable Taxon taxon ) {
        return gene2GOAssociationReadService.findByGOTermUris( uris, taxon );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Taxon, Collection<Gene>> findByGOTermUrisPerTaxon( Collection<String> uris ) {
        return gene2GOAssociationReadService.findByGOTermUrisPerTaxon( uris );
    }

    @Override
    @Transactional
    public int removeAll() {
        return this.gene2GOAssociationDao.removeAll();
    }
}
