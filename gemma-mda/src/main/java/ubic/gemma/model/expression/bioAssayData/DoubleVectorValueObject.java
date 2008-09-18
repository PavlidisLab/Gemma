/*
 * The Gemma project
 * 
 * Copyright (c) 2008 University of British Columbia
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
package ubic.gemma.model.expression.bioAssayData;

import java.util.Collection;

import ubic.basecode.math.DescriptiveWithMissing;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.genome.Gene;
import cern.colt.list.DoubleArrayList;

/**
 * Simple wrapper for a double[] that is derived from a DesignElementDataVector.
 * 
 * @author paul
 * @version $Id$
 */
public class DoubleVectorValueObject extends DataVectorValueObject {

    private boolean masked = false;
    private double[] data = null;
    private Double rankByMean;
    private Double rankByMax;

    /**
     * @param dedv
     */
    public DoubleVectorValueObject( DesignElementDataVector dedv ) {
        this( dedv, null );
    }

    /**
     * @param dedv
     * @param genes
     */
    public DoubleVectorValueObject( DesignElementDataVector dedv, Collection<Gene> genes ) {
        super( dedv, genes );
        QuantitationType qt = dedv.getQuantitationType();
        if ( !qt.getRepresentation().equals( PrimitiveType.DOUBLE ) ) {
            throw new IllegalArgumentException( "Can only store double vectors, got " + qt + " "
                    + qt.getRepresentation() );
        }
        if ( qt.getIsMaskedPreferred() ) {
            this.masked = true;
        }
        this.data = byteArrayConverter.byteArrayToDoubles( dedv.getData() );
        if ( dedv instanceof ProcessedExpressionDataVector ) {
            this.rankByMax = ( ( ProcessedExpressionDataVector ) dedv ).getRankByMax();
            this.rankByMean = ( ( ProcessedExpressionDataVector ) dedv ).getRankByMean();
        }
    }

    public double[] getData() {
        return data;
    }

    public boolean isMasked() {
        return masked;
    }

    public void setMasked( boolean masked ) {
        this.masked = masked;
    }

    /**
     * @return data adjusted to mean 0, variance 1.
     */

    public double[] standardize() {

        /*
         * DoubleArrayList constructor does not make a copy, so we have to make one.
         */
        double[] copy = new double[this.data.length];
        for ( int i = 0; i < data.length; i++ ) {
            copy[i] = data[i];
        }

        DescriptiveWithMissing.standardize( new DoubleArrayList( copy ) );
        return copy;

    }

    /** 
     */
    public DesignElementDataVector toDesignElementDataVector() {
        return toDesignElementDataVector( null );
    }

    /**
     * @param updatedQuantitationType
     * @return
     */
    public DesignElementDataVector toDesignElementDataVector( QuantitationType updatedQuantitationType ) {
        DesignElementDataVector result;
        if ( this.masked ) {
            result = ProcessedExpressionDataVector.Factory.newInstance();
            ( ( ProcessedExpressionDataVector ) result ).setRankByMax( rankByMax );
            ( ( ProcessedExpressionDataVector ) result ).setRankByMean( rankByMean );
        } else {
            result = RawExpressionDataVector.Factory.newInstance();
        }
        result.setExpressionExperiment( this.expressionExperiment );
        result.setBioAssayDimension( this.bioAssayDimension );
        if ( updatedQuantitationType == null ) {
            result.setQuantitationType( this.quantitationType );
        } else {
            result.setQuantitationType( updatedQuantitationType );
        }
        result.setDesignElement( designElement );
        result.setData( byteArrayConverter.doubleArrayToBytes( this.data ) );
        return result;
    }

}
