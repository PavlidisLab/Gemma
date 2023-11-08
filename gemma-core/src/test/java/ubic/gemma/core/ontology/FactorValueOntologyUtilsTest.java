package ubic.gemma.core.ontology;

import org.junit.Test;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.expression.experiment.Statement;

import static org.assertj.core.api.Assertions.assertThat;
import static ubic.gemma.core.ontology.FactorValueOntologyUtils.*;

public class FactorValueOntologyUtilsTest {

    @Test
    public void testGetUri() {
        assertThat( getUri( 1L ) )
                .isEqualTo( "http://gemma.msl.ubc.ca/ont/TGFVO/1" );
        assertThat( isAnnotationUri( "http://gemma.msl.ubc.ca/ont/TGFVO/1" ) ).isFalse();
        assertThat( isAnnotationUri( "http://gemma.msl.ubc.ca/ont/TGFVO/1/3" ) ).isTrue();
        assertThat( isAnnotationUri( "http://gemma.msl.ubc.ca/ont/TGFVO/1/foo" ) ).isFalse();
        assertThat( isAnnotationUri( "http://gemma.msl.ubc.ca/ont/TGFVO/1/3/4" ) ).isFalse();
    }

    @Test
    public void testParseUri() {
        parseUri( "http://gemma.msl.ubc.ca/ont/TGFVO/1" );
        parseUri( "http://gemma.msl.ubc.ca/ont/TGFVO/1/3" );
        assertThat( parseUri( "http://gemma.msl.ubc.ca/ont/TGEMO_000001" ) )
                .isNull();
    }

    @Test
    public void testGetAnnotationsById() {
        FactorValue fv = new FactorValue();
        fv.setId( 1L );
        Statement s = new Statement();
        s.setSubject( "foo" );
        s.setSubjectUri( "foo" );
        s.setObject( "bar" );
        s.setObjectUri( "bar" );
        fv.getCharacteristics().add( s );
        assertThat( getAnnotationsById( fv ) )
                .hasSize( 2 )
                .hasEntrySatisfying( "http://gemma.msl.ubc.ca/ont/TGFVO/1/1", v -> {
                    assertThat( v.getUri() ).isEqualTo( "foo" );
                    assertThat( v.getLabel() ).isEqualTo( "foo" );
                } )
                .hasEntrySatisfying( "http://gemma.msl.ubc.ca/ont/TGFVO/1/2", v -> {
                    assertThat( v.getUri() ).isEqualTo( "bar" );
                    assertThat( v.getLabel() ).isEqualTo( "bar" );
                } );
    }
}