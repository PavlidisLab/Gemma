package ubic.gemma.core.apps;

import org.springframework.stereotype.Component;
import ubic.gemma.core.loader.expression.singleCell.MexSingleCellDataLoader;
import ubic.gemma.core.loader.expression.singleCell.SingleCellDataLoader;

import javax.annotation.Nullable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

@Component
public class SingleCellDataLoaderCli extends ExpressionExperimentManipulatingCLI {

    @Override
    protected void doWork() throws Exception {
        Path dir = Paths.get( "/home/guillaume/Téléchargements/GSE224438_RAW" );
        List<String> sampleNames = Collections.singletonList( "GSM7022367" );
        List<Path> barcodeFiles = Collections.singletonList( dir.resolve( "GSM7022367_1_barcodes.tsv.gz" ) );
        List<Path> genesFiles = Collections.singletonList( dir.resolve( "GSM7022367_1_features.tsv.gz" ) );
        List<Path> matrixFiles = Collections.singletonList( dir.resolve( "GSM7022367_1_matrix.tsv.gz" ) );
        SingleCellDataLoader loader = new MexSingleCellDataLoader( sampleNames, barcodeFiles, genesFiles, matrixFiles );
    }

    @Nullable
    @Override
    public String getCommandName() {
        return "loadSingleCellData";
    }

    @Nullable
    @Override
    public String getShortDesc() {
        return "Load single-cell data.";
    }

    @Override
    public CommandGroup getCommandGroup() {
        return CommandGroup.EXPERIMENT;
    }
}
