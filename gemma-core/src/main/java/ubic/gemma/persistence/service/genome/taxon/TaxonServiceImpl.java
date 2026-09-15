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
package ubic.gemma.persistence.service.genome.taxon;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonValueObject;
import ubic.gemma.persistence.service.AbstractFilteringVoEnabledService;

import java.util.Collection;

/**
 * @author keshav
 * @see    TaxonService
 */
@Service
public class TaxonServiceImpl extends AbstractFilteringVoEnabledService<Taxon, TaxonValueObject> implements TaxonService {

    private final TaxonDao taxonDao;

    @Autowired
    private TaxonReadService readService;

    @Autowired
    public TaxonServiceImpl( TaxonDao taxonDao ) {
        super( taxonDao );
        this.taxonDao = taxonDao;
    }

    // =====================================================================
    // Read methods -- delegate to TaxonReadService.
    // ACL @Secured annotations live on the TaxonService interface and apply
    // at the facade proxy boundary. (No @Secured is declared on read methods
    // today.)
    // =====================================================================

    @Override
    public Taxon findByCommonName( final String commonName ) {
        return readService.findByCommonName( commonName );
    }

    @Override
    public Taxon findByScientificName( final String scientificName ) {
        return readService.findByScientificName( scientificName );
    }

    @Override
    public Taxon findByNcbiId( final Integer ncbiId ) {
        return readService.findByNcbiId( ncbiId );
    }

    @Override
    public Collection<Taxon> loadAllTaxaWithGenes() {
        return readService.loadAllTaxaWithGenes();
    }

    @Override
    public Collection<TaxonValueObject> getTaxaWithGenes() {
        return readService.getTaxaWithGenes();
    }

    @Override
    public Collection<TaxonValueObject> getTaxaWithDatasets() {
        return readService.getTaxaWithDatasets();
    }

    @Override
    public Collection<TaxonValueObject> getTaxaWithArrays() {
        return readService.getTaxaWithArrays();
    }

    // =====================================================================
    // Write methods -- stay on the facade.
    // =====================================================================

    @Override
    @Transactional
    public void updateGenesUsable( Taxon taxon, boolean isGenesUsable ) {
        taxon.setIsGenesUsable( isGenesUsable );
        update( taxon );
    }
}
