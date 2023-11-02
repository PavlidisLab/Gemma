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
    public void serialize( FactorValueBasicValueObject factorValueBasicValueObject, JsonGenerator jsonGenerator, SerializerProvider serializerProvider ) throws IOException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeObjectField( "id", factorValueBasicValueObject.getId() );
        jsonGenerator.writeObjectField( "experimentalFactorId", factorValueBasicValueObject.getExperimentalFactorId() );
        jsonGenerator.writeObjectField( "experimentalFactorCategory", factorValueBasicValueObject.getExperimentalFactorCategory() );
        if ( factorValueBasicValueObject.getMeasurement() != null ) {
            jsonGenerator.writeObjectField( "measurement", factorValueBasicValueObject.getMeasurement() );
        }
        writeCharacteristics( factorValueBasicValueObject.getStatements(), jsonGenerator );
        writeStatements( factorValueBasicValueObject.getStatements(), jsonGenerator );
        //noinspection deprecation
        jsonGenerator.writeStringField( "value", factorValueBasicValueObject.getValue() );
        //noinspection deprecation
        jsonGenerator.writeStringField( "summary", factorValueBasicValueObject.getSummary() );
        jsonGenerator.writeEndObject();
    }
}
