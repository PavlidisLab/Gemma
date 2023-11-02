package ubic.gemma.rest.serializers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonassert.JsonAssert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import org.springframework.test.util.JsonPathExpectationsHelper;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.expression.experiment.FactorValueValueObject;
import ubic.gemma.model.expression.experiment.Statement;
import ubic.gemma.persistence.util.TestComponent;
import ubic.gemma.rest.util.JacksonConfig;

import java.text.ParseException;

@ContextConfiguration
public class FactorValueValueObjectSerializerTest extends AbstractJUnit4SpringContextTests {

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
        fv.setExperimentalFactor( new ExperimentalFactor() );
        fv.getCharacteristics().add( createCharacteristic( 1L, "foo", null, "bar", null ) );
        fv.getCharacteristics().add( createStatement( 2L, "foo", null, "bar", null, "has role", null, "control", null ) );
        FactorValueValueObject fvvo = new FactorValueValueObject( fv );
        JsonAssert.with( objectMapper.writeValueAsString( fvvo ) )
                .assertEquals( "$.characteristics[0].id", 1 )
                .assertEquals( "$.characteristics[0].category", "foo" )
                .assertEquals( "$.characteristics[0].value", "bar" )
                .assertEquals( "$.statements[0].category", "foo" )
                .assertEquals( "$.statements[0].subjectId", 2 )
                .assertEquals( "$.statements[0].subject", "bar" )
                .assertEquals( "$.statements[0].predicate", "has role" )
                .assertEquals( "$.statements[0].objectId", 3 )
                .assertEquals( "$.statements[0].object", "control" );
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