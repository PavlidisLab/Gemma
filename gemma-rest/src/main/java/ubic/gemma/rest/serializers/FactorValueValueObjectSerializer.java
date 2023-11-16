package ubic.gemma.rest.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import ubic.gemma.core.ontology.FactorValueOntologyUtils;
import ubic.gemma.model.expression.experiment.FactorValueValueObject;

import java.io.IOException;

public class FactorValueValueObjectSerializer extends AbstractFactorValueValueObjectSerializer<FactorValueValueObject> {

    public FactorValueValueObjectSerializer() {
        super( FactorValueValueObject.class );
    }

    @Override
    public void serialize( FactorValueValueObject factorValueValueObject, JsonGenerator jsonGenerator, SerializerProvider serializerProvider ) throws IOException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeObjectField( "id", factorValueValueObject.getId() );
        jsonGenerator.writeStringField( "ontologyId", FactorValueOntologyUtils.getUri( factorValueValueObject.getId() ) );
        jsonGenerator.writeObjectField( "experimentalFactorId", factorValueValueObject.getExperimentalFactorId() );
        jsonGenerator.writeObjectField( "experimentalFactorCategory", factorValueValueObject.getExperimentalFactorCategory() );
        jsonGenerator.writeObjectField( "factorId", factorValueValueObject.getFactorId() );
        jsonGenerator.writeObjectField( "charId", factorValueValueObject.getCharId() );
        jsonGenerator.writeStringField( "category", factorValueValueObject.getCategory() );
        jsonGenerator.writeStringField( "categoryUri", factorValueValueObject.getCategoryUri() );
        jsonGenerator.writeStringField( "description", factorValueValueObject.getDescription() );
        jsonGenerator.writeObjectField( "factorValue", factorValueValueObject.getFactorValue() );
        jsonGenerator.writeBooleanField( "isMeasurement", factorValueValueObject.getMeasurementObject() != null );
        if ( factorValueValueObject.getMeasurementObject() != null ) {
            jsonGenerator.writeObjectField( "measurement", factorValueValueObject.getMeasurementObject() );
        }
        writeCharacteristics( factorValueValueObject.getId(), factorValueValueObject.getStatements(), jsonGenerator );
        writeStatements( factorValueValueObject.getId(), factorValueValueObject.getStatements(), jsonGenerator );
        jsonGenerator.writeStringField( "summary", factorValueValueObject.getSummary() );
        jsonGenerator.writeEndObject();
    }
}
