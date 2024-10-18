package ubic.gemma.core.datastructure.matrix.io;

import org.junit.Test;
import ubic.gemma.core.datastructure.matrix.DoubleSingleCellExpressionDataMatrix;
import ubic.gemma.model.expression.bioAssayData.SingleCellExpressionDataVector;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;
import static ubic.gemma.core.datastructure.matrix.io.TsvUtils.GEMMA_CITATION_NOTICE;
import static ubic.gemma.persistence.service.expression.experiment.SingleCellTestUtils.randomSingleCellVectors;

public class TabularMatrixWriterTest {

    private final TabularMatrixWriter writer = new TabularMatrixWriter();

    @Test
    public void test() throws IOException {
        Collection<SingleCellExpressionDataVector> vectors = randomSingleCellVectors();
        DoubleSingleCellExpressionDataMatrix matrix = new DoubleSingleCellExpressionDataMatrix( vectors );
        byte[] blob;
        try ( ByteArrayOutputStream baos = new ByteArrayOutputStream() ) {
            writer.write( matrix, baos );
            blob = baos.toByteArray();
        }
        assertThat( blob ).asString( StandardCharsets.UTF_8 )
                .contains( "probe_id\tprobe_name\tsample_id\tsample_name\tcell_id\tvalue" )
                .hasLineCount( 40000 + 3 + 1 + GEMMA_CITATION_NOTICE.length );
    }
}