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
import java.util.HashSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.sequenceAnalysis.AnnotationAssociation;
import ubic.gemma.model.genome.sequenceAnalysis.BlatAssociation;

/**
 * @see ubic.gemma.model.genome.gene.GeneProductService
 */
@Service
public class GeneProductServiceImpl extends ubic.gemma.model.genome.gene.GeneProductServiceBase {

    private static Logger log = LoggerFactory.getLogger( GeneProductServiceImpl.class );

    @Override
    protected Integer handleCountAll() {
        return this.getGeneProductDao().countAll();
    }

    /**
     * @see ubic.gemma.model.genome.gene.GeneProductService#create(ubic.gemma.model.genome.gene.GeneProduct)
     */
    @Override
    protected ubic.gemma.model.genome.gene.GeneProduct handleCreate(
            ubic.gemma.model.genome.gene.GeneProduct geneProduct ) {
        return this.getGeneProductDao().create( geneProduct );
    }

    /**
     * @see ubic.gemma.model.genome.gene.GeneProductService#delete(ubic.gemma.model.genome.gene.GeneProduct)
     */
    @Override
    protected void handleDelete( ubic.gemma.model.genome.gene.GeneProduct geneProduct ) {
        this.getGeneProductDao().remove( geneProduct );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.gene.GeneProductServiceBase#handleFind(ubic.gemma.model.genome.gene.GeneProduct)
     */
    @Override
    protected ubic.gemma.model.genome.gene.GeneProduct handleFind( ubic.gemma.model.genome.gene.GeneProduct gProduct ) {
        return this.getGeneProductDao().find( gProduct );
    }

    /**
     * @see ubic.gemma.model.genome.gene.GeneProductService#findOrCreate(ubic.gemma.model.genome.gene.GeneProduct)
     */
    @Override
    protected ubic.gemma.model.genome.gene.GeneProduct handleFindOrCreate(
            ubic.gemma.model.genome.gene.GeneProduct geneProduct ) {
        return this.getGeneProductDao().findOrCreate( geneProduct );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.gene.GeneProductServiceBase#handleGetGenesByName(java.lang.String)
     */
    @Override
    protected Collection<Gene> handleGetGenesByName( String search ) {
        return this.getGeneProductDao().getGenesByName( search );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.gene.GeneProductServiceBase#handleGetGenesByNcbiId(java.lang.String)
     */
    @Override
    protected Collection<Gene> handleGetGenesByNcbiId( String search ) {
        return this.getGeneProductDao().getGenesByNcbiId( search );
    }

    /**
     * @see ubic.gemma.model.genome.gene.GeneProductService#load(java.lang.Long)
     */
    @Override
    protected ubic.gemma.model.genome.gene.GeneProduct handleLoad( java.lang.Long id ) {
        return this.getGeneProductDao().load( id );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.gene.GeneProductServiceBase#handleLoadMultiple(java.util.Collection)
     */
    @Override
    protected Collection<GeneProduct> handleLoadMultiple( Collection<Long> ids ) {
        return ( Collection<GeneProduct> ) this.getGeneProductDao().load( ids );
    }

    /**
     * @see ubic.gemma.model.genome.gene.GeneProductService#update(ubic.gemma.model.genome.gene.GeneProduct)
     */
    @Override
    protected void handleUpdate( ubic.gemma.model.genome.gene.GeneProduct geneProduct ) {
        this.getGeneProductDao().update( geneProduct );
    }

    @Override
    public GeneProduct thaw( GeneProduct existing ) {
        return this.getGeneProductDao().thaw( existing );
    }

    @Override
    public void remove( Collection<GeneProduct> toRemove ) {
        Collection<? extends BlatAssociation> associations = this.getBlatAssociationDao().find( toRemove );
        if ( !associations.isEmpty() ) {
            log.info( "Removing " + associations.size() + " blat associations involving up to " + toRemove.size()
                    + " products." );
            this.getBlatAssociationDao().remove( associations );
        }

        Collection<AnnotationAssociation> annotationAssociations = this.getAnnotationAssociationDao().find( toRemove );
        if ( !annotationAssociations.isEmpty() ) {
            log.info( "Removing " + annotationAssociations.size() + " annotationAssociations involving up to "
                    + toRemove.size() + " products." );
            this.getAnnotationAssociationDao().remove( annotationAssociations );
        }

        // might need to add referenceAssociations also.

        // remove associations to database entries that are still associated with sequences.
        for ( GeneProduct gp : toRemove ) {
            GeneProduct tgp = this.thaw( gp );
            Collection<DatabaseEntry> accessions = tgp.getAccessions();
            Collection<DatabaseEntry> toRelease = new HashSet<DatabaseEntry>();
            for ( DatabaseEntry de : accessions ) {
                if ( this.getBioSequenceDao().findByAccession( de ) != null ) {
                    toRelease.add( de );
                }
            }
            tgp.getAccessions().removeAll( toRelease );
            this.getGeneProductDao().remove( tgp );

        }

    }

    @Override
    public GeneProduct findByGi( String ncbiGi ) {
        return this.getGeneProductDao().findByNcbiId( ncbiGi );
    }

    @Override
    public Collection<GeneProduct> findByName( String name, Taxon taxon ) {
        return this.getGeneProductDao().findByName( name, taxon );
    }

}