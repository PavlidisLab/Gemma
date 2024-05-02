package ubic.gemma.model.common.measurement;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import ubic.gemma.model.IdentifiableValueObject;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;

@SuppressWarnings("unused") // Used in frontend through FVBasicVO
public class MeasurementValueObject extends IdentifiableValueObject<Measurement> {

    private String value;
    private String unit;
    @JsonIgnore
    private Long unitId;
    private String type;
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

    public String getValue() {
        return value;
    }

    public String getUnit() {
        return unit;
    }

    public Long getUnitId() {
        return unitId;
    }

    @Schema(implementation = MeasurementType.class)
    public String getType() {
        return type;
    }

    @Schema(implementation = PrimitiveType.class)
    public String getRepresentation() {
        return representation;
    }

    public void setValue( String value ) {
        this.value = value;
    }

    public void setUnit( String unit ) {
        this.unit = unit;
    }

    public void setUnitId( Long unitId ) {
        this.unitId = unitId;
    }

    public void setType( String type ) {
        this.type = type;
    }

    public void setRepresentation( String representation ) {
        this.representation = representation;
    }
}
