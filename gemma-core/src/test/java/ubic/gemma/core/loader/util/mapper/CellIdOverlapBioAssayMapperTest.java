package ubic.gemma.core.loader.util.mapper;

import org.junit.Test;
import ubic.gemma.model.expression.bioAssay.BioAssay;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

public class CellIdOverlapBioAssayMapperTest {

    @Test
    public void test() {
        Map<String, Set<String>> s2c = new HashMap<>();
        s2c.computeIfAbsent( "a1", k -> new HashSet<>() )
                .add( "ACGTGAC" );
        s2c.computeIfAbsent( "a2", k -> new HashSet<>() )
                .add( "ACGTGAT" );
        s2c.computeIfAbsent( "b1", k -> new HashSet<>() )
                .addAll( Arrays.asList( "ACGTGAC", "ACGTGGG" ) ); // a collision with a1
        s2c.computeIfAbsent( "b2", k -> new HashSet<>() )
                .add( "ACGTGAG" );
        Map<BioAssay, Set<String>> b2c = new HashMap<>();
        BioAssay a = BioAssay.Factory.newInstance( "a" );
        b2c.computeIfAbsent( a, k -> new HashSet<>() )
                .addAll( Arrays.asList( "ACGTGAC", "ACGTGAT" ) );
        BioAssay b = BioAssay.Factory.newInstance( "b" );
        b2c.computeIfAbsent( b, k -> new HashSet<>() )
                .addAll( Arrays.asList( "ACGTGAC", "ACGTGGG", "ACGTGAG" ) );
        CellIdOverlapBioAssayMapper mapper = new CellIdOverlapBioAssayMapper( b2c, s2c );
        // it's possible to distinguish a1 from b due to the collision
        assertThat( mapper.matchOne( b2c.keySet(), "a1" ) )
                .hasValue( a );
        assertThat( mapper.matchOne( b2c.keySet(), "a2" ) )
                .hasValue( a );
        assertThat( mapper.matchOne( b2c.keySet(), "b1" ) )
                .hasValue( b );
        assertThat( mapper.matchOne( b2c.keySet(), "b2" ) )
                .hasValue( b );
    }
}