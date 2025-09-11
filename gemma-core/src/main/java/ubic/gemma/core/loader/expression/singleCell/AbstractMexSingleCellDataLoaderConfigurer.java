package ubic.gemma.core.loader.expression.singleCell;

import no.uib.cipr.matrix.io.MatrixInfo;
import no.uib.cipr.matrix.io.MatrixSize;
import no.uib.cipr.matrix.io.MatrixVectorReader;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ubic.gemma.core.util.FileUtils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractMexSingleCellDataLoaderConfigurer implements SingleCellDataLoaderConfigurer<MexSingleCellDataLoader> {

    protected final Log log = LogFactory.getLog( getClass() );

    @Override
    public MexSingleCellDataLoader configureLoader( SingleCellDataLoaderConfig config ) {
        List<String> sampleNames = getSampleNames();
        List<String> usedSampleNames = new ArrayList<>();
        List<Path> barcodeFiles = new ArrayList<>();
        List<Path> genesFiles = new ArrayList<>();
        List<Path> matrixFiles = new ArrayList<>();
        List<Path> sampleDirs = getSampleDirs();
        for ( int i = 0; i < sampleDirs.size(); i++ ) {
            String sampleName = sampleNames.get( i );
            Path sampleDir = sampleDirs.get( i );
            if ( !Files.exists( sampleDir ) ) {
                String m2 = "Sample directory " + sampleDir + " for " + sampleName + " does not exist.";
                if ( config.isIgnoreSamplesLackingData() ) {
                    log.warn( m2 );
                    continue;
                } else {
                    throw new IllegalStateException( m2 + " You can set ignoreSamplesLackingData to ignore this error." );
                }
            }
            Path b = sampleDir.resolve( "barcodes.tsv.gz" );
            Path f = sampleDir.resolve( "features.tsv.gz" );
            Path m = sampleDir.resolve( "matrix.mtx.gz" );
            if ( Files.exists( b ) && Files.exists( f ) && Files.exists( m ) ) {
                usedSampleNames.add( sampleName );
                barcodeFiles.add( b );
                genesFiles.add( f );
                matrixFiles.add( m );
            } else {
                String m2 = "Expected MEX files are missing in " + sampleDir + ".";
                if ( config.isIgnoreSamplesLackingData() ) {
                    log.warn( m2 );
                } else {
                    throw new IllegalStateException( m2 + " You can set ignoreSamplesLackingData to ignore this error." );
                }
            }
        }
        MexSingleCellDataLoader loader = new MexSingleCellDataLoader( usedSampleNames, barcodeFiles, genesFiles, matrixFiles );
        if ( config instanceof MexSingleCellDataLoaderConfig ) {
            MexSingleCellDataLoaderConfig mexConfig = ( MexSingleCellDataLoaderConfig ) config;
            loader.setAllowMappingDesignElementsToGeneSymbols( mexConfig.isAllowMappingDesignElementsToGeneSymbols() );
            loader.setUseDoublePrecision( ( mexConfig.isUseDoublePrecision() ) );
        }
        return loader;
    }

    protected abstract List<String> getSampleNames();

    protected abstract List<Path> getSampleDirs();
}
