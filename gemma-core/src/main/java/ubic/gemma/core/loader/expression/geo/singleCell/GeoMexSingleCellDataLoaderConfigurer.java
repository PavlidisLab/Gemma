package ubic.gemma.core.loader.expression.geo.singleCell;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.util.Assert;
import ubic.gemma.core.loader.expression.geo.model.GeoSample;
import ubic.gemma.core.loader.expression.geo.model.GeoSeries;
import ubic.gemma.core.loader.expression.singleCell.MexSingleCellDataLoader;
import ubic.gemma.core.loader.expression.singleCell.SingleCellDataLoaderConfigurer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@CommonsLog
class GeoMexSingleCellDataLoaderConfigurer implements SingleCellDataLoaderConfigurer<MexSingleCellDataLoader> {

    private final Path downloadDir;
    private final GeoSeries series;

    public GeoMexSingleCellDataLoaderConfigurer( Path downloadDir, GeoSeries series ) {
        this.downloadDir = downloadDir;
        this.series = series;
    }

    @Override
    public MexSingleCellDataLoader configureLoader() {
        List<String> sampleNames = new ArrayList<>();
        List<Path> barcodesFiles = new ArrayList<>(),
                featuresFiles = new ArrayList<>(),
                matricesFiles = new ArrayList<>();

        for ( GeoSample sample : series.getSamples() ) {
            Assert.notNull( sample.getGeoAccession() );
            Path sampleDir = downloadDir.resolve( sample.getGeoAccession() );
            if ( Files.exists( sampleDir ) ) {
                Path b = sampleDir.resolve( "barcodes.tsv.gz" ), f = sampleDir.resolve( "features.tsv.gz" ), m = sampleDir.resolve( "matrix.mtx.gz" );
                if ( Files.exists( b ) && Files.exists( f ) && Files.exists( m ) ) {
                    sampleNames.add( sample.getGeoAccession() );
                    barcodesFiles.add( b );
                    featuresFiles.add( f );
                    matricesFiles.add( m );
                } else {
                    throw new IllegalStateException( String.format( "Expected MEX files are missing in %s", sampleDir ) );
                }
            } else {
                log.warn( "No MEX data dir for " + sample );
            }
        }

        return new MexSingleCellDataLoader( sampleNames, barcodesFiles, featuresFiles, matricesFiles );
    }
}
