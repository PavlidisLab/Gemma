package ubic.gemma.rest.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import ubic.gemma.core.ontology.FactorValueOntologyServiceImpl;
import ubic.gemma.core.ontology.FactorValueOntologyUtils;
import ubic.gemma.model.expression.experiment.AbstractFactorValueValueObject;
import org.springframework.lang.Nullable;
import ubic.gemma.model.expression.experiment.StatementValueObject;

import java.io.IOException;
import java.util.Collection;

import static ubic.gemma.core.ontology.FactorValueOntologyUtils.visitCharacteristics;
import static ubic.gemma.core.ontology.FactorValueOntologyUtils.visitAllStatements;

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

    /**
     * Every statement, including the ones with nothing said about them.
     * <p>
     * 🛑 This array used to hold only the statements carrying an OBJECT, which made a plain
     * {@code organism part: chorionic villus} — the commonest annotation there is — arrive as
     * {@code statements: []} with the row visible only under {@code characteristics}. Three teams
     * read that as "this factor value has no statement" in a single day (2026-08-28): a write-back
     * was diagnosed against the wrong cause, and a grounded UBERON term rendered as free text on
     * every organism-part value of one dataset.
     * <p>
     * A grounded value with no predicate is not a different kind of annotation from one with a
     * predicate; it is the same annotation with less said about it, so it belongs in the same list
     * with {@code predicate} and {@code object} simply absent. {@code characteristics} still carries
     * the same rows for clients that read it.
     */
    private void writeStatements( Long factorValueId, Collection<StatementValueObject> svos, JsonGenerator jsonGenerator ) throws IOException {
        jsonGenerator.writeArrayFieldStart( "statements" );
        visitAllStatements( factorValueId, svos, ( svo, assignedIds ) -> {
            if ( assignedIds.getObjectId() != null ) {
                writeStatement( svo.getId(), svo.getCategory(), svo.getCategoryUri(), assignedIds.getSubjectId(), svo.getSubject(), svo.getSubjectUri(), svo.getPredicate(), svo.getPredicateUri(), assignedIds.getObjectId(), svo.getObject(), svo.getObjectUri(), jsonGenerator );
            }
            if ( assignedIds.getSecondObjectId() != null ) {
                writeStatement( svo.getId(), svo.getCategory(), svo.getCategoryUri(), assignedIds.getSubjectId(), svo.getSubject(), svo.getSubjectUri(), svo.getSecondPredicate(), svo.getSecondPredicateUri(), assignedIds.getSecondObjectId(), svo.getSecondObject(), svo.getSecondObjectUri(), jsonGenerator );
            }
            if ( assignedIds.getObjectId() == null && assignedIds.getSecondObjectId() == null ) {
                writeStatement( svo.getId(), svo.getCategory(), svo.getCategoryUri(), assignedIds.getSubjectId(), svo.getSubject(), svo.getSubjectUri(), null, null, null, null, null, jsonGenerator );
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

    private void writeStatement( Long id, String category, String categoryUri, String subjectId, String subject, String subjectUri, @Nullable String predicate, @Nullable String predicateUri, @Nullable String objectId, @Nullable String object, @Nullable String objectUri, JsonGenerator jsonGenerator ) throws IOException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeObjectField( "id", id );
        jsonGenerator.writeStringField( "category", category );
        jsonGenerator.writeStringField( "categoryUri", categoryUri );
        jsonGenerator.writeStringField( "subjectId", subjectId );
        jsonGenerator.writeStringField( "subject", subject );
        jsonGenerator.writeStringField( "subjectUri", subjectUri );
        // Absent rather than null when there is no predicate: null reads as "this was cleared", and a
        // subject-only statement has nothing to clear. The subject half is always written, including
        // its nulls, because those describe a term that IS there.
        if ( predicate != null ) {
            jsonGenerator.writeStringField( "predicate", predicate );
            jsonGenerator.writeStringField( "predicateUri", predicateUri );
        }
        if ( objectId != null ) {
            jsonGenerator.writeStringField( "objectId", objectId );
            jsonGenerator.writeStringField( "object", object );
            jsonGenerator.writeStringField( "objectUri", objectUri );
        }
        jsonGenerator.writeEndObject();
    }
}
