package ubic.gemma.core.analysis.preprocess;

import org.junit.Test;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.expression.experiment.Statement;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static ubic.gemma.core.analysis.preprocess.SplitExperimentServiceImpl.generateNameForSplit;

public class SplitExperimentNameGeneratorTest {

    @Test
    public void test() {
        ExpressionExperiment ee = new ExpressionExperiment();
        ee.setName( "test" );
        Statement c = new Statement();
        c.setSubject( "bar" );
        FactorValue fv = new FactorValue();
        fv.getCharacteristics().add( c );
        ExperimentalFactor ef = new ExperimentalFactor();
        ef.setName( "foo" );
        fv.setExperimentalFactor( ef );
        assertEquals( "Split part 1 of: test [foo = bar]", generateNameForSplit( ee, 1, fv ) );
        ee.setName( String.join( "", java.util.Collections.nCopies( 255, "a" ) ) );
        String name = generateNameForSplit( ee, 1, fv );
        assertEquals( 253, name.length() );
        assertEquals( 255, name.getBytes( StandardCharsets.UTF_8 ).length );
        assertEquals( "Split part 1 of: aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa… [foo = bar]", name );
        ee.setName( "test" );
        c.setSubject( String.join( "", java.util.Collections.nCopies( 255, "a" ) ) );
        assertThatThrownBy( () -> generateNameForSplit( ee, 1, fv ) )
                .isInstanceOf( IllegalArgumentException.class );

        // make sure that whitespaces before the ellipsis are trimmed
        int lengthOfEverythingElse = "Split part 1 of: [foo = bar]".length();
        ee.setName( String.join( "", java.util.Collections.nCopies( 255 - lengthOfEverythingElse, "a" ) ) + " " + "test" );
        assertEquals( "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa test", ee.getName() );
        c.setSubject( "bar" );
        name = generateNameForSplit( ee, 1, fv );
        assertEquals( 253, name.length() );
        assertEquals( 255, name.getBytes( StandardCharsets.UTF_8 ).length );
        assertEquals( "Split part 1 of: aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa… [foo = bar]", name );
    }
}
