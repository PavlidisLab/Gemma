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
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.Hibernate;
import ubic.gemma.model.IdentifiableValueObject;
import ubic.gemma.model.annotations.GemmaRestOnly;
import ubic.gemma.model.annotations.GemmaWebOnly;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.CharacteristicValueObject;
import ubic.gemma.model.common.measurement.MeasurementValueObject;

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

    @GemmaRestOnly
    private Long experimentalFactorId;

    @GemmaRestOnly
    private CharacteristicValueObject experimentalFactorCategory;

    /**
     * Measurement object if this FactorValue is a measurement.
     */
    @Schema(description = "This property exists only if a measurement")
    @JsonProperty("measurement")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private MeasurementValueObject measurementObject;

    private List<CharacteristicValueObject> characteristics;

    private List<StatementValueObject> statements;

    /**
     * Human-readable summary of the factor value.
     */
    private String summary;

    // fields of the characteristic being focused on
    // this is used by the FV editor to model each individual characteristic with its FV
    /**
     * ID of the experimental factor this FV belongs to.
     */
    @Schema(description = "Use `experimentalFactorId` instead.", deprecated = true)
    private Long factorId;
    /**
     * It could be the id of the measurement if there is no characteristic.
     */
    @Schema(description = "Use `measurement.id` or `characteristics.id` instead.", deprecated = true)
    private Long charId;
    @Schema(description = "Use experimentalFactorCategory.category instead.", deprecated = true)
    private String category;
    @Schema(description = "Use experimentalFactorCategory.categoryUri instead.", deprecated = true)
    private String categoryUri;
    @Deprecated
    @Schema(description = "This property is never filled nor used; use `summary` if you need a human-readable representation of this factor value.", deprecated = true)
    private String description;
    @Schema(description = "Use `summary` if you need a human-readable representation of this factor value or lookup the `characteristics` bag.", deprecated = true)
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
    @GemmaWebOnly
    private Boolean needsAttention;

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
     * Create a FactorValue VO.
     */
    public FactorValueValueObject( FactorValue value ) {
        super( value );

        this.experimentalFactorId = value.getExperimentalFactor().getId();
        this.factorId = value.getExperimentalFactor().getId();

        // make sure we fill in the category for this if no characteristic is being *focused* on
        if ( Hibernate.isInitialized( value.getExperimentalFactor() ) ) {
            Characteristic factorCategory = value.getExperimentalFactor().getCategory();
            if ( factorCategory != null ) {
                this.experimentalFactorCategory = new CharacteristicValueObject( factorCategory );
                this.category = factorCategory.getCategory();
                this.categoryUri = factorCategory.getCategoryUri();
            }
        }

        if ( value.getMeasurement() != null ) {
            this.charId = value.getMeasurement().getId();
            this.value = value.getMeasurement().getValue();
            this.factorValue = value.getMeasurement().getValue();
            this.measurementObject = new MeasurementValueObject( value.getMeasurement() );
        } else {
            this.factorValue = FactorValueUtils.getSummaryString( value );
        }

        this.characteristics = value.getCharacteristics().stream()
                .sorted()
                .map( CharacteristicValueObject::new )
                .collect( Collectors.toList() );

        this.statements = value.getCharacteristics().stream()
                .sorted()
                .map( StatementValueObject::new )
                .collect( Collectors.toList() );

        this.summary = FactorValueUtils.getSummaryString( value );

        this.needsAttention = value.getNeedsAttention();
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
    public FactorValueValueObject( FactorValue value, Characteristic c ) {
        this( value );
        if ( !value.getCharacteristics().contains( c ) ) {
            throw new IllegalArgumentException( "The focused characteristic does not belong to the factor value." );
        }
        if ( value.getMeasurement() != null ) {
            throw new IllegalArgumentException( "Continuous factor values cannot have a focused characteristic." );
        }
        // fill in the details of the *focused* characteristic
        this.charId = c.getId();
        this.category = c.getCategory();
        this.categoryUri = c.getCategoryUri();
        this.value = c.getValue(); // clobbers if we set it already
        this.valueUri = c.getValueUri();
    }

    /**
     * Create a FactorValue VO focusing on a specific statement.
     */
    public FactorValueValueObject( FactorValue fv, Statement c ) {
        this( fv, ( Characteristic ) c );
        this.predicate = c.getPredicate();
        this.predicateUri = c.getPredicateUri();
        this.object = c.getObject();
        this.objectUri = c.getObjectUri();
        this.secondPredicate = c.getSecondPredicate();
        this.secondPredicateUri = c.getSecondPredicateUri();
        this.secondObject = c.getSecondObject();
        this.secondObjectUri = c.getSecondObjectUri();
    }

    /**
     * Indicate if this FactorValue is a measurement.
     */
    @Schema(description = "Check if a `measurement` key exists instead.", deprecated = true)
    @JsonProperty("isMeasurement")
    public boolean isMeasurement() {
        return this.measurementObject != null;
    }

    @Override
    public String toString() {
        return "FactorValueValueObject [factor=" + factorValue + ", value=" + value + "]";
    }
}