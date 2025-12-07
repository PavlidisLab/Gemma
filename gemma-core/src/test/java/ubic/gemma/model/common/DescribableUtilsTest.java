package ubic.gemma.model.common;

import org.junit.After;
import org.junit.Test;
import ubic.gemma.model.common.quantitationtype.QuantitationType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static ubic.gemma.model.common.DescribableUtils.*;

public class DescribableUtilsTest {

    private final ThreadLocal<AtomicLong> idGenerator = ThreadLocal.withInitial( AtomicLong::new );

    @After
    public void tearDown() {
        idGenerator.remove();
    }

    @Test
    public void testAllAddByName() {
        List<Describable> col = createDescribablesWithNames( "a", "b" );
        assertThat( addAllByName( col, createDescribablesWithNames( "c" ), false, false ) )
                .extracting( Describable::getName )
                .containsExactly( "c" );
        assertThat( col )
                .extracting( Describable::getName )
                .containsExactly( "a", "b", "c" );
        assertThatThrownBy( () -> addAllByName(
                createDescribablesWithNames( "a", "b" ),
                createDescribablesWithNames( "b" ), false, false ) )
                .isInstanceOf( IllegalArgumentException.class )
                .hasMessage( "Collection already contains a QuantitationType with name b. Specify ignoreExisting to ignore it." );
    }

    @Test
    public void testAddAllByNameReplaceExisting() {
        List<Describable> col = createDescribablesWithNames( "a", "b" );
        assertThat( addAllByName( col, createDescribablesWithNames( "b" ), true, false ) )
                .extracting( Identifiable::getId )
                .containsExactly( 3L );
        assertThat( col )
                .extracting( Describable::getId )
                .containsExactly( 1L, 3L );
    }

    @Test
    public void testAddAllByNameIgnoreExisting() {
        List<Describable> col = createDescribablesWithNames( "a", "b" );
        assertThat( addAllByName( col, createDescribablesWithNames( "b" ), false, true ) )
                .isEmpty();
        assertThat( col )
                .extracting( Describable::getId )
                .containsExactly( 1L, 2L );
    }

    @Test
    public void testAddAllByNameIgnoreExisting2() {
        List<Describable> col = createDescribablesWithNames( "a", "b" );
        assertThat( addAllByName( col, createDescribablesWithNames( "b" ) ) )
                .isEmpty();
        assertThat( col )
                .extracting( Describable::getId )
                .containsExactly( 1L, 2L );
    }

    @Test
    public void testAddByName() {
        List<Describable> col = createDescribablesWithNames( "a", "b" );
        assertThat( addByName( col, createDescribablesWithNames( "c" ).get( 0 ) ) )
                .isNotNull();
        assertThat( col )
                .extracting( Describable::getId )
                .containsExactly( 1L, 2L, 3L );
    }

    @Test
    public void testAddByNameWithDuplicate() {
        List<Describable> col = createDescribablesWithNames( "a", "b" );
        assertThat( addByName( col, createDescribablesWithNames( "b" ).get( 0 ) ) )
                .isNull();
        assertThat( col )
                .extracting( Describable::getId )
                .containsExactly( 1L, 2L );
    }

    @Test
    public void testNextAvailableName() {
        assertEquals( "Test", getNextAvailableName( Collections.emptyList(), "Test" ) );
        assertEquals( "Test (2)", getNextAvailableName( createDescribablesWithNames( "Test" ), "Test" ) );
        assertEquals( "Foo", getNextAvailableName( createDescribablesWithNames( "Test" ), "Foo" ) );
    }

    private List<Describable> createDescribablesWithNames( String... names ) {
        List<Describable> d = new ArrayList<>();
        for ( String name : names ) {
            QuantitationType qt = QuantitationType.Factory.newInstance();
            qt.setId( idGenerator.get().incrementAndGet() );
            qt.setName( name );
            d.add( qt );
        }
        return d;
    }
}