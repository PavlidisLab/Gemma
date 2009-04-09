/*
 * The Gemma project.
 * 
 * Copyright (c) 2006-2008 University of British Columbia
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
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.model.analysis.expression.coexpression.CoexpressionCollectionValueObject;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.PhysicalLocation;
import ubic.gemma.model.genome.RelativeLocationData;
import ubic.gemma.model.genome.Taxon;

/**
 * @author pavlidis
 * @author keshav
 * @version $Id$
 * @see ubic.gemma.model.genome.gene.GeneService
 */
public class GeneServiceImpl extends ubic.gemma.model.genome.gene.GeneServiceBase {

    private static Log log = LogFactory.getLog( GeneServiceImpl.class.getName() );

    public Collection<Gene> find( PhysicalLocation physicalLocation ) {
        return this.getGeneDao().find( physicalLocation );
    }

    public RelativeLocationData findNearest( PhysicalLocation physicalLocation, boolean useStrand ) {
        return this.getGeneDao().findNearest( physicalLocation, useStrand );
    }

    @Override
    public Map<Gene, CoexpressionCollectionValueObject> handleGetCoexpressedGenes( Collection<Gene> genes,
            Collection<? extends BioAssaySet> ees, Integer stringency, boolean knownGenesOnly, boolean interGenesOnly ) {
        return this.getGeneDao().getCoexpressedGenes( genes, ees, stringency, knownGenesOnly, interGenesOnly );
    }

    @Override
    public void handleThawLite( Collection<Gene> genes ) {
        this.getGeneDao().thawLite( genes );
    }

    public void thawLite( Gene gene ) {
        this.getGeneDao().thawLite( gene );
    }

    @Override
    protected Integer handleCountAll() throws Exception {
        return this.getGeneDao().countAll();
    }

    /*
     * (non-Javadoc)
     * @see ubic.gemma.model.genome.gene.GeneServiceBase#handleCreate(java.util.Collection)
     */
    @Override
    protected Collection<Gene> handleCreate( Collection<Gene> genes ) throws Exception {
        return this.getGeneDao().create( genes );

    }

    @Override
    protected Gene handleCreate( Gene gene ) throws Exception {
        return this.getGeneDao().create( gene );
    }

    /*
     * (non-Javadoc)
     * @see ubic.gemma.model.genome.gene.GeneServiceBase#handleFind(ubic.gemma.model.genome.Gene)
     */
    @Override
    protected Gene handleFind( Gene gene ) throws Exception {
        return this.getGeneDao().find( gene );
    }

    /**
     * @see ubic.gemma.model.genome.gene.GeneService#findAllQtlsByPhysicalMapLocation(ubic.gemma.model.genome.PhysicalLocation)
     */
    @Override
    protected java.util.Collection handleFindAllQtlsByPhysicalMapLocation(
            ubic.gemma.model.genome.PhysicalLocation physicalMapLocation ) throws java.lang.Exception {
        return this.getGeneDao().findByPhysicalLocation( physicalMapLocation );
    }

    @Override
    protected Gene handleFindByAccession( String accession, ExternalDatabase source ) throws Exception {
        return this.getGeneDao().findByAccession( accession, source );
    }

    /*
     * (non-Javadoc)
     * @see ubic.gemma.model.genome.gene.GeneServiceBase#handleGetByGeneAlias(java.lang.String)
     */
    @Override
    protected Collection<Gene> handleFindByAlias( String search ) throws Exception {
        return this.getGeneDao().findByAlias( search );
    }

    /**
     * @see ubic.gemma.model.genome.gene.GeneService#handleFindByID(java.lang.long)
     */
    protected ubic.gemma.model.genome.Gene handleFindByID( Long id ) throws java.lang.Exception {
        return this.getGeneDao().load( id );
    }

    @Override
    protected Gene handleFindByNCBIId( String accession ) throws Exception {
        Collection<Gene> genes = this.getGeneDao().findByNcbiId( accession );
        if ( genes.size() > 1 ) {
            log.warn( "More than one gene with accession=" + accession );
        } else if ( genes.size() == 1 ) {
            return genes.iterator().next();
        }
        return null;

    }

    /**
     * @see ubic.gemma.model.genome.gene.GeneService#findByOfficialName(java.lang.String)
     */
    @Override
    protected java.util.Collection<Gene> handleFindByOfficialName( java.lang.String officialName )
            throws java.lang.Exception {
        return this.getGeneDao().findByOfficialName( officialName );
    }

    /**
     * @see ubic.gemma.model.genome.gene.GeneService#findByOfficialSymbol(java.lang.String)
     */
    @Override
    protected java.util.Collection<Gene> handleFindByOfficialSymbol( java.lang.String officialSymbol )
            throws java.lang.Exception {
        return this.getGeneDao().findByOfficalSymbol( officialSymbol );
    }

    @Override
    protected Gene handleFindByOfficialSymbol( String symbol, Taxon taxon ) throws Exception {
        return this.getGeneDao().findByOfficialSymbol( symbol, taxon );
    }

    /**
     * @see ubic.gemma.model.genome.gene.GeneService#handleFindByOfficialSymbolInexact(java.lang.String)
     */
    @Override
    protected java.util.Collection<Gene> handleFindByOfficialSymbolInexact( java.lang.String officialSymbol )
            throws java.lang.Exception {
        return this.getGeneDao().findByOfficialSymbolInexact( officialSymbol );
    }

    @Override
    protected Gene handleFindOrCreate( Gene gene ) throws Exception {
        return this.getGeneDao().findOrCreate( gene );
    }

    /*
     * (non-Javadoc)
     * @see ubic.gemma.model.genome.gene.GeneServiceBase#handleGetCoexpressedGenes(ubic.gemma.model.genome.Gene)
     */
    @Override
    protected CoexpressionCollectionValueObject handleGetCoexpressedGenes( Gene gene,
            Collection<? extends BioAssaySet> ees, Integer stringency, boolean knownGenesOnly ) throws Exception {
        return this.getGeneDao().getCoexpressedGenes( gene, ees, stringency, knownGenesOnly );
    }

    /*
     * (non-Javadoc)
     * @see ubic.gemma.model.genome.gene.GeneServiceBase#handleGetCoexpressedKnownGenes(ubic.gemma.model.genome.Gene,
     * java.util.Collection, java.lang.Integer)
     */
    @Override
    protected Collection handleGetCoexpressedKnownGenes( Gene gene, Collection ees, Integer stringency )
            throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    protected long handleGetCompositeSequenceCountById( Long id ) throws Exception {
        return this.getGeneDao().getCompositeSequenceCountById( id );
    }

    /*
     * (non-Javadoc)
     * @see ubic.gemma.model.genome.gene.GeneServiceBase#handleGetCompositeSequencesById(ubic.gemma.model.genome.Gene,
     * ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    @Override
    protected Collection handleGetCompositeSequences( Gene gene, ArrayDesign arrayDesign ) throws Exception {
        return this.getGeneDao().getCompositeSequences( gene, arrayDesign );
    }

    @Override
    protected Collection handleGetCompositeSequencesById( Long id ) throws Exception {
        return this.getGeneDao().getCompositeSequencesById( id );
    }

    @Override
    protected Collection<Gene> handleGetGenesByTaxon( Taxon taxon ) throws Exception {
        return this.getGeneDao().getGenesByTaxon( taxon );
    }

    /*
     * (non-Javadoc)
     * @see ubic.gemma.model.genome.gene.GeneServiceBase#handleGetMicroRnaByTaxon(Taxon)
     */
    @Override
    protected Collection handleGetMicroRnaByTaxon( Taxon taxon ) throws Exception {
        return this.getGeneDao().getMicroRnaByTaxon( taxon );
    }

    @Override
    protected Gene handleLoad( long id ) throws Exception {
        return this.getGeneDao().load( id );
    }

    /**
     * @see ubic.gemma.model.genome.gene.GeneServiceBase#handleGetAllGenes()
     */
    @Override
    protected Collection<Gene> handleLoadAll() throws Exception {
        return this.getGeneDao().loadAll();
    }

    @Override
    protected Collection<Gene> handleLoadKnownGenes( Taxon taxon ) throws Exception {
        return this.getGeneDao().loadKnownGenes( taxon );
    }

    /*
     * (non-Javadoc)
     * @see ubic.gemma.model.genome.gene.GeneServiceBase#handleLoadMultiple(java.util.Collection)
     */
    @Override
    protected Collection<Gene> handleLoadMultiple( Collection<Long> ids ) throws Exception {
        return this.getGeneDao().load( ids );
    }

    @Override
    protected Collection handleLoadPredictedGenes( Taxon taxon ) throws Exception {
        return this.getGeneDao().loadPredictedGenes( taxon );
    }

    @Override
    protected Collection handleLoadProbeAlignedRegions( Taxon taxon ) throws Exception {
        return this.getGeneDao().loadProbeAlignedRegions( taxon );
    }

    /*
     * (non-Javadoc)
     * @see ubic.gemma.model.genome.gene.GeneServiceBase#handleRemove(java.util.Collection)
     */
    @Override
    protected void handleRemove( Collection genes ) throws Exception {
        this.getGeneDao().remove( genes );

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

    /**
     * This was created because calling saveGene from Spring causes caching errors. I left saveGene in place on the
     * assumption that Kiran's loaders use it with success.
     * 
     * @see ubic.gemma.model.genome.gene.GeneService#createGene(ubic.gemma.model.genome.Gene)
     */
    protected ubic.gemma.model.genome.Gene handleSaveGene( ubic.gemma.model.genome.Gene gene )
            throws java.lang.Exception {
        return this.getGeneDao().create( gene );
    }

    @Override
    protected void handleThaw( Gene gene ) throws Exception {
        this.getGeneDao().thaw( gene );
    }

    /**
     * This was created because calling saveGene with an existing gene actually causes a caching error in Spring.
     * 
     * @see ubic.gemma.model.genome.gene.GeneService#updateGene(ubic.gemma.model.genome.Gene)
     */
    @Override
    protected void handleUpdate( ubic.gemma.model.genome.Gene gene ) throws java.lang.Exception {
        this.getGeneDao().update( gene );
    }

}