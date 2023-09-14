package ubic.gemma.core.datastructure.matrix;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MatrixWriterTest {

    @Test
    public void testReplacement() {
        assertEquals( "\\t\\n\\r\\\\", MatrixWriter.escapeTsv( "\t\n\r\\" ) );
    }
}