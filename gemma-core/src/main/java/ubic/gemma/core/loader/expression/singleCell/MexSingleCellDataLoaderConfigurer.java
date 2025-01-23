package ubic.gemma.core.loader.expression.singleCell;

import ubic.gemma.core.loader.util.mapper.BioAssayMapper;
import ubic.gemma.model.expression.bioAssay.BioAssay;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Configure a {@link MexSingleCellDataLoader} for a given directory and collection of {@link BioAssay}s.
 * @author poirigui
 */
public class MexSingleCellDataLoaderConfigurer implements SingleCellDataLoaderConfigurer<MexSingleCellDataLoader> {

    private final Path mexDir;
    private final Collection<BioAssay> bioAssays;
    private final BioAssayMapper bioAssayMapper;

    public MexSingleCellDataLoaderConfigurer( Path mexDir, Collection<BioAssay> bioAssays, BioAssayMapper bioAssayMapper ) {
        this.mexDir = mexDir;
        this.bioAssays = bioAssays;
        this.bioAssayMapper = bioAssayMapper;
    }

    @Override
    public MexSingleCellDataLoader configureLoader() {
        List<String> sampleNames = new ArrayList<>();
        List<Path> barcodeFiles = new ArrayList<>();
        List<Path> genesFiles = new ArrayList<>();
        List<Path> matrixFiles = new ArrayList<>();
        for ( BioAssay ba : bioAssays ) {
            Path sampleDir;
            if ( ba.getAccession() != null ) {
                sampleDir = mexDir.resolve( ba.getAccession().getAccession() );
            } else {
                sampleDir = mexDir.resolve( ba.getName() );
            }
            if ( !Files.exists( sampleDir ) ) {
                throw new IllegalStateException( "Sample directory " + sampleDir + " for " + ba + " does not exist." );
            }
            Path b = sampleDir.resolve( "barcodes.tsv.gz" ), f = sampleDir.resolve( "features.tsv.gz" ), m = sampleDir.resolve( "matrix.mtx.gz" );
            if ( Files.exists( sampleDir.resolve( "barcodes.tsv.gz" ) ) ) {
                sampleNames.add( ba.getName() );
                barcodeFiles.add( b );
                genesFiles.add( f );
                matrixFiles.add( m );
            } else {
                throw new IllegalStateException( "Expected MEX files are missing in " + sampleDir + "." );
            }
        }
        MexSingleCellDataLoader loader = new MexSingleCellDataLoader( sampleNames, barcodeFiles, genesFiles, matrixFiles );
        loader.setBioAssayToSampleNameMapper( bioAssayMapper );
        return loader;
    }
}
