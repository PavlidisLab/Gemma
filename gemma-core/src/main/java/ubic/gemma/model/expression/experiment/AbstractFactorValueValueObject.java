package ubic.gemma.model.expression.experiment;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import ubic.gemma.model.util.ModelUtils;
import ubic.gemma.model.common.IdentifiableValueObject;
import ubic.gemma.model.common.description.CharacteristicValueObject;
import ubic.gemma.model.common.measurement.MeasurementValueObject;

import java.util.List;
import java.util.stream.Collectors;

/**
 * The bare minimum to represent a factor value.
 * <p>
 * This class solely exist to get consistent behavior between the deprecated {@link FactorValueValueObject} and its
 * replacement {@link FactorValueBasicValueObject}.
 * @author poirigui
 */
@Getter
@Setter
public abstract class AbstractFactorValueValueObject extends IdentifiableValueObject<FactorValue> {

    /**
     * A unique ontology identifier (i.e. IRI) for this factor value.
     */
    private String ontologyId;

    /**
     * The ID of the experimental factor this factor value belongs to.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "This property is not filled if rendered within an ExperimentalFactorValueObject.")
    private Long experimentalFactorId;

    /**
     * The experimental factor type.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(allowableValues = { "categorical", "continuous" }, description = "This property is not filled if rendered within an ExperimentalFactorValueObject.")
    private String experimentalFactorType;

    /**
     * The experiment factor category.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "This property is not filled if rendered within an ExperimentalFactorValueObject.")
    private CharacteristicValueObject experimentalFactorCategory;

    /**
     * The measurement associated with this factor value.
     * <p>
     * This is named as such to avoid conflict with {@link #isMeasurement()}.
     */
    @JsonProperty("measurement")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "This property exists only if this factor value is a measurement.")
    private MeasurementValueObject measurementObject;

    /**
     * The characteristics associated with this factor value.
     */
    private List<CharacteristicValueObject> characteristics;

    /**
     * The statements associated with this factor value.
     */
    private List<StatementValueObject> statements;

    /**
     * Human-readable summary of the factor value.
     */
    private String summary;

    protected AbstractFactorValueValueObject() {
        super();
    }

    protected AbstractFactorValueValueObject( Long id ) {
        super( id );
    }

    protected AbstractFactorValueValueObject( FactorValue fv, boolean includeExperimentalFactor ) {
        super( fv );

        if ( includeExperimentalFactor ) {
            this.experimentalFactorId = fv.getExperimentalFactor().getId();
            if ( ModelUtils.isInitialized( fv.getExperimentalFactor() ) ) {
                if ( fv.getExperimentalFactor().getCategory() != null ) {
                    this.experimentalFactorType = fv.getExperimentalFactor().getType().equals( FactorType.CATEGORICAL ) ? "categorical" : "continuous";
                    this.experimentalFactorCategory = new CharacteristicValueObject( fv.getExperimentalFactor().getCategory() );
                }
            }
        }

        if ( fv.getMeasurement() != null ) {
            this.measurementObject = new MeasurementValueObject( fv.getMeasurement() );
        }

        this.characteristics = fv.getCharacteristics().stream()
                .sorted()
                .map( CharacteristicValueObject::new )
                .collect( Collectors.toList() );

        this.statements = fv.getCharacteristics().stream()
                .sorted()
                .map( StatementValueObject::new )
                .collect( Collectors.toList() );

        this.summary = FactorValueUtils.getSummaryString( fv );
    }

    /**
     * Indicate if this FactorValue is a measurement.
     */
    @Schema(description = "Indicate if this factor value represents a measurement. When this is true, the `measurement` field will be populated.")
    @JsonProperty("isMeasurement")
    public boolean isMeasurement() {
        return measurementObject != null;
    }
}
