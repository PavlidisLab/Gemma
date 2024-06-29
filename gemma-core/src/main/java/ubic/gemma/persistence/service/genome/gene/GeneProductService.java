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
package ubic.gemma.persistence.service.genome.gene;

import org.springframework.security.access.annotation.Secured;
import ubic.gemma.core.lang.Nullable;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.GeneProduct;
import ubic.gemma.model.genome.gene.GeneProductValueObject;
import ubic.gemma.persistence.service.BaseService;
import ubic.gemma.persistence.service.BaseVoEnabledService;

import java.util.Collection;

/**
 * @author kelsey
 */
public interface GeneProductService extends BaseService<GeneProduct>, BaseVoEnabledService<GeneProduct, GeneProductValueObject> {

    @Override
    @Secured({ "GROUP_USER" })
    GeneProduct findOrCreate( GeneProduct geneProduct );

    @Override
    @Secured({ "GROUP_USER" })
    GeneProduct create( GeneProduct entity );

    @Override
    @Secured({ "GROUP_ADMIN" })
    void remove( Collection<GeneProduct> toRemove );

    @Override
    @Secured({ "GROUP_ADMIN" })
    void remove( Long id );

    @Override
    @Secured({ "GROUP_ADMIN" })
    void remove( GeneProduct entity );

    @Override
    @Secured({ "GROUP_USER" })
    void update( Collection<GeneProduct> entities );

    @Override
    @Secured({ "GROUP_USER" })
    void update( GeneProduct entity );

    /**
     * @param search name
     * @return all the genes that share the given gene product name
     */
    Collection<Gene> getGenesByName( String search );

    /**
     * @param search ncbiID
     * @return all the genes that share the given gene product ncbi id
     */
    Collection<Gene> getGenesByNcbiId( String search );

    Collection<GeneProduct> findByName( String name, Taxon taxon );

    @Nullable
    GeneProduct thaw( GeneProduct geneProduct );

    GeneProduct thawOrFail( GeneProduct gp );
}
