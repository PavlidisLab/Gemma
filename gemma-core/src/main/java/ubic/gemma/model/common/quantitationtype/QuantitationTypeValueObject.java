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

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import ubic.gemma.model.annotations.WithheldFromApi;
import ubic.gemma.model.annotations.WithheldFromApi.Reason;
import ubic.gemma.model.common.IdentifiableValueObject;
import ubic.gemma.model.expression.bioAssayData.DataVector;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import org.springframework.lang.Nullable;

/**
 * Value object for the {@link QuantitationType}.
 * <p>
 * If {@link #expressionExperimentId} is set, then this QT is EE-associated, and can be distinguished from other QTs
 * with the same ID used in different experiments as per {@link #equals(Object)} and {@link #hashCode()}. In this case,
 * the {@link #vectorType} is also known, but could be null.
 *
 * @author thea
 */
@SuppressWarnings({ "unused", "WeakerAccess" }) // Used in frontend
@Data
@EqualsAndHashCode(of = { "expressionExperimentId" }, callSuper = true)
@ToString(of = { "name" }, callSuper = true)
public class QuantitationTypeValueObject extends IdentifiableValueObject<QuantitationType> {

    private static final long serialVersionUID = 7537853492100102404L;

    private String name;
    private String description;

    @Schema(implementation = GeneralType.class)
    private String generalType;
    @Schema(implementation = StandardQuantitationType.class)
    private String type;
    @Schema(implementation = PrimitiveType.class)
    private String representation;
    @Schema(implementation = ScaleType.class)
    private String scale;

    private boolean isBackground;
    private boolean isBackgroundSubtracted;
    private boolean isBatchCorrected;
    private boolean isNormalized;
    private boolean isRatio;
    private boolean isRecomputedFromRawData;

    /**
     * True when this quantitation type is preferred in ANY of the three senses Gemma records: the preferred
     * processed data, the preferred single-cell data, or the preferred masked data.
     * <p>
     * 🛑 It is an OR, so it does not identify one quantitation type per experiment. A single-cell dataset
     * normally has three that answer true — the single-cell counts, the aggregate, and the masked aggregate —
     * and after a re-aggregation the superseded cut keeps its masked-preferred flag, so two subset groups can
     * each hold one. A client picking "the live cut" by this field alone resolved the wrong group on 2 of 92
     * single-cell datasets (uib, 2026-09-03). {@link #isMaskedPreferred} and {@link #isSingleCellPreferred}
     * say which flag fired; {@code isPreferred && !isMaskedPreferred && !isSingleCellPreferred} is the
     * processed-data sense on its own.
     */
    private boolean isPreferred;
    @Deprecated
    @Schema(deprecated = true)
    private boolean isMaskedPreferred;
    /**
     * True when this is the preferred single-cell quantitation type.
     * <p>
     * One of the three flags {@link #isPreferred} ORs together. Exposed because it was the only one of them a
     * client could not see, which left {@code isPreferred} true with no way to tell why.
     */
    private boolean isSingleCellPreferred;

    /**
     * Associated expression experiment ID.
     * <p>
     * This is unnecessary in the context of the RESTful API because vector types are always retrieved when the
     * associated ExpressionExperiment is known.
     */
    @WithheldFromApi(value = Reason.REDUNDANT,
            comment = "Any serialized use of QuantitationTypeValueObjects rely on already having the experiment ID on hand")
    private Long expressionExperimentId = null;

    /**
     * Vector type this QT is associated to.
     * <p>
     * This only makes sense in the context of an associated EE.
     */
    @Nullable
    private String vectorType = null;

    public QuantitationTypeValueObject() {
        super();
    }

    public QuantitationTypeValueObject( QuantitationType qt ) {
        super( qt );
        this.name = qt.getName();
        this.description = qt.getDescription();

        this.generalType = qt.getGeneralType().toString();
        this.type = qt.getType().toString();
        this.scale = qt.getScale().toString();
        this.representation = qt.getRepresentation().toString();

        this.isBackground = qt.getIsBackground();
        this.isBackgroundSubtracted = qt.getIsBackgroundSubtracted();
        this.isBatchCorrected = qt.getIsBatchCorrected();
        this.isNormalized = qt.getIsNormalized();
        this.isRatio = qt.getIsRatio();
        this.isRecomputedFromRawData = qt.getIsRecomputedFromRawData();

        // for QT VO, we don't to expose the intricacies of the preferred flag, there is already a vectorType for the
        // purpose of telling which type of vector is preferred
        this.isPreferred = qt.getIsPreferred() || qt.getIsSingleCellPreferred() || qt.getIsMaskedPreferred();
        this.isMaskedPreferred = qt.getIsMaskedPreferred();
        this.isSingleCellPreferred = qt.getIsSingleCellPreferred();
    }

    /**
     * Create a {@link QuantitationType} VO in the context of an associated experiment.
     * <p>
     * Note that an associated EE does not imply that the QT is used in processed/raw vectors. There are generic QTs to
     * represent data transformation such as masking that require no storage of vectors.
     *
     * @param expressionExperiment associated experiment
     * @param vectorType vector type if applicable, otherwise null
     */
    public QuantitationTypeValueObject( QuantitationType qt, ExpressionExperiment expressionExperiment, @Nullable Class<? extends DataVector> vectorType ) {
        this( qt );
        this.expressionExperimentId = expressionExperiment.getId();
        if ( vectorType != null ) {
            this.vectorType = vectorType.getName();
        }
    }

    // because of the 'is' prefix, we need to override Lombok's getter/setter naming

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

    public boolean getIsNormalized() {
        return this.isNormalized;
    }

    public void setIsNormalized( boolean isNormalized ) {
        this.isNormalized = isNormalized;
    }

    public boolean getIsRatio() {
        return this.isRatio;
    }

    public void setIsRatio( boolean isRatio ) {
        this.isRatio = isRatio;
    }

    public boolean getIsRecomputedFromRawData() {
        return isRecomputedFromRawData;
    }

    public void setIsRecomputedFromRawData( boolean isRecomputedFromRawData ) {
        this.isRecomputedFromRawData = isRecomputedFromRawData;
    }

    public boolean getIsPreferred() {
        return this.isPreferred;
    }

    public void setIsPreferred( boolean isPreferred ) {
        this.isPreferred = isPreferred;
    }

    @Deprecated
    public boolean getIsMaskedPreferred() {
        return this.isMaskedPreferred;
    }

    public boolean getIsSingleCellPreferred() {
        return this.isSingleCellPreferred;
    }

    public void setIsSingleCellPreferred( boolean isSingleCellPreferred ) {
        this.isSingleCellPreferred = isSingleCellPreferred;
    }

    @Deprecated
    public void setIsMaskedPreferred( boolean isMaskedPreferred ) {
        this.isMaskedPreferred = isMaskedPreferred;
    }
}