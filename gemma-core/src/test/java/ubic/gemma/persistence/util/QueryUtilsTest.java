package ubic.gemma.persistence.util;

import org.junit.Test;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static ubic.gemma.persistence.util.QueryUtils.*;

public class QueryUtilsTest {

    @Test
    public void test() {
        assertThat( optimizeParameterList( Arrays.asList( 1L, 2L, null, 0L ) ) )
                .containsExactly( 0L, 1L, 2L, null );
    }

    @Test
    public void testIdentifiable() {
        assertThat( optimizeIdentifiableParameterList( Arrays.asList( createArrayDesign( 2L ),
                createArrayDesign( 1L ), createArrayDesign( 1L ), createArrayDesign( null ) ) ) )
                .extracting( ArrayDesign::getId )
                .containsExactly( 1L, 2L, null, null );
    }

    @Test
    public void testBatchParameterList() {
        assertThat( batchParameterList( new ArrayList<Integer>(), 4 ) )
                .isEmpty();
        assertThat( batchParameterList( Arrays.asList( 1, 2, 3 ), 4 ) )
                .containsExactly( Arrays.asList( 1, 2, 3, 3 ) );
        assertThat( batchParameterList( Arrays.asList( 1, 2, 3, 4 ), 4 ) )
                .containsExactly( Arrays.asList( 1, 2, 3, 4 ) );
        assertThat( batchParameterList( Arrays.asList( 1, 2, 3, null, 4, 14, 23, 1 ), 4 ) )
                .containsExactly( Arrays.asList( 1, 2, 3, 4 ), Arrays.asList( 14, 23, null, null ) );
    }

    private ArrayDesign createArrayDesign( @Nullable Long id ) {
        ArrayDesign ad = new ArrayDesign();
        ad.setId( id );
        return ad;
    }
}