package ubic.gemma.core.loader.expression.geo.singleCell;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.util.Assert;
import ubic.gemma.core.loader.expression.geo.model.GeoChannel;
import ubic.gemma.core.loader.expression.geo.model.GeoSample;
import ubic.gemma.core.loader.expression.geo.model.GeoSeries;
import ubic.gemma.core.loader.expression.singleCell.AbstractMexSingleCellDataLoaderConfigurer;
import ubic.gemma.core.loader.expression.singleCell.TenXCellRangerUtils;

import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.*;

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
        return TenXCellRangerUtils.detect10x( getGeoSample( sampleName ).getDataProcessing() )
                || super.detect10x( sampleName, sampleDir );
    }

    @Override
    protected boolean detectUnfiltered( String sampleName, Path sampleDir ) {
        if ( TenXCellRangerUtils.detect10xUnfiltered( getGeoSample( sampleName ).getDataProcessing() ) ) {
            return true;
        } else if ( TenXCellRangerUtils.detect10xFiltered( getGeoSample( sampleName ).getDataProcessing() ) ) {
            return false;
        } else {
            log.warn( "Failed to detect if " + sampleName + " is unfiltered from its GEO metadata, will have to lookup the MEX file." );
            return super.detectUnfiltered( sampleName, sampleDir );
        }
    }

    @Override
    protected String detect10xGenome( String sampleName, Path sampleDir ) {
        return getGeoSample( sampleName ).getOrganism();
    }

    @Override
    protected String detect10xChemistry( String sampleName, Path sampleDir ) {
        return getGeoSample( sampleName ).getChannels().stream()
                .map( GeoChannel::getExtractProtocol )
                .map( TenXCellRangerUtils::detect10xChemistry )
                .filter( Objects::nonNull )
                .findFirst().orElse( null );
    }

    private GeoSample getGeoSample( String sampleName ) {
        return requireNonNull( geoSampleBySampleName.get( sampleName ),
                "Expected " + series + " to have a sample named " + sampleName + "." );
    }
}
