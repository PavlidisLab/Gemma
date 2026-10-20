package ubic.gemma.model.common.measurement;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import ubic.gemma.model.common.IdentifiableValueObject;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;

@SuppressWarnings("unused") // Used in frontend through FVBasicVO
@Getter
@Setter
public class MeasurementValueObject extends IdentifiableValueObject<Measurement> {

    private String value;
    private String unit;
    @JsonIgnore
    private Long unitId;
    @Schema(implementation = MeasurementType.class)
    private String type;
    @Schema(implementation = PrimitiveType.class)
    private String representation;

    public MeasurementValueObject() {
        super();
    }

    public MeasurementValueObject( Measurement measurement ) {
        super( measurement );
        this.value = measurement.getValue();
        if ( measurement.getUnit() != null ) {
            this.unit = measurement.getUnit().getUnitNameCV();
            this.unitId = measurement.getUnit().getId();
        }
        this.type = measurement.getType() == null ? null : measurement.getType().name();
        this.representation =
                measurement.getRepresentation() == null ? null : measurement.getRepresentation().name();
    }
}
