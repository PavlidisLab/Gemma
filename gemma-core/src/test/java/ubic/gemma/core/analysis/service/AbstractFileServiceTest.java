package ubic.gemma.core.analysis.service;

import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AbstractFileServiceTest {

    private final AbstractFileService<Object> tsvFileService = new AbstractFileService<Object>() {
        @Override
        public void writeTsv( Object entity, Writer writer ) throws IOException {
            writer.write( escapeTsv( String.valueOf( entity ) ) );
        }
    };

    @Test
    public void testFormatNumber() {
        assertEquals( "0.0", tsvFileService.format( 0.0 ) );
        assertEquals( "0.0", tsvFileService.format( -0.0 ) );
        assertEquals( "1E-14", tsvFileService.format( 1e-14 ) );
        assertEquals( "-1E-14", tsvFileService.format( -1e-14 ) );
        assertEquals( "0.1111", tsvFileService.format( 0.1111 ) );
        assertEquals( "0.0001", tsvFileService.format( 0.0001 ) );
        assertEquals( "1E-5", tsvFileService.format( 1e-5 ) );
        assertEquals( "1000.0", tsvFileService.format( 1e3 ) );
        assertEquals( "1000.0", tsvFileService.format( 1000.0 ) );
        assertEquals( "1234.5", tsvFileService.format( 1234.5 ) );
        assertEquals( "100000.0", tsvFileService.format( 1e5 ) );
        assertEquals( "-100000.0", tsvFileService.format( -1e5 ) );
        assertEquals( "100.1234", tsvFileService.format( 100.1234 ) );
        assertEquals( "-100.1234", tsvFileService.format( -100.1234 ) );
        assertEquals( "1000.1", tsvFileService.format( 1000.1234 ) );
        assertEquals( "100000.1", tsvFileService.format( 100000.1234 ) );
        assertEquals( "", tsvFileService.format( null ) );
        assertEquals( "", tsvFileService.format( Double.NaN ) );
        assertEquals( "inf", tsvFileService.format( Double.POSITIVE_INFINITY ) );
        assertEquals( "-inf", tsvFileService.format( Double.NEGATIVE_INFINITY ) );
    }

    @Test
    public void testWrite() throws IOException {
        tsvFileService.write( null, new StringWriter(), "text/tab-separated-values" );
        tsvFileService.write( null, new StringWriter(), "application/json" );
        Assert.assertThrows( IllegalArgumentException.class, () -> {
            tsvFileService.write( null, new StringWriter(), "rdf/xml" );
        } );
    }

    @Test
    public void testWriteToFile() throws IOException {
        File f = File.createTempFile( "test", null );
        try {
            tsvFileService.write( "test", f, "text/tab-separated-values" );
            Assertions.assertThat( f ).hasContent( "test" );
            tsvFileService.write( "test2", f, "text/tab-separated-values" );
            Assertions.assertThat( f ).hasContent( "test2" );
        } finally {
            assertTrue( f.delete() );
        }
    }

    @Test
    public void testEscapeTsv() {
        assertEquals( "\\t\\n\\r\\\\", tsvFileService.escapeTsv( "\t\n\r\\" ) );
    }
}