package ubic.gemma.core.loader.expression.singleCell.transform;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import java.nio.file.Path;

/**
 * @author poirigui
 */
@Configuration
public class SingleCellTransformationConfig {

    @Value("${python.exe}")
    private Path pythonExecutable;

    @Value("${cellranger.dir}")
    private Path cellRangerPrefix;

    @Bean
    @Scope("prototype")
    public SingleCellDataTranspose singleCellDataTranspose() {
        return configure( new SingleCellDataTranspose() );
    }

    @Bean
    @Scope("prototype")
    public SingleCellDataPack singleCellDataPack() {
        return configure( new SingleCellDataPack() );
    }

    @Bean
    @Scope("prototype")
    public SingleCellDataSortBySample singleCellDataSortBySample() {
        return configure( new SingleCellDataSortBySample() );
    }

    @Bean
    @Scope("prototype")
    public SingleCellDataSample singleCellDataSample() {
        return configure( new SingleCellDataSample() );
    }

    @Bean
    @Scope("prototype")
    public SingleCellDataRewrite singleCellDataRewrite() {
        return configure( new SingleCellDataRewrite() );
    }

    @Bean
    @Scope("prototype")
    public SingleCellDataUnraw singleCellDataUnraw() {
        return configure( new SingleCellDataUnraw() );
    }

    @Bean
    @Scope("prototype")
    public SingleCellDataSparsify singleCellDataSparsify() {
        return configure( new SingleCellDataSparsify() );
    }

    @Bean
    @Scope("prototype")
    public SingleCell10xMexFilter singleCell10xMexFilter() {
        return configure( new SingleCell10xMexFilter() );
    }

    private <T extends SingleCellDataTransformation> T configure( T transformation ) {
        if ( transformation instanceof PythonBasedSingleCellDataTransformation ) {
            ( ( PythonBasedSingleCellDataTransformation ) transformation )
                    .setPythonExecutable( pythonExecutable );
        }
        if ( transformation instanceof CellRangerBasedTransformation ) {
            ( ( CellRangerBasedTransformation ) transformation )
                    .setCellRangerPrefix( cellRangerPrefix );
        }
        return transformation;
    }
}
