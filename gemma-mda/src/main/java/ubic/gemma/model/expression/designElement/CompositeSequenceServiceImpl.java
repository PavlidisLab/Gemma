/*
 * The Gemma project.
 * 
 * Copyright (c) 2006 University of British Columbia
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 */
package ubic.gemma.model.expression.designElement;

import java.util.Collection;
import java.util.HashSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.model.association.BioSequence2GeneProduct;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.gene.GeneProduct;

/**
 * @author keshav
 * @author pavlidis
 * @version $Id$
 * @see ubic.gemma.model.expression.designElement.CompositeSequenceService
 */
public class CompositeSequenceServiceImpl extends
        ubic.gemma.model.expression.designElement.CompositeSequenceServiceBase {
    Log log = LogFactory.getLog( this.getClass() );

    /**
     * @see ubic.gemma.model.expression.designElement.CompositeSequenceService#saveCompositeSequence(ubic.gemma.model.expression.designElement.CompositeSequence)
     */
    protected void handleSaveCompositeSequence(
            ubic.gemma.model.expression.designElement.CompositeSequence compositeSequence ) throws java.lang.Exception {
        this.getCompositeSequenceDao().create( compositeSequence );
    }

    @Override
    protected CompositeSequence handleFindOrCreate( CompositeSequence compositeSequence ) throws Exception {

        return this.getCompositeSequenceDao().findOrCreate( compositeSequence );
    }

    @Override
    protected void handleRemove( CompositeSequence compositeSequence ) throws Exception {
        this.getCompositeSequenceDao().findOrCreate( compositeSequence );

    }

    @Override
    protected CompositeSequence handleFind( CompositeSequence compositeSequence ) throws Exception {
        return this.getCompositeSequenceDao().find( compositeSequence );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.designElement.CompositeSequenceServiceBase#handleCreate(ubic.gemma.model.expression.designElement.CompositeSequence)
     */
    @Override
    protected CompositeSequence handleCreate( CompositeSequence compositeSequence ) throws Exception {
        return ( CompositeSequence ) this.getCompositeSequenceDao().create( compositeSequence );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.designElement.CompositeSequenceServiceBase#handleCreate(java.util.Collection)
     */
    @Override
    @SuppressWarnings("unchecked")
    protected Collection<CompositeSequence> handleCreate( Collection compositeSequences ) throws Exception {
        return this.getCompositeSequenceDao().create( compositeSequences );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.designElement.CompositeSequenceServiceBase#handleFindByName(java.lang.String)
     */
    @Override
    @SuppressWarnings("unchecked")
    protected Collection<CompositeSequence> handleFindByName( String name ) throws Exception {
        return this.getCompositeSequenceDao().findByName( name );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.designElement.CompositeSequenceServiceBase#handleFindByName(ubic.gemma.model.expression.arrayDesign.ArrayDesign,
     *      java.lang.String)
     */
    @Override
    protected CompositeSequence handleFindByName( ArrayDesign arrayDesign, String name ) throws Exception {
        return this.getCompositeSequenceDao().findByName( arrayDesign, name );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.designElement.CompositeSequenceServiceBase#handleGetAssociatedGenes(ubic.gemma.model.expression.designElement.CompositeSequence)
     */
    @Override
    protected Collection<Gene> handleGetAssociatedGenes( CompositeSequence compositeSequence ) throws Exception {

        Collection<Gene> genes = null;

        if ( compositeSequence.getBiologicalCharacteristic() != null ) {
            genes = new HashSet<Gene>();
            for ( BioSequence2GeneProduct bs2gp : compositeSequence.getBiologicalCharacteristic()
                    .getBioSequence2GeneProduct() ) {
                if ( bs2gp != null ) {
                    GeneProduct geneProduct = bs2gp.getGeneProduct();
                    if ( geneProduct != null ) genes.add( geneProduct.getGene() );
                }
            }
        }
        return genes;
    }
}