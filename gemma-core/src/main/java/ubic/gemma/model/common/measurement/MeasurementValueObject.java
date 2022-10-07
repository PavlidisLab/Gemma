package ubic.gemma.model.common.measurement;

import com.fasterxml.jackson.annotation.JsonIgnore;
import ubic.gemma.model.IdentifiableValueObject;
import ubic.gemma.model.annotations.GemmaWebOnly;

@SuppressWarnings("unused") // Used in frontend through FVBasicVO
public class MeasurementValueObject extends IdentifiableValueObject {

    private String value;
    private String unit;
    @JsonIgnore
    private Long unitId;
    private String type;
    private String representation;

    public MeasurementValueObject() {
    }

    public MeasurementValueObject( Measurement measurement ) {
        super( measurement.getId() );
        this.value = measurement.getValue();
        if ( measurement.getUnit() != null ) {
            this.unit = measurement.getUnit().getUnitNameCV();
            this.unitId = measurement.getUnit().getId();
        }
        this.type = measurement.getType() == null ? null : measurement.getType().getValue();
        this.representation =
                measurement.getRepresentation() == null ? null : measurement.getRepresentation().getValue();
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

    public String getType() {
        return type;
    }

    public String getRepresentation() {
        return representation;
    }
}
