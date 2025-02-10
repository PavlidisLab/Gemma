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

import cern.colt.list.DoubleArrayList;
import lombok.Data;
import org.springframework.util.Assert;
import ubic.basecode.math.DescriptiveWithMissing;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.QuantitationTypeValueObject;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject;
import ubic.gemma.model.expression.bioAssay.BioAssayValueObject;
import ubic.gemma.model.expression.biomaterial.BioMaterialValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;

import javax.annotation.Nullable;
import java.util.*;

/**
 * Simple wrapper for a double[] that is derived from a DesignElementDataVector.
 *
 * @author paul
 */
@Data
public class DoubleVectorValueObject extends DataVectorValueObject {

    private static final long serialVersionUID = -5116242513725297615L;

    private double[] data = null;
    private boolean masked = false;
    /**
     * True if the data has been rearranged relative to the bioassay dimension (as a matter of practice the
     * bioassay dimension should be set to null if it is not valid; this boolean is an additional check)
     */
    private boolean reorganized = false;
    private Double pvalue;
    private Double rank;
    private Double rankByMax;
    private Double rankByMean;

    public DoubleVectorValueObject() {

    }

    public DoubleVectorValueObject( BulkExpressionDataVector dedv, ExpressionExperimentValueObject eevo,
            QuantitationTypeValueObject qtvo, BioAssayDimensionValueObject badVo, @Nullable Collection<Long> genes ) {
        super( dedv, eevo, qtvo, badVo, genes );
        QuantitationType qt = dedv.getQuantitationType();
        if ( !qt.getRepresentation().equals( PrimitiveType.DOUBLE ) ) {
            throw new IllegalArgumentException(
                    "Can only store double vectors, got " + qt + " " + qt.getRepresentation() );
        }
        if ( qt.getIsMaskedPreferred() ) {
            this.masked = true;
        }
        this.data = dedv.getDataAsDoubles();
        if ( dedv instanceof ProcessedExpressionDataVector ) {
            this.rankByMax = ( ( ProcessedExpressionDataVector ) dedv ).getRankByMax();
            this.rankByMean = ( ( ProcessedExpressionDataVector ) dedv ).getRankByMean();
        }

        List<BioAssayValueObject> bioAssays = this.getBioAssays();
        for ( int j = 0; j < bioAssays.size(); j++ ) {
            BioAssayValueObject bVo = bioAssays.get( j );
            if ( bVo.isOutlier() ) {
                data[j] = Double.NaN;
            }
        }
    }

    /**
     * Create a vector where we expect to have to create one or more gaps to match other vectors, defined by dimToMatch.
     *
     * @param dimToMatch ensure that the vector missing values to match the locations of any bioassays in dimToMatch
     *        that aren't in the dedv's bioAssayDimension.
     * @param genes genes
     * @param dedv dedv
     * @param vectorsBadVo BA dimension vo
     */
    public DoubleVectorValueObject( BulkExpressionDataVector dedv, ExpressionExperimentValueObject eevo, QuantitationTypeValueObject qtvo, BioAssayDimensionValueObject vectorsBadVo, @Nullable Collection<Long> genes, BioAssayDimension dimToMatch ) {
        this( dedv, eevo, qtvo, vectorsBadVo, genes );
        if ( dimToMatch.getBioAssays().size() != this.data.length ) {
            this.addGaps( dimToMatch );
        }
    }

    /**
     * Copy constructor.
     */
    protected DoubleVectorValueObject( DoubleVectorValueObject dvvo ) {
        super( dvvo );
        this.masked = dvvo.masked;
        this.reorganized = dvvo.reorganized;
        this.data = Arrays.copyOf( dvvo.getData(), dvvo.getData().length );
        this.pvalue = dvvo.pvalue;
        this.rank = dvvo.rank;
        this.rankByMax = dvvo.rankByMax;
        this.rankByMean = dvvo.rankByMean;
    }

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj )
            return true;
        if ( !( obj instanceof DoubleVectorValueObject ) ) {
            return false;
        }
        DoubleVectorValueObject other = ( DoubleVectorValueObject ) obj;
        return super.equals( obj )
                && Arrays.equals( data, other.data );
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    /**
     * Create a copy of this vector.
     * <p>
     * Use this if you intend to modify it as the original might be stored in a shared cache.
     */
    public DoubleVectorValueObject copy() {
        return new DoubleVectorValueObject( this );
    }

    /**
     * Crate a vector that is a slice of this one.
     * Create a vector that is a slice of another one. The bioassays chosen are as given in the supplied
     * bioassay dimension.
     *
     * @param subset a subset by which we are slicing
     * @param bad all we nee is the id, the name and the list of bioassays from this.S
     * @param vec VO
     */
    public SlicedDoubleVectorValueObject slice( ExpressionExperimentSubSet subset, BioAssayDimensionValueObject bad ) {
        Assert.isTrue( subset.getSourceExperiment().getId().equals( getExpressionExperiment().getId() ), "The subset must belong to " + getExpressionExperiment() + "." );
        return new SlicedDoubleVectorValueObject( this, subset, bad );
    }

    /**
     * @return data adjusted to mean 0, variance 1.
     */

    public double[] standardize() {
        /*
         * DoubleArrayList constructor does not make a copy, so we have to make one.
         */
        double[] copy = new double[this.data.length];
        System.arraycopy( data, 0, copy, 0, data.length );

        DescriptiveWithMissing.standardize( new DoubleArrayList( copy ) );
        return copy;
    }

    private void addGaps( BioAssayDimension dimToMatch ) {
        BioAssayDimensionValueObject sourceBioAssayDimension = new BioAssayDimensionValueObject( dimToMatch );
        List<BioAssayValueObject> dimToMatchBioAssays = sourceBioAssayDimension.getBioAssays();

        double[] expandedData = new double[dimToMatch.getBioAssays().size()];
        BioAssayDimension expandedDim = BioAssayDimension.Factory.newInstance();
        expandedDim.setDescription( "Expanded bioassay dimension based on " + this.getBioAssayDimension().getName() );
        expandedDim.setName( "Expanded bioassay dimension based on " + this.getBioAssayDimension().getName() );

        Map<BioMaterialValueObject, BioAssayValueObject> bmap = new HashMap<>();
        ArrayDesignValueObject arrayDesign = null;
        for ( BioAssayValueObject b : this.getBioAssays() ) {

            bmap.put( b.getSample(), b );
            arrayDesign = b.getArrayDesign();
        }

        List<BioAssayValueObject> expandedBioAssays = new ArrayList<>();
        int i = 0;
        int indexInUngappedData = 0;
        for ( BioAssayValueObject b : dimToMatchBioAssays ) {

            BioMaterialValueObject bm = b.getSample();

            if ( !bmap.containsKey( bm ) ) {
                /*
                 * This is one where we have to put in a gap.
                 */
                expandedData[i] = Double.NaN;
                BioAssayValueObject placeholder = new BioAssayValueObject( -1L );
                placeholder.setName( "Missing bioassay for biomaterial=" + bm + " that was not run on " + arrayDesign );
                placeholder.setDescription(
                        "This is to represent a biomaterial that was not run on the platform for the rest of the bioassay dimension." );
                placeholder.setArrayDesign( arrayDesign );
                placeholder.setSample( bm );
                expandedBioAssays.add( placeholder );
            } else {
                expandedBioAssays.add( this.getBioAssays().get( indexInUngappedData ) );
                expandedData[i] = data[indexInUngappedData];
                indexInUngappedData++;
            }
            i++;
        }

        assert dimToMatchBioAssays.size() == expandedBioAssays.size();

        this.data = expandedData;
        BioAssayDimensionValueObject newBad = new BioAssayDimensionValueObject( -1L );
        newBad.setSourceBioAssayDimension( sourceBioAssayDimension );
        newBad.setIsSubset( true ); // not exactly, but have to make clear it's not real.
        newBad.clearBioAssays();
        newBad.addBioAssays( expandedBioAssays );
        newBad.setName( "Expanded bioassay dimension based on " + this.getBioAssayDimension().getName() );
        this.setBioAssayDimension( newBad );
    }
}
