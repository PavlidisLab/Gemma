package ubic.gemma.core.loader.expression.singleCell.transform;

import ubic.gemma.core.loader.expression.singleCell.SingleCellDataType;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * @author  poirigui
 */
public interface SingleCellDataTransformationFactory {

    <T extends SingleCellDataTransformation> T getTransformation( Class<T> transformationClass );

    SingleCellDataTransformationPipeline createPipeline( List<Class<? extends SingleCellInputOutputFileTransformation>> transformations );

    Path createTemporaryFile( SingleCellDataType dataType ) throws IOException;
}
