package ubic.gemma.core.analysis.report;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;

import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Failures to write a platform report used to be logged and swallowed, after the previous report had already been
 * deleted.
 */
class ArrayDesignReportServiceImplTest {

    @TempDir
    Path appdataHome;

    private ArrayDesignReportServiceImpl service;
    private ArrayDesignService arrayDesignService;

    @BeforeEach
    void setUp() {
        arrayDesignService = mock();
        service = new ArrayDesignReportServiceImpl();
        ReflectionTestUtils.setField( service, "arrayDesignService", arrayDesignService );
        ReflectionTestUtils.setField( service, "appdataHome", appdataHome.toString() );
        ArrayDesign ad = ArrayDesign.Factory.newInstance( "GPL1", null );
        ad.setId( 1L );
        when( arrayDesignService.load( 1L ) ).thenReturn( ad );
    }

    @Test
    void failedWriteThrowsAndKeepsThePreviousReport() throws Exception {
        ArrayDesignValueObject first = new ArrayDesignValueObject( 1L );
        first.setShortName( "GPL1" );
        service.generateArrayDesignReport( first );
        String dateCached = service.getSummaryObject( 1L ).getDateCached();
        assertThat( dateCached ).isNotNull();

        assertThatThrownBy( () -> service.generateArrayDesignReport( new Unserializable( 1L ) ) )
                .isInstanceOf( UncheckedIOException.class );

        ArrayDesignValueObject kept = service.getSummaryObject( 1L );
        assertThat( kept ).isNotNull();
        assertThat( kept.getShortName() ).isEqualTo( "GPL1" );
        try ( Stream<Path> files = Files.list( appdataHome.resolve( "ArrayDesignReports" ) ) ) {
            assertThat( files ).extracting( p -> p.getFileName().toString() )
                    .containsExactly( "ArrayDesignReport.1" );
        }
    }

    @Test
    void unwritableDirectoryThrows() throws Exception {
        // a regular file where the report directory should be
        Files.createFile( appdataHome.resolve( "ArrayDesignReports" ) );

        assertThatThrownBy( () -> service.generateArrayDesignReport( new ArrayDesignValueObject( 1L ) ) )
                .isInstanceOf( UncheckedIOException.class );
        assertThatThrownBy( () -> service.generateAllArrayDesignReport() )
                .isInstanceOf( UncheckedIOException.class );
    }

    /**
     * The id variant is called as a side effect of other work, and keeps logging rather than throwing.
     */
    @Test
    void idVariantLogsAndReturnsNull() throws Exception {
        Files.createFile( appdataHome.resolve( "ArrayDesignReports" ) );
        when( arrayDesignService.loadValueObjectsByIds( anyCollection() ) )
                .thenReturn( Collections.singletonList( new ArrayDesignValueObject( 1L ) ) );

        assertThat( service.generateArrayDesignReport( 1L ) ).isNull();
    }

    /**
     * A report whose serialization fails part-way through.
     */
    private static class Unserializable extends ArrayDesignValueObject {

        @SuppressWarnings("unused")
        private final Object notSerializable = new Object();

        private Unserializable( Long id ) {
            super( id );
        }
    }
}
