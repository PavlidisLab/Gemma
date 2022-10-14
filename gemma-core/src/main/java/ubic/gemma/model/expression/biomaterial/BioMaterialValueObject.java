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
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.StringUtils;
import ubic.gemma.model.IdentifiableValueObject;
import ubic.gemma.model.annotations.GemmaWebOnly;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssay.BioAssayValueObject;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.expression.experiment.FactorValueBasicValueObject;
import ubic.gemma.model.expression.experiment.FactorValueValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.CharacteristicValueObject;

import java.io.Serializable;
import java.util.*;

/**
 * @author lukem
 */
@SuppressWarnings({ "unused", "WeakerAccess" }) // Used in frontend
@Data
@EqualsAndHashCode(of = { "name" }, callSuper = true)
public class BioMaterialValueObject extends IdentifiableValueObject<BioMaterial> implements Serializable {

    private static final String CHARACTERISTIC_DELIMITER = "::::";
    private static final long serialVersionUID = -145137827948521045L;

    private String name;
    private String description;
    @GemmaWebOnly
    private String assayName;
    @GemmaWebOnly
    private String assayDescription;
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
    private Map<String, String> factors;

    @GemmaWebOnly
    private Date assayProcessingDate;

    /**
     * Required when using the class as a spring bean.
     */
    public BioMaterialValueObject() {
    }

    public BioMaterialValueObject( Long id ) {
        super( id );
    }

    public BioMaterialValueObject( BioMaterial bm ) {
        this( bm, false );
    }

    public BioMaterialValueObject( BioMaterial bm, boolean basic ) {
        super( bm.getId() );
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
            this.factors.put( factorId, factor.getName() );
            this.factorValues.put( factorValueId, this.getFactorValueString( fv ) );

            if ( fv.getMeasurement() == null ) {
                this.factorIdToFactorValueId.put( factorId, factorValueId );
            } else {
                /*
                 * use the actual value, not the factorvalue id.
                 */
                this.factorIdToFactorValueId.put( factorId, factorValues.get( factorValueId ) );
            }
        }

        // used for display of characteristics in the biomaterial experimental design editor view.
        for ( Characteristic c : bm.getCharacteristics() ) {
            if ( StringUtils.isBlank( c.getCategory() ) ) {
                continue;
            }
            this.characteristicValues.put( c.getCategory(), c.getValue() );
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
    }

    @JsonProperty("factorValues")
    public Collection<? extends IdentifiableValueObject> getFactorValues() {
        return basicFVs ? fVBasicVOs : factorValueObjects;
    }

    /**
     * @deprecated use {@link #getFactorValues()}
     */
    @Deprecated
    @JsonProperty("factorValueObjects")
    public Collection<? extends IdentifiableValueObject> getFactorValueObjects() {
        return basicFVs ? fVBasicVOs : factorValueObjects;
    }

    /**
     * Format the value as a string, either using the characteristic, value or measurement.
     */
    private String getFactorValueString( FactorValue value ) {
        if ( !value.getCharacteristics().isEmpty() ) {
            return StringUtils.join( value.getCharacteristics(), BioMaterialValueObject.CHARACTERISTIC_DELIMITER );
        } else if ( value.getMeasurement() != null ) {
            return value.getMeasurement().getValue();
        } else {
            return value.getValue();
        }
    }

    @Override
    public String toString() {
        return "BioMaterialValueObject{" +
                "assayName='" + assayName + '\'' +
                ", id=" + id +
                '}';
    }
}
