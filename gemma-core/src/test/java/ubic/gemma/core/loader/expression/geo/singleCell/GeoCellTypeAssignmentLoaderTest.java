package ubic.gemma.core.loader.expression.geo.singleCell;

import org.junit.Test;
import ubic.gemma.core.loader.expression.geo.model.GeoSeries;

import static org.mockito.Mockito.mock;

public class GeoCellTypeAssignmentLoaderTest {

    @Test
    public void testGSE() {
        GeoSeries series;
        GeoCellTypeAssignmentLoader loader = new GeoCellTypeAssignmentLoader( series, mock() );
        loader.getCellTypeAssignments( dimension );
    }
}