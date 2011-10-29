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
package ubic.gemma.model.genome.gene;

import java.util.Collection;

import org.springframework.stereotype.Service;

import ubic.gemma.model.genome.Gene;

/**
 * @see ubic.gemma.model.genome.gene.GeneProductService
 */
@Service
public class GeneProductServiceImpl extends ubic.gemma.model.genome.gene.GeneProductServiceBase {

    @Override
    protected Integer handleCountAll() throws Exception {
        return this.getGeneProductDao().countAll();
    }

    /**
     * @see ubic.gemma.model.genome.gene.GeneProductService#create(ubic.gemma.model.genome.gene.GeneProduct)
     */
    @Override
    protected ubic.gemma.model.genome.gene.GeneProduct handleCreate(
            ubic.gemma.model.genome.gene.GeneProduct geneProduct ) throws java.lang.Exception {
        return this.getGeneProductDao().create( geneProduct );
    }

    /**
     * @see ubic.gemma.model.genome.gene.GeneProductService#delete(ubic.gemma.model.genome.gene.GeneProduct)
     */
    @Override
    protected void handleDelete( ubic.gemma.model.genome.gene.GeneProduct geneProduct ) throws java.lang.Exception {
        this.getGeneProductDao().remove( geneProduct );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.gene.GeneProductServiceBase#handleFind(ubic.gemma.model.genome.gene.GeneProduct)
     */
    @Override
    protected ubic.gemma.model.genome.gene.GeneProduct handleFind( ubic.gemma.model.genome.gene.GeneProduct gProduct )
            throws Exception {
        return this.getGeneProductDao().find( gProduct );
    }

    /**
     * @see ubic.gemma.model.genome.gene.GeneProductService#findOrCreate(ubic.gemma.model.genome.gene.GeneProduct)
     */
    @Override
    protected ubic.gemma.model.genome.gene.GeneProduct handleFindOrCreate(
            ubic.gemma.model.genome.gene.GeneProduct geneProduct ) throws java.lang.Exception {
        return this.getGeneProductDao().findOrCreate( geneProduct );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.gene.GeneProductServiceBase#handleGetGenesByName(java.lang.String)
     */
    @Override
    protected Collection<Gene> handleGetGenesByName( String search ) throws Exception {
        return this.getGeneProductDao().getGenesByName( search );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.gene.GeneProductServiceBase#handleGetGenesByNcbiId(java.lang.String)
     */
    @Override
    protected Collection<Gene> handleGetGenesByNcbiId( String search ) throws Exception {
        return this.getGeneProductDao().getGenesByNcbiId( search );
    }

    /**
     * @see ubic.gemma.model.genome.gene.GeneProductService#load(java.lang.Long)
     */
    @Override
    protected ubic.gemma.model.genome.gene.GeneProduct handleLoad( java.lang.Long id ) throws java.lang.Exception {
        return this.getGeneProductDao().load( id );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.gene.GeneProductServiceBase#handleLoadMultiple(java.util.Collection)
     */
    @Override
    protected Collection<GeneProduct> handleLoadMultiple( Collection<Long> ids ) throws Exception {
        return this.getGeneProductDao().load( ids );
    }

    /**
     * @see ubic.gemma.model.genome.gene.GeneProductService#update(ubic.gemma.model.genome.gene.GeneProduct)
     */
    @Override
    protected void handleUpdate( ubic.gemma.model.genome.gene.GeneProduct geneProduct ) throws java.lang.Exception {
        this.getGeneProductDao().update( geneProduct );
    }

    @Override
    public GeneProduct thaw( GeneProduct existing ) {
        return this.getGeneProductDao().thaw( existing );
    }

    @Override
    public void remove( Collection<GeneProduct> toRemove ) {

        this.getBlatAssociationDao().remove( this.getBlatAssociationDao().find( toRemove ) );
        this.getAnnotationAssociationDao().remove( this.getAnnotationAssociationDao().find( toRemove ) );

        this.getGeneProductDao().remove( toRemove );
    }

    @Override
    public GeneProduct findByGi( String ncbiGi ) {
        return this.getGeneProductDao().findByNcbiId( ncbiGi );
    }

}