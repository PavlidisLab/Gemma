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

import org.springframework.stereotype.Service;

/**
 * @author keshav
 * @version $Id$
 */
@Service
public class TaxonServiceImpl extends TaxonServiceBase {

    /**
     * @see TaxonService#create(Taxon)
     */
    protected Taxon handleCreate( Taxon taxon ) throws java.lang.Exception {
        return this.getTaxonDao().create( taxon );
    }

    /**
     * @see TaxonService#find(Taxon)
     */
    @Override
    protected Taxon handleFind( Taxon taxon ) throws java.lang.Exception {
        return this.getTaxonDao().find( taxon );
    }

    @Override
    protected Taxon handleFindByAbbreviation( String abbreviation ) throws Exception {
        return this.getTaxonDao().findByAbbreviation( abbreviation );
    }

    @Override
    protected Taxon handleFindByCommonName( String commonName ) throws Exception {
        return this.getTaxonDao().findByCommonName( commonName );
    }

    @Override
    protected Taxon handleFindByScientificName( String scientificName ) throws Exception {
        return this.getTaxonDao().findByScientificName( scientificName );
    }

    /**
     * @see TaxonService#findChildTaxaByParent(Taxon)
     */
    @Override
    protected Collection<Taxon> handleFindChildTaxaByParent( Taxon taxon ) throws java.lang.Exception {
        return this.getTaxonDao().findChildTaxaByParent( taxon );
    }

    @Override
    protected Taxon handleFindOrCreate( Taxon taxon ) throws Exception {
        return this.getTaxonDao().findOrCreate( taxon );
    }

    @Override
    protected Taxon handleLoad( Long id ) throws Exception {
        return this.getTaxonDao().load( id );
    }

    @Override
    protected Collection<Taxon> handleLoadAll() throws Exception {
        return ( Collection<Taxon> ) this.getTaxonDao().loadAll();
    }

    /**
     * @see TaxonService#remove(Taxon)
     */
    @Override
    protected void handleRemove( Taxon taxon ) throws java.lang.Exception {
        this.getTaxonDao().remove( taxon );
    }

    /**
     * @see TaxonService#update(Taxon)
     */
    @Override
    protected void handleUpdate( Taxon taxon ) throws java.lang.Exception {
        this.getTaxonDao().update( taxon );
    }

   
    @Override
    protected void handleThaw( Taxon taxon ) throws Exception {
        this.getTaxonDao().thaw( taxon );        
    }

}