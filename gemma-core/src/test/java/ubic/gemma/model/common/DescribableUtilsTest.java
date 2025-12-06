package ubic.gemma.model.common;

import org.junit.Test;
import ubic.gemma.model.common.quantitationtype.QuantitationType;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

public class DescribableUtilsTest {

    @Test
    public void testNextAvailableName() {
        assertEquals( "Test", DescribableUtils.getNextAvailableName( Arrays.asList(), "Test" ) );
        assertEquals( "Test (2)", DescribableUtils.getNextAvailableName( Arrays.asList( createDescribableWithName( "Test" ) ), "Test" ) );
        assertEquals( "Foo", DescribableUtils.getNextAvailableName( Arrays.asList( createDescribableWithName( "Test" ) ), "Foo" ) );
    }

    private Describable createDescribableWithName( String name ) {
        QuantitationType qt = QuantitationType.Factory.newInstance();
        qt.setName( name );
        return qt;
    }
}