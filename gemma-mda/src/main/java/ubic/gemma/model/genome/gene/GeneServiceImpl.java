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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;

/**
 * @author pavlidis
 * @author keshav
 * @version $Id$
 * @see ubic.gemma.model.genome.gene.GeneService
 */
public class GeneServiceImpl extends ubic.gemma.model.genome.gene.GeneServiceBase {
    private Log log = LogFactory.getLog( GeneServiceImpl.class );

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.gene.GeneServiceBase#handleGetByGeneAlias(java.lang.String)
     */
    @Override
    protected Collection handleGetByGeneAlias( String search ) throws Exception {
        return this.getGeneDao().getByGeneAlias( search );
    }

    @Override
    protected Integer handleCountAll() throws Exception {
        return this.getGeneDao().countAll();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.gene.GeneServiceBase#handleGetCompositeSequenceCountById(long)
     */
    @Override
    protected long handleGetCompositeSequenceCountById( long id ) throws Exception {
        return this.getGeneDao().getCompositeSequenceCountById( id );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.gene.GeneServiceBase#handleGetCompositeSequencesById(long)
     */
    @Override
    protected Collection handleGetCompositeSequencesById( long id ) throws Exception {
        // TODO change name to getCompositeSequenceByGene(Gene gene)
        return this.getGeneDao().getCompositeSequencesById( id );
    }

    /**
     * This was created because calling saveGene with an existant gene actually causes a caching error in Spring.
     * 
     * @see ubic.gemma.model.genome.gene.GeneService#updateGene(ubic.gemma.model.genome.Gene)
     */
    @Override
    protected void handleUpdate( ubic.gemma.model.genome.Gene gene ) throws java.lang.Exception {
        this.getGeneDao().update( gene );
    }

    /**
     * This was created because calling saveGene from Spring causes caching errors. I left saveGene in place on the
     * assumption that Kiran's loaders use it with success.
     * 
     * @see ubic.gemma.model.genome.gene.GeneService#createGene(ubic.gemma.model.genome.Gene)
     */
    protected ubic.gemma.model.genome.Gene handleSaveGene( ubic.gemma.model.genome.Gene gene )
            throws java.lang.Exception {
        return ( Gene ) this.getGeneDao().create( gene );
    }

    /**
     * @see ubic.gemma.model.genome.gene.GeneService#findByOfficialName(java.lang.String)
     */
    @Override
    @SuppressWarnings("unchecked")
    protected java.util.Collection<Gene> handleFindByOfficialName( java.lang.String officialName )
            throws java.lang.Exception {
        return this.getGeneDao().findByOfficialName( officialName );
    }

    /**
     * @see ubic.gemma.model.genome.gene.GeneService#findByOfficialSymbol(java.lang.String)
     */
    @Override
    @SuppressWarnings("unchecked")
    protected java.util.Collection<Gene> handleFindByOfficialSymbol( java.lang.String officialSymbol )
            throws java.lang.Exception {
        return this.getGeneDao().findByOfficalSymbol( officialSymbol );
    }

    /**
     * @see ubic.gemma.model.genome.gene.GeneService#handleFindByOfficialSymbolInexact(java.lang.String)
     */
    @Override
    @SuppressWarnings("unchecked")
    protected java.util.Collection<Gene> handleFindByOfficialSymbolInexact( java.lang.String officialSymbol )
            throws java.lang.Exception {
        return this.getGeneDao().findByOfficialSymbolInexact( officialSymbol );
    }

    /**
     * @see ubic.gemma.model.genome.gene.GeneService#findAllQtlsByPhysicalMapLocation(ubic.gemma.model.genome.PhysicalLocation)
     */
    @Override
    protected java.util.Collection handleFindAllQtlsByPhysicalMapLocation(
            ubic.gemma.model.genome.PhysicalLocation physicalMapLocation ) throws java.lang.Exception {
        return this.getGeneDao().findByPhysicalLocation( physicalMapLocation );
    }

    /**
     * @see ubic.gemma.model.genome.gene.GeneService#handleFindByID(java.lang.long)
     */
    protected ubic.gemma.model.genome.Gene handleFindByID( Long id ) throws java.lang.Exception {
        return ( Gene ) this.getGeneDao().load( id );
    }

    /**
     * @see ubic.gemma.model.genome.gene.GeneServiceBase#handleGetAllGenes()
     */
    @Override
    protected Collection handleLoadAll() throws Exception {
        return this.getGeneDao().loadAll();
    }

    @Override
    protected void handleRemove( String officialName ) throws Exception {
        java.util.Collection col = this.getGeneDao().findByOfficialName( officialName );
        Iterator iter = col.iterator();
        while ( iter.hasNext() ) {
            Gene g = ( Gene ) iter.next();
            this.getGeneDao().remove( g );
        }
    }

    @Override
    protected Gene handleFindOrCreate( Gene gene ) throws Exception {
        return this.getGeneDao().findOrCreate( gene );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.gene.GeneServiceBase#handleRemove(java.util.Collection)
     */
    @Override
    protected void handleRemove( Collection genes ) throws Exception {
        this.getGeneDao().remove( genes );

    }

    @Override
    protected Gene handleCreate( Gene gene ) throws Exception {
        return ( Gene ) this.getGeneDao().create( gene );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.gene.GeneServiceBase#handleCreate(java.util.Collection)
     */
    @Override
    protected Collection handleCreate( Collection genes ) throws Exception {
        return this.getGeneDao().create( genes );

    }

    @Override
    protected Gene handleLoad( long id ) throws Exception {
        return ( Gene ) this.getGeneDao().load( id );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.gene.GeneServiceBase#handleFind(ubic.gemma.model.genome.Gene)
     */
    @Override
    protected Gene handleFind( Gene gene ) throws Exception {
        return this.getGeneDao().find( gene );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.gene.GeneServiceBase#handleGetCoexpressedElements(ubic.gemma.model.genome.Gene)
     */
    @Override
    protected Collection handleGetCoexpressedElements( Gene gene ) throws Exception {
        return this.getGeneDao().getCoexpressedElements( gene );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.gene.GeneServiceBase#handleGetCoexpressedElementsById(long)
     */
    @Override
    protected Collection handleGetCoexpressedElementsById( long id ) throws Exception {
        return this.getGeneDao().getCoexpressedElementsById( id );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.gene.GeneServiceBase#handleGetCoexpressedGenes(ubic.gemma.model.genome.Gene)
     */
    @Override
    protected Collection handleGetCoexpressedGenes( Gene gene ) throws Exception {
        return this.getGeneDao().getCoexpressedGenes( gene );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.gene.GeneServiceBase#handleGetCoexpressedGenesById(long)
     */
    @Override
    protected Collection handleGetCoexpressedGenesById( long id ) throws Exception {
        return this.getGeneDao().getCoexpressedGenesById( id );
    }

    @Override
    protected Collection handleGetGenesByTaxon( Taxon taxon ) throws Exception {
        return this.getGeneDao().getGenesByTaxon( taxon );
    }

    /*
     * (non-Javadoc) Returns a map of composite sequence collections, keyed by the gene (insert order of Map is
     * preserved).
     * 
     * @see ubic.gemma.model.genome.gene.GeneServiceBase#handleGetCompositeSequencesForGenes(java.lang.String[])
     */
    @Override
    protected Map handleGetCompositeSequencesForGenes( String[] officialSymbols ) throws Exception {

        Map<String, Collection<Gene>> genesMap = this.getMatchingGenes( officialSymbols );

        LinkedHashSet<String> geneOfficialSymbolKeyset = new LinkedHashSet<String>();

        LinkedHashMap<Gene, Collection<CompositeSequence>> compositeSequencesForGeneMap = new LinkedHashMap<Gene, Collection<CompositeSequence>>();

        for ( String officialSymbol : geneOfficialSymbolKeyset ) {
            log.debug( "official symbol: " + officialSymbol );
            Collection<Gene> genes = genesMap.get( officialSymbol );
            for ( Gene g : genes ) {
                Collection<CompositeSequence> compositeSequences = this.getCompositeSequencesById( g.getId() );
                compositeSequencesForGeneMap.put( g, compositeSequences );
            }
        }
        return compositeSequencesForGeneMap;
    }

    /*
     * (non-Javadoc) If gene with searchId[i] does not exist, it is discarded. Internal storage of the map is ordered
     * (insert order).
     * 
     * @see ubic.gemma.model.genome.gene.GeneServiceBase#handleGetMatchingGenes(java.lang.String[])
     */
    @Override
    protected Map handleGetMatchingGenes( String[] officialSymbols ) throws Exception {

        LinkedHashMap<String, Collection<Gene>> geneMap = new LinkedHashMap<String, Collection<Gene>>();
        /* for each gene, get the matching composite sequence. if it exists, remove. */
        for ( String officialSymbol : officialSymbols ) {
            officialSymbol = StringUtils.trim( officialSymbol );
            log.debug( "entered: " + officialSymbol );
            Collection<Gene> genes = this.findByOfficialSymbol( officialSymbol );// TODO restrict by qt.
            if ( genes == null || genes.isEmpty() ) {
                log
                        .warn( "Discarding genes with official symbol " + officialSymbol
                                + " do not exist.  Discarding ... " );
                continue;
            }
            geneMap.put( officialSymbol, genes );
        }

        return geneMap;
    }

}