package ubic.gemma.rest.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import ubic.gemma.model.expression.experiment.StatementValueObject;

import java.io.IOException;
import java.util.Collection;
import java.util.Objects;

public abstract class AbstractFactorValueValueObjectSerializer<T> extends StdSerializer<T> {
    protected AbstractFactorValueValueObjectSerializer( Class<T> t ) {
        super( t );
    }

    protected void writeCharacteristics( Collection<StatementValueObject> cvos, JsonGenerator jsonGenerator ) throws IOException {
        jsonGenerator.writeArrayFieldStart( "characteristics" );
        for ( StatementValueObject cvo : cvos ) {
            // statements are written in a separate collection
            if ( cvo.getObject() != null || cvo.getSecondObject() != null ) {
                continue;
            }
            jsonGenerator.writeStartObject();
            jsonGenerator.writeObjectField( "id", cvo.getId() );
            jsonGenerator.writeObjectField( "category", cvo.getCategory() );
            jsonGenerator.writeObjectField( "categoryUri", cvo.getCategoryUri() );
            jsonGenerator.writeObjectField( "value", cvo.getValue() );
            jsonGenerator.writeObjectField( "valueUri", cvo.getValueUri() );
            jsonGenerator.writeEndObject();
        }
        jsonGenerator.writeEndArray();
    }

    protected void writeStatements( Collection<StatementValueObject> svos, JsonGenerator jsonGenerator ) throws IOException {
        jsonGenerator.writeArrayFieldStart( "statements" );
        long maxId = svos.stream()
                .map( StatementValueObject::getId )
                .filter( Objects::nonNull )
                .max( Long::compareTo )
                .orElse( 0L );
        for ( StatementValueObject svo : svos ) {
            if ( svo.getObject() != null ) {
                writeStatement( svo.getCategory(), svo.getCategoryUri(), svo.getId(), svo.getSubject(), svo.getSubjectUri(), svo.getPredicate(), svo.getPredicateUri(), ++maxId, svo.getObject(), svo.getObjectUri(), jsonGenerator );
            }
            if ( svo.getSecondObject() != null ) {
                writeStatement( svo.getCategory(), svo.getCategoryUri(), svo.getId(), svo.getSubject(), svo.getSubjectUri(), svo.getSecondPredicate(), svo.getSecondPredicateUri(), ++maxId, svo.getSecondObject(), svo.getSecondObjectUri(), jsonGenerator );
            }
        }
        jsonGenerator.writeEndArray();
    }

    protected void writeStatement( String category, String categoryUri, Long subjectId, String subject, String subjectUri, String predicate, String predicateUri, Long objectId, String object, String objectUri, JsonGenerator jsonGenerator ) throws IOException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeObjectField( "category", category );
        jsonGenerator.writeObjectField( "categoryUri", categoryUri );
        jsonGenerator.writeObjectField( "subjectId", subjectId );
        jsonGenerator.writeObjectField( "subject", subject );
        jsonGenerator.writeObjectField( "subjectUri", subjectUri );
        jsonGenerator.writeObjectField( "predicate", predicate );
        jsonGenerator.writeObjectField( "predicateUri", predicateUri );
        jsonGenerator.writeObjectField( "objectId", objectId );
        jsonGenerator.writeObjectField( "object", object );
        jsonGenerator.writeObjectField( "objectUri", objectUri );
        jsonGenerator.writeEndObject();
    }
}
