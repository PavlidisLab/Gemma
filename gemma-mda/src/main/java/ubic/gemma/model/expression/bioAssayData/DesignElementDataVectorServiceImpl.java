/*
 * The Gemma project
 * 
 * Copyright (c) 2011 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.model.expression.bioAssayData;

import java.util.Collection;
import java.util.HashSet;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;

/**
 * @author pavlidis
 * @version $Id$
 * @see ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorService
 */
@Service
public class DesignElementDataVectorServiceImpl extends DesignElementDataVectorServiceBase {

    @Override
    @Transactional(readOnly = true)
    public Collection<? extends DesignElementDataVector> find( BioAssayDimension bioAssayDimension ) {
        return this.getRawExpressionDataVectorDao().find( bioAssayDimension );
    }

    @Override
    protected Integer handleCountAll() {
        return this.getRawExpressionDataVectorDao().countAll();
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Collection<? extends DesignElementDataVector> handleCreate(
            Collection<? extends DesignElementDataVector> vectors ) {

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
            QuantitationType quantitationType ) {
        Collection<DesignElementDataVector> results = new HashSet<DesignElementDataVector>();
        results.addAll( this.getRawExpressionDataVectorDao().find( arrayDesign, quantitationType ) );
        results.addAll( this.getProcessedExpressionDataVectorDao().find( arrayDesign, quantitationType ) );
        return results;
    }

    @Override
    protected Collection<? extends DesignElementDataVector> handleFind( Collection<QuantitationType> quantitationTypes ) {
        Collection<DesignElementDataVector> results = new HashSet<DesignElementDataVector>();
        results.addAll( this.getRawExpressionDataVectorDao().find( quantitationTypes ) );
        results.addAll( this.getProcessedExpressionDataVectorDao().find( quantitationTypes ) );
        return results;
    }

    @Override
    protected Collection<? extends DesignElementDataVector> handleFind( QuantitationType quantitationType ) {
        Collection<DesignElementDataVector> results = new HashSet<DesignElementDataVector>();
        results.addAll( this.getRawExpressionDataVectorDao().find( quantitationType ) );
        results.addAll( this.getProcessedExpressionDataVectorDao().find( quantitationType ) );
        return results;
    }

    @Override
    protected DesignElementDataVector handleLoad( Long id ) {
        return this.getRawExpressionDataVectorDao().load( id );
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void handleRemove( Collection<? extends DesignElementDataVector> vectors ) {
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
    protected void handleRemove( DesignElementDataVector dedv ) {
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
    protected void handleRemoveDataForCompositeSequence( CompositeSequence compositeSequence ) {
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
    protected void handleRemoveDataForQuantitationType( QuantitationType quantitationType ) {
        this.getRawExpressionDataVectorDao().removeDataForQuantitationType( quantitationType );
    }

    @Override
    protected void handleThaw( Collection<? extends DesignElementDataVector> vectors ) {

        if ( vectors == null ) {
            return;
        }

        // Doesn't matter which kind, the same thaw method gets called.
        this.getRawExpressionDataVectorDao().thaw( vectors );

    }

    @SuppressWarnings("unchecked")
    @Override
    protected void handleUpdate( Collection<? extends DesignElementDataVector> vectors ) {

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
    protected void handleUpdate( DesignElementDataVector dedv ) {
        if ( dedv instanceof RawExpressionDataVector ) {
            this.getRawExpressionDataVectorDao().update( ( Collection<RawExpressionDataVector> ) dedv );
        } else if ( dedv instanceof ProcessedExpressionDataVector ) {
            this.getProcessedExpressionDataVectorDao().update( ( Collection<ProcessedExpressionDataVector> ) dedv );
        } else {
            throw new UnsupportedOperationException( "Don't know how to process a " + dedv.getClass().getName() );
        }
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