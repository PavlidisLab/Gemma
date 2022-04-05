package ubic.gemma.persistence.util;

import org.junit.Test;
import ubic.gemma.model.common.Identifiable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class EntityUtilsTest {

    private static class Foo implements Identifiable {

        private final Long id;

        private Foo( Long id ) {
            this.id = id;
        }

        @Override
        public Long getId() {
            return id;
        }
    }

    @Test
    public void testGetIdMap() {
        Collection<Foo> foos = new ArrayList<>();
        foos.add( new Foo( 1L ) );
        foos.add( new Foo( 1L ) );
        assertThat( EntityUtils.getIdMap( foos ) )
                .containsKey( 1L );
    }

    @Test
    public void testGetProperty() {
        Identifiable i = mock( Identifiable.class );
        EntityUtils.getProperty( i, "id" );
        verify( i ).getId();
    }

    @Test
    public void testGetPropertyMap() {
        Identifiable i = mock( Identifiable.class );
        EntityUtils.getPropertyMap( Collections.singleton( i ), "id" );
        verify( i ).getId();
    }

    public static class Bar {

        private final Foo foo;

        public Bar( Foo foo ) {
            this.foo = foo;
        }

        public Foo getFoo() {
            return foo;
        }
    }

    @Test
    public void testGetNestedPropertyMap() {
        Foo f = mock( Foo.class );
        Bar b = mock( Bar.class );
        when( b.getFoo() ).thenReturn( f );
        when( f.getId() ).thenReturn( 1L );
        EntityUtils.getNestedPropertyMap( Collections.singleton( b ), "foo", "id" );
        verify( b ).getFoo();
        verify( f ).getId();
    }

}