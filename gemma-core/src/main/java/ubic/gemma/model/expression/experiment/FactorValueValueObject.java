/*
 * The Gemma project Copyright (c) 2009 University of British Columbia Licensed under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing permissions and limitations
 * under the License.
 */
package ubic.gemma.model.expression.experiment;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import ubic.gemma.model.IdentifiableValueObject;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.genome.gene.phenotype.valueObject.CharacteristicBasicValueObject;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Each {@link FactorValue} can be associated with multiple characteristics (or with a measurement). However, for
 * flattening out the objects for client display, there is only one characteristic associated here.
 * <p>
 * Note: this used to be called FactorValueObject and now replaces the old FactorValueValueObject. Confusing!
 *
 * @author Paul
 * @deprecated aim towards using the {@link FactorValueBasicValueObject}. This one is confusing. Once usage of this
 *             type has been completely phased out, revise the BioMaterialValueObject and relevant DAOs and Services.
 */
@Deprecated
@Data
@EqualsAndHashCode(of = { "charId", "value" }, callSuper = true)
public class FactorValueValueObject extends IdentifiableValueObject<FactorValue> {

    private static final long serialVersionUID = 3378801249808036785L;

    private String category;
    private String categoryUri;
    private String description;
    private String factorValue;
    private String value;
    private String valueUri;
    /**
     * It could be the id of the measurement if there is no characteristic.
     */
    private Long charId;
    private Long factorId;
    @JsonProperty("isMeasurement")
    private boolean measurement = false;
    private Collection<CharacteristicBasicValueObject> characteristics;

    /**
     * Required when using the class as a spring bean.
     */
    public FactorValueValueObject() {
        super();
    }

    public FactorValueValueObject( Long id ) {
        super( id );
    }

    /**
     * @param      c     - specific characteristic we're focusing on (yes, this is confusing). This is necessary if the
     *                   FactorValue has multiple characteristics. DO NOT pass in the ExperimentalFactor category, this
     *                   just
     *                   confuses things.
     *                   If c is null, the plain "value" is used.
     * @param      value value
     */
    public FactorValueValueObject( FactorValue value, @Nullable Characteristic c ) {
        super( value );
        this.factorValue = FactorValueBasicValueObject.getSummaryString( value );
        this.factorId = value.getExperimentalFactor().getId();

        if ( value.getMeasurement() != null ) {
            this.setMeasurement( true );
            this.value = value.getMeasurement().getValue();
            this.charId = value.getMeasurement().getId();
        } else if ( c != null && c.getId() != null ) {
            this.charId = c.getId();
        } else {
            this.value = value.getValue();
        }

        if ( c != null ) {
            this.category = c.getCategory();
            this.value = c.getValue(); // clobbers if we set it already
            this.categoryUri = c.getCategoryUri();
            this.valueUri = c.getValueUri();
        }

        /*
         * Make sure we fill in the Category for this.
         */
        Characteristic factorCategory = value.getExperimentalFactor().getCategory();
        if ( this.category == null && factorCategory != null ) {
            this.category = factorCategory.getCategory();
            this.categoryUri = factorCategory.getCategoryUri();
        }

        this.characteristics = value.getCharacteristics().stream()
                .map( CharacteristicBasicValueObject::new )
                .collect( Collectors.toList() );
    }

    public FactorValueValueObject( FactorValue fv ) {
        this( fv, pickCharacteristic( fv ) );
    }

    @Override
    public String toString() {
        return "FactorValueValueObject [factor=" + factorValue + ", value=" + value + "]";
    }

    /**
     * Pick an arbitrary characteristic from the factor value to represent it.
     */
    private static Characteristic pickCharacteristic( FactorValue fv ) {
        Characteristic c;
        if ( fv.getCharacteristics().size() == 1 ) {
            c = fv.getCharacteristics().iterator().next();
        } else if ( fv.getCharacteristics().size() > 1 ) {
            /*
             * Inadequate! Want to capture them all - use FactorValueBasicValueObject!
             */
            c = fv.getCharacteristics().iterator().next();
        } else {
            c = null;
        }
        return c;
    }
}