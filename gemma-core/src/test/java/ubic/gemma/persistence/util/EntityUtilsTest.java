package ubic.gemma.persistence.util;

import org.junit.Test;
import ubic.gemma.model.common.Identifiable;

import java.util.ArrayList;
import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;

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
        assertThat( IdentifiableUtils.getIdMap( foos ) )
                .containsKey( 1L );
    }
}