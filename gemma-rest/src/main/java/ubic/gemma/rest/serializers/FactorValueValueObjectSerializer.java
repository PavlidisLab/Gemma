package ubic.gemma.rest.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import ubic.gemma.model.expression.experiment.FactorValueValueObject;

import java.io.IOException;

@Deprecated
public class FactorValueValueObjectSerializer extends AbstractFactorValueValueObjectSerializer<FactorValueValueObject> {

    public FactorValueValueObjectSerializer() {
        super( FactorValueValueObject.class );
    }

    @Override
    protected void serializeInternal( FactorValueValueObject factorValueValueObject, JsonGenerator jsonGenerator, SerializerProvider serializerProvider ) throws IOException {
        if ( factorValueValueObject.getFactorId() != null ) {
            jsonGenerator.writeObjectField( "factorId", factorValueValueObject.getFactorId() );
            jsonGenerator.writeStringField( "category", factorValueValueObject.getCategory() );
            jsonGenerator.writeStringField( "categoryUri", factorValueValueObject.getCategoryUri() );
        }
        jsonGenerator.writeObjectField( "charId", factorValueValueObject.getCharId() );
        jsonGenerator.writeStringField( "description", factorValueValueObject.getDescription() );
        jsonGenerator.writeObjectField( "factorValue", factorValueValueObject.getFactorValue() );
        jsonGenerator.writeBooleanField( "isMeasurement", factorValueValueObject.getMeasurementObject() != null );
    }
}
