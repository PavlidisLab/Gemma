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
            writer.write( String.valueOf( entity ).replace( '\t', ' ' ) );
        }
    };

    @Test
    public void testParse() {
        assertEquals( "1E-14", tsvFileService.format( 1e-14 ) );
        assertEquals( "1.111E-1", tsvFileService.format( 0.1111 ) );
        assertEquals( "1E-5", tsvFileService.format( 1e-5 ) );
        assertEquals( "1E3", tsvFileService.format( 1000.0 ) );
        assertEquals( "1.235E3", tsvFileService.format( 1234.5 ) );
        assertEquals( "1E5", tsvFileService.format( 1e5 ) );
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
}