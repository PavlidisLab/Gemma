package ubic.gemma.rest.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import lombok.SneakyThrows;
import ubic.gemma.core.ontology.FactorValueOntologyServiceImpl;
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
public abstract class AbstractFactorValueValueObjectSerializer<T> extends StdSerializer<T> {
    protected AbstractFactorValueValueObjectSerializer( Class<T> t ) {
        super( t );
    }

    protected void writeCharacteristics( Long factorValueId, Collection<StatementValueObject> cvos, JsonGenerator jsonGenerator ) throws IOException {
        jsonGenerator.writeArrayFieldStart( "characteristics" );
        visitCharacteristics( factorValueId, cvos, ( cvo, valueId ) -> {
            writeCharacteristic( cvo.getId(), cvo.getCategory(), cvo.getCategoryUri(), valueId, cvo.getSubject(), cvo.getSubjectUri(), jsonGenerator );
        } );
        jsonGenerator.writeEndArray();
    }

    protected void writeStatements( Long factorValueId, Collection<StatementValueObject> svos, JsonGenerator jsonGenerator ) throws IOException {
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

    @SneakyThrows
    private void writeCharacteristic( Long id, String category, String categoryUri, String valueId, String value, String valueUri, JsonGenerator jsonGenerator ) {
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
