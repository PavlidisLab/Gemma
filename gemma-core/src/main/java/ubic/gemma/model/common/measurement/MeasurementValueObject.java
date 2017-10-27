package ubic.gemma.model.common.measurement;

import ubic.gemma.model.IdentifiableValueObject;

@SuppressWarnings("unused") // Used in frontend through FVBasicVO
public class MeasurementValueObject extends IdentifiableValueObject {

    private String value;

    public MeasurementValueObject() {
    }

    public MeasurementValueObject( Long id, Measurement measurement ) {
        super( id );
        this.value = measurement.getValue();
    }

    public String getValue() {
        return value;
    }
}
