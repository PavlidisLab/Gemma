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
import org.springframework.transaction.annotation.Transactional;

import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.sequenceAnalysis.AnnotationAssociation;
import ubic.gemma.model.genome.sequenceAnalysis.BlatAssociation;

/**
 * @see GeneProductService
 */
@Service
public class GeneProductServiceImpl extends GeneProductServiceBase {

    private static Logger log = LoggerFactory.getLogger( GeneProductServiceImpl.class );

    @Override
    @Transactional(readOnly = true)
    public GeneProduct findByGi( String ncbiGi ) {
        return this.getGeneProductDao().findByNcbiId( ncbiGi );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<GeneProduct> findByName( String name, Taxon taxon ) {
        return this.getGeneProductDao().findByName( name, taxon );
    }

    @Override
    @Transactional
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
    @Transactional(readOnly = true)
    public GeneProduct thaw( GeneProduct existing ) {
        return this.getGeneProductDao().thaw( existing );
    }

    @Override
    protected Integer handleCountAll() {
        return this.getGeneProductDao().countAll();
    }

    /**
     * @see GeneProductService#create(GeneProduct)
     */
    @Override
    protected GeneProduct handleCreate( GeneProduct geneProduct ) {
        return this.getGeneProductDao().create( geneProduct );
    }

    /**
     * @see GeneProductService#delete(GeneProduct)
     */
    @Override
    protected void handleDelete( GeneProduct geneProduct ) {
        this.getGeneProductDao().remove( geneProduct );
    }

    /*
     * (non-Javadoc)
     * 
     * @see GeneProductServiceBase#handleFind(GeneProduct)
     */
    @Override
    protected GeneProduct handleFind( GeneProduct gProduct ) {
        return this.getGeneProductDao().find( gProduct );
    }

    /**
     * @see GeneProductService#findOrCreate(GeneProduct)
     */
    @Override
    protected GeneProduct handleFindOrCreate( GeneProduct geneProduct ) {
        return this.getGeneProductDao().findOrCreate( geneProduct );
    }

    /*
     * (non-Javadoc)
     * 
     * @see GeneProductServiceBase#handleGetGenesByName(java.lang.String)
     */
    @Override
    protected Collection<Gene> handleGetGenesByName( String search ) {
        return this.getGeneProductDao().getGenesByName( search );
    }

    /*
     * (non-Javadoc)
     * 
     * @see GeneProductServiceBase#handleGetGenesByNcbiId(java.lang.String)
     */
    @Override
    protected Collection<Gene> handleGetGenesByNcbiId( String search ) {
        return this.getGeneProductDao().getGenesByNcbiId( search );
    }

    /**
     * @see GeneProductService#load(java.lang.Long)
     */
    @Override
    protected GeneProduct handleLoad( java.lang.Long id ) {
        return this.getGeneProductDao().load( id );
    }

    /*
     * (non-Javadoc)
     * 
     * @see GeneProductServiceBase#handleLoadMultiple(java.util.Collection)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Collection<GeneProduct> handleLoadMultiple( Collection<Long> ids ) {
        return ( Collection<GeneProduct> ) this.getGeneProductDao().load( ids );
    }

    /**
     * @see GeneProductService#update(GeneProduct)
     */
    @Override
    protected void handleUpdate( GeneProduct geneProduct ) {
        this.getGeneProductDao().update( geneProduct );
    }

}