package ubic.gemma.core.loader.expression.singleCell.transform;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.BeanFactory;
import ubic.gemma.core.loader.expression.singleCell.SingleCellDataType;

import org.springframework.lang.Nullable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Factory for obtaining {@link SingleCellDataTransformation}s.
 *
 * @author poirigui
 */
@CommonsLog
public class SingleCellDataTransformationFactoryImpl implements SingleCellDataTransformationFactory {

    private final BeanFactory beanFactory;
    @Nullable
    private final Path scratchDir;

    public SingleCellDataTransformationFactoryImpl( BeanFactory beanFactory, @Nullable Path scratchDir ) {
        this.beanFactory = beanFactory;
        this.scratchDir = scratchDir;
    }

    /**
     * Obtain a single-cell data transformation.
     * <p>
     * The transformation is pre-configured, so all that is left is to set any input/output files.
     */
    @Override
    public <T extends SingleCellDataTransformation> T getTransformation( Class<T> transformationClass ) {
        return beanFactory.getBean( transformationClass );
    }

    /**
     * Create a pipeline of single-cell data transformations.
     * <p>
     * The transformations are pre-configured as per {@link #getTransformation(Class)}.
     */
    @Override
    public final SingleCellDataTransformationPipeline createPipeline( List<Class<? extends SingleCellInputOutputFileTransformation>> transformations ) {
        return new SingleCellDataTransformationPipeline( transformations.stream()
                .map( this::getTransformation )
                .collect( Collectors.toList() ), scratchDir );
    }

    /**
     * Obtain a temporary file for the output of a transformation.
     * <p>
     * The file will be created in the scratch directory if it is configured.
     * <p>
     * In the case of {@link SingleCellDataType#MEX}, a temporary directory is created instead.
     *
     * @see SingleCellDataTransformationUtils#createTemporaryFile(Path, SingleCellDataType)
     */
    @Override
    public Path createTemporaryFile( SingleCellDataType dataType ) throws IOException {
        return SingleCellDataTransformationUtils.createTemporaryFile( scratchDir, dataType );
    }
}
