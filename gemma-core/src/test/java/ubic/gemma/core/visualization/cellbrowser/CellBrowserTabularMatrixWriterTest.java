package ubic.gemma.core.visualization.cellbrowser;

import org.junit.Test;
import ubic.gemma.core.datastructure.matrix.SingleCellExpressionDataDoubleMatrix;
import ubic.gemma.model.expression.bioAssayData.SingleCellExpressionDataVector;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;
import static ubic.gemma.persistence.service.expression.bioAssayData.RandomSingleCellDataUtils.randomSingleCellVectors;
import static ubic.gemma.persistence.service.expression.bioAssayData.RandomSingleCellDataUtils.setSeed;

public class CellBrowserTabularMatrixWriterTest {

    @Test
    public void test() throws IOException {
        CellBrowserTabularMatrixWriter writer = new CellBrowserTabularMatrixWriter();
        setSeed( 123 );
        Collection<SingleCellExpressionDataVector> vectors = randomSingleCellVectors();
        SingleCellExpressionDataDoubleMatrix matrix = new SingleCellExpressionDataDoubleMatrix( vectors );
        byte[] blob;
        try ( ByteArrayOutputStream baos = new ByteArrayOutputStream() ) {
            writer.write( vectors, null, new OutputStreamWriter( baos, StandardCharsets.UTF_8 ) );
            blob = baos.toByteArray();
        }
        assertThat( blob ).asString( StandardCharsets.UTF_8 )
                .startsWith( "gene\t" )
                .hasLineCount( 101 );
    }
}