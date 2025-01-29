package ubic.gemma.core.loader.expression.singleCell;

import no.uib.cipr.matrix.io.MatrixInfo;
import no.uib.cipr.matrix.io.MatrixSize;
import no.uib.cipr.matrix.io.MatrixVectorReader;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

public abstract class AbstractMexSingleCellDataLoaderConfigurer implements SingleCellDataLoaderConfigurer<MexSingleCellDataLoader> {

    protected final Log log = LogFactory.getLog( getClass() );

    @Override
    public MexSingleCellDataLoader configureLoader() {
        List<String> sampleNames = getSampleNames();
        List<Path> barcodeFiles = new ArrayList<>();
        List<Path> genesFiles = new ArrayList<>();
        List<Path> matrixFiles = new ArrayList<>();
        List<Path> sampleDirs = getSampleDirs();
        for ( int i = 0; i < sampleDirs.size(); i++ ) {
            Path sampleDir = sampleDirs.get( i );
            if ( !Files.exists( sampleDir ) ) {
                throw new IllegalStateException( "Sample directory " + sampleDir + " for " + sampleNames.get( i ) + " does not exist." );
            }
            Path b = sampleDir.resolve( "barcodes.tsv.gz" ), f = sampleDir.resolve( "features.tsv.gz" ), m = sampleDir.resolve( "matrix.mtx.gz" );
            if ( Files.exists( b ) && Files.exists( f ) && Files.exists( m ) ) {
                barcodeFiles.add( b );
                genesFiles.add( f );
                matrixFiles.add( m );
            } else {
                throw new IllegalStateException( "Expected MEX files are missing in " + sampleDir + "." );
            }
        }
        MexSingleCellDataLoader loader = new MexSingleCellDataLoader( sampleNames, barcodeFiles, genesFiles, matrixFiles );
        try {
            configureDiscardEmptyCell( loader, matrixFiles );
        } catch ( Exception e ) {
            try {
                loader.close();
            } catch ( IOException ex ) {
                log.error( e );
            }
            if ( e instanceof RuntimeException ) {
                throw ( RuntimeException ) e;
            } else {
                throw new RuntimeException( e );
            }
        }
        return loader;
    }

    /**
     * Check if the MEX files contain empty cells and configure the loader accordingly.
     */
    private void configureDiscardEmptyCell( MexSingleCellDataLoader loader, List<Path> matrixFiles ) throws IOException {
        for ( Path matrixFile : matrixFiles ) {
            try ( MatrixVectorReader reader = new MatrixVectorReader( new InputStreamReader( new GZIPInputStream( Files.newInputStream( matrixFile ) ) ) ) ) {
                MatrixInfo info = reader.readMatrixInfo();
                MatrixSize size = reader.readMatrixSize( info );
                if ( ( double ) size.numEntries() / ( ( double ) size.numRows() * ( double ) size.numColumns() ) < 0.01 ) {
                    log.info( matrixFile + " has less than 1% of non-zeroes, this is likely due to empty cells, so we'll discard those." );
                    loader.setDiscardEmptyCells( true );
                    break;
                }
            }
        }
    }

    protected abstract List<String> getSampleNames();

    protected abstract List<Path> getSampleDirs();
}
