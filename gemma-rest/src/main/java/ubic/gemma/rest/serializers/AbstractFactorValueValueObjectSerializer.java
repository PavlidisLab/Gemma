package ubic.gemma.rest.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import ubic.gemma.core.ontology.FactorValueOntologyServiceImpl;
import ubic.gemma.core.ontology.FactorValueOntologyUtils;
import ubic.gemma.model.expression.experiment.AbstractFactorValueValueObject;
import ubic.gemma.model.expression.experiment.StatementValueObject;

import java.io.IOException;
import java.util.Collection;

import static ubic.gemma.core.ontology.FactorValueOntologyUtils.visitCharacteristics;
import static ubic.gemma.core.ontology.FactorValueOntologyUtils.visitStatements;

/**
 * Base serializer for {@link ubic.gemma.model.expression.experiment.FactorValue} VOs.
 * <p>
 * See {@link FactorValueOntologyServiceImpl} for the logic related to how URIs are generated.
 */
public abstract class AbstractFactorValueValueObjectSerializer<T extends AbstractFactorValueValueObject> extends StdSerializer<T> {
    protected AbstractFactorValueValueObjectSerializer( Class<T> t ) {
        super( t );
    }

    @Override
    public void serialize( T factorValueValueObject, JsonGenerator jsonGenerator, SerializerProvider serializerProvider ) throws IOException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeObjectField( "id", factorValueValueObject.getId() );
        jsonGenerator.writeStringField( "ontologyId", FactorValueOntologyUtils.getUri( factorValueValueObject.getId() ) );
        if ( factorValueValueObject.getExperimentalFactorId() != null ) {
            jsonGenerator.writeObjectField( "experimentalFactorId", factorValueValueObject.getExperimentalFactorId() );
        }
        if ( factorValueValueObject.getExperimentalFactorType() != null ) {
            jsonGenerator.writeStringField( "experimentalFactorType", factorValueValueObject.getExperimentalFactorType() );
        }
        if ( factorValueValueObject.getExperimentalFactorCategory() != null ) {
            jsonGenerator.writeObjectField( "experimentalFactorCategory", factorValueValueObject.getExperimentalFactorCategory() );
        }
        serializeInternal( factorValueValueObject, jsonGenerator, serializerProvider );
        jsonGenerator.writeBooleanField( "isMeasurement", factorValueValueObject.isMeasurement() );
        if ( factorValueValueObject.getMeasurementObject() != null ) {
            jsonGenerator.writeObjectField( "measurement", factorValueValueObject.getMeasurementObject() );
        }
        writeCharacteristics( factorValueValueObject.getId(), factorValueValueObject.getStatements(), jsonGenerator );
        writeStatements( factorValueValueObject.getId(), factorValueValueObject.getStatements(), jsonGenerator );
        jsonGenerator.writeStringField( "summary", factorValueValueObject.getSummary() );
        jsonGenerator.writeEndObject();
    }

    protected abstract void serializeInternal( T t, JsonGenerator jsonGenerator, SerializerProvider serializerProvider ) throws IOException;

    private void writeCharacteristics( Long factorValueId, Collection<StatementValueObject> cvos, JsonGenerator jsonGenerator ) throws IOException {
        jsonGenerator.writeArrayFieldStart( "characteristics" );
        visitCharacteristics( factorValueId, cvos, ( cvo, valueId ) -> {
            writeCharacteristic( cvo.getId(), cvo.getCategory(), cvo.getCategoryUri(), valueId, cvo.getSubject(), cvo.getSubjectUri(), jsonGenerator );
        } );
        jsonGenerator.writeEndArray();
    }

    private void writeStatements( Long factorValueId, Collection<StatementValueObject> svos, JsonGenerator jsonGenerator ) throws IOException {
        jsonGenerator.writeArrayFieldStart( "statements" );
        visitStatements( factorValueId, svos, ( svo, assignedIds ) -> {
            if ( assignedIds.getObjectId() != null ) {
                writeStatement( svo.getCategory(), svo.getCategoryUri(), assignedIds.getSubjectId(), svo.getSubject(), svo.getSubjectUri(), svo.getPredicate(), svo.getPredicateUri(), assignedIds.getObjectId(), svo.getObject(), svo.getObjectUri(), jsonGenerator );
            }
            if ( assignedIds.getSecondObjectId() != null ) {
                writeStatement( svo.getCategory(), svo.getCategoryUri(), assignedIds.getSubjectId(), svo.getSubject(), svo.getSubjectUri(), svo.getSecondPredicate(), svo.getSecondPredicateUri(), assignedIds.getSecondObjectId(), svo.getSecondObject(), svo.getSecondObjectUri(), jsonGenerator );
            }
        } );
        jsonGenerator.writeEndArray();
    }

    private void writeCharacteristic( Long id, String category, String categoryUri, String valueId, String value, String valueUri, JsonGenerator jsonGenerator ) throws IOException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeObjectField( "id", id );
        jsonGenerator.writeStringField( "category", category );
        jsonGenerator.writeStringField( "categoryUri", categoryUri );
        jsonGenerator.writeStringField( "valueId", valueId );
        jsonGenerator.writeStringField( "value", value );
        jsonGenerator.writeStringField( "valueUri", valueUri );
        jsonGenerator.writeEndObject();
    }

    private void writeStatement( String category, String categoryUri, String subjectId, String subject, String subjectUri, String predicate, String predicateUri, String objectId, String object, String objectUri, JsonGenerator jsonGenerator ) throws IOException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeStringField( "category", category );
        jsonGenerator.writeStringField( "categoryUri", categoryUri );
        jsonGenerator.writeStringField( "subjectId", subjectId );
        jsonGenerator.writeStringField( "subject", subject );
        jsonGenerator.writeStringField( "subjectUri", subjectUri );
        jsonGenerator.writeStringField( "predicate", predicate );
        jsonGenerator.writeStringField( "predicateUri", predicateUri );
        jsonGenerator.writeStringField( "objectId", objectId );
        jsonGenerator.writeStringField( "object", object );
        jsonGenerator.writeStringField( "objectUri", objectUri );
        jsonGenerator.writeEndObject();
    }
}
