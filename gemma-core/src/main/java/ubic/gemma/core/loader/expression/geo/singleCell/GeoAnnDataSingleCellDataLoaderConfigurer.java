package ubic.gemma.core.loader.expression.geo.singleCell;

import lombok.extern.apachecommons.CommonsLog;
import ubic.gemma.core.loader.expression.geo.model.GeoSeries;
import ubic.gemma.core.loader.expression.singleCell.AbstractAnnDataSingleCellDataLoaderConfigurer;
import ubic.gemma.core.loader.expression.singleCell.transform.SingleCellDataTransformationFactory;
import ubic.gemma.core.loader.util.anndata.Dataframe;

import javax.annotation.Nullable;
import java.nio.file.Path;

@CommonsLog
class GeoAnnDataSingleCellDataLoaderConfigurer extends AbstractAnnDataSingleCellDataLoaderConfigurer {

    private static final GeoSampleToSampleNameMatcher matcher = new GeoSampleToSampleNameMatcher();

    private final GeoSeries series;

    public GeoAnnDataSingleCellDataLoaderConfigurer( Path annDataFile, GeoSeries geoSeries, @Nullable SingleCellDataTransformationFactory singleCellDataTransformationFactory ) {
        super( annDataFile, singleCellDataTransformationFactory );
        this.series = geoSeries;
    }

    @Override
    protected boolean isSampleNameColumn( Dataframe.Column<?, String> column ) {
        return matcher.matchAll( series.getSamples(), column.uniqueValues() )
                .values().stream()
                .allMatch( s -> s.size() == 1 );
    }
}
