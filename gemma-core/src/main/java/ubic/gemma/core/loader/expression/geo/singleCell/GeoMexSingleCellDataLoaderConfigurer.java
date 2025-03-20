package ubic.gemma.core.loader.expression.geo.singleCell;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.util.Assert;
import ubic.gemma.core.loader.expression.geo.model.GeoSample;
import ubic.gemma.core.loader.expression.geo.model.GeoSeries;
import ubic.gemma.core.loader.expression.singleCell.AbstractMexSingleCellDataLoaderConfigurer;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@CommonsLog
class GeoMexSingleCellDataLoaderConfigurer extends AbstractMexSingleCellDataLoaderConfigurer {

    private final List<String> sampleNames;
    private final List<Path> sampleDirs;

    public GeoMexSingleCellDataLoaderConfigurer( Path mexDir, GeoSeries series ) {
        sampleNames = new ArrayList<>();
        sampleDirs = new ArrayList<>();
        for ( GeoSample sample : series.getSamples() ) {
            Assert.notNull( sample.getGeoAccession() );
            Path sampleDir = mexDir.resolve( sample.getGeoAccession() );
            sampleNames.add( sample.getGeoAccession() );
            sampleDirs.add( sampleDir );
        }
    }

    @Override
    protected List<String> getSampleNames() {
        return sampleNames;
    }

    @Override
    protected List<Path> getSampleDirs() {
        return sampleDirs;
    }
}
