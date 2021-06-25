package ubic.gemma.core.analysis.service;

import org.apache.commons.lang.NotImplementedException;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class AbstractTsvFileServiceTest {

    private AbstractTsvFileService<Object> tsvFileService = new AbstractTsvFileService<Object>() {
        @Override
        public void writeTsvToAppendable( Object entity, Appendable appendable ) {
            throw new NotImplementedException( "This is just a stub!" );
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
    }
}