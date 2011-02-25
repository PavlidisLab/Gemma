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
import java.util.HashSet;

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
    public Collection<? extends DesignElementDataVector> find( BioAssayDimension bioAssayDimension ) {
        return this.getRawExpressionDataVectorDao().find( bioAssayDimension );
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
    protected Collection<? extends DesignElementDataVector> handleFind( ArrayDesign arrayDesign,
            QuantitationType quantitationType ) throws Exception {
        Collection<DesignElementDataVector> results = new HashSet<DesignElementDataVector>();
        results.addAll( this.getRawExpressionDataVectorDao().find( arrayDesign, quantitationType ) );
        results.addAll( this.getProcessedExpressionDataVectorDao().find( arrayDesign, quantitationType ) );
        return results;
    }

    @Override
    protected Collection<? extends DesignElementDataVector> handleFind( Collection<QuantitationType> quantitationTypes )
            throws Exception {
        Collection<DesignElementDataVector> results = new HashSet<DesignElementDataVector>();
        results.addAll( this.getRawExpressionDataVectorDao().find( quantitationTypes ) );
        results.addAll( this.getProcessedExpressionDataVectorDao().find( quantitationTypes ) );
        return results;
    }

    @Override
    protected Collection<? extends DesignElementDataVector> handleFind( QuantitationType quantitationType )
            throws Exception {
        Collection<DesignElementDataVector> results = new HashSet<DesignElementDataVector>();
        results.addAll( this.getRawExpressionDataVectorDao().find( quantitationType ) );
        results.addAll( this.getProcessedExpressionDataVectorDao().find( quantitationType ) );
        return results;
    }

    @Override
    protected DesignElementDataVector handleLoad( Long id ) throws Exception {
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
    protected void handleRemove( DesignElementDataVector dedv ) throws Exception {
        if ( dedv instanceof RawExpressionDataVector ) {
            this.getRawExpressionDataVectorDao().remove( ( RawExpressionDataVector ) dedv );
        } else if ( dedv instanceof ProcessedExpressionDataVector ) {
            this.getProcessedExpressionDataVectorDao().remove( ( ProcessedExpressionDataVector ) dedv );
        } else {
            throw new UnsupportedOperationException( "Don't know how to process a " + dedv.getClass().getName() );
        }

    }

    /*
     * (non-Javadoc)
     * 
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
     * 
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

    @SuppressWarnings("unchecked")
    @Override
    protected void handleThaw( DesignElementDataVector dedv ) throws Exception {
        if ( dedv instanceof RawExpressionDataVector )
            this.getRawExpressionDataVectorDao().thaw( ( Collection<? extends DesignElementDataVector> ) dedv );
        else if ( dedv instanceof ProcessedExpressionDataVector )
            this.getProcessedExpressionDataVectorDao().thaw( ( Collection<? extends DesignElementDataVector> ) dedv );
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void handleUpdate( Collection<? extends DesignElementDataVector> vectors ) throws Exception {

        if ( vectors == null || vectors.isEmpty() ) {
            return;
        }

        Class<? extends DesignElementDataVector> vectorClass = getVectorClass( vectors );

        if ( RawExpressionDataVector.class.isAssignableFrom( vectorClass ) ) {
            this.getRawExpressionDataVectorDao().update( ( Collection<RawExpressionDataVector> ) vectors );
        } else if ( ProcessedExpressionDataVector.class.isAssignableFrom( vectorClass ) ) {
            this.getProcessedExpressionDataVectorDao().update( ( Collection<ProcessedExpressionDataVector> ) vectors );
        } else {
            throw new UnsupportedOperationException( "Don't know how to process  " + vectorClass.getName() );
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void handleUpdate( DesignElementDataVector dedv ) throws Exception {
        if ( dedv instanceof RawExpressionDataVector ) {
            this.getRawExpressionDataVectorDao().update( ( Collection<RawExpressionDataVector> ) dedv );
        } else if ( dedv instanceof ProcessedExpressionDataVector ) {
            this.getProcessedExpressionDataVectorDao().update( ( Collection<ProcessedExpressionDataVector> ) dedv );
        } else {
            throw new UnsupportedOperationException( "Don't know how to process a " + dedv.getClass().getName() );
        }
    }

}