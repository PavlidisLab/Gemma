/*
 * The Gemma project Copyright (c) 2009 University of British Columbia Licensed under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing permissions and limitations
 * under the License.
 */
package ubic.gemma.model.expression.experiment;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.Hibernate;
import ubic.gemma.model.IdentifiableValueObject;
import ubic.gemma.model.annotations.GemmaRestOnly;
import ubic.gemma.model.annotations.GemmaWebOnly;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.CharacteristicValueObject;
import ubic.gemma.model.common.measurement.MeasurementValueObject;

import javax.annotation.Nullable;
import java.util.List;
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

    /**
     * A unique ontology identifier (i.e. IRI) for this factor value.
     */
    @GemmaRestOnly
    private String ontologyId;

    /**
     * ID of the experimental factor this FV belongs to.
     */
    private Long factorId;

    /**
     * Indicate if this FactorValue is a measurement.
     * @deprecated simply check if {@link #getMeasurementObject()} is non-null instead
     */
    @Deprecated
    @JsonProperty("isMeasurement")
    private boolean measurement;

    /**
     * Measurement object if this FactorValue is a measurement.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("measurement")
    private MeasurementValueObject measurementObject;

    /**
     * Characteristics.
     */
    private List<CharacteristicValueObject> characteristics;

    /**
     * Statements
     */
    private List<StatementValueObject> statements;

    // fields of the characteristic being focused on
    // this is used by the FV editor to model each individual characteristic with its FV
    /**
     * It could be the id of the measurement if there is no characteristic.
     */
    @GemmaWebOnly
    private Long charId;
    @GemmaWebOnly
    private String category;
    @GemmaWebOnly
    private String categoryUri;
    @GemmaWebOnly
    private String description;
    @GemmaWebOnly
    private String factorValue;
    @GemmaWebOnly
    private String value;
    @GemmaWebOnly
    private String valueUri;
    @GemmaWebOnly
    private String predicate;
    @GemmaWebOnly
    private String predicateUri;
    @GemmaWebOnly
    private String object;
    @GemmaWebOnly
    private String objectUri;
    @GemmaWebOnly
    private String secondPredicate;
    @GemmaWebOnly
    private String secondPredicateUri;
    @GemmaWebOnly
    private String secondObject;
    @GemmaWebOnly
    private String secondObjectUri;

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
     * Create a FactorValue VO focusing on a specific statement.
     * <p>
     * Prefer {@link #FactorValueValueObject(FactorValue, Statement)} since this should never hold a "plain"
     * characteristic, but always a statement of the factor value.
     *
     * @param value value
     * @param c     specific characteristic we're focusing on (yes, this is confusing). This is necessary if the factor
     *              value has multiple characteristics. DO NOT pass in the experimental factor category, this just
     *              confuses things. If c is null, the plain "value" is used.
     */
    public FactorValueValueObject( FactorValue value, @Nullable Characteristic c ) {
        super( value );
        this.factorValue = FactorValueUtils.getSummaryString( value );
        this.factorId = value.getExperimentalFactor().getId();

        // make sure we fill in the category for this if no characteristic is being *focused* on
        if ( Hibernate.isInitialized( value.getExperimentalFactor() ) ) {
            Characteristic factorCategory = value.getExperimentalFactor().getCategory();
            if ( factorCategory != null ) {
                this.category = factorCategory.getCategory();
                this.categoryUri = factorCategory.getCategoryUri();
            }
        }

        if ( value.getMeasurement() != null ) {
            this.measurement = true;
            this.measurementObject = new MeasurementValueObject( value.getMeasurement() );
            if ( c == null ) {
                this.value = value.getMeasurement().getValue();
                this.charId = value.getMeasurement().getId();
            }
        }

        this.characteristics = value.getCharacteristics().stream()
                .sorted()
                .map( CharacteristicValueObject::new )
                .collect( Collectors.toList() );

        this.statements = value.getCharacteristics().stream()
                .sorted()
                .map( StatementValueObject::new )
                .collect( Collectors.toList() );

        // fill in the details of the *focused* characteristic
        if ( c != null ) {
            this.charId = c.getId();
            this.category = c.getCategory();
            this.categoryUri = c.getCategoryUri();
            this.value = c.getValue(); // clobbers if we set it already
            this.valueUri = c.getValueUri();
        }
    }

    /**
     * Create a FactorValue VO focusing on a specific statement.
     */
    public FactorValueValueObject( FactorValue fv, @Nullable Statement c ) {
        this( fv, ( Characteristic ) c );
        if ( c != null ) {
            this.predicate = c.getPredicate();
            this.predicateUri = c.getPredicateUri();
            this.object = c.getObject();
            this.objectUri = c.getObjectUri();
            this.secondPredicate = c.getSecondPredicate();
            this.secondPredicateUri = c.getSecondPredicateUri();
            this.secondObject = c.getSecondObject();
            this.secondObjectUri = c.getSecondObjectUri();
        }
    }

    /**
     * Create a FactorValue VO, the statement being focused on is automatically picked.
     */
    @Deprecated
    public FactorValueValueObject( FactorValue fv ) {
        this( fv, pickStatement( fv ) );
    }

    @Override
    public String toString() {
        return "FactorValueValueObject [factor=" + factorValue + ", value=" + value + "]";
    }

    /**
     * Pick an arbitrary characteristic from the factor value to represent it.
     */
    private static Statement pickStatement( FactorValue fv ) {
        Statement c;
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