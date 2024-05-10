/*
 * The Gemma project.
 *
 * Copyright (c) 2006-2012 University of British Columbia
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
package ubic.gemma.model.common.quantitationtype;

import ubic.gemma.model.common.AbstractDescribable;

import java.io.Serializable;
import java.util.Objects;

public class QuantitationType extends AbstractDescribable implements Serializable {

    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = -9139594736279728431L;

    /**
     * This will be false except for some Qts on two-colour platforms.
     */
    private boolean isBackground;

    /**
     * True if this is explicitly background-subtracted by Gemma. This is not very important and would only apply to
     * two-colour platforms since we don't background-subtract otherwise.
     */
    private boolean isBackgroundSubtracted;

    /**
     * Refers to batch correction by Gemma. This should only ever be true for the ProcessedData.
     */
    private boolean isBatchCorrected;

    /**
     * Indicate if this quantitation is the preferred for processed data.
     * <p>
     * @deprecated this is useless as there can only be one QT for processed data per dataset.
     */
    @Deprecated
    private boolean isMaskedPreferred;

    /**
     * This is pretty confusing since we don't make clear what we mean by "normalized", so this isn't that useful.
     * It might be wise to separate out "quantile normalized", but since we always quantile normalize ProcessedData, this
     * isn't very useful.
     */
    private boolean isNormalized;

    /**
     * This is only useful for RawExpressionDataVectors; for the ProcessedData it is just confusing
     */
    private boolean isPreferred;

    /**
     * This is also confusing: it is an attempt to capture whether we just used the data from GEO (or whatever) or went
     * back to raw CEL or fastq files.
     */
    private boolean isRecomputedFromRawData = false;

    private boolean isRatio;
    private GeneralType generalType;
    private PrimitiveType representation;
    private ScaleType scale;
    private StandardQuantitationType type;

    /**
     * No-arg constructor added to satisfy javabean contract
     *
     * @author Paul
     */
    public QuantitationType() {
    }

    public GeneralType getGeneralType() {
        return this.generalType;
    }

    public void setGeneralType( GeneralType generalType ) {
        this.generalType = generalType;
    }

    /**
     * @return True if this is just a background measurement.
     */
    public boolean getIsBackground() {
        return this.isBackground;
    }

    public void setIsBackground( boolean isBackground ) {
        this.isBackground = isBackground;
    }

    /**
     * @return True if this is explicitly background-subtracted by Gemma (if it was background-subtracted before the data got to
     * us, we might not know)
     */
    public boolean getIsBackgroundSubtracted() {
        return this.isBackgroundSubtracted;
    }

    public void setIsBackgroundSubtracted( boolean isBackgroundSubtracted ) {
        this.isBackgroundSubtracted = isBackgroundSubtracted;
    }

    public boolean getIsBatchCorrected() {
        return this.isBatchCorrected;
    }

    public void setIsBatchCorrected( boolean isBatchCorrected ) {
        this.isBatchCorrected = isBatchCorrected;
    }

    /**
     * @return If the data represented is a missing-value masked version of the preferred data.
     */
    @Deprecated
    public boolean getIsMaskedPreferred() {
        return this.isMaskedPreferred;
    }

    @Deprecated
    public void setIsMaskedPreferred( boolean isMaskedPreferred ) {
        this.isMaskedPreferred = isMaskedPreferred;
    }

    public boolean getIsNormalized() {
        return this.isNormalized;
    }

    public void setIsNormalized( boolean isNormalized ) {
        this.isNormalized = isNormalized;
    }

    public boolean getIsPreferred() {
        return this.isPreferred;
    }

    public void setIsPreferred( boolean isPreferred ) {
        this.isPreferred = isPreferred;
    }

    /**
     * @return Indicates whether the quantitation type is expressed as a ratio (e.g., of expression to a reference or
     * pseudo-reference). This has a natural impact on the interpretation. If false, the value is "absolute".
     */
    public boolean getIsRatio() {
        return this.isRatio;
    }

    public void setIsRatio( boolean isRatio ) {
        this.isRatio = isRatio;
    }

    /**
     * @return the isRecomputedFromRawData
     */
    public boolean getIsRecomputedFromRawData() {
        return isRecomputedFromRawData;
    }

    /**
     * @param isRecomputedFromRawData the isRecomputedFromRawData to set
     */
    public void setIsRecomputedFromRawData( boolean isRecomputedFromRawData ) {
        this.isRecomputedFromRawData = isRecomputedFromRawData;
    }

    public PrimitiveType getRepresentation() {
        return this.representation;
    }

    public void setRepresentation( PrimitiveType representation ) {
        this.representation = representation;
    }

    public ScaleType getScale() {
        return this.scale;
    }

    public void setScale( ScaleType scale ) {
        this.scale = scale;
    }

    public StandardQuantitationType getType() {
        return this.type;
    }

    public void setType( StandardQuantitationType type ) {
        this.type = type;
    }


    @Override
    public boolean equals( Object object ) {
        if ( this == object ) {
            return true;
        }
        if ( !( object instanceof QuantitationType ) ) {
            return false;
        }
        final QuantitationType that = ( QuantitationType ) object;
        if ( getId() != null && that.getId() != null ) {
            return getId().equals( that.getId() );
        }
        return Objects.equals( getName(), that.getName() )
                && Objects.equals( getGeneralType(), that.getGeneralType() )
                && Objects.equals( getType(), that.getType() )
                && Objects.equals( getRepresentation(), that.getRepresentation() )
                && Objects.equals( getScale(), that.getScale() )
                && Objects.equals( getIsPreferred(), that.getIsPreferred() )
                && Objects.equals( getIsRatio(), that.getIsRatio() )
                && Objects.equals( getIsNormalized(), that.getIsNormalized() )
                && Objects.equals( getIsBackground(), that.getIsBackground() )
                && Objects.equals( getIsBackgroundSubtracted(), that.getIsBackgroundSubtracted() )
                && Objects.equals( getIsRecomputedFromRawData(), that.getIsRecomputedFromRawData() );
    }

    @Override
    public int hashCode() {
        return Objects.hash( getName(), getGeneralType(), getType(), getRepresentation(), getScale(), getIsPreferred(),
                getIsRatio(), getIsNormalized(), getIsBackground(), getIsBackgroundSubtracted(), getIsRecomputedFromRawData() );
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder( super.toString() );
        b.append( " General Type=" ).append( generalType )
                .append( " Type=" ).append( type )
                .append( " Scale=" ).append( scale )
                .append( " Representation=" ).append( representation );
        if ( isNormalized ) {
            b.append( " [Normalized]" );
        }
        if ( isBackground ) {
            b.append( " [Background]" );
        }
        if ( isBackgroundSubtracted ) {
            b.append( " [Background Subtracted]" );
        }
        if ( isRatio ) {
            b.append( " [Ratiometric]" );
        }
        if ( isBatchCorrected ) {
            b.append( " [Batch Corrected]" );
        }
        if ( isRecomputedFromRawData ) {
            b.append( " [Recomputed From Raw]" );
        }
        if ( isPreferred || isMaskedPreferred ) {
            b.append( " [Preferred]" );
        }
        return b.toString();
    }

    public static final class Factory {

        public static QuantitationType newInstance() {
            return new QuantitationType();
        }

        /**
         * Create a new QT with the same spec as the provided one.
         * <p>
         * Note: since this is a new instance, we don't copy the {@link #getId()}, {@link #getIsPreferred()} or
         * {@link #getIsMaskedPreferred()} over.
         */
        public static QuantitationType newInstance( QuantitationType quantitationType ) {
            QuantitationType result = newInstance();
            result.setName( quantitationType.getName() );
            result.setDescription( quantitationType.getDescription() );
            result.scale = quantitationType.scale;
            result.representation = quantitationType.representation;
            result.type = quantitationType.type;
            result.generalType = quantitationType.generalType;
            result.isNormalized = quantitationType.isNormalized;
            result.isRatio = quantitationType.isRatio;
            result.isBackground = quantitationType.isBackground;
            result.isBackgroundSubtracted = quantitationType.isBackgroundSubtracted;
            result.isBatchCorrected = quantitationType.isBatchCorrected;
            return result;
        }

    }

}