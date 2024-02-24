package ubic.gemma.core.loader.expression.geo;

import org.junit.Test;
import ubic.gemma.core.loader.expression.geo.model.GeoSeries;
import ubic.gemma.core.loader.expression.singleCell.AnnDataSingleCellDataLoader;
import ubic.gemma.core.loader.expression.singleCell.MexSingleCellDataLoader;
import ubic.gemma.core.loader.expression.singleCell.SingleCellDataLoader;
import ubic.gemma.model.expression.biomaterial.BioMaterial;

import java.util.Collection;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class GeoSingleCellDetectorTest {

    /**
     * AnnData (and also Seurat Disk, but the former is preferred)
     */
    @Test
    public void testGSE225158() {
        GeoSingleCellDetector detector = new GeoSingleCellDetector();
        GeoSeries series = mock();
        Collection<BioMaterial> samples = mock();
        Optional<SingleCellDataLoader> loader = detector.getSingleCellDataLoader( series, samples );
        assertThat( loader ).hasValueSatisfying( loader2 -> {
            assertThat( loader2 ).isInstanceOf( AnnDataSingleCellDataLoader.class );
        } );
    }

    /**
     * Typical MEX format: all the files are packed per-sample.
     */
    @Test
    public void testGSE224438() {
        GeoSingleCellDetector detector = new GeoSingleCellDetector();
        GeoSeries series = mock();
        Collection<BioMaterial> samples = mock();
        Optional<SingleCellDataLoader> loader = detector.getSingleCellDataLoader( series, samples );
        assertThat( loader ).hasValueSatisfying( loader2 -> {
            assertThat( loader2 ).isInstanceOf( MexSingleCellDataLoader.class );
        } );
    }

    /**
     * This is a case of a MEX dataset stored with individual gzipped files.
     */
    @Test
    public void testGSE201814() {
        GeoSingleCellDetector detector = new GeoSingleCellDetector();
        GeoSeries series = mock();
        Collection<BioMaterial> samples = mock();
        Optional<SingleCellDataLoader> loader = detector.getSingleCellDataLoader( series, samples );
        assertThat( loader ).hasValueSatisfying( loader2 -> {
            assertThat( loader2 ).isInstanceOf( MexSingleCellDataLoader.class );
        } );
    }
}