package ubic.gemma.core.loader.expression.geo.singleCell;

import lombok.extern.slf4j.Slf4j;
import ubic.gemma.core.loader.expression.geo.model.GeoSeries;
import ubic.gemma.core.loader.expression.singleCell.AbstractAnnDataSingleCellDataLoaderConfigurer;
import ubic.gemma.core.loader.expression.singleCell.transform.SingleCellDataTransformationFactory;
import ubic.gemma.core.loader.util.anndata.Dataframe;

import org.springframework.lang.Nullable;
import java.nio.file.Path;

@Slf4j
class GeoAnnDataSingleCellDataLoaderConfigurer extends AbstractAnnDataSingleCellDataLoaderConfigurer {

    private static final GeoSampleToSampleNameMatcher matcher = new GeoSampleToSampleNameMatcher();

    private final GeoSeries series;

    public GeoAnnDataSingleCellDataLoaderConfigurer( Path annDataFile, GeoSeries geoSeries, @Nullable SingleCellDataTransformationFactory singleCellDataTransformationFactory ) {
        super( annDataFile, singleCellDataTransformationFactory );
        this.series = geoSeries;
    }

    @Override
    protected boolean isSampleNameColumn( Dataframe.Column<?, String> column ) {
        return column.uniqueValues().stream().allMatch( val -> matcher.match( series.getSamples(), val ).size() == 1 );
    }
}
