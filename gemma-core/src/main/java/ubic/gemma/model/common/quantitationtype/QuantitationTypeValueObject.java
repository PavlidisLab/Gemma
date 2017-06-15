/*
 * The Gemma project
 * 
 * Copyright (c) 2006 University of British Columbia
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

import ubic.gemma.model.IdentifiableValueObject;

import java.io.Serializable;
import java.util.*;

/**
 * @author thea
 */
public class QuantitationTypeValueObject extends IdentifiableValueObject<QuantitationType> implements Serializable {

    private static final long serialVersionUID = 7537853492100102404L;
    private String description;
    private String generalType;
    private boolean isBackground;
    private boolean isBackgroundSubtracted;
    private boolean isBatchCorrected;
    private boolean isMaskedPreferred;
    private boolean isNormalized;
    private boolean isPreferred;
    private boolean isRatio;
    private boolean isRecomputedFromRawData = false;
    private String name;
    private String representation;
    private String scale;
    private String type;

    /**
     * Required when using the class as a spring bean.
     */
    public QuantitationTypeValueObject() {
    }

    /**
     * Constructor to build value object from QuantitationType
     */
    public QuantitationTypeValueObject( QuantitationType qt ) {
        super( qt.getId() );
        this.name = qt.getName();
        this.description = qt.getDescription();
        this.generalType = qt.getGeneralType().toString();
        this.isBackground = qt.getIsBackground() != null && qt.getIsBackground();
        this.isBackgroundSubtracted = qt.getIsBackgroundSubtracted() != null && qt.getIsBackgroundSubtracted() ;
        this.isBatchCorrected =  qt.getIsBatchCorrected() != null && qt.getIsBatchCorrected() ;
        this.isMaskedPreferred = qt.getIsMaskedPreferred() != null && qt.getIsMaskedPreferred() ;
        this.isNormalized = qt.getIsNormalized() != null && qt.getIsNormalized() ;
        this.isPreferred = qt.getIsPreferred() != null && qt.getIsPreferred() ;
        this.isRatio = qt.getIsRatio() != null && qt.getIsRatio() ;
        this.representation = qt.getRepresentation().toString() ;
        this.scale = qt.getScale().toString() ;
        this.type = qt.getType().toString() ;
        this.isRecomputedFromRawData = qt.getIsRecomputedFromRawData() != null && qt.getIsRecomputedFromRawData() ;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription( String description ) {
        this.description = description;
    }

    public String getGeneralType() {
        return this.generalType;
    }

    public void setGeneralType( String generalType ) {
        this.generalType = generalType;
    }

    public boolean getIsBackground() {
        return this.isBackground;
    }

    public void setIsBackground( boolean isBackground ) {
        this.isBackground = isBackground;
    }

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
     * <p>
     * If the data represented is a missing-value masked version of the preferred data.
     * </p>
     */
    public boolean getIsMaskedPreferred() {
        return this.isMaskedPreferred;
    }

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
     * <p>
     * Indicates whether the quantitation type is expressed as a ratio. This has a natural impact on the interpretation.
     * If false, the value is "absolute".
     * </p>
     */
    public boolean getIsRatio() {
        return this.isRatio;
    }

    public void setIsRatio( boolean isRatio ) {
        this.isRatio = isRatio;
    }

    public String getName() {
        return name;
    }

    public void setName( String name ) {
        this.name = name;
    }

    public String getRepresentation() {
        return this.representation;
    }

    public void setRepresentation( String representation ) {
        this.representation = representation;
    }

    public String getScale() {
        return this.scale;
    }

    public void setScale( String scale ) {
        this.scale = scale;
    }

    public String getType() {
        return this.type;
    }

    public void setType( String type ) {
        this.type = type;
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

}