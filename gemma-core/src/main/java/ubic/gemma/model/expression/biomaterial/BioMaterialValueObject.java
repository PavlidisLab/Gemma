/*
 * The Gemma project
 *
 * Copyright (c) 2007 University of British Columbia
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
package ubic.gemma.model.expression.biomaterial;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.StringUtils;
import ubic.gemma.model.annotations.WithheldFromApi;
import ubic.gemma.model.annotations.WithheldFromApi.Reason;
import ubic.gemma.model.common.IdentifiableValueObject;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.expression.experiment.StatementValueObject;
import ubic.gemma.model.expression.experiment.Statement;
import ubic.gemma.model.common.description.CharacteristicValueObject;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssay.BioAssayValueObject;
import ubic.gemma.model.expression.experiment.*;

import org.springframework.lang.Nullable;
import java.util.*;

/**
 * @author lukem
 */
@SuppressWarnings({ "unused", "WeakerAccess" }) // Used in frontend
@Data
@EqualsAndHashCode(of = { "name" }, callSuper = true)
public class BioMaterialValueObject extends IdentifiableValueObject<BioMaterial> {

    private static final String CHARACTERISTIC_DELIMITER = "::::";
    private static final long serialVersionUID = -145137827948521045L;

    private String name;
    private String description;
    @WithheldFromApi(value = Reason.REDUNDANT,
            comment = "duplicates the BioAssay payload")
    private String assayName;
    @WithheldFromApi(value = Reason.REDUNDANT,
            comment = "duplicates the BioAssay payload")
    private String assayDescription;

    @WithheldFromApi(value = Reason.DISCLOSURE,
            comment = "raw sequencer header text; carries internal paths, run ids and submitter-local naming")
    private String fastqHeaders = null;

    /**
     * Related {@link BioAssay} IDs.
     */
    private Collection<Long> bioAssayIds = new HashSet<>();
    private Collection<CharacteristicValueObject> characteristics = new HashSet<>();

    /**
     * The same annotations as {@link #characteristics}, as statements — carrying the predicate and
     * object when a curator wrote one.
     * <p>
     * 🛑 A sample annotation can be predicated: {@code DatasetsWebService.tagToCharacteristic} builds
     * a {@link Statement} whenever the write carries a statement field, and it is the same method
     * that writes experiment tags. {@link CharacteristicValueObject} has no predicate or object, so
     * before this the sample payload flattened such an annotation to its subject on every read — a
     * curator could write a predicated sample characteristic and never see it again.
     * <p>
     * Null, and so absent from the payload, when the caller opted out with
     * {@code GET /datasets/{dataset}/samples?exclude=sample.statements}. It is 21.5% of that response
     * and every row in it also appears under {@link #characteristics} minus the predicate and object,
     * so a client that renders only subjects can decline it. It stays on by default: an opt-out that
     * defaults to off would put predicated sample characteristics back out of sight, which is the
     * thing this collection was added to end.
     */
    @Nullable
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Collection<StatementValueObject> statements = new HashSet<>();

    /**
     * The BioMaterial this one was derived from, or {@code null} if this is a sample in its own right.
     * <p>
     * This is <b>always null</b> on {@code GET /datasets/{dataset}/samples}, for every dataset including
     * single-cell ones, and that is the correct answer rather than missing data: that route returns the
     * dataset's own assays, whose samples are the biological samples themselves and so derive from nothing.
     * Derived samples are created only by single-cell aggregation, which files each {sample, cell type}
     * population as a BioMaterial pointing back at the sample it came from, and hangs it off an
     * {@link ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet} rather than the parent
     * dataset. So the populated values are reached through
     * {@code GET /datasets/{dataset}/subSets/{subSet}/samples}, where each value is the id of a sample the
     * parent route returned.
     * <p>
     * Do not read this field to decide whether a dataset is single-cell: it is null on single-cell datasets
     * too, so the test silently answers "no" everywhere. Use the pre-added {@code assay} ExperimentTag
     * (OBI_0002631 / OBI_0003109), or {@code GET /datasets/{dataset}/singleCellDimension}, which 404s for
     * datasets that have no single-cell data.
     */
    @Nullable
    @Schema(description = "The BioMaterial this sample was derived from, or null if it derives from nothing. "
            + "Always null on /datasets/{dataset}/samples, which returns the dataset's own samples — these "
            + "are biological samples and have no source. Derived samples come from single-cell aggregation "
            + "and are reached via /datasets/{dataset}/subSets/{subSet}/samples, where this holds the id of a "
            + "sample listed by the parent route. Not a single-cell indicator: it is null for single-cell "
            + "datasets too — use the assay ExperimentTag or /datasets/{dataset}/singleCellDimension.")
    private Long sourceBioMaterialId;

    /*
     * Map of (informative) categories to values (for this biomaterial). This is only used for display so we don't need ids as well.
     */
    @WithheldFromApi(value = Reason.INTERNAL_ONLY,
            comment = "category-keyed and therefore lossy; characteristics already serializes")
    private Map<String, String> characteristicValues = new HashMap<>();

    /**
     * Map of categories to original text values (for this biomaterial).
     * This is only used for display and will only be populated if the original value is different from the value.
     */
    @WithheldFromApi(value = Reason.INTERNAL_ONLY,
            comment = "category-keyed and therefore lossy; characteristics carries originalValue")
    private Map<String, String> characteristicOriginalValues = new HashMap<>();

    /**
     * Indicate if this is using the {@link #fVBasicVOs} or {@link #factorValueObjects} for representing factor values.
     */
    @JsonIgnore
    private boolean basicFVs;

    /**
     * 🛑 This field's {@link JsonIgnore} does not hide it: the payload carries an {@code fvbasicVOs}
     * key regardless.
     * <p>
     * Lombok generates the getter as {@code getFVBasicVOs()}, and a generated getter does not inherit
     * the field's annotations. Jackson then derives an implicit property name from each accessor
     * independently — {@code fVBasicVOs} from the field, {@code fvbasicVOs} from the getter (the bean
     * de-capitalization rule lowercases the whole leading run of capitals in {@code FVBasicVOs}).
     * Those two names are not equal, so Jackson never pairs the getter with the field, the
     * {@code @JsonIgnore} applies only to the unpaired field, and the getter serializes as a property
     * in its own right. The same shape on {@link #factorValueObjects} is harmless because its getter
     * is {@code getFactorValueObjects()}, whose implicit name does match its field.
     * <p>
     * Repeating {@code @JsonIgnore} on {@link #getFVBasicVOs()} closes it, and is deliberately NOT done
     * here: {@code fvbasicVOs} has live readers in the curation-agents repos, which are being moved off
     * it separately. Do not delete the getter either — {@code BioAssayDimensionValueObject} calls
     * {@link #getFactorValueObjects()} from Java, and Java-live is not the same as wire-live.
     */
    @JsonIgnore
    private Collection<FactorValueBasicValueObject> fVBasicVOs = new HashSet<>();

    @JsonIgnore
    private Collection<FactorValueValueObject> factorValueObjects = new HashSet<>();

    /**
     * Map of ids (fv133) to a representation of the value (for this biomaterial.)
     */
    @WithheldFromApi(value = Reason.REDUNDANT,
            comment = "category-keyed label map; the basic factor-value VOs already serialize")
    private Map<String, String> factorValues;

    /**
     * Map of factor ids (factor232) to factor value (id or the actual value) for this biomaterial.
     */
    @WithheldFromApi(value = Reason.INTERNAL_ONLY,
            comment = "id cross-reference built for the Web editor")
    private Map<String, String> factorIdToFactorValueId;

    @WithheldFromApi(value = Reason.REDUNDANT,
            comment = "duplicates the BioAssay payload")
    private Date assayProcessingDate;

    /**
     * Required when using the class as a spring bean.
     */
    public BioMaterialValueObject() {
        super();
    }

    public BioMaterialValueObject( Long id ) {
        super( id );
    }

    public BioMaterialValueObject( BioMaterial bm ) {
        this( bm, false, false );
    }

    public BioMaterialValueObject( BioMaterial bm, BioAssay ba ) {
        this( bm, false, false );
        BioAssayValueObject baVo = new BioAssayValueObject( ba, false );
        this.bioAssayIds.add( baVo.getId() );
        this.assayName = ba.getName();
        this.assayDescription = ba.getDescription();
        this.assayName = ba.getName();
        this.assayDescription = ba.getDescription();
        this.assayProcessingDate = ba.getProcessingDate();
        this.fastqHeaders = ba.getFastqHeaders() == null ? "" : ba.getFastqHeaders();
    }

    /**
     *
     * @param basic                             if true, populate {@link #fVBasicVOs} instead of {@link #factorValueObjects}.
     *                                          Note that basic FVs should be preferred for new code.
     * @param allFactorValuesAndCharacteristics whether to include all factor values and characteristics, including
     *                                          those inherited from the source biomaterial, otherwise only those from
     *                                          the sample will be included
     */
    public BioMaterialValueObject( BioMaterial bm, boolean basic, boolean allFactorValuesAndCharacteristics ) {
        super( bm );
        this.name = bm.getName();
        this.description = bm.getDescription();

        this.basicFVs = basic;
        this.factorValues = new HashMap<>();
        this.factorIdToFactorValueId = new HashMap<>();
        Set<FactorValue> fvs = allFactorValuesAndCharacteristics ? bm.getAllFactorValues() : bm.getFactorValues();
        for ( FactorValue fv : fvs ) {
            if ( basicFVs ) {
                this.fVBasicVOs.add( new FactorValueBasicValueObject( fv ) );
            } else {
                this.factorValueObjects.add( new FactorValueValueObject( fv ) );
            }
            ExperimentalFactor factor = fv.getExperimentalFactor();
            String factorId = String.format( "factor%d", factor.getId() );
            String factorValueId = String.format( "fv%d", fv.getId() );
            if ( fv.getMeasurement() != null ) {
                String value = fv.getMeasurement().getValue();
                this.factorValues.put( factorValueId, value );
                // for measurement, use the actual value, not the FV ID
                this.factorIdToFactorValueId.put( factorId, value );
            } else {
                this.factorValues.put( factorValueId, FactorValueUtils.getSummaryString( fv, BioMaterialValueObject.CHARACTERISTIC_DELIMITER ) );
                this.factorIdToFactorValueId.put( factorId, factorValueId );
            }
        }

        Set<Characteristic> cs = allFactorValuesAndCharacteristics ? bm.getAllCharacteristics() : bm.getCharacteristics();
        for ( Characteristic c : cs ) {
            this.characteristics.add( new CharacteristicValueObject( c ) );
            // Every annotation is listed here, predicated or not: one collection, one listing, the
            // predicate simply absent when nothing was said. A bare Characteristic is promoted for
            // the listing rather than hidden from it — it is the same annotation with less said.
            this.statements.add( c instanceof Statement
                    ? new StatementValueObject( ( Statement ) c )
                    : new StatementValueObject( Statement.Factory.newInstance( c ) ) );

            // used for display of characteristics in the biomaterial experimental design editor view.
            if ( StringUtils.isBlank( c.getCategory() ) ) {
                continue;
            }
            this.characteristicValues.put( c.getCategory(), c.getValue() );
            if ( c.getOriginalValue() != null && !c.getOriginalValue().equals( c.getValue() ) ) {
                this.characteristicOriginalValues.put( c.getCategory(), c.getOriginalValue() );
            }
        }

        this.sourceBioMaterialId = bm.getSourceBioMaterial() != null ? bm.getSourceBioMaterial().getId() : null;
    }

    @JsonProperty("factorValues")
    @ArraySchema(schema = @Schema(implementation = FactorValueBasicValueObject.class))
    public Collection<? extends IdentifiableValueObject> getFactorValues() {
        return basicFVs ? fVBasicVOs : factorValueObjects;
    }

    /**
     * @deprecated use {@link #getFactorValues()}
     */
    @Deprecated
    @JsonProperty("factorValueObjects")
    @ArraySchema(
            arraySchema = @Schema(description = "This property is redundant, use `factorValues` instead.", deprecated = true),
            schema = @Schema(implementation = FactorValueBasicValueObject.class))
    public Collection<? extends IdentifiableValueObject> getFactorValueObjects() {
        return basicFVs ? fVBasicVOs : factorValueObjects;
    }

    @Override
    public String toString() {
        return "BioMaterialValueObject{" +
                "assayName='" + assayName + '\'' +
                ", id=" + id +
                '}';
    }
}
