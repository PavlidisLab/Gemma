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
import ubic.gemma.core.util.ListUtils;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.QuantitationTypeValueObject;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject;
import ubic.gemma.model.expression.bioAssay.BioAssayValueObject;
import ubic.gemma.model.expression.biomaterial.BioMaterialValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubsetValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;

import javax.annotation.Nullable;
import java.util.*;

/**
 * Value object for a {@link BulkExpressionDataVector} containing doubles.
 *
 * @author paul
 */
@Data
public class DoubleVectorValueObject extends DataVectorValueObject {

    /**
     * The data of this vector.
     */
    private double[] data;
    @Nullable
    private int[] numberOfCells;
    /**
     * Indicate if this vector is "masked", i.e. it is processed.
     */
    private boolean masked;
    /**
     * True if the data has been rearranged relative to the bioassay dimension (as a matter of practice the
     * bioassay dimension should be set to null if it is not valid; this boolean is an additional check)
     */
    private boolean reorganized;

    private Double rank;
    private Double rankByMax;
    private Double rankByMean;

    /**
     * If this vector is associated to a statistical test (i.e. from a DE analysis), this is the P-value.
     */
    @Nullable
    private Double pvalue;

    public DoubleVectorValueObject() {

    }

    /**
     * @see #DoubleVectorValueObject(BulkExpressionDataVector, ExpressionExperimentValueObject, QuantitationTypeValueObject, BioAssayDimensionValueObject, ArrayDesignValueObject, Collection)
     */
    public DoubleVectorValueObject( BulkExpressionDataVector dedv, ExpressionExperimentValueObject eevo,
            QuantitationTypeValueObject qtvo, BioAssayDimensionValueObject badVo, ArrayDesignValueObject advo, @Nullable Collection<Long> genes ) {
        super( dedv, eevo, qtvo, badVo, advo, genes );
        QuantitationType qt = dedv.getQuantitationType();
        if ( !qt.getRepresentation().equals( PrimitiveType.DOUBLE ) ) {
            throw new IllegalArgumentException(
                    "Can only store double vectors, got " + qt + " " + qt.getRepresentation() );
        }
        if ( qt.getIsMaskedPreferred() ) {
            this.masked = true;
        }
        this.data = dedv.getDataAsDoubles();
        this.numberOfCells = dedv.getNumberOfCells() != null ? dedv.getNumberOfCells() : null;
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
     * @param dedv         dedv
     * @param eevo         a VO for the experiment
     * @param qtvo         a VO for the quantitation type
     * @param vectorsBadVo BA dimension vo
     * @param genes        a collection of gene IDs that correspond to the design element of this vector, or null to
     *                     ignore
     * @param dimToMatch   ensure that the vector missing values to match the locations of any bioassays in dimToMatch
     *                     that aren't in the dedv's bioAssayDimension. This will result in "gaps" where the provided
     *                     vector is lacking assays.
     */
    public DoubleVectorValueObject( BulkExpressionDataVector dedv, ExpressionExperimentValueObject eevo,
            QuantitationTypeValueObject qtvo, BioAssayDimensionValueObject vectorsBadVo,
            ArrayDesignValueObject advo, @Nullable Collection<Long> genes,
            BioAssayDimensionValueObject dimToMatch ) {
        this( dedv, eevo, qtvo, vectorsBadVo, advo, genes );
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
        this.numberOfCells = dvvo.numberOfCells;
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

    public SlicedDoubleVectorValueObject slice( ExpressionExperimentSubsetValueObject subset, BioAssayDimensionValueObject slicedBad ) {
        Map<BioAssayValueObject, Integer> ba2i = ListUtils.indexOfElements( getBioAssays() );
        int[] bioAssayIndexMap = new int[slicedBad.getBioAssays().size()];
        for ( int i = 0; i < slicedBad.getBioAssays().size(); i++ ) {
            bioAssayIndexMap[i] = ba2i.get( slicedBad.getBioAssays().get( i ) );
        }
        return slice( subset, slicedBad, bioAssayIndexMap );
    }

    /**
     * Crate a vector that is a slice of this one.
     * <p>
     * Create a vector that is a slice of another one. The bioassays chosen are as given in the supplied
     * bioassay dimension.
     *
     * @param subset        a subset by which we are slicing
     * @param slicedBad     all we need is the id, the name and the list of bioassays from this.
     * @param bioAssayIndex position of each sliced bioassay in the original vector's dimension to avoid costly
     *                      recomputation
     */
    public SlicedDoubleVectorValueObject slice( ExpressionExperimentSubsetValueObject subset,
            BioAssayDimensionValueObject slicedBad, int[] bioAssayIndex ) {
        Assert.isTrue( getExpressionExperiment() == null || Objects.equals( getExpressionExperiment().getId(), subset.getSourceExperimentId() ),
                "The subset must belong to " + getExpressionExperiment() + "." );
        Assert.isTrue( bioAssayIndex.length == slicedBad.getBioAssays().size(),
                "The bioassay index length must match the number of bioassays in the sliced dimension." );
        SlicedDoubleVectorValueObject slicedVector = new SlicedDoubleVectorValueObject();
        // because this is a 'slice', not a persistent one,
        slicedVector.setId( null );
        slicedVector.setSourceVectorId( getId() );
        slicedVector.setExpressionExperiment( subset );
        slicedVector.setBioAssayDimension( slicedBad );
        slicedVector.setMasked( isMasked() );
        slicedVector.setQuantitationType( getQuantitationType() );
        slicedVector.setDesignElement( getDesignElement() );
        slicedVector.setGenes( getGenes() );
        double[] slicedData = new double[slicedBad.getBioAssays().size()];
        List<BioAssayValueObject> bioAssays = slicedBad.getBioAssays();
        for ( int i = 0; i < bioAssays.size(); i++ ) {
            slicedData[i] = getData()[bioAssayIndex[i]];
        }
        slicedVector.setData( slicedData );
        // not copying ranks because slicing affects them, those can be recomputed if needed
        return slicedVector;
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

    private void addGaps( BioAssayDimensionValueObject sourceBioAssayDimension ) {
        List<BioAssayValueObject> dimToMatchBioAssays = sourceBioAssayDimension.getBioAssays();

        double[] expandedData = new double[sourceBioAssayDimension.getBioAssays().size()];

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
