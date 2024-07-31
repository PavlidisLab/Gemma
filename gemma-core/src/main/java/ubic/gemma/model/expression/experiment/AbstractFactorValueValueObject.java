package ubic.gemma.model.expression.experiment;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.Hibernate;
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
    private Long experimentalFactorId;

    /**
     * The experiment factor category.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private CharacteristicValueObject experimentalFactorCategory;

    /**
     * The measurement associated with this factor value.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private MeasurementValueObject measurement;

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
            if ( Hibernate.isInitialized( fv.getExperimentalFactor() ) ) {
                if ( fv.getExperimentalFactor().getCategory() != null ) {
                    this.experimentalFactorCategory = new CharacteristicValueObject( fv.getExperimentalFactor().getCategory() );
                }
            }
        }

        if ( fv.getMeasurement() != null ) {
            this.measurement = new MeasurementValueObject( fv.getMeasurement() );
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
}
