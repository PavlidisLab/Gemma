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
package ubic.gemma.model.expression.bioAssayData;

import java.util.Collection;
import java.util.Map;

import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * @author pavlidis
 * @version $Id$
 * @see ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorService
 */
public class DesignElementDataVectorServiceImpl extends
        ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorServiceBase {

    @Override
    protected void handleUpdate( DesignElementDataVector dedv ) throws Exception {
        this.getDesignElementDataVectorDao().update( dedv );
    }

    @Override
    protected void handleUpdate( Collection dedvs ) throws Exception {
        this.getDesignElementDataVectorDao().update( dedvs );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorServiceBase#handleGetVectors(java.util.Collection,
     *      java.util.Collection, ubic.gemma.model.common.quantitationtype.QuantitationType)
     */
    @Override
    protected Map handleGetVectors( Collection ees, Collection genes ) throws Exception {
        return this.getDesignElementDataVectorDao().getVectors( ees, genes );
    }

    @Override
    protected DesignElementDataVector handleFindOrCreate( DesignElementDataVector designElementDataVector )
            throws Exception {

        return this.getDesignElementDataVectorDao().findOrCreate( designElementDataVector );
    }

    @Override
    protected void handleRemove( DesignElementDataVector designElementDataVector ) throws Exception {
        this.getDesignElementDataVectorDao().remove( designElementDataVector );

    }

    @Override
    protected Integer handleCountAll() throws Exception {
        return this.getDesignElementDataVectorDao().countAll();
    }

    /*
     * (non-Javadoc)R
     * 
     * @see ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorServiceBase#handleFindAllForMatrix(ubic.gemma.model.expression.experiment.ExpressionExperiment,
     *      ubic.gemma.model.common.quantitationtype.QuantitationType,
     *      ubic.gemma.model.expression.designElement.DesignElement)
     */
    @Override
    protected Collection handleFind( QuantitationType quantitationType ) throws Exception {
        return this.getDesignElementDataVectorDao().find( quantitationType );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorServiceBase#handleGetGenes(ubic.gemma.model.expression.bioAssayData.DesignElementDataVector)
     */
    @Override
    protected Collection handleGetGenes( DesignElementDataVector dedv ) throws Exception {
        return this.getDesignElementDataVectorDao().getGenes( dedv );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorServiceBase#handleGetGenesById(long)
     */
    @Override
    protected Collection handleGetGenesById( long id ) throws Exception {
        return this.getDesignElementDataVectorDao().getGenesById( id );
    }

    @Override
    protected void handleThaw( DesignElementDataVector designElementDataVector ) throws Exception {
        this.getDesignElementDataVectorDao().thaw( designElementDataVector );
    }

    @Override
    protected void handleThaw( Collection designElementDataVectors ) throws Exception {
        this.getDesignElementDataVectorDao().thaw( designElementDataVectors );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorServiceBase#handleGetGenes(java.util.Collection)
     */
    @Override
    protected Map handleGetGenes( Collection dataVectors ) throws Exception {
        return this.getDesignElementDataVectorDao().getGenes( dataVectors );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorServiceBase#handleRemoveDataForCompositeSequence(ubic.gemma.model.expression.designElement.CompositeSequence)
     */
    @Override
    protected void handleRemoveDataForCompositeSequence( CompositeSequence compositeSequence ) throws Exception {
        this.getDesignElementDataVectorDao().removeDataForCompositeSequence( compositeSequence );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorServiceBase#handleRemoveDataForQuantitationType(ubic.gemma.model.expression.experiment.ExpressionExperiment,
     *      ubic.gemma.model.common.quantitationtype.QuantitationType)
     */
    @Override
    protected void handleRemoveDataForQuantitationType( ExpressionExperiment expressionExperiment,
            QuantitationType quantitationType ) throws Exception {
        this.getDesignElementDataVectorDao().removeDataFromQuantitationType( expressionExperiment, quantitationType );
    }

    @Override
    protected Collection handleFind( Collection quantitationTypes ) throws Exception {
        return this.getDesignElementDataVectorDao().find( quantitationTypes );
    }

    @Override
    protected DesignElementDataVector handleLoad( Long id ) throws Exception {
        return ( DesignElementDataVector ) this.getDesignElementDataVectorDao().load( id );
    }

    @Override
    protected Collection handleCreate( Collection vectors ) throws Exception {
        return this.getDesignElementDataVectorDao().create( vectors );
    }

    @Override
    protected void handleRemove( Collection vectors ) throws Exception {
        this.getDesignElementDataVectorDao().remove( vectors );
    }

    @Override
    protected Collection handleFind( ArrayDesign arrayDesign, QuantitationType quantitationType ) throws Exception {
        return this.getDesignElementDataVectorDao().find( arrayDesign, quantitationType );
    }

    @Override
    protected Map handleGetDedv2GenesMap( Collection dedvs, QuantitationType qt ) throws Exception {
           return this.getDesignElementDataVectorDao().getDedv2GenesMap( dedvs, qt );
    }
}