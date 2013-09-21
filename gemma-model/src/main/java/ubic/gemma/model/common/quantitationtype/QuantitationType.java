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

import ubic.gemma.model.common.Describable;

/**
 * 
 */
public abstract class QuantitationType extends Describable {

    /**
     * Constructs new instances of {@link ubic.gemma.model.common.quantitationtype.QuantitationType}.
     */
    public static final class Factory {
        /**
         * Constructs a new instance of {@link ubic.gemma.model.common.quantitationtype.QuantitationType}.
         */
        public static ubic.gemma.model.common.quantitationtype.QuantitationType newInstance() {
            return new ubic.gemma.model.common.quantitationtype.QuantitationTypeImpl();
        }

    }

    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = -9139594736279728431L;
    private Boolean isBackground;

    private ubic.gemma.model.common.quantitationtype.PrimitiveType representation;

    private ubic.gemma.model.common.quantitationtype.GeneralType generalType;

    private ubic.gemma.model.common.quantitationtype.StandardQuantitationType type;

    private ubic.gemma.model.common.quantitationtype.ScaleType scale;

    private Boolean isPreferred;

    private Boolean isNormalized;

    private Boolean isBackgroundSubtracted;

    private Boolean isRatio;

    private Boolean isMaskedPreferred;

    private Boolean isBatchCorrected;

    /**
     * No-arg constructor added to satisfy javabean contract
     * 
     * @author Paul
     */
    public QuantitationType() {
    }

    /**
     * 
     */
    public ubic.gemma.model.common.quantitationtype.GeneralType getGeneralType() {
        return this.generalType;
    }

    /**
     * 
     */
    public Boolean getIsBackground() {
        return this.isBackground;
    }

    /**
     * 
     */
    public Boolean getIsBackgroundSubtracted() {
        return this.isBackgroundSubtracted;
    }

    /**
     * 
     */
    public Boolean getIsBatchCorrected() {
        return this.isBatchCorrected;
    }

    /**
     * <p>
     * If the data represented is a missing-value masked version of the preferred data.
     * </p>
     */
    public Boolean getIsMaskedPreferred() {
        return this.isMaskedPreferred;
    }

    /**
     * 
     */
    public Boolean getIsNormalized() {
        return this.isNormalized;
    }

    /**
     * 
     */
    public Boolean getIsPreferred() {
        return this.isPreferred;
    }

    /**
     * <p>
     * Indicates whether the quantitation type is expressed as a ratio (e.g., of expression to a reference or
     * pseudo-reference). This has a natural impact on the interpretation. If false, the value is "absolute".
     * </p>
     */
    public Boolean getIsRatio() {
        return this.isRatio;
    }

    /**
     * 
     */
    public ubic.gemma.model.common.quantitationtype.PrimitiveType getRepresentation() {
        return this.representation;
    }

    /**
     * 
     */
    public ubic.gemma.model.common.quantitationtype.ScaleType getScale() {
        return this.scale;
    }

    /**
     * 
     */
    public ubic.gemma.model.common.quantitationtype.StandardQuantitationType getType() {
        return this.type;
    }

    public void setGeneralType( ubic.gemma.model.common.quantitationtype.GeneralType generalType ) {
        this.generalType = generalType;
    }

    public void setIsBackground( Boolean isBackground ) {
        this.isBackground = isBackground;
    }

    public void setIsBackgroundSubtracted( Boolean isBackgroundSubtracted ) {
        this.isBackgroundSubtracted = isBackgroundSubtracted;
    }

    public void setIsBatchCorrected( Boolean isBatchCorrected ) {
        this.isBatchCorrected = isBatchCorrected;
    }

    public void setIsMaskedPreferred( Boolean isMaskedPreferred ) {
        this.isMaskedPreferred = isMaskedPreferred;
    }

    public void setIsNormalized( Boolean isNormalized ) {
        this.isNormalized = isNormalized;
    }

    public void setIsPreferred( Boolean isPreferred ) {
        this.isPreferred = isPreferred;
    }

    public void setIsRatio( Boolean isRatio ) {
        this.isRatio = isRatio;
    }

    public void setRepresentation( ubic.gemma.model.common.quantitationtype.PrimitiveType representation ) {
        this.representation = representation;
    }

    public void setScale( ubic.gemma.model.common.quantitationtype.ScaleType scale ) {
        this.scale = scale;
    }

    public void setType( ubic.gemma.model.common.quantitationtype.StandardQuantitationType type ) {
        this.type = type;
    }

}