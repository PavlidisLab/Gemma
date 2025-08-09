package ubic.gemma.core.datastructure.matrix.io;

import ubic.gemma.model.expression.bioAssayData.SingleCellDimension;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import java.io.IOException;
import java.io.Writer;

public interface SingleCellMetadataWriter {

    void write( ExpressionExperiment ee, SingleCellDimension singleCellDimension, Writer writer ) throws IOException;
}
