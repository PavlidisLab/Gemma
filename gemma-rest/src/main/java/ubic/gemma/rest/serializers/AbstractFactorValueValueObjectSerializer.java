package ubic.gemma.rest.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import ubic.gemma.core.ontology.FactorValueOntologyServiceImpl;
import ubic.gemma.model.expression.experiment.StatementValueObject;

import java.io.IOException;
import java.util.SortedSet;

import static ubic.gemma.core.ontology.FactorValueOntologyService.factorValueAnnotationUri;

/**
 * Base serializer for {@link ubic.gemma.model.expression.experiment.FactorValue} VOs.
 * <p>
 * See {@link FactorValueOntologyServiceImpl} for the logic related to how URIs are generated.
 */
public abstract class AbstractFactorValueValueObjectSerializer<T> extends StdSerializer<T> {
    protected AbstractFactorValueValueObjectSerializer( Class<T> t ) {
        super( t );
    }

    protected void writeCharacteristics( Long factorValueId, SortedSet<StatementValueObject> cvos, JsonGenerator jsonGenerator ) throws IOException {
        long nextAvailableId = 1L;
        jsonGenerator.writeArrayFieldStart( "characteristics" );
        for ( StatementValueObject cvo : cvos ) {
            long valueId = nextAvailableId++;
            // statements are written in a separate collection
            if ( cvo.getObject() == null && cvo.getSecondObject() == null ) {
                jsonGenerator.writeStartObject();
                jsonGenerator.writeObjectField( "id", cvo.getId() );
                jsonGenerator.writeObjectField( "category", cvo.getCategory() );
                jsonGenerator.writeObjectField( "categoryUri", cvo.getCategoryUri() );
                jsonGenerator.writeObjectField( "valueId", factorValueAnnotationUri( factorValueId, valueId ) );
                jsonGenerator.writeObjectField( "value", cvo.getValue() );
                jsonGenerator.writeObjectField( "valueUri", cvo.getValueUri() );
                jsonGenerator.writeEndObject();
            }
            if ( cvo.getObject() != null ) {
                nextAvailableId++;
            }
            if ( cvo.getSecondObject() != null ) {
                nextAvailableId++;
            }
        }
        jsonGenerator.writeEndArray();
    }

    protected void writeStatements( Long factorValueId, SortedSet<StatementValueObject> svos, JsonGenerator jsonGenerator ) throws IOException {
        long nextAvailableId = 1L;
        jsonGenerator.writeArrayFieldStart( "statements" );
        for ( StatementValueObject svo : svos ) {
            long subjectId = nextAvailableId++;
            if ( svo.getObject() != null ) {
                writeStatement( factorValueId, svo.getId(), svo.getCategory(), svo.getCategoryUri(), subjectId, svo.getSubject(), svo.getSubjectUri(), svo.getPredicate(), svo.getPredicateUri(), nextAvailableId++, svo.getObject(), svo.getObjectUri(), jsonGenerator );
            }
            if ( svo.getSecondObject() != null ) {
                writeStatement( factorValueId, svo.getId(), svo.getCategory(), svo.getCategoryUri(), subjectId, svo.getSubject(), svo.getSubjectUri(), svo.getSecondPredicate(), svo.getSecondPredicateUri(), nextAvailableId++, svo.getSecondObject(), svo.getSecondObjectUri(), jsonGenerator );
            }
        }
        jsonGenerator.writeEndArray();
    }

    protected void writeStatement( Long factorValueId, Long id, String category, String categoryUri, Long subjectId, String subject, String subjectUri, String predicate, String predicateUri, Long objectId, String object, String objectUri, JsonGenerator jsonGenerator ) throws IOException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeObjectField( "id", id );
        jsonGenerator.writeObjectField( "category", category );
        jsonGenerator.writeObjectField( "categoryUri", categoryUri );
        jsonGenerator.writeObjectField( "subjectId", factorValueAnnotationUri( factorValueId, subjectId ) );
        jsonGenerator.writeObjectField( "subject", subject );
        jsonGenerator.writeObjectField( "subjectUri", subjectUri );
        jsonGenerator.writeObjectField( "predicate", predicate );
        jsonGenerator.writeObjectField( "predicateUri", predicateUri );
        jsonGenerator.writeObjectField( "objectId", factorValueAnnotationUri( factorValueId, objectId ) );
        jsonGenerator.writeObjectField( "object", object );
        jsonGenerator.writeObjectField( "objectUri", objectUri );
        jsonGenerator.writeEndObject();
    }
}
