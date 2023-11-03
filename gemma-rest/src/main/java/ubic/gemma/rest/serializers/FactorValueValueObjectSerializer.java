package ubic.gemma.rest.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import ubic.gemma.core.ontology.FactorValueOntologyService;
import ubic.gemma.model.expression.experiment.FactorValueValueObject;
import ubic.gemma.model.expression.experiment.StatementValueObject;

import java.io.IOException;
import java.util.TreeSet;

public class FactorValueValueObjectSerializer extends AbstractFactorValueValueObjectSerializer<FactorValueValueObject> {

    public FactorValueValueObjectSerializer() {
        super( FactorValueValueObject.class );
    }

    @Override
    public void serialize( FactorValueValueObject factorValueValueObject, JsonGenerator jsonGenerator, SerializerProvider serializerProvider ) throws IOException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeObjectField( "id", factorValueValueObject.getId() );
        jsonGenerator.writeObjectField( "ontologyId", FactorValueOntologyService.factorValueUri( factorValueValueObject.getId() ) );
        jsonGenerator.writeObjectField( "factorId", factorValueValueObject.getFactorId() );
        jsonGenerator.writeBooleanField( "isMeasurement", factorValueValueObject.getMeasurementObject() != null );
        if ( factorValueValueObject.getMeasurementObject() != null ) {
            jsonGenerator.writeObjectField( "measurement", factorValueValueObject.getMeasurementObject() );
        }
        TreeSet<StatementValueObject> statements = new TreeSet<>( factorValueValueObject.getStatements() );
        writeCharacteristics( factorValueValueObject.getId(), statements, jsonGenerator );
        writeStatements( factorValueValueObject.getId(), statements, jsonGenerator );
        jsonGenerator.writeEndObject();
    }
}
