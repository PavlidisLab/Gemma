package ubic.gemma.core.loader.expression.geo.singleCell;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.util.Assert;
import ubic.gemma.core.loader.expression.geo.model.GeoSample;
import ubic.gemma.core.loader.expression.geo.model.GeoSeries;
import ubic.gemma.core.loader.expression.singleCell.AbstractMexSingleCellDataLoaderConfigurer;

import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Objects.requireNonNull;

@CommonsLog
public class GeoMexSingleCellDataLoaderConfigurer extends AbstractMexSingleCellDataLoaderConfigurer {

    private final GeoSeries series;
    private final Map<String, GeoSample> geoSampleBySampleName;
    private final List<String> sampleNames;
    private final List<Path> sampleDirs;

    public GeoMexSingleCellDataLoaderConfigurer( Path mexDir, GeoSeries series, @Nullable Path cellRangerPrefix ) {
        super( cellRangerPrefix );
        this.series = series;
        geoSampleBySampleName = new HashMap<>();
        sampleNames = new ArrayList<>();
        sampleDirs = new ArrayList<>();
        for ( GeoSample sample : series.getSamples() ) {
            Assert.notNull( sample.getGeoAccession() );
            Path sampleDir = mexDir.resolve( sample.getGeoAccession() );
            sampleNames.add( sample.getGeoAccession() );
            sampleDirs.add( sampleDir );
            geoSampleBySampleName.put( sample.getGeoAccession(), sample );
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

    @Override
    protected boolean detect10x( String sampleName, Path sampleDir ) {
        if ( super.detect10x( sampleName, sampleDir ) ) {
            return true;
        }
        // rely on additional heuristics
        return TenXCellRangerUtils.detect10x( getGeoSample( sampleName ) );
    }

    @Override
    protected boolean detectUnfiltered( String sampleName, Path sampleDir ) {
        return TenXCellRangerUtils.detect10xUnfiltered( getGeoSample( sampleName ) );
    }

    @Override
    protected String detect10xGenome( String sampleName, Path sampleDir ) {
        return getGeoSample( sampleName ).getOrganism();
    }

    @Override
    protected String detect10xChemistry( String sampleName, Path sampleDir ) {
        String chemistry;
        if ( ( chemistry = TenXCellRangerUtils.detect10xChemistry( getGeoSample( sampleName ) ) ) != null ) {
            return chemistry;
        } else {
            return null;
        }
    }

    private GeoSample getGeoSample( String sampleName ) {
        return requireNonNull( geoSampleBySampleName.get( sampleName ),
                "Expected " + series + " to have a sample named " + sampleName + "." );
    }
}
