package ubic.gemma.core.loader.expression.geo.singleCell;

import lombok.extern.apachecommons.CommonsLog;
import ubic.gemma.core.loader.expression.geo.model.GeoSeries;
import ubic.gemma.core.loader.expression.singleCell.AbstractAnnDataSingleCellDataLoaderConfigurer;
import ubic.gemma.core.loader.util.anndata.Dataframe;

import java.nio.file.Path;
import java.util.Set;

@CommonsLog
class GeoAnnDataSingleCellDataLoaderConfigurer extends AbstractAnnDataSingleCellDataLoaderConfigurer {

    private static final GeoSampleToSampleNameMatcher matcher = new GeoSampleToSampleNameMatcher();

    private final GeoSeries series;

    public GeoAnnDataSingleCellDataLoaderConfigurer( Path annDataFile, GeoSeries geoSeries ) {
        super( annDataFile );
        this.series = geoSeries;
    }

    @Override
    protected boolean isSampleNameColumn( Dataframe<?> ad, String col, Set<String> vals ) {
        return vals.stream().allMatch( val -> matcher.match( series.getSamples(), val ).size() == 1 );
    }
}
