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

import org.springframework.stereotype.Service;

import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;

/**
 * @author pavlidis
 * @version $Id$
 * @see ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorService
 */
@Service
public class DesignElementDataVectorServiceImpl extends
        ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorServiceBase {

    @Override
    protected Integer handleCountAll() throws Exception {
        return this.getRawExpressionDataVectorDao().countAll();
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Collection<? extends DesignElementDataVector> handleCreate(
            Collection<? extends DesignElementDataVector> vectors ) throws Exception {

        if ( vectors == null || vectors.isEmpty() ) {
            return vectors;
        }

        Class<? extends DesignElementDataVector> vectorClass = getVectorClass( vectors );

        if ( vectorClass.equals( RawExpressionDataVector.class ) ) {
            return this.getRawExpressionDataVectorDao().create( ( Collection<RawExpressionDataVector> ) vectors );
        }
        return this.getProcessedExpressionDataVectorDao()
                .create( ( Collection<ProcessedExpressionDataVector> ) vectors );

    }

    @Override
    protected Collection<RawExpressionDataVector> handleFind( ArrayDesign arrayDesign, QuantitationType quantitationType )
            throws Exception {
        return this.getRawExpressionDataVectorDao().find( arrayDesign, quantitationType );
    }

    @Override
    protected Collection<RawExpressionDataVector> handleFind( Collection<QuantitationType> quantitationTypes )
            throws Exception {
        return this.getRawExpressionDataVectorDao().find( quantitationTypes );
    }

    /*
     * (non-Javadoc)
     * @see
     * ubic.gemma.model.expression.bioAssayData.RawExpressionDataVectorServiceBase#handleFindAllForMatrix(ubic.gemma
     * .model.expression.experiment.ExpressionExperiment, ubic.gemma.model.common.quantitationtype.QuantitationType,
     * ubic.gemma.model.expression.designElement.DesignElement)
     */
    @Override
    protected Collection<RawExpressionDataVector> handleFind( QuantitationType quantitationType ) throws Exception {
        return this.getRawExpressionDataVectorDao().find( quantitationType );
    }

    @Override
    protected RawExpressionDataVector handleLoad( Long id ) throws Exception {
        return this.getRawExpressionDataVectorDao().load( id );
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void handleRemove( Collection<? extends DesignElementDataVector> vectors ) throws Exception {
        if ( vectors == null || vectors.isEmpty() ) {
            return;
        }
        Class<? extends DesignElementDataVector> vectorClass = getVectorClass( vectors );

        if ( vectorClass.equals( RawExpressionDataVector.class ) ) {
            this.getRawExpressionDataVectorDao().remove( ( Collection<RawExpressionDataVector> ) vectors );
        } else {
            this.getProcessedExpressionDataVectorDao().remove( ( Collection<ProcessedExpressionDataVector> ) vectors );
        }
    }

    @Override
    protected void handleRemove( RawExpressionDataVector designElementDataVector ) throws Exception {
        this.getRawExpressionDataVectorDao().remove( designElementDataVector );

    }

    /*
     * (non-Javadoc)
     * @see
     * ubic.gemma.model.expression.bioAssayData.RawExpressionDataVectorServiceBase#handleRemoveDataForCompositeSequence
     * (ubic.gemma.model.expression.designElement.CompositeSequence)
     */
    @Override
    protected void handleRemoveDataForCompositeSequence( CompositeSequence compositeSequence ) throws Exception {
        this.getRawExpressionDataVectorDao().removeDataForCompositeSequence( compositeSequence );
    }

    /*
     * (non-Javadoc)
     * @see
     * ubic.gemma.model.expression.bioAssayData.RawExpressionDataVectorServiceBase#handleRemoveDataForQuantitationType
     * (ubic.gemma.model.expression.experiment.ExpressionExperiment,
     * ubic.gemma.model.common.quantitationtype.QuantitationType)
     */
    @Override
    protected void handleRemoveDataForQuantitationType( QuantitationType quantitationType ) throws Exception {
        this.getRawExpressionDataVectorDao().removeDataForQuantitationType( quantitationType );
    }

    @Override
    protected void handleThaw( Collection<? extends DesignElementDataVector> vectors ) throws Exception {

        if ( vectors == null ) {
            return;
        }

        // Doesn't matter which kind, the same thaw method gets called.
        this.getRawExpressionDataVectorDao().thaw( vectors );

    }

    @Override
    protected void handleThaw( RawExpressionDataVector designElementDataVector ) throws Exception {
        this.getRawExpressionDataVectorDao().thaw( designElementDataVector );
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void handleUpdate( Collection<? extends DesignElementDataVector> vectors ) throws Exception {

        if ( vectors == null || vectors.isEmpty() ) {
            return;
        }

        Class<? extends DesignElementDataVector> vectorClass = getVectorClass( vectors );

        if ( vectorClass.equals( RawExpressionDataVector.class ) ) {
            this.getRawExpressionDataVectorDao().update( ( Collection<RawExpressionDataVector> ) vectors );
        } else {
            this.getProcessedExpressionDataVectorDao().update( ( Collection<ProcessedExpressionDataVector> ) vectors );
        }
    }

    @Override
    protected void handleUpdate( RawExpressionDataVector dedv ) throws Exception {
        this.getRawExpressionDataVectorDao().update( dedv );
    }

    /**
     * @param vectors
     * @return
     */
    private Class<? extends DesignElementDataVector> getVectorClass(
            Collection<? extends DesignElementDataVector> vectors ) {
        Class<? extends DesignElementDataVector> vectorClass = null;
        for ( DesignElementDataVector designElementDataVector : vectors ) {
            if ( vectorClass == null ) {
                vectorClass = designElementDataVector.getClass();
            }
            if ( !vectorClass.equals( designElementDataVector.getClass() ) ) {
                throw new IllegalArgumentException( "Two types of vector in one collection, not supported" );
            }
        }
        return vectorClass;
    }

}