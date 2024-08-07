package ubic.gemma.rest.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import ubic.gemma.model.expression.experiment.FactorValueBasicValueObject;

import java.io.IOException;

public class FactorValueBasicValueObjectSerializer extends AbstractFactorValueValueObjectSerializer<FactorValueBasicValueObject> {

    public FactorValueBasicValueObjectSerializer() {
        super( FactorValueBasicValueObject.class );
    }

    @Override
    protected void serializeInternal( FactorValueBasicValueObject factorValueBasicValueObject, JsonGenerator jsonGenerator, SerializerProvider serializerProvider ) throws IOException {
        //noinspection deprecation
        jsonGenerator.writeStringField( "value", factorValueBasicValueObject.getValue() );
        jsonGenerator.writeStringField( "summary", factorValueBasicValueObject.getSummary() );
    }
}
