package ubic.gemma.model.expression.experiment;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
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
@EqualsAndHashCode(of = { "characteristics" }, callSuper = true)
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
     * Whether this factor value is a "forced" baseline condition. Mirrors {@link FactorValue#getIsBaseline()};
     * {@code null} when unset. Ignored for continuous factors. Exposed on the wire as {@code isBaseline} so the
     * design read/write round-trip (e.g. the composite curation commit) can carry the baseline flag.
     */
    @JsonProperty("isBaseline")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Boolean baseline;

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

        this.baseline = fv.getIsBaseline();

        // Summarize the STATEMENTS this object is about to serialize, not the entity they came
        // from. The statement VOs above canonicalize their term URIs and labels; the entity does
        // not, so one factor value was serializing summary "KMH-2 cell" beside subject
        // "KM-H2 cell" -- the same object disagreeing with itself about what it says.
        //
        // With no statements and no measurement there is nothing for that to matter to, and only
        // the entity can reach the denormalized FACTOR_VALUE.VALUE column: the subclass field
        // holding it is not assigned until after this constructor returns.
        this.summary = ( this.statements.isEmpty() && this.measurementObject == null )
                ? FactorValueUtils.getSummaryString( fv )
                : FactorValueUtils.getSummaryString( this );
    }

    /**
     * Indicate if this FactorValue is a measurement.
     */
    @Schema(description = "Indicate if this factor value represents a measurement. When this is true, the `measurement` field will be populated.")
    // READ_ONLY: this is derived from measurementObject != null, so Jackson should emit it but never try
    // to set it back — otherwise clients who round-trip the JSON hit "Unrecognized field 'isMeasurement'".
    @JsonProperty(value = "isMeasurement", access = JsonProperty.Access.READ_ONLY)
    public boolean isMeasurement() {
        return measurementObject != null;
    }
}
