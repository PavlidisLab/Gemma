package ubic.gemma.rest.serializers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonassert.JsonAssert;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.util.test.BaseTest5;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.expression.experiment.FactorValueValueObject;
import ubic.gemma.model.expression.experiment.Statement;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.rest.util.JacksonConfig;

import java.text.ParseException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@ContextConfiguration
public class FactorValueValueObjectSerializerTest extends BaseTest5 {

    @Configuration
    @TestComponent
    @Import(JacksonConfig.class)
    static class FactorValueValueObjectSerializerTestContextConfiguration {
    }

    @Autowired
    private ObjectMapper objectMapper;


    @Test
    public void test() throws JsonProcessingException, ParseException {
        FactorValue fv = new FactorValue();
        fv.setId( 1L );
        fv.setExperimentalFactor( new ExperimentalFactor() );
        fv.getCharacteristics().add( createCharacteristic( 1L, "foo", null, "bar", null ) );
        fv.getCharacteristics().add( createStatement( 2L, "foo", null, "bar", null, "has role", null, "control", null ) );
        FactorValueValueObject fvvo = new FactorValueValueObject( fv );
        JsonAssert.with( objectMapper.writeValueAsString( fvvo ) )
                .assertEquals( "$.characteristics[0].id", 2 )
                .assertEquals( "$.characteristics[0].category", "foo" )
                .assertEquals( "$.characteristics[0].valueId", "http://gemma.msl.ubc.ca/ont/TGFVO/1/1" )
                .assertEquals( "$.characteristics[0].value", "bar" )
                .assertEquals( "$.characteristics[1].id", 1 )
                .assertEquals( "$.characteristics[1].category", "foo" )
                .assertEquals( "$.characteristics[1].valueId", "http://gemma.msl.ubc.ca/ont/TGFVO/1/3" )
                .assertEquals( "$.characteristics[1].value", "bar" )
                .assertEquals( "$.statements[0].category", "foo" )
                .assertEquals( "$.statements[0].subjectId", "http://gemma.msl.ubc.ca/ont/TGFVO/1/1" )
                .assertEquals( "$.statements[0].subject", "bar" )
                .assertEquals( "$.statements[0].predicate", "has role" )
                .assertEquals( "$.statements[0].objectId", "http://gemma.msl.ubc.ca/ont/TGFVO/1/2" )
                .assertEquals( "$.statements[0].object", "control" );
    }

    /**
     * A compound statement reaches the wire flattened: two entries in {@code statements} sharing one
     * subject, the second clause carrying {@code secondPredicate} / {@code secondObject} under the
     * generic {@code predicate} / {@code object} keys.
     * <p>
     * This pins the reason the four {@code second*} fields on {@link ubic.gemma.model.expression.experiment.StatementValueObject}
     * are annotated {@code @WithheldFromApi(REDUNDANT)}: the data is already published, under
     * another name, by this serializer. Un-hide them and a client reading both shapes sees the
     * second clause twice. The behaviour dates to {@code dff752727c} (fix #814), which un-hid
     * {@code predicate*} / {@code object*} and added this flattening in the same commit.
     */
    @Test
    public void aCompoundStatementIsFlattenedIntoTwoStatementsRatherThanExposingTheSecondClauseFields() throws JsonProcessingException {
        FactorValue fv = new FactorValue();
        fv.setId( 1L );
        fv.setExperimentalFactor( new ExperimentalFactor() );
        fv.getCharacteristics().add( createCompoundStatement( 10L, "treatment", null,
                "high fat diet", null,
                "has dose", null, "60% kcal fat", null,
                "for", null, "12 weeks", null ) );

        String json = objectMapper.writeValueAsString( new FactorValueValueObject( fv ) );

        // exactly two entries, not one truncated entry and not three
        assertEquals( 2, objectMapper.readTree( json ).get( "statements" ).size() );

        JsonAssert.with( json )
                // both clauses hang off the same subject, and both report the parent statement's id
                .assertEquals( "$.statements[0].id", 10 )
                .assertEquals( "$.statements[1].id", 10 )
                .assertEquals( "$.statements[0].subjectId", "http://gemma.msl.ubc.ca/ont/TGFVO/1/1" )
                .assertEquals( "$.statements[1].subjectId", "http://gemma.msl.ubc.ca/ont/TGFVO/1/1" )
                .assertEquals( "$.statements[0].subject", "high fat diet" )
                .assertEquals( "$.statements[1].subject", "high fat diet" )
                // first clause
                .assertEquals( "$.statements[0].predicate", "has dose" )
                .assertEquals( "$.statements[0].object", "60% kcal fat" )
                .assertEquals( "$.statements[0].objectId", "http://gemma.msl.ubc.ca/ont/TGFVO/1/2" )
                // second clause, under the SAME keys — this is what makes the raw fields redundant
                .assertEquals( "$.statements[1].predicate", "for" )
                .assertEquals( "$.statements[1].object", "12 weeks" )
                .assertEquals( "$.statements[1].objectId", "http://gemma.msl.ubc.ca/ont/TGFVO/1/3" );

        // and the raw field names appear nowhere in the payload, under any nesting
        assertFalse( json.contains( "secondPredicate" ), "secondPredicate must stay off the wire" );
        assertFalse( json.contains( "secondObject" ), "secondObject must stay off the wire" );
    }

    private Statement createCharacteristic( Long id, String category, String categoryUri, String value, String valueUri ) {
        Statement statement = new Statement();
        statement.setId( id );
        statement.setCategory( category );
        statement.setCategoryUri( categoryUri );
        statement.setSubject( value );
        statement.setSubjectUri( valueUri );
        return statement;
    }

    private Statement createCompoundStatement( Long id, String category, String categoryUri, String subject, String subjectUri, String predicate, String predicateUri, String object, String objectUri, String secondPredicate, String secondPredicateUri, String secondObject, String secondObjectUri ) {
        Statement statement = createStatement( id, category, categoryUri, subject, subjectUri, predicate, predicateUri, object, objectUri );
        statement.setSecondPredicate( secondPredicate );
        statement.setSecondPredicateUri( secondPredicateUri );
        statement.setSecondObject( secondObject );
        statement.setSecondObjectUri( secondObjectUri );
        return statement;
    }

    private Statement createStatement( Long id, String category, String categoryUri, String subject, String subjectUri, String predicate, String predicateUri, String object, String objectUri ) {
        Statement statement = new Statement();
        statement.setId( id );
        statement.setCategory( category );
        statement.setCategoryUri( categoryUri );
        statement.setSubject( subject );
        statement.setSubjectUri( subjectUri );
        statement.setPredicate( predicate );
        statement.setPredicateUri( predicateUri );
        statement.setObject( object );
        statement.setObjectUri( objectUri );
        return statement;
    }
}