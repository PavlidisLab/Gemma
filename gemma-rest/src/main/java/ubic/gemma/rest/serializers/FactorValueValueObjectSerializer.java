package ubic.gemma.rest.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import ubic.gemma.core.ontology.FactorValueOntologyUtils;
import ubic.gemma.model.expression.experiment.FactorValueValueObject;

import java.io.IOException;

import static ubic.gemma.core.ontology.FactorValueOntologyUtils.getUri;

public class FactorValueValueObjectSerializer extends AbstractFactorValueValueObjectSerializer<FactorValueValueObject> {

    public FactorValueValueObjectSerializer() {
        super( FactorValueValueObject.class );
    }

    @Override
    public void serialize( FactorValueValueObject factorValueValueObject, JsonGenerator jsonGenerator, SerializerProvider serializerProvider ) throws IOException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeObjectField( "id", factorValueValueObject.getId() );
        jsonGenerator.writeObjectField( "ontologyId", FactorValueOntologyUtils.getUri( factorValueValueObject.getId() ) );
        jsonGenerator.writeObjectField( "factorId", factorValueValueObject.getFactorId() );
        jsonGenerator.writeBooleanField( "isMeasurement", factorValueValueObject.getMeasurementObject() != null );
        if ( factorValueValueObject.getMeasurementObject() != null ) {
            jsonGenerator.writeObjectField( "measurement", factorValueValueObject.getMeasurementObject() );
        }
        writeCharacteristics( factorValueValueObject.getId(), factorValueValueObject.getStatements(), jsonGenerator );
        writeStatements( factorValueValueObject.getId(), factorValueValueObject.getStatements(), jsonGenerator );
        jsonGenerator.writeEndObject();
    }
}
