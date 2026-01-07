package ubic.gemma.model.util;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class ModelUtilsTest {

    @Test
    public void testEquals() {
        assertEquals( ModelUtils.EqualityOutcome.UNDETERMINED, ModelUtils.equals( new UninitializedList<>(), new UninitializedList<>() ) );
        List<?> a = new UninitializedList<>();
        assertEquals( ModelUtils.EqualityOutcome.EQUAL, ModelUtils.equals( a, a ) );
        assertEquals( ModelUtils.EqualityOutcome.EQUAL, ModelUtils.equals( new UninitializedList<>( 0 ), new UninitializedList<>( 0 ) ) );
        assertEquals( ModelUtils.EqualityOutcome.NOT_EQUAL, ModelUtils.equals( new UninitializedList<>( 0 ), new UninitializedList<>( 1 ) ) );

        assertEquals( ModelUtils.EqualityOutcome.UNDETERMINED, ModelUtils.equals( new UninitializedSet<>(), new UninitializedSet<>() ) );
        assertEquals( ModelUtils.EqualityOutcome.NOT_EQUAL, ModelUtils.equals( new UninitializedSet<>( 0 ), new UninitializedSet<>( 1 ) ) );
        assertEquals( ModelUtils.EqualityOutcome.EQUAL, ModelUtils.equals( new UninitializedSet<>( 0 ), new UninitializedSet<>( 0 ) ) );

        assertEquals( ModelUtils.EqualityOutcome.NOT_EQUAL, ModelUtils.equals( new UninitializedList<>( 0 ), new UninitializedSet<>( 0 ) ) );
    }
}