package ubic.gemma.core.loader.expression.singleCell;

import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import ubic.gemma.core.loader.expression.geo.GeoDomainObjectGenerator;
import ubic.gemma.core.loader.expression.geo.model.GeoSeries;
import ubic.gemma.core.loader.expression.geo.service.GeoService;
import ubic.gemma.core.loader.expression.geo.singleCell.GeoSingleCellDetector;
import ubic.gemma.core.loader.expression.geo.singleCell.NoSingleCellDataFoundException;
import ubic.gemma.core.util.test.BaseIntegrationTest;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;

/**
 * Complete integration tests for loading single-cell data.
 */
public class SingleCellDataLoaderServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private GeoService geoService;

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private SingleCellDataLoaderService singleCellDataLoaderService;

    @Value("${gemma.download.path}/singleCellData/GEO")
    private Path downloadDir;

    private ExpressionExperiment ee;

    @After
    public void removeFixtures() {
        if ( ee != null ) {
            expressionExperimentService.remove( ee );
        }
    }

    @Test
    @Ignore
    public void testGSE208742() throws NoSingleCellDataFoundException, IOException {
        GeoSeries series = ( GeoSeries ) new GeoDomainObjectGenerator().generate( "GSE208472" )
                .iterator().next();
        try ( GeoSingleCellDetector detector = new GeoSingleCellDetector() ) {
            detector.setDownloadDirectory( downloadDir );
            detector.downloadSingleCellData( series );
        }
        Collection<?> loaded = geoService.fetchAndLoad( "GSE208742", false, true, true );
        ee = ( ExpressionExperiment ) loaded.iterator().next();
        ArrayDesign genericPlatform = new ArrayDesign();
        SingleCellDataLoaderConfig config = SingleCellDataLoaderConfig.builder()
                .build();
        singleCellDataLoaderService.load( ee, genericPlatform, config );
    }
}