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
package ubic.gemma.persistence.service.genome.taxon;

import org.springframework.security.access.annotation.Secured;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonValueObject;
import ubic.gemma.persistence.service.BaseService;
import ubic.gemma.persistence.service.FilteringVoEnabledService;

import java.util.Collection;

/**
 * @author kelsey
 */
public interface TaxonService extends BaseService<Taxon>, FilteringVoEnabledService<Taxon, TaxonValueObject> {

    Taxon findByCommonName( String commonName );

    Taxon findByScientificName( String scientificName );

    Taxon findByNcbiId( Integer ncbiId );

    @Override
    @Secured({ "GROUP_USER" })
    Taxon findOrCreate( Taxon taxon );

    @Override
    @Secured({ "GROUP_USER" })
    void remove( Collection<Taxon> entities );

    @Override
    @Secured({ "GROUP_ADMIN" })
    void remove( Long id );

    @Override
    @Secured({ "GROUP_USER" })
    void remove( Taxon taxon );

    @Override
    @Secured({ "GROUP_USER" })
    void update( Collection<Taxon> entities );

    @Override
    @Secured({ "GROUP_USER" })
    void update( Taxon taxon );

    /**
     * @return Taxon that have genes loaded into Gemma and that should be used
     */
    Collection<Taxon> loadAllTaxaWithGenes();

    /**
     * @return Taxon that have genes loaded into Gemma and that should be used
     */
    Collection<TaxonValueObject> getTaxaWithGenes();

    /**
     * @return collection of taxa that have expression experiments available.
     */
    Collection<TaxonValueObject> getTaxaWithDatasets();

    /**
     * @return List of taxa with array designs in gemma
     */
    Collection<TaxonValueObject> getTaxaWithArrays();
}
