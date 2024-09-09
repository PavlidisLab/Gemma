package ubic.gemma.core.loader.expression.geo.singleCell;

import org.junit.Test;
import ubic.gemma.model.expression.bioAssay.BioAssay;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

public class GeoBioAssayToSampleNameMatcherTest {

    @Test
    public void test() {
        GeoBioAssayToSampleNameMatcher matcher = new GeoBioAssayToSampleNameMatcher();
        BioAssay b1 = new BioAssay();
        b1.setName( "test" );
        assertThat( matcher.match( Collections.emptyList(), "foo bar" ) )
                .containsExactly( b1 );
    }
}