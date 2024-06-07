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
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Hibernate;
import ubic.gemma.model.common.IdentifiableValueObject;
import ubic.gemma.model.annotations.GemmaWebOnly;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.CharacteristicValueObject;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssay.BioAssayValueObject;
import ubic.gemma.model.expression.experiment.*;

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
    @GemmaWebOnly
    private String assayName;
    @GemmaWebOnly
    private String assayDescription;

    @GemmaWebOnly
    private String fastqHeaders = null;

    /**
     * Related {@link BioAssay} IDs.
     */
    private Collection<Long> bioAssayIds = new HashSet<>();
    private Collection<CharacteristicValueObject> characteristics = new HashSet<>();

    /*
     * Map of (informative) categories to values (for this biomaterial). This is only used for display so we don't need ids as well.
     */
    @GemmaWebOnly
    private Map<String, String> characteristicValues = new HashMap<>();

    /**
     * Map of categories to original text values (for this biomaterial).
     * This is only used for display and will only be populated if the original value is different from the value.
     */
    @GemmaWebOnly
    private Map<String, String> characteristicOriginalValues = new HashMap<>();

    /**
     * Indicate if this is using the {@link #fVBasicVOs} or {@link #factorValueObjects} for representing factor values.
     */
    @JsonIgnore
    private boolean basicFVs;

    @JsonIgnore
    private Collection<FactorValueBasicValueObject> fVBasicVOs = new HashSet<>();

    @JsonIgnore
    private Collection<FactorValueValueObject> factorValueObjects = new HashSet<>();

    /**
     * Map of ids (fv133) to a representation of the value (for this biomaterial.)
     */
    @GemmaWebOnly
    private Map<String, String> factorValues;

    /**
     * Map of factor ids (factor232) to factor value (id or the actual value) for this biomaterial.
     */
    @GemmaWebOnly
    private Map<String, String> factorIdToFactorValueId;

    /**
     * Map of ids (factor232) to a representation of the factor (e.g., the name).
     */
    @Deprecated
    @Schema(description = "This is deprecated, use the `factorValues` collection instead.", deprecated = true)
    private Map<String, String> factors;

    @GemmaWebOnly
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
        this( bm, false );
    }

    public BioMaterialValueObject( BioMaterial bm, boolean basic ) {
        super( bm );
        this.name = bm.getName();
        this.description = bm.getDescription();

        for ( Characteristic ch : bm.getCharacteristics() ) {
            this.characteristics.add( new CharacteristicValueObject( ch ) );
        }

        this.basicFVs = basic;
        this.factors = new HashMap<>();
        this.factorValues = new HashMap<>();
        this.factorIdToFactorValueId = new HashMap<>();
        for ( FactorValue fv : bm.getFactorValues() ) {
            if ( basicFVs ) {
                this.fVBasicVOs.add( new FactorValueBasicValueObject( fv ) );
            } else {
                this.factorValueObjects.add( new FactorValueValueObject( fv ) );
            }
            ExperimentalFactor factor = fv.getExperimentalFactor();
            String factorId = String.format( "factor%d", factor.getId() );
            String factorValueId = String.format( "fv%d", fv.getId() );
            if ( Hibernate.isInitialized( factor ) ) {
                this.factors.put( factorId, factor.getName() );
            }
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

        // used for display of characteristics in the biomaterial experimental design editor view.
        for ( Characteristic c : bm.getCharacteristics() ) {
            if ( StringUtils.isBlank( c.getCategory() ) ) {
                continue;
            }
            this.characteristicValues.put( c.getCategory(), c.getValue() );
            if ( c.getOriginalValue() != null && !c.getOriginalValue().equals( c.getValue() ) ) {
                this.characteristicOriginalValues.put( c.getCategory(), c.getOriginalValue() );
            }
        }
    }

    public BioMaterialValueObject( BioMaterial bm, BioAssay ba ) {
        this( bm );
        BioAssayValueObject baVo = new BioAssayValueObject( ba, false );
        this.bioAssayIds.add( baVo.getId() );
        this.assayName = ba.getName();
        this.assayDescription = ba.getDescription();
        this.assayName = ba.getName();
        this.assayDescription = ba.getDescription();
        this.assayProcessingDate = ba.getProcessingDate();
        this.fastqHeaders = ba.getFastqHeaders() == null ? "" : ba.getFastqHeaders();
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
