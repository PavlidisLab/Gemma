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
package ubic.gemma.model.genome;

import java.util.Collection;

/**
 * @author keshav
 * @version $Id$
 */
public class TaxonServiceImpl extends ubic.gemma.model.genome.TaxonServiceBase {

    /**
     * @see ubic.gemma.model.genome.TaxonService#find(ubic.gemma.model.genome.Taxon)
     */
    @Override
    protected ubic.gemma.model.genome.Taxon handleFind( ubic.gemma.model.genome.Taxon taxon )
            throws java.lang.Exception {
        return this.getTaxonDao().find( taxon );
    }

    /**
     * @see ubic.gemma.model.genome.TaxonService#update(ubic.gemma.model.genome.Taxon)
     */
    @Override
    protected void handleUpdate( ubic.gemma.model.genome.Taxon taxon ) throws java.lang.Exception {
        this.getTaxonDao().update( taxon );
    }

    /**
     * @see ubic.gemma.model.genome.TaxonService#remove(ubic.gemma.model.genome.Taxon)
     */
    @Override
    protected void handleRemove( ubic.gemma.model.genome.Taxon taxon ) throws java.lang.Exception {
        this.getTaxonDao().remove( taxon );
    }

    /**
     * @see ubic.gemma.model.genome.TaxonService#create(ubic.gemma.model.genome.Taxon)
     */
    protected ubic.gemma.model.genome.Taxon handleCreate( ubic.gemma.model.genome.Taxon taxon )
            throws java.lang.Exception {
        return this.getTaxonDao().create( taxon );
    }

    @Override
    protected Taxon handleFindOrCreate( Taxon taxon ) throws Exception {
        return this.getTaxonDao().findOrCreate( taxon );
    }

    @Override
    protected Taxon handleFindByScientificName( String scientificName ) throws Exception {
        return this.getTaxonDao().findByScientificName( scientificName );
    }

    @Override
    protected Taxon handleFindByCommonName( String commonName ) throws Exception {
        return this.getTaxonDao().findByCommonName( commonName );
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Collection<Taxon> handleLoadAll() throws Exception {
        return this.getTaxonDao().loadAll();
    }

    @Override
    protected Taxon handleLoad( Long id ) throws Exception {
        return this.getTaxonDao().load( id );
    }

}