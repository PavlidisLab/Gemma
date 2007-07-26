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
import java.util.Map;

import ubic.gemma.model.coexpression.CoexpressionCollectionValueObject;
import ubic.gemma.model.coexpression.MultipleCoexpressionCollectionValueObject;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;

/**
 * @author pavlidis
 * @author keshav
 * @version $Id$
 * @see ubic.gemma.model.genome.gene.GeneService
 */
public class GeneServiceImpl extends ubic.gemma.model.genome.gene.GeneServiceBase {

	@Override
    protected Map handleGetCoexpressedGeneMap( int stringincy, Gene gene ) throws Exception {
        return this.getGeneDao().getCoexpressedGeneMap( stringincy, gene );
    }

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
     * @see ubic.gemma.model.genome.gene.GeneServiceBase#handleGetCoexpressedGenes(ubic.gemma.model.genome.Gene)
     */
    @Override
    protected Object handleGetCoexpressedGenes( Gene gene, Collection ees, Integer stringency ) throws Exception {
        return this.getGeneDao().getCoexpressedGenes( gene, ees, stringency );
    }

    /* (non-Javadoc)
	 * @see ubic.gemma.model.genome.gene.GeneServiceBase#handleGetMultipleCoexpressionResults(java.util.Collection, java.util.Collection, java.lang.Integer)
	 */
	@Override
	protected Object handleGetMultipleCoexpressionResults(Collection genes, Collection ees, Integer stringency) throws Exception {
		MultipleCoexpressionCollectionValueObject results = new MultipleCoexpressionCollectionValueObject();
		for ( Iterator iter = genes.iterator(); iter.hasNext(); ) {
			Gene gene = (Gene) iter.next();
            CoexpressionCollectionValueObject current = (CoexpressionCollectionValueObject)getCoexpressedGenes( gene, ees, stringency );
            results.addCoexpressionCollection( current );
		}
		return results;
	}

    @Override
    protected Collection handleGetGenesByTaxon( Taxon taxon ) throws Exception {
        return this.getGeneDao().getGenesByTaxon( taxon );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.gene.GeneServiceBase#handleLoad(java.util.Collection)
     */
    @Override
    protected Collection handleLoad( Collection ids ) throws Exception {
        return this.getGeneDao().load( ids );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.gene.GeneServiceBase#handleGetMicroRnaByTaxon(Taxon)
     */
    @Override
    protected Collection handleGetMicroRnaByTaxon( Taxon taxon ) throws Exception {
        return this.getGeneDao().getMicroRnaByTaxon( taxon );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.gene.GeneServiceBase#handleGetCompositeSequencesById(ubic.gemma.model.genome.Gene,
     *      ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    @Override
    protected Collection handleGetCompositeSequences( Gene gene, ArrayDesign arrayDesign ) throws Exception {
        return this.getGeneDao().getCompositeSequences( gene, arrayDesign );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.gene.GeneServiceBase#handleGetCompositeSequenceMap(java.util.Collection)
     */
    @Override
    protected Map handleGetCompositeSequenceMap( Collection genes ) throws Exception {
        return this.getGeneDao().getCompositeSequenceMap( genes );
    }

    @Override
    protected long handleGetCompositeSequenceCountById( Long id ) throws Exception {
        return this.getGeneDao().getCompositeSequenceCountById( id );
    }

    @Override
    protected Collection handleGetCompositeSequencesById( Long id ) throws Exception {
        return this.getGeneDao().getCompositeSequencesById( id );
    }

    @Override
    protected Map handleGetCS2GeneMap( Collection csIds ) throws Exception {
        // TODO Auto-generated method stub
        return this.getGeneDao().getCS2GeneMap( csIds );
    }

}