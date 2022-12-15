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

public abstract class QuantitationType extends AbstractDescribable {

    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = -9139594736279728431L;

    /**
     * This will be false except for some Qts on two-colour platforms.
     */
    private Boolean isBackground = false;

    /**
     * True if this is explicitly background-subtracted by Gemma. This is not very important and would only apply to
     * two-colour platforms since we don't background-subtract otherwise.
     */
    private Boolean isBackgroundSubtracted;

    /**
     * Refers to batch correction by Gemma. This should only ever be true for the ProcessedData.
     */
    private Boolean isBatchCorrected = false;

    /**
     * This is useless because the processed data is always masked
     */
    private Boolean isMaskedPreferred;

    /**
     * This is pretty confusing since we don't make clear what we mean by "normalized", so this isn't that useful.
     * It might be wise to separate out "quantile normalized", but since we always quantile normalize ProcessedData, this
     * isn't very useful.
     */
    private Boolean isNormalized;

    /**
     * This is only useful for RawExpressionDataVectors; for the ProcessedData it is just confusing
     */
    private Boolean isPreferred;

    /**
     * This is also confusing: it is an attempt to capture whether we just used the data from GEO (or whatever) or went
     * back to raw CEL or fastq files.
     */
    private Boolean isRecomputedFromRawData = false;

    private Boolean isRatio;
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
    public Boolean getIsBackground() {
        return this.isBackground;
    }

    public void setIsBackground( Boolean isBackground ) {
        this.isBackground = isBackground;
    }

    /**
     * @return True if this is explicitly background-subtracted by Gemma (if it was background-subtracted before the data got to
     * us, we might not know)
     */
    public Boolean getIsBackgroundSubtracted() {
        return this.isBackgroundSubtracted;
    }

    public void setIsBackgroundSubtracted( Boolean isBackgroundSubtracted ) {
        this.isBackgroundSubtracted = isBackgroundSubtracted;
    }

    public Boolean getIsBatchCorrected() {
        return this.isBatchCorrected;
    }

    public void setIsBatchCorrected( Boolean isBatchCorrected ) {
        this.isBatchCorrected = isBatchCorrected;
    }

    /**
     * @return If the data represented is a missing-value masked version of the preferred data.
     */
    public Boolean getIsMaskedPreferred() {
        return this.isMaskedPreferred;
    }

    public void setIsMaskedPreferred( Boolean isMaskedPreferred ) {
        this.isMaskedPreferred = isMaskedPreferred;
    }

    public Boolean getIsNormalized() {
        return this.isNormalized;
    }

    public void setIsNormalized( Boolean isNormalized ) {
        this.isNormalized = isNormalized;
    }

    public Boolean getIsPreferred() {
        return this.isPreferred;
    }

    public void setIsPreferred( Boolean isPreferred ) {
        this.isPreferred = isPreferred;
    }

    /**
     * @return Indicates whether the quantitation type is expressed as a ratio (e.g., of expression to a reference or
     * pseudo-reference). This has a natural impact on the interpretation. If false, the value is "absolute".
     */
    public Boolean getIsRatio() {
        return this.isRatio;
    }

    public void setIsRatio( Boolean isRatio ) {
        this.isRatio = isRatio;
    }

    /**
     * @return the isRecomputedFromRawData
     */
    public Boolean getIsRecomputedFromRawData() {
        return isRecomputedFromRawData;
    }

    /**
     * @param isRecomputedFromRawData the isRecomputedFromRawData to set
     */
    public void setIsRecomputedFromRawData( Boolean isRecomputedFromRawData ) {
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

    public static final class Factory {

        public static QuantitationType newInstance() {
            return new QuantitationTypeImpl();
        }

    }

}